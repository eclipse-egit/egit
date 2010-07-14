/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.team.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;

import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.LocalBranchesNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.TagsNode;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tag;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotPerspective;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.TableCollection;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Branch action
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class BranchAndResetActionTest extends LocalRepositoryTestCase {
	private static File repositoryFile;

	private static SWTBotPerspective perspective;

	private static String LOCAL_BRANCHES;

	private static String TAGS;

	@BeforeClass
	public static void setup() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		Repository repo = lookupRepository(repositoryFile);

		Tag tag = new Tag(repo);
		tag.setTag("SomeTag");
		tag.setAuthor(new PersonIdent(TestUtil.TESTAUTHOR));
		tag.setMessage("I'm just a little tag");
		tag.setObjId(repo.resolve(repo.getFullBranch()));
		TagOperation top = new TagOperation(repo, tag, false);
		top.execute(null);
		touchAndSubmit();

		perspective = bot.activePerspective();
		bot.perspectiveById("org.eclipse.pde.ui.PDEPerspective").activate();
		RepositoriesViewLabelProvider provider = new RepositoriesViewLabelProvider();
		LOCAL_BRANCHES = provider.getText(new LocalBranchesNode(
				new RepositoryNode(null, repo), repo));
		TAGS = provider.getText(new TagsNode(new RepositoryNode(null, repo),
				repo));
	}

	@AfterClass
	public static void shutdown() {
		perspective.activate();
	}

	@Before
	public void prepare() throws Exception {
		Repository repo = lookupRepository(repositoryFile);
		if (!repo.getBranch().equals("master")) {
			BranchOperation bop = new BranchOperation(repo, "refs/heads/master");
			bop.execute(null);
		}
	}

	@Test
	public void testCheckoutLocalBranches() throws Exception {
		checkoutAndVerify(new String[] { LOCAL_BRANCHES, "master" },
				new String[] { LOCAL_BRANCHES, "stable" });
		checkoutAndVerify(new String[] { LOCAL_BRANCHES, "stable" },
				new String[] { LOCAL_BRANCHES, "master" });
	}

	@Test
	public void testResetToLocalBranch() throws Exception {
		checkoutAndVerify(new String[] { LOCAL_BRANCHES, "master" },
				new String[] { LOCAL_BRANCHES, "stable" });
		String stable = getTestFileContent();
		checkoutAndVerify(new String[] { LOCAL_BRANCHES, "stable" },
				new String[] { LOCAL_BRANCHES, "master" });
		String master = getTestFileContent();
		assertFalse(stable.equals(master));
		SWTBotShell resetDialog = openResetDialog();
		resetDialog.bot().tree().getTreeItem(LOCAL_BRANCHES).getNode("stable")
				.select();
		activateItemByKeyboard(resetDialog,
				UIText.ResetTargetSelectionDialog_ResetTypeHardButton);

		resetDialog.bot().button(UIText.ResetTargetSelectionDialog_ResetButton)
				.click();

		bot.shell(UIText.ResetTargetSelectionDialog_ResetQuestion).bot()
				.button(IDialogConstants.YES_LABEL).click();

		String reset = getTestFileContent();
		assertEquals("Wrong content after reset", stable, reset);
	}

	@Test
	public void testCreateBranch() throws Exception {
		SWTBotShell dialog = openBranchDialog();
		dialog.bot().button(UIText.BranchSelectionDialog_NewBranch).click();
		SWTBotShell branchNameDialog = bot
				.shell(UIText.BranchSelectionDialog_QuestionNewBranchTitle);
		branchNameDialog.bot().text().setText("master");
		assertFalse(branchNameDialog.bot().button(IDialogConstants.OK_LABEL)
				.isEnabled());
		branchNameDialog.bot().text().setText("NewBranch");
		branchNameDialog.bot().button(IDialogConstants.OK_LABEL).click();

		assertEquals("New Branch should be selected", "NewBranch", bot.tree()
				.selection().get(0, 0));
		bot.button(UIText.BranchSelectionDialog_OkCheckout).click();

		assertEquals("New Branch should be checked out", "NewBranch",
				lookupRepository(repositoryFile).getBranch());
	}

	private SWTBotShell openBranchDialog() {
		SWTBotTree projectExplorerTree = bot.viewById(
				"org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String menuString = util.getPluginLocalizedValue("BranchAction_label");
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
				menuString);
		SWTBotShell dialog = bot.shell(NLS.bind(
				UIText.BranchSelectionDialog_TitleCheckout, repositoryFile
						.toString()));
		return dialog;
	}

	private SWTBotShell openResetDialog() {
		SWTBotTree projectExplorerTree = bot.viewById(
				"org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String menuString = util.getPluginLocalizedValue("ResetAction_label");
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
				menuString);
		SWTBotShell dialog = bot
				.shell(UIText.ResetTargetSelectionDialog_WindowTitle);
		return dialog;
	}

	@Test
	public void testRenameBranch() throws Exception {
		SWTBotShell dialog = openBranchDialog();
		dialog.bot().button(UIText.BranchSelectionDialog_NewBranch).click();
		SWTBotShell branchNameDialog = bot
				.shell(UIText.BranchSelectionDialog_QuestionNewBranchTitle);
		branchNameDialog.bot().text().setText("master");
		assertFalse(branchNameDialog.bot().button(IDialogConstants.OK_LABEL)
				.isEnabled());
		branchNameDialog.bot().text().setText("Unrenamed");
		branchNameDialog.bot().button(IDialogConstants.OK_LABEL).click();

		assertEquals("New Branch should be selected", "Unrenamed", bot.tree()
				.selection().get(0, 0));

		bot.button(UIText.BranchSelectionDialog_Rename).click();

		branchNameDialog = bot
				.shell(UIText.BranchSelectionDialog_QuestionNewBranchTitle);
		assertFalse(branchNameDialog.bot().button(IDialogConstants.OK_LABEL)
				.isEnabled());
		branchNameDialog.bot().text().setText("Renamed");
		bot.button(IDialogConstants.OK_LABEL).click();

		bot.button(UIText.BranchSelectionDialog_OkCheckout).click();

		assertEquals("New Branch should be checked out", "Renamed",
				lookupRepository(repositoryFile).getBranch());
	}

	@Test
	public void testCheckoutTags() throws Exception {
		checkoutAndVerify(new String[] { LOCAL_BRANCHES, "master" },
				new String[] { TAGS, "SomeTag" });
	}

	private void checkoutAndVerify(String[] currentBranch, String[] newBranch)
			throws IOException, Exception {
		SWTBotShell dialog = openBranchDialog();
		TableCollection tc = dialog.bot().tree().selection();
		assertEquals("Wrong selection count", 1, tc.rowCount());
		assertEquals("Wrong item selected", currentBranch[1], tc.get(0, 0));

		dialog.bot().tree().getTreeItem(newBranch[0]).expand().getNode(
				newBranch[1]).select();
		tc = dialog.bot().tree().selection();
		assertEquals("Wrong selection count", 1, tc.rowCount());
		assertEquals("Wrong item selected", newBranch[1], tc.get(0, 0));

		Repository repo = lookupRepository(repositoryFile);

		dialog.bot().button(UIText.BranchSelectionDialog_OkCheckout).click();
		if (ObjectId.isId(repo.getBranch())) {
			String mapped = Activator.getDefault().getRepositoryUtil()
					.mapCommitToRef(repo, repo.getBranch(), false);
			assertEquals("Wrong branch", newBranch[1], mapped.substring(mapped
					.lastIndexOf('/') + 1));
		} else
			assertEquals("Wrong branch", newBranch[1], repo.getBranch());
	}

}
