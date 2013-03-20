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
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.internal.Activator;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.commit.CommitEditorInput;
import org.eclipse.egit.ui.internal.commit.CommitEditorInputFactory;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.ui.XMLMemento;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests of {@link CommitEditorInputFactory}
 */
public class CommitEditorInputFactoryTest extends LocalRepositoryTestCase {

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
	public void testPersistable() {
		CommitEditorInput input = new CommitEditorInput(new RepositoryCommit(
				repository, commit));
		XMLMemento memento = XMLMemento.createWriteRoot("test");
		input.getPersistable().saveState(memento);
		CommitEditorInputFactory factory = new CommitEditorInputFactory();
		IAdaptable created = factory.createElement(memento);
		assertNotNull(created);
		assertTrue(created instanceof CommitEditorInput);
		CommitEditorInput createdInput = (CommitEditorInput) created;
		assertEquals(input.getCommit().getRevCommit().name(), createdInput
				.getCommit().getRevCommit().name());
		assertEquals(input.getCommit().getRepository().getDirectory(),
				createdInput.getCommit().getRepository().getDirectory());
	}

}
