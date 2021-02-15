/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thomas Wolf -- factored out of Activator
 *******************************************************************************/
package org.eclipse.egit.core;

import java.net.Authenticator;
import java.net.ProxySelector;
import java.text.MessageFormat;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.EGitSshdSessionFactory;
import org.eclipse.jgit.transport.HttpTransport;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.http.JDKHttpConnectionFactory;
import org.eclipse.jgit.transport.http.apache.HttpClientConnectionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * An OSGI service for configuring HTTP and SSH transports.
 */
@Component
public class TransportConfigurator {

	private enum HttpClientType {
		JDK, APACHE
	}

	private IPreferencesService preferencesService;

	private IProxyService proxyService;

	private IPreferenceChangeListener preferenceChangeListener;

	@Reference
	void setPreferencesService(IPreferencesService service) {
		this.preferencesService = service;
	}

	@Reference
	void setProxyService(IProxyService service) {
		this.proxyService = service;
	}

	@Activate
	void start() {
		setupHttp();
		preferenceChangeListener = event -> {
			if (GitCorePreferences.core_httpClient.equals(event.getKey())) {
				setupHttp();
			}
		};
		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID)
				.addPreferenceChangeListener(preferenceChangeListener);
		SshSessionFactory.setInstance(new EGitSshdSessionFactory(proxyService));

		ProxySelector.setDefault(new EclipseProxySelector(proxyService));
		Authenticator.setDefault(new EclipseAuthenticator(proxyService));
	}

	@Deactivate
	void shutDown() {
		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID)
				.removePreferenceChangeListener(preferenceChangeListener);
		SshSessionFactory current = SshSessionFactory.getInstance();
		if (current instanceof SshdSessionFactory) {
			((SshdSessionFactory) current).close();
		}
	}

	private void setupHttp() {
		String httpClient = preferencesService.getString(Activator.PLUGIN_ID,
				GitCorePreferences.core_httpClient,
				HttpClientType.JDK.toString(), null);
		if (HttpClientType.APACHE.name().equalsIgnoreCase(httpClient)) {
			HttpTransport
					.setConnectionFactory(new HttpClientConnectionFactory());
		} else {
			if (!HttpClientType.JDK.name().equalsIgnoreCase(httpClient)) {
				Activator.logWarning(MessageFormat.format(
						CoreText.Activator_HttpClientUnknown, httpClient),
						null);
			}
			HttpTransport.setConnectionFactory(new JDKHttpConnectionFactory());
		}
	}

}
