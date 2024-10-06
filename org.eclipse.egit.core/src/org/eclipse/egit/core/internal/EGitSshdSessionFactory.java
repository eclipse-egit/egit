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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.sshd.KeyPasswordProvider;
import org.eclipse.jgit.transport.sshd.ProxyData;
import org.eclipse.jgit.transport.sshd.ProxyDataFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.agent.Connector;
import org.eclipse.jgit.transport.sshd.agent.ConnectorFactory;
import org.eclipse.jgit.util.StringUtils;

/**
 * A bridge between the Eclipse ssh2 configuration (which originally was done
 * for JSch) and JGit's Apache MINA {@link SshdSessionFactory}.
 */
public class EGitSshdSessionFactory extends SshdSessionFactory {

	/**
	 * Creates a new {@link EGitSshdSessionFactory}. It doesn't use a key cache,
	 * and a proxy database based on the {@link IProxyService}.
	 *
	 * @param service
	 *            the {@link IProxyService} to use
	 */
	public EGitSshdSessionFactory(IProxyService service) {
		super(null, new EGitProxyDataFactory(service));
		SshPreferencesMirror.INSTANCE.start();
	}

	@Override
	public void close() {
		SshPreferencesMirror.INSTANCE.stop();
		super.close();
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
	protected ConnectorFactory getConnectorFactory() {
		if (Platform.getPreferencesService().getBoolean(Activator.PLUGIN_ID,
				GitCorePreferences.core_sshAgent, true, null)) {
			ConnectorFactory factory = super.getConnectorFactory();
			if (factory != null) {
				return new WrappedSshAgentConnectorFactory(factory);
			}
		}
		return null;
	}

	@Override
	protected KeyPasswordProvider createKeyPasswordProvider(
			CredentialsProvider provider) {
		return new EGitFilePasswordProvider(provider);
	}

	private static class EGitProxyDataFactory implements ProxyDataFactory {

		private final IProxyService proxyService;

		public EGitProxyDataFactory(IProxyService service) {
			proxyService = service;
		}

		@Override
		public ProxyData get(InetSocketAddress remoteAddress) {
			try {
				IProxyData[] data = proxyService
						.select(new URI(IProxyData.SOCKS_PROXY_TYPE,
						"//" + remoteAddress.getHostString(), null)); //$NON-NLS-1$
				if (data == null || data.length == 0) {
					data = proxyService.select(new URI(
							IProxyData.HTTP_PROXY_TYPE,
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

	private static class WrappedSshAgentConnectorFactory
			implements ConnectorFactory {

		private static final AtomicBoolean WARNED = new AtomicBoolean();

		private final ConnectorFactory delegate;

		WrappedSshAgentConnectorFactory(@NonNull ConnectorFactory realFactory) {
			delegate = realFactory;
		}

		@Override
		public Connector create(String identityAgent, File homeDir)
				throws IOException {
			String agentConnection = identityAgent;
			if (StringUtils.isEmptyOrNull(identityAgent)) {
				String preference = Platform.getPreferencesService().getString(
						Activator.PLUGIN_ID, GitCorePreferences.core_sshDefaultAgent,
						null, null);
				if (preference != null) {
					if (getSupportedConnectors().stream().anyMatch(d -> preference.equals(d.getIdentityAgent()))) {
						agentConnection = preference;
					} else if (!WARNED.getAndSet(true)) {
						Activator.logWarning(MessageFormat.format(
								CoreText.EGitSshdSessionFactory_sshUnknownAgentWarning,
								preference), null);
					}
				}
			}
			return delegate.create(agentConnection, homeDir);
		}

		@Override
		public boolean isSupported() {
			return delegate.isSupported();
		}

		@Override
		public String getName() {
			return delegate.getName();
		}

		@Override
		public Collection<ConnectorDescriptor> getSupportedConnectors() {
			return delegate.getSupportedConnectors();
		}

		@Override
		public ConnectorDescriptor getDefaultConnector() {
			return delegate.getDefaultConnector();
		}
	}

}
