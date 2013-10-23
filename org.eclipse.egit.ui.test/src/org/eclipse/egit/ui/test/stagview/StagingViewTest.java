/*******************************************************************************
 * Copyright (C) 2011, 2013 Jens Baumgart <jens.baumgart@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.test.stagview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.common.StagingViewTester;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.test.CommitMessageUtil;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StagingViewTest extends LocalRepositoryTestCase {

	private static final GitRepositoriesViewTestUtils repoViewUtil = new GitRepositoriesViewTestUtils();

	private File repositoryFile;

	private Repository repository;

	@Before
	public void before() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		repository = lookupRepository(repositoryFile);
		TestUtil.configureTestCommitterAsUser(repository);
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(repositoryFile);
	}

	@After
	public void after() {
		Activator.getDefault().getRepositoryUtil().removeDir(repositoryFile);
	}

	@Test
	public void testCommitSingleFile() throws Exception {
		selectRepositoryNode();
		StagingViewTester stagingViewTester = StagingViewTester
				.openStagingView();
		setTestFileContent("I have changed this");
		new Git(repository).add().addFilepattern(".").call();
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		stagingViewTester.setAuthor(TestUtil.TESTAUTHOR);
		stagingViewTester.setCommitter(TestUtil.TESTCOMMITTER);
		stagingViewTester.setCommitMessage("The new commit");
		stagingViewTester.commit();
		TestUtil.checkHeadCommit(repository, TestUtil.TESTAUTHOR,
				TestUtil.TESTCOMMITTER, "The new commit");
	}

	@Test
	public void testAmend() throws Exception {
		RevCommit oldHeadCommit = TestUtil.getHeadCommit(repository);
		commitOneFileChange("Yet another Change");
		RevCommit headCommit = TestUtil.getHeadCommit(repository);
		ObjectId headCommitId = headCommit.getId();
		String changeId = CommitMessageUtil.extractChangeId(headCommit
				.getFullMessage());
		setTestFileContent("Changes over changes");
		new Git(repository).add().addFilepattern(".").call();
		StagingViewTester stagingViewTester = StagingViewTester
				.openStagingView();
		stagingViewTester.setAmend(true);
		assertTrue(stagingViewTester.getCommitMessage().indexOf("Change-Id") > 0);
		assertTrue(stagingViewTester.getCommitMessage()
				.indexOf("Signed-off-by") > 0);
		assertTrue(stagingViewTester.getSignedOff());
		assertTrue(stagingViewTester.getInsertChangeId());
		stagingViewTester.commit();
		headCommit = TestUtil.getHeadCommit(repository);
		if(headCommitId.equals(headCommit.getId()))
			fail("There is no new commit");
		assertEquals(oldHeadCommit, headCommit.getParent(0));
		assertTrue(headCommit.getFullMessage().indexOf(changeId) > 0);
	}

	private void commitOneFileChange(String fileContent) throws Exception {
		setTestFileContent(fileContent);
		new Git(repository).add().addFilepattern(".").call();
		selectRepositoryNode(); // update staging view after add
		// TODO: adding should be done by using staging view.
		// Can be done when add is available via context menu
		StagingViewTester stagingViewTester = StagingViewTester
				.openStagingView();
		stagingViewTester.setAuthor(TestUtil.TESTAUTHOR);
		stagingViewTester.setCommitter(TestUtil.TESTCOMMITTER);
		stagingViewTester.setCommitMessage("Commit message");
		stagingViewTester.setInsertChangeId(true);
		stagingViewTester.setSignedOff(true);
		String commitMessage = stagingViewTester.getCommitMessage();
		assertTrue(commitMessage.indexOf("Change-Id") > 0);
		assertTrue(commitMessage.indexOf("Signed-off-by") > 0);
		stagingViewTester.commit();
	}

	private void selectRepositoryNode() throws Exception {
		SWTBotView repositoriesView = TestUtil
				.showView(RepositoriesView.VIEW_ID);
		SWTBotTree tree = repositoriesView.bot().tree();

		SWTBotTreeItem repoNode = repoViewUtil
				.getRootItem(tree, repositoryFile);
		repoNode.select();
	}
}
