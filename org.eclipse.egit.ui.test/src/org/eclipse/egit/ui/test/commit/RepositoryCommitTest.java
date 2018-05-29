/*******************************************************************************
 *  Copyright (c) 2011, 2013 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests of {@link RepositoryCommit}
 */
public class RepositoryCommitTest extends LocalRepositoryTestCase {

	private Repository repository;

	private RevCommit commit;

	@Before
	public void setup() throws Exception {
		File repoFile = createProjectAndCommitToRepository();
		assertNotNull(repoFile);
		repository = Activator.getDefault().getRepositoryCache()
				.lookupRepository(repoFile);
		assertNotNull(repository);

		try (RevWalk walk = new RevWalk(repository)) {
			commit = walk.parseCommit(repository.resolve(Constants.HEAD));
			assertNotNull(commit);
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

	@Test
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
