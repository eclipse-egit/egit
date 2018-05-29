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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

		HotfixStartOperation hotfixStartOperation = new HotfixStartOperation(gfRepo, MY_HOTFIX);
		assertNull(hotfixStartOperation.getSchedulingRule());
		hotfixStartOperation.execute(null);

		assertEquals(gfRepo.getConfig().getFullHotfixBranchName(MY_HOTFIX),
				repository.getFullBranch());
	}
}
