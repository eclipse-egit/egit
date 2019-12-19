/*******************************************************************************
 * Copyright (C) 2011, 2012 Benjamin Muskalla <benjamin.muskalla@tasktop.com>
 * and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.egit.core.test.TestUtils;
import org.eclipse.egit.core.test.models.ModelTestCase;
import org.eclipse.egit.core.test.models.SampleResourceMapping;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.core.mapping.IMergeStatus;
import org.eclipse.team.core.mapping.IResourceMappingMerger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GitSubscriberMergeContextTest extends ModelTestCase {

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

	@After
	public void clearGitResources() throws Exception {
		testRepo.disconnect(iProject);
		testRepo.dispose();
		repo = null;
		super.tearDown();
	}

	@Test
	public void markAsMerged() throws Exception {
		String fileName = "src/Main.java";
		File file = testRepo.createFile(iProject, fileName);
		testRepo.appendContentAndCommit(iProject, file, "class Main {}",
				"some file");
		testRepo.addToIndex(iProject.getFile(".classpath"));
		testRepo.addToIndex(iProject.getFile(".project"));
		testRepo.commit("project files");

		IFile workspaceFile = testRepo.getIFile(iProject, file);

		testRepo.appendFileContent(file, "some changes");
		Status status = status(repo);
		assertEquals(0, status.getAdded().size());
		assertEquals(1, status.getModified().size());
		String repoRelativePath = testRepo.getRepoRelativePath(workspaceFile
				.getLocation().toPortableString());
		assertTrue(status.getModified().contains(repoRelativePath));

		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());

		IMergeContext mergeContext = prepareContext(repo, workspaceFile, HEAD,
				HEAD);
		IDiff node = mergeContext.getDiffTree().getDiff(workspaceFile);
		assertNotNull(node);
		// First of all, "markAsMerged" is not supposed to have any effect on a
		// folder.
		// Second, it should only be used on IDiff obtained from the context,
		// not created for the occasion.
		mergeContext.markAsMerged(node, true, null);

		status = status(repo);
		assertEquals(1, status.getChanged().size());
		assertEquals(0, status.getModified().size());
		assertTrue(status.getChanged().contains(repoRelativePath));
	}

	@Test
	public void mergeNoConflict() throws Exception {
		String fileName = "src/Main.java";
		File file = testRepo.createFile(iProject, fileName);
		final String initialContent = "class Main {}\n";
		testRepo.appendContentAndCommit(iProject, file, initialContent,
				"some file");
		testRepo.addToIndex(iProject.getFile(".classpath"));
		testRepo.addToIndex(iProject.getFile(".project"));
		testRepo.commit("project files");

		IFile workspaceFile = testRepo.getIFile(iProject, file);
		String repoRelativePath = testRepo.getRepoRelativePath(workspaceFile
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		final String branchChanges = "branch changes\n";
		setContentsAndCommit(testRepo, workspaceFile, branchChanges
				+ initialContent, "branch commit");

		testRepo.checkoutBranch(MASTER);

		final String masterChanges = "some changes\n";
		setContentsAndCommit(testRepo, workspaceFile, initialContent
				+ masterChanges, "master commit");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		IMergeContext mergeContext = prepareContext(repo, workspaceFile,
				MASTER, BRANCH);
		IDiff node = mergeContext.getDiffTree().getDiff(workspaceFile);
		assertNotNull(node);

		IStatus mergeStatus = mergeContext.merge(node, false,
				new NullProgressMonitor());
		assertEquals(IStatus.OK, mergeStatus.getSeverity());
		assertContentEquals(workspaceFile, branchChanges + initialContent
				+ masterChanges);

		Status status = status(repo);
		assertEquals(1, status.getChanged().size());
		assertEquals(0, status.getModified().size());
		assertTrue(status.getChanged().contains(repoRelativePath));
	}

	@Test
	public void mergeModelNoConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);

		String initialContent1 = "some content for the first file";
		String initialContent2 = "some content for the second file";
		testRepo.appendContentAndCommit(iProject, file1, initialContent1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2, initialContent2,
				"second file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());
		String repoRelativePath2 = testRepo.getRepoRelativePath(iFile2
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		final String branchChanges = "branch changes\n";
		setContentsAndCommit(testRepo, iFile1, branchChanges + initialContent1,
				"branch commit");
		setContentsAndCommit(testRepo, iFile2, branchChanges + initialContent2,
				"branch commit");

		testRepo.checkoutBranch(MASTER);

		final String masterChanges = "some changes\n";
		setContentsAndCommit(testRepo, iFile1, initialContent1 + masterChanges,
				"master commit");
		setContentsAndCommit(testRepo, iFile2, initialContent2 + masterChanges,
				"master commit");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		IMergeContext mergeContext = prepareModelContext(repo, iFile1, MASTER,
				BRANCH);
		IDiff node = mergeContext.getDiffTree().getDiff(iFile1);
		assertNotNull(node);
		node = mergeContext.getDiffTree().getDiff(iFile2);
		assertNotNull(node);

		IResourceMappingMerger merger = createMerger();
		IStatus mergeStatus = merger.merge(mergeContext,
				new NullProgressMonitor());
		assertEquals(IStatus.OK, mergeStatus.getSeverity());
		assertContentEquals(iFile1, branchChanges + initialContent1
				+ masterChanges);
		assertContentEquals(iFile2, branchChanges + initialContent2
				+ masterChanges);

		Status status = status(repo);
		assertEquals(2, status.getChanged().size());
		assertEquals(0, status.getModified().size());
		assertTrue(status.getChanged().contains(repoRelativePath1));
		assertTrue(status.getChanged().contains(repoRelativePath2));
	}

	@Test
	public void mergeWithConflict() throws Exception {
		String fileName = "src/Main.java";
		File file = testRepo.createFile(iProject, fileName);
		final String initialContent = "class Main {}\n";
		testRepo.appendContentAndCommit(iProject, file, initialContent,
				"some file");
		testRepo.addToIndex(iProject.getFile(".classpath"));
		testRepo.addToIndex(iProject.getFile(".project"));
		testRepo.commit("project files");

		IFile workspaceFile = testRepo.getIFile(iProject, file);
		String repoRelativePath = testRepo.getRepoRelativePath(workspaceFile
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		final String branchChanges = "branch changes\n";
		setContentsAndCommit(testRepo, workspaceFile, initialContent
				+ branchChanges, "branch commit");

		testRepo.checkoutBranch(MASTER);

		final String masterChanges = "some changes\n";
		setContentsAndCommit(testRepo, workspaceFile, initialContent
				+ masterChanges, "master commit");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		IMergeContext mergeContext = prepareContext(repo, workspaceFile,
				MASTER,
				BRANCH);
		IDiff node = mergeContext.getDiffTree().getDiff(workspaceFile);
		assertNotNull(node);

		IStatus mergeStatus = mergeContext.merge(node, false,
				new NullProgressMonitor());
		assertEquals(IStatus.ERROR, mergeStatus.getSeverity());
		assertContentEquals(workspaceFile, initialContent + masterChanges);

		Status status = status(repo);
		assertEquals(0, status.getChanged().size());
		assertEquals(0, status.getModified().size());
		assertFalse(status.getChanged().contains(repoRelativePath));
	}

	@Test
	public void mergeModelWithConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);

		String initialContent1 = "some content for the first file";
		String initialContent2 = "some content for the second file";
		testRepo.appendContentAndCommit(iProject, file1, initialContent1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2, initialContent2,
				"second file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		final String branchChanges = "branch changes\n";
		setContentsAndCommit(testRepo, iFile1, initialContent1 + branchChanges,
				"branch commit");
		setContentsAndCommit(testRepo, iFile2, initialContent2 + branchChanges,
				"branch commit");

		testRepo.checkoutBranch(MASTER);

		final String masterChanges = "some changes\n";
		setContentsAndCommit(testRepo, iFile1, initialContent1 + masterChanges,
				"master commit");
		setContentsAndCommit(testRepo, iFile2, initialContent2 + masterChanges,
				"master commit");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		IMergeContext mergeContext = prepareModelContext(repo, iFile1, MASTER,
				BRANCH);
		IDiff node = mergeContext.getDiffTree().getDiff(iFile1);
		assertNotNull(node);
		node = mergeContext.getDiffTree().getDiff(iFile2);
		assertNotNull(node);

		IResourceMappingMerger merger = createMerger();
		IStatus mergeStatus = merger.merge(mergeContext,
				new NullProgressMonitor());
		assertEquals(IStatus.ERROR, mergeStatus.getSeverity());

		assertTrue(mergeStatus instanceof IMergeStatus);
		assertEquals(2,
				((IMergeStatus) mergeStatus).getConflictingMappings().length);
		Set<IFile> conflictingFiles = new LinkedHashSet<>();
		for (ResourceMapping conflictingMapping : ((IMergeStatus) mergeStatus)
				.getConflictingMappings()) {
			assertTrue(conflictingMapping instanceof SampleResourceMapping
					&& conflictingMapping.getModelObject() instanceof IFile);
			conflictingFiles.add((IFile) conflictingMapping.getModelObject());
		}
		assertTrue(conflictingFiles.contains(iFile1));
		assertTrue(conflictingFiles.contains(iFile2));

		assertContentEquals(iFile1, initialContent1 + masterChanges);
		assertContentEquals(iFile2, initialContent2 + masterChanges);

		Status status = status(repo);
		assertEquals(0, status.getChanged().size());
		assertEquals(0, status.getModified().size());
	}

	@Test
	public void mergeModelWithDeletedFileNoConflict() throws Exception {
		IEclipsePreferences p = InstanceScope.INSTANCE
				.getNode(Activator.getPluginId());
		p.putBoolean(GitCorePreferences.core_autoStageDeletion, true);
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);

		String initialContent1 = "some content for the first file";
		String initialContent2 = "some content for the second file";
		testRepo.appendContentAndCommit(iProject, file1, initialContent1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2, initialContent2,
				"second file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());
		String repoRelativePath2 = testRepo.getRepoRelativePath(iFile2
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		final String branchChanges = "branch changes\n";
		setContentsAndCommit(testRepo, iFile1, branchChanges + initialContent1,
				"branch commit");
		iFile2.delete(true, new NullProgressMonitor());
		TestUtils.waitForJobs(500, 5000, null);
		assertFalse(iFile2.exists());

		testRepo.addAndCommit(iProject, file2, "branch commit - deleted file2."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.checkoutBranch(MASTER);
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		TestUtils.waitForJobs(500, 5000, null);
		if (!iFile2.exists()) {
			// Debug output to track down sporadically failing test
			System.out.println(iFile2 + " is synchronized? " + Boolean
					.toString(iFile2.isSynchronized(IResource.DEPTH_ZERO)));
			System.out.println(TestUtils.dumpThreads());
			System.out.println("***** WARNING: IFile reported as not existing");
			System.out.println(iProject + " is open? "
					+ Boolean.toString(iProject.isOpen()));
			System.out.println(file2.getPath() + " exists? "
					+ Boolean.toString(file2.exists()));
			System.out.println(iFile2 + " exists now? "
					+ Boolean.toString(iFile2.exists()));
			iFile2 = iProject.getFile(iFile2.getName());
			System.out.println(iFile2 + " exists now? "
					+ Boolean.toString(iFile2.exists()));
			fail(iFile2 + " reported not to exist");
		}
		assertTrue(iFile2.exists());

		final String masterChanges = "some changes\n";
		setContentsAndCommit(testRepo, iFile1, initialContent1 + masterChanges,
				"master commit");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup
		TestUtils.waitForJobs(500, 5000, null);
		IMergeContext mergeContext = prepareModelContext(repo, iFile1, MASTER,
				BRANCH);
		IDiff node = mergeContext.getDiffTree().getDiff(iFile1);
		assertNotNull(node);
		node = mergeContext.getDiffTree().getDiff(iFile2);
		assertNotNull(node);

		IResourceMappingMerger merger = createMerger();
		IStatus mergeStatus = merger.merge(mergeContext,
				new NullProgressMonitor());
		assertEquals(IStatus.OK, mergeStatus.getSeverity());
		assertContentEquals(iFile1, branchChanges + initialContent1
				+ masterChanges);
		assertFalse(iFile2.exists());

		Status status = status(repo);
		assertEquals(1, status.getChanged().size());
		assertEquals(1, status.getRemoved().size());
		assertEquals(0, status.getModified().size());
		assertTrue(status.getChanged().contains(repoRelativePath1));
		assertTrue(status.getRemoved().contains(repoRelativePath2));
	}
}
