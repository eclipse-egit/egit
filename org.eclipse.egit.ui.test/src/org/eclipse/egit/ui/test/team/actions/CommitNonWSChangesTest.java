/*******************************************************************************
 * Copyright (C) 2011, 2013 Jens Baumgart <jens.baumgart@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarToggleButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Commit action
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class CommitNonWSChangesTest extends LocalRepositoryTestCase {
	private File repositoryFile;

	private Repository repository;

	@Before
	public void setup() throws Exception {
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.ALWAYS_USE_STAGING_VIEW, false);
		repositoryFile = createProjectAndCommitToRepository();
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(repositoryFile);
		repository = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache().lookupRepository(repositoryFile);
	}

	@After
	public void tearDown() {
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.ALWAYS_USE_STAGING_VIEW, true);
	}

	@Test
	public void testCommitDeletedProject() throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);
		project.delete(true, false, null);
		clickOnCommit();

		SWTBotShell commitDialog = bot.shell(UIText.CommitDialog_CommitChanges);
		SWTBotToolbarToggleButton showUntracked = commitDialog.bot()
				.toolbarToggleButtonWithTooltip(
						UIText.CommitDialog_ShowUntrackedFiles);
		if (!showUntracked.isChecked())
			showUntracked.select();

		SWTBotTree tree = commitDialog.bot().tree();
		assertEquals("Wrong row count", 4, tree.rowCount());
		assertTreeLineContent(tree, 0, "GeneralProject/.project");
		assertTreeLineContent(tree, 1, "GeneralProject/folder/test.txt");
		assertTreeLineContent(tree, 2, "GeneralProject/folder/test2.txt");
		assertTreeLineContent(tree, 3, "ProjectWithoutDotProject/.project");

		commitDialog.bot().textWithLabel(UIText.CommitDialog_Author)
				.setText(TestUtil.TESTAUTHOR);
		commitDialog.bot().textWithLabel(UIText.CommitDialog_Committer)
				.setText(TestUtil.TESTCOMMITTER);
		commitDialog.bot()
				.styledTextWithLabel(UIText.CommitDialog_CommitMessage)
				.setText("Delete Project GeneralProject");
		selectAllCheckboxes(tree);
		commitDialog.bot().button(UIText.CommitDialog_Commit).click();
		// wait until commit is completed
		Job.getJobManager().join(JobFamilies.COMMIT, null);
		String[] paths = { "ProjectWithoutDotProject/.project",
				"ProjectWithoutDotProject/folder/test.txt",
				"ProjectWithoutDotProject/folder/test2.txt" };
		TestUtil.assertRepositoryContainsFiles(repository, paths);
		// check there is nothing to commit
		clickOnCommit();
		bot.shell(UIText.CommitAction_noFilesToCommit).bot()
				.button(IDialogConstants.NO_LABEL).click();
	}

	private void assertTreeLineContent(SWTBotTree tree, int rowIndex,
			String file) {
		SWTBotTreeItem treeItem = tree.getAllItems()[rowIndex];
		assertEquals(file, treeItem.cell(1));
	}

	private void selectAllCheckboxes(SWTBotTree tree) {
		for (int i = 0; i < tree.rowCount(); i++) {
			tree.getAllItems()[i].check();
		}
	}

	private void clickOnCommit() throws Exception {
		SWTBotView repoView = TestUtil.showView(RepositoriesView.VIEW_ID);
		TestUtil.joinJobs(JobFamilies.REPO_VIEW_REFRESH);
		SWTBotTree tree = repoView.bot().tree();
		TestUtil.waitUntilTreeHasNodeContainsText(bot, tree, REPO1, 10000);
		tree.getAllItems()[0].contextMenu("Commit...").click();
	}
}
