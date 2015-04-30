/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import static org.junit.Assert.*;

import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

public class FeatureRebaseOperationTest extends AbstractFeatureOperationTest {
	@Test
	public void testFeatureRebase() throws Exception {

		RevCommit initialCommit = testRepository
				.createInitialCommit("testFeatureRebase\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		new FeatureStartOperation(gfRepo, MY_FEATURE).execute(null);
		String branchCommitMessage = "adding first file on feature branch";
		addFileAndCommit("theFile.txt", branchCommitMessage);

		testRepository.checkoutBranch(gfRepo.getDevelop());
		RevCommit developCommit = addFileAndCommit("theOtherFile.txt",
				"adding second file on develop branch");

		new FeatureCheckoutOperation(gfRepo, MY_FEATURE).execute(null);
		assertEquals(initialCommit, gfRepo.findHead().getParent(0));
		FeatureRebaseOperation featureRebaseOperation = new FeatureRebaseOperation(
				gfRepo);
		featureRebaseOperation.execute(null);

		RebaseResult res = featureRebaseOperation.getOperationResult();
		assertEquals(RebaseResult.Status.OK, res.getStatus());

		assertEquals(branchCommitMessage, gfRepo.findHead().getShortMessage());
		assertEquals(developCommit,
				findCommit(repository, repository.resolve("HEAD^")));
	}
}
