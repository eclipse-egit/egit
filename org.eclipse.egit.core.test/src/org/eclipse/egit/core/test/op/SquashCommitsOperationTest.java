/*******************************************************************************
 * Copyright (c) 2014 Maik Schreiber
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Maik Schreiber - initial implementation
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.op.SquashCommitsOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SquashCommitsOperationTest extends GitTestCase {
	private TestRepository testRepository;
	private RevCommit commit1;
	private RevCommit commit2;
	private RevCommit commit3;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		gitDir = new File(project.getProject().getLocationURI().getPath(),
				Constants.DOT_GIT);
		testRepository = new TestRepository(gitDir);
		testRepository.connect(project.getProject());
		testRepository.createInitialCommit("initial");

		File file = testRepository.createFile(project.getProject(), "file-1");
		commit1 = testRepository.addAndCommit(project.getProject(), file,
				"commit 1");
		testRepository.appendFileContent(file, "file-2");
		commit2 = testRepository.addAndCommit(project.getProject(), file,
				"commit 2");
		testRepository.appendFileContent(file, "file-3");
		commit3 = testRepository.addAndCommit(project.getProject(), file,
				"commit 3");
	}

	@Override
	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		super.tearDown();
	}

	@Test
	public void squash() throws Exception {
		InteractiveHandler messageHandler = new InteractiveHandler() {
			@Override
			public void prepareSteps(List<RebaseTodoLine> steps) {
				// not used
			}

			@Override
			public String modifyCommitMessage(String commit) {
				return "squashed";
			}
		};

		List<RevCommit> commits = Arrays.asList(commit1, commit2, commit3);
		SquashCommitsOperation op = new SquashCommitsOperation(
				testRepository.getRepository(), commits, messageHandler);
		op.execute(new NullProgressMonitor());

		assertEquals(2, countCommitsInHead());

		LogCommand log;
		try (Git git = new Git(testRepository.getRepository())) {
			log = git.log();
		}
		Iterable<RevCommit> logCommits = log.call();
		RevCommit latestCommit = logCommits.iterator().next();
		assertEquals("squashed", latestCommit.getFullMessage());
	}

	private int countCommitsInHead() throws GitAPIException {
		LogCommand log;
		try (Git git = new Git(testRepository.getRepository())) {
			log = git.log();
		}
		Iterable<RevCommit> commits = log.call();
		int result = 0;
		for (Iterator i = commits.iterator(); i.hasNext();) {
			i.next();
			result++;
		}
		return result;
	}
}
