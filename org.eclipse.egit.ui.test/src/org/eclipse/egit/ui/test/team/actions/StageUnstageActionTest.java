/*******************************************************************************
 * Copyright (c) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.resources.IResourceState;
import org.eclipse.egit.ui.internal.resources.ResourceStateFactory;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
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
	public void testActionsInitiallyNotPresent() {
		// Verify that neither add to index nor remove from index are available.
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
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
				addToIndexLabel);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		// Verify file got staged
		verifyStaging(PROJ_A, filePath, true);
		// Remove from index
		util.getProjectItems(projectExplorerTree, PROJ_A)[0].select();
		assertFalse("Add To Index should not be present",
				ContextMenuHelper.contextMenuItemExists(projectExplorerTree,
						"Team", addToIndexLabel));
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
				removeFromIndexLabel);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		// Verify file is unstaged again
		verifyStaging(PROJ_A, filePath, false);
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
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
				addToIndexLabel);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		// Verify both files got staged
		verifyStaging(PROJ_A, filePath, true);
		verifyStaging(PROJ_B, filePath, true);
		// Select both projects
		projectExplorerTree.select(projectExplorerTree.getAllItems());
		// Remove from index
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
				removeFromIndexLabel);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		// Verify both files got unstaged
		verifyStaging(PROJ_A, filePath, false);
		verifyStaging(PROJ_B, filePath, false);
	}

	private void verifyStaging(String projectName, String filePath,
			boolean expected) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(projectName);
		IResource resource = project.findMember(filePath);
		assertNotNull(filePath + " should exist", resource);
		IResourceState state = ResourceStateFactory.getInstance().get(resource);
		if (expected) {
			assertTrue(projectName + '/' + filePath + " should be staged",
					state.isStaged());
		} else {
			assertFalse(projectName + '/' + filePath + " should be unstaged",
					state.isStaged());
		}
	}
}
