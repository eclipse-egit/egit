/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.test.commit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.egit.core.internal.Activator;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests of {@link RepositoryCommit}
 */
public class RepositoryCommitTest extends LocalRepositoryTestCase {

	private static Repository repository;

	private static RevCommit commit;

	@BeforeClass
	public static void setup() throws Exception {
		File repoFile = createProjectAndCommitToRepository();
		assertNotNull(repoFile);
		repository = Activator.getDefault().getRepositoryCache()
				.lookupRepository(repoFile);
		assertNotNull(repository);

		RevWalk walk = new RevWalk(repository);
		try {
			commit = walk.parseCommit(repository.resolve(Constants.HEAD));
			assertNotNull(commit);
		} finally {
			walk.release();
		}
	}

	@Test
	public void testConstructorAsserts() throws Exception {
		try {
			assertNull(new RepositoryCommit(null, null));
		} catch (AssertionFailedException afe) {
			assertNotNull(afe);
		}
		try {
			assertNull(new RepositoryCommit(repository, null));
		} catch (AssertionFailedException afe) {
			assertNotNull(afe);
		}
		try {
			assertNull(new RepositoryCommit(null, commit));
		} catch (AssertionFailedException afe) {
			assertNotNull(afe);
		}
	}

	@Test
	public void testAdapters() throws Exception {
		RepositoryCommit repoCommit = new RepositoryCommit(repository, commit);
		assertEquals(repository, repoCommit.getAdapter(Repository.class));
		assertEquals(commit, repoCommit.getAdapter(RevCommit.class));
	}

	public void testGetters() {
		RepositoryCommit repoCommit = new RepositoryCommit(repository, commit);
		assertEquals(repository, repoCommit.getRepository());
		assertEquals(commit, repoCommit.getRevCommit());
		assertNotNull(repoCommit.getRepositoryName());
		assertNotNull(repoCommit.abbreviate());
	}

	@Test
	public void testDiffs() throws Exception {
		RepositoryCommit repoCommit = new RepositoryCommit(repository, commit);
		FileDiff[] diffs = repoCommit.getDiffs();
		assertNotNull(diffs);
		assertTrue(diffs.length > 0);
		for (FileDiff diff : diffs)
			assertNotNull(diff);
	}

}
