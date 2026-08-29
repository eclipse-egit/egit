/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel <lars.vogel@vogella.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.hosts;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProxySelector;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.NetUtil;
import org.eclipse.egit.core.credentials.CredentialsStore;
import org.eclipse.egit.core.credentials.UserPasswordCredentials;
import org.eclipse.egit.core.internal.hosts.GitHosts.ServerType;
import org.eclipse.egit.core.internal.trace.GitTraceLocation;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UserAgent;
import org.eclipse.jgit.util.HttpSupport;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Information about a pull request, obtained best-effort from the REST API of
 * the git host.
 */
public final class PullRequestInfo {

	private static final int TIMEOUT_MILLIS = 5_000;

	private static final int MAX_RESPONSE_BYTES = 1024 * 1024;

	private static final String GITHUB_HOST = "github.com"; //$NON-NLS-1$

	private static final String GITHUB_API = "https://api.github.com"; //$NON-NLS-1$

	private static final Pattern SEGMENT = Pattern
			.compile("[A-Za-z0-9][A-Za-z0-9._-]*"); //$NON-NLS-1$

	private final @NonNull String sourceBranch;

	private PullRequestInfo(@NonNull String sourceBranch) {
		this.sourceBranch = sourceBranch;
	}

	/**
	 * Retrieves the name of the branch the pull request was created from.
	 *
	 * @return the branch name, without any ref prefix
	 */
	public @NonNull String getSourceBranch() {
		return sourceBranch;
	}

