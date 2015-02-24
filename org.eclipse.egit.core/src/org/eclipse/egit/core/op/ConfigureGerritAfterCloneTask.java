/*******************************************************************************
 * Copyright (C) 2015, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.gerrit.GerritUtil;
import org.eclipse.egit.core.op.CloneOperation.PostCloneTask;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

/**
 * Configure Gerrit if repository was cloned from a Gerrit server
 */
public class ConfigureGerritAfterCloneTask implements PostCloneTask {

	private static final String ECLIPSE_GERRIT_SERVER = "https://git.eclipse.org/r/"; //$NON-NLS-1$

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

	private String uri;

	private String remoteName;

	/**
	 * @param uri
	 * @param remoteName
	 */
	public ConfigureGerritAfterCloneTask(String uri, String remoteName) {
		this.uri = uri;
		this.remoteName = remoteName;
	}

	public void execute(Repository repository, IProgressMonitor monitor)
			throws CoreException {
		try {
			if (isGerrit()) {
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
	 * @return {@code true} if the repository is hosted on a Gerrit server
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	private boolean isGerrit() throws MalformedURLException, IOException,
			URISyntaxException {
		// shortcut for Eclipse Gerrit server
		if (uri.startsWith(ECLIPSE_GERRIT_SERVER)) {
			return true;
		}

		URIish u = new URIish(uri);
		String s = u.getScheme();
		if (s.equals("http") || s.equals("https")) { //$NON-NLS-1$ //$NON-NLS-2$
			String baseURL = uri.substring(0, uri.lastIndexOf(u.getPath()));
			String path = ""; //$NON-NLS-1$
			HttpURLConnection httpConnection = null;
			try {
				while (true) {
					int slash = 1;
					InputStream in = null;
					try {
						httpConnection = (HttpURLConnection) new URL(baseURL
								+ path + "/config/server/version").openConnection(); //$NON-NLS-1$
						httpConnection.setRequestMethod("GET"); //$NON-NLS-1$
						int responseCode = httpConnection.getResponseCode();
						switch (responseCode) {
						case HttpURLConnection.HTTP_OK:
							in = httpConnection.getInputStream();
							String response = readFully(in, "UTF-8"); //$NON-NLS-1$
							if (response.startsWith(GERRIT_XSSI_MAGIC_STRING)) {
								return true;
							} else {
								return false;
							}
						case HttpURLConnection.HTTP_NOT_FOUND:
							String p = u.getPath();
							slash = p.indexOf('/', slash);
							if (slash == p.length()) {
								return false;
							} else if (slash == -1) {
								slash = p.length() - 1;
							}
							path = p.substring(0, slash);
							break;
						default:
							return false;
						}
					} finally {
						if (in != null) {
							in.close();
						}
					}
				}
			} finally {
				if (httpConnection != null) {
					httpConnection.disconnect();
				}
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

}
