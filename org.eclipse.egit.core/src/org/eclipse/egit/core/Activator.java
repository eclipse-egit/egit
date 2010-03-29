/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core;

import java.util.Hashtable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.internal.trace.GitTraceLocation;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.framework.BundleContext;

/**
 * The plugin class for the org.eclipse.egit.core plugin. This
 * is a singleton class.
 */
public class Activator extends Plugin implements DebugOptionsListener {
	private static Activator plugin;

	/**
	 * @return the singleton {@link Activator}
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * @return the name of this plugin
	 */
	public static String getPluginId() {
		return getDefault().getBundle().getSymbolicName();
	}

	/**
	 * Utility method to help throwing errors in the Egit plugin. This method
	 * does not actually throw the exception, but just creates an instance.
	 *
	 * @param message User comprehensible message
	 * @param thr cause
	 * @return an Initialized {@link CoreException}
	 */
	public static CoreException error(final String message, final Throwable thr) {
		return new CoreException(new Status(IStatus.ERROR, getPluginId(), 0,
				message, thr));
	}

	/**
	 * Utility method to log errors in the Egit plugin.
	 * @param message User comprehensible message
	 * @param thr The exception through which we noticed the error
	 */
	public static void logError(final String message, final Throwable thr) {
		getDefault().getLog().log(
				new Status(IStatus.ERROR, getPluginId(), 0, message, thr));
	}

	/**
	 * Construct the {@link Activator} singleton instance
	 */
	public Activator() {
		plugin = this;
	}

	public void start(final BundleContext context) throws Exception {
		super.start(context);
        // register this as DebugOptions listener
		Hashtable<String, String> props = new Hashtable<String, String>(4);
		// we want to get notified about our own DebugOptions
		props.put(DebugOptions.LISTENER_SYMBOLICNAME, context.getBundle().getSymbolicName());
		context.registerService(DebugOptionsListener.class.getName(), this, props);

		GitProjectData.reconfigureWindowCache();
		GitProjectData.attachToWorkspace(true);
	}

	public void stop(final BundleContext context) throws Exception {
		GitProjectData.detachFromWorkspace();
		super.stop(context);
		plugin = null;
	}

	public void optionsChanged(DebugOptions options) {
		GitTraceLocation.initializeFromOptions(options, isDebugging());
	}
}
