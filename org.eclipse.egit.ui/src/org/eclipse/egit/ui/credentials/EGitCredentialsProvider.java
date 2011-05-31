/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Edwin Kempin <edwin.kempin@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.credentials;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.UIText;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * This class implements a {@link CredentialsProvider} for EGit. The provider
 * tries to retrieve the credentials (user, password) for a given URI from the
 * secure store. A login popup is shown if no credentials are available.
 */
public class EGitCredentialsProvider extends CredentialsProvider {

	@Override
	public boolean isInteractive() {
		return true;
	}

	@Override
	public boolean supports(CredentialItem... items) {
		for (CredentialItem i : items) {
			if (i instanceof CredentialItem.Username)
				continue;
			else if (i instanceof CredentialItem.Password)
				continue;
			else
				return false;
		}
		return true;
	}

	@Override
	public boolean get(final URIish uri, CredentialItem... items)
			throws UnsupportedCredentialItem {
		CredentialItem.Username userItem = null;
		CredentialItem.Password passwordItem = null;

		for (CredentialItem item : items) {
			if (item instanceof CredentialItem.Username)
				userItem = (CredentialItem.Username) item;
			else if (item instanceof CredentialItem.Password)
				passwordItem = (CredentialItem.Password) item;
			else
				throw new UnsupportedCredentialItem(uri, item.getPromptText());
		}

		UserPasswordCredentials credentials = getCredentialsFromSecureStore(uri);

		if (credentials == null) {
			credentials = getCredentialsFromUser(uri, passwordItem.getPromptText());
			if (credentials == null)
				return false;
		}
		if (userItem != null)
			userItem.setValue(credentials.getUser());
		if (passwordItem != null)
			passwordItem.setValue(credentials.getPassword().toCharArray());
		return true;
	}

	private UserPasswordCredentials getCredentialsFromUser(final URIish uri, final String promptText) {
		final AtomicReference<UserPasswordCredentials> aRef = new AtomicReference<UserPasswordCredentials>(
				null);
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				Shell shell = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell();
				aRef.set(LoginService.login(shell, uri, promptText));
			}
		});
		return aRef.get();
	}

	private UserPasswordCredentials getCredentialsFromSecureStore(final URIish uri) {
		UserPasswordCredentials credentials = null;
		try {
			credentials = Activator.getDefault().getSecureStore()
					.getCredentials(uri);
		} catch (StorageException e) {
			Activator.logError(
					UIText.EGitCredentialsProvider_errorReadingCredentials, e);
		}
		return credentials;
	}

}
