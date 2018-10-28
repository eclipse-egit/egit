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

import static org.eclipse.egit.gitflow.GitFlowDefaults.DEVELOP;
import static org.eclipse.egit.gitflow.GitFlowDefaults.FEATURE_PREFIX;
import static org.eclipse.egit.gitflow.GitFlowDefaults.HOTFIX_PREFIX;
import static org.eclipse.egit.gitflow.GitFlowDefaults.RELEASE_PREFIX;
import static org.eclipse.egit.gitflow.GitFlowDefaults.VERSION_TAG;
import static org.eclipse.jgit.lib.Constants.DOT_GIT;
import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.egit.gitflow.op.AbstractDualRepositoryTestCase;
import org.eclipse.egit.gitflow.op.FeatureStartOperation;
import org.eclipse.egit.gitflow.op.HotfixStartOperation;
import org.eclipse.egit.gitflow.op.InitOperation;
import org.eclipse.egit.gitflow.op.ReleaseFinishOperation;
import org.eclipse.egit.gitflow.op.ReleaseStartOperation;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

public class GitFlowRepositoryTest extends AbstractDualRepositoryTestCase {
	@Test(expected = WrongGitFlowStateException.class)
	public void testFindHeadFailOnEmptyRepository() throws Exception {
		File workdir3 = testUtils.createTempDir("Repository3");
		TestRepository repository3 = new TestRepository(new File(workdir3, DOT_GIT));
		GitFlowRepository gfRepo = new GitFlowRepository(repository3.getRepository());

		gfRepo.findHead();
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

		assertEquals(R_HEADS + gfRepo.getConfig().getFeaturePrefix()
				+ MY_FEATURE, gfRepo.getFeatureBranches().get(0).getName());
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

		assertEquals(R_HEADS + gfRepo.getConfig().getReleasePrefix()
				+ MY_RELEASE, gfRepo.getReleaseBranches().get(0).getName());
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

		assertEquals(
				R_HEADS + gfRepo.getConfig().getHotfixPrefix() + MY_HOTFIX,
				gfRepo.getHotfixBranches().get(0).getName());
	}

	@Test
	public void testGetFeatureBranchName() throws Exception {
		repository1
				.createInitialCommit("testGetFeatureBranchName\n\nfirst commit\n");

		Repository repository = repository1.getRepository();
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		InitParameters initParameters = new InitParameters();
		initParameters.setDevelop(DEVELOP);
		initParameters.setMaster(GitFlowDefaults.MASTER);
		initParameters.setFeature(FEATURE_PREFIX);
		initParameters.setRelease(RELEASE_PREFIX);
		initParameters.setHotfix(HOTFIX_PREFIX);
		initParameters.setVersionTag(VERSION_TAG);
		new InitOperation(repository, initParameters).execute(null);

		assertTrue(gfRepo.getFeatureBranches().isEmpty());

		new FeatureStartOperation(gfRepo, MY_FEATURE).execute(null);

		Ref actualFeatureRef = repository.exactRef(R_HEADS
				+ gfRepo.getConfig().getFeaturePrefix() + MY_FEATURE);
		assertEquals(MY_FEATURE, gfRepo.getFeatureBranchName(actualFeatureRef));
	}


	@Test
	public void testIsOnDevelop() throws Exception {
		Repository repository = repository1.getRepository();
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		repository1.checkoutBranch(gfRepo.getConfig().getDevelop());

		RevCommit developBranchCommit = repository1.commit("develop branch commit");
		assertTrue(gfRepo.isOnDevelop(developBranchCommit));

		new FeatureStartOperation(gfRepo, MY_FEATURE).execute(null);
		RevCommit featureBranchCommit = repository1.commit("feature branch commit");
		assertFalse(gfRepo.isOnDevelop(featureBranchCommit));

		// the initial commit was made on master, but is also on develop
		assertTrue(gfRepo.isOnDevelop(initialCommit));
	}
}
