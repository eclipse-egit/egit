/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import static org.junit.Assert.assertEquals;

import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.WrongGitFlowStateException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

@SuppressWarnings("restriction")
public class FeatureFinishOperationTest extends AbstractFeatureOperationTest {
	@Test
	public void testFeatureFinish() throws Exception {
		testRepository
				.createInitialCommit("testFeatureFinish\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		new FeatureStartOperation(gfRepo, MY_FEATURE).execute(null);

		new FeatureFinishOperation(gfRepo).execute(null);

		assertEquals(gfRepo.getDevelopFull(), repository.getFullBranch());

		String branchName = gfRepo.getFeatureBranchName(MY_FEATURE);

		RevCommit branchCommit = testRepository
				.createInitialCommit("testFeatureFinish\n\nbranch commit\n");

		assertEquals(findBranch(repository, branchName), null);

		RevCommit developHead = gfRepo.findHead();
		assertEquals(branchCommit, developHead);
	}

	@Test(expected = WrongGitFlowStateException.class)
	public void testFeatureFinishFail() throws Exception {
		testRepository
				.createInitialCommit("testFeatureFinishFail\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		new FeatureStartOperation(gfRepo, MY_FEATURE).execute(null);

		new BranchOperation(repository, gfRepo.getDevelop()).execute(null);

		new FeatureFinishOperation(gfRepo).execute(null);
	}
}
