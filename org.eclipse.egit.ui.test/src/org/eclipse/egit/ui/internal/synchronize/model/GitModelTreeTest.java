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

import static org.eclipse.compare.structuremergeviewer.Differencer.CHANGE;
import static org.eclipse.compare.structuremergeviewer.Differencer.RIGHT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.ui.Activator;
import org.junit.Before;
import org.junit.Test;

public class GitModelTreeTest extends GitModelTestCase {

	@Test public void shouldReturnEqualForSameInstance() throws Exception {
		// given
		GitModelTree left = createModelTree();

		// when
		boolean actual = left.equals(left);

		// then
		assertTrue(actual);
	}

	@Test public void shouldReturnEqualForSameBaseCommit() throws Exception {
		// given
		GitModelTree left = createModelTree(getTreeLocation());
		GitModelTree right = createModelTree(getTreeLocation());

		// when
		boolean actual = left.equals(right);

		// then
		assertTrue(actual);
	}

	@Test public void shouldReturnNotEqualForDifferentLocation()
			throws Exception {
		// given
		GitModelTree left = createModelTree(getTreeLocation());
		GitModelTree right = createModelTree(getTree1Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForTreeAndCommit()
			throws Exception {
		// given
		GitModelObject left = createModelTree(getTreeLocation());
		GitModelObject right = mock(GitModelCommit.class);

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForTreeAndBlob()
			throws Exception {
		// given
		GitModelObject left = createModelTree(getTreeLocation());
		GitModelObject right = mock(GitModelBlob.class);

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForTreeAndCacheTree()
			throws Exception {
		// given
		GitModelTree left = createModelTree(getTreeLocation());
		GitModelCacheTree right = mock(GitModelCacheTree.class);

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

	private GitModelTree createModelTree() throws Exception {
		return createModelTree(getTreeLocation());
	}

	private GitModelTree createModelTree(IPath location)
			throws Exception {
		return new GitModelTree(createModelCommit(), location, RIGHT | CHANGE);
	}

}
