/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Chris Aniszczyk <caniszczyk@gmail.com> - tag API changes
 *    Mathias Kinzler (SAP AG) - compare with previous actions
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.internal.op.BranchOperation;
import org.eclipse.egit.core.internal.op.ResetOperation;
import org.eclipse.egit.core.internal.op.TagOperation;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.TagsNode;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotPerspective;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Compare With actions
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class CompareActionsTest extends LocalRepositoryTestCase {
	private static File repositoryFile;

	private static SWTBotPerspective perspective;

	// private static String LOCAL_BRANCHES;
	//
	private static String TAGS;
	private static ObjectId commitOfTag;

	@BeforeClass
	public static void setup() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		Repository repo = lookupRepository(repositoryFile);
		perspective = bot.activePerspective();
		bot.perspectiveById("org.eclipse.pde.ui.PDEPerspective").activate();

		TagBuilder tag = new TagBuilder();
		tag.setTag("SomeTag");
		tag.setTagger(RawParseUtils.parsePersonIdent(TestUtil.TESTAUTHOR));
		tag.setMessage("I'm just a little tag");
		tag.setObjectId(repo.resolve(repo.getFullBranch()),
				Constants.OBJ_COMMIT);
		commitOfTag = tag.getObjectId();
		TagOperation top = new TagOperation(repo, tag, false);
		top.execute(null);
		touchAndSubmit(null);

		RepositoriesViewLabelProvider provider = GitRepositoriesViewTestUtils
				.createLabelProvider();
		// LOCAL_BRANCHES = provider.getText(new LocalNode(new RepositoryNode(
		// null, repo), repo));
		TAGS = provider.getText(new TagsNode(new RepositoryNode(null, repo),
				repo));
		waitInUI();
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
	public void testCompareWithCommit() throws Exception {
		String compareWithCommitMenuText = util
				.getPluginLocalizedValue("CompareWithCommitAction.label");
		String dialogTitle = UIText.CommitSelectionDialog_WindowTitle;
		SWTBotShell dialog = openCompareWithDialog(compareWithCommitMenuText,
				dialogTitle);
		// use the default (the last commit) -> no changes
		assertEquals(3, dialog.bot().table().rowCount());
		dialog.bot().table().select(0);
		dialog.bot().button(IDialogConstants.OK_LABEL).click();
		TestUtil.waitUntilViewWithGivenIdShows(CompareTreeView.ID);
		assertEquals(0, bot.viewById(CompareTreeView.ID).bot().tree()
				.getAllItems().length);
		// use the second (previous) -> should have a change
		dialog = openCompareWithDialog(compareWithCommitMenuText, dialogTitle);
		dialog.bot().table().select(1);
		dialog.bot().button(IDialogConstants.OK_LABEL).click();
		waitUntilCompareTreeViewTreeHasNodeCount(1);
	}

	@Test
	public void testCompareWithRef() throws Exception {
		String compareWithRefActionLabel = util
				.getPluginLocalizedValue("CompareWithBranchOrTagAction.label");
		String dialogTitle = UIText.CompareTargetSelectionDialog_WindowTitle;

		SWTBotShell dialog = openCompareWithDialog(compareWithRefActionLabel,
				dialogTitle);
		// use the default (the last commit) -> no changes
		dialog.bot().button(UIText.CompareTargetSelectionDialog_CompareButton)
				.click();
		waitUntilCompareTreeViewTreeHasNodeCount(0);

		// use the tag -> should have a change
		dialog = openCompareWithDialog(compareWithRefActionLabel, dialogTitle);
		dialog.bot().tree().getTreeItem(TAGS).expand().getNode("SomeTag")
				.select();
		dialog.bot().button(UIText.CompareTargetSelectionDialog_CompareButton)
				.click();
		waitUntilCompareTreeViewTreeHasNodeCount(1);
	}

	@Test
	public void testCompareWithPrevious() throws Exception {
		String menuLabel = util
				.getPluginLocalizedValue("CompareWithPreviousAction.label");
		clickCompareWith(menuLabel);
		waitUntilCompareTreeViewTreeHasNodeCount(1);
	}

	@Test
	public void testCompareWithPreviousWithMerge() throws Exception {
		Repository repo = lookupRepository(repositoryFile);

		Git git = new Git(repo);
		ObjectId masterId = repo.resolve("refs/heads/master");
		Ref newBranch = git.checkout().setCreateBranch(true)
				.setStartPoint(commitOfTag.name()).setName("toMerge").call();
		ByteArrayInputStream bis = new ByteArrayInputStream(
				"Modified".getBytes());
		ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFolder(FOLDER).getFile(FILE2)
				.setContents(bis, false, false, null);
		bis.close();
		git.commit().setAll(true).setMessage("To be merged").call();
		git.merge().include(masterId).call();
		String menuLabel = util
				.getPluginLocalizedValue("CompareWithPreviousAction.label");
		SWTBotShell selectDialog = openCompareWithDialog(menuLabel, UIText.CommitSelectDialog_WindowTitle);
		assertEquals(2, selectDialog.bot().table().rowCount());
		selectDialog.close();
		// cleanup: checkout again master and delete merged branch
		git.checkout().setName("refs/heads/master").call();
		git.branchDelete().setBranchNames(newBranch.getName()).setForce(true).call();
	}

	@Test
	public void testCompareWithIndex() throws Exception {
		String compareWithIndexActionLabel = util
				.getPluginLocalizedValue("CompareWithIndexAction_label");
		clickCompareWith(compareWithIndexActionLabel);

		// compare with index should not have any changes
		assertEquals(0, bot.viewById(CompareTreeView.ID).bot().tree()
				.getAllItems().length);
		// change test file -> should have one change
		setTestFileContent("Hello there");

		clickCompareWith(compareWithIndexActionLabel);

		waitUntilCompareTreeViewTreeHasNodeCount(1);

		// add to index -> no more changes
		new Git(lookupRepository(repositoryFile)).add().addFilepattern(
				PROJ1 + "/" + FOLDER + "/" + FILE1).call();

		clickCompareWith(compareWithIndexActionLabel);

		assertEquals(0, bot.viewById(CompareTreeView.ID).bot().tree()
				.getAllItems().length);

		// reset -> there should be no more changes
		ResetOperation rop = new ResetOperation(
				lookupRepository(repositoryFile), "refs/heads/master",
				ResetType.HARD);
		rop.execute(new NullProgressMonitor());

		clickCompareWith(compareWithIndexActionLabel);
		waitUntilCompareTreeViewTreeHasNodeCount(0);
	}

	@Test
	public void testCompareWithHead() throws Exception {
		String compareWithHeadMenuLabel = util
				.getPluginLocalizedValue("CompareWithHeadAction_label");
		clickCompareWith(compareWithHeadMenuLabel);

		// compare with HEAD should not have any changes
		waitUntilCompareTreeViewTreeHasNodeCount(0);
		// change test file -> should have one change
		setTestFileContent("Hello there");

		clickCompareWith(compareWithHeadMenuLabel);

		waitUntilCompareTreeViewTreeHasNodeCount(1);
		// add to index -> should still show as change
		new Git(lookupRepository(repositoryFile)).add().addFilepattern(
				PROJ1 + "/" + FOLDER + "/" + FILE1).call();

		clickCompareWith(compareWithHeadMenuLabel);

		assertEquals(1, bot.viewById(CompareTreeView.ID).bot().tree()
				.getAllItems().length);

		// reset -> there should be no more changes
		ResetOperation rop = new ResetOperation(
				lookupRepository(repositoryFile), "refs/heads/master",
				ResetType.HARD);
		rop.execute(new NullProgressMonitor());

		clickCompareWith(compareWithHeadMenuLabel);
		waitUntilCompareTreeViewTreeHasNodeCount(0);
	}

	private void clickCompareWith(String menuLabel) {
		SWTBotTree projectExplorerTree = selectProjectItem();
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Compare With",
				menuLabel);
	}

	private SWTBotShell openCompareWithDialog(String menuLabel,
			String dialogTitle) {
		SWTBotTree projectExplorerTree = selectProjectItem();
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Compare With",
				menuLabel);
		SWTBotShell dialog = bot.shell(dialogTitle);
		return dialog;
	}

	private SWTBotTree selectProjectItem() {
		SWTBotTree projectExplorerTree = bot.viewById(
				"org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		return projectExplorerTree;
	}

	private void waitUntilCompareTreeViewTreeHasNodeCount(int nodeCount) {
		SWTBotTree tree = bot.viewById(CompareTreeView.ID).bot().tree();
		bot.waitUntil(Conditions.treeHasRows(tree, nodeCount), 10000);
	}

}
