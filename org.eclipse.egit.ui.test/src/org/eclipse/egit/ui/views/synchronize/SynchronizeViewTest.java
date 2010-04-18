/*******************************************************************************
 * Copyright (C) 2009, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Ketan Padegaonkar <KetanPadegaonkar@gmail.com>
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.views.synchronize;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;
import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellIsActive;
import static org.junit.Assert.assertEquals;

import org.eclipse.egit.ui.common.EGitTesCase;
import org.eclipse.egit.ui.common.GitImportRepoWizard;
import org.eclipse.egit.ui.common.RepoPropertiesPage;
import org.eclipse.egit.ui.common.RepoRemoteBranchesPage;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Before;
import org.junit.Test;

public class SynchronizeViewTest extends EGitTesCase {

	@Test
	public void shouldReturnNoChanges() throws Exception {
		// when
		changeFilesInProject();
		showSynchronizationDialog("org.eclipse.egit.ui");

		// given
		bot.shell("Synchronize repository: egit/.git").activate();

		// select local HEAD as source branch
		bot.comboBox(0).setSelection(0); // select local
		bot.comboBox(1).setSelection(0); // select HEAD
		// select local master as destination branch
		bot.comboBox(2).setSelection(0); // select local
		bot.comboBox(3).setSelection(1); // select master

		// do not check 'Include local changes'

		// fire action
		bot.button("OK").click();

		// handle "Confirm open perspective"
		bot.waitUntil(shellIsActive("Confirm Open Perspective"), 15000);
		bot.button("No").click();

		// wait for dispose "Git Resource Synchronization"
		SWTBotShell gitResSyncShell = bot.shell("Git Resource Synchronization");
		bot.waitUntil(shellCloses(gitResSyncShell), 6000000);

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(0, syncViewTree.getAllItems().length);
	}

	@Test
	public void shouldReturnListOfChnages() throws Exception {
		// when
		changeFilesInProject();
		showSynchronizationDialog("org.eclipse.egit.ui");

		// given
		bot.shell("Synchronize repository: egit/.git").activate();

		// select local HEAD as source branch
		bot.comboBox(0).setSelection(0); // select local
		bot.comboBox(1).setSelection(0); // select HEAD
		// select local master as destination branch
		bot.comboBox(2).setSelection(0); // select local
		bot.comboBox(3).setSelection(1); // select master

		// do not check 'Include local changes'
		bot.checkBox("Include local uncommited changes in comparison").click();

		// fire action
		bot.button("OK").click();

		// handle "Confirm open perspective"
		bot.waitUntil(shellIsActive("Confirm Open Perspective"), 45000);
		bot.button("No").click();

		// wait for dispose "Git Resource Synchronization"
		SWTBotShell shell = bot.shell("Git Resource Synchronization");
		bot.waitUntil(shellCloses(shell), 6000000);

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(2, syncViewTree.getAllItems().length);

		SWTBotTreeItem[] syncItems = syncViewTree.getAllItems();
		assertEquals("org.eclipse.egit.core", syncItems[0].getText());
		assertEquals("org.eclipse.egit.ui", syncItems[1].getText());

		syncItems[0].expand();
		syncItems[1].expand();

		assertEquals(1, syncItems[0].getNodes().size());
		assertEquals("pom.xml", syncItems[0].getNodes().get(0));
		assertEquals(1, syncItems[1].getNodes().size());
		assertEquals("pom.xml", syncItems[1].getNodes().get(0));
	}

	@Before
	public void setupViews() {
		bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();
		bot.viewByTitle("Package Explorer").show();

		// import egit project if it isn't already imported
		if (bot.viewByTitle("Package Explorer").bot().tree().rowCount() < 8) {
			GitImportRepoWizard importWizard = new GitImportRepoWizard();

			importWizard.openWizard();
			addRepository(importWizard, "git://egit.eclipse.org/egit.git");

			importWizard.selectAndCloneRepository(0);
			importWizard.waitForCreate();
			waitForWorkspaceRefresh();
		}
	}

	private void addRepository(GitImportRepoWizard importWizard, String repoUrl) {
		RepoPropertiesPage propertiesPage = importWizard.openCloneWizard();

		RepoRemoteBranchesPage remoteBranches = propertiesPage
				.nextToRemoteBranches(repoUrl);
		remoteBranches.selectBranches("master");

		remoteBranches.nextToWorkingCopy().waitForCreate();
	}

	private void changeFilesInProject() throws Exception {
		SWTBot packageExplorerBot = bot.viewByTitle("Package Explorer").bot();
		packageExplorerBot.activeShell();
		SWTBotTree tree = packageExplorerBot.tree();

		SWTBotTreeItem coreTreeItem = tree.getAllItems()[3];
		coreTreeItem.expand();
		for (String node : coreTreeItem.getNodes()) {
			if (node.contains("pom.xml")) {
				coreTreeItem.getNode(node).doubleClick();
				break;
			}
		}

		SWTBotEditor corePomEditor = bot.editorByTitle("pom.xml");
		corePomEditor.toTextEditor()
				.insertText("<!-- EGit jUnit test case -->");
		corePomEditor.saveAndClose();
		coreTreeItem.collapse();

		SWTBotTreeItem uiTreeItem = tree.getAllItems()[6];
		uiTreeItem.expand();
		for (String node : uiTreeItem.getNodes()) {
			if (node.contains("pom.xml")) {
				uiTreeItem.getNode(node).doubleClick();
				break;
			}
		}

		SWTBotEditor uiPomEditor = bot.editorByTitle("pom.xml");
		uiPomEditor.toTextEditor().insertText("<!-- EGit jUnit test case -->");
		uiPomEditor.saveAndClose();
		uiTreeItem.collapse();
	}

	private void showSynchronizationDialog(String projectName) {
		SWTBot packageExplorerBot = bot.viewByTitle("Package Explorer").bot();
		packageExplorerBot.activeShell();
		SWTBotTree tree = packageExplorerBot.tree();

		// EGit adds some 'decorators' for Package Explorer project's name. That
		// 'decorators' indicate does there are any uncommited changes in
		// project ('>' prefix in project name) and adds repository name and
		// current branch name ('[<repo name> <branch name>]' suffix). To bypass
		// such inconvenience we use here this for-if-contains loop
		for (SWTBotTreeItem item : tree.getAllItems()) {
			if (item.getText().contains(projectName)) {
				item.select();
				break;
			}
		}

		ContextMenuHelper.clickContextMenu(tree, "Team", "Synchronize...");
	}

}
