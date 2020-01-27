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
 *    Andreas Hermann <a.v.hermann@gmail.com> - Test for stash commit flag
 *******************************************************************************/
package org.eclipse.egit.ui.test.commit;

import static org.eclipse.egit.ui.test.commit.RepositoryCommitMatcher.isSameCommit;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.commit.CommitEditorInput;
import org.eclipse.egit.ui.internal.commit.CommitEditorInputFactory;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.ui.XMLMemento;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests of {@link CommitEditorInputFactory}
 */
public class CommitEditorInputFactoryTest extends LocalRepositoryTestCase {

	private Repository repository;

	private RevCommit commit;

	private XMLMemento memento;

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
		memento = XMLMemento.createWriteRoot("test");
	}

	@Test
	public void shouldPersistAndRestoreCommit() {
		RepositoryCommit repositoryCommit = new RepositoryCommit(
				repository, commit);

		new CommitEditorInput(repositoryCommit).saveState(memento);
		IAdaptable restored = new CommitEditorInputFactory()
				.createElement(memento);

		assertNotNull(restored);
		assertThat(restored, instanceOf(CommitEditorInput.class));
		assertThat(repositoryCommit,
				isSameCommit(((CommitEditorInput) restored).getCommit()));
	}

	@Test
	public void shouldPersistAndRestoreStashCommit() {
		RepositoryCommit stashCommit = new RepositoryCommit(
				repository, commit);
		stashCommit.setStash(true);

		new CommitEditorInput(stashCommit).saveState(memento);
		IAdaptable restored = new CommitEditorInputFactory()
				.createElement(memento);

		assertNotNull(restored);
		assertThat(restored, instanceOf(CommitEditorInput.class));
		assertThat(stashCommit,
				isSameCommit(((CommitEditorInput) restored).getCommit()));
	}
}
