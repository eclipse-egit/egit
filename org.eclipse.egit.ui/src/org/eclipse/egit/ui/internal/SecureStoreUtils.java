/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Philipp Thun <philipp.thun@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.egit.core.credentials.UserPasswordCredentials;
import org.eclipse.egit.ui.Activator;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.transport.URIish;

/**
 * Utilities for EGit secure store
 */
public class SecureStoreUtils {
	/**
	 * Store credentials for the given uri
	 *
	 * @param credentials
	 * @param uri
	 * @return true if successful
	 */
	public static boolean storeCredentials(UserPasswordCredentials credentials,
			URIish uri) {
		if (credentials != null && uri != null) {
			try {
				org.eclipse.egit.core.Activator.getDefault()
						.getCredentialsStore().putCredentials(uri, credentials);
			} catch (StorageException | IOException e) {
				Activator.handleError(MessageFormat.format(
						UIText.SecureStoreUtils_writingCredentialsFailed, uri),
						e, true);
				return false;
			}
		}
		return true;
	}

	/**
	 * Gets credentials stored for the given uri. Logs {@code StorageException}
	 * if thrown by the secure store implementation and removes credentials
	 * which can't be read from secure store
	 *
	 * @param uri
	 * @return credentials stored in secure store for given uri
	 */
	public static @Nullable UserPasswordCredentials getCredentials(
			final URIish uri) {
		if (uri == null) {
			return null;
		}
		try {
			return org.eclipse.egit.core.Activator.getDefault()
					.getCredentialsStore().getCredentials(uri);
		} catch (StorageException e) {
			Activator.logError(MessageFormat.format(
					UIText.SecureStoreUtils_errorReadingCredentials,
					uri), e);
			clearCredentials(uri);
			return null;
		}
	}

	/**
	 * Clear credentials stored for the given uri if any exist
	 *
	 * @param uri
	 */
	public static void clearCredentials(final URIish uri) {
		if (uri == null) {
			return;
		}
		try {
			org.eclipse.egit.core.Activator.getDefault().getCredentialsStore()
					.clearCredentials(uri);
		} catch (IOException e) {
			Activator.logError(MessageFormat.format(
					UIText.SecureStoreUtils_errorClearingCredentials, uri), e);
		}
	}

}
