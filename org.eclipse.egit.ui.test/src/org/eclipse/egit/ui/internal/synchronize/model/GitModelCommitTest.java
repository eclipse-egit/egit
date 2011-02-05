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

import java.io.File;

import org.eclipse.egit.ui.Activator;
import org.junit.BeforeClass;
import org.junit.Test;

public class GitModelCommitTest extends GitModelTestCase {

	@Test public void shouldReturnEqualForSameInstance() throws Exception {
		// given
		GitModelCommit left = new GitModelCommit(createModelRepository(),
				getCommit(leftRepoFile, HEAD), LEFT);

		// when
		boolean actual = left.equals(left);

		// then
		assertTrue(actual);
	}

	@Test public void shouldReturnNotEqualForDifferentCommits()
			throws Exception {
		// given
		GitModelCommit left = new GitModelCommit(createModelRepository(),
				getCommit(leftRepoFile, HEAD), LEFT);
		GitModelCommit right = new GitModelCommit(createModelRepository(),
				getCommit(leftRepoFile, HEAD + "~1"), LEFT);

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForDifferentParents()
			throws Exception {
		// given
		File localRightRepoFile = createProjectAndCommitToRepository(REPO2);

		GitModelRepository rightGsd = new GitModelRepository(
				getGSD(lookupRepository(localRightRepoFile)));
		GitModelCommit left = new GitModelCommit(createModelRepository(),
				getCommit(leftRepoFile, HEAD), LEFT);
		GitModelCommit right = new GitModelCommit(rightGsd,
				getCommit(localRightRepoFile, HEAD), LEFT);

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnEqualForSameCommits()
			throws Exception {
		// given
		GitModelCommit left = new GitModelCommit(createModelRepository(),
				getCommit(leftRepoFile, HEAD), LEFT);
		GitModelCommit right = new GitModelCommit(createModelRepository(),
				getCommit(leftRepoFile, HEAD), LEFT);

		// when
		boolean actual = left.equals(right);

		// then
		assertTrue(actual);
	}

	@Test public void shouldReturnNotEqualWhenCommitTreeAndCache()
			throws Exception {
		// given
		GitModelCommit left = new GitModelCommit(createModelRepository(),
				getCommit(leftRepoFile, HEAD), LEFT);
		GitModelCache right = new GitModelCache(createModelRepository(),
				getCommit(leftRepoFile, HEAD));

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualWhenCommitTreeAndTree()
			throws Exception {
		// given
		GitModelCommit left = new GitModelCommit(createModelRepository(),
				getCommit(leftRepoFile, HEAD), LEFT);
		GitModelTree right = new GitModelTree(left,
				getCommit(leftRepoFile, HEAD), null, null, null, null, null);

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualWhenCommitTreeAndBlob()
			throws Exception {
		// given
		GitModelCommit left = new GitModelCommit(createModelRepository(),
				getCommit(leftRepoFile, HEAD), LEFT);
		GitModelBlob right = new GitModelBlob(left,
				getCommit(leftRepoFile, HEAD), null, null, null, null,
				getFile1Location());

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
