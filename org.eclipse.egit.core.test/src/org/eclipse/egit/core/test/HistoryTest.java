/*******************************************************************************
 * Copyright (C) 2013, Laurent Goubet <laurent.goubet@obeo.fr>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HistoryTest extends GitTestCase {
	private static final String MASTER = Constants.R_HEADS + Constants.MASTER;

	private static final String BRANCH = Constants.R_HEADS + "branch";

	private TestRepository testRepository;

	private IFile iFile1;

	private IFile iFile2;

	private final List<RevCommit> commits = new ArrayList<>();

	private RevCommit masterCommit1;

	private RevCommit masterCommit2;

	private RevCommit masterCommit3;

	private RevCommit branchCommit1;

	private RevCommit branchCommit2;

	private IFileHistoryProvider historyProvider;

	@Override
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
		branchCommit2 = testRepository.appendContentAndCommit(
				project.getProject(), file2, "branch-commit-2",
				"branch-commit-2");

		commits.add(masterCommit1);
		commits.add(masterCommit2);
		commits.add(masterCommit3);
		commits.add(branchCommit1);

		historyProvider = RepositoryProvider.getProvider(project.getProject())
				.getFileHistoryProvider();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		super.tearDown();
	}

	@Test
	public void queryFile1FullHistory() throws CoreException {
		final List<RevCommit> expectedHistory = Arrays.asList(masterCommit1,
				masterCommit3, branchCommit1);
		assertFullHistoryMatches(iFile1, expectedHistory);
	}

	@Test
	public void queryFile2FullHistory() throws CoreException {
		final List<RevCommit> expectedHistory = Arrays.asList(masterCommit2,
				masterCommit3, branchCommit2);
		assertFullHistoryMatches(iFile2, expectedHistory);
	}

	private void assertFullHistoryMatches(IFile target,
			List<RevCommit> expectedHistory) throws CoreException {
		// Whatever the position of HEAD, the history should be the same
		for (RevCommit commit : commits) {
			testRepository.checkoutBranch(commit.getName());
			final IFileHistory history = historyProvider.getFileHistoryFor(
					target, IFileHistoryProvider.NONE,
					new NullProgressMonitor());
			assertNotNull(history);

			final IFileRevision[] revisions = history.getFileRevisions();
			assertEquals(expectedHistory.size(), revisions.length);
			final List<RevCommit> commitList = new ArrayList<>(
					expectedHistory);
			assertMatchingRevisions(Arrays.asList(revisions), commitList);
		}
	}

	@Test
	public void querySingleRevisions() throws CoreException {
		for (RevCommit commit : commits) {
			for (IFile target : Arrays.asList(iFile1, iFile2)) {
				testRepository.checkoutBranch(commit.getName());
				final IFileHistory history = historyProvider.getFileHistoryFor(
						target, IFileHistoryProvider.SINGLE_REVISION,
						new NullProgressMonitor());
				assertNotNull(history);

				final IFileRevision[] revisions = history.getFileRevisions();
				assertEquals(1, revisions.length);
				assertRevisionMatchCommit(revisions[0], commit);
			}
		}
	}

	@Test
	public void queryFile1Contributors() {
		final IFileHistory history = historyProvider.getFileHistoryFor(iFile1,
				IFileHistoryProvider.NONE, new NullProgressMonitor());
		assertNotNull(history);

		final IFileRevision[] revisions = history.getFileRevisions();
		IFileRevision branchFileRevision1 = null;
		IFileRevision masterFileRevision3 = null;
		IFileRevision masterFileRevision1 = null;
		for (IFileRevision revision : revisions) {
			final String revisionId = revision.getContentIdentifier();
			if (branchCommit1.getName().equals(revisionId))
				branchFileRevision1 = revision;
			else if (masterCommit3.getName().equals(revisionId))
				masterFileRevision3 = revision;
			else if (masterCommit1.getName().equals(revisionId))
				masterFileRevision1 = revision;
		}
		assertNotNull(branchFileRevision1);
		assertNotNull(masterFileRevision3);
		assertNotNull(masterFileRevision1);

		/*
		 * The "direct" parent of branchCommit1 is masterCommit2. However, that
		 * commit did not contain file1. We thus expect the returned contributor
		 * to be masterCommit1.
		 */
		final IFileRevision[] branchCommit1Parents = history
				.getContributors(branchFileRevision1);
		assertEquals(1, branchCommit1Parents.length);
		assertRevisionMatchCommit(branchCommit1Parents[0], masterCommit1);

		// Likewise for masterCommit3
		final IFileRevision[] masterCommit3Parents = history
				.getContributors(masterFileRevision3);
		assertEquals(1, masterCommit3Parents.length);
		assertRevisionMatchCommit(masterCommit3Parents[0], masterCommit1);

		// masterCommit1 is our initial commit
		final IFileRevision[] masterCommit1Parents = history
				.getContributors(masterFileRevision1);
		assertEquals(0, masterCommit1Parents.length);
	}

	@Test
	public void queryFile2Contributors() {
		final IFileHistory history = historyProvider.getFileHistoryFor(iFile2,
				IFileHistoryProvider.NONE, new NullProgressMonitor());
		assertNotNull(history);

		final IFileRevision[] revisions = history.getFileRevisions();
		IFileRevision masterFileRevision3 = null;
		IFileRevision masterFileRevision2 = null;
		IFileRevision branchFileRevision2 = null;
		for (IFileRevision revision : revisions) {
			final String revisionId = revision.getContentIdentifier();
			if (masterCommit3.getName().equals(revisionId))
				masterFileRevision3 = revision;
			else if (masterCommit2.getName().equals(revisionId))
				masterFileRevision2 = revision;
			else if (branchCommit2.getName().equals(revisionId))
				branchFileRevision2 = revision;
		}
		assertNotNull(masterFileRevision3);
		assertNotNull(masterFileRevision2);
		assertNotNull(branchFileRevision2);

		final IFileRevision[] masterCommit3Parents = history
				.getContributors(masterFileRevision3);
		assertEquals(1, masterCommit3Parents.length);
		assertRevisionMatchCommit(masterCommit3Parents[0], masterCommit2);

		/*
		 * The direct parent of masterCommit2 is the initial commit,
		 * masterCommit1. However, file2 was not included in that commit. We
		 * thus expect no parent.
		 */
		final IFileRevision[] masterCommit2Parents = history
				.getContributors(masterFileRevision2);
		assertEquals(0, masterCommit2Parents.length);

		final IFileRevision[] branchCommit2Parents = history
				.getContributors(branchFileRevision2);
		assertEquals(1, branchCommit2Parents.length);
		assertRevisionMatchCommit(branchCommit2Parents[0], masterCommit2);
	}

	@Test
	public void queryFile1Targets() {
		final IFileHistory history = historyProvider.getFileHistoryFor(iFile1,
				IFileHistoryProvider.NONE, new NullProgressMonitor());
		assertNotNull(history);

		final IFileRevision[] revisions = history.getFileRevisions();
		IFileRevision branchFileRevision1 = null;
		IFileRevision masterFileRevision3 = null;
		IFileRevision masterFileRevision1 = null;
		for (IFileRevision revision : revisions) {
			final String revisionId = revision.getContentIdentifier();
			if (branchCommit1.getName().equals(revisionId))
				branchFileRevision1 = revision;
			else if (masterCommit3.getName().equals(revisionId))
				masterFileRevision3 = revision;
			else if (masterCommit1.getName().equals(revisionId))
				masterFileRevision1 = revision;
		}
		assertNotNull(branchFileRevision1);
		assertNotNull(masterFileRevision3);
		assertNotNull(masterFileRevision1);

		/*
		 * The "direct" child of masterCommit1 is masterCommit2. However, that
		 * commit did not contain file1. We thus expect the returned children to
		 * be masterCommit3 and branchCommit1, since the ignored masterCommit2
		 * is a branching point.
		 */
		final IFileRevision[] masterCommit1Children = history
				.getTargets(masterFileRevision1);
		assertEquals(2, masterCommit1Children.length);
		final List<RevCommit> expected = new ArrayList<>(
				Arrays.asList(masterCommit3, branchCommit1));
		assertMatchingRevisions(Arrays.asList(masterCommit1Children), expected);

		// masterCommit3 and branchCommit1 are leafs
		final IFileRevision[] masterCommit3Children = history
				.getTargets(masterFileRevision3);
		assertEquals(0, masterCommit3Children.length);

		final IFileRevision[] branchCommit1Children = history
				.getTargets(branchFileRevision1);
		assertEquals(0, branchCommit1Children.length);
	}

	@Test
	public void queryFile2Targets() {
		final IFileHistory history = historyProvider.getFileHistoryFor(iFile2,
				IFileHistoryProvider.NONE, new NullProgressMonitor());
		assertNotNull(history);

		final IFileRevision[] revisions = history.getFileRevisions();
		IFileRevision masterFileRevision3 = null;
		IFileRevision masterFileRevision2 = null;
		IFileRevision branchFileRevision2 = null;
		for (IFileRevision revision : revisions) {
			final String revisionId = revision.getContentIdentifier();
			if (masterCommit3.getName().equals(revisionId))
				masterFileRevision3 = revision;
			else if (masterCommit2.getName().equals(revisionId))
				masterFileRevision2 = revision;
			else if (branchCommit2.getName().equals(revisionId))
				branchFileRevision2 = revision;
		}
		assertNotNull(masterFileRevision3);
		assertNotNull(masterFileRevision2);
		assertNotNull(branchFileRevision2);

		final IFileRevision[] masterCommit2Children = history
				.getTargets(masterFileRevision2);
		assertEquals(2, masterCommit2Children.length);
		assertTrue(Arrays.asList(masterCommit2Children).contains(
				masterFileRevision3));
		assertTrue(Arrays.asList(masterCommit2Children).contains(
				branchFileRevision2));

		final IFileRevision[] masterCommit3Children = history
				.getTargets(masterFileRevision3);
		assertEquals(0, masterCommit3Children.length);

		final IFileRevision[] branchCommit2Children = history
				.getTargets(branchFileRevision2);
		assertEquals(0, branchCommit2Children.length);
	}

	/*
	 * This aims at exerting the behavior of the EGit history provider when used
	 * through the Team APIs. This is the behavior extenders will see when
	 * interfacing with EGit through the synchronize view.
	 *
	 * The exact comparison with which we've reached the synchronize perspective
	 * should not be relevant. To keep this test as short as possible, we'll
	 * only test a single comparison.
	 */
	@Test
	public void queryHistoryThroughTeam() throws IOException, CoreException {
		GitSynchronizeData gsd = new GitSynchronizeData(
				testRepository.getRepository(), MASTER, BRANCH, false);
		GitSynchronizeDataSet gsds = new GitSynchronizeDataSet(gsd);
		GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				gsds);
		subscriber.init(new NullProgressMonitor());

		IDiff diff = subscriber.getDiff(iFile2);
		assertTrue(diff instanceof IThreeWayDiff);

		IFileRevision sourceRevision = getSource(diff);
		IFileRevision destinationRevision = getDestination(diff);
		IFileRevision baseRevision = getBase(diff);

		assertRevisionMatchCommit(baseRevision, masterCommit2);
		assertRevisionMatchCommit(destinationRevision, branchCommit2);
		assertRevisionMatchCommit(sourceRevision, masterCommit3);

		final IFileHistory history = historyProvider.getFileHistoryFor(iFile2,
				IFileHistoryProvider.NONE,
				new NullProgressMonitor());
		assertNotNull(history);

		// no parent of masterCommit2 in file2's history
		IFileRevision[] parents = history.getContributors(baseRevision);
		assertEquals(0, parents.length);

		/*
		 * branchCommit1 did not contain file2, so the "child" of masterCommit2
		 * (branching point) in file2's history is branchCommit2.
		 */
		IFileRevision[] children = history.getTargets(baseRevision);
		List<RevCommit> expectedChildren = new ArrayList<>(
				Arrays.asList(masterCommit3, branchCommit2));
		assertEquals(expectedChildren.size(), children.length);
		assertMatchingRevisions(Arrays.asList(children), expectedChildren);
	}

	private static IFileRevision getSource(IDiff diff) {
		if (diff instanceof IResourceDiff)
			return ((IResourceDiff) diff).getBeforeState();

		if (diff instanceof IThreeWayDiff) {
			final IThreeWayDiff twd = (IThreeWayDiff) diff;
			final IDiff localChange = twd.getLocalChange();
			if (localChange instanceof IResourceDiff)
				return ((IResourceDiff) localChange).getAfterState();
		}

		return null;
	}

	private static IFileRevision getDestination(IDiff diff) {
		if (diff instanceof IResourceDiff)
			return ((IResourceDiff) diff).getAfterState();

		if (diff instanceof IThreeWayDiff) {
			final IThreeWayDiff twd = (IThreeWayDiff) diff;
			final IDiff remoteChange = twd.getRemoteChange();
			if (remoteChange instanceof IResourceDiff)
				return ((IResourceDiff) remoteChange).getAfterState();

			final IDiff localChange = twd.getLocalChange();
			if (localChange instanceof IResourceDiff)
				return ((IResourceDiff) localChange).getBeforeState();
		}

		return null;
	}

	private static IFileRevision getBase(IDiff diff) {
		if (diff instanceof IThreeWayDiff) {
			final IThreeWayDiff twd = (IThreeWayDiff) diff;
			final IDiff remoteChange = twd.getRemoteChange();
			if (remoteChange instanceof IResourceDiff)
				return ((IResourceDiff) remoteChange).getBeforeState();

			final IDiff localChange = twd.getLocalChange();
			if (localChange instanceof IResourceDiff)
				return ((IResourceDiff) localChange).getBeforeState();
		}

		return null;
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
