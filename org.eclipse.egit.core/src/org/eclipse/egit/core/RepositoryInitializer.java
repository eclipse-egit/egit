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

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.hosts.GitHosts;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.util.SystemReader;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * An OSGi component triggered when the workspace is ready that sets up the
 * central EGit caches.
 */
@Component
public class RepositoryInitializer {

	private IPreferencesService preferencesService;

	private ServiceRegistration<RepositoryCache> registration;

	private IEclipsePreferences gitCorePreferences;

	private IPreferenceChangeListener prefsListener;

	@Reference
	void setPreferencesService(IPreferencesService service) {
		this.preferencesService = service;
	}

	@Reference
	void setWorkspace(@SuppressWarnings("unused") IWorkspace workspace) {
		// Needed indirectly by the preferences service
	}

	@Activate
	void start() {
		try {
			reconfigureWindowCache(preferencesService);
		} catch (RuntimeException | ExceptionInInitializerError e) {
			Activator.logError(CoreText.Activator_ReconfigureWindowCacheError,
					e);
		}
		gitCorePreferences = InstanceScope.INSTANCE
				.getNode(Activator.PLUGIN_ID);
		GitHosts.loadFromPreferences(gitCorePreferences);
		prefsListener = event -> {
			if (GitCorePreferences.core_gitServers.equals(event.getKey())) {
				GitHosts.loadFromPreferences(gitCorePreferences);
			}
		};
		gitCorePreferences.addPreferenceChangeListener(prefsListener);
		registration = FrameworkUtil.getBundle(getClass()).getBundleContext()
				.registerService(RepositoryCache.class,
						RepositoryCache.INSTANCE, null);
	}

	@Deactivate
	void shutDown() {
		registration.unregister();
		if (gitCorePreferences != null) {
			if (prefsListener != null) {
				gitCorePreferences
						.removePreferenceChangeListener(prefsListener);
				prefsListener = null;
			}
			gitCorePreferences = null;
		}
		RepositoryUtil.INSTANCE.clear();
		IndexDiffCache.INSTANCE.dispose();
		RepositoryCache.INSTANCE.clear();
	}

	/**
	 * Update the settings for the global window cache of the workspace.
	 */
	public static void reconfigureWindowCache() {
		reconfigureWindowCache(Platform.getPreferencesService());
	}

	private static void reconfigureWindowCache(IPreferencesService prefs) {
		WindowCacheConfig c = new WindowCacheConfig();
		c.setPackedGitLimit(prefs.getInt(Activator.PLUGIN_ID,
				GitCorePreferences.core_packedGitLimit, 0, null));
		c.setPackedGitWindowSize(prefs.getInt(Activator.PLUGIN_ID,
				GitCorePreferences.core_packedGitWindowSize, 0, null));
		if (SystemReader.getInstance().isWindows()) {
			c.setPackedGitMMAP(false);
		} else {
			c.setPackedGitMMAP(prefs.getBoolean(Activator.PLUGIN_ID,
					GitCorePreferences.core_packedGitMMAP, false, null));
		}
		c.setDeltaBaseCacheLimit(prefs.getInt(Activator.PLUGIN_ID,
				GitCorePreferences.core_deltaBaseCacheLimit, 0, null));
		c.setStreamFileThreshold(prefs.getInt(Activator.PLUGIN_ID,
				GitCorePreferences.core_streamFileThreshold, 0, null));
		c.setExposeStatsViaJmx(false);
		c.install();
	}
}