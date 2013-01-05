/*******************************************************************************
 * Copyright (C) 2010-2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.view.synchronize;

import static org.eclipse.egit.ui.UIText.GitModelWorkingTree_workingTree;
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
import java.io.FileWriter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.synchronize.GitChangeSetModelProvider;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
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
		SWTBot viewBot = bot.viewByTitle("Synchronize").bot();
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
		SWTBot compare = getCompareEditorForFileInGitChangeSet(FILE1, true)
				.bot();
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

		SWTBotEditor outgoingCompare = getCompareEditorForFileInGitChangeSet(
				FILE1, false);
		SWTBot outgoingCompareBot = outgoingCompare.bot();
		// save left value from compare editor
		String outgoingLeft = outgoingCompareBot.styledText(0).getText();
		// save right value from compare editor
		String outgoingRight = outgoingCompareBot.styledText(1).getText();
		outgoingCompare.close();

		assertNotSame("Text from SWTBot widgets was the same", outgoingLeft, outgoingRight);

		// when
		// compare tag against HEAD
		launchSynchronization(INITIAL_TAG, HEAD, false);

		// then
		SWTBot incomingComp = getCompareEditorForFileInGitChangeSet(
				FILE1, false).bot();
		// right side from compare editor should be equal with left
		assertThat(outgoingLeft, equalTo(incomingComp.styledText(1).getText()));
		// left side from compare editor should be equal with right
		assertThat(outgoingRight, equalTo(incomingComp.styledText(0).getText()));
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
		BufferedWriter writer = new BufferedWriter(new FileWriter(nonWorkspace));
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
		BufferedWriter writer = new BufferedWriter(new FileWriter(nonWorkspace));
		writer.append(content);
		writer.close();
		// TODO Synchronize currently shows "No changes" when the only thing that has
		// changed is a non-workspace file, so add another change so that this
		// does not happen. When the bug is fixed, this should be removed.
		setTestFileContent("other content");

		// when
		launchSynchronization(INITIAL_TAG, HEAD, true);

		// then
		SWTBotEditor editor = getCompareEditorForNonWorkspaceFileInGitChangeSet(name);
		editor.setFocus();

		// the WidgetNotFoundException will be thrown when widget with given content cannot be not found
		SWTBotStyledText left = editor.bot().styledText(content);
		SWTBotStyledText right = editor.bot().styledText("");
		// to be complete sure assert that both sides are not the same
		assertNotSame(left, right);
	}

	@Test
	public void shouldStagePartialChangeInCompareEditor() throws Exception {
		// given
		changeFilesInProject();
		launchSynchronization(HEAD, HEAD, true);
		getCompareEditorForFileInGitChangeSet(FILE1, true).bot();

		// when
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				CommonUtils.runCommand("org.eclipse.compare.copyLeftToRight",
						null);
			}
		});
		bot.activeEditor().save();


		// then file FILE1 should be in index
		FileRepository repo = lookupRepository(repositoryFile);
		Status status = new Git(repo).status().call();
		assertThat(Long.valueOf(status.getChanged().size()),
				is(Long.valueOf(1L)));
		assertThat(status.getChanged().iterator().next(), is(PROJ1 + "/"
				+ FOLDER + "/" + FILE1));
	}

	@Test
	public void shouldUnStagePartialChangeInCompareEditor() throws Exception {
		// given
		changeFilesInProject();
		launchSynchronization(HEAD, HEAD, true);
		getCompareEditorForFileInGitChangeSet(FILE1, true).bot();

		// when
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				CommonUtils.runCommand("org.eclipse.compare.copyRightToLeft",
						null);
			}
		});
		bot.activeEditor().save();

		// then file FILE1 should be unchanged in working tree
		FileRepository repo = lookupRepository(repositoryFile);
		Status status = new Git(repo).status().call();
		assertThat(Long.valueOf(status.getModified().size()),
				is(Long.valueOf(1)));
		assertThat(status.getModified().iterator().next(), is(PROJ1 + "/"
				+ FOLDER + "/" + FILE2));
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
		assertEquals(GitModelWorkingTree_workingTree, workingTree.getText());
		// WidgetNotFoundException will be thrown when node named 'new.txt' not
		// exists
		assertNotNull(workingTree.getNode(PROJ1).expand());
		assertNotNull(workingTree.getNode(PROJ1).getNode(newFileName));
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
		SWTBot viewBot = bot.viewByTitle("Synchronize").bot();
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

	protected SWTBotTreeItem getChangeSetTreeItem() {
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		SWTBotTreeItem changeSetItem = waitForNodeWithText(syncViewTree,
				UIText.GitChangeSetModelProviderLabel);
		return changeSetItem;
	}

	protected SWTBotEditor getCompareEditorForFileInGitChangeSet(
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

	protected SWTBotEditor getCompareEditorForNonWorkspaceFileInGitChangeSet(
			final String fileName) {
		SWTBotTreeItem changeSetTreeItem = getChangeSetTreeItem();

		SWTBotTreeItem rootTree = waitForNodeWithText(changeSetTreeItem,
					GitModelWorkingTree_workingTree);
		waitForNodeWithText(rootTree, fileName).doubleClick();

		SWTBotEditor editor = bot
				.editor(new CompareEditorTitleMatcher(fileName));

		return editor;
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
