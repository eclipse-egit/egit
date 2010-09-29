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

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.Eclipse;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.SWTBot;
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

		bot.comboBox(0)
				.setSelection(UIText.SynchronizeWithAction_localRepoName);
		bot.comboBox(1).setSelection(HEAD);

		bot.comboBox(2)
				.setSelection(UIText.SynchronizeWithAction_localRepoName);
		bot.comboBox(3).setSelection(MASTER);

		// do not check 'Include local changes'

		// fire action
		bot.button("OK").click();

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

		bot.comboBox(0)
				.setSelection(UIText.SynchronizeWithAction_localRepoName);
		bot.comboBox(1).setSelection(HEAD);

		bot.comboBox(2)
				.setSelection(UIText.SynchronizeWithAction_localRepoName);
		bot.comboBox(3).setSelection(MASTER);

		// include local changes
		bot.checkBox("Include local uncommited changes in comparison").click();

		// fire action
		bot.button("OK").click();

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(2, syncViewTree.getAllItems().length);

		SWTBotTreeItem[] syncItems = syncViewTree.getAllItems();
		assertEquals(PROJ1, syncItems[0].getText());
		assertEquals(PROJ1, syncItems[1].getText());

		syncItems[0].expand();
		syncItems[1].expand();

		assertEquals(1, syncItems[0].getNodes().size());
		assertEquals(FILE1, syncItems[0].getNodes().get(0));
		assertEquals(1, syncItems[1].getNodes().size());
		assertEquals(FILE2, syncItems[1].getNodes().get(0));
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

		bot.comboBox(0)
				.setSelection(UIText.SynchronizeWithAction_localRepoName);
		bot.comboBox(1).setSelection("v0.0");

		bot.comboBox(2)
				.setSelection(UIText.SynchronizeWithAction_localRepoName);
		bot.comboBox(3).setSelection(HEAD);

		// fire action
		bot.button("OK").click();

		// wait for synchronization process finish
		bot.sleep(1000);

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
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

		bot.comboBox(0)
				.setSelection(UIText.SynchronizeWithAction_localRepoName);
		bot.comboBox(1).setSelection("v0.1");

		bot.comboBox(2)
				.setSelection(UIText.SynchronizeWithAction_localRepoName);
		bot.comboBox(3).setSelection("v0.2");

		// fire action
		bot.button("OK").click();

		// wait for synchronization process finish
		bot.sleep(1000);

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(1, syncViewTree.getAllItems().length);
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
		bot.menu("Window").click().menu("Preferences").click();
		bot.shell("Preferences").activate();
		bot.tree().getTreeItem("Team").expand().select();
		SWTBotRadio syncPerspectiveCheck = bot.radio("Never");
		if (!syncPerspectiveCheck.isSelected())
			syncPerspectiveCheck.click();

		bot.button("OK").click();

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

	private void resetRepository(String projectName) {
		showDialog(projectName, "Team", "Reset...");

		bot.shell(UIText.ResetCommand_WizardTitle).bot().activeShell();
		bot.radio(UIText.ResetTargetSelectionDialog_ResetTypeHardButton)
				.click();
		bot.button(UIText.ResetTargetSelectionDialog_ResetButton).click();

		bot.shell(UIText.ResetTargetSelectionDialog_ResetQuestion).bot()
				.activeShell();
		bot.button("Yes").click();

	}

	private void createTag(String projectName, String tagName) {
		showDialog(projectName, "Team", "Tag...");

		bot.shell("Create new tag").bot().activeShell();
		bot.text(0).setFocus();
		bot.text(0).setText(tagName);
		bot.text(1).setFocus();
		bot.text(1).setText(tagName);
		bot.button("OK").click();
	}

	private void makeChangesAndCommit(String projectName) throws Exception {
		changeFilesInProject();

		showDialog(projectName, "Team", UIText.CommitAction_commit);

		bot.shell(UIText.CommitDialog_CommitChanges).bot().activeShell();
		bot.styledText(0).setText("test commit");
		bot.button(UIText.CommitDialog_Commit).click();
	}

	private void showDialog(String projectName, String... cmd) {
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
