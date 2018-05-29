/*******************************************************************************
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.egit.core.test.TestUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CommitOperationTest extends GitTestCase {

	private static List<IFile> EMPTY_FILE_LIST = new ArrayList<IFile>();

	private List<IResource> resources = new ArrayList<IResource>();

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
	}

	@Override
	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		repository = null;
		super.tearDown();
	}

	@Test
	public void testCommitAddedToIndexDeletedInWorkspace() throws Exception {
		testUtils.addFileToProject(project.getProject(), "foo/a.txt", "some text");
		resources.add(project.getProject().getFolder("foo"));
		new AddToIndexOperation(resources).execute(null);
		CommitOperation commitOperation = new CommitOperation(null, null, TestUtils.AUTHOR, TestUtils.COMMITTER, "first commit");
		commitOperation.setCommitAll(true);
		commitOperation.setRepository(repository);
		commitOperation.execute(null);

		testUtils.addFileToProject(project.getProject(), "zar/b.txt", "some text");
		resources.add(project.getProject().getFolder("zar"));
		new AddToIndexOperation(resources).execute(null);
		IFile zarFile = project.getProject().getFile("zar/b.txt");
		IPath zarFilePath = zarFile.getLocation();
		// delete file and refresh. Deleting using the resource would trigger
		// GitMoveDeleteHook which removes the file from the index
		assertTrue("could not delete file " + zarFilePath.toOSString(),
				zarFilePath.toFile().delete());
		zarFile.refreshLocal(0, null);

		assertFalse(project.getProject().getFile("zar/b.txt").exists());

		IFile[] filesToCommit = new IFile[] { project.getProject().getFile("zar/b.txt") };
		commitOperation = new CommitOperation(filesToCommit, null, TestUtils.AUTHOR, TestUtils.COMMITTER, "first commit");
		commitOperation.setRepository(repository);
		try {
			commitOperation.execute(null);
			// TODO this is very ugly. CommitCommand should be extended
			// not to throw an JGitInternalException in case of an empty
			// commit
			fail("expected CoreException");
		} catch (CoreException e) {
			assertEquals("No changes", e.getCause().getMessage());
		}

		try (TreeWalk treeWalk = new TreeWalk(repository)) {
			treeWalk.addTree(repository.resolve("HEAD^{tree}"));
			assertTrue(treeWalk.next());
			assertEquals("foo", treeWalk.getPathString());
			treeWalk.enterSubtree();
			assertTrue(treeWalk.next());
			assertEquals("foo/a.txt", treeWalk.getPathString());
			assertFalse(treeWalk.next());
		}
	}

	@Test
	public void testCommitAll() throws Exception {
		IFile file1 = testUtils.addFileToProject(project.getProject(),
				"sub/a.txt", "some text");
		testUtils.addFileToProject(project.getProject(),
				"sub/b.txt", "some text");

		resources.add(project.getProject().getFolder("sub"));
		new AddToIndexOperation(resources).execute(null);
		CommitOperation commitOperation = new CommitOperation(null, null, TestUtils.AUTHOR,
				TestUtils.COMMITTER, "first commit");
		commitOperation.setCommitAll(true);
		commitOperation.setRepository(repository);
		commitOperation.execute(null);

		Iterator<RevCommit> commits;
		try (Git git = new Git(repository)) {
			commits = git.log().call().iterator();
		}
		RevCommit firstCommit = commits.next();
		assertTrue(firstCommit.getCommitTime() > 0);

		assertEquals("first commit", firstCommit.getFullMessage());

		testUtils.changeContentOfFile(project.getProject(), file1, "changed text");

		commitOperation = new CommitOperation(null, null, TestUtils.AUTHOR,
				TestUtils.COMMITTER, "second commit");
		commitOperation.setCommitAll(true);
		commitOperation.setRepository(repository);
		commitOperation.execute(null);

		try (Git git = new Git(repository)) {
			commits = git.log().call().iterator();
		}
		RevCommit secondCommit = commits.next();
		assertTrue(secondCommit.getCommitTime() > 0);

		assertEquals("second commit", secondCommit.getFullMessage());
		secondCommit.getParent(0).equals(firstCommit);
		assertEquals("The Author", secondCommit.getAuthorIdent().getName());
		assertEquals("The.author@some.com", secondCommit.getAuthorIdent().getEmailAddress());
		assertEquals("The Commiter", secondCommit.getCommitterIdent().getName());
		assertEquals("The.committer@some.com", secondCommit.getCommitterIdent().getEmailAddress());
	}

	@Test
	public void testCommitEmptiedTree() throws Exception {
		// Set up a directory structure
		testUtils.addFileToProject(project.getProject(),
				"sub1/a.txt", "some text");
		testUtils.addFileToProject(project.getProject(),
				"sub2/b.txt", "some text");
		resources.add(project.getProject().getFolder("sub1"));
		resources.add(project.getProject().getFolder("sub2"));
		new AddToIndexOperation(resources).execute(null);
		CommitOperation commitOperation = new CommitOperation(null, null, TestUtils.AUTHOR,
				TestUtils.COMMITTER, "first commit");
		commitOperation.setCommitAll(true);
		commitOperation.setRepository(repository);
		commitOperation.execute(null);

		Iterator<RevCommit> commits;
		try (Git git = new Git(repository)) {
			commits = git.log().call().iterator();
		}
		RevCommit secondCommit = commits.next();
		try (TreeWalk treeWalk = new TreeWalk(repository)) {
			treeWalk.addTree(secondCommit.getTree().getId());
			treeWalk.setRecursive(true);
			treeWalk.setPostOrderTraversal(true);
			assertTrue(treeWalk.next());
			assertEquals("sub1/a.txt", treeWalk.getPathString());
			assertTrue(treeWalk.next());
			assertEquals("sub1", treeWalk.getPathString());
			assertTrue(treeWalk.next());
			assertEquals("sub2/b.txt", treeWalk.getPathString());
			assertTrue(treeWalk.next());
			assertEquals("sub2", treeWalk.getPathString());
			assertFalse(treeWalk.next());
		}
		project.getProject().getFolder("sub2").delete(IResource.FORCE, null);
		IFile[] filesToCommit = { project.getProject().getFile("sub2/b.txt") };
		ArrayList<IFile> notIndexed = new ArrayList<IFile>();
		notIndexed.add(filesToCommit[0]);
		ArrayList<IFile> notTracked = new ArrayList<IFile>();
		commitOperation = new CommitOperation(filesToCommit, notTracked, TestUtils.AUTHOR, TestUtils.COMMITTER, "second commit");
		commitOperation.setCommitAll(false);
		commitOperation.execute(null);

		try (Git git = new Git(repository)) {
			commits = git.log().call().iterator();
		}
		secondCommit = commits.next();
		try (TreeWalk treeWalk = new TreeWalk(repository)) {
			treeWalk.addTree(secondCommit.getTree().getId());
			treeWalk.setRecursive(true);
			treeWalk.setPostOrderTraversal(true);
			assertTrue(treeWalk.next());
			assertEquals("sub1/a.txt", treeWalk.getPathString());
			assertTrue(treeWalk.next());
			assertEquals("sub1", treeWalk.getPathString());
			assertFalse(treeWalk.next());
		}
	}

	@Test
	public void testCommitUntracked() throws Exception {
		IFile fileA = testUtils.addFileToProject(project.getProject(),
				"foo/a.txt", "some text");
		IFile fileB = testUtils.addFileToProject(project.getProject(),
				"foo/b.txt", "some text");
		testUtils.addFileToProject(project.getProject(), "foo/c.txt",
				"some text");
		IFile[] filesToCommit = { fileA, fileB };
		CommitOperation commitOperation = new CommitOperation(filesToCommit,
				Arrays.asList(filesToCommit), TestUtils.AUTHOR,
				TestUtils.COMMITTER, "first commit");
		commitOperation.execute(null);
		testUtils.assertRepositoryContainsFiles(repository, getRepoRelativePaths(filesToCommit));
	}

	private String[] getRepoRelativePaths(IFile[] files) {
		ArrayList<String> result = new ArrayList<String>();
		for (IFile file:files)
			result.add(file.getProjectRelativePath().toString());
		return result.toArray(new String[result.size()]);
	}

	@Test
	public void testCommitStaged() throws Exception {
		IFile fileA = testUtils.addFileToProject(project.getProject(),
				"foo/a.txt", "some text");
		IFile fileB = testUtils.addFileToProject(project.getProject(),
				"foo/b.txt", "some text");
		IFile[] filesToCommit = { fileA, fileB };
		CommitOperation commitOperation = new CommitOperation(filesToCommit,
				Arrays.asList(filesToCommit), TestUtils.AUTHOR,
				TestUtils.COMMITTER, "first commit");
		commitOperation.execute(null);
		testUtils.changeContentOfFile(project.getProject(), fileA,
				"new content of A");
		testUtils.changeContentOfFile(project.getProject(), fileB,
				"new content of B");
		resources.add(fileA);
		resources.add(fileB);
		new AddToIndexOperation(resources).execute(null);
		commitOperation = new CommitOperation(filesToCommit, EMPTY_FILE_LIST,
				TestUtils.AUTHOR, TestUtils.COMMITTER, "second commit");
		commitOperation.execute(null);

		testUtils.assertRepositoryContainsFilesWithContent(repository,
				"foo/a.txt", "new content of A", "foo/b.txt",
				"new content of B");
	}

	@Test
	public void testCommitIndexSubset() throws Exception {
		IFile fileA = testUtils.addFileToProject(project.getProject(),
				"foo/a.txt", "some text");
		IFile fileB = testUtils.addFileToProject(project.getProject(),
				"foo/b.txt", "some text");
		IFile[] filesToCommit = { fileA, fileB };
		CommitOperation commitOperation = new CommitOperation(filesToCommit,
				Arrays.asList(filesToCommit), TestUtils.AUTHOR,
				TestUtils.COMMITTER, "first commit");
		commitOperation.execute(null);
		testUtils.changeContentOfFile(project.getProject(), fileA,
				"new content of A");
		testUtils.changeContentOfFile(project.getProject(), fileB,
				"new content of B");
		resources.add(fileA);
		resources.add(fileB);
		new AddToIndexOperation(resources).execute(null);
		IFile[] filesToCommit2 = { fileA };
		commitOperation = new CommitOperation(filesToCommit2, EMPTY_FILE_LIST,
				TestUtils.AUTHOR, TestUtils.COMMITTER, "second commit");
		commitOperation.execute(null);

		testUtils.assertRepositoryContainsFilesWithContent(repository,
				"foo/a.txt", "new content of A", "foo/b.txt", "some text");
	}

	@Test
	public void testCommitWithStaging() throws Exception {
		IFile fileA = testUtils.addFileToProject(project.getProject(),
				"foo/a.txt", "some text");
		IFile fileB = testUtils.addFileToProject(project.getProject(),
				"foo/b.txt", "some text");
		IFile[] filesToCommit = { fileA, fileB };
		CommitOperation commitOperation = new CommitOperation(filesToCommit,
				Arrays.asList(filesToCommit), TestUtils.AUTHOR,
				TestUtils.COMMITTER, "first commit");
		commitOperation.execute(null);

		testUtils.changeContentOfFile(project.getProject(), fileA,
				"new content of A");
		testUtils.changeContentOfFile(project.getProject(), fileB,
				"new content of B");
		resources.add(fileA);
		resources.add(fileB);
		commitOperation = new CommitOperation(filesToCommit,
				EMPTY_FILE_LIST, TestUtils.AUTHOR,
				TestUtils.COMMITTER, "first commit");
		commitOperation.execute(null);

		testUtils.assertRepositoryContainsFilesWithContent(repository,
				"foo/a.txt", "new content of A", "foo/b.txt",
				"new content of B");
	}

}
