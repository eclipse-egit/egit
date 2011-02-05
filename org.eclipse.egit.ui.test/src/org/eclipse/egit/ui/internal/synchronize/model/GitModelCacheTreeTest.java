/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.ObjectId.fromString;
import static org.eclipse.jgit.lib.ObjectId.zeroId;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.BeforeClass;
import org.junit.Test;

public class GitModelCacheTreeTest extends GitModelTestCase {

	@Test public void shouldReturnEqualForSameInstance() throws Exception {
		// given
		GitModelCacheTree left = crateCacheTree(zeroId(), getTreeLocation());

		// when
		boolean actual = left.equals(left);

		// then
		assertTrue(actual);
	}

	@Test public void shouldReturnEqualForSameData() throws Exception {
		// given
		GitModelCacheTree left = crateCacheTree(zeroId(), getTreeLocation());
		GitModelCacheTree right = crateCacheTree(zeroId(), getTreeLocation());

		// when
		boolean actual = left.equals(right);

		// then
		assertTrue(actual);
	}

	@Test public void shouldReturnNotEqualForDifferetnRepoId() throws Exception {
		// given
		GitModelCacheTree left = crateCacheTree(zeroId(), getTreeLocation());
		GitModelCacheTree right = crateCacheTree(
				fromString("4c879313cd1332e594b1ad20b1485bdff9533034"),
				getTreeLocation());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForDifferetnLocation()
			throws Exception {
		// given
		GitModelCacheTree left = crateCacheTree(zeroId(), getTreeLocation());
		GitModelCacheTree right = crateCacheTree(zeroId(), getTree1Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualWhenComparingCacheTreeAndTree()
			throws Exception {
		// given
		GitModelCacheTree left = crateCacheTree(zeroId(), getTreeLocation());
		GitModelTree right = new GitModelTree(createModelCommit(), getCommit(
				leftRepoFile, HEAD), null, null, null, null, getTreeLocation());

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

	private GitModelCacheTree crateCacheTree(ObjectId repoId, IPath location)
			throws Exception {
		return new GitModelCacheTree(createModelCommit(), getCommit(
				leftRepoFile, HEAD), repoId, null, location, null);
	}
}
