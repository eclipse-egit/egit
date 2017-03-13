/*******************************************************************************
 * Copyright (C) 2010-2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.view.synchronize;

import static org.eclipse.egit.ui.internal.UIText.GitModelWorkingTree_workingTree;
import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.Constants.MASTER;
import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_TAGS;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.allOf;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withRegex;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.common.CompareEditorTester;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.synchronize.GitChangeSetModelProvider;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class SynchronizeViewGitChangeSetModelTest extends
		AbstractSynchronizeViewTest {

	@Before
	public void setUpEnabledModelProvider() {
		setEnabledModelProvider(GitChangeSetModelProvider.ID);
	}

	@Test
	public void shouldReturnNoChanges() throws Exception {
		// given
		changeFilesInProject();

		// when
		launchSynchronization(HEAD, R_HEADS + MASTER, false);

		// then
		SWTBot viewBot = bot.viewById(ISynchronizeView.VIEW_ID).bot();
		@SuppressWarnings("unchecked")
		Matcher matcher = allOf(widgetOfType(Label.class),
				withRegex("No changes in .*"));

		@SuppressWarnings("unchecked")
		SWTBotLabel l = new SWTBotLabel((Label) viewBot.widget(matcher));
		assertNotNull(l);
	}

	@Test
	public void shouldReturnListOfChanges() throws Exception {
		// given
		changeFilesInProject();

		// when
		launchSynchronization(HEAD, HEAD, true);

		// then
		SWTBotTreeItem workingTreeItem = getExpandedWorkingTreeItem();
		assertTrue(workingTreeItem.getText().endsWith(GitModelWorkingTree_workingTree));
	}

	@Test
	public void shouldCompareBranchAgainstTag() throws Exception {
		// given
		makeChangesAndCommit(PROJ1);

		// when
		launchSynchronization(INITIAL_TAG, HEAD, false);

		// then
		SWTBotTreeItem changeSetTreeItem = getChangeSetTreeItem();
		assertEquals(1, changeSetTreeItem.getItems().length);
	}

	@Test
	public void shouldCompareTagAgainstTag() throws Exception {
		// given
		makeChangesAndCommit(PROJ1);
		createTag("v0.1");

		// when
		launchSynchronization(INITIAL_TAG, R_TAGS + "v0.1", false);

		// then
		SWTBotTreeItem changeSetTreeItem = getChangeSetTreeItem();
		assertEquals(1, changeSetTreeItem.getItems().length);
	}

	@Test
	public void shouldOpenCompareEditor() throws Exception {
		// given
		changeFilesInProject();

		// when
		launchSynchronization(HEAD, INITIAL_TAG, true);

		// then
		CompareEditorTester compare = getCompareEditorForFileInGitChangeSet(
				FILE1, true);
		assertNotNull(compare);
	}

	@Test
	public void shouldListFileDeletedChange() throws Exception {
		// given
		deleteFileAndCommit(PROJ1);

		// when
		launchSynchronization(HEAD, HEAD + "~1", true);

		// then
		SWTBotTreeItem changeSetTreeItem = getChangeSetTreeItem();
		assertEquals(1, changeSetTreeItem.getItems().length);

		SWTBotTreeItem commitTree = waitForNodeWithText(changeSetTreeItem,
				TEST_COMMIT_MSG);
		SWTBotTreeItem projectTree = waitForNodeWithText(commitTree, PROJ1);
		assertEquals(1, projectTree.getItems().length);

		SWTBotTreeItem folderTree = waitForNodeWithText(projectTree, FOLDER);
		assertEquals(1, folderTree.getItems().length);

		SWTBotTreeItem fileTree = folderTree.getItems()[0];
		assertEquals("test.txt", fileTree.getText());
	}

	@Test
	public void shouldSynchronizeInEmptyRepository() throws Exception {
		// given
		createEmptyRepository();

		// when
		launchSynchronization(EMPTY_PROJECT, "", "", true);

		// then
		SWTBotTreeItem workingTree = getExpandedWorkingTreeItem();
		SWTBotTreeItem projectTree = waitForNodeWithText(workingTree,
				EMPTY_PROJECT);
		assertEquals(2, projectTree.getItems().length);

		SWTBotTreeItem folderTree = waitForNodeWithText(projectTree, FOLDER);
		assertEquals(2, folderTree.getItems().length);

		SWTBotTreeItem fileTree = folderTree.getItems()[0];
		assertEquals(FILE1, fileTree.getText());
		fileTree = folderTree.getItems()[1];
		assertEquals(FILE2, fileTree.getText());
	}

	@Test
	public void shouldExchangeCompareEditorSidesBetweenIncomingAndOutgoingChanges()
			throws Exception {
		// given
		makeChangesAndCommit(PROJ1);

		// compare HEAD against tag
		launchSynchronization(HEAD, INITIAL_TAG, false);

		CompareEditorTester outgoingCompare = getCompareEditorForFileInGitChangeSet(
				FILE1, false);
		// save left value from compare editor
		String outgoingLeft = outgoingCompare.getLeftEditor().getText();
		// save right value from compare editor
		String outgoingRight = outgoingCompare.getRightEditor().getText();
		outgoingCompare.close();

		assertNotSame("Text from SWTBot widgets was the same", outgoingLeft, outgoingRight);

		// when
		// compare tag against HEAD
		launchSynchronization(INITIAL_TAG, HEAD, false);

		// then
		CompareEditorTester incomingComp = getCompareEditorForFileInGitChangeSet(
				FILE1, false);
		// right side from compare editor should be equal with left
		assertThat(outgoingLeft, equalTo(incomingComp.getRightEditor().getText()));
		// left side from compare editor should be equal with right
		assertThat(outgoingRight, equalTo(incomingComp.getLeftEditor().getText()));
	}

	@Test
	public void shouldNotShowIgnoredFiles()
			throws Exception {
		// given
		String ignoredName = "to-be-ignored.txt";

		IProject proj = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);

		IFile ignoredFile = proj.getFile(ignoredName);
		ignoredFile.create(new ByteArrayInputStream("content of ignored file"
				.getBytes(proj.getDefaultCharset())), false, null);

		IFile gitignore = proj.getFile(".gitignore");
		gitignore.create(
				new ByteArrayInputStream(ignoredName.getBytes(proj
						.getDefaultCharset())), false, null);
		proj.refreshLocal(IResource.DEPTH_INFINITE, null);

		// when
		launchSynchronization(INITIAL_TAG, HEAD, true);

		// then
		// asserts for Git Change Set model
		SWTBotTreeItem workingTree = getExpandedWorkingTreeItem();
		assertEquals(1, workingTree.getItems().length);
		SWTBotTreeItem proj1Node = workingTree.getItems()[0];
		assertEquals(1, proj1Node.getItems().length);
		assertEquals(".gitignore", proj1Node.getItems()[0].getText());
	}

	@Test
	public void shouldShowNonWorkspaceFileInSynchronization()
			throws Exception {
		// given
		String name = "non-workspace.txt";
		File root = new File(getTestDirectory(), REPO1);
		File nonWorkspace = new File(root, name);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(nonWorkspace), "UTF-8"));

		writer.append("file content");
		writer.close();
		// TODO Synchronize currently shows "No changes" when the only thing that has
		// changed is a non-workspace file, so add another change so that this
		// does not happen. When the bug is fixed, this should be removed.
		setTestFileContent("other content");

		// when
		launchSynchronization(INITIAL_TAG, HEAD, true);

		// then
		SWTBotTreeItem workingTree = getExpandedWorkingTreeItem();
		assertEquals(1, workingTree.getNodes(name).size());
	}

	@Test
	public void shouldShowCompareEditorForNonWorkspaceFileFromSynchronization()
			throws Exception {
		// given
		String content = "file content";
		String name = "non-workspace.txt";
		File root = new File(getTestDirectory(), REPO1);
		File nonWorkspace = new File(root, name);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(nonWorkspace), "UTF-8"));
		writer.append(content);
		writer.close();
		// TODO Synchronize currently shows "No changes" when the only thing that has
		// changed is a non-workspace file, so add another change so that this
		// does not happen. When the bug is fixed, this should be removed.
		setTestFileContent("other content");

		// when
		launchSynchronization(INITIAL_TAG, HEAD, true);

		// then
		CompareEditorTester editor = getCompareEditorForNonWorkspaceFileInGitChangeSet(name);

		String left = editor.getLeftEditor().getText();
		String right = editor.getRightEditor().getText();
		assertEquals(content, left);
		assertEquals("", right);
	}

	@Test
	public void shouldStagePartialChangeInCompareEditor() throws Exception {
		// given
		changeFilesInProject();
		launchSynchronization(HEAD, HEAD, true);
		CompareEditorTester compareEditor = getCompareEditorForFileInGitChangeSet(
				FILE1, true);

		// when
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				CommonUtils.runCommand("org.eclipse.compare.copyLeftToRight",
						null);
			}
		});
		compareEditor.save();


		// then file FILE1 should be in index
		Repository repo = lookupRepository(repositoryFile);
		Status status;
		try (Git git = new Git(repo)) {
			status = git.status().call();
		}
		assertThat(Long.valueOf(status.getChanged().size()),
				is(Long.valueOf(1L)));
		assertThat(status.getChanged().iterator().next(),
				is(PROJ1 + "/" + FOLDER + "/" + FILE1));
	}

	@Test
	public void shouldUnStagePartialChangeInCompareEditor() throws Exception {
		// given
		changeFilesInProject();
		launchSynchronization(HEAD, HEAD, true);
		CompareEditorTester compareEditor = getCompareEditorForFileInGitChangeSet(
				FILE1, true);

		// when
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				CommonUtils.runCommand("org.eclipse.compare.copyRightToLeft",
						null);
			}
		});
		compareEditor.save();

		// then file FILE1 should be unchanged in working tree
		Repository repo = lookupRepository(repositoryFile);
		try (Git git = new Git(repo)) {
			Status status = git.status().call();
			assertThat(Long.valueOf(status.getModified().size()),
					is(Long.valueOf(1)));
			assertThat(status.getModified().iterator().next(),
					is(PROJ1 + "/" + FOLDER + "/" + FILE2));
		}
	}

	@Test
	public void shouldRefreshSyncResultAfterWorkspaceChange() throws Exception {
		// given
		String newFileName = "new.txt";
		launchSynchronization(INITIAL_TAG, HEAD, true);
		IProject proj = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);

		// when
		IFile newFile = proj.getFile(newFileName);
		newFile.create(
				new ByteArrayInputStream("content of new file".getBytes(proj
						.getDefaultCharset())), false, null);
		// force refresh
		proj.refreshLocal(IResource.DEPTH_INFINITE, null);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);

		// then
		SWTBotTreeItem workingTree = getExpandedWorkingTreeItem();
		assertTrue(workingTree.getText()
				.endsWith(GitModelWorkingTree_workingTree));
		// WidgetNotFoundException will be thrown when node named 'new.txt' not
		// exists
		assertNotNull(TestUtil.getNode(workingTree.getItems(), PROJ1)
				.getNode(newFileName));
	}

	@Test
	public void shouldRefreshSyncResultAfterRepositoryChange() throws Exception {
		// given
		changeFilesInProject();
		launchSynchronization(HEAD, HEAD, true);

		// preconditions - sync result should contain two uncommitted changes
		SWTBotTreeItem workingTree = getExpandedWorkingTreeItem();
		assertTrue(workingTree.getText().endsWith(GitModelWorkingTree_workingTree));
		assertEquals(2,
				workingTree.getItems()[0].getItems()[0].getItems().length);

		// when
		commit(PROJ1);

		// then - synchronize view should be empty
		SWTBot viewBot = bot.viewById(ISynchronizeView.VIEW_ID).bot();
		@SuppressWarnings("unchecked")
		Matcher matcher = allOf(widgetOfType(Label.class),
				withRegex("No changes in .*"));

		@SuppressWarnings("unchecked")
		SWTBotLabel l = new SWTBotLabel((Label) viewBot.widget(matcher));
		assertNotNull(l);
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

		SWTBotTree repositoriesTree = bot.viewById(RepositoriesView.VIEW_ID)
				.bot()
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
		SWTBotTree syncViewTree = bot.viewById(ISynchronizeView.VIEW_ID).bot().tree();
		assertEquals(8, syncViewTree.getAllItems().length);
	}

	protected SWTBotTreeItem getChangeSetTreeItem() {
		SWTBotTree syncViewTree = bot.viewById(ISynchronizeView.VIEW_ID).bot().tree();
		SWTBotTreeItem changeSetItem = waitForNodeWithText(syncViewTree,
				UIText.GitChangeSetModelProviderLabel);
		return changeSetItem;
	}

	protected CompareEditorTester getCompareEditorForFileInGitChangeSet(
			String fileName,
			boolean includeLocalChanges) {
		SWTBotTreeItem changeSetTreeItem = getChangeSetTreeItem();

		SWTBotTreeItem rootTree;
		if (includeLocalChanges)
			rootTree = waitForNodeWithText(changeSetTreeItem,
					GitModelWorkingTree_workingTree);
		else
			rootTree = waitForNodeWithText(changeSetTreeItem, TEST_COMMIT_MSG);

		SWTBotTreeItem projNode = waitForNodeWithText(rootTree, PROJ1);
		return getCompareEditor(projNode, fileName);
	}

	protected CompareEditorTester getCompareEditorForNonWorkspaceFileInGitChangeSet(
			final String fileName) {
		SWTBotTreeItem changeSetTreeItem = getChangeSetTreeItem();

		SWTBotTreeItem rootTree = waitForNodeWithText(changeSetTreeItem,
					GitModelWorkingTree_workingTree);
		waitForNodeWithText(rootTree, fileName).doubleClick();

		return CompareEditorTester.forTitleContaining(fileName);
	}

	private SWTBotTreeItem getExpandedWorkingTreeItem() {
		SWTBotTreeItem changeSetTreeItem = getChangeSetTreeItem();
		String workingTreeNodeNameString = getWorkingTreeNodeName(changeSetTreeItem);
		SWTBotTreeItem node = changeSetTreeItem.getNode(workingTreeNodeNameString);
		// Full expansion
		return node.doubleClick();
	}

	private String getWorkingTreeNodeName(SWTBotTreeItem changeSetTreeItem) {
		for (SWTBotTreeItem item : changeSetTreeItem.getItems()) {
			String nodeName = item.getText();
			if (nodeName.contains(UIText.GitModelWorkingTree_workingTree))
				return nodeName;
		}

		return UIText.GitModelWorkingTree_workingTree;
	}

}
