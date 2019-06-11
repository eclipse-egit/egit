/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Edwin Kempin <edwin.kempin@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.securestorage;

import java.io.IOException;

import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.StringUtils;

/**
 * This class wraps the Eclipse secure store. It provides methods to put
 * credentials for a given URI to the secure store and to retrieve credentials
 * for a given URI.
 */
public class EGitSecureStore {

	private static final String USER = "user"; //$NON-NLS-1$

	private static final String PASSWORD = "password"; //$NON-NLS-1$

	private static final String GIT_PATH_PREFIX = "/GIT/"; //$NON-NLS-1$

	private final ISecurePreferences preferences;

	/**
	 * Constructor
	 *
	 * @param preferences
	 *            the Eclipse secure store should be passed here if not in test
	 *            mode
	 */
	public EGitSecureStore(ISecurePreferences preferences) {
		this.preferences = preferences;
	}

	/**
	 * Puts credentials for the given URI into the secure store
	 *
	 * @param uri
	 * @param credentials
	 * @throws StorageException
	 * @throws IOException
	 */
	public void putCredentials(URIish uri, UserPasswordCredentials credentials)
			throws StorageException, IOException {
		String u = credentials.getUser();
		String p = credentials.getPassword();
		if (StringUtils.isEmptyOrNull(u) || StringUtils.isEmptyOrNull(p)) {
			return;
		}
		String pathName = calcNodePath(uri);
		ISecurePreferences node = preferences.node(pathName);
		node.put(USER, u, false);
		node.put(PASSWORD, p, true);
		node.flush();
	}

	/**
	 * Retrieves credentials stored for the given URI from the secure store
	 *
	 * @param uri
	 * @return credentials
	 * @throws StorageException
	 */
	public UserPasswordCredentials getCredentials(URIish uri)
			throws StorageException {
		String pathName = calcNodePath(uri);
		if (!preferences.nodeExists(pathName))
			return null;
		ISecurePreferences node = preferences.node(pathName);
		String user = node.get(USER, ""); //$NON-NLS-1$
		String password = node.get(PASSWORD, ""); //$NON-NLS-1$
		if (uri.getUser() != null && !user.equals(uri.getUser()))
			return null;
		return new UserPasswordCredentials(user, password);
	}

	static String calcNodePath(URIish uri) {
		URIish storedURI = uri.setUser(null).setPass(null);
		if (uri.getScheme() != null && !"file".equals(uri.getScheme())) { //$NON-NLS-1$
			storedURI = storedURI.setPath(null);
			if (uri.getPort() == -1) {
				String s = uri.getScheme();
				if ("http".equals(s)) //$NON-NLS-1$
					storedURI = storedURI.setPort(80);
				else if ("https".equals(s)) //$NON-NLS-1$
					storedURI = storedURI.setPort(443);
				else if ("ssh".equals(s) || "sftp".equals(s)) //$NON-NLS-1$ //$NON-NLS-2$
					storedURI = storedURI.setPort(22);
				else if ("ftp".equals(s)) //$NON-NLS-1$
					storedURI = storedURI.setPort(21);
			}
		}
		String pathName = GIT_PATH_PREFIX
				+ EncodingUtils.encodeSlashes(storedURI.toString());
		return pathName;
	}

	/**
	 * Clear credentials for the given uri.
	 *
	 * @param uri
	 * @throws IOException
	 */
	public void clearCredentials(URIish uri) throws IOException {
		String pathName = calcNodePath(uri);
		if (!preferences.nodeExists(pathName))
			return;
		ISecurePreferences node = preferences.node(pathName);
		node.removeNode();
		preferences.flush();
	}

}
