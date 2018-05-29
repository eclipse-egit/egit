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

import static org.junit.Assert.*;

import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

public class FeatureRebaseOperationTest extends AbstractFeatureOperationTest {
	@Test
	public void testFeatureRebase() throws Exception {
		Repository repository = testRepository.getRepository();
		GitFlowRepository gfRepo = init("testFeatureRebase\n\nfirst commit\n");
		RevCommit initialCommit = gfRepo.findHead();

		new FeatureStartOperation(gfRepo, MY_FEATURE).execute(null);
		String branchCommitMessage = "adding first file on feature branch";
		addFileAndCommit("theFile.txt", branchCommitMessage);

		testRepository.checkoutBranch(gfRepo.getConfig().getDevelop());
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
				parseCommit(repository, repository.resolve("HEAD^")));
	}
}
