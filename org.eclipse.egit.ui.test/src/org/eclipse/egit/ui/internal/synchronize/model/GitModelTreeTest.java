/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.compare.structuremergeviewer.Differencer.LEFT;
import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.ui.Activator;
import org.junit.BeforeClass;
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
		GitModelTree left = createModelTree(HEAD, getTreeLocation());
		GitModelTree right = createModelTree(HEAD, getTreeLocation());
		
		// when
		boolean actual = left.equals(right);
		
		// then
		assertTrue(actual);
	}

	@Test public void shouldReturnNotEqualForDifferentBaseCommit()
			throws Exception {
		// given
		GitModelTree left = createModelTree(HEAD, getTreeLocation());
		GitModelTree right = createModelTree(HEAD + "~1", getTreeLocation());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForDifferentLocation()
			throws Exception {
		// given
		GitModelTree left = createModelTree(HEAD, getTreeLocation());
		GitModelTree right = createModelTree(HEAD, getTree1Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForTreeAndCommit()
			throws Exception {
		// given
		GitModelTree left = createModelTree(HEAD, getTreeLocation());
		GitModelCommit right = new GitModelCommit(createModelRepository(),
				getCommit(leftRepoFile, HEAD), LEFT);

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForTreeAndBlob()
			throws Exception {
		// given
		GitModelTree left = createModelTree(HEAD, getTreeLocation());
		GitModelBlob right = new GitModelBlob(createModelCommit(), getCommit(
				leftRepoFile, HEAD), null, null, null, null, getTreeLocation());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForTreeAndCacheTree()
			throws Exception {
		// given
		GitModelTree left = createModelTree(HEAD, getTreeLocation());
		GitModelCacheTree right = new GitModelCacheTree(createModelCommit(),
				getCommit(leftRepoFile, HEAD), null, null, getTreeLocation(),
				null);

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

	private GitModelTree createModelTree() throws Exception {
		return createModelTree(HEAD, getTreeLocation());
	}

	private GitModelTree createModelTree(String revStr, IPath location)
			throws Exception {
		return new GitModelTree(createModelCommit(), getCommit(
				leftRepoFile, revStr), getCommit(leftRepoFile, revStr), null,
				null, null, location);
	}

}
