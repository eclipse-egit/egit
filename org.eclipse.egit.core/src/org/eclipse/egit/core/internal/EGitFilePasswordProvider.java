/*******************************************************************************
 * Copyright (C) 2018, 2024 Thomas Wolf <twolf@apache.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.credentials.UserPasswordCredentials;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.sshd.IdentityPasswordProvider;
import org.eclipse.jgit.util.StringUtils;
import org.osgi.service.prefs.BackingStoreException;

/**
 * An {@link IdentityPasswordProvider} that integrates with the
 * {@link org.eclipse.egit.core.credentials.CredentialsStore}.
 */
public class EGitFilePasswordProvider extends IdentityPasswordProvider {

	private boolean useSecureStore;

	/**
	 * Creates a new instance based on a {@link CredentialsProvider}.
	 *
	 * @param provider
	 *            {@link CredentialsProvider} to use for user interactions
	 */
	public EGitFilePasswordProvider(CredentialsProvider provider) {
		super(provider);
	}

	@Override
	protected char[] getPassword(URIish uri, int attempt,
			@NonNull State state) throws IOException {
		if (attempt == 0) {
			useSecureStore = Platform.getPreferencesService().getBoolean(
					Activator.PLUGIN_ID,
					GitCorePreferences.core_saveCredentialsInSecureStore,
					true, null);
			// Obtain a password from secure store and return it if
			// successful
			if (useSecureStore) {
				try {
					UserPasswordCredentials credentials = Activator
							.getDefault().getCredentialsStore()
							.getCredentials(uri);
					if (credentials != null) {
						String password = credentials.getPassword();
						if (password != null) {
							char[] pass = password.toCharArray();
							state.setPassword(pass);
							// Don't increment the count; this attempt shall
							// not count against the limit, and we rely on
							// count still being zero when we handle the
							// result.
							return pass;
						}
					}
				} catch (StorageException e) {
					if (e.getErrorCode() == StorageException.NO_PASSWORD) {
						// User canceled dialog: don't try to use the secure
						// storage anymore
						useSecureStore = false;
						savePrefs();
					} else {
						Activator.logError(e.getMessage(), e);
					}
				} catch (RuntimeException e) {
					Activator.logError(e.getMessage(), e);
				}
			}
		}
		return super.getPassword(uri, attempt, state);
	}

	@Override
	protected char[] getPassword(URIish uri, String message) {
		CredentialsProvider provider = getCredentialsProvider();
		if (provider == null) {
			return null;
		}
		boolean haveMessage = !StringUtils.isEmptyOrNull(message);
		List<CredentialItem> items = new ArrayList<>(haveMessage ? 3 : 2);
		if (haveMessage) {
			items.add(new CredentialItem.InformationalMessage(message));
		}
		CredentialItem.Password password = new CredentialItem.Password(
				CoreText.EGitSshdSessionFactory_sshKeyEncryptedPrompt);
		items.add(password);
		CredentialItem.YesNoType storeValue = new CredentialItem.YesNoType(
				CoreText.EGitSshdSessionFactory_sshKeyPassphraseStorePrompt);
		storeValue.setValue(useSecureStore);
		items.add(storeValue);
		try {
			if (!provider.get(uri, items)) {
				cancelAuthentication();
			}
			boolean shouldStore = storeValue.getValue();
			if (useSecureStore != shouldStore) {
				useSecureStore = shouldStore;
				savePrefs();
			}
			char[] pass = password.getValue();
			return pass == null ? null : pass.clone();
		} finally {
			password.clear();
		}
	}

	@Override
	protected boolean keyLoaded(URIish uri, State state, char[] password,
			Exception err)
			throws IOException, GeneralSecurityException {
		if (state != null && password != null) {
			if (state.getCount() == 0) {
				// We tried the secure store.
				if (err != null) {
					// Clear the secure store entry for this resource -- it
					// didn't work. On the next round we'll not find a
					// password in the secure store, increment the count,
					// and go through the CredentialsProvider.
					try {
						Activator.getDefault().getCredentialsStore()
								.clearCredentials(uri);
					} catch (IOException | RuntimeException e) {
						Activator.logError(e.getMessage(), e);
					}
					return true; // Re-try
				}
			} else if (err == null) {
				if (useSecureStore) {
					// A user-entered password worked: store it in the
					// secure store. We need a dummy user name to go with
					// it.
					UserPasswordCredentials credentials = new UserPasswordCredentials(
							"egit:ssh:resource", new String(password)); //$NON-NLS-1$
					try {
						Activator.getDefault().getCredentialsStore()
								.putCredentials(uri, credentials);
					} catch (StorageException e) {
						if (e.getErrorCode() == StorageException.NO_PASSWORD) {
							// User canceled dialog: don't try to use the
							// secure storage anymore
							useSecureStore = false;
							savePrefs();
						} else {
							Activator.logError(e.getMessage(), e);
						}
					} catch (RuntimeException e) {
						Activator.logError(e.getMessage(), e);
					}
				}
			}
		}
		return super.keyLoaded(uri, state, password, err);
	}

	private void savePrefs() {
		IEclipsePreferences prefs = InstanceScope.INSTANCE
				.getNode(Activator.PLUGIN_ID);
		prefs.putBoolean(
				GitCorePreferences.core_saveCredentialsInSecureStore,
				useSecureStore);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			Activator.logError(
					CoreText.EGitSshdSessionFactory_savingPreferencesFailed,
					e);
		}
	}
}