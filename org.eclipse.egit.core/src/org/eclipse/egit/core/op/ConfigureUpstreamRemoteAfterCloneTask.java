/*******************************************************************************
 * Copyright (C) 2026 EGit Contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.NetUtil;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.credentials.UserPasswordCredentials;
import org.eclipse.egit.core.settings.GitSettings;
import org.eclipse.equinox.security.storage.StorageException;
import org.osgi.service.prefs.BackingStoreException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

/**
 * Post-clone task that performs best-effort detection of whether the cloned
 * repository is a fork on GitHub, GitLab, or Bitbucket, and if so adds an
 * {@code upstream} remote pointing to the parent repository.
 * <p>
 * HTTP calls use {@link java.net.HttpURLConnection} which honours the Java
 * {@link java.net.ProxySelector} and {@link java.net.Authenticator} registered
 * by EGit's {@code EclipseProxySelector} and {@code EclipseAuthenticator}.
 * This makes the calls compatible with Eclipse "Native" proxy mode, including
 * NTLM-authenticated system proxies on Windows.
 * </p>
 * <p>
 * When a fork is detected the repository config is marked with
 * {@code egit.forkScenarioDetected=true} so that other UI components (e.g. the
 * Pull dialog) can adapt their defaults accordingly.
 * </p>
 */
