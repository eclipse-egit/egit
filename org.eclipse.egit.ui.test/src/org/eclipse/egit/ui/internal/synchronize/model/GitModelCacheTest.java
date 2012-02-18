/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;

import org.eclipse.egit.ui.Activator;
import org.junit.BeforeClass;
import org.junit.Test;

public class GitModelCacheTest extends GitModelTestCase {

	@Test public void shouldReturnEqualForSameInstance() throws Exception {
		// given
		GitModelCache left = new GitModelCache(createModelRepository(),
				lookupRepository(leftRepoFile), null);

		// when
		boolean actual = left.equals(left);

		// then
		assertTrue(actual);
	}

	@Test public void shouldReturnNotEqualForDifferentRepositories()
			throws Exception {
		// given
		File localRightRepoFile = createProjectAndCommitToRepository(REPO2);
		GitModelRepository rightGsd = new GitModelRepository(
				getGSD(lookupRepository(localRightRepoFile)));
		GitModelCache left = new GitModelCache(createModelRepository(),
				lookupRepository(leftRepoFile), null);
		GitModelCache right = new GitModelCache(rightGsd,
				lookupRepository(leftRepoFile), null);

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnEqualForSameCommits()
			throws Exception {
		// given
		GitModelCache left = new GitModelCache(createModelRepository(),
				lookupRepository(leftRepoFile), null);
		GitModelCache right = new GitModelCache(createModelRepository(),
				lookupRepository(leftRepoFile), null);

		// when
		boolean actual = left.equals(right);

		// then
		assertTrue(actual);
	}

	@Test public void shouldReturnNotEqualWhenComparingCacheAndWorkingTree()
			throws Exception {
		// given
		GitModelCache left = new GitModelCache(createModelRepository(),
				lookupRepository(leftRepoFile), null);
		GitModelCache right = mock(GitModelWorkingTree.class);

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualWhenCacheTreeAndCommit()
			throws Exception {
		// given
		GitModelCache left = new GitModelCache(createModelRepository(),
				lookupRepository(leftRepoFile), null);
		GitModelCommit right = mock(GitModelCommit.class);

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@BeforeClass public static void setupEnvironment() throws Exception {
		leftRepoFile = createProjectAndCommitToRepository();

		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(leftRepoFile);
	}

}
