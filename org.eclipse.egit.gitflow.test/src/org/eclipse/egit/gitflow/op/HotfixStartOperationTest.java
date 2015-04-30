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

import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

public class HotfixStartOperationTest extends AbstractGitFlowOperationTest {
	@Test
	public void testHotfixStart() throws Exception {
		testRepository.createInitialCommit("testHotfixStart\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		new HotfixStartOperation(gfRepo, MY_HOTFIX).execute(null);

		assertEquals(gfRepo.getFullHotfixBranchName(MY_HOTFIX),
				repository.getFullBranch());

	}
}
