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
package org.eclipse.egit.gitflow;

import static org.eclipse.egit.gitflow.BranchNameValidator.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.egit.gitflow.op.AbstractGitFlowOperationTest;
import org.eclipse.egit.gitflow.op.FeatureStartOperation;
import org.eclipse.egit.gitflow.op.InitOperation;
import org.eclipse.egit.gitflow.op.ReleaseStartOperation;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

public class BranchNameValidatorTest extends AbstractGitFlowOperationTest {

	@Test
	public void testFeatureExists() throws Exception {
		testRepository
				.createInitialCommit("testInitOperation\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);
		new FeatureStartOperation(gfRepo, MY_FEATURE).execute(null);

		assertTrue(featureExists(gfRepo, MY_FEATURE));
	}

	@Test
	public void testReleaseExists() throws Exception {
		testRepository
				.createInitialCommit("testInitOperation\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);
		new ReleaseStartOperation(gfRepo, MY_RELEASE).execute(null);

		assertTrue(releaseExists(gfRepo, MY_RELEASE));
	}

	@Test
	public void testBranchNotExists() throws Exception {
		testRepository
				.createInitialCommit("testInitOperation\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);
		new ReleaseStartOperation(gfRepo, MY_RELEASE).execute(null);

		assertFalse(releaseExists(gfRepo, "notThere"));
	}

	@Test
	public void testBranchNameValid() throws Exception {
		assertTrue(isBranchNameValid(MY_RELEASE));
		assertTrue(isBranchNameValid(MY_FEATURE));
		assertFalse(isBranchNameValid("/"));
		assertFalse(isBranchNameValid(""));
	}
}
