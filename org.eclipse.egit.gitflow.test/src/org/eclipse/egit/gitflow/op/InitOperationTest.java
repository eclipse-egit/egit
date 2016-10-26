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
import static org.eclipse.egit.gitflow.GitFlowDefaults.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.RenameBranchOperation;
import org.eclipse.egit.core.test.TestUtils;
import org.eclipse.egit.gitflow.GitFlowRepository;

import static org.eclipse.egit.gitflow.GitFlowConfig.*;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.Test;

public class InitOperationTest extends AbstractGitFlowOperationTest {

	@Test
	public void testInit() throws Exception {
		testRepository
				.createInitialCommit("testInitOperation\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		InitOperation initOperation = new InitOperation(repository);
		initOperation.execute(null);

		TestUtils.waitForJobs(500, 30000, null);

		GitFlowRepository gfRepo = new GitFlowRepository(repository);
		assertEquals(gfRepo.getConfig().getDevelopFull(), repository.getFullBranch());

		assertPrefixEquals(FEATURE_PREFIX, FEATURE_KEY, repository);
		assertPrefixEquals(RELEASE_PREFIX, RELEASE_KEY, repository);
		assertPrefixEquals(HOTFIX_PREFIX, HOTFIX_KEY, repository);
		assertPrefixEquals(VERSION_TAG, VERSION_TAG_KEY, repository);

		assertBranchEquals(DEVELOP, DEVELOP_KEY, repository);
		assertBranchEquals(MASTER, MASTER_KEY, repository);
	}

	private void assertPrefixEquals(String expected, String key,
			Repository repo) {
		assertEquals("Unexpected prefix in: " + repo.getConfig().toText(),
				expected, getPrefix(repo, key));
	}

	private void assertBranchEquals(String expected, String key,
			Repository repo) {
		assertEquals("Unexpected branch in: " + repo.getConfig().toText(),
				expected, getBranch(repo, key));
	}

	private String getPrefix(Repository repository, String prefixName) {
		StoredConfig config = repository.getConfig();
		return config.getString(GITFLOW_SECTION, PREFIX_SECTION, prefixName);
	}

	private String getBranch(Repository repository, String branch) {
		StoredConfig config = repository.getConfig();
		return config.getString(GITFLOW_SECTION, BRANCH_SECTION, branch);
	}

	@Test
	public void testInitEmptyRepository() throws Exception {
		Repository repository = testRepository.getRepository();
		InitOperation initOperation = new InitOperation(repository);
		initOperation.execute(null);

		TestUtils.waitForJobs(500, 30000, null);

		GitFlowRepository gfRepo = new GitFlowRepository(repository);
		assertEquals(gfRepo.getConfig().getDevelopFull(), repository.getFullBranch());
	}

	@Test(expected = CoreException.class)
	public void testInitLocalMasterMissing() throws Exception {
		testRepository
				.createInitialCommit("testInitLocalMasterMissing\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new RenameBranchOperation(repository, repository.findRef(repository
				.getBranch()), "foobar").execute(null);

		new InitOperation(repository).execute(null);
	}
}
