/*******************************************************************************
 * Copyright (C) 2013, Laurent Goubet <laurent.goubet@obeo.fr>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HistoryTest extends GitTestCase {
	private static final String MASTER = Constants.R_HEADS + Constants.MASTER;

	private static final String BRANCH = Constants.R_HEADS + "branch";

	private TestRepository testRepository;

	private IFile iFile1;

	private IFile iFile2;

	private final List<RevCommit> commits = new ArrayList<RevCommit>();

	private RevCommit masterCommit1;

	private RevCommit masterCommit2;

	private RevCommit masterCommit3;

	private RevCommit branchCommit1;

	private IFileHistoryProvider historyProvider;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		testRepository = new TestRepository(gitDir);
		testRepository.connect(project.getProject());

		File file1 = testRepository.createFile(project.getProject(), "file1");
		File file2 = testRepository.createFile(project.getProject(), "file2");

		iFile1 = testRepository.getIFile(project.getProject(), file1);
		iFile2 = testRepository.getIFile(project.getProject(), file2);

		masterCommit1 = testRepository.addAndCommit(project.getProject(),
				file1, "master-commit-1");
		masterCommit2 = testRepository.addAndCommit(project.getProject(),
				file2, "master-commit-2");
		testRepository.createBranch(MASTER, BRANCH);

		testRepository.appendFileContent(file1, "master-commit-3");
		testRepository.appendFileContent(file2, "master-commit-3");
		testRepository.track(file1);
		testRepository.track(file2);
		testRepository.addToIndex(project.getProject(), file1);
		testRepository.addToIndex(project.getProject(), file2);
		masterCommit3 = testRepository.commit("master-commit-3");

		testRepository.checkoutBranch(BRANCH);
		branchCommit1 = testRepository.appendContentAndCommit(
				project.getProject(), file1, "branch-commit-1",
				"branch-commit-1");

		commits.add(masterCommit1);
		commits.add(masterCommit2);
		commits.add(masterCommit3);
		commits.add(branchCommit1);

		historyProvider = RepositoryProvider.getProvider(project.getProject()).getFileHistoryProvider();
	}

	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		super.tearDown();
	}

	@Test
	public void queryFile1FullHistory() throws CoreException {
		// Whatever the position of HEAD, the history should be the same
		final List<RevCommit> expectedHistory = Arrays.asList(masterCommit1,
				masterCommit3, branchCommit1);
		for (RevCommit ref : commits) {
			testRepository.checkoutBranch(ref.getName());
			final IFileHistory history = historyProvider.getFileHistoryFor(
					iFile1, IFileHistoryProvider.NONE,
					new NullProgressMonitor());
			assertNotNull(history);

			final IFileRevision[] revisions = history.getFileRevisions();
			assertEquals(3, revisions.length);
			final List<RevCommit> commitList = new ArrayList<RevCommit>(
					expectedHistory);
			assertMatchingRevisions(Arrays.asList(revisions), commitList);
		}
	}

	@Test
	public void queryFile2FullHistory() throws CoreException {
		// Whatever the position of HEAD, the history should be the same
		final List<RevCommit> expectedHistory = Arrays.asList(masterCommit2,
				masterCommit3);
		for (RevCommit ref : commits) {
			testRepository.checkoutBranch(ref.getName());
			final IFileHistory history = historyProvider.getFileHistoryFor(
					iFile2, IFileHistoryProvider.NONE,
					new NullProgressMonitor());
			assertNotNull(history);

			final IFileRevision[] revisions = history.getFileRevisions();
			assertEquals(2, revisions.length);
			final List<RevCommit> commitList = new ArrayList<RevCommit>(
					expectedHistory);
			assertMatchingRevisions(Arrays.asList(revisions), commitList);
		}
	}

	@Test
	public void queryFile1SingleRevision() throws CoreException {
		for (RevCommit ref : commits) {
			testRepository.checkoutBranch(ref.getName());
			final IFileHistory history = historyProvider.getFileHistoryFor(
					iFile1, IFileHistoryProvider.SINGLE_REVISION,
					new NullProgressMonitor());
			assertNotNull(history);

			final IFileRevision[] revisions = history.getFileRevisions();
			assertEquals(1, revisions.length);
			assertRevisionMatchCommit(revisions[0], ref);
		}
	}

	@Test
	public void queryFile2SingleRevision() throws CoreException {
		for (RevCommit ref : commits) {
			testRepository.checkoutBranch(ref.getName());
			final IFileHistory history = historyProvider.getFileHistoryFor(
					iFile2, IFileHistoryProvider.SINGLE_REVISION,
					new NullProgressMonitor());
			assertNotNull(history);

			final IFileRevision[] revisions = history.getFileRevisions();
			assertEquals(1, revisions.length);
			assertRevisionMatchCommit(revisions[0], ref);
		}
	}

	private static void assertRevisionMatchCommit(IFileRevision revision,
			RevCommit commit) {
		assertEquals(commit.getAuthorIdent().getName(), revision.getAuthor());
		assertEquals(commit.getFullMessage(), revision.getComment());
		assertEquals(commit.getName(), revision.getContentIdentifier());
		// Git is in seconds, Team in milliseconds
		assertEquals(commit.getCommitTime(), revision.getTimestamp() / 1000);
	}

	private static void assertMatchingRevisions(List<IFileRevision> revisions,
			List<RevCommit> commits) {
		assertEquals(commits.size(), revisions.size());
		// Copy list : we'll empty it as we go
		for (IFileRevision revision : revisions) {
			boolean foundMatch = false;
			final Iterator<RevCommit> commitIterator = commits.iterator();
			while (commitIterator.hasNext() && !foundMatch) {
				final RevCommit commit = commitIterator.next();
				if (revision.getContentIdentifier().equals(commit.getName())) {
					assertRevisionMatchCommit(revision, commit);
					foundMatch = true;
					commitIterator.remove();
				}
			}
			assertTrue(foundMatch);
		}
		assertTrue(commits.isEmpty());
	}
}
