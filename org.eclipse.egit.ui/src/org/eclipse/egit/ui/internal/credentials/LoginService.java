/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.credentials;

import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.UIText;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.widgets.Shell;

/**
 * This class implements services for interactive login and changing stored
 * credentials.
 */
public class LoginService {

	/**
	 * The method shows a login dialog for a given URI. The user field is taken
	 * from the URI if a user is present in the URI. In this case the user is
	 * not editable.
	 *
	 * @param parent
	 * @param uri
	 * @return credentials, <code>null</code> if the user canceled the dialog.
	 */
	public static UserPasswordCredentials login(Shell parent, URIish uri) {
		LoginDialog dialog = new LoginDialog(parent, uri);
		if (dialog.open() == Window.OK) {
			UserPasswordCredentials credentials = dialog.getCredentials();
			if (credentials != null && dialog.getStoreInSecureStore())
				storeCredentials(uri, credentials);
			return credentials;
		}
		return null;
	}

	/**
	 * The method shows a change credentials dialog for a given URI. The user
	 * field is taken from the URI if a user is present in the URI. In this case
	 * the user is not editable.
	 *
	 * @param parent
	 * @param uri
	 * @return credentials, <code>null</code> if the user canceled the dialog.
	 */
	public static UserPasswordCredentials changeCredentials(Shell parent,
			URIish uri) {
		LoginDialog dialog = new LoginDialog(parent, uri);
		dialog.setChangeCredentials(true);
		UserPasswordCredentials oldCredentials = getCredentialsFromSecureStore(uri);
		if (oldCredentials != null)
			dialog.setOldUser(oldCredentials.getUser());
		if (dialog.open() == Window.OK) {
			UserPasswordCredentials credentials = dialog.getCredentials();
			if (credentials != null)
				storeCredentials(uri, credentials);
			return credentials;
		}
		return null;
	}

	private static void storeCredentials(URIish uri,
			UserPasswordCredentials credentials) {
		try {
			org.eclipse.egit.core.Activator.getDefault().getSecureStore()
					.putCredentials(uri, credentials);
		} catch (StorageException e) {
			Activator.handleError(UIText.LoginService_storingCredentialsFailed, e, true);
		} catch (IOException e) {
			Activator.handleError(UIText.LoginService_storingCredentialsFailed, e, true);
		}
	}

	private static UserPasswordCredentials getCredentialsFromSecureStore(final URIish uri) {
		UserPasswordCredentials credentials = null;
		try {
			credentials = org.eclipse.egit.core.Activator.getDefault().getSecureStore()
					.getCredentials(uri);
		} catch (StorageException e) {
			Activator.logError(
					UIText.LoginService_readingCredentialsFailed, e);
		}
		return credentials;
	}

}
