/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui;

import java.io.IOException;
import java.net.Authenticator;
import java.net.ProxySelector;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jsch.core.IJSchService;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.swt.graphics.Font;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.themes.ITheme;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This is a plugin singleton mostly controlling logging.
 */
public class Activator extends AbstractUIPlugin {

	/**
	 *  The one and only instance
	 */
	private static Activator plugin;

	/**
	 * Property listeners for plugin specific events
	 */
	private static List<IPropertyChangeListener> propertyChangeListeners =
		new ArrayList<IPropertyChangeListener>(5);

	/**
	 * Property constant indicating the decorator configuration has changed.
	 */
	public static final String DECORATORS_CHANGED = "org.eclipse.egit.ui.DECORATORS_CHANGED"; //$NON-NLS-1$

	private RepositoryUtil repositoryUtil;

	/**
	 * @return the {@link Activator} singleton.
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * @return the id of the egit ui plugin
	 */
	public static String getPluginId() {
		return getDefault().getBundle().getSymbolicName();
	}

	/**
	 * Handle an error. The error is logged. If <code>show</code> is
	 * <code>true</code> the error is shown to the user.
	 *
	 * @param message 		a localized message
	 * @param throwable
	 * @param show
	 */
	public static void handleError(String message, Throwable throwable,
			boolean show) {
		IStatus status = new Status(IStatus.ERROR, getPluginId(), message,
				throwable);
		int style = StatusManager.LOG;
		if (show)
			style |= StatusManager.SHOW;
		StatusManager.getManager().handle(status, style);
	}

	/**
	 * Shows an error. The error is NOT logged.
	 *
	 * @param message
	 *            a localized message
	 * @param throwable
	 */
	public static void showError(String message, Throwable throwable) {
		IStatus status = new Status(IStatus.ERROR, getPluginId(), message,
				throwable);
		StatusManager.getManager().handle(status, StatusManager.SHOW);
	}

	/**
	 * Get the theme used by this plugin.
	 *
	 * @return our theme.
	 */
	public static ITheme getTheme() {
		return plugin.getWorkbench().getThemeManager().getCurrentTheme();
	}

	/**
	 * Get a font known to this plugin.
	 *
	 * @param id
	 *            one of our THEME_* font preference ids (see
	 *            {@link UIPreferences});
	 * @return the configured font, borrowed from the registry.
	 */
	public static Font getFont(final String id) {
		return getTheme().getFontRegistry().get(id);
	}

	/**
	 * Get a font known to this plugin, but with bold style applied over top.
	 *
	 * @param id
	 *            one of our THEME_* font preference ids (see
	 *            {@link UIPreferences});
	 * @return the configured font, borrowed from the registry.
	 */
	public static Font getBoldFont(final String id) {
		return getTheme().getFontRegistry().getBold(id);
	}

	private RCS rcs;

	/**
	 * Constructor for the egit ui plugin singleton
	 */
	public Activator() {
		plugin = this;
	}

	public void start(final BundleContext context) throws Exception {
		super.start(context);
		repositoryUtil = new RepositoryUtil();

		if (isDebugging()) {
			ServiceTracker debugTracker = new ServiceTracker(context,
					DebugOptions.class.getName(), null);
			debugTracker.open();

			DebugOptions opts = (DebugOptions) debugTracker.getService();
			GitTraceLocation.initializeFromOptions(opts, true);
		}

		setupSSH(context);
		setupProxy(context);
		setupRepoChangeScanner();
	}

	/**
	 * Register for changes made to Team properties.
	 *
	 * @param listener
	 *            The listener to register
	 */
	public static synchronized void addPropertyChangeListener(
			IPropertyChangeListener listener) {
		propertyChangeListeners.add(listener);
	}

	/**
	 * Remove a Team property changes.
	 *
	 * @param listener
	 *            The listener to remove
	 */
	public static synchronized void removePropertyChangeListener(
			IPropertyChangeListener listener) {
		propertyChangeListeners.remove(listener);
	}

	/**
	 * Broadcast a Team property change.
	 *
	 * @param event
	 *            The event to broadcast
	 */
	public static synchronized void broadcastPropertyChange(PropertyChangeEvent event) {
		for (IPropertyChangeListener listener : propertyChangeListeners)
			listener.propertyChange(event);
	}


