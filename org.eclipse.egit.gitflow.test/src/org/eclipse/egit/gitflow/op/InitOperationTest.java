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
		GitFlowRepository gfRepo = new GitFlowRepository(repository);
		assertEquals(gfRepo.getConfig().getDevelopFull(), repository.getFullBranch());

		assertEquals(FEATURE_PREFIX, getPrefix(repository, FEATURE_KEY));
		assertEquals(RELEASE_PREFIX, getPrefix(repository, RELEASE_KEY));
		assertEquals(HOTFIX_PREFIX, getPrefix(repository, HOTFIX_KEY));
		assertEquals(VERSION_TAG, getPrefix(repository, VERSION_TAG_KEY));
		assertEquals(DEVELOP, getBranch(repository, DEVELOP_KEY));
		assertEquals(MASTER, getBranch(repository, MASTER_KEY));
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
