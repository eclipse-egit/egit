/*******************************************************************************
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.egit.core.test.TestUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CommitOperationTest extends GitTestCase {

	private List<IResource> resources = new ArrayList<IResource>();

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
	}

	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		repository = null;
		super.tearDown();
	}

	@Test
	public void testCommitAll() throws Exception {
		IFile file1 = testUtils.addFileToProject(project.getProject(),
				"sub/a.txt", "some text");
		testUtils.addFileToProject(project.getProject(),
				"sub/b.txt", "some text");

		resources.add(project.getProject().getFolder("sub"));
		new AddToIndexOperation(resources).execute(null);
		CommitOperation commitOperation = new CommitOperation(null, null, null,
				TestUtils.AUTHOR, TestUtils.COMMITTER,
				"first commit");
		commitOperation.setCommitAll(true);
		commitOperation.setRepos(new Repository[]{repository});
		commitOperation.execute(null);

		Git git = new Git(repository);
		Iterator<RevCommit> commits = git.log().call().iterator();
		RevCommit firstCommit = commits.next();
		assertTrue(firstCommit.getCommitTime() > 0);

		assertEquals("first commit", firstCommit.getFullMessage());

		testUtils.changeContentOfFile(project.getProject(), file1, "changed text");

		commitOperation = new CommitOperation(null, null, null,
				TestUtils.AUTHOR, TestUtils.COMMITTER,
				"second commit");
		commitOperation.setCommitAll(true);
		commitOperation.setRepos(new Repository[]{repository});
		commitOperation.execute(null);

		git = new Git(repository);
		commits = git.log().call().iterator();
		RevCommit secondCommit = commits.next();
		assertTrue(secondCommit.getCommitTime() > 0);

		assertEquals("second commit", secondCommit.getFullMessage());
		secondCommit.getParent(0).equals(firstCommit);
		assertEquals("The Author", secondCommit.getAuthorIdent().getName());
		assertEquals("The.author@some.com", secondCommit.getAuthorIdent().getEmailAddress());
		assertEquals("The Commiter", secondCommit.getCommitterIdent().getName());
		assertEquals("The.committer@some.com", secondCommit.getCommitterIdent().getEmailAddress());
	}

}
