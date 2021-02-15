/*******************************************************************************
 * Copyright (C) 2008, 2021 Shawn O. Pearce <spearce@spearce.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.EclipseSystemReader;
import org.eclipse.egit.core.internal.ReportingTypedConfigGetter;
import org.eclipse.egit.core.securestorage.EGitSecureStore;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;

/**
 * The plugin class for the org.eclipse.egit.core plugin. This
 * is a singleton class.
 */
public class Activator extends Plugin {

	/** The plug-in ID for Egit core. */
	public static final String PLUGIN_ID = "org.eclipse.egit.core"; //$NON-NLS-1$

	private static Activator plugin;

	private EGitSecureStore secureStore;

	private Collection<MergeStrategyDescriptor> mergeStrategies;

	/**
	 * @return the singleton {@link Activator}
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Utility to create an error status for this plug-in.
	 *
	 * @param message User comprehensible message
	 * @param thr cause
	 * @return an initialized error status
	 */
	public static IStatus error(final String message, final Throwable thr) {
		return new Status(IStatus.ERROR, PLUGIN_ID, 0, message, thr);
	}

	/**
	 * Utility to create a cancel status for this plug-in.
	 *
	 * @param message
	 *            User comprehensible message
	 * @param thr
	 *            cause
	 * @return an initialized cancel status
	 */
	public static IStatus cancel(final String message, final Throwable thr) {
		return new Status(IStatus.CANCEL, PLUGIN_ID, 0, message, thr);
	}

	/**
	 * Utility method to log errors in the Egit plugin.
	 *
	 * @param message
	 *            User comprehensible message
	 * @param thr
	 *            The exception through which we noticed the error
	 */
	public static void logError(final String message, final Throwable thr) {
		getDefault().getLog().log(error(message, thr));
	}

	/**
	 * Log an info message for this plug-in
	 *
	 * @param message
	 */
	public static void logInfo(final String message) {
		getDefault().getLog().log(
				new Status(IStatus.INFO, PLUGIN_ID, 0, message, null));
	}

	/**
	 * Utility to create a warning status for this plug-in.
	 *
	 * @param message
	 *            User comprehensible message
	 * @param thr
	 *            cause
	 * @return an initialized warning status
	 */
	public static IStatus warning(final String message, final Throwable thr) {
		return new Status(IStatus.WARNING, PLUGIN_ID, 0, message, thr);
	}

	/**
	 * Utility method to log warnings for this plug-in.
	 *
	 * @param message
	 *            User comprehensible message
	 * @param thr
	 *            The exception through which we noticed the warning
	 */
	public static void logWarning(final String message, final Throwable thr) {
		getDefault().getLog().log(warning(message, thr));
	}

	/**
	 * Construct the {@link Activator} singleton instance
	 */
	public Activator() {
		setActivator(this);
	}

	private static void setActivator(Activator a) {
		plugin = a;
	}

	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		FS.FileStoreAttributes.setBackground(true);

		SystemReader.setInstance(
				new EclipseSystemReader(SystemReader.getInstance()));

		Config.setTypedConfigGetter(new ReportingTypedConfigGetter());

		// Set an initial window cache config to suppress loading the JMX bean
		try {
			WindowCacheConfig c = new WindowCacheConfig();
			c.setExposeStatsViaJmx(false);
			c.install();
		} catch (RuntimeException | ExceptionInInitializerError e) {
			logError(CoreText.Activator_ReconfigureWindowCacheError, e);
		}
		secureStore = new EGitSecureStore(SecurePreferencesFactory.getDefault());
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		secureStore = null;
		Config.setTypedConfigGetter(null);
		SystemReader.setInstance(null);
		super.stop(context);
		plugin = null;
	}

	/**
	 * Provides the 3-way merge strategy to use according to the user's
	 * preferences. The preferred merge strategy is JGit's default merge
	 * strategy unless the user has explicitly chosen a different strategy among
	 * the registered strategies.
	 *
	 * @return The MergeStrategy to use, can be {@code null}, in which case the
	 *         default merge strategy should be used as defined by JGit.
	 * @since 4.1
	 */
	public MergeStrategy getPreferredMergeStrategy() {
		String key = Platform.getPreferencesService().getString(PLUGIN_ID,
				GitCorePreferences.core_preferredMergeStrategy, null, null);
		if (!StringUtils.isEmptyOrNull(key)
				&& !GitCorePreferences.core_preferredMergeStrategy_Default
						.equals(key)) {
			MergeStrategy result = MergeStrategy.get(key);
			if (result != null) {
				return result;
			}
			logError(NLS.bind(CoreText.Activator_invalidPreferredMergeStrategy,
					key), null);
		}
		return null;
	}

	void setMergeStrategies(Collection<MergeStrategyDescriptor> strategies) {
		mergeStrategies = strategies;
	}

	/**
	 * @return Provides a read-only view of the registered MergeStrategies
	 *         available.
	 * @since 4.1
	 */
	public Collection<MergeStrategyDescriptor> getRegisteredMergeStrategies() {
		Collection<MergeStrategyDescriptor> strategies = mergeStrategies;
		return strategies == null ? Collections.emptyList() : strategies;
	}

	/**
	 * @return the secure store
	 */
	public EGitSecureStore getSecureStore() {
		return secureStore;
	}

	/**
	 * @return {@code true} if files that get deleted should be automatically
	 *         staged
	 * @since 4.6
	 */
	public static boolean autoStageDeletion() {
		return Platform.getPreferencesService().getBoolean(PLUGIN_ID,
				GitCorePreferences.core_autoStageDeletion, false, null);
	}

	/**
	 * @return {@code true} if files that are moved should be automatically
	 *         staged
	 * @since 4.6
	 */
	public static boolean autoStageMoves() {
		return Platform.getPreferencesService().getBoolean(PLUGIN_ID,
				GitCorePreferences.core_autoStageMoves, false, null);
	}
}