	static class RCS extends Job {
		RCS() {
			super(UIText.Activator_repoScanJobName);
		}

		// FIXME, need to be more intelligent about this to avoid too much work
		private static final long REPO_SCAN_INTERVAL = 10000L;
		// volatile in order to ensure thread synchronization
		private volatile boolean doReschedule = true;

		void setReschedule(boolean reschedule){
			doReschedule = reschedule;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Repository[] repos = org.eclipse.egit.core.Activator.getDefault()
					.getRepositoryCache().getAllReposiotries();
			if (repos.length == 0)
				return Status.OK_STATUS;
			monitor.beginTask(UIText.Activator_scanningRepositories,
					repos.length);
			try {
				for (Repository repo : repos) {
					if (monitor.isCanceled())
						break;
					// TODO is this the right location?
					if (GitTraceLocation.UI.isActive())
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.UI.getLocation(),
								"Scanning " + repo + " for changes"); //$NON-NLS-1$ //$NON-NLS-2$

					repo.scanForRepoChanges();
					monitor.worked(1);
				}
			} catch (IOException e) {
				// TODO is this the right location?
				if (GitTraceLocation.UI.isActive())
					GitTraceLocation.getTrace().trace(
							GitTraceLocation.UI.getLocation(),
							"Stopped rescheduling " + getName() + "job"); //$NON-NLS-1$ //$NON-NLS-2$
				return createErrorStatus(UIText.Activator_scanError, e);
			} finally {
				monitor.done();
			}
			// TODO is this the right location?
			if (GitTraceLocation.UI.isActive())
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.UI.getLocation(),
						"Rescheduling " + getName() + " job"); //$NON-NLS-1$ //$NON-NLS-2$
			if (doReschedule)
				schedule(REPO_SCAN_INTERVAL);
			return Status.OK_STATUS;
		}
	}

	private void setupRepoChangeScanner() {
		rcs = new RCS();
		rcs.setSystem(true);
		rcs.schedule(RCS.REPO_SCAN_INTERVAL);
	}

	private void setupSSH(final BundleContext context) {
		final ServiceReference ssh;

		ssh = context.getServiceReference(IJSchService.class.getName());
		if (ssh != null) {
			SshSessionFactory.setInstance(new EclipseSshSessionFactory(
					(IJSchService) context.getService(ssh)));
		}
	}

	private void setupProxy(final BundleContext context) {
		final ServiceReference proxy;

		proxy = context.getServiceReference(IProxyService.class.getName());
		if (proxy != null) {
			ProxySelector.setDefault(new EclipseProxySelector(
					(IProxyService) context.getService(proxy)));
			Authenticator.setDefault(new EclipseAuthenticator(
					(IProxyService) context.getService(proxy)));
		}
	}

	public void stop(final BundleContext context) throws Exception {

		if (GitTraceLocation.UI.isActive())
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.UI.getLocation(),
					"Trying to cancel " + rcs.getName() + " job"); //$NON-NLS-1$ //$NON-NLS-2$

		rcs.setReschedule(false);

		rcs.cancel();

		rcs.join();

		if (GitTraceLocation.UI.isActive())
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.UI.getLocation(), "Jobs terminated"); //$NON-NLS-1$

		repositoryUtil.dispose();
		repositoryUtil = null;
		super.stop(context);
		plugin = null;
	}

	/**
	 * @param message
	 * @param e
	 */
	public static void logError(String message, Throwable e) {
		handleError(message, e, false);
	}

	/**
	 * @param message
	 * @param e
	 */
	public static void error(String message, Throwable e) {
		handleError(message, e, false);
	}

	/**
	 * Creates an error status
	 *
	 * @param message
	 *            a localized message
	 * @param throwable
	 * @return a new Status object
	 */
	public static IStatus createErrorStatus(String message, Throwable throwable) {
		return new Status(IStatus.ERROR, getPluginId(), message, throwable);
	}

	/**
	 * @return the {@link RepositoryUtil} instance
	 */
	public RepositoryUtil getRepositoryUtil() {
		return repositoryUtil;
	}
}
