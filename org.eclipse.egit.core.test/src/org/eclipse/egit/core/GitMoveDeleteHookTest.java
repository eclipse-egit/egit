/*******************************************************************************
 * Copyright (C) 2011, Robin Rosenberg
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.egit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.test.TestProject;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.egit.core.test.TestUtils;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.junit.MockSystemReader;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.SystemReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

/**
 * All sorts of interesting cases
 */
@RunWith(Theories.class)
public class GitMoveDeleteHookTest {
	@DataPoints
	public static boolean[] autoStage = { true, false };

	TestUtils testUtils = new TestUtils();

	TestRepository testRepository;

	Repository repository;

	Set<File> testDirs = new HashSet<>();

	File workspaceSupplement;

	File workspace;

	@Before
	public void setUp() throws Exception {
		Activator.getDefault().getRepositoryCache().clear();
		MockSystemReader mockSystemReader = new MockSystemReader();
		SystemReader.setInstance(mockSystemReader);
		mockSystemReader.setProperty(Constants.GIT_CEILING_DIRECTORIES_KEY,
				ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile()
						.getParentFile().getAbsoluteFile().toString());
		workspaceSupplement = testUtils.createTempDir("wssupplement");
		testDirs.add(testUtils.getBaseTempDir());
		workspace = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().getAbsoluteFile();
	}

	@After
	public void tearDown() throws IOException, CoreException {
		ResourcesPlugin.getWorkspace().getRoot().delete(IResource.FORCE, null);
		if (testRepository != null)
			testRepository.dispose();
		repository = null;
		for (File d : testDirs)
			if (d.exists())
				FileUtils.delete(d, FileUtils.RECURSIVE | FileUtils.RETRY);
		SystemReader.setInstance(null);
	}

	private TestProject initRepoInsideProjectInsideWorkspace()
			throws Exception {
		TestProject project = new TestProject(true, "Project-1", true, workspaceSupplement);
		File gitDir = new File(project.getProject().getLocationURI().getPath(),
				Constants.DOT_GIT);
		testDirs.add(gitDir);
		testRepository = new TestRepository(gitDir);
		repository = testRepository.getRepository();
		testRepository.connect(project.getProject());
		registerWorkspaceRelativeTestDir("Project-1");
		return project;
	}

	private TestProject initRepoInsideProjectOutsideWorkspace()
			throws Exception {
		TestProject project = new TestProject(true, "Project-1", false,
				workspaceSupplement);
		File gitDir = new File(project.getProject().getLocationURI().getPath(),
				Constants.DOT_GIT);
		testDirs.add(gitDir);
		testRepository = new TestRepository(gitDir);
		repository = testRepository.getRepository();
		testRepository.connect(project.getProject());
		return project;
	}

	private TestProject initRepoAboveProjectInsideWs(String srcParent, String d)
			throws Exception {
		return initRepoAboveProject(srcParent, d, true);
	}

	private TestProject initRepoAboveProject(String srcParent, String d, boolean insidews)
			throws Exception {
		registerWorkspaceRelativeTestDir(srcParent);
		TestProject project = new TestProject(true, srcParent + "Project-1", insidews, workspaceSupplement);
		File gd = new File(insidews?workspace:workspaceSupplement, d);

		File gitDir = new File(gd, Constants.DOT_GIT);
		testDirs.add(gitDir);
		testRepository = new TestRepository(gitDir);
		repository = testRepository.getRepository();
		testRepository.connect(project.getProject());
		return project;
	}

