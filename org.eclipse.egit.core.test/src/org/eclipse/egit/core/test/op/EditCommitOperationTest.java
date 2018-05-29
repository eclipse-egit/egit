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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.op.EditCommitOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EditCommitOperationTest extends GitTestCase {
	private static final String GIT_REBASE_TODO = RebaseCommand.REBASE_MERGE
			+ "/git-rebase-todo";

	private TestRepository testRepository;

	private RevCommit firstCommit;

	private RevCommit secondCommit;

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
		firstCommit = testRepository.addAndCommit(project.getProject(), file,
				"a commit");

		testRepository.appendFileContent(file, "some text");
		secondCommit = testRepository
				.addAndCommit(project.getProject(), file, "second commit");
	}

	@Override
	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		super.tearDown();
	}

	@Test
	public void edit() throws Exception {
		EditCommitOperation op = new EditCommitOperation(
				testRepository.getRepository(), firstCommit);
		op.execute(new NullProgressMonitor());

		assertEquals(RepositoryState.REBASING_INTERACTIVE, testRepository
				.getRepository().getRepositoryState());

		List<RebaseTodoLine> todos = testRepository.getRepository()
				.readRebaseTodo(GIT_REBASE_TODO, false);
		assertEquals(1, todos.size());
		assertEquals(RebaseTodoLine.Action.PICK, todos.get(0).getAction());
		assertTrue(secondCommit.getId().startsWith(todos.get(0).getCommit()));

		ObjectId headId = testRepository.getRepository()
				.resolve(Constants.HEAD);
		assertEquals(firstCommit.getId(), headId);
	}
}
