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
	 *
	 * [1]
	 * https://gerrit-documentation.storage.googleapis.com/Documentation/2.11
	 * /rest-api-config.html#get-version
	 *
	 * @return {@code true} if the repository is hosted on a Gerrit server
	 */
	private boolean isGerrit() {
		// To prevent against Cross Site Script Inclusion (XSSI) attacks, the
		// Gerrit JSON response body starts with a magic prefix line we can use
		// as a second marker beyond the get version endpoint to detect a Gerrit
		// server
		// https://gerrit-documentation.storage.googleapis.com/Documentation/2.11/rest-api.html#output
		final String GERRIT_XSSI_MAGIC_STRING = ")]}\'\n"; //$NON-NLS-1$
		if (uri.startsWith("https://") || uri.startsWith("http://")) { //$NON-NLS-1$ //$NON-NLS-2$
			try {
				String u = uri;
				int protocolEnd = uri.indexOf("://") + 3; //$NON-NLS-1$
				while (true) {
					HttpURLConnection httpConnection = (HttpURLConnection) new URL(
							u + "/config/server/version").openConnection(); //$NON-NLS-1$
					httpConnection.setRequestMethod("GET"); //$NON-NLS-1$
					int responseCode = httpConnection.getResponseCode();
					switch (responseCode) {
					case HttpURLConnection.HTTP_OK:
						InputStream in = httpConnection.getInputStream();
						String response = readFully(in, "UTF-8"); //$NON-NLS-1$
						if (response.startsWith(GERRIT_XSSI_MAGIC_STRING)) {
							return true;
						} else {
							return false;
						}
					case HttpURLConnection.HTTP_NOT_FOUND:
						int slash = u.lastIndexOf('/');
						if (slash <= protocolEnd) {
							return false;
						}
						u = u.substring(0, u.lastIndexOf('/'));
						break;
					default:
						return false;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
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

	private void configureGerrit(Repository repository) {
		StoredConfig config = repository.getConfig();
		RemoteConfig remoteConfig;
		try {
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
		} catch (Exception e) {
			Activator.logError(e.getMessage(), e);
		}
	}

}
