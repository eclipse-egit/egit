/*******************************************************************************
 * Copyright (C) 2011, 2014 Jens Baumgart <jens.baumgart@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test.stagview;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.op.DisconnectProviderOperation;
import org.eclipse.egit.ui.common.CompareEditorTester;
import org.eclipse.egit.ui.common.StagingViewTester;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.actions.ReplaceConflictActionHandler;
import org.eclipse.egit.ui.internal.staging.StagingEntry;
import org.eclipse.egit.ui.test.CommitMessageUtil;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.JobJoiner;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.team.core.RepositoryProvider;
import org.junit.Test;

public class StagingViewTest extends AbstractStagingViewTestCase {

	private void commonCommitManyFiles(Repository myRepository,
			StagingViewTester stagingViewTester) throws Exception {

		File workDir = myRepository.getWorkTree();
		// step 1 create and add many files in one step to index and commit
		createManyEmptyFiles(workDir, "many1", 30);
		stagingViewTester.refreshIndex(myRepository);
		assertTrue("Not all files were staged.",
				stagingViewTester.stageAllFiles("many129.txt"));

		stagingViewTester.setCommitMessage("The 1st commit");
		stagingViewTester.commit();
		TestUtil.checkHeadCommit(myRepository, TestUtil.TESTCOMMITTER,
				TestUtil.TESTCOMMITTER, "The 1st commit");
		assertFalse("Commit Button should be disabled",
				stagingViewTester.isCommitEnabled());
		// step 2 delete some of the previously created files and remove them
		// from the index in one step and commit
		removeManyEmptyFiles(workDir, "many1", 0, 15);
		stagingViewTester.refreshIndex(myRepository);
		assertTrue("Not all files were staged.",
				stagingViewTester.stageAllFiles("many114.txt"));

		stagingViewTester.setCommitMessage("The 2nd commit");
		stagingViewTester.commit();
		TestUtil.checkHeadCommit(myRepository, TestUtil.TESTCOMMITTER,
				TestUtil.TESTCOMMITTER, "The 2nd commit");
		assertFalse("Commit Button should be disabled",
				stagingViewTester.isCommitEnabled());

		// step 3 delete some of the previously created files and remove them
		// from the index in the same step add some new files to the index
		// and commit
		removeManyEmptyFiles(workDir, "many1", 18, 24);
		createManyEmptyFiles(workDir, "many1", 14);
		stagingViewTester.refreshIndex(myRepository);
		assertTrue("Not all files were staged.",
				stagingViewTester.stageAllFiles("many123.txt"));

		stagingViewTester.setCommitMessage("The 3rd commit");
		stagingViewTester.commit();
		TestUtil.checkHeadCommit(myRepository, TestUtil.TESTCOMMITTER,
				TestUtil.TESTCOMMITTER, "The 3rd commit");
		assertFalse("Commit Button should be disabled",
				stagingViewTester.isCommitEnabled());
	}

	@Test
	public void testCommitManyFilesNoProject() throws Exception {
		StagingViewTester stagingViewTester = StagingViewTester
				.openStagingView();

		// create and select a git repository without a associated Eclipse
		// project
		Repository myRepository = createLocalTestRepository("testmany");
		selectRepositoryNode(myRepository.getDirectory());

		commonCommitManyFiles(myRepository, stagingViewTester);
		RepositoryUtil.INSTANCE.removeDir(myRepository.getDirectory());
	}

	@Test
	public void testCommitManyFiles() throws Exception {
		StagingViewTester stagingViewTester = StagingViewTester
				.openStagingView();
		commonCommitManyFiles(repository, stagingViewTester);

	}


	@Test
	public void testCommitSingleFile() throws Exception {
		setContent("I have changed this");

		StagingViewTester stagingViewTester = StagingViewTester
				.openStagingView();

		stagingViewTester.stageFile(FILE1_PATH);

		stagingViewTester.setAuthor(TestUtil.TESTAUTHOR);
		stagingViewTester.setCommitter(TestUtil.TESTCOMMITTER);
		stagingViewTester.setCommitMessage("The new commit");
		stagingViewTester.commit();
		TestUtil.checkHeadCommit(repository, TestUtil.TESTAUTHOR,
				TestUtil.TESTCOMMITTER, "The new commit");
		assertFalse("Commit Button should be disabled",
				stagingViewTester.isCommitEnabled());
	}

	@Test
	public void testAmend() throws Exception {
		RevCommit oldHeadCommit = TestUtil.getHeadCommit(repository);
		commitOneFileChange("Yet another Change");
		RevCommit headCommit = TestUtil.getHeadCommit(repository);
		ObjectId headCommitId = headCommit.getId();
		String changeId = CommitMessageUtil.extractChangeId(headCommit
				.getFullMessage());
		setContent("Changes over changes");
		StagingViewTester stagingViewTester = StagingViewTester
				.openStagingView();
		stagingViewTester.stageFile(FILE1_PATH);
		stagingViewTester.setAmend(true);
		assertTrue(stagingViewTester.getCommitMessage().indexOf("Change-Id") > 0);
		assertTrue(stagingViewTester.getCommitMessage()
				.indexOf("Signed-off-by") > 0);
		assertTrue(stagingViewTester.getSignedOff());
		assertTrue(stagingViewTester.getInsertChangeId());
		stagingViewTester.commit();
		headCommit = TestUtil.getHeadCommit(repository);
		if (headCommitId.equals(headCommit.getId()))
			fail("There is no new commit");
		assertEquals(oldHeadCommit, headCommit.getParent(0));
		assertTrue(headCommit.getFullMessage().indexOf(changeId) > 0);
	}

	@Test
	public void testMergeConflict() throws Exception {
		try (Git git = new Git(repository)) {
			git.checkout().setCreateBranch(true).setName("side").call();
			commitOneFileChange("on side");

			git.checkout().setName("master").call();
			commitOneFileChange("on master");

			git.merge().include(repository.findRef("side")).call();
		}
		assertEquals(RepositoryState.MERGING, repository.getRepositoryState());

		// IndexDiffCache.INSTANCE.getIndexDiffCacheEntry(repository)
		// .refresh();
		JobJoiner jobJoiner = JobJoiner.startListening(
				org.eclipse.egit.core.JobFamilies.INDEX_DIFF_CACHE_UPDATE, 30,
				TimeUnit.SECONDS);
		IndexDiffCache.INSTANCE.getIndexDiffCacheEntry(repository).refresh();
		jobJoiner.join();
		StagingViewTester stagingView = StagingViewTester
				.openStagingView();
		assertEquals("", stagingView.getCommitMessage());
		stagingView.assertCommitEnabled(false);

		setContent("resolved");
		stagingView.stageFile(FILE1_PATH);
		assertEquals(RepositoryState.MERGING_RESOLVED,
				repository.getRepositoryState());
		String expectedMessage = "Merge branch 'side'";
		assertThat(stagingView.getCommitMessage(), startsWith(expectedMessage));

		stagingView.commit();
		assertEquals(RepositoryState.SAFE, repository.getRepositoryState());

		assertEquals(expectedMessage, TestUtil.getHeadCommit(repository)
				.getShortMessage());
	}

	@Test
	public void testMergeConflictCheckoutHead() throws Exception {
		try (Git git = new Git(repository)) {
			git.checkout().setCreateBranch(true).setName("side").call();
			commitOneFileChange("on side");

			git.checkout().setName("master").call();
			commitOneFileChange("on master");

			git.merge().include(repository.findRef("side")).call();
		}
		assertEquals(RepositoryState.MERGING, repository.getRepositoryState());

		// IndexDiffCache.INSTANCE.getIndexDiffCacheEntry(repository).refresh();
		JobJoiner jobJoiner1 = JobJoiner.startListening(
				org.eclipse.egit.core.JobFamilies.INDEX_DIFF_CACHE_UPDATE, 30,
				TimeUnit.SECONDS);
		IndexDiffCache.INSTANCE.getIndexDiffCacheEntry(repository).refresh();
		jobJoiner1.join();

		StagingViewTester stagingView = StagingViewTester.openStagingView();
		assertEquals("", stagingView.getCommitMessage());
		stagingView.assertCommitEnabled(false);

		// Resolve the conflict via "Replace with HEAD"
		SWTBot viewBot = stagingView.getView().bot();
		SWTBotTree unstagedTree = viewBot.tree(0);

		TestUtil.waitUntilTreeHasNodeContainsText(viewBot, unstagedTree,
				FILE1_PATH, 10000);

		TestUtil.getNode(unstagedTree.getAllItems(), FILE1_PATH).select();

		ContextMenuHelper.clickContextMenuSync(unstagedTree,
				UIText.StagingView_replaceWithHeadRevision);

		viewBot.waitUntil(new DefaultCondition() {

			@Override
			public boolean test() throws Exception {
				return unstagedTree.getAllItems().length < 2;
			}

			@Override
			public String getFailureMessage() {
				return "Checkout did not resolve the conflict";
			}
		});

		assertEquals(RepositoryState.MERGING_RESOLVED,
				repository.getRepositoryState());
		assertEquals("on master", getTestFileContent());
		String expectedMessage = "Merge branch 'side'";
		assertThat(stagingView.getCommitMessage(), startsWith(expectedMessage));

		JobJoiner jobJoiner = JobJoiner.startListening(
				org.eclipse.egit.core.JobFamilies.INDEX_DIFF_CACHE_UPDATE, 30,
				TimeUnit.SECONDS);
		stagingView.commit();
		jobJoiner.join();

		assertEquals(RepositoryState.SAFE, repository.getRepositoryState());

		assertEquals(expectedMessage,
				TestUtil.getHeadCommit(repository).getShortMessage());
	}

	/**
	 * Tests resolving a modify-delete conflict by deleting the file and then
	 * staging the change.
	 *
	 * @throws Exception
	 *             on errors
	 */
	@Test
	public void testDeleteModifyConflict() throws Exception {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFolder(FOLDER).getFile(FILE1);
		try (Git git = new Git(repository)) {
			git.checkout().setCreateBranch(true).setName("side").call();
			assertTrue(file.exists());
			file.delete(true, null);
			assertFalse(file.exists());
			git.rm().addFilepattern(FILE1_PATH).call();
			TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
			git.commit().setMessage("File deleted").call();
			TestUtil.waitForJobs(50, 5000);

			git.checkout().setName("master").call();
			commitOneFileChange("on master");

			git.merge().include(repository.findRef("side")).call();
		}
		assertEquals(RepositoryState.MERGING, repository.getRepositoryState());

		StagingViewTester stagingView = StagingViewTester.openStagingView();
		assertEquals("", stagingView.getCommitMessage());
		stagingView.assertCommitEnabled(false);

		assertTrue(file.exists());
		file.delete(true, null);
		assertFalse(file.exists());
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		stagingView.stageFile(FILE1_PATH);
		assertEquals(RepositoryState.MERGING_RESOLVED,
				repository.getRepositoryState());
		String expectedMessage = "Merge branch 'side'";
		assertThat(stagingView.getCommitMessage(), startsWith(expectedMessage));

		stagingView.commit();
		assertEquals(RepositoryState.SAFE, repository.getRepositoryState());

		assertEquals(expectedMessage,
				TestUtil.getHeadCommit(repository).getShortMessage());

		assertFalse(file.exists());
	}

	/**
	 * Tests resolving a modify-delete conflict via "Replace with theirs" in the
	 * staging view, which should remove the file and stage the result.
	 *
	 * @throws Exception
	 *             on errors
	 */
	@Test
	public void testModifyDeleteConflict() throws Exception {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFolder(FOLDER).getFile(FILE1);
		RevCommit side;
		try (Git git = new Git(repository)) {
			git.checkout().setCreateBranch(true).setName("side").call();
			assertTrue(file.exists());
			file.delete(true, null);
			assertFalse(file.exists());
			git.rm().addFilepattern(FILE1_PATH).call();
			TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
			side = git.commit().setMessage("File deleted").call();
			TestUtil.waitForJobs(50, 5000);

			git.checkout().setName("master").call();
			commitOneFileChange("on master");

			git.merge().include(repository.findRef("side")).call();
		}
		assertEquals(RepositoryState.MERGING, repository.getRepositoryState());

		StagingViewTester stagingView = StagingViewTester.openStagingView();
		assertEquals("", stagingView.getCommitMessage());
		stagingView.assertCommitEnabled(false);

		SWTBotView view = stagingView.getView();
		SWTBot viewBot = view.bot();
		SWTBotTree unstagedTree = viewBot.tree(0);

		TestUtil.waitUntilTreeHasNodeContainsText(viewBot, unstagedTree,
				FILE1_PATH, 10000);

		SWTBotTreeItem item = TestUtil.getNode(unstagedTree.getAllItems(),
				FILE1_PATH);
		item.select();

		JobJoiner jobJoiner = JobJoiner.startListening(
				org.eclipse.egit.core.JobFamilies.INDEX_DIFF_CACHE_UPDATE, 30,
				TimeUnit.SECONDS);

		ContextMenuHelper.clickContextMenu(unstagedTree,
				UIText.StagingView_ReplaceWith,
				ReplaceConflictActionHandler.formatCommitLabel(
						UIText.ReplaceWithOursTheirsMenu_TheirsWithCommitLabel,
						side));

		jobJoiner.join();

		assertFalse(file.exists());

		assertEquals(RepositoryState.MERGING_RESOLVED,
				repository.getRepositoryState());
		String expectedMessage = "Merge branch 'side'";
		assertThat(stagingView.getCommitMessage(), startsWith(expectedMessage));

		SWTBotTree stagedTree = viewBot.tree(1);
		TestUtil.waitUntilTreeHasNodeContainsText(viewBot, stagedTree,
				FILE1_PATH, 10000);
		SWTBotTreeItem item2 = TestUtil.getNode(stagedTree.getAllItems(),
				FILE1_PATH);
		Object data = UIThreadRunnable.syncExec(() -> item2.widget.getData());
		assertEquals(StagingEntry.class, data.getClass());
		StagingEntry entry = (StagingEntry) data;
		assertEquals(StagingEntry.State.REMOVED, entry.getState());

		stagingView.commit();
		assertEquals(RepositoryState.SAFE, repository.getRepositoryState());

		assertEquals(expectedMessage,
				TestUtil.getHeadCommit(repository).getShortMessage());
	}

	/**
	 * Tests resolving a modify-delete conflict via "Replace with theirs" in the
	 * project explorer, which should remove the file and stage the result.
	 *
	 * @throws Exception
	 *             on errors
	 */
	@Test
	public void testModifyDeleteConflict2() throws Exception {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFolder(FOLDER).getFile(FILE1);
		RevCommit side;
		try (Git git = new Git(repository)) {
			git.checkout().setCreateBranch(true).setName("side").call();
			assertTrue(file.exists());
			file.delete(true, null);
			assertFalse(file.exists());
			git.rm().addFilepattern(FILE1_PATH).call();
			TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
			side = git.commit().setMessage("File deleted").call();
			TestUtil.waitForJobs(50, 5000);

			git.checkout().setName("master").call();
			commitOneFileChange("on master");

			git.merge().include(repository.findRef("side")).call();
		}
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		assertEquals(RepositoryState.MERGING, repository.getRepositoryState());

		StagingViewTester stagingView = StagingViewTester.openStagingView();
		assertEquals("", stagingView.getCommitMessage());
		stagingView.assertCommitEnabled(false);

		SWTBotView view = stagingView.getView();
		SWTBot viewBot = view.bot();

		SWTBotTree explorer = TestUtil.getExplorerTree();
		SWTBotTreeItem item = TestUtil.navigateTo(explorer, PROJ1, FOLDER,
				FILE1);
		item.select();

		JobJoiner jobJoiner = JobJoiner.startListening(
				org.eclipse.egit.core.JobFamilies.INDEX_DIFF_CACHE_UPDATE, 30,
				TimeUnit.SECONDS);

		ContextMenuHelper.clickContextMenu(explorer,
				UIText.StagingView_ReplaceWith,
				ReplaceConflictActionHandler.formatCommitLabel(
						UIText.ReplaceWithOursTheirsMenu_TheirsWithCommitLabel,
						side));

		jobJoiner.join();

		assertFalse(file.exists());

		assertEquals(RepositoryState.MERGING_RESOLVED,
				repository.getRepositoryState());
		String expectedMessage = "Merge branch 'side'";
		assertThat(stagingView.getCommitMessage(), startsWith(expectedMessage));

		SWTBotTree stagedTree = viewBot.tree(1);
		TestUtil.waitUntilTreeHasNodeContainsText(viewBot, stagedTree,
				FILE1_PATH, 10000);
		SWTBotTreeItem item2 = TestUtil.getNode(stagedTree.getAllItems(),
				FILE1_PATH);
		Object data = UIThreadRunnable.syncExec(() -> item2.widget.getData());
		assertEquals(StagingEntry.class, data.getClass());
		StagingEntry entry = (StagingEntry) data;
		assertEquals(StagingEntry.State.REMOVED, entry.getState());

		stagingView.commit();
		assertEquals(RepositoryState.SAFE, repository.getRepositoryState());

		assertEquals(expectedMessage,
				TestUtil.getHeadCommit(repository).getShortMessage());
	}

	private void openComparison(boolean shared) throws Exception {
		setContent("I have changed this");

		if (!shared) {
			IProject project = ResourcesPlugin.getWorkspace().getRoot()
					.getProject(PROJ1);
			new DisconnectProviderOperation(List.of(project)).execute(null);
			assertNull(RepositoryProvider.getProvider(project, GitProvider.ID));
		}

		StagingViewTester stagingView = StagingViewTester.openStagingView();
		SWTBotView view = stagingView.getView();
		SWTBot viewBot = view.bot();
		SWTBotTree unstagedTree = viewBot.tree(0);
		TestUtil.waitUntilTreeHasNodeContainsText(viewBot, unstagedTree,
				FILE1_PATH, 10000);
		SWTBotTreeItem item = TestUtil.getNode(unstagedTree.getAllItems(),
				FILE1_PATH);
		item.select();
		ContextMenuHelper.clickContextMenu(unstagedTree,
				UIText.StagingView_CompareWithIndexMenuLabel);

		CompareEditorTester compareEditor = CompareEditorTester
				.forTitleContaining("Compare " + FILE1);
		String leftText = compareEditor.getLeftEditor().getText();
		assertEquals("I have changed this", leftText);
	}

	/**
	 * Tests that a comparison between an unstaged file and the index works.
	 *
	 * @throws Exception
	 *             on errors
	 */
	@Test
	public void testCompare() throws Exception {
		openComparison(true);
	}

	/**
	 * Tests that a comparison between an unstaged file and the index works if
	 * the file is in an Eclipse project that is not managed by EGit.
	 *
	 * @throws Exception
	 *             on errors
	 */
	@Test
	public void testCompareNonShared() throws Exception {
		openComparison(false);
	}

	/**
	 * Tests that a comparison between an unstaged file and the index works if
	 * the file is in an Eclipse project that is not managed by EGit, and is at
	 * the repository root.
	 *
	 * @throws Exception
	 *             on errors
	 */
	@Test
	public void testCompareNonSharedAtRoot() throws Exception {
		Repository rootRepo = createLocalTestRepository("RootRepository");
		File workingTree = rootRepo.getWorkTree();
		try (Git git = new Git(rootRepo)) {
			File testFile = new File(workingTree, FILE1);
			Files.write(testFile.toPath(),
					"Content".getBytes(StandardCharsets.UTF_8));
			git.add().addFilepattern(".").call();
			git.commit().setMessage("Initial commit").call();
		}
		File gitDir = rootRepo.getDirectory();
		rootRepo.close();
		// Import this repository into Eclipse
		RepositoryUtil.INSTANCE.addConfiguredRepository(gitDir);
		rootRepo = lookupRepository(gitDir);
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject("unsharedProject");
		assertFalse(project.exists());
		// Import the project
		IWorkspaceRunnable importProject = monitor -> {
			SubMonitor progress = SubMonitor.convert(monitor, 2);
			IProjectDescription desc = ResourcesPlugin.getWorkspace()
					.newProjectDescription("unsharedProject");
			desc.setLocation(new Path(workingTree.getPath()));
			project.create(desc, progress.newChild(1));
			project.open(progress.newChild(1));
		};
		ResourcesPlugin.getWorkspace().run(importProject, null);
		assertTrue(project.exists());
		assertTrue(project.isAccessible());
		assertNull(RepositoryProvider.getProvider(project, GitProvider.ID));
		// Modify the file
		IFile file = project.getFile(new Path(FILE1));
		file.refreshLocal(0, null);
		file.setContents(
				new ByteArrayInputStream(
						"Changed".getBytes(project.getDefaultCharset())),
				0, null);

		// IndexDiffCache.INSTANCE.getIndexDiffCacheEntry(rootRepo).refresh();
		JobJoiner jobJoiner = JobJoiner.startListening(
				org.eclipse.egit.core.JobFamilies.INDEX_DIFF_CACHE_UPDATE, 30,
				TimeUnit.SECONDS);
		IndexDiffCache.INSTANCE.getIndexDiffCacheEntry(rootRepo).refresh();
		jobJoiner.join();

		// Open the git repositories view, select this repository
		SWTBotTree repoTree = getOrOpenView().bot().tree();
		SWTBotTreeItem repoItem = myRepoViewUtil.getRootItem(repoTree, gitDir);
		repoItem.select();
		// Open the staging view, compare
		StagingViewTester stagingView = StagingViewTester.openStagingView();
		SWTBotView view = stagingView.getView();
		SWTBot viewBot = view.bot();
		SWTBotTree unstagedTree = viewBot.tree(0);
		TestUtil.waitUntilTreeHasNodeContainsText(viewBot, unstagedTree, FILE1,
				10000);
		SWTBotTreeItem item = TestUtil.getNode(unstagedTree.getAllItems(),
				FILE1);
		item.select();
		ContextMenuHelper.clickContextMenu(unstagedTree,
				UIText.StagingView_CompareWithIndexMenuLabel);

		CompareEditorTester compareEditor = CompareEditorTester
				.forTitleContaining("Compare " + FILE1);
		String leftText = compareEditor.getLeftEditor().getText();
		assertEquals("Changed", leftText);
	}

	/**
	 * Tests that a staged rename compares against the original file.
	 *
	 * @throws Exception
	 *             on errors
	 */
	@Test
	public void testCompareRenamedFileStaged() throws Exception {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFolder(FOLDER).getFile(FILE1);
		String moved = "moved.txt";
		String movedPath = PROJ1 + '/' + FOLDER + '/' + moved;
		try (Git git = new Git(repository)) {
			assertTrue(file.exists());
			file.move(file.getParent().getFullPath().append(moved), true,
					null);
			assertFalse(file.exists());
			git.rm().addFilepattern(FILE1_PATH).call();
			git.add().addFilepattern(movedPath).call();
			TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		}
		StagingViewTester stagingView = StagingViewTester.openStagingView();
		SWTBotView view = stagingView.getView();
		SWTBot viewBot = view.bot();
		SWTBotTree stagedTree = viewBot.tree(1);
		TestUtil.waitUntilTreeHasNodeContainsText(viewBot, stagedTree,
				movedPath, 10000);
		SWTBotTreeItem item = TestUtil.getNode(stagedTree.getAllItems(),
				movedPath);
		item.select();
		ContextMenuHelper.clickContextMenu(stagedTree,
				UIText.StagingView_CompareWithHeadMenuLabel);

		CompareEditorTester compareEditor = CompareEditorTester
				.forTitleContaining("Compare " + moved);
		String leftText = compareEditor.getLeftEditor().getText();
		String rightText = compareEditor.getRightEditor().getText();
		assertEquals(leftText, rightText);
	}

	/**
	 * Tests that a staged copy compares against the original file.
	 *
	 * @throws Exception
	 *             on errors
	 */
	@Test
	public void testCompareCopiedFileStaged() throws Exception {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFolder(FOLDER).getFile(FILE1);
		String moved = "moved.txt";
		String movedPath = PROJ1 + '/' + FOLDER + '/' + moved;
		try (Git git = new Git(repository)) {
			assertTrue(file.exists());
			file.move(file.getParent().getFullPath().append(moved), true, null);
			assertFalse(file.exists());
			touch("New content");
			assertTrue(file.exists());
			git.add().addFilepattern(movedPath).addFilepattern(FILE1_PATH)
					.call();
			TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		}
		StagingViewTester stagingView = StagingViewTester.openStagingView();
		SWTBotView view = stagingView.getView();
		SWTBot viewBot = view.bot();
		SWTBotTree stagedTree = viewBot.tree(1);
		TestUtil.waitUntilTreeHasNodeContainsText(viewBot, stagedTree,
				movedPath, 10000);
		SWTBotTreeItem item = TestUtil.getNode(stagedTree.getAllItems(),
				movedPath);
		item.select();
		ContextMenuHelper.clickContextMenu(stagedTree,
				UIText.StagingView_CompareWithHeadMenuLabel);

		CompareEditorTester compareEditor = CompareEditorTester
				.forTitleContaining("Compare " + moved);
		String leftText = compareEditor.getLeftEditor().getText();
		String rightText = compareEditor.getRightEditor().getText();
		assertEquals(leftText, rightText);
		item = TestUtil.getNode(stagedTree.getAllItems(), FILE1_PATH);
		item.select();
		ContextMenuHelper.clickContextMenu(stagedTree,
				UIText.StagingView_CompareWithHeadMenuLabel);

		compareEditor = CompareEditorTester
				.forTitleContaining("Compare " + FILE1);
		leftText = compareEditor.getLeftEditor().getText();
		assertEquals("New content", leftText);
	}

	private StagingViewTester commitOneFileChange(String fileContent)
			throws Exception {
		return commitOneFileChange(fileContent, TestUtil.TESTAUTHOR,
				TestUtil.TESTCOMMITTER);
	}

	private StagingViewTester commitOneFileChange(String fileContent,
			String author,
			String committer) throws Exception {
		setContent(fileContent);

		StagingViewTester stagingViewTester = StagingViewTester
				.openStagingView();
		stagingViewTester.stageFile(FILE1_PATH);
		stagingViewTester.setAuthor(author);
		stagingViewTester.setCommitter(committer);
		stagingViewTester.setCommitMessage("Commit message");
		stagingViewTester.setInsertChangeId(true);
		stagingViewTester.setSignedOff(true);
		String commitMessage = stagingViewTester.getCommitMessage();
		assertTrue(commitMessage.indexOf("Change-Id") > 0);
		assertTrue(commitMessage.indexOf("Signed-off-by") > 0);
		stagingViewTester.commit();
		return stagingViewTester;
	}

	@Test
	public void testCommitMessageCommitterChangeSignOff() throws Exception {
		setContent("something");

		StagingViewTester stagingViewTester = StagingViewTester
				.openStagingView();
		stagingViewTester.setAuthor(TestUtil.TESTAUTHOR);
		stagingViewTester.setCommitter(TestUtil.TESTCOMMITTER);
		stagingViewTester.setCommitMessage("Commit message");
		stagingViewTester.setSignedOff(true);
		String commitMessage = stagingViewTester.getCommitMessage();
		assertTrue("Should have a signed-off footer",
				commitMessage.indexOf("Signed-off-by") > 0);
		// Edit the committer field: pretend the user typed a backspace,
		// deleting the closing ">"
		stagingViewTester.setCommitter(TestUtil.TESTCOMMITTER.substring(0,
				TestUtil.TESTCOMMITTER.length() - 1));
		// Now the committer field has an invalid value.
		assertTrue("Sign off should still be enabled",
				stagingViewTester.getSignedOff());
		assertEquals("Commit message should be unchanged", commitMessage,
				stagingViewTester.getCommitMessage());
		// Now change it back to some valid value.
		stagingViewTester.setCommitter("Somebody <some.body@some.where.org>");
		assertEquals("Commit message should be updated",
				commitMessage.replace(TestUtil.TESTCOMMITTER,
						"Somebody <some.body@some.where.org>"),
				stagingViewTester.getCommitMessage());
		assertTrue("Sign off should still be enabled",
				stagingViewTester.getSignedOff());
	}

	@Test
	public void testCommitMessageConfigChange() throws Exception {
		setContent("something");

		StagingViewTester stagingViewTester = StagingViewTester
				.openStagingView();
		stagingViewTester.setAuthor(TestUtil.TESTAUTHOR);
		stagingViewTester.setCommitter(TestUtil.TESTCOMMITTER);
		stagingViewTester.setCommitMessage("Commit message");
		stagingViewTester.setSignedOff(true);
		String commitMessage = stagingViewTester.getCommitMessage();
		assertTrue("Should have a signed-off footer",
				commitMessage.indexOf("Signed-off-by") > 0);
		StoredConfig cfg = repository.getConfig();
		cfg.load();
		cfg.setString(ConfigConstants.CONFIG_USER_SECTION, null,
				ConfigConstants.CONFIG_KEY_NAME, "Some One");
		cfg.save();
		String expectedCommitter = "Some One <" + TestUtil.TESTCOMMITTER_EMAIL
				+ '>';
		assertEquals("Author should be unchanged", TestUtil.TESTAUTHOR,
				stagingViewTester.getAuthor());
		assertEquals("Committer should be changed", expectedCommitter,
				stagingViewTester.getCommitter());
		assertEquals("Commit message should be updated",
				commitMessage.replace(TestUtil.TESTCOMMITTER,
						expectedCommitter),
				stagingViewTester.getCommitMessage());
		assertTrue("Sign-off should be enabled",
				stagingViewTester.getSignedOff());
	}

	@Test
	public void testCommitMessageConfigChangeNoSignOff() throws Exception {
		setContent("something");

		StagingViewTester stagingViewTester = StagingViewTester
				.openStagingView();
		stagingViewTester.setAuthor(TestUtil.TESTAUTHOR);
		stagingViewTester.setCommitter(TestUtil.TESTCOMMITTER);
		stagingViewTester.setCommitMessage("Commit message");
		stagingViewTester.setSignedOff(true);
		String commitMessage = stagingViewTester.getCommitMessage();
		assertTrue("Should have a signed-off footer",
				commitMessage.indexOf("Signed-off-by") > 0);
		stagingViewTester.setSignedOff(false);
		commitMessage = stagingViewTester.getCommitMessage();
		assertTrue("Should not have a signed-off footer", !commitMessage.contains("Signed-off-by"));
		StoredConfig cfg = repository.getConfig();
		cfg.load();
		cfg.setString(ConfigConstants.CONFIG_USER_SECTION, null,
				ConfigConstants.CONFIG_KEY_NAME, "Some One");
		cfg.save();
		String expectedCommitter = "Some One <" + TestUtil.TESTCOMMITTER_EMAIL
				+ '>';
		assertEquals("Author should be unchanged", TestUtil.TESTAUTHOR,
				stagingViewTester.getAuthor());
		assertEquals("Committer should be changed", expectedCommitter,
				stagingViewTester.getCommitter());
		assertEquals("Commit message should be unchanged", commitMessage,
				stagingViewTester.getCommitMessage());
		assertFalse("Sign-off should be disabled",
				stagingViewTester.getSignedOff());
	}

	@Test
	public void testCommitMessageConfigChangeWithAuthor() throws Exception {
		setContent("something");

		StagingViewTester stagingViewTester = StagingViewTester
				.openStagingView();
		stagingViewTester.setAuthor(TestUtil.TESTCOMMITTER);
		stagingViewTester.setCommitter(TestUtil.TESTCOMMITTER);
		stagingViewTester.setCommitMessage("Commit message");
		stagingViewTester.setSignedOff(true);
		String commitMessage = stagingViewTester.getCommitMessage();
		assertTrue("Should have a signed-off footer",
				commitMessage.indexOf("Signed-off-by") > 0);
		StoredConfig cfg = repository.getConfig();
		cfg.load();
		cfg.setString(ConfigConstants.CONFIG_USER_SECTION, null,
				ConfigConstants.CONFIG_KEY_NAME, "Some One");
		cfg.save();
		String expectedCommitter = "Some One <" + TestUtil.TESTCOMMITTER_EMAIL
				+ '>';
		assertEquals("Author should be changed", expectedCommitter,
				stagingViewTester.getAuthor());
		assertEquals("Committer should be changed", expectedCommitter,
				stagingViewTester.getCommitter());
		assertEquals("Commit message should be updated",
				commitMessage.replace(TestUtil.TESTCOMMITTER,
						expectedCommitter),
				stagingViewTester.getCommitMessage());
		assertTrue("Sign-off should be enabled",
				stagingViewTester.getSignedOff());
	}

	@Test
	public void testCommitMessageConfigChangeBranchSwitchToNew()
			throws Exception {
		setContent("something");
		StagingViewTester stagingViewTester = StagingViewTester
				.openStagingView();
		stagingViewTester.setAuthor(TestUtil.TESTCOMMITTER);
		stagingViewTester.setCommitter(TestUtil.TESTCOMMITTER);
		stagingViewTester.setCommitMessage("Commit message");
		stagingViewTester.setSignedOff(true);
		String commitMessage = stagingViewTester.getCommitMessage();
		assertTrue("Should have a signed-off footer",
				commitMessage.indexOf("Signed-off-by") > 0);
		try (Git git = new Git(repository)) {
			git.checkout().setAllPaths(true).setCreateBranch(true)
					.setName("refs/heads/myBranch").call();
		}
		TestUtil.joinJobs(
				org.eclipse.egit.core.JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		assertEquals("Author should be unchanged", TestUtil.TESTCOMMITTER,
				stagingViewTester.getAuthor());
		assertEquals("Committer should be unchanged", TestUtil.TESTCOMMITTER,
				stagingViewTester.getCommitter());
		assertEquals("Commit message should be unchanged", commitMessage,
				stagingViewTester.getCommitMessage());
		assertTrue("Sign-off should be enabled",
				stagingViewTester.getSignedOff());

	}

	@Test
	public void testCommitMessageConfigChangeAmending() throws Exception {
		// Make a commit directly -- we want to test amending a commit made by
		// someone else (for instance, fetched from Gerrit).
		setContent("something");
		try (Git git = new Git(repository)) {
			git.add().addFilepattern(FILE1_PATH).call();
			PersonIdent author = RawParseUtils
					.parsePersonIdent(TestUtil.TESTAUTHOR);
			git.commit().setAuthor(author).setCommitter(author)
					.setMessage("Author's commit\n\nSigned-off-by: "
							+ TestUtil.TESTAUTHOR)
					.call();
		}
		StagingViewTester stagingViewTester = StagingViewTester
				.openStagingView();
		stagingViewTester.setAmend(true);
		stagingViewTester.setSignedOff(true);
		String commitMessage = stagingViewTester.getCommitMessage();
		assertTrue("Commit message should have two signed-off lines",
				commitMessage.contains(TestUtil.TESTAUTHOR)
						&& commitMessage.contains(TestUtil.TESTCOMMITTER));
		StoredConfig cfg = repository.getConfig();
		cfg.load();
		cfg.setString(ConfigConstants.CONFIG_USER_SECTION, null,
				ConfigConstants.CONFIG_KEY_NAME, "Some One");
		cfg.save();
		assertEquals("Author should be unchanged", TestUtil.TESTAUTHOR,
				stagingViewTester.getAuthor());
		String expectedCommitter = "Some One <" + TestUtil.TESTCOMMITTER_EMAIL
				+ '>';
		assertEquals("Committer should be changed", expectedCommitter,
				stagingViewTester.getCommitter());
		assertTrue("Sign-off should be enabled",
				stagingViewTester.getSignedOff());
		assertTrue("Amend should be enabled", stagingViewTester.getAmend());
		commitMessage = stagingViewTester.getCommitMessage();
		assertTrue("Commit message should have two signed-off lines",
				commitMessage.contains(TestUtil.TESTAUTHOR)
						&& commitMessage.contains(expectedCommitter)
						&& !commitMessage.contains(TestUtil.TESTCOMMITTER));
	}

	@Test
	public void testButtonEnablement() throws Exception {
		StagingViewTester stagingViewTester = StagingViewTester
				.openStagingView();
		stagingViewTester.assertCommitEnabled(false);

		setContent("I have changed this");
		stagingViewTester.assertCommitEnabled(false);

		stagingViewTester.stageFile(FILE1_PATH);
		stagingViewTester.assertCommitEnabled(true);

		stagingViewTester.unStageFile(FILE1_PATH);
		stagingViewTester.assertCommitEnabled(false);

		stagingViewTester.setAmend(true);
		stagingViewTester.assertCommitEnabled(true);

		stagingViewTester.setAmend(false);
		stagingViewTester.assertCommitEnabled(false);
	}
}
