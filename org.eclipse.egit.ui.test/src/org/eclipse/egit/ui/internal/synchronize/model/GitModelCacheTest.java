/*******************************************************************************
 * Copyright (C) 2011, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.jgit.junit.JGitTestUtil.writeTrashFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.Map;

import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change;
import org.eclipse.egit.core.synchronize.StagedChangeCache;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
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
		GitModelObject left = new GitModelCache(createModelRepository(),
				lookupRepository(leftRepoFile), null);
		GitModelObject right = mock(GitModelCommit.class);

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test
	public void shouldReturnChildren() throws Exception {
		Repository repo = lookupRepository(leftRepoFile);
		writeTrashFile(repo, "dir/a.txt", "trash");
		writeTrashFile(repo, "dir/b.txt", "trash");
		writeTrashFile(repo, "dir/c.txt", "trash");
		writeTrashFile(repo, "dir/d.txt", "trash");
		try (Git git = new Git(repo)) {
			git.add().addFilepattern("dir").call();
		}

		Map<String, Change> changes = StagedChangeCache.build(repo);
		assertEquals(4, changes.size());

		GitModelCache cache = new GitModelCache(createModelRepository(), repo,
				changes);

		GitModelObject[] cacheChildren = cache.getChildren();
		assertEquals(1, cacheChildren.length);
		GitModelObject dir = cacheChildren[0];
		assertEquals("dir", dir.getName());

		GitModelObject[] dirChildren = dir.getChildren();
		assertEquals(4, dirChildren.length);
	}

	@Before
	public void setupEnvironment() throws Exception {
		leftRepoFile = createProjectAndCommitToRepository();

		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(leftRepoFile);
	}

}
