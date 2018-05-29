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

import static org.eclipse.jgit.lib.Constants.DEFAULT_REMOTE_NAME;
import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.junit.Test;

public class CurrentBranchPublishOperationTest extends AbstractDualRepositoryTestCase {
	@Test
	public void testFeaturePublish() throws Exception {
		new InitOperation(repository2.getRepository()).execute(null);
		GitFlowRepository gfRepo2 = new GitFlowRepository(
				repository2.getRepository());

		new FeatureStartOperation(gfRepo2, MY_FEATURE).execute(null);
		RevCommit branchCommit = repository2
				.createInitialCommit("testFeaturePublish");
		CurrentBranchPublishOperation featurePublishOperation = new CurrentBranchPublishOperation(
				gfRepo2, 0);
		featurePublishOperation.execute(null);
		PushOperationResult result = featurePublishOperation
				.getOperationResult();

		assertTrue(result.isSuccessfulConnection(repository1.getUri()));
		PushResult pushResult = result.getPushResult(repository1.getUri());
		assertEquals(RefUpdate.Result.NEW, pushResult.getTrackingRefUpdates()
				.iterator().next().getResult());

		assertCommitArrivedAtRemote(branchCommit, repository1.getRepository());

		// config updated?
		assertEquals(DEFAULT_REMOTE_NAME, getRemoteName(gfRepo2, MY_FEATURE));
		assertEquals(R_HEADS + gfRepo2.getConfig().getFeatureBranchName(MY_FEATURE),
				gfRepo2.getUpstreamBranchName(MY_FEATURE));
	}
}
