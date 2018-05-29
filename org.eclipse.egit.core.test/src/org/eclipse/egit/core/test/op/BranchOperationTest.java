/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BranchOperationTest extends GitTestCase{

	private static final String TEST = Constants.R_HEADS + "test";
	private static final String MASTER = Constants.R_HEADS + "master";
	TestRepository testRepository;
	Repository repository;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		testRepository = new TestRepository(gitDir);
		repository = testRepository.getRepository();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		repository = null;
		super.tearDown();
	}

	@Test
	public void testBranchOperation() throws Exception {
		// create first commit containing a dummy file
		testRepository.createInitialCommit("testBranchOperation\n\nfirst commit\n");
		// create branch test and switch to branch test
		testRepository.createBranch(MASTER, TEST);
		new BranchOperation(repository, TEST).execute(null);
		assertTrue(repository.getFullBranch().equals(TEST));
		// add .project to version control and commit
		String path = project.getProject().getLocation().append(".project").toOSString();
		File file = new File(path);
		testRepository.track(file);
		testRepository.commit("Add .project file");
		// switch back to master branch
		// .project must disappear, related Eclipse project must be deleted
		new BranchOperation(repository, MASTER).execute(null);
		assertFalse(file.exists());
		assertFalse(project.getProject().exists());
		// switch back to master test
		// .project must reappear
		new BranchOperation(repository, TEST).execute(null);
		assertTrue(file.exists());
	}
}
