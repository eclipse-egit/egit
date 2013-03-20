package org.eclipse.egit.ui.test.team.actions;

/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Commit action
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class CommitNonWSChangesTest extends LocalRepositoryTestCase {
	private static File repositoryFile;

	private static Repository repository;

	protected static final GitRepositoriesViewTestUtils myRepoViewUtil = new GitRepositoriesViewTestUtils();

	@BeforeClass
	public static void setup() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(repositoryFile);
		repository = org.eclipse.egit.core.internal.Activator.getDefault()
				.getRepositoryCache().lookupRepository(repositoryFile);
	}

	@Test
	public void testCommitDeletedProject() throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);
		project.delete(true, false, null);
		clickOnCommit();

		SWTBotShell commitDialog = bot.shell(UIText.CommitDialog_CommitChanges);
		SWTBotTable table = commitDialog.bot().table();
		assertEquals("Wrong row count", 4, table.rowCount());
		assertTableLineContent(table, 0, "Rem., not staged",
				"GeneralProject/.project");
		assertTableLineContent(table, 1, "Rem., not staged",
				"GeneralProject/folder/test.txt");
		assertTableLineContent(table, 2, "Rem., not staged",
				"GeneralProject/folder/test2.txt");
		assertTableLineContent(table, 3, "Untracked",
				"ProjectWithoutDotProject/.project");

		commitDialog.bot().textWithLabel(UIText.CommitDialog_Author)
				.setText(TestUtil.TESTAUTHOR);
		commitDialog.bot().textWithLabel(UIText.CommitDialog_Committer)
				.setText(TestUtil.TESTCOMMITTER);
		commitDialog.bot()
				.styledTextWithLabel(UIText.CommitDialog_CommitMessage)
				.setText("Delete Project GeneralProject");
		selectAllCheckboxes(table);
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

	private void assertTableLineContent(SWTBotTable table, int rowIndex,
			String status, String file) {
		assertEquals(status, table.getTableItem(rowIndex).getText(0));
		assertEquals(file, table.getTableItem(rowIndex).getText(1));
	}

	private void selectAllCheckboxes(SWTBotTable table) {
		for (int i = 0; i < table.rowCount(); i++) {
			table.getTableItem(i).check();
		}
	}

	private void clickOnCommit() throws Exception {
		SWTBotView repoView = myRepoViewUtil.openRepositoriesView(bot);
		SWTBotTree tree = repoView.bot().tree();
		TestUtil.waitUntilTreeHasNodeContainsText(bot, tree, REPO1, 10000);
		tree.getAllItems()[0].contextMenu("Commit...").click();
	}
}