	@Theory
	public void testDeleteFile(boolean autoStageDelete) throws Exception {
		IEclipsePreferences p = InstanceScope.INSTANCE
				.getNode(Activator.getPluginId());
		p.putBoolean(GitCorePreferences.core_autoStageDeletion,
				autoStageDelete);

		TestProject project = initRepoInsideProjectInsideWorkspace();
		testUtils.addFileToProject(project.getProject(), "file.txt",
				"some text");
		testUtils.addFileToProject(project.getProject(), "file2.txt",
				"some  more text");
		IFile file = project.getProject().getFile("file.txt");
		AddToIndexOperation addToIndexOperation = new AddToIndexOperation(
				new IResource[] { file,
						project.getProject().getFile("file2.txt") });
		addToIndexOperation.execute(null);

		// Validate pre-conditions
		DirCache dirCache = DirCache.read(repository.getIndexFile(),
				FS.DETECTED);
		assertEquals(2, dirCache.getEntryCount());
		assertNotNull(dirCache.getEntry("file.txt"));
		assertNotNull(dirCache.getEntry("file2.txt"));
		// Modify the content before the move
		testUtils.changeContentOfFile(project.getProject(), file, "other text");
		TestUtils.waitForJobs(500, 10000, JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		file.delete(true, null);

		TestUtils.waitForJobs(500, 10000, JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		// Check index for the deleted file
		dirCache.read();
		if (autoStageDelete) {
			assertEquals(1, dirCache.getEntryCount());
			assertNull(dirCache.getEntry("file.txt"));
		} else {
			assertEquals(2, dirCache.getEntryCount());
			assertNotNull(dirCache.getEntry("file.txt"));
		}
		assertNotNull(dirCache.getEntry("file2.txt"));
		// Actual file is deleted
		assertFalse(file.exists());
		// But a non-affected file remains
		assertTrue(project.getProject().getFile("file2.txt").exists());
	}

	@Test
	public void testDeleteFolder() throws Exception {
		TestProject project = initRepoInsideProjectInsideWorkspace();
		testUtils.addFileToProject(project.getProject(), "folder/file.txt",
				"some text");
		testUtils.addFileToProject(project.getProject(), "folder2/file.txt",
				"some other text");
		AddToIndexOperation addToIndexOperation = new AddToIndexOperation(
				new IResource[] {
						project.getProject().getFile("folder/file.txt"),
						project.getProject().getFile("folder2/file.txt") });
		addToIndexOperation.execute(null);

		DirCache dirCache = DirCache.read(repository.getIndexFile(),
				FS.DETECTED);
		assertNotNull(dirCache.getEntry("folder/file.txt"));
		assertNotNull(dirCache.getEntry("folder2/file.txt"));
		// Modify the content before the move
		testUtils.changeContentOfFile(project.getProject(), project
				.getProject().getFile("folder/file.txt"), "other text");
		project.getProject().getFolder("folder").delete(true, null);

		dirCache.read();
		// Unlike delete file, dircache is untouched... pretty illogical
		// TODO: Change the behavior of the hook.
		assertNotNull(dirCache.getEntry("folder/file.txt"));
		// Not moved file still there
		assertNotNull(dirCache.getEntry("folder2/file.txt"));
	}

	@Test
	public void testDeleteProject() throws Exception {
		TestProject project = initRepoAboveProjectInsideWs("P/", "");
		testUtils.addFileToProject(project.getProject(), "file.txt",
				"some text");
		AddToIndexOperation addToIndexOperation = new AddToIndexOperation(
				new IResource[] { project.getProject().getFile("file.txt") });
		addToIndexOperation.execute(null);

		RepositoryMapping mapping = RepositoryMapping.getMapping(project
				.getProject());
		IPath gitDirAbsolutePath = mapping.getGitDirAbsolutePath();
		Repository db = FileRepositoryBuilder.create(gitDirAbsolutePath
				.toFile());
		DirCache index = DirCache.read(db.getIndexFile(), db.getFS());
		assertNotNull(index.getEntry("P/Project-1/file.txt"));
		db.close();
		db = null;
		project.getProject().delete(true, null);
		assertNull(RepositoryMapping.getMapping(project.getProject()));
		// Check that the repo is still there. Being a bit paranoid we look for
		// a file
		assertTrue(gitDirAbsolutePath.toString(),
				gitDirAbsolutePath.append("HEAD").toFile().exists());

		db = FileRepositoryBuilder.create(gitDirAbsolutePath.toFile());
		index = DirCache.read(db.getIndexFile(), db.getFS());
		// FIXME: Shouldn't we unstage deleted projects?
		assertNotNull(index.getEntry("P/Project-1/file.txt"));
		db.close();
	}

	private void configureAutoStageMoves(boolean autoStageMoves) {
		IEclipsePreferences p = InstanceScope.INSTANCE
				.getNode(Activator.getPluginId());
		p.putBoolean(GitCorePreferences.core_autoStageMoves, autoStageMoves);
	}

	@Theory
	public void testMoveFile(boolean autoStageMoves) throws Exception {
		configureAutoStageMoves(autoStageMoves);

		TestProject project = initRepoInsideProjectInsideWorkspace();
		testUtils.addFileToProject(project.getProject(), "file.txt",
				"some text");
		testUtils.addFileToProject(project.getProject(), "file2.txt",
				"some  more text");
		AddToIndexOperation addToIndexOperation = new AddToIndexOperation(
				new IResource[] { project.getProject().getFile("file.txt"),
						project.getProject().getFile("file2.txt") });
		addToIndexOperation.execute(null);

		// Validate pre-conditions
		DirCache dirCache = DirCache.read(repository.getIndexFile(),
				FS.DETECTED);
		assertNotNull(dirCache.getEntry("file.txt"));
		assertNotNull(dirCache.getEntry("file2.txt"));
		assertNull(dirCache.getEntry("data.txt"));
		assertFalse(project.getProject().getFile("data.txt").exists());
		ObjectId oldContentId = dirCache.getEntry("file.txt").getObjectId();
		// Modify the content before the move
		testUtils.changeContentOfFile(project.getProject(), project
				.getProject().getFile("file.txt"), "other text");
		project.getProject()
				.getFile("file.txt")
				.move(project.getProject().getFile("data.txt").getFullPath(),
						false, null);

		dirCache.read();
		assertTrue(project.getProject().getFile("data.txt").exists());
		if (autoStageMoves) {
			assertNotNull(dirCache.getEntry("data.txt"));
			// Same content in index as before the move
			assertEquals(oldContentId,
					dirCache.getEntry("data.txt").getObjectId());
		} else {
			assertNull(dirCache.getEntry("data.txt"));
		}

		// Not moved file still in its old place
		assertNotNull(dirCache.getEntry("file2.txt"));
	}

	/**
	 * Rename "folder" to "dir".
	 * @param autoStageMoves
	 * @throws Exception
	 */
	@Theory
	public void testMoveFolder(boolean autoStageMoves) throws Exception {
		configureAutoStageMoves(autoStageMoves);

		TestProject project = initRepoInsideProjectInsideWorkspace();
		testUtils.addFileToProject(project.getProject(), "folder/file.txt",
				"some text");
		testUtils.addFileToProject(project.getProject(), "folder2/file.txt",
				"some other text");
		AddToIndexOperation addToIndexOperation = new AddToIndexOperation(
				new IResource[] {
						project.getProject().getFile("folder/file.txt"),
						project.getProject().getFile("folder2/file.txt") });
		addToIndexOperation.execute(null);

		DirCache dirCache = DirCache.read(repository.getIndexFile(),
				FS.DETECTED);
		assertNotNull(dirCache.getEntry("folder/file.txt"));
		assertNotNull(dirCache.getEntry("folder2/file.txt"));
		assertNull(dirCache.getEntry("dir/file.txt"));
		assertFalse(project.getProject().getFile("dir/file.txt").exists());
		ObjectId oldContentId = dirCache.getEntry("folder/file.txt")
				.getObjectId();
		// Modify the content before the move
		testUtils.changeContentOfFile(project.getProject(), project
				.getProject().getFile("folder/file.txt"), "other text");
		project.getProject()
				.getFolder("folder")
				.move(project.getProject().getFolder("dir").getFullPath(),
						false, null);

		dirCache.read();
		assertTrue(project.getProject().getFile("dir/file.txt").exists());
		if (autoStageMoves) {
			assertNull(dirCache.getEntry("folder/file.txt"));
			assertNotNull(dirCache.getEntry("dir/file.txt"));
			// Same content in index as before the move
			assertEquals(oldContentId,
					dirCache.getEntry("dir/file.txt").getObjectId());
		} else {
			assertNotNull(dirCache.getEntry("folder/file.txt"));
			assertNull(dirCache.getEntry("dir/file.txt"));
		}
		// Not moved file still there
		assertNotNull(dirCache.getEntry("folder2/file.txt"));
	}

	/**
	 * Rename and move a project in the workspace containing a Git repository.
	 * <p>
	 * The repository will be moved with the project.
	 * Note that there is no way to rename a project in the workspace without
	 * moving it. See https://bugs.eclipse.org/358828 for a discussion.
	 *
	 * @throws Exception
	 */
	@Test
	public void testMoveAndRenameProjectContainingGitRepo() throws Exception {
		ResourcesPlugin.getWorkspace().getRoot().getProject("Project-1").delete(true, null);
		ResourcesPlugin.getWorkspace().getRoot().getProject("P2").delete(true, null);

		TestProject project = initRepoInsideProjectInsideWorkspace();
		testUtils.addFileToProject(project.getProject(), "file.txt",
				"some text");
		AddToIndexOperation addToIndexOperation = new AddToIndexOperation(
				new IResource[] { project.getProject().getFile("file.txt") });
		addToIndexOperation.execute(null);
		IProjectDescription description = project.getProject().getDescription();
		description.setName("P2");
		registerWorkspaceRelativeTestDir("P2");
		project.getProject().move(description,
				IResource.FORCE | IResource.SHALLOW, null);
		IProject project2 = ResourcesPlugin.getWorkspace().getRoot()
				.getProject("P2");
		assertNotNull(RepositoryMapping.getMapping(project2.getProject()));
		Repository movedRepo = RepositoryMapping.getMapping(project2)
				.getRepository();
		assertEquals("P2",
				movedRepo.getDirectory().getParentFile()
						.getName());
		DirCache dc = movedRepo.readDirCache();
		assertEquals(1, dc.getEntryCount());
		assertEquals("file.txt", dc.getEntry(0).getPathString());

		assertFalse(ResourcesPlugin.getWorkspace().getRoot().getProject("Project-1").exists());
	}

	/**
	 * Rename a project outside the workspace containing a Git repository.
	 * <p>
	 * Note the similarity of the code with {@link #testMoveAndRenameProjectContainingGitRepo()}
	 *
	 * @throws Exception
	 */
	@Test
	public void testRenameProjectOutsideWorkspaceContainingGitRepo() throws Exception {
		ResourcesPlugin.getWorkspace().getRoot().getProject("Project-1").delete(true, null);
		ResourcesPlugin.getWorkspace().getRoot().getProject("P2").delete(true, null);
		TestProject project = initRepoInsideProjectOutsideWorkspace();
		testUtils.addFileToProject(project.getProject(), "file.txt",
				"some text");
		AddToIndexOperation addToIndexOperation = new AddToIndexOperation(
				new IResource[] { project.getProject().getFile("file.txt") });
		addToIndexOperation.execute(null);
		IProjectDescription description = project.getProject().getDescription();
		description.setName("P2");
		project.getProject().move(description,
				IResource.FORCE | IResource.SHALLOW, null);
		IProject project2 = ResourcesPlugin.getWorkspace().getRoot()
				.getProject("P2");
		assertNotNull(RepositoryMapping.getMapping(project2.getProject()));
		Repository movedRepo = RepositoryMapping.getMapping(project2)
				.getRepository();
		assertEquals("Project-1",
				movedRepo.getDirectory().getParentFile()
						.getName());
		DirCache dc = movedRepo.readDirCache();
		assertEquals(1, dc.getEntryCount());
		assertEquals("file.txt", dc.getEntry(0).getPathString());

		assertFalse(ResourcesPlugin.getWorkspace().getRoot().getProject("Project-1").exists());
	}

	/**
	 * Move a project outside the workspace containing a Git repository, but do not rename it.
	 * <p>
	 * Note the similarity of the code with {@link #testMoveAndRenameProjectContainingGitRepo()}
	 *
	 * @throws Exception
	 */
	@Test
	public void testMoveButDoNotRenameProjectOutsideWorkspaceContainingGitRepo() throws Exception {
		ResourcesPlugin.getWorkspace().getRoot().getProject("Project-1").delete(true, null);
		ResourcesPlugin.getWorkspace().getRoot().getProject("P2").delete(true, null);
		TestProject project = initRepoInsideProjectOutsideWorkspace();
		testUtils.addFileToProject(project.getProject(), "file.txt",
				"some text");
		AddToIndexOperation addToIndexOperation = new AddToIndexOperation(
				new IResource[] { project.getProject().getFile("file.txt") });
		addToIndexOperation.execute(null);
		IProjectDescription description = project.getProject().getDescription();
		description.setLocationURI(URIUtil.toURI(new Path(new File(project.getWorkspaceSupplement(), "P2").getAbsolutePath())));
		project.getProject().move(description,
				IResource.FORCE | IResource.SHALLOW, null);
		IProject project2 = ResourcesPlugin.getWorkspace().getRoot()
				.getProject("Project-1"); // same name
		assertNotNull(RepositoryMapping.getMapping(project2.getProject()));
		Repository movedRepo = RepositoryMapping.getMapping(project2)
				.getRepository();
		assertEquals("P2",
				movedRepo.getDirectory().getParentFile()
						.getName());
		DirCache dc = movedRepo.readDirCache();
		assertEquals(1, dc.getEntryCount());
		assertEquals("file.txt", dc.getEntry(0).getPathString());

		assertFalse(ResourcesPlugin.getWorkspace().getRoot().getProject("P2").exists());
	}


	@Theory
	public void testMoveProjectWithinGitRepoMoveAtSameTopLevel(
			boolean autoStageMoves)
			throws Exception {
		dotestMoveProjectWithinRepoWithinWorkspace("", "Project-1", "", "P2",
				"", autoStageMoves);
	}

	@Theory
	public void testMoveProjectWithinGitRepoMoveFromTopOneLevelDown(
			boolean autoStageMoves)
			throws Exception {
		dotestMoveProjectWithinRepoWithinWorkspace("", "Project-1", "X/", "P2",
				"", autoStageMoves);
	}

	@Theory
	public void testMoveProjectWithinGitRepoMoveFromOneLevelDownToTop(
			boolean autoStageMoves)
			throws Exception {
		dotestMoveProjectWithinRepoWithinWorkspace("P/", "Project-1", "", "P2",
				"", autoStageMoves);
	}

	@Theory
	public void testMoveProjectWithinGitRepoMoveFromOneLevelDownToSameDepth(
			boolean autoStageMoves)
			throws Exception {
		dotestMoveProjectWithinRepoWithinWorkspace("P/", "Project-1", "X/",
				"P2", "", autoStageMoves);
	}

	@Theory
	public void testMoveProjectWithinGitRepoMoveFromOneLevelDownOutsideTheRepo(
			boolean autoStageMoves) throws Exception {
		dotestMoveProjectWithinRepoWithinWorkspace("P/", "Project-1", "P/",
				"P2", "P/", autoStageMoves);
	}

	@Theory
	public void testMoveProjectWithinGitOutsideWorkspaceRepoMoveAtSameTopLevel(
			boolean autoStageMoves)
			throws Exception {
		dotestMoveProjectWithinRepoOutsideWorkspace("", "Project-1", "", "P2",
				"", autoStageMoves);
	}

	@Theory
	public void testMoveProjectWithinGitOutsideWorkspaceRepoMoveFromTopOneLevelDown(
			boolean autoStageMoves)
			throws Exception {
		dotestMoveProjectWithinRepoOutsideWorkspace("", "Project-1", "X/", "P2",
				"", autoStageMoves);
	}

	@Theory
	public void testMoveProjectWithinGitOutsideWorkspaceRepoMoveFromOneLevelDownToTop(
			boolean autoStageMoves)
			throws Exception {
		dotestMoveProjectWithinRepoOutsideWorkspace("P/", "Project-1", "", "P2",
				"", autoStageMoves);
	}

	@Theory
	public void testMoveProjectWithinGitOutsideWorkspaceRepoMoveFromOneLevelDownToSameDepth(
			boolean autoStageMoves)
			throws Exception {
		dotestMoveProjectWithinRepoOutsideWorkspace("P/", "Project-1", "X/",
				"P2", "", autoStageMoves);
	}

	@Theory
	public void testMoveProjectWithinGitOutsideWorkspaceRepoMoveFromOneLevelDownOutsideTheRepo(
			boolean autoStageMoves)
			throws Exception {
		dotestMoveProjectWithinRepoOutsideWorkspace("P/", "Project-1", "P/",
				"P2", "P/", autoStageMoves);
	}


	@Theory
	public void testMoveProjectWithinGitRepoMoveFromLevelZeroDownOne(
			boolean autoStageMoves)
			throws Exception {
		// In this case we'd expect the project to move, but not the repository
		// TODO: Eclipse cannot do this even without the Git plugin either,
		// TODO: See Bug 307140)
		try {
			dotestMoveProjectWithinRepoWithinWorkspace("P/", "Project-1",
					"P/Project-1/", "P2", "P/Project-1/", autoStageMoves);
			if (!"true".equals(System.getProperty("egit.assume_307140_fixed")))
				fail("ResourceException expected, core functionality dangerously broken and therefore forbidden");
		} catch (CoreException e) {
			if ("true".equals(System.getProperty("egit.assume_307140_fixed")))
				throw e;
		}
	}

	@Theory
	public void testMoveFileWithConflictsShouldBeCanceled(
			boolean autoStageMoves) throws Exception {
		configureAutoStageMoves(autoStageMoves);

		TestProject project = initRepoInsideProjectInsideWorkspace();
		String filePath = "file.txt";
		IFile file = testUtils.addFileToProject(project.getProject(), filePath, "some text");

		Repository repo = testRepository.getRepository();
		DirCache index = repo.lockDirCache();
		DirCacheBuilder builder = index.builder();
		addUnmergedEntry(filePath, builder);
		builder.commit();

		try {
			file.move(new Path("destination.txt"), false, null);
			fail("Expected move of file with conflicts to fail.");
		} catch (CoreException e) {
			IStatus status = e.getStatus();
			assertNotNull(status);
			assertEquals(IStatus.WARNING, status.getSeverity());
		}

		assertTrue("File should still exist at old location", file.exists());
		DirCache indexAfter = repo.readDirCache();
		DirCacheEntry entry = indexAfter.getEntry(filePath);
		assertEquals("Expected entry to still be in non-zero (conflict) stage",
				DirCacheEntry.STAGE_1, entry.getStage());
	}

	@Theory
	public void testMoveFolderWithFileWithConflictsShouldBeCanceled(
			boolean autoStageMoves) throws Exception {
		configureAutoStageMoves(autoStageMoves);

		TestProject project = initRepoInsideProjectInsideWorkspace();
		String filePath = "folder/file.txt";
		IFile file = testUtils.addFileToProject(project.getProject(), filePath, "some text");

		Repository repo = testRepository.getRepository();
		DirCache index = repo.lockDirCache();
		DirCacheBuilder builder = index.builder();
		addUnmergedEntry(filePath, builder);
		builder.commit();

		try {
			project.getProject()
					.getFolder("folder")
					.move(project.getProject().getFolder("newfolder")
							.getFullPath(), false, null);
			fail("Expected move of folder with file with conflicts to fail.");
		} catch (CoreException e) {
			IStatus status = e.getStatus();
			assertNotNull(status);
			assertEquals(IStatus.WARNING, status.getSeverity());
		}

		assertTrue("File should still exist at old location", file.exists());
		DirCache indexAfter = repo.readDirCache();
		DirCacheEntry entry = indexAfter.getEntry(filePath);
		assertEquals("Expected entry to still be in non-zero (conflict) stage",
				DirCacheEntry.STAGE_1, entry.getStage());
	}

	private static void addUnmergedEntry(String filePath, DirCacheBuilder builder) {
		DirCacheEntry stage1 = new DirCacheEntry(filePath, DirCacheEntry.STAGE_1);
		DirCacheEntry stage2 = new DirCacheEntry(filePath, DirCacheEntry.STAGE_2);
		DirCacheEntry stage3 = new DirCacheEntry(filePath, DirCacheEntry.STAGE_3);
		stage1.setFileMode(FileMode.REGULAR_FILE);
		stage2.setFileMode(FileMode.REGULAR_FILE);
		stage3.setFileMode(FileMode.REGULAR_FILE);
		builder.add(stage1);
		builder.add(stage2);
		builder.add(stage3);
	}

	private void dotestMoveProjectWithinRepoWithinWorkspace(String srcParent,
			String srcProjectName, String dstParent, String dstProjecName,
			String gitDir, boolean autoStageMoves) throws CoreException,
			IOException, Exception,
			CorruptObjectException {
		dotestMoveProjectWithinRepo(srcParent, srcProjectName, dstParent,
				dstProjecName, gitDir, true, autoStageMoves);
	}

	private void dotestMoveProjectWithinRepoOutsideWorkspace(String srcParent,
			String srcProjectName, String dstParent, String dstProjecName,
			String gitDir, boolean autoStageMoves) throws CoreException,
			IOException, Exception,
			CorruptObjectException {
		dotestMoveProjectWithinRepo(srcParent, srcProjectName, dstParent,
				dstProjecName, gitDir, false, autoStageMoves);
	}

	private void dotestMoveProjectWithinRepo(String srcParent,
			String srcProjectName, String dstParent, String dstProjecName,
			String gitDir, boolean sourceInsideWs, boolean autoStageMoves)
			throws Exception {
		configureAutoStageMoves(autoStageMoves);
		String gdRelativeSrcParent = srcParent + srcProjectName + "/";
		if (gdRelativeSrcParent.startsWith(gitDir))
			gdRelativeSrcParent = gdRelativeSrcParent
					.substring(gitDir.length());
		testDirs.add(new File(dstParent));
		String gdRelativeDstParent = dstParent + dstProjecName + "/";
		if (gdRelativeDstParent.startsWith(gitDir))
			gdRelativeDstParent = gdRelativeDstParent
					.substring(gitDir.length());

		registerWorkspaceRelativeTestDirProject(srcParent, srcProjectName);
		registerWorkspaceRelativeTestDirProject(dstParent, dstProjecName);

		// Old cruft may be laying around
		TestProject project = initRepoAboveProject(srcParent, gitDir, sourceInsideWs);
		IProject project0 = project.getProject().getWorkspace().getRoot()
				.getProject(dstProjecName);
		project0.delete(true, null);

		testUtils.addFileToProject(project.getProject(), "file.txt",
				"some text");
		AddToIndexOperation addToIndexOperation = new AddToIndexOperation(
				new IResource[] { project.getProject().getFile("file.txt") });
		addToIndexOperation.execute(null);

		// Check condition before move
		DirCache dirCache = DirCache.read(repository.getIndexFile(),
				FS.DETECTED);
		assertNotNull(dirCache.getEntry(gdRelativeSrcParent + "file.txt"));
		ObjectId oldContentId = dirCache.getEntry(
				gdRelativeSrcParent + "file.txt").getObjectId();

		// Modify the content before the move, we want to see the staged content
		// as it was before the move in the index
		testUtils.changeContentOfFile(project.getProject(), project
				.getProject().getFile("file.txt"), "other text");
		IProjectDescription description = project.getProject().getDescription();
		description.setName(dstProjecName);
		if (sourceInsideWs)
			if (dstParent.length() > 0)
				description.setLocationURI(URIUtil.toURI(project.getProject()
						.getWorkspace().getRoot().getLocation()
						.append(dstParent + dstProjecName)));
			else
				description.setLocationURI(null);
		else
			description.setLocationURI(URIUtil.toURI(new Path(workspaceSupplement + "/" + dstParent + "/" + dstProjecName)));
		project.getProject().move(description,
				IResource.FORCE | IResource.SHALLOW, null);
		IProject project2 = project.getProject().getWorkspace().getRoot()
				.getProject(dstProjecName);
		assertTrue(project2.exists());
		assertNotNull(RepositoryMapping.getMapping(project2));

		// Check that our file exists on disk has a new location in the index
		dirCache.read();
		assertTrue(project2.getFile("file.txt").exists());
		if (autoStageMoves) {
			assertNotNull(dirCache.getEntry(gdRelativeDstParent + "file.txt"));
			// Same content in index as before the move, i.e. not same as on
			// disk
			assertEquals(oldContentId, dirCache
					.getEntry(gdRelativeDstParent + "file.txt").getObjectId());
		} else {
			assertNull(dirCache.getEntry(gdRelativeDstParent + "file.txt"));
		}
	}


	private void registerWorkspaceRelativeTestDirProject(String parent, String projName) {
		if ((parent != null) && !parent.equals(""))
			registerWorkspaceRelativeTestDir(parent);
		else
			registerWorkspaceRelativeTestDir(projName);
	}

	private void registerWorkspaceRelativeTestDir(String relativeDir) {
		if ((relativeDir != null) && !relativeDir.equals("")) {
			File d = new File(workspace, relativeDir);
			testDirs.add(d);
		}
	}
}
