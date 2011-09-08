/*******************************************************************************
 * Copyright (c) 2011 Chris Aniszczyk and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class EGitMylynUI extends AbstractUIPlugin {

	/** The Plug-in ID */
	public static final String PLUGIN_ID = "org.eclipse.egit.mylyn.ui"; //$NON-NLS-1$

	// The shared instance
	private static EGitMylynUI plugin;

//	private static ITaskActivationListener listener;
//	private static ITaskActivityManager manager;

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

//		manager = TasksUi.getTaskActivityManager();
//		listener = new TaskActivationListener();
//		manager.addActivationListener(listener);
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * @return the shared instance
	 */
	public static EGitMylynUI getDefault() {
		return plugin;
	}

}
