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

import java.io.File;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.egit.core.internal.Activator;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.commit.CommitEditorInput;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for {@link CommitEditorInput}
 */
public class CommitEditorInputTest extends LocalRepositoryTestCase {

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
	public void testConstructorAsserts() {
		try {
			assertNull(new CommitEditorInput(null));
		} catch (AssertionFailedException afe) {
			assertNotNull(afe);
		}
	}

	@Test
	public void testAdapters() {
		RepositoryCommit repoCommit = new RepositoryCommit(repository, commit);
		CommitEditorInput input = new CommitEditorInput(repoCommit);
		assertEquals(repoCommit, input.getAdapter(RepositoryCommit.class));
		assertEquals(repository, input.getAdapter(Repository.class));
		assertEquals(commit, input.getAdapter(RevCommit.class));
	}

	@Test
	public void testInput() {
		RepositoryCommit repoCommit = new RepositoryCommit(repository, commit);
		CommitEditorInput input = new CommitEditorInput(repoCommit);
		assertNotNull(input.getImageDescriptor());
		assertNotNull(input.getToolTipText());
		assertNotNull(input.getName());
		assertEquals(repoCommit, input.getCommit());
		assertNotNull(input.getPersistable());
	}

}
