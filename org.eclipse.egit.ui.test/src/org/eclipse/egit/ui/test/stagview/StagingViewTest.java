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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.ui.common.StagingViewTester;
import org.eclipse.egit.ui.test.CommitMessageUtil;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.RawParseUtils;
import org.junit.Test;

public class StagingViewTest extends AbstractStagingViewTestCase {

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
		TestUtil.processUIEvents();
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
		TestUtil.processUIEvents();
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
		TestUtil.processUIEvents();
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
		TestUtil.processUIEvents();
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
		TestUtil.processUIEvents();
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
}
