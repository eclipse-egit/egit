/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.eclipse.egit.ui.Activator;
import org.junit.Before;
import org.junit.Test;

public class GitModelWorkingTreeTest extends GitModelTestCase {

	@Test public void shouldReturnEqualsForTheSameInstance() throws Exception {
		// given
		GitModelWorkingTree left = new GitModelWorkingTree(
				createModelRepository(), lookupRepository(leftRepoFile), null);

		// when
		boolean actual = left.equals(left);

		// then
		assertTrue(actual);
	}

	@Test public void shouldReturnNotEqualsWhenComparingWorkingTreeAndCache()
			throws Exception {
		// given
		GitModelWorkingTree left = new GitModelWorkingTree(
				createModelRepository(), lookupRepository(leftRepoFile), null);
		GitModelCache right = mock(GitModelCache.class);

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Before
	public void setupEnvironment() throws Exception {
		leftRepoFile = createProjectAndCommitToRepository();

		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(leftRepoFile);
	}

}
