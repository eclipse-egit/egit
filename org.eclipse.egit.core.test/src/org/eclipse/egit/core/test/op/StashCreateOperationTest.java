/*******************************************************************************
 * Copyright (C) 2012, 2013 Maik Schreiber <blizzy@blizzy.de> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.op.StashCreateOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StashCreateOperationTest extends GitTestCase {

	TestRepository testRepository;

	Repository repository;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		gitDir = new File(project.getProject()
				.getLocationURI().getPath(), Constants.DOT_GIT);
		testRepository = new TestRepository(gitDir);
		repository = testRepository.getRepository();
		testRepository.connect(project.getProject());
		testRepository.commit("initial commit");
	}

	@Override
	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		repository = null;
		super.tearDown();
	}

	@Test
	public void testDefaultMessage() throws Exception {
		IFile file = testUtils.addFileToProject(project.getProject(),
				"foo/a.txt", "some text");
		new AddToIndexOperation(Arrays.asList(file)).execute(null);
		StashCreateOperation stashCreateOperation = new StashCreateOperation(repository);
		stashCreateOperation.execute(null);

		try (RevWalk revWalk = new RevWalk(repository)) {
			RevCommit commit = revWalk
					.parseCommit(repository.resolve("stash@{0}"));
			assertTrue(commit.getFullMessage().length() > 0);
		}
	}

	@Test
	public void testCustomMessage() throws Exception {
		IFile file = testUtils.addFileToProject(project.getProject(),
				"foo/a.txt", "some text");
		new AddToIndexOperation(Arrays.asList(file)).execute(null);
		String message = "stash message";
		StashCreateOperation stashCreateOperation = new StashCreateOperation(repository, message);
		stashCreateOperation.execute(null);

		try (RevWalk revWalk = new RevWalk(repository)) {
			RevCommit commit = revWalk
					.parseCommit(repository.resolve("stash@{0}"));
			assertEquals(message, commit.getFullMessage());
		}
	}

	@Test
	public void testUntrackedFlag() throws Exception {
		testUtils.addFileToProject(project.getProject(), "foo/untracked.txt",
				"some text");
		String message = "stash with untracked files";
		StashCreateOperation stashCreateOperation = new StashCreateOperation(
				repository, message, true);
		stashCreateOperation.execute(null);

		try (RevWalk revWalk = new RevWalk(repository)) {
			RevCommit commit = revWalk
					.parseCommit(repository.resolve("stash@{0}"));
			// untracked commit is the third parent
			assertEquals(commit.getParentCount(), 3);
		}
	}

}
