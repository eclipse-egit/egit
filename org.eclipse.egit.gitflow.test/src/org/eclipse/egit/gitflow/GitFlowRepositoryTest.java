/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow;

import static org.eclipse.egit.gitflow.GitFlowDefaults.DEVELOP;
import static org.eclipse.egit.gitflow.GitFlowDefaults.FEATURE_PREFIX;
import static org.eclipse.egit.gitflow.GitFlowDefaults.HOTFIX_PREFIX;
import static org.eclipse.egit.gitflow.GitFlowDefaults.RELEASE_PREFIX;
import static org.eclipse.egit.gitflow.GitFlowDefaults.VERSION_TAG;
import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.egit.gitflow.op.AbstractDualRepositoryTestCase;
import org.eclipse.egit.gitflow.op.FeatureStartOperation;
import org.eclipse.egit.gitflow.op.HotfixStartOperation;
import org.eclipse.egit.gitflow.op.InitOperation;
import org.eclipse.egit.gitflow.op.ReleaseFinishOperation;
import org.eclipse.egit.gitflow.op.ReleaseStartOperation;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

public class GitFlowRepositoryTest extends AbstractDualRepositoryTestCase {
	@Test
	public void testIsInitialized() throws Exception {
		repository1
				.createInitialCommit("testIsInitialized\n\nfirst commit\n");

		Repository repository = repository2.getRepository();
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		assertFalse(gfRepo.isInitialized());

		new InitOperation(repository).execute(null);

		assertTrue(gfRepo.isInitialized());
	}

	@Test
	public void testIsMaster() throws Exception {
		repository1.createInitialCommit("testIsMaster\n\nfirst commit\n");

		Repository repository = repository2.getRepository();
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		assertTrue(gfRepo.isMaster());

		new InitOperation(repository).execute(null);

		assertFalse(gfRepo.isMaster());
	}

	@Test
	public void testGetFeatureBranches() throws Exception {
		repository1
				.createInitialCommit("testGetFeatureBranches\n\nfirst commit\n");

		Repository repository = repository1.getRepository();
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		new InitOperation(repository).execute(null);

		assertTrue(gfRepo.getFeatureBranches().isEmpty());

		new FeatureStartOperation(gfRepo, MY_FEATURE).execute(null);

		assertEquals(R_HEADS + gfRepo.getFeaturePrefix() + MY_FEATURE, gfRepo
				.getFeatureBranches().get(0).getName());
	}

	@Test
	public void testGetReleaseBranches() throws Exception {
		repository1
				.createInitialCommit("testGetReleaseBranches\n\nfirst commit\n");

		Repository repository = repository1.getRepository();
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		new InitOperation(repository).execute(null);

		assertTrue(gfRepo.getReleaseBranches().isEmpty());

		new ReleaseStartOperation(gfRepo, MY_RELEASE).execute(null);

		assertEquals(R_HEADS + gfRepo.getReleasePrefix() + MY_RELEASE, gfRepo
				.getReleaseBranches().get(0).getName());
	}

	@Test
	public void testGetHotfixBranches() throws Exception {
		repository1
				.createInitialCommit("testGetHotfixBranches\n\nfirst commit\n");

		Repository repository = repository1.getRepository();
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		new InitOperation(repository).execute(null);

		assertTrue(gfRepo.getHotfixBranches().isEmpty());

		new ReleaseStartOperation(gfRepo, MY_RELEASE).execute(null);
		new ReleaseFinishOperation(gfRepo, MY_RELEASE).execute(null);
		new HotfixStartOperation(gfRepo, MY_HOTFIX).execute(null);

		assertEquals(R_HEADS + gfRepo.getHotfixPrefix() + MY_HOTFIX, gfRepo
				.getHotfixBranches().get(0).getName());
	}

	@Test
	public void testGetFeatureBranchName() throws Exception {
		repository1
				.createInitialCommit("testGetFeatureBranchName\n\nfirst commit\n");

		Repository repository = repository1.getRepository();
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		new InitOperation(repository, DEVELOP, GitFlowDefaults.MASTER,
				FEATURE_PREFIX, RELEASE_PREFIX, HOTFIX_PREFIX, VERSION_TAG)
				.execute(null);

		assertTrue(gfRepo.getFeatureBranches().isEmpty());

		new FeatureStartOperation(gfRepo, MY_FEATURE).execute(null);

		Ref actualFeatureRef = repository.getRef(R_HEADS
				+ gfRepo.getFeaturePrefix() + MY_FEATURE);
		assertEquals(MY_FEATURE, gfRepo.getFeatureBranchName(actualFeatureRef));
	}

	@Test
	public void testHasDefaultRemote() throws Exception {
		repository1
				.createInitialCommit("testHasDefaultRemote\n\nfirst commit\n");

		Repository repository = repository1.getRepository();
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		assertFalse(gfRepo.hasDefaultRemote());

		GitFlowRepository gfRepo2 = new GitFlowRepository(repository2.getRepository());
		assertTrue(gfRepo2.hasDefaultRemote());
	}

}
