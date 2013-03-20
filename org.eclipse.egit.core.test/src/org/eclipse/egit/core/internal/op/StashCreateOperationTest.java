/*******************************************************************************
 * Copyright (C) 2012, Maik Schreiber <blizzy@blizzy.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.core.internal.op;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.egit.core.internal.op.StashCreateOperation;
import org.eclipse.egit.core.internal.test.GitTestCase;
import org.eclipse.egit.core.internal.test.TestRepository;
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

	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		repository = null;
		super.tearDown();
	}

	@Test
	public void testDefaultMessage() throws Exception {
		testUtils.addFileToProject(project.getProject(), "foo/a.txt", "some text");
		StashCreateOperation stashCreateOperation = new StashCreateOperation(repository);
		stashCreateOperation.execute(null);

		RevWalk revWalk = new RevWalk(repository);
		RevCommit commit = revWalk.parseCommit(repository.resolve("stash@{0}"));
		assertTrue(commit.getFullMessage().length() > 0);
	}

	@Test
	public void testCustomMessage() throws Exception {
		testUtils.addFileToProject(project.getProject(), "foo/a.txt", "some text");
		String message = "stash message";
		StashCreateOperation stashCreateOperation = new StashCreateOperation(repository, message);
		stashCreateOperation.execute(null);

		RevWalk revWalk = new RevWalk(repository);
		RevCommit commit = revWalk.parseCommit(repository.resolve("stash@{0}"));
		assertEquals(message, commit.getFullMessage());
	}

}
