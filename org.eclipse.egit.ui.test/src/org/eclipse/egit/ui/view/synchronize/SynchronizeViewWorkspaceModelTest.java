/*******************************************************************************
 * Copyright (C) 2010, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.view.synchronize;

import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.Constants.MASTER;
import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_TAGS;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.allOf;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withRegex;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.file.Files;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.common.CompareEditorTester;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class SynchronizeViewWorkspaceModelTest extends AbstractSynchronizeViewTest {

	@Before
	public void setUpEnabledModelProvider() {
		setEnabledModelProvider(ModelProvider.RESOURCE_MODEL_PROVIDER_ID);
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
		SWTBotTree syncViewTree = bot.viewById(ISynchronizeView.VIEW_ID).bot().tree();
		SWTBotTreeItem[] syncItems = syncViewTree.getAllItems();
		assertTrue(syncItems[0].getText().contains(PROJ1));
	}

	@Test
	public void shouldCompareBranchAgainstTag() throws Exception {
		// given
		makeChangesAndCommit(PROJ1);

		// when
		launchSynchronization(INITIAL_TAG, HEAD, false);

		// then
		SWTBotTree syncViewTree = bot.viewById(ISynchronizeView.VIEW_ID).bot().tree();
		assertEquals(1, syncViewTree.getAllItems().length);
	}

	@Test
	public void shouldCompareTagAgainstTag() throws Exception {
		// given
		makeChangesAndCommit(PROJ1);
		createTag("v0.1");

		// when
		launchSynchronization(INITIAL_TAG, R_TAGS + "v0.1", false);

		// then
		SWTBotTree syncViewTree = bot.viewById(ISynchronizeView.VIEW_ID).bot().tree();
		assertEquals(1, syncViewTree.getAllItems().length);
	}

	@Test
	public void shouldOpenCompareEditor()
			throws Exception {
		// given
		makeChangesAndCommit(PROJ1);
		changeFilesInProject();

		// when
		launchSynchronization(HEAD, INITIAL_TAG, true);

		// then
		CompareEditorTester compare = getCompareEditorForFileInWorkspaceModel(FILE1);
		assertNotNull(compare);
	}

	@Test
	public void shouldListFileDeletedChange() throws Exception {
		// given
		deleteFileAndCommit(PROJ1);

		// when
		launchSynchronization(HEAD, HEAD + "~1", true);

		// then
		SWTBotTree syncViewTree = bot.viewById(ISynchronizeView.VIEW_ID).bot().tree();
		assertEquals(1, syncViewTree.getAllItems().length);

		SWTBotTreeItem projectTree = waitForNodeWithText(syncViewTree, PROJ1);
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
		SWTBotTree syncViewTree = bot.viewById(ISynchronizeView.VIEW_ID).bot().tree();
		SWTBotTreeItem projectTree = waitForNodeWithText(syncViewTree,
				EMPTY_PROJECT);
		assertEquals(1, syncViewTree.getAllItems().length);

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
		CompareEditorTester outgoingCompare = getCompareEditorForFileInWorkspaceModel(
				FILE1);
		// save left value from compare editor
		String outgoingLeft = outgoingCompare.getLeftEditor().getText();
		// save right value from compare editor
		String outgoingRight = outgoingCompare.getRightEditor().getText();
		outgoingCompare.close();

		// when
		// compare tag against HEAD
		launchSynchronization(INITIAL_TAG, HEAD, false);

		// then
		CompareEditorTester incomingComp = getCompareEditorForFileInWorkspaceModel(FILE1);
		String incomingLeft = incomingComp.getLeftEditor().getText();
		String incomingRight = incomingComp.getRightEditor().getText();
		// right side from compare editor should be equal with left
		assertThat(outgoingLeft, equalTo(incomingRight));
		// left side from compare editor should be equal with right
		assertThat(outgoingRight, equalTo(incomingLeft));
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
		SWTBotTree syncViewTree = bot.viewById(ISynchronizeView.VIEW_ID).bot().tree();
		SWTBotTreeItem projectTree = waitForNodeWithText(syncViewTree, PROJ1);
		TestUtil.expandAndWait(projectTree);
		assertEquals(1, projectTree.getItems().length);
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
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);

		// then
		SWTBotTree syncViewTree = bot.viewById(ISynchronizeView.VIEW_ID).bot().tree();
		SWTBotTreeItem[] syncItems = syncViewTree.getAllItems();
		assertTrue(syncItems[0].getText().contains(PROJ1));
		TestUtil.expandAndWait(syncItems[0]);
		// WidgetNotFoundException will be thrown when node named 'new.txt' not exists
		assertNotNull(syncItems[0].getNode(newFileName));
	}

	@Test
	public void shouldRefreshSyncResultAfterRepositoryChange() throws Exception {
		// given
		changeFilesInProject();
		launchSynchronization(HEAD, HEAD, true);

		// preconditions - sync result should contain two uncommitted changes
		SWTBotTree syncViewTree = bot.viewById(ISynchronizeView.VIEW_ID).bot().tree();
		SWTBotTreeItem[] syncItems = syncViewTree.getAllItems();
		assertTrue(syncItems[0].getText().contains(PROJ1));
		TestUtil.expandAndWait(syncItems[0]);
		TestUtil.expandAndWait(syncItems[0].getItems()[0]);
		assertEquals(2, syncItems[0].getItems()[0].getItems().length);

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

	@Test @Ignore// workspace model dosn't show non-workspace files ... yet ;)
	public void shouldShowNonWorkspaceFileInSynchronization()
			throws Exception {
		// given
		String name = "non-workspace.txt";
		File root = new File(getTestDirectory(), REPO1);
		File nonWorkspace = new File(root, name);
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				Files.newOutputStream(nonWorkspace.toPath()), "UTF-8"))) {
			writer.append("file content");
		}

		// when
		launchSynchronization(INITIAL_TAG, HEAD, true);

		// then
		SWTBotTree syncViewTree = bot.viewById(ISynchronizeView.VIEW_ID).bot().tree();
		SWTBotTreeItem workingTree = syncViewTree.expandNode(PROJ1);
		assertEquals(1, syncViewTree.getAllItems().length);
		assertEquals(1, workingTree.getNodes(name).size());
	}

	@Test @Ignore// workspace model dosn't show non-workspace files ... yet ;)
	public void shouldShowCompareEditorForNonWorkspaceFileFromSynchronization()
			throws Exception {
		// given
		String content = "file content";
		String name = "non-workspace.txt";
		File root = new File(getTestDirectory(), REPO1);
		File nonWorkspace = new File(root, name);
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				Files.newOutputStream(nonWorkspace.toPath()), "UTF-8"))) {
			writer.append(content);
		}

		// when
		launchSynchronization(INITIAL_TAG, HEAD, true);

		// then
		SWTBotTree syncViewTree = bot.viewById(ISynchronizeView.VIEW_ID).bot().tree();
		SWTBotTreeItem workingTree = syncViewTree.expandNode(PROJ1);
		assertEquals(1, syncViewTree.getAllItems().length);
		TestUtil.expandAndWait(workingTree).getNode(name).doubleClick();

		SWTBotEditor editor = bot.editorByTitle(name);
		editor.setFocus();

		// the WidgetNotFoundException will be thrown when widget with given content cannot be not found
		SWTBotStyledText left = editor.bot().styledText(content);
		SWTBotStyledText right = editor.bot().styledText("");
		// to be complete sure assert that both sides are not the same
		assertNotSame(left, right);
	}

	protected CompareEditorTester getCompareEditorForFileInWorkspaceModel(
			String fileName) {
		SWTBotTree syncViewTree = bot.viewById(ISynchronizeView.VIEW_ID).bot().tree();

		SWTBotTreeItem projNode = waitForNodeWithText(syncViewTree, PROJ1);
		return getCompareEditor(projNode, fileName);
	}

}
