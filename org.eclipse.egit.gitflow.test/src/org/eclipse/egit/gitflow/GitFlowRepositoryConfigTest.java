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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.egit.gitflow.op.AbstractDualRepositoryTestCase;
import org.eclipse.egit.gitflow.op.InitOperation;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

public class GitFlowRepositoryConfigTest extends AbstractDualRepositoryTestCase {
	@Test
	public void testIsInitialized() throws Exception {
		repository1
				.createInitialCommit("testIsInitialized\n\nfirst commit\n");

		Repository repository = repository2.getRepository();
		GitFlowConfig gitFlowConfig = new GitFlowConfig(repository);

		assertFalse(gitFlowConfig.isInitialized());

		new InitOperation(repository).execute(null);

		assertTrue(gitFlowConfig.isInitialized());
	}

	@Test
	public void testHasDefaultRemote() throws Exception {
		repository1
				.createInitialCommit("testHasDefaultRemote\n\nfirst commit\n");

		Repository repository = repository1.getRepository();
		GitFlowConfig gitFlowConfig = new GitFlowConfig(repository);

		assertFalse(gitFlowConfig.hasDefaultRemote());

		GitFlowConfig gitFlowConfig2 = new GitFlowConfig(repository2.getRepository());
		assertTrue(gitFlowConfig2.hasDefaultRemote());
	}

}
