/*******************************************************************************
 * Copyright (C) 2018, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.securestorage.EGitSecureStore;
import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.sshd.IdentityPasswordProvider;
import org.eclipse.jgit.transport.sshd.KeyPasswordProvider;
import org.eclipse.jgit.transport.sshd.ProxyData;
import org.eclipse.jgit.transport.sshd.ProxyDataFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.util.StringUtils;
import org.osgi.service.prefs.BackingStoreException;

/**
 * A bridge between the Eclipse ssh2 configuration (which originally was done
 * for JSch) and JGit's Apache MINA {@link SshdSessionFactory}.
 */
public class EGitSshdSessionFactory extends SshdSessionFactory {

	/**
	 * Creates a new {@link EGitSshdSessionFactory}. It doesn't use a key
	 * cache, and a proxy database based on the {@link IProxyService}.
	 */
	public EGitSshdSessionFactory() {
		super(null, new EGitProxyDataFactory());
	}

	@Override
	public File getSshDirectory() {
		File file = super.getSshDirectory();
		if (file != null) {
			// Someone explicitly set an ssh directory: use it
			return file;
		}
		return SshPreferencesMirror.INSTANCE.getSshDirectory();
	}

	@Override
	@NonNull
	protected List<Path> getDefaultIdentities(@NonNull File sshDir) {
		List<Path> defaultKeys = SshPreferencesMirror.INSTANCE
				.getDefaultIdentities(sshDir);
		if (defaultKeys == null || defaultKeys.isEmpty()) {
			// None configured, or something configured, but invalid: use
			// default
			return super.getDefaultIdentities(sshDir);
		}
		return defaultKeys;
	}

	@Override
	protected String getDefaultPreferredAuthentications() {
		return SshPreferencesMirror.INSTANCE.getPreferredAuthentications();
	}

	@Override
	protected KeyPasswordProvider createKeyPasswordProvider(
			CredentialsProvider provider) {
		return new EGitFilePasswordProvider(provider,
				Activator.getDefault().getSecureStore());
	}

	private static class EGitProxyDataFactory implements ProxyDataFactory {

		@Override
		public ProxyData get(InetSocketAddress remoteAddress) {
			IProxyService service = Activator.getDefault().getProxyService();
			if (service == null || !service.isProxiesEnabled()) {
				return null;
			}
			try {
				IProxyData[] data = service
						.select(new URI(IProxyData.SOCKS_PROXY_TYPE,
						"//" + remoteAddress.getHostString(), null)); //$NON-NLS-1$
				if (data == null || data.length == 0) {
					data = service.select(new URI(IProxyData.HTTP_PROXY_TYPE,
							"//" + remoteAddress.getHostString(), null)); //$NON-NLS-1$
					if (data == null || data.length == 0) {
						return null;
					}
				}
				return newData(data[0]);
			} catch (URISyntaxException e) {
				return null;
			}
		}

		private ProxyData newData(IProxyData data) {
			if (data == null) {
				return null;
			}
			InetSocketAddress proxyAddress = new InetSocketAddress(
					data.getHost(), data.getPort());
			char[] password = null;
			try {
				password = data.getPassword() == null ? null
						: data.getPassword().toCharArray();
				Proxy proxy;
				switch (data.getType()) {
				case IProxyData.HTTP_PROXY_TYPE:
					proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
					return new ProxyData(proxy, data.getUserId(), password);
				case IProxyData.SOCKS_PROXY_TYPE:
					proxy = new Proxy(Proxy.Type.SOCKS, proxyAddress);
					return new ProxyData(proxy, data.getUserId(), password);
				default:
					return null;
				}
			} finally {
				if (password != null) {
					Arrays.fill(password, '\000');
				}
			}
		}
	}

	private static class EGitFilePasswordProvider
			extends IdentityPasswordProvider {

		private final EGitSecureStore store;

		private boolean useSecureStore;

		public EGitFilePasswordProvider(CredentialsProvider provider,
				EGitSecureStore store) {
			super(provider);
			this.store = store;
		}

		@Override
		protected char[] getPassword(URIish uri, int attempt,
				@NonNull State state) throws IOException {
			if (attempt == 0) {
				useSecureStore = Platform.getPreferencesService().getBoolean(
						Activator.getPluginId(),
						GitCorePreferences.core_saveCredentialsInSecureStore,
						true, null);
				// Obtain a password from secure store and return it if
				// successful
				if (useSecureStore) {
					try {
						UserPasswordCredentials credentials = store
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
			if (store == null) {
				return super.getPassword(uri, message);
			}
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
							store.clearCredentials(uri);
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
							store.putCredentials(uri, credentials);
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
					.getNode(Activator.getPluginId());
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
}
