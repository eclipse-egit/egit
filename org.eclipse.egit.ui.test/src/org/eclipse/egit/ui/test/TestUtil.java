/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.test;

import static org.eclipse.swtbot.eclipse.finder.waits.Conditions.waitForView;
import static org.eclipse.swtbot.swt.finder.waits.Conditions.widgetIsEnabled;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.commit.CommitHelper;
import org.eclipse.egit.ui.internal.commit.CommitHelper.CommitInfo;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.osgi.service.localization.BundleLocalization;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressConstants;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Utilities to be used by SWTBot tests
 */
public class TestUtil {

	public final static String TESTAUTHOR = "Test Author <test.author@test.com>";

	public final static String TESTCOMMITTER = "Test Committer <test.committer@test.com>";

	public final static String TESTCOMMITTER_NAME = "Test Committer";

	public final static String TESTCOMMITTER_EMAIL = "test.committer@test.com";

	private final static char AMPERSAND = '&';

	private Map<Bundle, ResourceBundle> bundle2ResourceBundle = new HashMap<>();

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
		return getPluginLocalizedValue(key, keepAmpersands, Activator.getDefault().getBundle());
	}

	/**
	 * Allows access to the localized values of the given Bundle
	 * <p>
	 *
	 * @param key
	 *            see {@link #getPluginLocalizedValue(String)}
	 * @param keepAmpersands
	 *            if <code>true</code>, ampersands will be kept
	 * @param bundle
	 *            the Bundle that contains the localization
	 * @return see {@link #getPluginLocalizedValue(String)}
	 * @throws MissingResourceException
	 *             see {@link #getPluginLocalizedValue(String)}
	 */
	public synchronized String getPluginLocalizedValue(String key,
			boolean keepAmpersands, Bundle bundle) throws MissingResourceException {
		ResourceBundle myBundle = bundle2ResourceBundle.get(bundle);
		if (myBundle == null) {
			BundleContext context = bundle.getBundleContext();

			ServiceTracker<BundleLocalization, BundleLocalization> localizationTracker =
					new ServiceTracker<BundleLocalization, BundleLocalization>(
					context, BundleLocalization.class, null);
			localizationTracker.open();

			BundleLocalization location = localizationTracker.getService();
			if (location != null) {
				myBundle = location.getLocalization(bundle, Locale.getDefault().toString());
				bundle2ResourceBundle.put(bundle, myBundle);
			}

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
		// join() returns immediately if the job is not yet scheduled.
		// To avoid unstable tests, let us first wait some time
		TestUtil.waitForJobs(100, 1000);
		Job.getJobManager().join(family, null);
		TestUtil.processUIEvents();
	}

	@SuppressWarnings("restriction")
	public static void waitForDecorations() throws InterruptedException {
		TestUtil.joinJobs(
				org.eclipse.ui.internal.decorators.DecoratorManager.FAMILY_DECORATE);
	}

	/**
	 * Utility for waiting until the execution of jobs of any family has
	 * finished or timeout is reached. If no jobs are running, the method waits
	 * given minimum wait time. While this method is waiting for jobs, UI events
	 * are processed.
	 *
	 * @param minTimeMs
	 *            minimum wait time in milliseconds
	 * @param maxTimeMs
	 *            maximum wait time in milliseconds
	 */
	public static void waitForJobs(long minTimeMs, long maxTimeMs) {
		if (maxTimeMs < minTimeMs) {
			throw new IllegalArgumentException(
					"Max time is smaller as min time!");
		}
		final long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < minTimeMs) {
			processUIEvents();
		}
		while (!Job.getJobManager().isIdle()
				&& System.currentTimeMillis() - start < maxTimeMs) {
			processUIEvents();
		}
	}

	/**
	 * Process all queued UI events. If called from background thread, blocks
	 * until all pending events are processed in UI thread.
	 */
	public static void processUIEvents() {
		processUIEvents(0);
	}

	/**
	 * Process all queued UI events. If called from background thread, blocks
	 * until all pending events are processed in UI thread.
	 *
	 * @param timeInMillis
	 *            time to wait. During this time all UI events are processed but
	 *            the current thread is blocked
	 */
	public static void processUIEvents(final long timeInMillis) {
		if (Display.getCurrent() != null) {
			if (timeInMillis <= 0) {
				while (Display.getCurrent().readAndDispatch()) {
					// process queued ui events at least once
				}
			} else {
				long start = System.currentTimeMillis();
				while (System.currentTimeMillis() - start <= timeInMillis) {
					while (Display.getCurrent().readAndDispatch()) {
						// process queued ui events
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							break;
						}
					}
				}
			}
		} else {
			// synchronously refresh UI
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					processUIEvents();
				}
			});
		}
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
		Writer fw = null;
		try {
			fw = new OutputStreamWriter(new FileOutputStream(file, append),
					"UTF-8");
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

			@Override
			public boolean test() throws Exception {
				for (SWTBotTreeItem item : tree.getAllItems())
					if (item.getText().contains(text))
						return true;
				return false;
			}

			@Override
			public void init(SWTBot bot2) {
				// empty
			}

			@Override
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

			@Override
			public boolean test() throws Exception {
				for (SWTBotTreeItem item : treeItem.getItems())
					if (item.getText().contains(text))
						return true;
				return false;
			}

			@Override
			public void init(SWTBot bot2) {
				// empty
			}

			@Override
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

			@Override
			public boolean test() throws Exception {
				return tree.selection().get(0, 0).equals(text);
			}

			@Override
			public void init(SWTBot bot2) {
				// empty
			}

			@Override
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

			@Override
			public boolean test() throws Exception {
				if (table.indexOf(text)<0)
					return false;
				return true;
			}

			@Override
			public void init(SWTBot bot2) {
				// empty
			}

			@Override
			public String getFailureMessage() {
				return null;
			}
		}, timeout);
	}

	public static void waitUntilEditorIsActive(SWTWorkbenchBot bot,
			final SWTBotEditor editor, long timeout) {
		bot.waitUntil(new ICondition() {

			@Override
			public boolean test() throws Exception {
				return editor.isActive();
			}

			@Override
			public void init(SWTBot bot2) {
				// empty
			}

			@Override
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
		ServiceReference<IProxyService> serviceReference = context.getServiceReference(IProxyService.class);
		IProxyService proxyService = context.getService(serviceReference);
		proxyService.setSystemProxiesEnabled(false);
		proxyService.setProxiesEnabled(false);
	}

	// TODO: this method is both needed by UI tests and Core tests
	// provide a common base for UI tests and core tests
	/**
	 * verifies that repository contains exactly the given files.
	 * @param repository
	 * @param paths
	 * @throws Exception
	 */
	public static void assertRepositoryContainsFiles(Repository repository,
			String[] paths) throws Exception {
		Set<String> expectedfiles = new HashSet<>();
		for (String path : paths)
			expectedfiles.add(path);
		try (TreeWalk treeWalk = new TreeWalk(repository)) {
			treeWalk.addTree(repository.resolve("HEAD^{tree}"));
			treeWalk.setRecursive(true);
			while (treeWalk.next()) {
				String path = treeWalk.getPathString();
				if (!expectedfiles.contains(path))
					fail("Repository contains unexpected expected file "
							+ path);
				expectedfiles.remove(path);
			}
		}
		if (expectedfiles.size() > 0) {
			StringBuilder message = new StringBuilder(
					"Repository does not contain expected files: ");
			for (String path : expectedfiles) {
				message.append(path);
				message.append(" ");
			}
			fail(message.toString());
		}
	}

	/**
	 * verifies that repository contains exactly the given files with the given
	 * content. Usage example:<br>
	 *
	 * <code>
	 * assertRepositoryContainsFiles(repository, "foo/a.txt", "content of A",
	 *                                           "foo/b.txt", "content of B")
	 * </code>
	 * @param repository
	 * @param args
	 * @throws Exception
	 */
	public static void assertRepositoryContainsFilesWithContent(Repository repository,
			String... args) throws Exception {
		HashMap<String, String> expectedfiles = mkmap(args);
		try (TreeWalk treeWalk = new TreeWalk(repository)) {
			treeWalk.addTree(repository.resolve("HEAD^{tree}"));
			treeWalk.setRecursive(true);
			while (treeWalk.next()) {
				String path = treeWalk.getPathString();
				assertTrue(expectedfiles.containsKey(path));
				ObjectId objectId = treeWalk.getObjectId(0);
				byte[] expectedContent = expectedfiles.get(path)
						.getBytes("UTF-8");
				byte[] repoContent = treeWalk.getObjectReader().open(objectId)
						.getBytes();
				if (!Arrays.equals(repoContent, expectedContent))
					fail("File " + path + " has repository content "
							+ new String(repoContent, "UTF-8")
							+ " instead of expected content "
							+ new String(expectedContent, "UTF-8"));
				expectedfiles.remove(path);
			}
		}
		if (expectedfiles.size() > 0) {
			StringBuilder message = new StringBuilder(
					"Repository does not contain expected files: ");
			for (String path : expectedfiles.keySet()) {
				message.append(path);
				message.append(" ");
			}
			fail(message.toString());
		}
	}

	private static HashMap<String, String> mkmap(String... args) {
		if ((args.length % 2) > 0)
			throw new IllegalArgumentException("needs to be pairs");
		HashMap<String, String> map = new HashMap<>();
		for (int i = 0; i < args.length; i += 2)
			map.put(args[i], args[i+1]);
		return map;
	}

	/**
	 * @param projectExplorerTree
	 * @param projects
	 *            name of a project
	 * @return the project item pertaining to the project
	 */
	public SWTBotTreeItem[] getProjectItems(SWTBotTree projectExplorerTree,
			String... projects) {
		List<SWTBotTreeItem> items = new ArrayList<SWTBotTreeItem>();
		for (SWTBotTreeItem item : projectExplorerTree.getAllItems()) {
			String itemText = item.getText();
			StringTokenizer tok = new StringTokenizer(itemText, " ");
			String name = tok.nextToken();
			// may be a dirty marker
			if (name.equals(">"))
				name = tok.nextToken();
			for (String project : projects)
				if (project.equals(name))
					items.add(item);
		}
		return items.isEmpty() ? null : items.toArray(new SWTBotTreeItem[0]);
	}

	/**
	 * Expand a node and wait until it is expanded. Use only if the node is
	 * expected to have children.
	 *
	 * @param treeItem
	 *            to expand
	 * @return the {@code treeItem}
	 */
	public static SWTBotTreeItem expandAndWait(final SWTBotTreeItem treeItem) {
		final String text = treeItem.getText();
		treeItem.expand();
		new SWTBot().waitUntil(new DefaultCondition() {

			@Override
			public boolean test() {
				if (treeItem.widget.isDisposed()) {
					return true; // Stop waiting and report failure below.
				}
				SWTBotTreeItem[] children = treeItem.getItems();
				return children != null && children.length > 0;
			}

			@Override
			public String getFailureMessage() {
				return "No children found for " + text;
			}
		});
		if (treeItem.widget.isDisposed()) {
			fail("Widget disposed while waiting for expansion of node " + text);
		}
		return treeItem;
	}

	/**
	 * Expand a node and wait until it is expanded and has a child node with the
	 * given name. Use only if the node is expected to have children.
	 *
	 * @param treeItem
	 *            to expand
	 * @param childName
	 *            to wait for
	 * @return the child node
	 */
	public static SWTBotTreeItem expandAndWaitFor(final SWTBotTreeItem treeItem,
			final String childName) {
		final String text = treeItem.getText();
		final SWTBotTreeItem[] result = { null };
		treeItem.expand();
		new SWTBot().waitUntil(new DefaultCondition() {

			@Override
			public boolean test() {
				if (treeItem.widget.isDisposed()) {
					return true; // Stop waiting and report failure below.
				}
				try {
					result[0] = treeItem.getNode(childName);
					return true;
				} catch (WidgetNotFoundException e) {
					return false;
				}
			}

			@Override
			public String getFailureMessage() {
				return "Child " + childName + " not found under " + text;
			}
		});
		if (treeItem.widget.isDisposed()) {
			fail("Widget disposed while waiting for child node " + text + '.'
					+ childName);
		}
		return result[0];
	}

	/**
	 * Navigates in the given tree to the node denoted by the labels in the
	 * path, using {@link #getNode(SWTBotTreeItem[], String)} to find children.
	 *
	 * @param tree
	 *            to navigate
	 * @param path
	 *            sequence of strings denoting the node to navigate to
	 * @return the node found
	 * @throws WidgetNotFoundException
	 *             if a node along the path cannot be found, or there are
	 *             multiple candidates
	 */
	public static SWTBotTreeItem navigateTo(SWTBotTree tree, String... path) {
		assertNotNull(path);

		new SWTBot().waitUntil(widgetIsEnabled(tree));
		SWTBotTreeItem item = getNode(tree.getAllItems(), path[0]);
		for (int i = 1; item != null && i < path.length; i++) {
			item = expandAndWait(item);
			item = getChildNode(item, path[i]);
		}
		return item;
	}

	/**
	 * @param node
	 * @param childNodeText
	 * @return child node containing childNodeText
	 * @see #getNode(SWTBotTreeItem[], String)
	 */
	public static SWTBotTreeItem getChildNode(SWTBotTreeItem node,
			String childNodeText) {
		return getNode(node.getItems(), childNodeText);
	}

	/**
	 * Finds the node that contains the given text. Throws a nice message in
	 * case the item is not found or more than one matching node was found.
	 *
	 * @param nodes
	 * @param searchText
	 * @return node containing the text
	 */
	public static SWTBotTreeItem getNode(SWTBotTreeItem[] nodes, String searchText) {
		List<String> texts = new ArrayList<>();
		List<SWTBotTreeItem> matchingItems = new ArrayList<SWTBotTreeItem>();

		for (SWTBotTreeItem item : nodes) {
			String text = item.getText();
			if (text.contains(searchText))
				matchingItems.add(item);
			texts.add(text);
		}

		if (matchingItems.isEmpty())
			throw new WidgetNotFoundException(
					"Tree item element containg text \"" + searchText
							+ "\" was not found. Existing tree items:\n"
							+ StringUtils.join(texts, "\n"));
		else if (matchingItems.size() > 1)
			throw new WidgetNotFoundException(
					"Tree item element containg text \""
							+ searchText
							+ "\" could not be uniquely identified. All tree items:\n"
							+ StringUtils.join(texts, "\n"));

		return matchingItems.get(0);
	}

	public static RevCommit getHeadCommit(Repository repository)
			throws Exception {
		RevCommit headCommit = null;
		ObjectId parentId = repository.resolve(Constants.HEAD);
		if (parentId != null) {
			try (RevWalk rw = new RevWalk(repository)) {
				headCommit = rw.parseCommit(parentId);
			}
		}
		return headCommit;
	}

	public static void checkHeadCommit(Repository repository, String author,
			String committer, String message) throws Exception {
		CommitInfo commitInfo = CommitHelper.getHeadCommitInfo(repository);
		assertEquals(author, commitInfo.getAuthor());
		assertEquals(committer, commitInfo.getCommitter());
		assertEquals(message, commitInfo.getCommitMessage());
	}

	public static void configureTestCommitterAsUser(Repository repository) {
		StoredConfig config = repository.getConfig();
		config.setString(ConfigConstants.CONFIG_USER_SECTION, null,
				ConfigConstants.CONFIG_KEY_NAME, TestUtil.TESTCOMMITTER_NAME);
		config.setString(ConfigConstants.CONFIG_USER_SECTION, null,
				ConfigConstants.CONFIG_KEY_EMAIL, TestUtil.TESTCOMMITTER_EMAIL);
	}

	public static void waitUntilViewWithGivenIdShows(final String viewId) {
		waitForView(new BaseMatcher<IViewReference>() {
			@Override
			public boolean matches(Object item) {
				if (item instanceof IViewReference)
					return viewId.equals(((IViewReference) item).getId());
				return false;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Wait for view with ID=" + viewId);
			}
		});
	}

	public static void waitUntilViewWithGivenTitleShows(final String viewTitle) {
		waitForView(new BaseMatcher<IViewReference>() {
			@Override
			public boolean matches(Object item) {
				if (item instanceof IViewReference)
					return viewTitle.equals(((IViewReference) item).getTitle());

				return false;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Wait for view with title " + viewTitle);
			}
		});
	}

	public static SWTBotShell botForShellStartingWith(final String titlePrefix) {
		SWTWorkbenchBot bot = new SWTWorkbenchBot();

		Matcher<Shell> matcher = new TypeSafeMatcher<Shell>() {
			@Override
			protected boolean matchesSafely(Shell item) {
				String title = item.getText();
				return title != null && title.startsWith(titlePrefix);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Shell with title starting with '"
						+ titlePrefix + "'");
			}
		};

		Shell shell = bot.widget(matcher);
		return new SWTBotShell(shell);
	}

	public static SWTBotView showView(final String viewId) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow();
				IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();
				try {
					workbenchPage.showView(viewId);
					processUIEvents();
				} catch (PartInitException e) {
					throw new RuntimeException("Showing view with ID " + viewId
							+ " failed.", e);
				}
			}
		});

		SWTWorkbenchBot bot = new SWTWorkbenchBot();
		SWTBotView viewbot = bot.viewById(viewId);
		assertNotNull("View with ID " + viewId + " not found via SWTBot.",
				viewbot);
		return viewbot;
	}

	public static void hideView(final String viewId) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbenchWindow[] windows = PlatformUI.getWorkbench()
						.getWorkbenchWindows();
				for (IWorkbenchWindow window : windows) {
					IWorkbenchPage workbenchPage = window.getActivePage();
					IViewReference[] views = workbenchPage.getViewReferences();
					for (int i = 0; i < views.length; i++) {
						IViewReference view = views[i];
						if (viewId.equals(view.getId())) {
							workbenchPage.hideView(view);
						}
					}
					processUIEvents();
				}
			}
		});
	}

	public static SWTBotView showHistoryView() {
		return showView(IHistoryView.VIEW_ID);
	}

	public static SWTBotView showExplorerView() {
		return showView(JavaUI.ID_PACKAGES);
	}

	public static SWTBotTree getExplorerTree() {
		SWTBotView view = showExplorerView();
		return view.bot().tree();
	}

	public static void openJobResultDialog(Job job) {
		assertNotNull("Job should not be null", job);
		final Action action = (Action) job
				.getProperty(IProgressConstants.ACTION_PROPERTY);
		if (action != null) {
			UIThreadRunnable.asyncExec(new VoidResult() {

				@Override
				public void run() {
					action.run();
				}
			});
		}
	}
}
