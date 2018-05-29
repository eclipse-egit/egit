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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.WrongGitFlowStateException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

public class FeatureFinishOperationTest extends AbstractFeatureOperationTest {
	@Test
	public void testFeatureFinishFastForward() throws Exception {
		String fileName = "theFirstFile.txt";
		Repository repository = testRepository.getRepository();
		GitFlowRepository gfRepo = init("testFeatureFinish\n\nfirst commit\n");

		new FeatureStartOperation(gfRepo, MY_FEATURE).execute(null);
		RevCommit branchCommit = addFileAndCommit(fileName, "adding file on feature branch");
		new FeatureFinishOperation(gfRepo).execute(null);
		assertEquals(gfRepo.getConfig().getDevelopFull(), repository.getFullBranch());

		String branchName = gfRepo.getConfig().getFeatureBranchName(MY_FEATURE);
		assertNull(findBranch(repository, branchName));

		RevCommit developHead = gfRepo.findHead();
		assertEquals(branchCommit, developHead);

		assertEquals(2, countCommits(repository));
		assertTrue(new File(repository.getDirectory() + "/../" + fileName).exists());
	}

	@Test
	public void testFeatureFinishSquash() throws Exception {
		String fileName = "theFirstFile.txt";
		String fileName2 = "theSecondFile.txt";

		Repository repository = testRepository.getRepository();
		GitFlowRepository gfRepo = init("testFeatureFinishSquash\n\nfirst commit\n");

		new FeatureStartOperation(gfRepo, MY_FEATURE).execute(null);

		String branchName = gfRepo.getConfig().getFeatureBranchName(MY_FEATURE);
		addFileAndCommit(fileName, "adding first file on feature branch");
		addFileAndCommit(fileName2, "adding second file on feature branch");
		FeatureFinishOperation featureFinishOperation = new FeatureFinishOperation(gfRepo);
		featureFinishOperation.setSquash(true);
		featureFinishOperation.execute(null);
		assertEquals(gfRepo.getConfig().getDevelopFull(),
				repository.getFullBranch());
		assertEquals(null, findBranch(repository, branchName));

		assertEquals(1, countCommits(repository));
		assertTrue(new File(repository.getDirectory() + "/../" + fileName).exists());
		assertTrue(new File(repository.getDirectory() + "/../" + fileName2).exists());

		Status status = Git.wrap(repository).status().call();
		assertTrue(status.hasUncommittedChanges());
	}

	@Test
	public void testFeatureFinish() throws Exception {
		Repository repository = testRepository.getRepository();
		GitFlowRepository gfRepo = init("testFeatureFinish\n\nfirst commit\n");

		new FeatureStartOperation(gfRepo, MY_FEATURE).execute(null);
		addFileAndCommit("foo.txt", "testFeatureFinish\n\nbranch commit 1\n");
		addFileAndCommit("bar.txt", "testFeatureFinish\n\nbranch commit 2\n");
		new FeatureFinishOperation(gfRepo).execute(null);
		assertEquals(gfRepo.getConfig().getDevelopFull(),
				repository.getFullBranch());

		String branchName = gfRepo.getConfig().getFeatureBranchName(MY_FEATURE);

		assertEquals(formatMergeCommitMessage(branchName) + " into develop", gfRepo.findHead()
				.getFullMessage());
	}

	@Test
	public void testFeatureFinishKeepBranch() throws Exception {
		Repository repository = testRepository.getRepository();
		GitFlowRepository gfRepo = init("testFeatureFinishKeepBranch\n\nfirst commit\n");

		new FeatureStartOperation(gfRepo, MY_FEATURE).execute(null);
		addFileAndCommit("foo.txt", "testFeatureFinishKeepBranch\n\nbranch commit 1\n");
		addFileAndCommit("bar.txt", "testFeatureFinishKeepBranch\n\nbranch commit 2\n");
		FeatureFinishOperation featureFinishOperation = new FeatureFinishOperation(gfRepo);
		featureFinishOperation.setKeepBranch(true);
		featureFinishOperation.execute(null);
		assertEquals(gfRepo.getConfig().getDevelopFull(),
				repository.getFullBranch());

		String branchName = gfRepo.getConfig().getFeatureBranchName(MY_FEATURE);
		assertNotNull(findBranch(repository, branchName));

		assertEquals(formatMergeCommitMessage(branchName) + " into develop", gfRepo.findHead()
				.getFullMessage());
	}

	@Test(expected = WrongGitFlowStateException.class)
	public void testFeatureFinishFail() throws Exception {
		Repository repository = testRepository.getRepository();
		GitFlowRepository gfRepo = init("testFeatureFinishFail\n\nfirst commit\n");

		new FeatureStartOperation(gfRepo, MY_FEATURE).execute(null);
		new BranchOperation(repository, gfRepo.getConfig().getDevelop()).execute(null);
		new FeatureFinishOperation(gfRepo).execute(null);
	}
}
