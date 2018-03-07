/*******************************************************************************
 * Copyright (C) 2015, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2015, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.NetUtil;
import org.eclipse.egit.core.internal.gerrit.GerritUtil;
import org.eclipse.egit.core.op.CloneOperation.PostCloneTask;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteSession;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.io.MessageWriter;
import org.eclipse.jgit.util.io.StreamCopyThread;

/**
 * Configure Gerrit if repository was cloned from a Gerrit server
 */
public class ConfigureGerritAfterCloneTask implements PostCloneTask {

	private static final String GIT_ECLIPSE_ORG = "git.eclipse.org"; //$NON-NLS-1$

	private static final String GERRIT_CONTEXT_ROOT = "/r/"; //$NON-NLS-1$

	private static final String HTTP = "http"; //$NON-NLS-1$

	private static final String HTTPS = "https"; //$NON-NLS-1$

	private static final String SSH = "ssh"; //$NON-NLS-1$

	private static final String GERRIT_CONFIG_SERVER_VERSION_API = "/config/server/version"; //$NON-NLS-1$

	private static final int GERRIT_SSHD_DEFAULT_PORT = 29418;

	private static final String GERRIT_SSHD_VERSION_API = "gerrit version"; //$NON-NLS-1$

	/**
	 * Pattern to match the sshd reply[1] against to determine whether it's a
	 * Gerrit.
	 * <p>
	 * [1]<a href=
	 * "https://gerrit-documentation.storage.googleapis.com/Documentation/2.11/cmd-version.html">
	 * Gerrit 2.11 gerrit version ssh command</a>
	 * </p>
	 * <p>
	 * We match the whole reply from Gerrit's sshd (as opposed to a prefix match
	 * for "gerrit version") just in case a non-Gerrit has the great idea to
	 * return an error message like "gerrit version: unknown command" or some
	 * such on its stdout...
	 * </p>
	 */
	private static final Pattern GERRIT_SSHD_REPLY = Pattern
			.compile(GERRIT_SSHD_VERSION_API
					+ "\\s+(?:\\d+(?:\\.\\d+)+|.+-\\d+-g[0-9a-fA-F]{7,})"); //$NON-NLS-1$

	/**
	 * To prevent against Cross Site Script Inclusion (XSSI) attacks, the Gerrit
	 * JSON response body starts with a magic prefix line we can use as a second
	 * marker beyond the get version endpoint [1] to detect a Gerrit server
	 * <p/>
	 * [1] <a href=
	 * "https://gerrit-documentation.storage.googleapis.com/Documentation/2.11
	 * /rest-api-config.html#get-version">Gerrit 2.11 Get Version REST
	 * endpoint</a>
	 */
	private static final String GERRIT_XSSI_MAGIC_STRING = ")]}\'\n"; //$NON-NLS-1$

	private final String uri;

	private final String remoteName;

	private final CredentialsProvider credentialsProvider;

	private int timeout;

	/**
	 * @param uri
	 *            not null
	 * @param remoteName
	 *            not null
	 * @param credentialsProvider
	 *            {@link CredentialsProvider} to use for remote communication;
	 *            if {@code null} auto-configuration for repositories cloned
	 *            over ssh will work only for git.eclipse.org
	 * @param timeout
	 *            timeout for remote communication in seconds
	 */
	public ConfigureGerritAfterCloneTask(String uri, String remoteName,
			CredentialsProvider credentialsProvider, int timeout) {
		this.uri = uri;
		this.remoteName = remoteName;
		this.timeout = timeout;
		this.credentialsProvider = credentialsProvider;
	}

	@Override
	public void execute(Repository repository, IProgressMonitor monitor)
			throws CoreException {
		try {
			if (isGerrit(repository)) {
				Activator.logInfo(uri
						+ " was detected to be hosted by a Gerrit server"); //$NON-NLS-1$
				configureGerrit(repository);
			}
		} catch (Exception e) {
			throw new CoreException(Activator.error(e.getMessage(), e));
		}
	}

