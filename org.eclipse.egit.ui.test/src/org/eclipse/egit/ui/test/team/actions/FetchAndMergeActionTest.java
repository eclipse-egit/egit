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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.LocalNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteTrackingNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.JobJoiner;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotPerspective;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Fetch and Team->Merge actions
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class FetchAndMergeActionTest extends LocalRepositoryTestCase {
	private static File repositoryFile;

	private static File childRepositoryFile;

	private static SWTBotPerspective perspective;

	private static String REMOTE_BRANCHES;

	private static String LOCAL_BRANCHES;

	@BeforeClass
	public static void setup() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		childRepositoryFile = createChildRepository(repositoryFile);
		perspective = bot.activePerspective();
		bot.perspectiveById("org.eclipse.pde.ui.PDEPerspective").activate();
		RepositoriesViewLabelProvider provider = GitRepositoriesViewTestUtils
				.createLabelProvider();
		Repository repo = lookupRepository(childRepositoryFile);
		REMOTE_BRANCHES = provider.getText(new RemoteTrackingNode(
				new RepositoryNode(null, repo), repo));
		LOCAL_BRANCHES = provider.getText(new LocalNode(new RepositoryNode(
				null, repo), repo));
	}

	@AfterClass
	public static void shutdown() {
		perspective.activate();
	}

	private String prepare() throws Exception {
		deleteAllProjects();
		shareProjects(repositoryFile);
		Repository repo = lookupRepository(repositoryFile);
		RevWalk rw = new RevWalk(repo);
		ObjectId id = repo.resolve(repo.getFullBranch());
		String commitId = rw.parseCommit(id).name();
		touchAndSubmit(null);
		deleteAllProjects();
		shareProjects(childRepositoryFile);
		waitInUI();
		return commitId;
	}

	@Test
	public void testFetchFromOriginThenMerge() throws Exception {
		String previousCommit = prepare();
		String oldContent = getTestFileContent();
		fetch();

		String uri = lookupRepository(childRepositoryFile).getConfig()
				.getString(ConfigConstants.CONFIG_REMOTE_SECTION, "origin",
						ConfigConstants.CONFIG_KEY_URL);
		SWTBotShell confirm = bot.shell(NLS.bind(
				UIText.FetchResultDialog_title, uri));
		SWTBotTree tree = confirm.bot().tree();
		String branch = tree.getAllItems()[0].getText();
		assertTrue("Wrong result",
				branch.contains(previousCommit.substring(0, 7)));

		confirm.close();

		String newContent = getTestFileContent();
		assertEquals(oldContent, newContent);

		fetch();
		confirm = bot.shell(NLS.bind(UIText.FetchResultDialog_title, uri));
		int count = confirm.bot().tree().rowCount();

		confirm.close();

		assertEquals("Wrong result count", 0, count);

		newContent = getTestFileContent();
		assertEquals(oldContent, newContent);

		SWTBotShell mergeDialog = openMergeDialog();

		mergeDialog.bot().tree().getTreeItem(REMOTE_BRANCHES).expand().getNode(
				"origin/master").select();
		mergeDialog.bot().button(UIText.MergeTargetSelectionDialog_ButtonMerge)
				.click();
		bot.shell(UIText.MergeAction_MergeResultTitle).close();
		newContent = getTestFileContent();
		assertFalse(oldContent.equals(newContent));
	}

	@Test
	public void testMergeSquash() throws Exception {
		prepare();
		String oldContent = getTestFileContent();
		RevCommit oldCommit = getCommitForHead();
		createNewBranch("newBranch", true);
		touchAndSubmit("branch commit #1");
		touchAndSubmit("branch commit #2");
		String branchContent = getTestFileContent();
		checkoutBranch(Constants.MASTER);
		assertEquals(oldContent, getTestFileContent());

		mergeBranch("newBranch", true);

		assertEquals(oldCommit, getCommitForHead());
		assertEquals(branchContent, getTestFileContent());
	}

	private RevCommit getCommitForHead() throws Exception {
		Repository repo = lookupRepository(repositoryFile);
		RevWalk rw = new RevWalk(repo);
		ObjectId id = repo.resolve(repo.getFullBranch());
		return rw.parseCommit(id);
	}

	private void mergeBranch(String branchToMerge, boolean squash) throws Exception {
		SWTBotShell mergeDialog = openMergeDialog();
		mergeDialog.bot().tree().getTreeItem(LOCAL_BRANCHES).expand().getNode(branchToMerge).select();
		if (squash)
			mergeDialog.bot().radio(UIText.MergeTargetSelectionDialog_MergeTypeSquashButton).click();
		mergeDialog.bot().button(UIText.MergeTargetSelectionDialog_ButtonMerge).click();
		bot.shell(UIText.MergeAction_MergeResultTitle).close();
	}

	private void createNewBranch(String newBranch, boolean checkout) {
		SWTBotShell newBranchDialog = openCreateBranchDialog();
		newBranchDialog.bot().comboBoxWithId("BaseBranch").setSelection(0);
		newBranchDialog.bot().textWithId("BranchName").setText(newBranch);
		if (!checkout)
			newBranchDialog.bot().checkBox(UIText.CreateBranchPage_CheckoutButton).deselect();
		newBranchDialog.bot().button(IDialogConstants.FINISH_LABEL).click();
	}

	private void fetch() throws Exception {
		SWTBotShell fetchDialog = openFetchDialog();
		fetchDialog.bot().button(IDialogConstants.NEXT_LABEL).click();
		JobJoiner jobJoiner = JobJoiner.startListening(JobFamilies.FETCH, 20, TimeUnit.SECONDS);
		fetchDialog.bot().button(IDialogConstants.FINISH_LABEL).click();
		jobJoiner.join();
	}

	private SWTBotShell openFetchDialog() throws Exception {
		SWTBotTree projectExplorerTree = bot.viewById(
				"org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String menuString = util.getPluginLocalizedValue("FetchAction_label");
		String submenuString = util
				.getPluginLocalizedValue("RemoteSubMenu.label");
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
				submenuString, menuString);
		SWTBotShell dialog = bot.shell(UIText.FetchWizard_windowTitleDefault);
		return dialog;
	}

	private SWTBotShell openMergeDialog() throws Exception {
		SWTBotTree projectExplorerTree = bot.viewById(
				"org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String menuString = util.getPluginLocalizedValue("MergeAction_label");
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
				menuString);
		Repository repo = lookupRepository(childRepositoryFile);
		SWTBotShell dialog = bot.shell(NLS.bind(
				UIText.MergeTargetSelectionDialog_TitleMergeWithBranch,
				repo.getBranch()));
		return dialog;
	}

	private SWTBotShell openCreateBranchDialog() {
		SWTBotTree projectExplorerTree = bot
				.viewById("org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String[] menuPath = new String[] {
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("SwitchToMenu.label"),
				UIText.SwitchToMenu_NewBranchMenuLabel };
		ContextMenuHelper.clickContextMenu(projectExplorerTree, menuPath);
		SWTBotShell dialog = bot
				.shell(UIText.CreateBranchWizard_NewBranchTitle);
		return dialog;
	}

	private void checkoutBranch(String branchToCheckout) {
		SWTBotTree projectExplorerTree = bot
				.viewById("org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String[] menuPath = new String[] {
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("SwitchToMenu.label"),
				branchToCheckout };
		JobJoiner jobJoiner = JobJoiner.startListening(JobFamilies.CHECKOUT, 60, TimeUnit.SECONDS);
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, menuPath);
		jobJoiner.join();
	}
}
