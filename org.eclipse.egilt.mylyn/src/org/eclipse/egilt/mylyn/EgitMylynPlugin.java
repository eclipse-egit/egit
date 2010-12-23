/*******************************************************************************
 * Copyright (C) 2010, Thorsten Kamann <thorsten@kamann.info>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egilt.mylyn;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * Activator class for the <code>org.eclipse.egit.mylyn</code>-PlugIn. Here you
 * can find common methods you can use in all classes of this plugin.
 */
public class EgitMylynPlugin extends Plugin {
	private static EgitMylynPlugin plugin;

	/**
	 * @return the singleton {@link EgitMylynPlugin}
	 */
	public static EgitMylynPlugin getDefault() {
		if (plugin == null) {
			plugin = new EgitMylynPlugin();
		}
		return plugin;
	}

	/**
	 * @return the name of this plugin
	 */
	public static String getPluginId() {
		return getDefault().getBundle().getSymbolicName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		super.stop(bundleContext);
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
		getDefault().getLog().log(
				new Status(IStatus.ERROR, "egit.mylyn", 0, message, thr));
	}
}
