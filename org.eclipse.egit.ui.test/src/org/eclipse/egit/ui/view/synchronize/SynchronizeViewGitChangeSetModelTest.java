/*******************************************************************************
 * Copyright (C) 2010,2011 Dariusz Luksza <dariusz@luksza.org>
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
import org.junit.Ignore;
import org.junit.Test;

public class SynchronizeViewGitChangeSetModelTest extends
		AbstractSynchronizeViewTest {

	@Test
	public void shouldReturnNoChanges() throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		changeFilesInProject();

		// when
		launchSynchronization(HEAD, R_HEADS + MASTER, false);
		setGitChangeSetPresentationModel();

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
		resetRepositoryToCreateInitialTag();
		changeFilesInProject();

		// when
		launchSynchronization(HEAD, HEAD, true);
		setGitChangeSetPresentationModel();

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		SWTBotTreeItem[] syncItems = syncViewTree.getAllItems();
		assertTrue(syncItems[0].getText().endsWith(GitModelWorkingTree_workingTree));
	}

	@Test
	public void shouldCompareBranchAgainstTag() throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		makeChangesAndCommit(PROJ1);

		// when
		launchSynchronization(INITIAL_TAG, HEAD, false);
		setGitChangeSetPresentationModel();

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(1, syncViewTree.getAllItems().length);
	}

	@Test
	public void shouldCompareTagAgainstTag() throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		makeChangesAndCommit(PROJ1);
		createTag("v0.1");

		// when
		launchSynchronization(INITIAL_TAG, R_TAGS + "v0.1", false);
		setGitChangeSetPresentationModel();

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(1, syncViewTree.getAllItems().length);
	}

	@Test
	public void shouldOpenCompareEditor() throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		changeFilesInProject();

		// when
		launchSynchronization(HEAD, INITIAL_TAG, true);
		setGitChangeSetPresentationModel();

		// then
		SWTBot compare = getCompareEditorForFileInGitChangeSet(FILE1, true)
				.bot();
		assertNotNull(compare);
	}

	@Test public void shouldListFileDeletedChange() throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		deleteFileAndCommit(PROJ1);

		// when
		launchSynchronization(HEAD, HEAD + "~1", true);
		setGitChangeSetPresentationModel();

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(1, syncViewTree.getAllItems().length);

		SWTBotTreeItem commitTree = waitForNodeWithText(syncViewTree,
				TEST_COMMIT_MSG);
		SWTBotTreeItem projectTree = waitForNodeWithText(commitTree, PROJ1);
		assertEquals(1, projectTree.getItems().length);

		SWTBotTreeItem folderTree = waitForNodeWithText(projectTree, FOLDER);
		assertEquals(1, folderTree.getItems().length);

		SWTBotTreeItem fileTree = folderTree.getItems()[0];
		assertEquals("test.txt", fileTree.getText());
	}

	@Test public void shouldSynchronizeInEmptyRepository() throws Exception {
		// given
		createEmptyRepository();

		// when
		launchSynchronization(EMPTY_PROJECT, "", "", true);
		setGitChangeSetPresentationModel();

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		SWTBotTreeItem commitTree = expandWorkingTreeNode(syncViewTree);
		assertEquals(2, syncViewTree.getAllItems().length);
		SWTBotTreeItem projectTree = waitForNodeWithText(commitTree,
				EMPTY_PROJECT);
		assertEquals(2, projectTree.getItems().length);

		SWTBotTreeItem folderTree = waitForNodeWithText(projectTree, FOLDER);
		assertEquals(2, folderTree.getItems().length);

		SWTBotTreeItem fileTree = folderTree.getItems()[0];
		assertEquals(FILE1, fileTree.getText());
		fileTree = folderTree.getItems()[1];
		assertEquals(FILE2, fileTree.getText());
	}

	@Test public void shouldExchangeCompareEditorSidesBetweenIncomingAndOutgoingChanges()
			throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		makeChangesAndCommit(PROJ1);

		// compare HEAD against tag
		launchSynchronization(HEAD, INITIAL_TAG, false);
		setGitChangeSetPresentationModel();
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
		setGitChangeSetPresentationModel();

		// then
		SWTBot incomingComp = getCompareEditorForFileInGitChangeSet(
				FILE1, false).bot();
		// right side from compare editor should be equal with left
		assertThat(outgoingLeft, equalTo(incomingComp.styledText(1).getText()));
		// left side from compare editor should be equal with right
		assertThat(outgoingRight, equalTo(incomingComp.styledText(0).getText()));
	}

	@Test public void shouldNotShowIgnoredFiles()
			throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
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
		setGitChangeSetPresentationModel();

		// then
		// asserts for Git Change Set model
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		expandWorkingTreeNode(syncViewTree);
		assertEquals(2, syncViewTree.getAllItems().length);
		SWTBotTreeItem proj1Node = syncViewTree.getAllItems()[0];
		proj1Node.getItems()[0].expand();
		assertEquals(1, proj1Node.getItems()[0].getItems().length);
		assertEquals(".gitignore",
				proj1Node.getItems()[0].getItems()[0].getText());
	}

	@Test public void shouldShowNonWorkspaceFileInSynchronization()
			throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		String name = "non-workspace.txt";
		File root = new File(getTestDirectory(), REPO1);
		File nonWorkspace = new File(root, name);
		BufferedWriter writer = new BufferedWriter(new FileWriter(nonWorkspace));
		writer.append("file content");
		writer.close();

		// when
		launchSynchronization(INITIAL_TAG, HEAD, true);
		setGitChangeSetPresentationModel();

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		SWTBotTreeItem workingTree = expandWorkingTreeNode(syncViewTree);
		assertEquals(2, syncViewTree.getAllItems().length);
		assertEquals(1, workingTree.getNodes(name).size());
	}

	@Test
	public void shouldShowCompareEditorForNonWorkspaceFileFromSynchronization()
			throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		String content = "file content";
		String name = "non-workspace.txt";
		File root = new File(getTestDirectory(), REPO1);
		File nonWorkspace = new File(root, name);
		BufferedWriter writer = new BufferedWriter(new FileWriter(nonWorkspace));
		writer.append(content);
		writer.close();

		// when
		launchSynchronization(INITIAL_TAG, HEAD, true);
		setGitChangeSetPresentationModel();

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		SWTBotTreeItem workingTree = expandWorkingTreeNode(syncViewTree);
		assertEquals(2, syncViewTree.getAllItems().length);
		workingTree.expand().getNode(name).doubleClick();

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
		resetRepositoryToCreateInitialTag();
		changeFilesInProject();
		launchSynchronization(HEAD, HEAD, true);
		setGitChangeSetPresentationModel();
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
		resetRepositoryToCreateInitialTag();
		changeFilesInProject();
		launchSynchronization(HEAD, HEAD, true);
		setGitChangeSetPresentationModel();
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

	public void shouldRefreshSyncResultAfterWorkspaceChange() throws Exception {
		// given
		String newFileName = "new.txt";
		resetRepositoryToCreateInitialTag();
		launchSynchronization(INITIAL_TAG, HEAD, true);
		setGitChangeSetPresentationModel();
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
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		SWTBotTreeItem[] syncItems = syncViewTree.getAllItems();
		assertEquals(GitModelWorkingTree_workingTree, syncItems[0].getText());
		syncItems[0].doubleClick(); // expand all
		// WidgetNotFoundException will be thrown when node named 'new.txt' not
		// exists
		assertNotNull(syncItems[0].getNode(PROJ1));
		assertNotNull(syncItems[0].getNode(PROJ1).getNode(newFileName));
	}

	// TODO: stabilize test and reenable it
	@Ignore
	@Test
	public void shouldRefreshSyncResultAfterRepositoryChange() throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		changeFilesInProject();
		launchSynchronization(HEAD, HEAD, true);
		setGitChangeSetPresentationModel();

		// preconditions - sync result should contain two uncommitted changes
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		SWTBotTreeItem[] syncItems = syncViewTree.getAllItems();
		assertTrue(syncItems[0].getText().endsWith(GitModelWorkingTree_workingTree));
		syncItems[0].doubleClick();
		assertEquals(2,
				syncItems[0].getItems()[0].getItems()[0].getItems().length);

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

	private SWTBotTreeItem expandWorkingTreeNode(SWTBotTree syncViewTree) {
		String workingTreeNodeNameString = getWorkingTreeNodeName(syncViewTree);
		return syncViewTree.expandNode(workingTreeNodeNameString);
	}

	private String getWorkingTreeNodeName(SWTBotTree syncViewTree) {
		for (SWTBotTreeItem item : syncViewTree.getAllItems()) {
			String nodeName = item.getText();
			if (nodeName.contains(UIText.GitModelWorkingTree_workingTree))
				return nodeName;
		}

		return UIText.GitModelWorkingTree_workingTree;
	}

}
