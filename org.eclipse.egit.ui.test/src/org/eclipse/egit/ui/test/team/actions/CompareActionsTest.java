/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
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

import static org.eclipse.jface.dialogs.MessageDialogWithToggle.NEVER;
import static org.eclipse.team.internal.ui.IPreferenceIds.SYNCHRONIZING_COMPLETE_PERSPECTIVE;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.op.ResetOperation;
import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.TagsNode;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.JobJoiner;
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
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Compare With actions
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class CompareActionsTest extends LocalRepositoryTestCase {
	private File repositoryFile;

	private static String TAGS;
	private static ObjectId commitOfTag;

	@Before
	public void setup() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		Repository repo = lookupRepository(repositoryFile);

		disablePerspectiveSwitchPrompt();

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
	}

	@SuppressWarnings("restriction")
	private static void disablePerspectiveSwitchPrompt() {
		// disable perspective synchronize selection
		TeamUIPlugin.getPlugin().getPreferenceStore()
				.setValue(SYNCHRONIZING_COMPLETE_PERSPECTIVE, NEVER);
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.SYNC_VIEW_FETCH_BEFORE_LAUNCH, false);
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

		JobJoiner jobJoiner = JobJoiner.startListening(
				ISynchronizeManager.FAMILY_SYNCHRONIZE_OPERATION, 60,
				TimeUnit.SECONDS);
		dialog.bot().button(IDialogConstants.OK_LABEL).click();
		jobJoiner.join();

		closeFirstEmptySynchronizeDialog();

		assertSynchronizeNoChange();

		// use the second (previous) -> should have a change
		dialog = openCompareWithDialog(compareWithCommitMenuText, dialogTitle);
		dialog.bot().table().select(1);

		jobJoiner = JobJoiner.startListening(
				ISynchronizeManager.FAMILY_SYNCHRONIZE_OPERATION, 60,
				TimeUnit.SECONDS);
		dialog.bot().button(IDialogConstants.OK_LABEL).click();
		jobJoiner.join();

		assertSynchronizeFile1Changed();
	}

	@Test
	public void testCompareWithRef() throws Exception {
		String compareWithRefActionLabel = util
				.getPluginLocalizedValue("CompareWithBranchOrTagAction.label");
		String dialogTitle = UIText.CompareTargetSelectionDialog_WindowTitle;
		SWTBotShell dialog = openCompareWithDialog(compareWithRefActionLabel,
				dialogTitle);

		// use the default (the last commit) -> no changes
		JobJoiner jobJoiner = JobJoiner.startListening(
				ISynchronizeManager.FAMILY_SYNCHRONIZE_OPERATION, 60,
				TimeUnit.SECONDS);
		dialog.bot().button(UIText.CompareTargetSelectionDialog_CompareButton)
				.click();
		jobJoiner.join();

		closeFirstEmptySynchronizeDialog();

		assertSynchronizeNoChange();

		// use the tag -> should have a change
		dialog = openCompareWithDialog(compareWithRefActionLabel, dialogTitle);
		SWTBotTreeItem tags = TestUtil
				.expandAndWait(dialog.bot().tree().getTreeItem(TAGS));
		TestUtil.getChildNode(tags, "SomeTag").select();

		jobJoiner = JobJoiner.startListening(
				ISynchronizeManager.FAMILY_SYNCHRONIZE_OPERATION, 60,
				TimeUnit.SECONDS);
		dialog.bot().button(UIText.CompareTargetSelectionDialog_CompareButton)
				.click();
		jobJoiner.join();

		assertSynchronizeFile1Changed();
	}

	@Test
	public void testCompareWithPrevious() throws Exception {
		String menuLabel = util
				.getPluginLocalizedValue("CompareWithPreviousAction.label");
		clickCompareWithAndWaitForSync(menuLabel);

		closeFirstEmptySynchronizeDialog();

		assertSynchronizeFile1Changed();
	}

	@Test
	public void testCompareWithPreviousWithMerge() throws Exception {
		Repository repo = lookupRepository(repositoryFile);

		try (Git git = new Git(repo)) {
			ObjectId masterId = repo.resolve("refs/heads/master");
			Ref newBranch = git.checkout().setCreateBranch(true)
					.setStartPoint(commitOfTag.name()).setName("toMerge")
					.call();
			ByteArrayInputStream bis = new ByteArrayInputStream(
					"Modified".getBytes("UTF-8"));
			ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
					.getFolder(FOLDER).getFile(FILE2)
					.setContents(bis, false, false, null);
			bis.close();
			git.commit().setAll(true).setMessage("To be merged").call();
			git.merge().include(masterId).call();
			String menuLabel = util
					.getPluginLocalizedValue("CompareWithPreviousAction.label");
			SWTBotShell selectDialog = openCompareWithDialog(menuLabel,
					UIText.CommitSelectDialog_WindowTitle);
			assertEquals(2, selectDialog.bot().table().rowCount());
			selectDialog.close();
			// cleanup: checkout again master and delete merged branch
			git.checkout().setName("refs/heads/master").call();
			git.branchDelete().setBranchNames(newBranch.getName())
					.setForce(true).call();
		}
	}

	@Test
	public void testCompareWithIndex() throws Exception {
		String compareWithIndexActionLabel = util
				.getPluginLocalizedValue("CompareWithIndexAction_label");
		clickCompareWith(compareWithIndexActionLabel);

		// compare with index should not have any changes
		assertTreeCompareNoChange();
		// change test file -> should have one change
		setTestFileContent("Hello there");

		clickCompareWith(compareWithIndexActionLabel);

		assertTreeCompareChanges(1);

		// add to index -> no more changes
		try (Git git = new Git(lookupRepository(repositoryFile))) {
			git.add().addFilepattern(PROJ1 + "/" + FOLDER + "/" + FILE1).call();
		}

		clickCompareWith(compareWithIndexActionLabel);

		assertTreeCompareNoChange();

		// reset -> there should be no more changes
		ResetOperation rop = new ResetOperation(
				lookupRepository(repositoryFile), "refs/heads/master",
				ResetType.HARD);
		rop.execute(new NullProgressMonitor());

		clickCompareWith(compareWithIndexActionLabel);

		assertTreeCompareNoChange();
	}

	@Test
	public void testCompareWithHead() throws Exception {
		String compareWithHeadMenuLabel = util
				.getPluginLocalizedValue("CompareWithHeadAction_label");
		clickCompareWithAndWaitForSync(compareWithHeadMenuLabel);
		closeFirstEmptySynchronizeDialog();
		assertSynchronizeNoChange();

		// change test file -> should have one change
		setTestFileContent("Hello there");

		clickCompareWithAndWaitForSync(compareWithHeadMenuLabel);

		assertSynchronizeFile1Changed();

		// add to index -> should still show as change
		try (Git git = new Git(lookupRepository(repositoryFile))) {
			git.add().addFilepattern(PROJ1 + "/" + FOLDER + "/" + FILE1).call();
		}

		clickCompareWithAndWaitForSync(compareWithHeadMenuLabel);

		assertSynchronizeFile1Changed();

		// reset -> there should be no more changes
		ResetOperation rop = new ResetOperation(
				lookupRepository(repositoryFile), "refs/heads/master",
				ResetType.HARD);
		rop.execute(new NullProgressMonitor());

		clickCompareWithAndWaitForSync(compareWithHeadMenuLabel);

		assertSynchronizeNoChange();
	}

	private void clickCompareWith(String menuLabel) {
		SWTBotTree projectExplorerTree = selectProjectItem();
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Compare With",
				menuLabel);
	}

	private void clickCompareWithAndWaitForSync(String menuLabel) {
		JobJoiner jobJoiner = JobJoiner.startListening(
				ISynchronizeManager.FAMILY_SYNCHRONIZE_OPERATION, 60,
				TimeUnit.SECONDS);
		clickCompareWith(menuLabel);
		jobJoiner.join();
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
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		return projectExplorerTree;
	}

	private SWTBotTree waitUntilCompareTreeViewTreeHasNodeCount(int nodeCount) {
		SWTBotTree tree = bot.viewById(CompareTreeView.ID).bot().tree();
		bot.waitUntil(Conditions.treeHasRows(tree, nodeCount), 10000);
		return tree;
	}

	/**
	 * On the very first synchronization with no result, Team will display a
	 * modal dialog. This aims at closing it if visible.
	 */
	private void closeFirstEmptySynchronizeDialog() {
		// Do not use bot.shell(String) : we don't want to fail if not present.
		SWTBotShell[] shells = bot.shells();
		for (int i = 0; i < shells.length; i++) {
			SWTBotShell shell = shells[i];
			if ("Synchronize Complete - Git".equals(shell.getText()))
				shell.close();
		}
	}

	private void assertSynchronizeNoChange() {
		// 0 => title, 1 => ?, 2 => "no result" Label
		SWTBotLabel syncViewLabel = bot.viewById(ISynchronizeView.VIEW_ID).bot()
				.label(0);

		String noResultLabel = syncViewLabel.getText();
		String expected = "No changes in 'Git (" + PROJ1 + ")'.";
		if (!noResultLabel.contains(expected)) {
			syncViewLabel = bot.viewById(ISynchronizeView.VIEW_ID).bot().label(2);
			noResultLabel = syncViewLabel.getText();
			assertTrue(noResultLabel.contains(expected));
		}
	}

	private void assertSynchronizeFile1Changed() {
		SWTBotTree syncViewTree = bot.viewById(ISynchronizeView.VIEW_ID).bot().tree();
		SWTBotTreeItem[] syncItems = syncViewTree.getAllItems();
		assertEquals(syncItems.length, 1);
		String text = syncItems[0].getText();
		assertTrue("Received unexpected text: " + text, text.contains(PROJ1));

		TestUtil.expandAndWait(syncItems[0]);
		SWTBotTreeItem[] level1Children = syncItems[0].getItems();
		assertEquals(level1Children.length, 1);
		assertTrue(level1Children[0].getText().contains(FOLDER));

		TestUtil.expandAndWait(level1Children[0]);
		SWTBotTreeItem[] level2Children = level1Children[0].getItems();
		assertEquals(level2Children.length, 1);
		assertTrue(level2Children[0].getText().contains(FILE1));
	}

	private void assertTreeCompareNoChange() {
		SWTBotTree tree = waitUntilCompareTreeViewTreeHasNodeCount(1);
		SWTBotTreeItem[] items = tree.getAllItems();
		assertEquals(1, items.length);
		assertEquals(UIText.CompareTreeView_NoDifferencesFoundMessage,
				items[0].getText());
	}

	private void assertTreeCompareChanges(int nodeCount) {
		SWTBotTree tree = waitUntilCompareTreeViewTreeHasNodeCount(nodeCount);
		SWTBotTreeItem[] items = tree.getAllItems();
		assertThat(items[0].getText(),
				not(UIText.CompareTreeView_NoDifferencesFoundMessage));
	}
}
