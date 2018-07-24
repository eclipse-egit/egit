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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.JobJoiner;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Fetch and Team->Merge actions
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class FetchAndMergeActionTest extends LocalRepositoryTestCase {
	private File repositoryFile;

	private File childRepositoryFile;

	private String REMOTE_BRANCHES;

	private String LOCAL_BRANCHES;

	private String initialCommitId;

	@Before
	public void setup() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		childRepositoryFile = createChildRepository(repositoryFile);
		Repository repo = lookupRepository(childRepositoryFile);
		REMOTE_BRANCHES = UIText.RepositoriesViewLabelProvider_RemoteTrackingNodetext;
		LOCAL_BRANCHES = UIText.RepositoriesViewLabelProvider_LocalNodetext;
		ObjectId id = repo.resolve(repo.getFullBranch());
		initialCommitId = id.name();
	}

	@Test
	public void testFetchFromOriginThenMerge() throws Exception {
		touchAndSubmit(null);
		deleteAllProjects();
		shareProjects(childRepositoryFile);

		String oldContent = getTestFileContent();
		fetch();
		final String title = NLS.bind(UIText.FetchResultDialog_title,
				childRepositoryFile.getParentFile().getName() + " - origin");
		bot.waitUntil(Conditions.shellIsActive(title));

		SWTBotShell confirm = bot.shell(title);
		SWTBotTree tree = confirm.bot().tree();
		String branch = tree.getAllItems()[0].getText();
		assertTrue("Wrong result",
				branch.contains(initialCommitId.substring(0, 7)));

		confirm.close();

		String newContent = getTestFileContent();
		assertEquals(oldContent, newContent);

		fetch();
		bot.waitUntil(Conditions.shellIsActive(title));
		confirm = bot.shell(title);
		int count = confirm.bot().tree().rowCount();

		confirm.close();

		assertEquals("Wrong result count", 0, count);

		newContent = getTestFileContent();
		assertEquals(oldContent, newContent);

		SWTBotShell mergeDialog = openMergeDialog();

		SWTBotTreeItem remoteBranches = TestUtil.expandAndWait(
				mergeDialog.bot().tree().getTreeItem(REMOTE_BRANCHES));
		TestUtil.getChildNode(remoteBranches, "origin/master").select();
		mergeDialog.bot().button(UIText.MergeTargetSelectionDialog_ButtonMerge)
				.click();
		bot.shell(UIText.MergeAction_MergeResultTitle).close();
		newContent = getTestFileContent();
		assertFalse(oldContent.equals(newContent));
	}

	@Test
	public void testMergeSquash() throws Exception {
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
		try (RevWalk rw = new RevWalk(repo)) {
			ObjectId id = repo.resolve(repo.getFullBranch());
			return rw.parseCommit(id);
		}
	}

	private void mergeBranch(String branchToMerge, boolean squash) throws Exception {
		SWTBotShell mergeDialog = openMergeDialog();
		TestUtil.navigateTo(mergeDialog.bot().tree(),
				new String[] { LOCAL_BRANCHES, branchToMerge }).select();
		if (squash)
			mergeDialog.bot().radio(UIText.MergeTargetSelectionDialog_MergeTypeSquashButton).click();
		mergeDialog.bot().button(UIText.MergeTargetSelectionDialog_ButtonMerge).click();
		bot.shell(UIText.MergeAction_MergeResultTitle).close();
	}

	private void createNewBranch(String newBranch, boolean checkout) {
		SWTBotShell newBranchDialog = openCreateBranchDialog();
		newBranchDialog.bot().textWithId("BranchName").setText(newBranch);
		if (!checkout)
			newBranchDialog.bot().checkBox(UIText.CreateBranchPage_CheckoutButton).deselect();
		newBranchDialog.bot().button(IDialogConstants.FINISH_LABEL).click();
	}

	private void fetch() throws Exception {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String menuString = util
				.getPluginLocalizedValue("FetchFromUpstreamAction.label");
		JobJoiner jobJoiner = JobJoiner.startListening(JobFamilies.FETCH, 20, TimeUnit.SECONDS);
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
				menuString);
		TestUtil.openJobResultDialog(jobJoiner.join());
	}

	private SWTBotShell openMergeDialog() throws Exception {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
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
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
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
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
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
