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
package org.eclipse.egit.ui.performance;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.Activator;
import org.eclipse.osgi.service.localization.BundleLocalization;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
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
		if (myBundle != null) {
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
		return null;
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

	/**
	 * Appends content to given file.
	 *
	 * @param file
	 * @param content
	 * @param append
	 *            if true, then bytes will be written to the end of the file
	 *            rather than the beginning
	 * @throws IOException
	 */
	public static void appendFileContent(File file, String content, boolean append)
			throws IOException {
		FileWriter fw = null;
		try {
			fw = new FileWriter(file, append);
			fw.append(content);
		} finally {
			if (fw != null)
				fw.close();
		}
	}

	/**
	 * Waits until the given tree has a node whose label contains text
	 * @param bot
	 * @param tree
	 * @param text
	 * @param timeout
	 * @throws TimeoutException
	 */
	public static void waitUntilTreeHasNodeContainsText(SWTBot bot,
			final SWTBotTree tree, final String text, long timeout)
			throws TimeoutException {
		bot.waitUntil(new ICondition() {

			public boolean test() throws Exception {
				for (SWTBotTreeItem item : tree.getAllItems())
					if (item.getText().contains(text))
						return true;
				return false;
			}

			public void init(SWTBot bot2) {
				// empty
			}

			public String getFailureMessage() {
				return null;
			}
		}, timeout);
	}

	/**
	 * Waits until the given tree item has a node whose label contains text
	 * @param bot
	 * @param treeItem
	 * @param text
	 * @param timeout
	 * @throws TimeoutException
	 */
	public static void waitUntilTreeHasNodeContainsText(SWTBot bot,
			final SWTBotTreeItem treeItem, final String text, long timeout)
			throws TimeoutException {
		bot.waitUntil(new ICondition() {

			public boolean test() throws Exception {
				for (SWTBotTreeItem item : treeItem.getItems())
					if (item.getText().contains(text))
						return true;
				return false;
			}

			public void init(SWTBot bot2) {
				// empty
			}

			public String getFailureMessage() {
				return null;
			}
		}, timeout);
	}

	/**
	 * Waits until the given tree item has a selected node with the given text
	 *
	 * @param bot
	 * @param tree
	 * @param text
	 * @param timeout
	 * @throws TimeoutException
	 */
	public static void waitUntilTreeHasSelectedNodeWithText(SWTBot bot,
			final SWTBotTree tree, final String text, long timeout)
			throws TimeoutException {
		bot.waitUntil(new ICondition() {

			public boolean test() throws Exception {
				return tree.selection().get(0, 0).equals(text);
			}

			public void init(SWTBot bot2) {
				// empty
			}

			public String getFailureMessage() {
				return null;
			}
		}, timeout);
	}

	/**
	 * Waits until the given table has an item with the given text
	 * @param bot
	 * @param table
	 * @param text
	 * @param timeout
	 * @throws TimeoutException
	 */
	public static void waitUntilTableHasRowWithText(SWTBot bot, final SWTBotTable table,
			final String text, long timeout) throws TimeoutException {
		bot.waitUntil(new ICondition() {

			public boolean test() throws Exception {
				if (table.indexOf(text)<0)
					return false;
				return true;
			}

			public void init(SWTBot bot2) {
				// empty
			}

			public String getFailureMessage() {
				return null;
			}
		}, timeout);
	}

	public static void waitUntilEditorIsActive(SWTWorkbenchBot bot,
			final SWTBotEditor editor, long timeout) {
		bot.waitUntil(new ICondition() {

			public boolean test() throws Exception {
				return editor.isActive();
			}

			public void init(SWTBot bot2) {
				// empty
			}

			public String getFailureMessage() {
				return null;
			}
		}, timeout);
	}

	/**
	 * Disables usage of proxy servers
	 */
	public static void disableProxy() {
		BundleContext context = Activator.getDefault().getBundle().getBundleContext();
		ServiceReference serviceReference = context.getServiceReference(IProxyService.class.getName());
		IProxyService proxyService = (IProxyService) context.getService(serviceReference);
		proxyService.setSystemProxiesEnabled(false);
		proxyService.setProxiesEnabled(false);
	}


}
