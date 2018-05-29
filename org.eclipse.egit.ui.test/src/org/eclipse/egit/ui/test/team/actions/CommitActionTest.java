/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Chris Aniszczyk <caniszczyk@gmail.com> - tag API changes
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.common.CommitDialogTester;
import org.eclipse.egit.ui.common.CommitDialogTester.NoFilesToCommitPopup;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.CommitMessageUtil;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Commit action
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class CommitActionTest extends LocalRepositoryTestCase {
	private File repositoryFile;

	@Before
	public void setup() throws Exception {
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.ALWAYS_USE_STAGING_VIEW, false);
		repositoryFile = createProjectAndCommitToRepository();
		Repository repo = lookupRepository(repositoryFile);
		TestUtil.configureTestCommitterAsUser(repo);
		// TODO delete the second project for the time being (.gitignore is
		// currently not hiding the .project file from commit)
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ2);
		File dotProject = new File(project.getLocation().toOSString(), ".project");
		project.delete(false, false, null);
		assertTrue(dotProject.delete());
	}

	@After
	public void tearDown() {
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.ALWAYS_USE_STAGING_VIEW, true);
	}

	@Test
	public void testOpenCommitWithoutChanged() throws Exception {
		NoFilesToCommitPopup popup = CommitDialogTester
				.openCommitDialogExpectNoFilesToCommit(PROJ1);
		popup.cancelPopup();
	}

	@Test
	public void testCommitSingleFile() throws Exception {
		setTestFileContent("I have changed this");
		CommitDialogTester commitDialogTester = CommitDialogTester
				.openCommitDialog(PROJ1);
		assertEquals("Wrong row count", 1, commitDialogTester.getRowCount());
		assertTrue("Wrong file",
				commitDialogTester.getEntryText(0).endsWith("test.txt"));
		commitDialogTester.setAuthor(TestUtil.TESTAUTHOR);
		commitDialogTester.setCommitter(TestUtil.TESTCOMMITTER);
		commitDialogTester.setCommitMessage("The new commit");
		commitDialogTester.commit();
		TestUtil.checkHeadCommit(lookupRepository(repositoryFile),
				TestUtil.TESTAUTHOR, TestUtil.TESTCOMMITTER, "The new commit");
		NoFilesToCommitPopup popup = CommitDialogTester
				.openCommitDialogExpectNoFilesToCommit(PROJ1);
		popup.cancelPopup();
	}

	@Test
	public void testAmendWithChangeIdPreferenceOff() throws Exception {
		Repository repo = lookupRepository(repositoryFile);
		repo.getConfig().setBoolean(ConfigConstants.CONFIG_GERRIT_SECTION,
				null, ConfigConstants.CONFIG_KEY_CREATECHANGEID, true);
		setTestFileContent("Another Change");
		CommitDialogTester commitDialogTester = CommitDialogTester
				.openCommitDialog(PROJ1);
		assertEquals("Wrong row count", 1, commitDialogTester.getRowCount());
		assertTrue("Wrong file",
				commitDialogTester.getEntryText(0).endsWith("test.txt"));
		commitDialogTester.setAuthor(TestUtil.TESTAUTHOR);
		commitDialogTester.setCommitter(TestUtil.TESTCOMMITTER);
		String commitMessage = commitDialogTester.getCommitMessage();
		assertTrue(commitMessage.indexOf("Change-Id") > 0);
		String newCommitMessage = "Change to be amended \n\n" + commitMessage;
		commitDialogTester.setCommitMessage(newCommitMessage);
		commitDialogTester.commit();
		NoFilesToCommitPopup noFilesToCommitPopup = CommitDialogTester
				.openCommitDialogExpectNoFilesToCommit(PROJ1);
		repo.getConfig().setBoolean(ConfigConstants.CONFIG_GERRIT_SECTION,
				null, ConfigConstants.CONFIG_KEY_CREATECHANGEID, false);
		commitDialogTester = noFilesToCommitPopup.confirmPopup();
		assertTrue(commitDialogTester.getCommitMessage().indexOf("Change-Id") > 0);
	}

	@Test
	public void testLaunchedWithAmend() throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RevCommit oldHeadCommit = TestUtil.getHeadCommit(repository);
		commitOneFileChange("Again another Change");
		ObjectId headCommitId = TestUtil.getHeadCommit(repository).getId();
		NoFilesToCommitPopup noFilesToCommitPopup = CommitDialogTester
				.openCommitDialogExpectNoFilesToCommit(PROJ1);
		CommitDialogTester commitDialogTester = noFilesToCommitPopup.confirmPopup();
		assertTrue(commitDialogTester.getCommitMessage().indexOf("Change-Id") > 0);
		assertTrue(commitDialogTester.getCommitMessage().indexOf("Signed-off-by") > 0);
		assertTrue(commitDialogTester.getAmend());
		assertTrue(commitDialogTester.getSignedOff());
		assertTrue(commitDialogTester.getInsertChangeId());
		// change commit message to get a different SHA1 for the commit
		commitDialogTester.setCommitMessage("Changed "
				+ commitDialogTester.getCommitMessage());
		commitDialogTester.commit();
		RevCommit headCommit = TestUtil.getHeadCommit(repository);
		if(headCommitId.equals(headCommit.getId()))
			fail("There is no new commit");
		assertEquals(oldHeadCommit, headCommit.getParent(0));
	}

	private void commitOneFileChange(String fileContent) throws Exception {
		setTestFileContent(fileContent);
		CommitDialogTester commitDialogTester = CommitDialogTester
				.openCommitDialog(PROJ1);
		commitDialogTester.setShowUntracked(false);

		assertEquals("Wrong row count", 1, commitDialogTester.getRowCount());
		assertTrue("Wrong file",
				commitDialogTester.getEntryText(0).endsWith("test.txt"));
		commitDialogTester.setAuthor(TestUtil.TESTAUTHOR);
		commitDialogTester.setCommitter(TestUtil.TESTCOMMITTER);
		commitDialogTester.setCommitMessage("Commit message");
		commitDialogTester.setInsertChangeId(true);
		commitDialogTester.setSignedOff(true);

		String commitMessage = commitDialogTester.getCommitMessage();
		assertTrue(commitMessage.indexOf("Change-Id") > 0);
		assertTrue(commitMessage.indexOf("Signed-off-by") > 0);
		commitDialogTester.commit();
	}

	@Test
	public void testAmend() throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RevCommit oldHeadCommit = TestUtil.getHeadCommit(repository);
		commitOneFileChange("Yet another Change");
		RevCommit headCommit = TestUtil.getHeadCommit(repository);
		ObjectId headCommitId = headCommit.getId();
		String changeId = CommitMessageUtil.extractChangeId(headCommit
				.getFullMessage());
		setTestFileContent("Changes over changes");
		CommitDialogTester commitDialogTester = CommitDialogTester
				.openCommitDialog(PROJ1);
		commitDialogTester.setAmend(true);
		assertTrue(commitDialogTester.getCommitMessage().indexOf("Change-Id") > 0);
		assertTrue(commitDialogTester.getCommitMessage().indexOf(
				"Signed-off-by") > 0);
		assertTrue(commitDialogTester.getSignedOff());
		assertTrue(commitDialogTester.getInsertChangeId());
		commitDialogTester.commit();
		headCommit = TestUtil.getHeadCommit(repository);
		if(headCommitId.equals(headCommit.getId()))
			fail("There is no new commit");
		assertEquals(oldHeadCommit, headCommit.getParent(0));
		assertTrue(headCommit.getFullMessage().indexOf(changeId) > 0);
	}

	@Test
	public void testIncludeUntracked() throws Exception {
		boolean include = Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.COMMIT_DIALOG_INCLUDE_UNTRACKED);
		try {
			Activator
					.getDefault()
					.getPreferenceStore()
					.setValue(UIPreferences.COMMIT_DIALOG_INCLUDE_UNTRACKED,
							true);
			IProject prj = ResourcesPlugin.getWorkspace().getRoot().getProject(
					PROJ1);
			if (!prj.isAccessible())
				throw new IllegalStateException("No project found");
			IFile file = prj.getFile("untracked.txt");
			assertFalse(file.exists());
			file.create(
					new ByteArrayInputStream("new file".getBytes(prj
							.getDefaultCharset())), 0, null);
			assertTrue(file.exists());
			CommitDialogTester commitDialogTester = CommitDialogTester
					.openCommitDialog(PROJ1);
			assertEquals(1, commitDialogTester.getRowCount());
			assertTrue(commitDialogTester.isEntryChecked(0));
			String path = RepositoryMapping.getMapping(file)
					.getRepoRelativePath(file);
			assertEquals(path, commitDialogTester.getEntryText(0));
			commitDialogTester.setCommitMessage("Add new file");
			commitDialogTester.commit();
			file.delete(false, null);
		} finally {
			Activator
					.getDefault()
					.getPreferenceStore()
					.setValue(UIPreferences.COMMIT_DIALOG_INCLUDE_UNTRACKED,
							include);
		}
	}

	@Test
	public void testSortingByName() throws Exception {
		IFile fileA = touch(PROJ1, "a", "a");
		IFile fileB = touch(PROJ1, "b", "b");
		CommitDialogTester commitDialogTester = CommitDialogTester
				.openCommitDialog(PROJ1);
		commitDialogTester.setShowUntracked(true);
		assertEquals(2, commitDialogTester.getRowCount());
		assertEquals(PROJ1 + "/a", commitDialogTester.getEntryText(0));
		assertEquals(PROJ1 + "/b", commitDialogTester.getEntryText(1));
		// Sort ascending (first click changes default sort order)
		commitDialogTester.sortByName();
		// Sort descending (now the sort order should be reversed)
		commitDialogTester.sortByName();
		assertEquals(PROJ1 + "/b", commitDialogTester.getEntryText(0));
		assertEquals(PROJ1 + "/a", commitDialogTester.getEntryText(1));
		commitDialogTester.cancel();
		fileA.delete(false, null);
		fileB.delete(false, null);
	}
}
