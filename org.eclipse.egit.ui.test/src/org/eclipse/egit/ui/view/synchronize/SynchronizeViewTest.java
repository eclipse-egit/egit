/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.view.synchronize;

import static org.eclipse.egit.ui.test.ContextMenuHelper.clickContextMenu;
import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.Constants.MASTER;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.Eclipse;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotRadio;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class SynchronizeViewTest extends LocalRepositoryTestCase {

	@Test
	public void shouldReturnNoChanges() throws Exception {
		// given
		resetRepository(PROJ1);
		changeFilesInProject();
		showDialog(PROJ1, "Team", "Synchronize...");

		// when
		bot.shell("Synchronize repository: " + REPO1 + File.separator + ".git")
				.activate();

		// we need to disable 'Include local changes'
		bot.checkBox(
				UIText.SelectSynchronizeResourceDialog_includeUncommitedChanges)
				.click();

		bot.comboBox(0)
				.setSelection(UIText.SynchronizeWithAction_localRepoName);
		bot.comboBox(1).setSelection(HEAD);

		bot.comboBox(2)
				.setSelection(UIText.SynchronizeWithAction_localRepoName);
		bot.comboBox(3).setSelection(MASTER);

		// fire action
		bot.button(IDialogConstants.OK_LABEL).click();

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(0, syncViewTree.getAllItems().length);
	}

	// this test fails when is run inside eclipse with Maven POM editor
	@Test
	public void shouldReturnListOfChanges() throws Exception {
		// given
		resetRepository(PROJ1);
		changeFilesInProject();
		showDialog(PROJ1, "Team", "Synchronize...");

		// when
		bot.shell("Synchronize repository: " + REPO1 + File.separator + ".git")
				.activate();

		// include local changes are enabled by default

		bot.comboBox(2)
				.setSelection(UIText.SynchronizeWithAction_localRepoName);
		bot.comboBox(3).setSelection(MASTER);

		// fire action
		bot.button(IDialogConstants.OK_LABEL).click();

		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();

		// wait for synchronization process finish
		waitUntilTreeHasNodeWithText(syncViewTree,
				UIText.GitModelWorkingTree_workingTree);

		SWTBotTreeItem[] syncItems = syncViewTree.getAllItems();
		assertEquals(UIText.GitModelWorkingTree_workingTree,
				syncItems[0].getText());
	}

	@Test
	public void shouldCompareBranchAgainstTag() throws Exception {
		// given
		resetRepository(PROJ1);
		createTag(PROJ1, "v0.0");
		makeChangesAndCommit(PROJ1);
		showDialog(PROJ1, "Team", "Synchronize...");

		// when
		bot.shell("Synchronize repository: " + REPO1 + File.separator + ".git")
				.activate();

		// we need to disable 'Include local changes'
		bot.checkBox(
				UIText.SelectSynchronizeResourceDialog_includeUncommitedChanges)
				.click();

		bot.comboBox(0)
				.setSelection(UIText.SynchronizeWithAction_tagsName);
		bot.comboBox(1).setSelection("v0.0");

		bot.comboBox(2)
				.setSelection(UIText.SynchronizeWithAction_localRepoName);
		bot.comboBox(3).setSelection(HEAD);

		// fire action
		bot.button(IDialogConstants.OK_LABEL).click();

		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();

		// wait for synchronization process finish
		waitUntilTreeHasNodeWithText(syncViewTree, "test commit");

		// then
		assertEquals(1, syncViewTree.getAllItems().length);
	}

	@Test
	public void shouldCompareTagAgainstTag() throws Exception {
		// given
		resetRepository(PROJ1);
		createTag(PROJ1, "v0.1");
		makeChangesAndCommit(PROJ1);
		createTag(PROJ1, "v0.2");
		makeChangesAndCommit(PROJ1);
		showDialog(PROJ1, "Team", "Synchronize...");

		// when
		bot.shell("Synchronize repository: " + REPO1 + File.separator + ".git")
				.activate();

		// we need to disable 'Include local changes'
		bot.checkBox(
				UIText.SelectSynchronizeResourceDialog_includeUncommitedChanges)
				.click();

		bot.comboBox(0)
				.setSelection(UIText.SynchronizeWithAction_tagsName);
		bot.comboBox(1).setSelection("v0.1");

		bot.comboBox(2)
				.setSelection(UIText.SynchronizeWithAction_tagsName);
		bot.comboBox(3).setSelection("v0.2");

		// fire action
		bot.button(IDialogConstants.OK_LABEL).click();

		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();

		// wait for synchronization process finish
		waitUntilTreeHasNodeWithText(syncViewTree, "test commit");

		// then
		assertEquals(1, syncViewTree.getAllItems().length);
	}

	@Test
	public void shouldListFileDeletedChange() throws Exception {
		// given
		resetRepository(PROJ1);
		createTag(PROJ1, "base");
		deleteFileAndCommit(PROJ1);
		showDialog(PROJ1, "Team", "Synchronize...");

		// when
		bot.shell("Synchronize repository: " + REPO1 + File.separator + ".git")
				.activate();

		bot.comboBox(2)
				.setSelection(UIText.SynchronizeWithAction_tagsName);
		bot.comboBox(3).setSelection("base");

		// fire action
		bot.button(IDialogConstants.OK_LABEL).click();

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(1, syncViewTree.getAllItems().length);
		SWTBotTreeItem commitTree = syncViewTree.getAllItems()[0];
		commitTree.expand();
		SWTBotTreeItem projectTree = commitTree.getItems()[0];
		projectTree.expand();
		assertEquals(1, projectTree.getItems().length);
		SWTBotTreeItem folderTree = projectTree.getItems()[0];
		folderTree.expand();
		assertEquals(1, folderTree.getItems().length);
		SWTBotTreeItem fileTree = folderTree.getItems()[0];
		assertEquals("test.txt", fileTree.getText());
	}

	private void waitUntilTreeHasNodeWithText(final SWTBotTree tree,
			final String text) {
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
		}, 10000);
	}

	// this test always fails with cause:
	// Timeout after: 5000 ms.: Could not find context menu with text:
	// Synchronize
	@Ignore
	@Test
	public void shouldLaunchSynchronizationFromGitRepositories()
			throws Exception {
		// given
		bot.menu("Window").menu("Show View").menu("Other...").click();
		bot.shell("Show View").bot().tree().expandNode("Git").getNode(
				"Git Repositories").doubleClick();

		SWTBotTree repositoriesTree = bot.viewByTitle("Git Repositories").bot()
				.tree();
		SWTBotTreeItem egitRoot = repositoriesTree.getAllItems()[0];
		egitRoot.expand();
		egitRoot.collapse();
		egitRoot.expand();
		SWTBotTreeItem remoteBranch = egitRoot.expandNode("Branches")
				.expandNode("Remote Branches");
		SWTBotTreeItem branchNode = remoteBranch.getNode("origin/stable-0.7");
		branchNode.select();
		branchNode.contextMenu("Synchronize").click();

		// when

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(8, syncViewTree.getAllItems().length);
	}

	@Before
	public void setupViews() {
		bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();
		bot.viewByTitle("Package Explorer").show();
	}

	@BeforeClass
	public static void setupEnvironment() throws Exception {
		// disable perspective synchronize selection
		new Eclipse().openPreferencePage(null);
		bot.tree().getTreeItem("Team").expand().select();
		SWTBotRadio syncPerspectiveCheck = bot.radio("Never");
		if (!syncPerspectiveCheck.isSelected())
			syncPerspectiveCheck.click();

		bot.button(IDialogConstants.OK_LABEL).click();

		File repositoryFile = createProjectAndCommitToRepository();
		createChildRepository(repositoryFile);
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(repositoryFile);
	}

	@AfterClass
	public static void restoreEnvironmentSetup() throws Exception {
		new Eclipse().reset();
	}

	private void changeFilesInProject() throws Exception {
		SWTBot packageExplorerBot = bot.viewByTitle("Package Explorer").bot();
		packageExplorerBot.activeShell();
		SWTBotTree tree = packageExplorerBot.tree();

		SWTBotTreeItem coreTreeItem = tree.getAllItems()[0];
		SWTBotTreeItem rootNode = coreTreeItem.expand().getNode(0)
				.expand().select();
		rootNode.getNode(0).select().doubleClick();

		SWTBotEditor corePomEditor = bot.editorByTitle(FILE1);
		corePomEditor.toTextEditor()
				.insertText("<!-- EGit jUnit test case -->");
		corePomEditor.saveAndClose();

		rootNode.getNode(1).select().doubleClick();
		SWTBotEditor uiPomEditor = bot.editorByTitle(FILE2);
		uiPomEditor.toTextEditor().insertText("<!-- EGit jUnit test case -->");
		uiPomEditor.saveAndClose();
		coreTreeItem.collapse();
	}

	private void resetRepository(String projectName) throws Exception {
		showDialog(projectName, "Team", "Reset...");

		bot.shell(UIText.ResetCommand_WizardTitle).bot().activeShell();
		bot.radio(UIText.ResetTargetSelectionDialog_ResetTypeHardButton)
				.click();
		bot.button(UIText.ResetTargetSelectionDialog_ResetButton).click();

		bot.shell(UIText.ResetTargetSelectionDialog_ResetQuestion).bot()
				.activeShell();
		bot.button("Yes").click();
		TestUtil.joinJobs(JobFamilies.RESET);
	}

	private void createTag(String projectName, String tagName) throws Exception {
		showDialog(projectName, "Team", "Tag...");

		bot.shell("Create new tag").bot().activeShell();
		bot.text(0).setFocus();
		bot.text(0).setText(tagName);
		bot.styledText(0).setFocus();
		bot.styledText(0).setText(tagName);
		bot.button(IDialogConstants.OK_LABEL).click();
		TestUtil.joinJobs(JobFamilies.TAG);
	}

	private void makeChangesAndCommit(String projectName) throws Exception {
		changeFilesInProject();
		Thread.sleep(1000); // wait 1 s to get different time stamps
							// TODO can be removed when commit is based on DirCache
		commit(projectName);
	}

	private void deleteFileAndCommit(String projectName) throws Exception {
		ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFile(new Path("folder/test.txt")).delete(true, null);

		commit(projectName);
	}

	private void commit(String projectName) throws InterruptedException {
		showDialog(projectName, "Team", UIText.CommitAction_commit);

		bot.shell(UIText.CommitDialog_CommitChanges).bot().activeShell();
		bot.styledText(0).setText("test commit");
		bot.button(UIText.CommitDialog_SelectAll).click();
		bot.button(UIText.CommitDialog_Commit).click();
		TestUtil.joinJobs(JobFamilies.COMMIT);
	}

	private void showDialog(String projectName, String... cmd) {
		bot.activeView();
		SWTBot packageExplorerBot = bot.viewByTitle("Package Explorer").bot();
		packageExplorerBot.activeShell();
		SWTBotTree tree = packageExplorerBot.tree();

		// EGit decorates the project node shown in the package explorer. The
		// '>' decorator indicates that there are uncommitted changes present in
		// the project. Also the repository and branch name are added as a
		// suffix ('[<repo name> <branch name>]' suffix). To bypass this
		// decoration we use here this loop.
		for (SWTBotTreeItem item : tree.getAllItems()) {
			if (item.getText().contains(projectName)) {
				item.select();
				break;
			}
		}

		clickContextMenu(tree, cmd);
	}

}
