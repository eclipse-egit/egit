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
package org.eclipse.egit.ui.view.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

/**
 * Collection of utility methods for Git Repositories View tests
 */
public abstract class GitRepositoriesViewTestBase extends
		LocalRepositoryTestCase {

	protected static final RepositoriesViewLabelProvider labelProvider = new RepositoriesViewLabelProvider();

	// test utilities
	protected static final TestUtil myUtil = new TestUtil();
	protected static final GitRepositoriesViewTestUtils myRepoViewUtil = new GitRepositoriesViewTestUtils();

	// the "Git Repositories View" bot
	private SWTBotView viewbot;

	// the human-readable view name
	protected static String viewName;

	// the human readable Git category
	private static String gitCategory;

	static {
		viewName = myUtil.getPluginLocalizedValue("GitRepositoriesView_name");
		gitCategory = myUtil.getPluginLocalizedValue("GitCategory_name");
	}

	/**
	 * remove all configured repositories from the view
	 */
	protected static void clearView() {
		new InstanceScope().getNode(Activator.getPluginId()).remove(
				RepositoryUtil.PREFS_DIRECTORIES);
	}

	protected SWTBotView getOrOpenView() throws Exception {
		if (viewbot == null) {
			bot.menu("Window").menu("Show View").menu("Other...").click();
			SWTBotShell shell = bot.shell("Show View").activate();
			shell.bot().tree().expandNode(gitCategory).getNode(viewName)
					.select();
			shell.bot().button(IDialogConstants.OK_LABEL).click();

			viewbot = bot.viewByTitle(viewName);

			assertNotNull("Repositories View should not be null", viewbot);
		} else
			viewbot.setFocus();
		return viewbot;
	}

	protected void assertHasRepo(File repositoryDir) throws Exception {
		final SWTBotTree tree = getOrOpenView().bot().tree();
		final SWTBotTreeItem[] items = tree.getAllItems();
		boolean found = false;
		for (SWTBotTreeItem item : items) {
			if (item.getText().startsWith(
					repositoryDir.getParentFile().getName())) {
				found = true;
				break;
			}
		}
		assertTrue("Tree should have item with correct text", found);
	}

	protected void assertEmpty() throws Exception {
		final SWTBotView view = getOrOpenView();
		final SWTBotTreeItem[] items = view.bot().tree().getAllItems();
		assertTrue("Tree should have no items", items.length == 0);
	}

	protected void refreshAndWait() throws Exception {
		RepositoriesView view = (RepositoriesView) getOrOpenView()
				.getReference().getPart(false);
		Job refreshJob = view.refresh();
		refreshJob.join();
	}

	@SuppressWarnings("boxing")
	protected void assertProjectExistence(String projectName, boolean existence) {
		IProject prj = ResourcesPlugin.getWorkspace().getRoot().getProject(
				projectName);
		assertEquals("Project existence " + projectName, prj.exists(),
				existence);
	}
}
