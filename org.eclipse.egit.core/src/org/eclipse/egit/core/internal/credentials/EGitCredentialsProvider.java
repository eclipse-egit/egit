/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Edwin Kempin <edwin.kempin@sap.com>
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.credentials;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.function.Function;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.credentials.CredentialsUI;
import org.eclipse.egit.core.credentials.UserPasswordCredentials;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * This class implements a {@link CredentialsProvider} for EGit. The provider
 * tries to retrieve the credentials (user, password) for a given URI from the
 * secure store. If no credentials are available, it uses the highest ranking
 * available {@link CredentialsUI} OSGi service to obtain them. If no such
 * service is available, and error is logged. EGit UI provides a default
 * implementation with the default service ranking.
 */
public class EGitCredentialsProvider extends CredentialsProvider {

	private String user;
	private String password;

	/**
	 * Default constructor.
	 */
	public EGitCredentialsProvider() {
		// empty
	}

	/**
	 * Creates an EGitCredentialsProvider with pre-filled user name and
	 * password.
	 *
	 * @param user
	 *            user name to use
	 * @param password
	 *            password to use
	 */
	public EGitCredentialsProvider(String user, String password) {
		this.user = user;
		// If the password is empty try secure store or ask the user
		this.password = password != null && password.isEmpty() ? null
				: password;
	}

	@Override
	public boolean isInteractive() {
		return true;
	}

	@Override
	public boolean supports(CredentialItem... items) {
		for (CredentialItem i : items) {
			if (i instanceof CredentialItem.StringType) {
				continue;
			} else if (i instanceof CredentialItem.CharArrayType) {
				continue;
			} else if (i instanceof CredentialItem.YesNoType) {
				continue;
			} else if (i instanceof CredentialItem.InformationalMessage) {
				continue;
			}
			return false;
		}
		return true;
	}

	@Override
	public boolean get(URIish uri, CredentialItem... items)
			throws UnsupportedCredentialItem {
		if (items.length == 0) {
			return true;
		}

		CredentialItem.Username userItem = null;
		CredentialItem.Password passwordItem = null;
		boolean isSpecial = false;

		for (CredentialItem item : items) {
			if (item instanceof CredentialItem.Username) {
				userItem = (CredentialItem.Username) item;
			} else if (item instanceof CredentialItem.Password) {
				passwordItem = (CredentialItem.Password) item;
			} else {
				isSpecial = true;
			}
		}

		if (!isSpecial && (userItem != null || passwordItem != null)) {
			UserPasswordCredentials credentials = null;
			if ((user != null) && (password != null)) {
				credentials = new UserPasswordCredentials(user, password);
			} else {
				try {
					credentials = Activator.getDefault().getCredentialsStore()
							.getCredentials(uri);
				} catch (StorageException e) {
					Activator.logError(MessageFormat.format(
							CoreText.EGitCredentialsProvider_errorReadingCredentials,
							uri), e);
					clearCredentials(uri);
				}
			}
			if (credentials == null) {
				credentials = callCredentialsUI(ui -> ui.getCredentials(uri));
				if (credentials == null) {
					return false;
				}
			}
			if (userItem != null) {
				userItem.setValue(credentials.getUser());
			}
			if (passwordItem != null) {
				passwordItem.setValue(credentials.getPassword().toCharArray());
			}
			return true;
		}

		// Special handling for non-user,non-password type items
		return callCredentialsUI(
				ui -> Boolean.valueOf(ui.fillCredentials(uri, items)))
						.booleanValue();
	}

	@Override
	public void reset(URIish uri) {
		clearCredentials(uri);
		user = null;
		password = null;
	}

	private void clearCredentials(URIish uri) {
		try {
			Activator.getDefault().getCredentialsStore().clearCredentials(uri);
		} catch (IOException e) {
			Activator.logError(MessageFormat.format(
					CoreText.EGitCredentialsProvider_errorClearingCredentials,
					uri), e);
		}
	}

	private <T> T callCredentialsUI(
			Function<CredentialsUI, ? extends T> getter) {
		Bundle bundle = Activator.getDefault().getBundle();
		BundleContext context = bundle.getBundleContext();
		ServiceReference<CredentialsUI> reference = context
				.getServiceReference(CredentialsUI.class);
		CredentialsUI ui = context.getService(reference);
		if (ui == null) {
			Activator.logError(
					CoreText.EGitCredentialsProvider_noCredentialsProviderUI,
					null);
		} else {
			try {
				return getter.apply(ui);
			} catch (Exception e) {
				Activator.logError(MessageFormat.format(
						CoreText.EGitCredentialsProvider_credentialsProviderUIfailed,
						ui.getClass().getName()), e);
			} finally {
				try {
					context.ungetService(reference);
				} catch (IllegalStateException e) {
					// Ignore; just trying to clean up, but context is not valid
					// anymore.
				}
			}
		}
		return null;
	}
}
