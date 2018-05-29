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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.JobJoiner;
import org.eclipse.egit.ui.test.StagingUtil;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Commit action
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class CommitActionStagingViewTest extends LocalRepositoryTestCase {
	private File repositoryFile;

	private boolean initialUseStagingView;

	private boolean initialAutoStage;

	@Before
	public void setup() throws Exception {
		TestUtil.hideView(StagingView.VIEW_ID);
		initialUseStagingView = Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.ALWAYS_USE_STAGING_VIEW);
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.ALWAYS_USE_STAGING_VIEW, true);
		initialAutoStage = Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.AUTO_STAGE_ON_COMMIT);
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.AUTO_STAGE_ON_COMMIT, false);
		Activator.getDefault().getPreferenceStore()
				.setDefault(UIPreferences.STAGING_VIEW_SYNC_SELECTION, false);
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.STAGING_VIEW_SYNC_SELECTION, false);
		repositoryFile = createProjectAndCommitToRepository();
		Repository repo = lookupRepository(repositoryFile);
		TestUtil.configureTestCommitterAsUser(repo);
		// TODO delete the second project for the time being (.gitignore is
		// currently not hiding the .project file from commit)
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ2);
		File dotProject = new File(project.getLocation().toOSString(), ".project");
		project.delete(false, false, null);
		assertTrue(dotProject.delete());
	}

	@After
	public void tearDown() {
		Activator.getDefault().getPreferenceStore().setValue(
				UIPreferences.ALWAYS_USE_STAGING_VIEW, initialUseStagingView);
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.AUTO_STAGE_ON_COMMIT, initialAutoStage);
		Activator.getDefault().getPreferenceStore()
				.setDefault(UIPreferences.STAGING_VIEW_SYNC_SELECTION, true);
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.STAGING_VIEW_SYNC_SELECTION, true);
	}

	@Test
	public void testOpenStagingViewNoLinkWithSelection() throws Exception {
		setTestFileContent("I have changed this");
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		util.getProjectItems(projectExplorerTree, PROJ1)[0].select();
		String menuString = util.getPluginLocalizedValue("CommitAction_label");
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
				menuString);
		TestUtil.joinJobs(ADD_TO_INDEX);
		TestUtil.waitUntilViewWithGivenIdShows(StagingView.VIEW_ID);
		final Repository[] repo = { null };
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				StagingView view;
				try {
					view = (StagingView) PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage()
							.showView(StagingView.VIEW_ID);
					repo[0] = view.getCurrentRepository();
				} catch (PartInitException e) {
					// Ignore, repo[0] remains null
				}
			}
		});
		Repository repository = lookupRepository(repositoryFile);
		assertNotNull("No repository found", repository);
		assertEquals("Repository mismatch", repository, repo[0]);
	}

	@Test
	public void testCommitWithoutAutoStage() throws Exception {
		setTestFileContent("I have changed this");
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		util.getProjectItems(projectExplorerTree, PROJ1)[0].select();
		String menuString = util.getPluginLocalizedValue("CommitAction_label");
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
				menuString);
		TestUtil.joinJobs(ADD_TO_INDEX);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		TestUtil.waitUntilViewWithGivenIdShows(StagingView.VIEW_ID);
		StagingUtil.assertStaging(PROJ1, FOLDER + '/' + FILE1, false);
	}

	@Test
	public void testCommitWithAutoStageNoUntracked() throws Exception {
		assertCommitWithAutoStage(false);
	}

	@Test
	public void testCommitWithAutoStageWithUntracked() throws Exception {
		assertCommitWithAutoStage(true);
	}

	private void assertCommitWithAutoStage(boolean withUntracked)
			throws Exception {
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.AUTO_STAGE_ON_COMMIT, true);
		boolean initialIncludeUntracked = Activator.getDefault()
				.getPreferenceStore()
				.getBoolean(UIPreferences.COMMIT_DIALOG_INCLUDE_UNTRACKED);
		Activator.getDefault().getPreferenceStore().setValue(
				UIPreferences.COMMIT_DIALOG_INCLUDE_UNTRACKED, withUntracked);
		String newFile = "newFile.txt";
		try {
			setTestFileContent("I have changed this");
			touch(PROJ1, newFile, "New file content");
			SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
			util.getProjectItems(projectExplorerTree, PROJ1)[0].select();
			String menuString = util
					.getPluginLocalizedValue("CommitAction_label");
			JobJoiner joiner = JobJoiner.startListening(ADD_TO_INDEX, 10,
					TimeUnit.SECONDS);
			ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
					menuString);
			joiner.join();
			TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
			TestUtil.waitUntilViewWithGivenIdShows(StagingView.VIEW_ID);
			StagingUtil.assertStaging(PROJ1, FOLDER + '/' + FILE1, true);
			StagingUtil.assertStaging(PROJ1, newFile, withUntracked);
		} finally {
			Activator.getDefault().getPreferenceStore().setValue(
					UIPreferences.COMMIT_DIALOG_INCLUDE_UNTRACKED,
					initialIncludeUntracked);
		}
	}

}