public class ConfigureUpstreamRemoteAfterCloneTask
		implements CloneOperation.PostCloneTask {

	/** Name of the remote created for the upstream (parent) repository. */
	public static final String UPSTREAM_REMOTE_NAME = "upstream"; //$NON-NLS-1$

	/**
	 * Eclipse instance-scope preference key used to record that a fork/upstream
	 * scenario was detected at clone time. Stored per-repository via
	 * {@link RepositoryUtil#getRepositorySpecificPreferenceKey}.
	 */
	public static final String FORK_SCENARIO_PREF_KEY = "forkScenarioDetected"; //$NON-NLS-1$

	private static final int HTTP_TIMEOUT_MS = 8000;

	private final URIish cloneUri;

	private final UserPasswordCredentials credentials;

	/**
	 * @param cloneUri
	 *            the URI that was used to clone the repository
	 */
	public ConfigureUpstreamRemoteAfterCloneTask(URIish cloneUri) {
		this(cloneUri, null);
	}

	/**
	 * @param cloneUri
	 *            the URI that was used to clone the repository
	 * @param credentials
	 *            credentials collected for the clone operation, if any
	 */
	public ConfigureUpstreamRemoteAfterCloneTask(URIish cloneUri,
			UserPasswordCredentials credentials) {
		this.cloneUri = cloneUri;
		this.credentials = credentials;
	}

	@Override
	public void execute(Repository repository, IProgressMonitor monitor)
			throws CoreException {
		String upstreamUrl = detectUpstreamUrl(repository);
		if (upstreamUrl == null || upstreamUrl.isEmpty()) {
			setForkScenarioDetected(repository, false);
			return;
		}
		try {
			RemoteConfig upstreamRemote = addUpstreamRemote(repository,
					upstreamUrl);
			setForkScenarioDetected(repository, true);
			if (upstreamRemote != null) {
				fetchUpstreamRemote(repository, upstreamRemote, monitor);
			}
		} catch (Exception e) {
			Activator.logError(
					"Failed to configure upstream remote after fork detection", //$NON-NLS-1$
					e);
		}
	}

	private String detectUpstreamUrl(Repository repository) {
		String host = cloneUri.getHost();
		if (host == null) {
			return null;
		}
		try {
			if ("github.com".equalsIgnoreCase(host)) { //$NON-NLS-1$
				return detectGitHubUpstream(repository);
			} else if ("gitlab.com".equalsIgnoreCase(host)) { //$NON-NLS-1$
				return detectGitLabUpstream(repository);
			} else if ("bitbucket.org".equalsIgnoreCase(host)) { //$NON-NLS-1$
				return detectBitbucketUpstream(repository);
			}
		} catch (Exception e) {
			// Best-effort: do not surface errors to the user
			Activator.logWarning(
					"Fork detection skipped for " + host + ": " + e.getMessage(), //$NON-NLS-1$ //$NON-NLS-2$
					e);
		}
		return null;
	}

	private String detectGitHubUpstream(Repository repository)
			throws IOException {
		String path = normalizePath(cloneUri.getPath());
		if (path == null || !path.contains("/")) { //$NON-NLS-1$
			return null;
		}
		String json = httpGet("https://api.github.com/repos/" + path, //$NON-NLS-1$
				repository);
		return parseGitHubForkParentUrl(json, isSshUri());
	}

	/**
	 * Parses the upstream URL from a GitHub repository API JSON response.
	 *
	 * @param json
	 *            the JSON response from the GitHub repos API, or {@code null}
	 * @param ssh
	 *            {@code true} to return the SSH URL, {@code false} for HTTPS
	 * @return the parent repository URL, or {@code null} if not a fork
	 */
	static String parseGitHubForkParentUrl(String json, boolean ssh) {
		if (json == null || !containsTrueBooleanField(json, "fork")) { //$NON-NLS-1$
			return null;
		}
		return extractFromBlock(json, "parent", //$NON-NLS-1$
				ssh ? "ssh_url" : "clone_url"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String detectGitLabUpstream(Repository repository)
			throws IOException {
		String path = normalizePath(cloneUri.getPath());
		if (path == null) {
			return null;
		}
		// GitLab API requires the path to be URL-encoded (/ → %2F)
		String encodedPath = path.replace("/", "%2F"); //$NON-NLS-1$ //$NON-NLS-2$
		String json = httpGet(
				"https://gitlab.com/api/v4/projects/" + encodedPath, //$NON-NLS-1$
				repository);
		return parseGitLabForkParentUrl(json, isSshUri());
	}

	/**
	 * Parses the upstream URL from a GitLab project API JSON response.
	 *
	 * @param json
	 *            the JSON response from the GitLab projects API, or
	 *            {@code null}
	 * @param ssh
	 *            {@code true} to return the SSH URL, {@code false} for HTTPS
	 * @return the parent repository URL, or {@code null} if not a fork
	 */
	static String parseGitLabForkParentUrl(String json, boolean ssh) {
		if (json == null || !json.contains("\"forked_from_project\"")) { //$NON-NLS-1$
			return null;
		}
		String urlKey = ssh ? "ssh_url_to_repo" : "http_url_to_repo"; //$NON-NLS-1$ //$NON-NLS-2$
		return extractFromBlock(json, "forked_from_project", urlKey); //$NON-NLS-1$
	}

	private String detectBitbucketUpstream(Repository repository)
			throws IOException {
		String path = normalizePath(cloneUri.getPath());
		if (path == null) {
			return null;
		}
		String json = httpGet(
				"https://api.bitbucket.org/2.0/repositories/" + path, //$NON-NLS-1$
				repository);
		return parseBitbucketForkParentUrl(json, isSshUri());
	}

	/**
	 * Parses the upstream URL from a Bitbucket repository API JSON response.
	 *
	 * @param json
	 *            the JSON response from the Bitbucket repositories API, or
	 *            {@code null}
	 * @param ssh
	 *            {@code true} to return the SSH URL, {@code false} for HTTPS
	 * @return the parent repository URL, or {@code null} if not a fork
	 */
	static String parseBitbucketForkParentUrl(String json, boolean ssh) {
		if (json == null || !json.contains("\"parent\"")) { //$NON-NLS-1$
			return null;
		}
		int parentIdx = json.indexOf("\"parent\""); //$NON-NLS-1$
		if (parentIdx < 0) {
			return null;
		}
		String parentSubset = json.substring(parentIdx);
		int cloneIdx = parentSubset.indexOf("\"clone\""); //$NON-NLS-1$
		if (cloneIdx < 0) {
			return null;
		}
		String cloneSubset = parentSubset.substring(cloneIdx);
		// Each clone link object has {"href":"...","name":"https"} or {"name":"ssh","href":"..."}
		String protocol = ssh ? "\"ssh\"" : "\"https\""; //$NON-NLS-1$ //$NON-NLS-2$
		int protIdx = cloneSubset.indexOf(protocol);
		if (protIdx < 0) {
			return null;
		}
		// Find the start of the JSON object containing this protocol name so
		// we don't accidentally match an href from a different entry.
		int objStart = cloneSubset.lastIndexOf('{', protIdx);
		if (objStart < 0) {
			return null;
		}
		int objEnd = cloneSubset.indexOf('}', objStart);
		String obj = objEnd >= 0 ? cloneSubset.substring(objStart, objEnd + 1)
				: cloneSubset.substring(objStart);
		Matcher m = Pattern.compile("\"href\"\\s*:\\s*\"([^\"]+)\"") //$NON-NLS-1$
				.matcher(obj);
		return m.find() ? m.group(1) : null;
	}

	private boolean isSshUri() {
		String scheme = cloneUri.getScheme();
		if (scheme != null && scheme.startsWith("ssh")) { //$NON-NLS-1$
			return true;
		}
		// git@github.com:owner/repo.git style (no explicit scheme, user=git)
		return "git".equals(cloneUri.getUser()) && cloneUri.getScheme() == null; //$NON-NLS-1$
	}

	private static String normalizePath(String path) {
		if (path == null) {
			return null;
		}
		return path.replaceAll("^/", "") //$NON-NLS-1$ //$NON-NLS-2$
				.replaceAll("\\.git$", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static boolean containsTrueBooleanField(String json,
			String fieldName) {
		Pattern pattern = Pattern.compile(
				"\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*true"); //$NON-NLS-1$ //$NON-NLS-2$
		Matcher matcher = pattern.matcher(json);
		return matcher.find();
	}

	// Finds "key":"value" inside the first "blockName":{...} block.
	private static String extractFromBlock(String json, String blockName,
			String key) {
		int blockIdx = json.indexOf("\"" + blockName + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (blockIdx < 0) {
			return null;
		}
		return extractJsonString(json.substring(blockIdx), "\"" + key + "\""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static String extractJsonString(String json, String quotedKey) {
		int keyIdx = json.indexOf(quotedKey);
		if (keyIdx < 0) {
			return null;
		}
		int colonIdx = json.indexOf(':', keyIdx + quotedKey.length());
		if (colonIdx < 0) {
			return null;
		}
		int quoteStart = json.indexOf('"', colonIdx + 1);
		if (quoteStart < 0) {
			return null;
		}
		int quoteEnd = json.indexOf('"', quoteStart + 1);
		if (quoteEnd < 0) {
			return null;
		}
		return json.substring(quoteStart + 1, quoteEnd);
	}

	private String httpGet(String urlString, Repository repository)
			throws IOException {
		URL url;
		try {
			url = URI.create(urlString).toURL();
		} catch (IllegalArgumentException e) {
			throw new MalformedURLException(urlString);
		}
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		try {
			conn.setRequestMethod("GET"); //$NON-NLS-1$
			conn.setConnectTimeout(HTTP_TIMEOUT_MS);
			conn.setReadTimeout(HTTP_TIMEOUT_MS);
			conn.setRequestProperty("Accept", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$
			conn.setRequestProperty("User-Agent", "EGit"); //$NON-NLS-1$ //$NON-NLS-2$
			addAuthentication(conn);
			NetUtil.setSslVerification(repository, conn);
			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return null;
			}
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(conn.getInputStream(),
							StandardCharsets.UTF_8))) {
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
				return sb.toString();
			}
		} finally {
			conn.disconnect();
		}
	}

	private void addAuthentication(HttpURLConnection conn) {
		UserPasswordCredentials credentialsToUse = getCredentials();
		if (credentialsToUse == null || credentialsToUse.getUser() == null
				|| credentialsToUse.getPassword() == null) {
			return;
		}
		String credentials = credentialsToUse.getUser() + ':'
				+ credentialsToUse.getPassword();
		String encoded = Base64.getEncoder().encodeToString(
				credentials.getBytes(StandardCharsets.UTF_8));
		conn.setRequestProperty("Authorization", "Basic " + encoded); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private UserPasswordCredentials getCredentials() {
		if (credentials != null) {
			return credentials;
		}
		try {
			return Activator.getDefault().getCredentialsStore()
					.getCredentials(cloneUri);
		} catch (StorageException e) {
			Activator.logError("Failed to read stored clone credentials", e); //$NON-NLS-1$
			return null;
		}
	}

	private static RemoteConfig addUpstreamRemote(Repository repository,
			String upstreamUrl) throws URISyntaxException, IOException {
		StoredConfig config = repository.getConfig();
		if (config.getSubsections("remote").contains(UPSTREAM_REMOTE_NAME)) { //$NON-NLS-1$
			return null;
		}
		URIish upstreamUri = new URIish(upstreamUrl);
		RemoteConfig upstreamRemote = new RemoteConfig(config,
				UPSTREAM_REMOTE_NAME);
		upstreamRemote.addURI(upstreamUri);
		RefSpec fetchSpec = new RefSpec().setForceUpdate(true)
				.setSourceDestination(Constants.R_HEADS + "*", //$NON-NLS-1$
						Constants.R_REMOTES + UPSTREAM_REMOTE_NAME + "/*"); //$NON-NLS-1$
		upstreamRemote.addFetchRefSpec(fetchSpec);
		// Configure the upstream remote for fetching only; no explicit push URI
		// is set here.
		upstreamRemote.update(config);
		config.save();
		return upstreamRemote;
	}

	private static void setForkScenarioDetected(Repository repository,
			boolean detected) {
		try {
			IEclipsePreferences prefs = RepositoryUtil.INSTANCE.getPreferences();
			prefs.putBoolean(RepositoryUtil.INSTANCE
					.getRepositorySpecificPreferenceKey(repository,
							FORK_SCENARIO_PREF_KEY),
					detected);
			prefs.flush();
		} catch (BackingStoreException e) {
			Activator.logError("Failed to persist fork scenario flag", e); //$NON-NLS-1$
		}
	}

	private static void fetchUpstreamRemote(Repository repository,
			RemoteConfig upstreamRemote, IProgressMonitor monitor) {
		FetchOperation fetch = new FetchOperation(repository, upstreamRemote,
				GitSettings.getRemoteConnectionTimeout(), false);
		try {
			fetch.run(monitor);
		} catch (InvocationTargetException e) {
			Activator.logError(
					"Failed to fetch upstream remote after fork detection", //$NON-NLS-1$
					e.getCause() != null ? e.getCause() : e);
		}
	}
}