	/**
	 * Looks up a pull request on the host the remote URI points to. Any
	 * failure is only traced, so callers get {@code null} and should fall back
	 * to what they know already.
	 *
	 * @param repository
	 *            whose configuration governs the connection
	 * @param server
	 *            type of the host
	 * @param remote
	 *            URI of the remote repository on that host
	 * @param changeNumber
	 *            number of the pull request
	 * @return the information, or {@code null} if it could not be obtained
	 */
	public static @Nullable PullRequestInfo lookup(
			@NonNull Repository repository, @NonNull ServerType server,
			@NonNull URIish remote, long changeNumber) {
		String apiUrl = apiUrl(server, remote, changeNumber);
		if (apiUrl == null) {
			return null;
		}
		HttpURLConnection connection = null;
		try {
			URL url = new URL(apiUrl);
			connection = (HttpURLConnection) url.openConnection(
					HttpSupport.proxyFor(ProxySelector.getDefault(), url));
			NetUtil.setSslVerification(repository, connection);
			connection.setRequestMethod("GET"); //$NON-NLS-1$
			connection.setConnectTimeout(TIMEOUT_MILLIS);
			connection.setReadTimeout(TIMEOUT_MILLIS);
			connection.setRequestProperty(HttpSupport.HDR_ACCEPT,
					"application/json"); //$NON-NLS-1$
			connection.setRequestProperty(HttpSupport.HDR_USER_AGENT,
					UserAgent.get());
			String authorization = authorization(server, remote, url);
			if (authorization != null) {
				connection.setRequestProperty(HttpSupport.HDR_AUTHORIZATION,
						authorization);
			}
			int status = connection.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK) {
				trace(apiUrl + " answered " + status, null); //$NON-NLS-1$
				return null;
			}
			String body;
			try (InputStream in = connection.getInputStream()) {
				byte[] data = in.readNBytes(MAX_RESPONSE_BYTES + 1);
				if (data.length > MAX_RESPONSE_BYTES) {
					trace(apiUrl + " response too large", null); //$NON-NLS-1$
					return null;
				}
				body = new String(data, StandardCharsets.UTF_8);
			}
			return fromJson(server, body);
		} catch (IOException | JsonParseException e) {
			trace(apiUrl + " failed", e); //$NON-NLS-1$
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * Determines the REST API URL for a pull request.
	 *
	 * @param server
	 *            type of the host
	 * @param remote
	 *            URI of the remote repository on that host
	 * @param changeNumber
	 *            number of the pull request
	 * @return the URL, or {@code null} if it cannot be determined
	 */
	static String apiUrl(ServerType server, URIish remote,
			long changeNumber) {
		String host = remote.getHost();
		String path = repositoryPath(remote);
		if (host == null || host.isEmpty() || path == null
				|| changeNumber < 0) {
			return null;
		}
		String[] segments = path.split("/"); //$NON-NLS-1$
		for (String segment : segments) {
			if (!SEGMENT.matcher(segment).matches()) {
				return null;
			}
		}
		String scheme = remote.getScheme();
		boolean isHttp = "http".equals(scheme) || "https".equals(scheme); //$NON-NLS-1$ //$NON-NLS-2$
		String base = (isHttp ? scheme : "https") + "://" + host; //$NON-NLS-1$ //$NON-NLS-2$
		if (isHttp && remote.getPort() > 0) {
			base += ':' + Integer.toString(remote.getPort());
		}
		switch (server) {
		case GITHUB:
			if (segments.length != 2) {
				return null;
			}
			if (host.equalsIgnoreCase(GITHUB_HOST)) {
				base = GITHUB_API;
			} else {
				base += "/api/v3"; //$NON-NLS-1$
			}
			return base + "/repos/" + path + "/pulls/" + changeNumber; //$NON-NLS-1$ //$NON-NLS-2$
		case GITLAB:
			if (segments.length < 2) {
				return null;
			}
			return base + "/api/v4/projects/" //$NON-NLS-1$
					+ URLEncoder.encode(path, StandardCharsets.UTF_8)
					+ "/merge_requests/" + changeNumber; //$NON-NLS-1$
		case GITEA:
			if (segments.length != 2) {
				return null;
			}
			return base + "/api/v1/repos/" + path + "/pulls/" + changeNumber; //$NON-NLS-1$ //$NON-NLS-2$
		default:
			return null;
		}
	}

	/**
	 * Extracts the pull request information from an API response.
	 *
	 * @param server
	 *            type of the host that answered
	 * @param json
	 *            response body
	 * @return the information, or {@code null} if the response has no source
	 *         branch
	 * @throws JsonParseException
	 *             if {@code json} is not valid JSON
	 */
	static PullRequestInfo fromJson(ServerType server, String json)
			throws JsonParseException {
		JsonElement parsed = JsonParser.parseString(json);
		String branch = server == ServerType.GITLAB
				? getString(parsed, "source_branch") //$NON-NLS-1$
				: getString(parsed, "head", "ref"); //$NON-NLS-1$ //$NON-NLS-2$
		if (branch == null) {
			return null;
		}
		// Gitea reports refs/pull/N/head once the source branch is gone
		if (branch.startsWith(Constants.R_HEADS)) {
			branch = branch.substring(Constants.R_HEADS.length());
		} else if (branch.startsWith(Constants.R_REFS)) {
			return null;
		}
		if (branch.isEmpty()) {
			return null;
		}
		return new PullRequestInfo(branch);
	}

	private static String getString(JsonElement json, String... path) {
		JsonElement current = json;
		for (String key : path) {
			if (current == null || !current.isJsonObject()) {
				return null;
			}
			current = ((JsonObject) current).get(key);
		}
		if (current != null && current.isJsonPrimitive()
				&& current.getAsJsonPrimitive().isString()) {
			return current.getAsString();
		}
		return null;
	}

	private static String repositoryPath(URIish remote) {
		String path = remote.getPath();
		if (path == null) {
			return null;
		}
		int start = 0;
		while (start < path.length() && path.charAt(start) == '/') {
			start++;
		}
		int end = path.length();
		while (end > start && path.charAt(end - 1) == '/') {
			end--;
		}
		path = path.substring(start, end);
		if (path.endsWith(".git")) { //$NON-NLS-1$
			path = path.substring(0, path.length() - 4);
		}
		return path.isEmpty() ? null : path;
	}

	// Re-uses credentials stored for an http(s) remote, but only for the API
	// of the same host (or api.github.com for github.com).
	private static String authorization(ServerType server, URIish remote,
			URL apiUrl) {
		String scheme = remote.getScheme();
		if (!"http".equals(scheme) && !"https".equals(scheme)) { //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
		String remoteHost = remote.getHost();
		String apiHost = apiUrl.getHost();
		boolean sameService = apiHost.equalsIgnoreCase(remoteHost)
				|| (GITHUB_HOST.equalsIgnoreCase(remoteHost)
						&& apiHost.equalsIgnoreCase("api.github.com")); //$NON-NLS-1$
		if (!sameService) {
			return null;
		}
		CredentialsStore store = Activator.getDefault().getCredentialsStore();
		if (store == null) {
			return null;
		}
		UserPasswordCredentials credentials;
		try {
			credentials = store.getCredentials(remote);
		} catch (StorageException e) {
			trace("Cannot read credentials for " + remote, e); //$NON-NLS-1$
			return null;
		}
		if (credentials == null || credentials.getPassword() == null
				|| credentials.getPassword().isEmpty()) {
			return null;
		}
		if (server == ServerType.GITLAB) {
			return "Bearer " + credentials.getPassword(); //$NON-NLS-1$
		}
		String user = credentials.getUser() == null ? "" //$NON-NLS-1$
				: credentials.getUser();
		String pair = user + ':' + credentials.getPassword();
		return "Basic " + Base64.getEncoder().encodeToString( //$NON-NLS-1$
				pair.getBytes(StandardCharsets.UTF_8));
	}

	private static void trace(String message, Throwable error) {
		if (GitTraceLocation.HOSTS.isActive()) {
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.HOSTS.getLocation(), message, error);
		}
	}
}
