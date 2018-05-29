/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
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
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.RebaseResult.Status;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class RebaseOperationTest extends GitTestCase {

	private static final String TOPIC = Constants.R_HEADS + "topic";

	private static final String MASTER = Constants.R_HEADS + "master";

	TestRepository testRepository;

	Repository repository;

	Git git;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		testRepository = new TestRepository(gitDir);
		repository = testRepository.getRepository();
		// create first commit containing a dummy file
		testRepository
				.createInitialCommit("testRebaseOperation\n\nfirst commit\n");
		git = new Git(repository);
	}

	@Test
	@Ignore
	// currently not working as expected; see also TODO in RebaseCommand
	public void testUpToDate() throws Exception {
		IFile file = project.createFile("theFile.txt", "Hello, world"
				.getBytes("UTF-8"));
		// first commit in master: add theFile.txt
		RevCommit first = testRepository.addAndCommit(project.project,
				new File(file.getLocationURI()), "Adding theFile.txt");

		testRepository.createBranch(MASTER, TOPIC);

		// checkout topic
		testRepository.checkoutBranch(TOPIC);

		file = project.createFile("theSecondFile.txt", "Hello, world"
				.getBytes("UTF-8"));
		// topic commit: add second file
		RevCommit topicCommit = testRepository.addAndCommit(project.project,
				new File(file.getLocationURI()), "Adding theSecondFile.txt");

		// parent of topic commit should be first master commit before rebase
		assertEquals(first, topicCommit.getParent(0));

		// rebase topic onto master
		RebaseOperation op = new RebaseOperation(testRepository.getRepository(),
				testRepository.getRepository().exactRef(MASTER));
		op.execute(null);

		RebaseResult res = op.getResult();
		assertEquals(RebaseResult.Status.UP_TO_DATE, res.getStatus());

		try (RevWalk rw = new RevWalk(repository)) {
			RevCommit newTopic = rw.parseCommit(repository.resolve(TOPIC));
			assertEquals(topicCommit, newTopic);
			assertEquals(first, newTopic.getParent(0));
		}
	}

	@Test
	public void testNoConflict() throws Exception {
		IFile file = project.createFile("theFile.txt", "Hello, world"
				.getBytes("UTF-8"));
		// first commit in master: add theFile.txt
		RevCommit first = testRepository.addAndCommit(project.project,
				new File(file.getLocationURI()), "Adding theFile.txt");

		testRepository.createBranch(MASTER, TOPIC);

		file.setContents(new ByteArrayInputStream("master".getBytes("UTF-8")),
				0, null);
		// second commit in master: modify theFile.txt
		RevCommit second = git.commit().setAll(true).setMessage(
				"Modify theFile.txt").call();
		assertEquals(first, second.getParent(0));

		// checkout topic
		testRepository.checkoutBranch(TOPIC);

		file = project.createFile("theSecondFile.txt", "Hello, world"
				.getBytes("UTF-8"));
		// topic commit: add second file
		RevCommit topicCommit = testRepository.addAndCommit(project.project,
				new File(file.getLocationURI()), "Adding theSecondFile.txt");

		// parent of topic commit should be first master commit before rebase
		assertEquals(first, topicCommit.getParent(0));

		// rebase topic onto master
		RebaseOperation op = new RebaseOperation(testRepository.getRepository(),
				testRepository.getRepository().exactRef(MASTER));
		op.execute(null);

		RebaseResult res = op.getResult();
		assertEquals(RebaseResult.Status.OK, res.getStatus());

		try (RevWalk rw = new RevWalk(repository)) {
			RevCommit newTopic = rw.parseCommit(repository.resolve(TOPIC));
			assertEquals(second, newTopic.getParent(0));
		}
	}

	@Test
	public void testStopAndAbortOnConflict() throws Exception {
		IFile file = project.createFile("theFile.txt", "Hello, world"
				.getBytes("UTF-8"));
		// first commit in master: add theFile.txt
		RevCommit first = testRepository.addAndCommit(project.project,
				new File(file.getLocationURI()), "Adding theFile.txt");

		testRepository.createBranch(MASTER, TOPIC);

		file.setContents(new ByteArrayInputStream("master".getBytes("UTF-8")),
				0, null);
		// second commit in master: modify theFile.txt
		RevCommit second = git.commit().setAll(true).setMessage(
				"Modify theFile.txt").call();
		assertEquals(first, second.getParent(0));

		// checkout topic
		testRepository.checkoutBranch(TOPIC);

		// set conflicting content in topic
		file.setContents(new ByteArrayInputStream("topic".getBytes("UTF-8")),
				0, null);
		// topic commit: add second file
		RevCommit topicCommit = testRepository.addAndCommit(project.project,
				new File(file.getLocationURI()), "Changing theFile.txt again");

		// parent of topic commit should be first master commit before rebase
		assertEquals(first, topicCommit.getParent(0));

		// rebase topic onto master
		RebaseOperation op = new RebaseOperation(testRepository.getRepository(),
				testRepository.getRepository().exactRef(MASTER));
		op.execute(null);

		RebaseResult res = op.getResult();
		assertEquals(RebaseResult.Status.STOPPED, res.getStatus());

		// let's try to abort this here
		RebaseOperation abort = new RebaseOperation(repository, Operation.ABORT);
		abort.execute(null);
		RebaseResult abortResult = abort.getResult();
		assertEquals(Status.ABORTED, abortResult.getStatus());

		assertEquals(topicCommit, repository.resolve(Constants.HEAD));
	}

	@Test
	public void testExceptionWhenRestartingStoppedRebase() throws Exception {
		IFile file = project.createFile("theFile.txt", "Hello, world"
				.getBytes("UTF-8"));
		// first commit in master: add theFile.txt
		RevCommit first = testRepository.addAndCommit(project.project,
				new File(file.getLocationURI()), "Adding theFile.txt");

		testRepository.createBranch(MASTER, TOPIC);

		file.setContents(new ByteArrayInputStream("master".getBytes("UTF-8")),
				0, null);
		// second commit in master: modify theFile.txt
		RevCommit second = git.commit().setAll(true).setMessage(
				"Modify theFile.txt").call();
		assertEquals(first, second.getParent(0));

		// checkout topic
		testRepository.checkoutBranch(TOPIC);

		// set conflicting content in topic
		file.setContents(new ByteArrayInputStream("topic".getBytes("UTF-8")),
				0, null);
		// topic commit: add second file
		RevCommit topicCommit = testRepository.addAndCommit(project.project,
				new File(file.getLocationURI()), "Changing theFile.txt again");

		// parent of topic commit should be first master commit before rebase
		assertEquals(first, topicCommit.getParent(0));

		// rebase topic onto master
		RebaseOperation op = new RebaseOperation(repository,
				repository.exactRef(MASTER));
		op.execute(null);

		RebaseResult res = op.getResult();
		assertEquals(RebaseResult.Status.STOPPED, res.getStatus());

		try {
			// let's try to start again, we should get a wrapped
			// WrongRepositoryStateException
			op = new RebaseOperation(repository, repository.exactRef(MASTER));
			op.execute(null);
			fail("Expected Exception not thrown");
		} catch (CoreException e) {
			Throwable cause = e.getCause();
			assertTrue(cause instanceof WrongRepositoryStateException);
		}
	}
}
