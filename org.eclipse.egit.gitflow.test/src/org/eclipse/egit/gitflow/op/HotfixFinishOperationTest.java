/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import static org.eclipse.egit.gitflow.GitFlowDefaults.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;

import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

@SuppressWarnings("restriction")
public class HotfixFinishOperationTest extends AbstractGitFlowOperationTest {
	@Test
	public void testHotfixFinish() throws Exception {
		testRepository
				.createInitialCommit("testHotfixFinish\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		new HotfixStartOperation(gfRepo, MY_HOTFIX).execute(null);

		RevCommit branchCommit = testRepository
				.createInitialCommit("testHotfixFinish\n\nbranch commit\n");

		new HotfixFinishOperation(gfRepo).execute(null);

		assertEquals(gfRepo.getDevelopFull(), repository.getFullBranch());

		String branchName = gfRepo.getHotfixBranchName(MY_HOTFIX);

		// tag created?
		assertEquals(branchCommit, gfRepo.findCommitForTag(MY_HOTFIX));

		// branch removed?
		assertEquals(findBranch(repository, branchName), null);

		RevCommit developHead = gfRepo.findHead(DEVELOP);
		assertEquals(branchCommit, developHead);

		RevCommit masterHead = gfRepo.findHead(MY_MASTER);
		assertEquals(branchCommit, masterHead);

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

		new ReleaseFinishOperation(gfRepo).execute(null);

		new HotfixStartOperation(gfRepo, MY_HOTFIX).execute(null);
		// modify on first branch
		RevCommit hotfixCommit = testRepository.appendContentAndCommit(
				project.getProject(), file, "Hello Hotfix", "Hotfix Commit");
		new BranchOperation(repository, gfRepo.getDevelop()).execute(null);
		assertEquals(gfRepo.getDevelopFull(), repository.getFullBranch());

		// modify on second branch
		RevCommit developCommit = testRepository.appendContentAndCommit(
				project.getProject(), file, "Hello Develop", "Develop Commit");

		String branchName = gfRepo.getHotfixBranchName(MY_HOTFIX);
		new BranchOperation(repository, branchName).execute(null);
		HotfixFinishOperation hotfixFinishOperation = new HotfixFinishOperation(
				gfRepo);
		hotfixFinishOperation.execute(null);

		// tag not created?
		assertNotEquals(hotfixCommit, gfRepo.findCommitForTag(MY_HOTFIX));

		// branch not removed?
		assertNotEquals(findBranch(repository, branchName), null);

		// not merged on develop => conflict
		RevCommit developHead = gfRepo.findHead(DEVELOP);
		assertEquals(developCommit, developHead);
		assertEquals(MergeResult.MergeStatus.CONFLICTING, hotfixFinishOperation
				.getOperationResult().getMergeStatus());

		// merged on master
		RevCommit masterHead = gfRepo.findHead(MY_MASTER);
		assertEquals(hotfixCommit, masterHead);

		assertEquals(gfRepo.getDevelopFull(), repository.getFullBranch());

	}
}
