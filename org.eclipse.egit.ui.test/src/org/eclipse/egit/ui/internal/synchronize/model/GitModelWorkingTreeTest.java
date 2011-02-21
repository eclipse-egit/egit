/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.junit.Assert.assertFalse;

import java.io.File;

import org.eclipse.egit.ui.Activator;
import org.junit.BeforeClass;
import org.junit.Test;

public class GitModelWorkingTreeTest extends GitModelTestCase {

	@Test public void shouldReturnEqualsForTheSameInstance() throws Exception {
		// given
		GitModelWorkingTree left = new GitModelWorkingTree(createModelCommit());

		// when
		boolean actual = left.equals(left);

		// then
		assertFalse(!actual);
	}

	@Test public void shouldReturnNotEqualsForTheDifferentParents()
			throws Exception {
		// given
		File localRightRepoFile = createProjectAndCommitToRepository(REPO2);
		GitModelRepository rightGsd = new GitModelRepository(
				getGSD(lookupRepository(localRightRepoFile)));
		GitModelWorkingTree left = new GitModelWorkingTree(createModelCommit());
		GitModelWorkingTree right = new GitModelWorkingTree(rightGsd);

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnEqualsForTheSameCommits()
			throws Exception {
		// given
		GitModelWorkingTree left = new GitModelWorkingTree(createModelCommit());
		GitModelWorkingTree right = new GitModelWorkingTree(
				createModelCommit());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(!actual);
	}

	@Test public void shouldReturnNotEqualsWhenComparingWorkingTreeAndCache()
			throws Exception {
		// given
		GitModelWorkingTree left = new GitModelWorkingTree(createModelCommit());
		GitModelCache right = new GitModelCache(createModelCommit(),
				getCommit(leftRepoFile, HEAD));

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
