/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
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
package org.eclipse.egit.ui.test.team.actions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.team.ui.history.IHistoryView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Show History and Team->Show in Repositories View actions
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class ShowInTest extends LocalRepositoryTestCase {

	@Before
	public void setup() throws Exception {
		createProjectAndCommitToRepository();
	}

	@Test
	public void testOpenHistory() throws Exception {
		try {
			SWTBotView view = bot.viewById(IHistoryView.VIEW_ID);
			view.close();
		} catch (Exception e) {
			// ignore
		}

		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String menuString = util
				.getPluginLocalizedValue("ShowResourceInHistoryAction_label");
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Team",
				menuString);
		bot.viewById(IHistoryView.VIEW_ID).close();
	}

	@Test
	public void testOpenHistoryMultiSelection() throws Exception {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
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

		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String menuString = util
				.getPluginLocalizedValue("ShowRepositoryAction_label");
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Team",
				menuString);
		bot.viewById(RepositoriesView.VIEW_ID).close();
	}

	@Test
	public void testOpenRepoViewMultiSelection() throws Exception {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		projectExplorerTree.select(0, 1);
		String menuString = util
				.getPluginLocalizedValue("ShowRepositoryAction_label");
		// Team->show in repository must be disabled on a multiple selection
		assertFalse(ContextMenuHelper.isContextMenuItemEnabled(projectExplorerTree, "Team",
				menuString));
	}

}
