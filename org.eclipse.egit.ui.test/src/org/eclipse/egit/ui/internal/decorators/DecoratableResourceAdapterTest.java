/*******************************************************************************
 * Copyright (C) 2011, Philipp Thun <philipp.thun@sap.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2011, Christian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.decorators.IDecoratableResource.Staged;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.junit.LocalDiskRepositoryTestCase;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.team.core.RepositoryProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DecoratableResourceAdapterTest extends LocalDiskRepositoryTestCase {

	private static final String TEST_PROJECT = "TestProject";

	private static final String TEST_FILE = "TestFile";

	private static final String TEST_FILE2 = "TestFolder2/TestFile2";

	private static final String TEST_FOLDER = "TestFolder";

	private static final String TEST_FOLDER2 = "TestFolder2";

	private static final String SUB_FOLDER = "SubFolder";

	private File gitDir;

	private Repository repository;

	private IProject project;

	private Git git;

	private IndexDiffCacheEntry indexDiffCacheEntry;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		gitDir = new File(root.getLocation().toFile(), Constants.DOT_GIT);

		repository = new FileRepository(gitDir);
		repository.create();
		repository.close();
		repository = Activator.getDefault().getRepositoryCache()
				.lookupRepository(gitDir);

		project = root.getProject(TEST_PROJECT);
		project.create(null);
		project.open(null);

		project.getFolder(TEST_FOLDER2).create(true, true, null);
		IFile testFile2 = project.getFile(TEST_FILE2);
		testFile2.create(new ByteArrayInputStream("content".getBytes()), true,
				null);

		RepositoryMapping mapping = new RepositoryMapping(project, gitDir);

		GitProjectData projectData = new GitProjectData(project);
		projectData.setRepositoryMappings(Collections.singleton(mapping));
		projectData.store();
		GitProjectData.add(project, projectData);

		RepositoryProvider.map(project, GitProvider.class.getName());

		git = new Git(repository);
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Initial commit").call();

		indexDiffCacheEntry = Activator.getDefault().getIndexDiffCache()
				.getIndexDiffCacheEntry(repository);
		waitForIndexDiffUpdate(false);
	}

	private void waitForIndexDiffUpdate(final boolean refreshCache)
			throws Exception {
		if (refreshCache)
			indexDiffCacheEntry.refresh();
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();

		// Reverse setup...

		RepositoryProvider.unmap(project);

		GitProjectData.delete(project);

		project.delete(true, true, null);

		repository.close();

		Activator.getDefault().getRepositoryCache().clear();

		recursiveDelete(gitDir);
	}

	@Test
	public void testDecorationEmptyProject() throws Exception {
		IDecoratableResource[] expectedDRs = new IDecoratableResource[] { new TestDecoratableResource(
				project, true, false, false, false, Staged.NOT_STAGED) };

		IDecoratableResource[] actualDRs = { new DecoratableResourceAdapter(
				indexDiffCacheEntry.getIndexDiff(), project) };

		for (int i = 0; i < expectedDRs.length; i++)
			assertTrue(expectedDRs[i].equals(actualDRs[i]));
	}

	@Test
	public void testDecorationNewFolder() throws Exception {
		// Create new folder with sub folder
		IFolder folder = project.getFolder(TEST_FOLDER);
		folder.create(true, true, null);
		IFolder subFolder = folder.getFolder(SUB_FOLDER);
		subFolder.create(true, true, null);

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				new TestDecoratableResource(project, true, false, false, false,
						Staged.NOT_STAGED),
				new TestDecoratableResource(folder, false, false, false, false,
						Staged.NOT_STAGED),
				new TestDecoratableResource(subFolder, false, false, false,
						false, Staged.NOT_STAGED) };

		waitForIndexDiffUpdate(true);
		IndexDiffData indexDiffData = indexDiffCacheEntry.getIndexDiff();
		IDecoratableResource[] actualDRs = {
				new DecoratableResourceAdapter(indexDiffData, project),
				new DecoratableResourceAdapter(indexDiffData, folder),
				new DecoratableResourceAdapter(indexDiffData, subFolder) };

		for (int i = 0; i < expectedDRs.length; i++)
			assertTrue(expectedDRs[i].equals(actualDRs[i]));
	}

	@Test
	public void testDecorationFolderPrefixOfOtherFolder() throws Exception {
		project.getFolder(TEST_FOLDER).create(true, true, null);
		IFolder testFolder2 = project.getFolder(TEST_FOLDER2);

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] { new TestDecoratableResource(
				testFolder2, true, false, false, false, Staged.NOT_STAGED) };
		waitForIndexDiffUpdate(true);
		IndexDiffData indexDiffData = indexDiffCacheEntry.getIndexDiff();
		IDecoratableResource[] actualDRs = { new DecoratableResourceAdapter(
				indexDiffData, testFolder2) };

		for (int i = 0; i < expectedDRs.length; i++)
			assertTrue(expectedDRs[i].equals(actualDRs[i]));
	}

	@Test
	public void testDecorationNewFile() throws Exception {
		// Create new file
		write(new File(project.getLocation().toFile(), TEST_FILE), "Something");
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		IResource file = project.findMember(TEST_FILE);

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				new TestDecoratableResource(project, true, false, true, false,
						Staged.NOT_STAGED),
				new TestDecoratableResource(file, false, false, false, false,
						Staged.NOT_STAGED) };
		waitForIndexDiffUpdate(true);
		IndexDiffData indexDiffData = indexDiffCacheEntry.getIndexDiff();
		IDecoratableResource[] actualDRs = {
				new DecoratableResourceAdapter(indexDiffData, project),
				new DecoratableResourceAdapter(indexDiffData, file) };

		for (int i = 0; i < expectedDRs.length; i++)
			assertTrue(expectedDRs[i].equals(actualDRs[i]));
	}

	@Test
	public void testDecorationNewFileInSubfolder() throws Exception {
		// Create new folder with sub folder
		IFolder folder = project.getFolder(TEST_FOLDER);
		folder.create(true, true, null);
		IFolder subFolder = folder.getFolder(SUB_FOLDER);
		subFolder.create(true, true, null);
		// Create new file
		write(new File(subFolder.getLocation().toFile().getAbsolutePath(), TEST_FILE), "Something");
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		IResource file = subFolder.findMember(TEST_FILE);

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				new TestDecoratableResource(project, true, false, true, false,
						Staged.NOT_STAGED),
				new TestDecoratableResource(folder, false, false, true, false,
						Staged.NOT_STAGED),
				new TestDecoratableResource(subFolder, false, false, true,
						false, Staged.NOT_STAGED),
				new TestDecoratableResource(file, false, false, false, false,
						Staged.NOT_STAGED) };
		waitForIndexDiffUpdate(true);
		IndexDiffData indexDiffData = indexDiffCacheEntry.getIndexDiff();
		IDecoratableResource[] actualDRs = {
				new DecoratableResourceAdapter(indexDiffData, project),
				new DecoratableResourceAdapter(indexDiffData, folder),
				new DecoratableResourceAdapter(indexDiffData, subFolder),
				new DecoratableResourceAdapter(indexDiffData, file) };

		for (int i = 0; i < expectedDRs.length; i++)
			assertTrue(expectedDRs[i].equals(actualDRs[i]));
	}

	@Test
	public void testDecorationAddedFile() throws Exception {
		// Create new file
		write(new File(project.getLocation().toFile(), TEST_FILE), "Something");
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		IResource file = project.findMember(TEST_FILE);
		// Add file
		git.add().addFilepattern(".").call();

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				new TestDecoratableResource(project, true, false, false, false,
						Staged.MODIFIED),
				new TestDecoratableResource(file, true, false, false, false,
						Staged.ADDED) };
		waitForIndexDiffUpdate(true);
		IndexDiffData indexDiffData = indexDiffCacheEntry.getIndexDiff();
		IDecoratableResource[] actualDRs = {
				new DecoratableResourceAdapter(indexDiffData, project),
				new DecoratableResourceAdapter(indexDiffData, file) };

		for (int i = 0; i < expectedDRs.length; i++)
			assertTrue(expectedDRs[i].equals(actualDRs[i]));
	}

	@Test
	public void testDecorationCommittedFile() throws Exception {
		// Create new file
		write(new File(project.getLocation().toFile(), TEST_FILE), "Something");
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		IResource file = project.findMember(TEST_FILE);
		// Add and commit file
		git.add().addFilepattern(".").call();
		git.commit().setMessage("First commit").call();

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				new TestDecoratableResource(project, true, false, false, false,
						Staged.NOT_STAGED),
				new TestDecoratableResource(file, true, false, false, false,
						Staged.NOT_STAGED) };

		waitForIndexDiffUpdate(true);
		IndexDiffData indexDiffData = indexDiffCacheEntry.getIndexDiff();
		IDecoratableResource[] actualDRs = {
				new DecoratableResourceAdapter(indexDiffData, project),
				new DecoratableResourceAdapter(indexDiffData, file) };

		for (int i = 0; i < expectedDRs.length; i++)
			assertTrue(expectedDRs[i].equals(actualDRs[i]));
	}

	@Test
	public void testDecorationModifiedFile() throws Exception {
		// Create new file
		File f = new File(project.getLocation().toFile(), TEST_FILE);
		write(f, "Something");
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		IResource file = project.findMember(TEST_FILE);
		// Add and commit file
		git.add().addFilepattern(".").call();
		git.commit().setMessage("First commit").call();

		// Change file content
		write(f, "SomethingElse");

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				new TestDecoratableResource(project, true, false, true, false,
						Staged.NOT_STAGED),
				new TestDecoratableResource(file, true, false, true, false,
						Staged.NOT_STAGED) };

		waitForIndexDiffUpdate(true);
		IndexDiffData indexDiffData = indexDiffCacheEntry.getIndexDiff();
		IDecoratableResource[] actualDRs = {
				new DecoratableResourceAdapter(indexDiffData, project),
				new DecoratableResourceAdapter(indexDiffData, file) };

		for (int i = 0; i < expectedDRs.length; i++)
			assertTrue(expectedDRs[i].equals(actualDRs[i]));
	}

	@Test
	public void testDecorationConflictingFile() throws Exception {
		// Create new file
		File f = new File(project.getLocation().toFile(), TEST_FILE);
		write(f, "Something");
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		IResource file = project.findMember(TEST_FILE);
		// Add and commit file
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Commit on master branch").call();

		// Create and checkout new branch, change file content, add and commit
		// file
		git.checkout().setCreateBranch(true).setName("first_topic").call();
		write(f, "SomethingElse");
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		git.add().addFilepattern(".").call();
		RevCommit commitOnFirstTopicBranch = git.commit()
				.setMessage("Commit on first topic branch").call();

		// Create and checkout new branch (from master), change file content,
		// add and commit file
		git.checkout().setName("master").call();
		git.checkout().setCreateBranch(true).setName("second_topic").call();
		write(f, "SomethingDifferent");
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Commit on second topic branch").call();

		// Merge HEAD ('Commit on second topic branch') with 'Commit on first
		// topic branch' to create a conflict
		assertTrue(git.merge().include(commitOnFirstTopicBranch).call()
				.getMergeStatus() == MergeStatus.CONFLICTING);

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				new TestDecoratableResource(project, true, false, false, true,
						Staged.NOT_STAGED),
				new TestDecoratableResource(file, true, false, false, true,
						Staged.NOT_STAGED) };

		waitForIndexDiffUpdate(true);
		IndexDiffData indexDiffData = indexDiffCacheEntry.getIndexDiff();
		IDecoratableResource[] actualDRs = {
				new DecoratableResourceAdapter(indexDiffData, project),
				new DecoratableResourceAdapter(indexDiffData, file) };

		for (int i = 0; i < expectedDRs.length; i++)
			assertTrue(expectedDRs[i].equals(actualDRs[i]));
	}
}

class TestDecoratableResource extends DecoratableResource {

	public TestDecoratableResource(IResource resource, boolean tracked,
			boolean ignored, boolean dirty, boolean conflicts, Staged staged) {
		super(resource);
		this.tracked = tracked;
		this.ignored = ignored;
		this.dirty = dirty;
		this.conflicts = conflicts;
		this.staged = staged;
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof IDecoratableResource))
			return false;

		IDecoratableResource decoratableResource = (IDecoratableResource) obj;
		if (!(decoratableResource.getType() == getType()))
			return false;
		if (!decoratableResource.getName().equals(getName()))
			return false;
		if (!(decoratableResource.isTracked() == isTracked()))
			return false;
		if (!(decoratableResource.isIgnored() == isIgnored()))
			return false;
		if (!(decoratableResource.isDirty() == isDirty()))
			return false;
		if (!(decoratableResource.hasConflicts() == hasConflicts()))
			return false;
		if (!decoratableResource.staged().equals(staged()))
			return false;

		return true;
	}

	public int hashCode() {
		// this appeases FindBugs
		return super.hashCode();
	}
}
