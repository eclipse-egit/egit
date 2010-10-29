/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.test;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.Activator;
import org.eclipse.osgi.service.localization.BundleLocalization;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Utilities to be used by SWTBot tests
 */
public class TestUtil {

	public final static String TESTAUTHOR = "Test Author <test.author@test.com>";

	public final static String TESTCOMMITTER = "Test Committer <test.committer@test.com>";

	private final static char AMPERSAND = '&';

	private ResourceBundle myBundle;

	/**
	 * Allows access to the localized values of the EGit UI Plug-in
	 * <p>
	 * This will effectively read the plugin.properties. Ampersands (often used
	 * in menu items and field labels for keyboard shortcuts) will be filtered
	 * out (see also {@link #getPluginLocalizedValue(String, boolean)} in order
	 * to be able to reference these fields using SWTBot).
	 * 
	 * @param key
	 *            the key, must not be null
	 * @return the localized value in the current default {@link Locale}, or
	 *         null
	 * @throws MissingResourceException
	 *             if no value is found for the given key
	 */
	public synchronized String getPluginLocalizedValue(String key)
			throws MissingResourceException {
		return getPluginLocalizedValue(key, false);
	}

	/**
	 * Allows access to the localized values of the EGit UI Plug-in
	 * <p>
	 * 
	 * @param key
	 *            see {@link #getPluginLocalizedValue(String)}
	 * @param keepAmpersands
	 *            if <code>true</code>, ampersands will be kept
	 * @return see {@link #getPluginLocalizedValue(String)}
	 * @throws MissingResourceException
	 *             see {@link #getPluginLocalizedValue(String)}
	 */
	public synchronized String getPluginLocalizedValue(String key,
			boolean keepAmpersands) throws MissingResourceException {
		if (myBundle == null) {
			ServiceTracker localizationTracker;

			BundleContext context = Activator.getDefault().getBundle()
					.getBundleContext();

			localizationTracker = new ServiceTracker(context,
					BundleLocalization.class.getName(), null);
			localizationTracker.open();

			BundleLocalization location = (BundleLocalization) localizationTracker
					.getService();
			if (location != null)
				myBundle = location.getLocalization(Activator.getDefault()
						.getBundle(), Locale.getDefault().toString());
		}

		String raw = myBundle.getString(key);

		if (keepAmpersands || raw.indexOf(AMPERSAND) < 0)
			return raw;

		StringBuilder sb = new StringBuilder(raw.length());
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (c != AMPERSAND)
				sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * Utility for waiting until the execution of jobs of a given
	 * family has finished.
	 * @param family
	 * @throws InterruptedException
	 */
	public static void joinJobs(Object family) throws InterruptedException  {
		Job.getJobManager().join(family, null);
	}
}
