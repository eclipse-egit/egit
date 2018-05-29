/*******************************************************************************
 *  Copyright (c) 2014 Maik Schreiber
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Maik Schreiber - initial implementation
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.op.RewordCommitOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RewordCommitsOperationTest extends GitTestCase {
	private TestRepository testRepository;

	private RevCommit commit;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		gitDir = new File(project.getProject().getLocationURI().getPath(),
				Constants.DOT_GIT);
		testRepository = new TestRepository(gitDir);
		testRepository.connect(project.getProject());
		testRepository.createInitialCommit("initial");

		File file = testRepository.createFile(project.getProject(), "file");
		commit = testRepository.addAndCommit(project.getProject(), file,
				"a commit");
	}

	@Override
	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		super.tearDown();
	}

	@Test
	public void reword() throws Exception {
		RewordCommitOperation op = new RewordCommitOperation(
				testRepository.getRepository(), commit, "new message");
		op.execute(new NullProgressMonitor());

		LogCommand log;
		try (Git git = new Git(testRepository.getRepository())) {
			log = git.log();
		}
		RevCommit newCommit = log.call().iterator().next();
		assertEquals("new message", newCommit.getFullMessage());
	}
}
