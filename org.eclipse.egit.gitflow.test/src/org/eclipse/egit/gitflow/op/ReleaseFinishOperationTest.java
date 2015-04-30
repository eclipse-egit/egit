/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import static org.eclipse.egit.gitflow.GitFlowDefaults.DEVELOP;
import static org.eclipse.egit.gitflow.GitFlowDefaults.FEATURE_PREFIX;
import static org.eclipse.egit.gitflow.GitFlowDefaults.HOTFIX_PREFIX;
import static org.eclipse.egit.gitflow.GitFlowDefaults.MASTER;
import static org.eclipse.egit.gitflow.GitFlowDefaults.RELEASE_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.WrongGitFlowStateException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

@SuppressWarnings("restriction")
public class ReleaseFinishOperationTest extends AbstractGitFlowOperationTest {
	@Test
	public void testReleaseFinish() throws Exception {
		testRepository
				.createInitialCommit("testReleaseFinish\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository, DEVELOP, MASTER, FEATURE_PREFIX,
				RELEASE_PREFIX, HOTFIX_PREFIX, MY_VERSION_TAG).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		new ReleaseStartOperation(gfRepo, MY_RELEASE).execute(null);

		RevCommit branchCommit = testRepository
				.createInitialCommit("testReleaseFinish\n\nbranch commit\n");

		new ReleaseFinishOperation(gfRepo).execute(null);

		assertEquals(gfRepo.getDevelopFull(), repository.getFullBranch());

		String branchName = gfRepo.getReleaseBranchName(MY_RELEASE);

		// tag created?
		assertEquals(branchCommit,
				gfRepo.findCommitForTag(MY_VERSION_TAG + MY_RELEASE));

		// branch removed?
		assertEquals(findBranch(repository, branchName), null);

		RevCommit developHead = gfRepo.findHead();
		assertEquals(branchCommit, developHead);

		RevCommit masterHead = gfRepo.findHead(MY_MASTER);
		assertEquals(branchCommit, masterHead);

	}

	@Test
	public void testReleaseFinishFail() throws Exception {
		testRepository
				.createInitialCommit("testReleaseFinishFail\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		new ReleaseStartOperation(gfRepo, MY_RELEASE).execute(null);

		new BranchOperation(repository, gfRepo.getDevelop()).execute(null);

		try {
			new ReleaseFinishOperation(gfRepo).execute(null);
			fail();
		} catch (WrongGitFlowStateException e) {
			// success
		}
	}

	@Test
	public void testReleaseTagWithWrongReferenceExists() throws Exception {
		testRepository
				.createInitialCommit("testReleaseTagExists\n\nfirst commit\n");
		testRepository
				.createInitialCommit("testReleaseTagExists\n\nsecond commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		new ReleaseStartOperation(gfRepo, MY_RELEASE).execute(null);

		RevCommit next = getPreviousCommit(repository, 1);

		ReleaseFinishOperation releaseFinishOperation = new ReleaseFinishOperation(
				gfRepo);
		releaseFinishOperation.createTag(null, next, MY_RELEASE, "irrelevant");

		try {
			releaseFinishOperation.execute(null);
			fail();
		} catch (CoreException e) {
			assertFalse(e.getStatus().isOK());
		}
	}

	@Test
	public void testReleaseTagWithCorrectReferenceExists() throws Exception {
		testRepository
				.createInitialCommit("testReleaseTagExists\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		new ReleaseStartOperation(gfRepo, MY_RELEASE).execute(null);

		RevCommit next = getPreviousCommit(repository, 0);

		ReleaseFinishOperation releaseFinishOperation = new ReleaseFinishOperation(
				gfRepo);
		releaseFinishOperation.createTag(null, next, MY_RELEASE, "irrelevant");
		releaseFinishOperation.execute(null);
	}

	private RevCommit getPreviousCommit(Repository repository, int count)
			throws GitAPIException, NoHeadException {
		Iterable<RevCommit> logs = Git.wrap(repository).log().call();
		Iterator<RevCommit> i = logs.iterator();
		for (int j = 0; j < count; j++) {
			i.next();
		}
		RevCommit next = i.next();
		return next;
	}
}
