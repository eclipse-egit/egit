/*******************************************************************************
 * Copyright (C) 2012, Tomasz Zarna <tomasz.zarna@tasktop.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.op.MergeOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MergeOperationTest extends GitTestCase {

	private static final String MASTER = Constants.R_HEADS +  Constants.MASTER;
	private static final String SIDE = Constants.R_HEADS + "side";

	private TestRepository testRepository;
	private RevCommit secondCommit;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		gitDir = new File(project.getProject()
				.getLocationURI().getPath(), Constants.DOT_GIT);
		testRepository = new TestRepository(gitDir);
		testRepository.connect(project.getProject());

		File file1 = testRepository.createFile(project.getProject(), "file1-1");
		testRepository.addAndCommit(project.getProject(), file1,
				"master commit 1");
		testRepository.createBranch(MASTER, SIDE);
		testRepository.appendFileContent(file1, "file1-2");
		secondCommit = testRepository.addAndCommit(project.getProject(), file1,
				"master commit 2");
		testRepository.checkoutBranch(SIDE);
	}

	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		super.tearDown();
	}

	@Test
	public void testMergeFF() throws Exception {
		MergeOperation operation = new MergeOperation(
				testRepository.getRepository(), MASTER);
		operation.execute(new NullProgressMonitor());

		assertTrue(testRepository.getRepository().resolve(SIDE).equals(secondCommit));
	}

	@Test
	public void testMergeFFOnly() throws Exception {
		StoredConfig config = testRepository.getRepository().getConfig();
		config.setEnum(ConfigConstants.CONFIG_BRANCH_SECTION, MASTER,
				ConfigConstants.CONFIG_KEY_MERGEOPTIONS,
				FastForwardMode.FF_ONLY);
		config.save();
		File file2 = testRepository.createFile(project.getProject(), "file2");
		testRepository.appendFileContent(file2, "file2-1");
		RevCommit commit = testRepository.addAndCommit(project.getProject(), file2,
				"side commit 1");

		MergeOperation operation = new MergeOperation(
				testRepository.getRepository(), MASTER);
		operation.execute(new NullProgressMonitor());

		assertTrue(testRepository.getRepository().resolve(SIDE).equals(commit));
	}
}
