/*******************************************************************************
 * Copyright (c) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.eclipse.egit.ui.JobFamilies.ADD_TO_INDEX;
import static org.eclipse.egit.ui.JobFamilies.REMOVE_FROM_INDEX;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.StagingUtil;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Add To Index/Remove From Index actions.
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class StageUnstageActionTest extends LocalRepositoryTestCase {

	private static final String REPO_A = "repoA";

	private static final String REPO_B = "repoB";

	private static final String PROJ_A = "FirstProject";

	private static final String PROJ_B = "SecondProject";

	private static final String UNSHARED = "UnsharedProject";

	private String addToIndexLabel;

	private String removeFromIndexLabel;

	@Before
	public void setup() throws Exception {
		createProjectAndCommitToRepository(REPO_A, PROJ_A);
		createProjectAndCommitToRepository(REPO_B, PROJ_B);
		addToIndexLabel = util
				.getPluginLocalizedValue("AddToIndexAction_label");
		removeFromIndexLabel = util
				.getPluginLocalizedValue("RemoveFromIndexAction_label");
	}

	@Test
	public void testActionsInitiallyNotPresent() throws Exception {
		// Verify that neither add to index nor remove from index are available.
		IProject unshared = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(UNSHARED);
		unshared.create(null);
		unshared.open(null);
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		util.getProjectItems(projectExplorerTree, PROJ_A)[0].select();
		assertFalse("Add To Index should not be present",
				ContextMenuHelper.contextMenuItemExists(projectExplorerTree,
						"Team", addToIndexLabel));
		assertFalse("Remove From Index should not be present",
				ContextMenuHelper.contextMenuItemExists(projectExplorerTree,
						"Team", removeFromIndexLabel));
		util.getProjectItems(projectExplorerTree, PROJ_B)[0].select();
		assertFalse("Add To Index should not be present",
				ContextMenuHelper.contextMenuItemExists(projectExplorerTree,
						"Team", addToIndexLabel));
		assertFalse("Remove From Index should not be present",
				ContextMenuHelper.contextMenuItemExists(projectExplorerTree,
						"Team", removeFromIndexLabel));
		util.getProjectItems(projectExplorerTree, UNSHARED)[0].select();
		assertFalse("Add To Index should not be present",
				ContextMenuHelper.contextMenuItemExists(projectExplorerTree,
						"Team", addToIndexLabel));
		assertFalse("Remove From Index should not be present",
				ContextMenuHelper.contextMenuItemExists(projectExplorerTree,
						"Team", removeFromIndexLabel));
		projectExplorerTree.select(projectExplorerTree.getAllItems());
		assertFalse("Add To Index should not be present",
				ContextMenuHelper.contextMenuItemExists(projectExplorerTree,
						"Team", addToIndexLabel));
		assertFalse("Remove From Index should not be present",
				ContextMenuHelper.contextMenuItemExists(projectExplorerTree,
						"Team", removeFromIndexLabel));
		// And again, with only the two shared projects
		unshared.delete(true, null);
		projectExplorerTree = TestUtil.getExplorerTree();
		projectExplorerTree.select(projectExplorerTree.getAllItems());
		assertFalse("Add To Index should not be present",
				ContextMenuHelper.contextMenuItemExists(projectExplorerTree,
						"Team", addToIndexLabel));
		assertFalse("Remove From Index should not be present",
				ContextMenuHelper.contextMenuItemExists(projectExplorerTree,
						"Team", removeFromIndexLabel));
	}

	@Test
	public void testSingleProject() throws Exception {
		// Change something in first project
		String filePath = FOLDER + '/' + FILE1;
		touch(PROJ_A, filePath, "Changed content");
		// Select other project
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		util.getProjectItems(projectExplorerTree, PROJ_B)[0].select();
		// Verify that Add to index is not present
		assertFalse("Add To Index should not be present",
				ContextMenuHelper.contextMenuItemExists(projectExplorerTree,
						"Team", addToIndexLabel));
		assertFalse("Remove From Index should not be present",
				ContextMenuHelper.contextMenuItemExists(projectExplorerTree,
						"Team", removeFromIndexLabel));
		// Select first project; add to index
		util.getProjectItems(projectExplorerTree, PROJ_A)[0].select();
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Team",
				addToIndexLabel);
		TestUtil.joinJobs(ADD_TO_INDEX);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		// Verify file got staged
		StagingUtil.assertStaging(PROJ_A, filePath, true);
		// Remove from index
		util.getProjectItems(projectExplorerTree, PROJ_A)[0].select();
		assertFalse("Add To Index should not be present",
				ContextMenuHelper.contextMenuItemExists(projectExplorerTree,
						"Team", addToIndexLabel));
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Team",
				removeFromIndexLabel);
		TestUtil.joinJobs(REMOVE_FROM_INDEX);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		// Verify file is unstaged again
		StagingUtil.assertStaging(PROJ_A, filePath, false);
	}

	@Test
	public void testBothProjects() throws Exception {
		// Change something in both
		String filePath = FOLDER + '/' + FILE1;
		touch(PROJ_A, filePath, "Changed content");
		touch(PROJ_B, filePath, "Changed content");
		// Select both projects
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		projectExplorerTree.select(projectExplorerTree.getAllItems());
		assertFalse("Remove From Index should not be present",
				ContextMenuHelper.contextMenuItemExists(projectExplorerTree,
						"Team", removeFromIndexLabel));
		// Add to index
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Team",
				addToIndexLabel);
		TestUtil.joinJobs(ADD_TO_INDEX);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		// Verify both files got staged
		StagingUtil.assertStaging(PROJ_A, filePath, true);
		StagingUtil.assertStaging(PROJ_B, filePath, true);
		// Select both projects
		projectExplorerTree.select(projectExplorerTree.getAllItems());
		// Remove from index
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Team",
				removeFromIndexLabel);
		TestUtil.joinJobs(REMOVE_FROM_INDEX);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		// Verify both files got unstaged
		StagingUtil.assertStaging(PROJ_A, filePath, false);
		StagingUtil.assertStaging(PROJ_B, filePath, false);
	}

	@Test
	public void testCompareIndexWithHeadEnablement() throws Exception {
		String compareIndexWithHead = util.getPluginLocalizedValue("CompareIndexWithHeadAction_label");
		// Change something in first project
		String filePath = FOLDER + '/' + FILE1;
		touch(PROJ_A, filePath, "Changed content");
		// Add it to the index
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		SWTBotTreeItem fileItem = TestUtil.navigateTo(projectExplorerTree,
				PROJ_A, FOLDER, FILE1);
		fileItem.select();
		// Verify the menu entry
		assertTrue("Compare With->Index with HEAD should be disabled or absent",
				!ContextMenuHelper.contextMenuItemExists(projectExplorerTree,
						"Compare With", compareIndexWithHead)
						|| !ContextMenuHelper.isContextMenuItemEnabled(
								projectExplorerTree, "Compare With",
								compareIndexWithHead));
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Team",
				addToIndexLabel);
		TestUtil.joinJobs(ADD_TO_INDEX);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		// Verify file got staged
		StagingUtil.assertStaging(PROJ_A, filePath, true);
		fileItem.select();
		assertTrue("Compare With->Index with HEAD should be enabled",
				ContextMenuHelper.isContextMenuItemEnabled(projectExplorerTree,
						"Compare With", compareIndexWithHead));
		// Remove from index
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Team",
				removeFromIndexLabel);
		TestUtil.joinJobs(REMOVE_FROM_INDEX);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		// Verify file is unstaged again
		StagingUtil.assertStaging(PROJ_A, filePath, false);
		// Verify the menu entry again
		fileItem.select();
		assertTrue(
				"Compare With->Index with HEAD should be disabled or absent again",
				!ContextMenuHelper.contextMenuItemExists(projectExplorerTree,
						"Compare With", compareIndexWithHead)
						|| !ContextMenuHelper.isContextMenuItemEnabled(
								projectExplorerTree, "Compare With",
								compareIndexWithHead));
	}
}
