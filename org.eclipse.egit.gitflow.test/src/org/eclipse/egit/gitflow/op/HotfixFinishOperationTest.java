/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import static org.eclipse.egit.gitflow.GitFlowDefaults.DEVELOP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

public class HotfixFinishOperationTest extends AbstractGitFlowOperationTest {
	@Test
	public void testHotfixFinishSingleCommit() throws Exception {
		testRepository
				.createInitialCommit("testHotfixFinish\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		new HotfixStartOperation(gfRepo, MY_HOTFIX).execute(null);

		RevCommit branchCommit = testRepository
				.createInitialCommit("testHotfixFinish\n\nbranch commit\n");

		new HotfixFinishOperation(gfRepo).execute(null);

		assertEquals(gfRepo.getConfig().getDevelopFull(), repository.getFullBranch());

		String branchName = gfRepo.getConfig().getHotfixBranchName(MY_HOTFIX);

		// tag created?
		RevCommit taggedCommit = gfRepo.findCommitForTag(MY_HOTFIX);
		assertEquals(formatMergeCommitMessage(branchName), taggedCommit.getShortMessage());

		// branch removed?
		assertEquals(findBranch(repository, branchName), null);

		RevCommit developHead = gfRepo.findHead(DEVELOP);
		assertNotEquals(branchCommit, developHead);

		RevCommit masterHead = gfRepo.findHead(MY_MASTER);
		assertEquals(formatMergeCommitMessage(branchName), masterHead.getShortMessage());
	}

	@Test
	public void testMergeToDevelopFail() throws Exception {
		testRepository
				.createInitialCommit("testMergeToDevelopFail\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		// setup something we can modify later
		File file = testRepository.createFile(project.getProject(),
				"folder1/file1.txt");

		new ReleaseStartOperation(gfRepo, MY_RELEASE).execute(null);

		testRepository.appendContentAndCommit(project.getProject(), file,
				"Hello Release", "Release Commit");
		testRepository.appendContentAndCommit(project.getProject(), file,
				"Hello Merge Commit", "Release Commit 2");

		new ReleaseFinishOperation(gfRepo).execute(null);

		new HotfixStartOperation(gfRepo, MY_HOTFIX).execute(null);
		// modify on first branch
		testRepository.appendContentAndCommit(
				project.getProject(), file, "Hello Hotfix", "Hotfix Commit");
		new BranchOperation(repository, gfRepo.getConfig().getDevelop()).execute(null);
		assertEquals(gfRepo.getConfig().getDevelopFull(), repository.getFullBranch());

		// modify on second branch
		RevCommit developCommit = testRepository.appendContentAndCommit(
				project.getProject(), file, "Hello Develop", "Develop Commit");

		String branchName = gfRepo.getConfig().getHotfixBranchName(MY_HOTFIX);
		new BranchOperation(repository, branchName).execute(null);
		HotfixFinishOperation hotfixFinishOperation = new HotfixFinishOperation(
				gfRepo);
		hotfixFinishOperation.execute(null);

		// TODO: check if the reference implementation cleans up in this case
		assertNotNull(gfRepo.findCommitForTag(MY_HOTFIX));

		// branch not removed?
		assertNotEquals(findBranch(repository, branchName), null);

		// not merged on develop => conflict
		RevCommit developHead = gfRepo.findHead(DEVELOP);
		assertEquals(developCommit, developHead);
		assertEquals(MergeResult.MergeStatus.CONFLICTING, hotfixFinishOperation
				.getMergeResult().getMergeStatus());

		// merged on master
		RevCommit masterHead = gfRepo.findHead(MY_MASTER);
		assertEquals(String.format("Merge branch '%s'", branchName), masterHead.getFullMessage());

		assertEquals(gfRepo.getConfig().getDevelopFull(), repository.getFullBranch());
	}
}
