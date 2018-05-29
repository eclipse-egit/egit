/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.credentials;

import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.internal.SecureStoreUtils;
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
				SecureStoreUtils.storeCredentials(credentials, uri);
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
		UserPasswordCredentials oldCredentials = SecureStoreUtils
				.getCredentials(uri);
		if (oldCredentials != null)
			dialog.setOldUser(oldCredentials.getUser());
		if (dialog.open() == Window.OK) {
			UserPasswordCredentials credentials = dialog.getCredentials();
			if (credentials != null)
				SecureStoreUtils.storeCredentials(credentials, uri);
			return credentials;
		}
		return null;
	}
}
