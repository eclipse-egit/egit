/*******************************************************************************
 * Copyright (C) 2016 Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.view.synchronize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SynchronizeViewRemoteAwareChangeSetModelTest
		extends AbstractSynchronizeViewTest {

	private IFile mockLogicalFile;

	private static final String MOCK_LOGICAL_PROJECT = "MockLogical";

	@Before
	public void setUpPreferences() {
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.USE_LOGICAL_MODEL, true);
	}

	@Before
	public void setUpEnabledModelProvider() {
		setEnabledModelProvider(ModelProvider.RESOURCE_MODEL_PROVIDER_ID);
	}

	@After
	public void tearDownPreferences() {
		Activator.getDefault().getPreferenceStore()
				.setToDefault(UIPreferences.USE_LOGICAL_MODEL);
	}

	/**
	 * Make sure that files that are not part of the logical model because of
	 * remote file content will be taken into account in the comparison.
	 *
	 * @throws Exception
	 */
	@Test
	public void shouldShowRemoteFilesInSynchronization() throws Exception {
		// given
		createMockLogicalRepository();

		// when comparing file 'index.mocklogical' with branch 'stable'
		String compareWithBranchActionLabel = util
				.getPluginLocalizedValue("ReplaceWithRefAction_label");
		clickCompareWith(compareWithBranchActionLabel);
		SWTBotShell compareShell = bot.shell("Compare");
		SWTBotTree tree = compareShell.bot().tree();
		for (SWTBotTreeItem item : tree.getTreeItem("Local").getItems()) {
			if (item.getText().contains("stable")) {
				tree.select(item);
				break;
			}
		}
		bot.button("Compare").click();

		// then
		SWTBotTree syncViewTree = bot.viewById(ISynchronizeView.VIEW_ID).bot()
				.tree();
		SWTBotTreeItem mockLogicalProjectItem = waitForNodeWithText(
				syncViewTree, MOCK_LOGICAL_PROJECT);
		SWTBotTreeItem[] items = mockLogicalProjectItem.getItems();
		assertEquals(4, items.length);

		SWTBotTreeItem fileTree = items[0];
		assertEquals("file1.txt", fileTree.getText());
		fileTree = items[1];
		assertEquals("file2.txt", fileTree.getText());
		fileTree = items[2];
		assertEquals("file3.txt", fileTree.getText());
		fileTree = items[3];
		assertEquals("index.mocklogical", fileTree.getText());
	}

	private void clickCompareWith(String menuLabel) {
		SWTBotTree projectExplorerTree = selectMockLogicalItem();
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Compare With",
				menuLabel);
	}

	private SWTBotTree selectMockLogicalItem() {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		SWTBotTreeItem projectItem = getProjectItem(projectExplorerTree,
				MOCK_LOGICAL_PROJECT);
		projectItem.expand();
		for (SWTBotTreeItem item : projectItem.getItems()) {
			if (item.getText().contains("index.mocklogical")) {
				item.select();
			}
		}
		return projectExplorerTree;
	}

	protected void createMockLogicalRepository() throws Exception {
		File gitDir = new File(
				new File(getTestDirectory(), MOCK_LOGICAL_PROJECT),
				Constants.DOT_GIT);
		Repository repo = FileRepositoryBuilder.create(gitDir);
		repo.create();

		// we need to commit into master first
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(MOCK_LOGICAL_PROJECT);

		if (project.exists()) {
			project.delete(true, null);
			TestUtil.waitForJobs(100, 5000);
		}
		IProjectDescription desc = ResourcesPlugin.getWorkspace()
				.newProjectDescription(MOCK_LOGICAL_PROJECT);
		desc.setLocation(new Path(
				new File(repo.getWorkTree(), MOCK_LOGICAL_PROJECT).getPath()));
		project.create(desc, null);
		project.open(null);
		assertTrue("Project is not accessible: " + project,
				project.isAccessible());

		TestUtil.waitForJobs(50, 5000);
		try {
			new ConnectProviderOperation(project, gitDir).execute(null);
		} catch (Exception e) {
			Activator.logError("Failed to connect project to repository", e);
		}
		assertConnected(project);

		mockLogicalFile = project.getFile("index.mocklogical");
		mockLogicalFile.create(new ByteArrayInputStream(
				"file1.txt\nfile2.txt".getBytes(project.getDefaultCharset())),
				false, null);
		IFile file1 = project.getFile("file1.txt");
		file1.create(
				new ByteArrayInputStream(
						"Content 1".getBytes(project.getDefaultCharset())),
				false, null);
		IFile file2 = project.getFile("file2.txt");
		file2.create(
				new ByteArrayInputStream(
						"Content 2".getBytes(project.getDefaultCharset())),
				false, null);

		IFile[] commitables = new IFile[] { mockLogicalFile, file1, file2 };
		List<IFile> untracked = new ArrayList<>();
		untracked.addAll(Arrays.asList(commitables));
		CommitOperation op = new CommitOperation(commitables, untracked,
				TestUtil.TESTAUTHOR, TestUtil.TESTCOMMITTER, "Initial commit");
		op.execute(null);
		RevCommit firstCommit = op.getCommit();

		CreateLocalBranchOperation createBranchOp = new CreateLocalBranchOperation(
				repo, "refs/heads/stable", firstCommit);
		createBranchOp.execute(null);

		// Delete file2.txt from logical model and add file3
		mockLogicalFile = touch(MOCK_LOGICAL_PROJECT, "index.mocklogical",
				"file1.txt\nfile3.txt");
		file2.delete(true, null);
		touch(MOCK_LOGICAL_PROJECT, "file1.txt", "Content 1 modified");
		IFile file3 = project.getFile("file3.txt");
		file3.create(
				new ByteArrayInputStream(
						"Content 3".getBytes(project.getDefaultCharset())),
				false, null);
		commitables = new IFile[] { mockLogicalFile, file1, file2, file3 };
		untracked = new ArrayList<>();
		untracked.add(file3);
		op = new CommitOperation(commitables, untracked, TestUtil.TESTAUTHOR,
				TestUtil.TESTCOMMITTER, "Second commit");
		op.execute(null);
	}

}
