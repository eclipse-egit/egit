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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private static final Pattern CHANGE_ID = Pattern
			.compile("\nChange-Id: I[0-9a-fA-F]{40}\n");

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
		super.tearDown();
	}

	@Test
	public void reword() throws Exception {
		RewordCommitOperation op = new RewordCommitOperation(
				testRepository.getRepository(), commit, "new message", false);
		op.execute(new NullProgressMonitor());

		LogCommand log;
		try (Git git = new Git(testRepository.getRepository())) {
			log = git.log();
		}
		RevCommit newCommit = log.call().iterator().next();
		assertEquals("new message", newCommit.getFullMessage());
	}

	@Test
	public void rewordWithChangeId() throws Exception {
		RewordCommitOperation op = new RewordCommitOperation(
				testRepository.getRepository(), commit,
				"new message\n\nChange-Id: I0000000000000000000000000000000000000000\n",
				true);
		op.execute(new NullProgressMonitor());

		LogCommand log;
		try (Git git = new Git(testRepository.getRepository())) {
			log = git.log();
		}
		RevCommit newCommit = log.call().iterator().next();
		String newMessage = newCommit.getFullMessage();

		assertTrue(newMessage.startsWith("new message\n\n"));
		checkChangeId(newMessage);
	}

	@Test
	public void rewordWithNewChangeId() throws Exception {
		RewordCommitOperation op = new RewordCommitOperation(
				testRepository.getRepository(), commit, "new message\n", true);
		op.execute(new NullProgressMonitor());

		LogCommand log;
		try (Git git = new Git(testRepository.getRepository())) {
			log = git.log();
		}
		RevCommit newCommit = log.call().iterator().next();
		String newMessage = newCommit.getFullMessage();

		assertTrue(newMessage.startsWith("new message\n\n"));
		checkChangeId(newMessage);
	}

	@Test
	public void rewordWithNewChangeIdNoNewline() throws Exception {
		RewordCommitOperation op = new RewordCommitOperation(
				testRepository.getRepository(), commit, "new message", true);
		op.execute(new NullProgressMonitor());

		LogCommand log;
		try (Git git = new Git(testRepository.getRepository())) {
			log = git.log();
		}
		RevCommit newCommit = log.call().iterator().next();
		String newMessage = newCommit.getFullMessage();

		assertTrue(newMessage.startsWith("new message\n\n"));
		checkChangeId(newMessage);
	}

	@Test
	public void rewordWithExistingChangeId() throws Exception {
		RewordCommitOperation op = new RewordCommitOperation(
				testRepository.getRepository(), commit,
				"new message\n\nChange-Id: I1230000000000000000000000000000000000321\n",
				true);
		op.execute(new NullProgressMonitor());

		LogCommand log;
		try (Git git = new Git(testRepository.getRepository())) {
			log = git.log();
		}
		RevCommit newCommit = log.call().iterator().next();
		String newMessage = newCommit.getFullMessage();

		assertEquals(
				"new message\n\nChange-Id: I1230000000000000000000000000000000000321\n",
				newMessage);
	}

	private void checkChangeId(String message) {
		assertFalse(message.contains(
				"\nChange-Id: I0000000000000000000000000000000000000000\n"));
		Matcher m = CHANGE_ID.matcher(message);
		int start = 0;
		boolean found = false;
		while (m.find(start)) {
			if (found) {
				assertEquals("Expected only one Change-Id", message, "");
			}
			found = true;
			start = m.end();
		}
		assertTrue(found);
	}
}
