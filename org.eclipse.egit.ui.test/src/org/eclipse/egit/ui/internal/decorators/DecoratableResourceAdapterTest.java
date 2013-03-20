/*******************************************************************************
 * Copyright (C) 2011, Philipp Thun <philipp.thun@sap.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2011, Christian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2013, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import static org.eclipse.jgit.junit.JGitTestUtil.write;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.internal.Activator;
import org.eclipse.egit.core.internal.JobFamilies;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.decorators.IDecoratableResource.Staged;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DecoratableResourceAdapterTest extends LocalRepositoryTestCase {

	private static final String TEST_FILE = "TestFile";

	private static final String TEST_FILE2 = "TestFolder2/TestFile2";

	private static final String TEST_FOLDER = "TestFolder";

	private static final String TEST_FOLDER2 = "TestFolder2";

	private static final String SUB_FOLDER = "SubFolder";

	private File gitDir;

	private IProject project;

	private Git git;

	private IndexDiffCacheEntry indexDiffCacheEntry;

	@Before
	public void setUp() throws Exception {
		gitDir = createProjectAndCommitToRepository();
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1);

		FileRepository repo = lookupRepository(gitDir);
		git = new Git(repo);
		indexDiffCacheEntry = Activator.getDefault().getIndexDiffCache()
				.getIndexDiffCacheEntry(repo);
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
		deleteAllProjects();
		shutDownRepositories();
		FileUtils.delete(gitDir.getParentFile(), FileUtils.RECURSIVE
				| FileUtils.RETRY);
	}

	@Test
	public void testDecorationEmptyProject() throws Exception {
		IDecoratableResource[] expectedDRs = new IDecoratableResource[] { new TestDecoratableResource(
				project, true, false, false, false, Staged.NOT_STAGED) };

		IDecoratableResource[] actualDRs = { new DecoratableResourceAdapter(
				indexDiffCacheEntry.getIndexDiff(), project) };

		assertArrayEquals(expectedDRs, actualDRs);
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

		assertArrayEquals(expectedDRs, actualDRs);
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

		assertArrayEquals(expectedDRs, actualDRs);
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

		assertArrayEquals(expectedDRs, actualDRs);
	}

	@Test
	public void testDecorationIgnoredFile() throws Exception {
		// Create new file

		write(new File(project.getLocation().toFile(), "Test.dat"), "Something");
		write(new File(project.getLocation().toFile(), TEST_FILE2), "Something");
		write(new File(project.getLocation().toFile(), "Test"), "Something");
		write(new File(project.getLocation().toFile(), ".gitignore"), "Test"); // Test is prefix of TestFile
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		IResource file = project.findMember("Test.dat");
		IResource gitignore = project.findMember(".gitignore");
		IResource test = project.findMember("Test");
		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				new TestDecoratableResource(project, true, false, true, false,
						Staged.NOT_STAGED),
				new TestDecoratableResource(gitignore, false, false, false, false,
						Staged.NOT_STAGED),
				new TestDecoratableResource(file, false, false, false, false,
						Staged.NOT_STAGED),
				new TestDecoratableResource(test, false, true, false, false,
						Staged.NOT_STAGED)
		};
		waitForIndexDiffUpdate(true);
		IndexDiffData indexDiffData = indexDiffCacheEntry.getIndexDiff();
		IDecoratableResource[] actualDRs = {
				new DecoratableResourceAdapter(indexDiffData, project),
				new DecoratableResourceAdapter(indexDiffData, gitignore),
				new DecoratableResourceAdapter(indexDiffData, file),
				new DecoratableResourceAdapter(indexDiffData, test)
		};

		assertArrayEquals(expectedDRs, actualDRs);
	}

	@Test
	public void testDecorationFileInIgnoredFolder() throws Exception {
		// Create new file

		FileUtils.mkdir(new File(project.getLocation().toFile(),"dir"));
		write(new File(project.getLocation().toFile(), "dir/file"), "Something");
		write(new File(project.getLocation().toFile(), ".gitignore"), "dir");
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		IResource dir = project.findMember("dir");
		IResource file = project.findMember("dir/file");
		IResource gitignore = project.findMember(".gitignore");
		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				new TestDecoratableResource(project, true, false, true, false,
						Staged.NOT_STAGED),
				new TestDecoratableResource(gitignore, false, false, false, false,
						Staged.NOT_STAGED),
				new TestDecoratableResource(file, false, true, false, false,
						Staged.NOT_STAGED),
				new TestDecoratableResource(dir, false, true, false, false,
						Staged.NOT_STAGED)
		};
		waitForIndexDiffUpdate(true);
		IndexDiffData indexDiffData = indexDiffCacheEntry.getIndexDiff();
		IDecoratableResource[] actualDRs = {
				new DecoratableResourceAdapter(indexDiffData, project),
				new DecoratableResourceAdapter(indexDiffData, gitignore),
				new DecoratableResourceAdapter(indexDiffData, file),
				new DecoratableResourceAdapter(indexDiffData, dir)
		};

		assertArrayEquals(expectedDRs, actualDRs);
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
			assert(expectedDRs[i].equals(actualDRs[i]));
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

		assertArrayEquals(expectedDRs, actualDRs);
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

		assertArrayEquals(expectedDRs, actualDRs);
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

		assertArrayEquals(expectedDRs, actualDRs);
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

		assertArrayEquals(expectedDRs, actualDRs);
	}

	@Test
	public void testDecorationDeletedFile() throws Exception {
		// Create new file
		File f = new File(project.getLocation().toFile(), TEST_FILE);
		write(f, "Something");
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		// Add and commit file
		git.add().addFilepattern(".").call();
		git.commit().setMessage("First commit").call();

		// Delete file
		FileUtils.delete(f);

		IDecoratableResource expectedDR = new TestDecoratableResource(project,
				true, false, true, false, Staged.NOT_STAGED);

		waitForIndexDiffUpdate(true);
		IndexDiffData indexDiffData = indexDiffCacheEntry.getIndexDiff();
		IDecoratableResource actualDR = new DecoratableResourceAdapter(
				indexDiffData, project);

		assertEquals(expectedDR, actualDR);
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

	@Override
	public String toString() {
		return "TestDecoratableResourceAdapter[" + getName() + (isTracked() ? ", tracked" : "") + (isIgnored() ? ", ignored" : "") + (isDirty() ? ", dirty" : "") + (hasConflicts() ? ",conflicts" : "") + ", staged=" + staged() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$//$NON-NLS-7$//$NON-NLS-8$//$NON-NLS-9$//$NON-NLS-10$//$NON-NLS-11$
	}
}