	/**
	 * Try to use Gerrit's "Get Version" REST API endpoint [1] to detect if the
	 * repository is hosted on a Gerrit server.
	 * <p/>
	 * [1] <a href=
	 * "https://gerrit-documentation.storage.googleapis.com/Documentation/2.11
	 * /rest-api-config.html#get-version">Gerrit 2.11 Get Version REST
	 * endpoint</a>
	 *
	 * @param repo
	 *            the repository to be configured
	 *
	 * @return {@code true} if the repository is hosted on a Gerrit server
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	private boolean isGerrit(Repository repo) throws MalformedURLException,
			IOException,
			URISyntaxException {
		URIish u = new URIish(uri);
		final String s = u.getScheme();
		final String host = u.getHost();
		final String path = u.getPath();

		// shortcut for Eclipse Gerrit server
		if (host != null && host.equals(GIT_ECLIPSE_ORG)) {
			if (HTTPS.equals(s) && (u.getPort() == 443 || u.getPort() == -1)
					&& path != null && path.startsWith(GERRIT_CONTEXT_ROOT)) {
				return true;
			} else if (SSH.equals(s)
					&& u.getPort() == GERRIT_SSHD_DEFAULT_PORT) {
				return true;
			}
		}

		if (path != null && (HTTP.equals(s) || HTTPS.equals(s))) {
			String baseURL = u.setPath("/").toString(); //$NON-NLS-1$
			baseURL = baseURL.substring(0, baseURL.length() - 1);
			String tmpPath = ""; //$NON-NLS-1$
			int slash = 1;
			while (true) {
				HttpURLConnection httpConnection = null;
				try {
					httpConnection = (HttpURLConnection) new URL(baseURL
							+ tmpPath + GERRIT_CONFIG_SERVER_VERSION_API)
									.openConnection();
					NetUtil.setSslVerification(repo, httpConnection);
					httpConnection.setRequestMethod("GET"); //$NON-NLS-1$
					httpConnection.setReadTimeout(1000 * timeout);
					int responseCode = httpConnection.getResponseCode();
					switch (responseCode) {
					case HttpURLConnection.HTTP_OK:
						try (InputStream in = httpConnection.getInputStream()) {
							String response = readFully(in, "UTF-8"); //$NON-NLS-1$
							return response
									.startsWith(GERRIT_XSSI_MAGIC_STRING);
						}
					case HttpURLConnection.HTTP_NOT_FOUND:
						if (slash > path.length()) {
							return false;
						}
						slash = path.indexOf('/', slash);
						if (slash == -1) {
							// try the entire path
							slash = path.length();
						}
						tmpPath = path.substring(0, slash);
						slash++;
						break;
					default:
						return false;
					}
				} catch (IOException e) {
					return false;
				} finally {
					if (httpConnection != null) {
						httpConnection.disconnect();
					}
				}
			}
		} else if (SSH.equals(s)) {
			if (u.getPort() < 0) {
				return false;
			}
			URIish sshUri = u.setPath(""); //$NON-NLS-1$
			try {
				String result = runSshCommand(sshUri, credentialsProvider,
						repo.getFS(), GERRIT_SSHD_VERSION_API);
				return result != null
						&& GERRIT_SSHD_REPLY.matcher(result).matches();
			} catch (IOException e) {
				// Something went wrong with the connection or with the command
				// execution. Maybe the server didn't recognize the command. Do
				// the safe thing and claim it wasn't a Gerrit. In the worst
				// case, the user may have to do the Gerrit config setup via
				// the ConfigureGerritWizard.
				return false;
			}
		}
		return false;
	}

	private String readFully(InputStream inputStream, String encoding)
			throws IOException {
		return new String(readFully(inputStream), encoding);
	}

	private byte[] readFully(InputStream inputStream) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length = 0;
		while ((length = inputStream.read(buffer)) != -1) {
			os.write(buffer, 0, length);
		}
		return os.toByteArray();
	}

	private void configureGerrit(Repository repository)
			throws URISyntaxException, IOException {
		StoredConfig config = repository.getConfig();
		RemoteConfig remoteConfig;
		remoteConfig = GerritUtil.findRemoteConfig(config, remoteName);
		if (remoteConfig == null) {
			return;
		}
		GerritUtil.configurePushURI(remoteConfig, new URIish(uri));
		GerritUtil.configurePushRefSpec(remoteConfig, Constants.MASTER);
		GerritUtil.configureFetchNotes(remoteConfig);
		GerritUtil.setCreateChangeId(config);
		remoteConfig.update(config);
		config.save();
	}

	private String runSshCommand(URIish sshUri, CredentialsProvider provider,
			FS fs, String command) throws IOException {
		RemoteSession session = null;
		Process process = null;
		StreamCopyThread errorThread = null;
		try (MessageWriter stderr = new MessageWriter()) {
			session = SshSessionFactory.getInstance().getSession(sshUri,
					provider, fs, 1000 * timeout);
			process = session.exec(command, 0);
			errorThread = new StreamCopyThread(process.getErrorStream(),
					stderr.getRawStream());
			errorThread.start();
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream(),
							Constants.CHARSET))) {
				return reader.readLine();
			}
		} finally {
			if (errorThread != null) {
				try {
					errorThread.halt();
				} catch (InterruptedException e) {
					// Stop waiting and return anyway.
				} finally {
					errorThread = null;
				}
			}
			if (process != null) {
				process.destroy();
			}
			if (session != null) {
				SshSessionFactory.getInstance().releaseSession(session);
			}
		}
	}

}
