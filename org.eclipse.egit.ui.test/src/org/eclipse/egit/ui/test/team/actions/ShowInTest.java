/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.egit.ui.internal.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotPerspective;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.team.ui.history.IHistoryView;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Show History and Team->Show in Repositories View actions
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class ShowInTest extends LocalRepositoryTestCase {

	private static SWTBotPerspective perspective;

	@BeforeClass
	public static void setup() throws Exception {
		createProjectAndCommitToRepository();
		perspective = bot.activePerspective();
		bot.perspectiveById("org.eclipse.pde.ui.PDEPerspective").activate();
		waitInUI();
	}

	@AfterClass
	public static void shutdown() {
		perspective.activate();
	}

	@Test
	public void testOpenHistory() throws Exception {
		try {
			SWTBotView view = bot.viewById(IHistoryView.VIEW_ID);
			view.close();
		} catch (Exception e) {
			// ignore
		}

		SWTBotTree projectExplorerTree = bot.viewById(
				"org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String menuString = util
				.getPluginLocalizedValue("ShowResourceInHistoryAction_label");
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Team",
				menuString);
		bot.viewById(IHistoryView.VIEW_ID).close();
	}

	@Test
	public void testOpenHistoryMultiSelection() throws Exception {
		SWTBotTree projectExplorerTree = bot.viewById(
				"org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		projectExplorerTree.select(0, 1);
		String menuString = util
				.getPluginLocalizedValue("ShowResourceInHistoryAction_label");
		// Team->show in history must be enabled on a multiple selection
		assertTrue(ContextMenuHelper.isContextMenuItemEnabled(projectExplorerTree, "Team",
					menuString));
	}

	@Test
	public void testOpenRepoView() throws Exception {
		try {
			SWTBotView view = bot.viewById(RepositoriesView.VIEW_ID);
			view.close();
		} catch (Exception e) {
			// ignore
		}

		SWTBotTree projectExplorerTree = bot.viewById(
				"org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String menuString = util
				.getPluginLocalizedValue("ShowRepositoryAction_label");
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Team",
				menuString);
		bot.viewById(RepositoriesView.VIEW_ID).close();
	}

	@Test
	public void testOpenRepoViewMultiSelection() throws Exception {
		SWTBotTree projectExplorerTree = bot.viewById(
				"org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		projectExplorerTree.select(0, 1);
		String menuString = util
				.getPluginLocalizedValue("ShowRepositoryAction_label");
		// Team->show in repository must be disabled on a multiple selection
		assertFalse(ContextMenuHelper.isContextMenuItemEnabled(projectExplorerTree, "Team",
				menuString));
	}

}
