/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 *
 * @since 4.0
 */
public class Activator extends Plugin {

	private static Plugin instance;

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		instance = this;
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

	/**
	 * @param message
	 * @param throwable
	 * @return Status constructed from parameters.
	 */
	public static IStatus error(String message, Throwable throwable) {
		return new Status(IStatus.ERROR, getPluginId(), 0, message, throwable);
	}

	/**
	 * @param throwable
	 * @return Status constructed from parameters.
	 */
	public static IStatus error(Throwable throwable) {
		return error(throwable.getMessage(), throwable);
	}

	/**
	 * @param message
	 * @return Status constructed from parameters.
	 */
	public static IStatus error(String message) {
		return new Status(IStatus.ERROR, getPluginId(), message);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Plugin getDefault() {
		return instance;
	}

	/**
	 * @return the id of the gitflow plugin
	 */
	public static String getPluginId() {
		return getDefault().getBundle().getSymbolicName();
	}

	/**
	 * Log an info message for this plug-in
	 *
	 * @param message
	 * @since 5.1
	 */
	public static void logInfo(final String message) {
		getDefault().getLog()
				.log(new Status(IStatus.INFO, getPluginId(), 0, message, null));
	}
}
