/*******************************************************************************
 * Copyright (C) 2013, Laurent Goubet <laurent.goubet@obeo.fr> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.junit.JGitTestUtil;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Test;

public class GitSubscriberResourceMappingContextTest extends GitTestCase {

	private static final String MASTER = Constants.R_HEADS + Constants.MASTER;

	private static final String BRANCH = Constants.R_HEADS + "branch";

	private Repository repo;

	private IProject iProject;

	private TestRepository testRepo;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		iProject = project.project;
		testRepo = new TestRepository(gitDir);
		testRepo.connect(iProject);
		repo = RepositoryMapping.getMapping(iProject).getRepository();

		// make initial commit
		try (Git git = new Git(repo)) {
			git.commit().setAuthor("JUnit", "junit@jgit.org")
					.setMessage("Initial commit").call();
		}
	}

	@Test
	public void hasLocalChange() throws Exception {
		File file1 = testRepo.createFile(iProject, "a.txt");
		File file2 = testRepo.createFile(iProject, "b.txt");
		testRepo.appendContentAndCommit(iProject, file1, "content a",
				"commit a");
		testRepo.appendContentAndCommit(iProject, file2, "content b",
				"commit b");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);

		RemoteResourceMappingContext context = prepareContext(MASTER, MASTER);
		assertFalse(context.hasLocalChange(iFile1, new NullProgressMonitor()));
		assertFalse(context.hasLocalChange(iFile2, new NullProgressMonitor()));

		JGitTestUtil.write(file1, "changed content a");
		JGitTestUtil.write(file2, "changed content b");

		refresh(context, iFile1, iFile2);
		assertTrue(context.hasLocalChange(iFile1, new NullProgressMonitor()));
		assertTrue(context.hasLocalChange(iFile2, new NullProgressMonitor()));

		JGitTestUtil.write(file2, "content b");

		refresh(context, iFile2);
		assertTrue(context.hasLocalChange(iFile1, new NullProgressMonitor()));
		assertFalse(context.hasLocalChange(iFile2, new NullProgressMonitor()));
	}

	@Test
	public void hasLocalChangeWithFileRemoval() throws Exception {
		File file1 = testRepo.createFile(iProject, "a.txt");
		File file2 = testRepo.createFile(iProject, "b.txt");
		File file3 = testRepo.createFile(iProject, "c.txt");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		IFile iFile3 = testRepo.getIFile(iProject, file3);

		RemoteResourceMappingContext context = prepareContext(MASTER, MASTER);
		assertTrue(context.hasLocalChange(iFile1, new NullProgressMonitor()));
		assertTrue(context.hasLocalChange(iFile2, new NullProgressMonitor()));
		assertTrue(context.hasLocalChange(iFile3, new NullProgressMonitor()));

		iFile1.delete(false, null);
		refresh(context, iFile1, iFile2, iFile3);
		assertTrue(context.hasLocalChange(iFile2, new NullProgressMonitor()));
		assertTrue(context.hasLocalChange(iFile3, new NullProgressMonitor()));
	}

	@Test
	public void hasLocalChangeInNewFolder() throws Exception {
		iProject.getFolder("folder").create(false, true, null);
		RemoteResourceMappingContext context = prepareContext(MASTER, MASTER);
		// Folder is now known, but not yet file in it

		File file = testRepo.createFile(iProject, "folder/b.txt");
		IFile iFile = testRepo.getIFile(iProject, file);
		refresh(context, iFile);
		assertTrue(context.hasLocalChange(iFile, new NullProgressMonitor()));

		testRepo.addToIndex(iProject, file);
		refresh(context, iFile);
		assertTrue(context.hasLocalChange(iFile, new NullProgressMonitor()));

		JGitTestUtil.write(file, "changed content b");
		refresh(context, iFile);
		assertTrue(context.hasLocalChange(iFile, new NullProgressMonitor()));
	}

	@Test
	public void hasRemoteChanges() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1.sample");
		File file2 = testRepo.createFile(iProject, "file2.sample");

		testRepo.appendContentAndCommit(iProject, file1,
				"initial content - file 1",
				"first file - initial commit MASTER");
		testRepo.appendContentAndCommit(iProject, file2,
				"initial content - file 2",
				"second file - initial commit MASTER");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(iFile1, "change in branch - file 1",
				"branch commit - file1");
		setContentsAndCommit(iFile2, "change in branch - file 2",
				"branch commit - file2");

		testRepo.checkoutBranch(MASTER);

		RemoteResourceMappingContext context = prepareContext(MASTER, BRANCH);
		assertFalse(context.hasLocalChange(iFile1, new NullProgressMonitor()));
		assertTrue(context.hasRemoteChange(iFile1, new NullProgressMonitor()));
		assertFalse(context.hasLocalChange(iFile2, new NullProgressMonitor()));
		assertTrue(context.hasRemoteChange(iFile2, new NullProgressMonitor()));

		setContents(iFile1, "change in master - file 1");
		refresh(context, iFile1);
		assertTrue(context.hasLocalChange(iFile1, new NullProgressMonitor()));
		assertTrue(context.hasRemoteChange(iFile1, new NullProgressMonitor()));

		setContents(iFile2, "change in branch - file 2");
		refresh(context, iFile2);
		assertTrue(context.hasLocalChange(iFile2, new NullProgressMonitor()));
		assertTrue(context.hasRemoteChange(iFile2, new NullProgressMonitor()));

		setContentsAndCommit(iFile1, "change in branch - file 1",
				"change in master (same as in branch) - file 2");
		refresh(context, iFile1);
		assertTrue(context.hasLocalChange(iFile1, new NullProgressMonitor()));
		assertTrue(context.hasRemoteChange(iFile1, new NullProgressMonitor()));
	}

	@Test
	public void hasRemoteChangeInNewFile() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1.sample");
		String initialContent1 = "some content for the first file";
		testRepo.appendContentAndCommit(iProject, file1, initialContent1,
				"first file - initial commit");
		IFile iFile1 = testRepo.getIFile(iProject, file1);

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		File file2 = testRepo.createFile(iProject, "file2.sample");
		String initialContent2 = "some content for the second file";
		testRepo.appendContentAndCommit(iProject, file2, initialContent2,
				"second file - initial commit");
		IFile iFile2 = testRepo.getIFile(iProject, file2);

		testRepo.checkoutBranch(MASTER);

		RemoteResourceMappingContext context = prepareContext(MASTER, BRANCH);
		assertFalse(context.hasRemoteChange(iFile1, new NullProgressMonitor()));
		assertTrue(context.hasRemoteChange(iFile2, new NullProgressMonitor()));
	}

	@Test
	public void hasRemoteChangeInNewFolder() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1.sample");
		String initialContent1 = "some content for the first file";
		testRepo.appendContentAndCommit(iProject, file1, initialContent1,
				"first file - initial commit");
		IFile iFile1 = testRepo.getIFile(iProject, file1);

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		iProject.getFolder("folder").create(true, true,
				new NullProgressMonitor());
		File file2 = testRepo.createFile(iProject, "folder/file2.sample");
		String initialContent2 = "some content for the second file";
		testRepo.appendContentAndCommit(iProject, file2, initialContent2,
				"second file - initial commit");
		IFile iFile2 = testRepo.getIFile(iProject, file2);

		testRepo.checkoutBranch(MASTER);

		RemoteResourceMappingContext context = prepareContext(MASTER, BRANCH);
		assertFalse(context.hasRemoteChange(iFile1, new NullProgressMonitor()));
		assertTrue(context.hasRemoteChange(iFile2, new NullProgressMonitor()));
	}

	@Test
	public void hasLocalAndRemoteChange() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1.sample");
		testRepo.appendContentAndCommit(iProject, file1, "initial content",
				"first commit in master");
		IFile iFile1 = testRepo.getIFile(iProject, file1);

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);
		setContentsAndCommit(iFile1, "changed content in branch",
				"first commit in BRANCH");

		testRepo.checkoutBranch(MASTER);
		setContentsAndCommit(iFile1, "changed content in master",
				"second commit in MASTER");

		RemoteResourceMappingContext context = prepareContext(MASTER, BRANCH);
		assertTrue(context.hasRemoteChange(iFile1, new NullProgressMonitor()));
		assertTrue(context.hasLocalChange(iFile1, new NullProgressMonitor()));
	}

	@Test
	public void hasLocalAndRemoteChangeInSubFolder() throws Exception {
		File file1 = testRepo.createFile(iProject, "folder/file1.sample");
		testRepo.appendContentAndCommit(iProject, file1, "initial content",
				"first commit in master");
		IFile iFile1 = testRepo.getIFile(iProject, file1);

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);
		setContentsAndCommit(iFile1, "changed content in branch",
				"first commit in BRANCH");

		testRepo.checkoutBranch(MASTER);
		setContentsAndCommit(iFile1, "changed content in master",
				"second commit in MASTER");

		RemoteResourceMappingContext context = prepareContext(MASTER, BRANCH);
		assertTrue(context.hasRemoteChange(iFile1, new NullProgressMonitor()));
		assertTrue(context.hasLocalChange(iFile1, new NullProgressMonitor()));
	}

	@Test
	public void hasLocalChangeWhenRefreshingParentFolder() throws Exception {
		IFolder folder = iProject.getFolder("newfolder");
		folder.create(false, true, null);

		IFile file = folder.getFile("a.txt");
		file.create(new ByteArrayInputStream("a".getBytes("UTF-8")), false,
				null);

		RemoteResourceMappingContext context = prepareContext(MASTER, MASTER);
		refresh(context, file);

		assertTrue(context.hasLocalChange(file, new NullProgressMonitor()));

		file.delete(false, null);

		// Refresh of folder, not file directly
		refresh(context, folder);

		assertFalse(context.hasLocalChange(file, new NullProgressMonitor()));
	}

	private RevCommit setContentsAndCommit(IFile targetFile,
			String newContents, String commitMessage)
			throws Exception {
		setContents(targetFile, newContents);
		return addAndCommit(targetFile, commitMessage);
	}

	private RevCommit addAndCommit(IFile targetFile, String commitMessage) throws Exception {
		testRepo.addToIndex(targetFile);
		return testRepo.commit(commitMessage);
	}

	private void setContents(IFile targetFile, String newContents)
			throws CoreException, UnsupportedEncodingException {
		targetFile.setContents(
				new ByteArrayInputStream(newContents.getBytes("UTF-8")),
				IResource.FORCE, new NullProgressMonitor());
	}

	private RemoteResourceMappingContext prepareContext(String srcRev,
			String dstRev) throws Exception {
		GitSynchronizeData gsd = new GitSynchronizeData(repo, srcRev, dstRev,
				true);
		GitSynchronizeDataSet gsds = new GitSynchronizeDataSet(gsd);
		GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				gsds);
		subscriber.init(new NullProgressMonitor());

		return new GitSubscriberResourceMappingContext(subscriber, gsds);
	}

	private void refresh(RemoteResourceMappingContext context,
			IResource... resources) throws Exception {
		context.refresh(new ResourceTraversal[] { new ResourceTraversal(
				resources, IResource.DEPTH_INFINITE, 0) }, 0,
				new NullProgressMonitor());
	}
}
