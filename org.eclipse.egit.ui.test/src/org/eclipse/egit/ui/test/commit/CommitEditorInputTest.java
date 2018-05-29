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

import java.io.File;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.commit.CommitEditorInput;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link CommitEditorInput}
 */
public class CommitEditorInputTest extends LocalRepositoryTestCase {

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
