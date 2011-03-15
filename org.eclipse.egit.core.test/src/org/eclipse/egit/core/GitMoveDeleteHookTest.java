package org.eclipse.egit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.test.TestProject;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.egit.core.test.TestUtils;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.junit.MockSystemReader;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.SystemReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * All sorts of interesting cases
 */
public class GitMoveDeleteHookTest  {

	TestUtils testUtils = new TestUtils();

	TestRepository testRepository;

	Repository repository;

	List<File> gitDirs = new ArrayList<File>();

	@Before
	public void setUp() throws Exception {
		Activator.getDefault().getRepositoryCache().clear();
		MockSystemReader mockSystemReader = new MockSystemReader();
		SystemReader.setInstance(mockSystemReader);
		mockSystemReader.setProperty(Constants.GIT_CEILING_DIRECTORIES_KEY,
				ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile()
						.getAbsoluteFile()/*.getParent()*/.toString());
	}

	@After
	public void tearDown() throws IOException {
		testRepository.dispose();
		repository = null;
		for (File d : gitDirs)
			if (d.exists())
				FileUtils.delete(d, FileUtils.RECURSIVE | FileUtils.RETRY);
	}

	private TestProject initRepoInsideProject() throws IOException, CoreException {
		TestProject project = new TestProject(true, "Project-1");
		File gitDir = new File(project.getProject().getLocationURI().getPath(),
				Constants.DOT_GIT);
		gitDirs.add(gitDir);
		testRepository = new TestRepository(gitDir);
		repository = testRepository.getRepository();
		testRepository.connect(project.getProject());
		return project;
	}

	private TestProject initRepoAboveProject(String srcParent,String d) throws IOException, CoreException {
		TestProject project = new TestProject(true, srcParent + "Project-1");
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		File gd = URIUtil.toPath(root.getRawLocationURI()).append(d).toFile();

		File gitDir = new File(gd, Constants.DOT_GIT);
		gitDirs.add(gitDir);
		testRepository = new TestRepository(gitDir);
		repository = testRepository.getRepository();
		testRepository.connect(project.getProject());
		return project;
	}

	@Test
	public void testDeleteFile() throws Exception {
		TestProject project = initRepoInsideProject();
		testUtils.addFileToProject(project.getProject(), "file.txt",
				"some text");
		testUtils.addFileToProject(project.getProject(), "file2.txt",
		"some  more text");
		AddToIndexOperation addToIndexOperation = new AddToIndexOperation(
				new IResource[] {
						project.getProject().getFile("file.txt"),
						project.getProject().getFile("file2.txt")
						});
		addToIndexOperation.execute(null);

		// Validate pre-conditions
		DirCache dirCache = DirCache.read(repository.getIndexFile(),
				FS.DETECTED);
		assertEquals(2, dirCache.getEntryCount());
		assertNotNull(dirCache.getEntry("file.txt"));
		assertNotNull(dirCache.getEntry("file2.txt"));
		// Modify the content before the move
		testUtils.changeContentOfFile(project.getProject(), project
				.getProject().getFile("file.txt"), "other text");
		project.getProject()
				.getFile("file.txt")
				.delete(true, null);

		// Check index for the deleted file
		dirCache.read();
		assertEquals(1, dirCache.getEntryCount());
		assertNull(dirCache.getEntry("file.txt"));
		assertNotNull(dirCache.getEntry("file2.txt"));
		// Actual file is deleted
		assertFalse(project.getProject().getFile("file.txt").exists());
		// But a non-affected file remains
		assertTrue(project.getProject().getFile("file2.txt").exists());
	}

	@Test
	public void testDeleteFolder() throws Exception {
		TestProject project = initRepoInsideProject();
		testUtils.addFileToProject(project.getProject(), "folder/file.txt",
				"some text");
		testUtils.addFileToProject(project.getProject(), "folder2/file.txt",
		"some other text");
		AddToIndexOperation addToIndexOperation = new AddToIndexOperation(
				new IResource[] {
						project.getProject().getFile("folder/file.txt"),
						project.getProject().getFile("folder2/file.txt")
						});
		addToIndexOperation.execute(null);

		DirCache dirCache = DirCache.read(repository.getIndexFile(),
				FS.DETECTED);
		assertNotNull(dirCache.getEntry("folder/file.txt"));
		assertNotNull(dirCache.getEntry("folder2/file.txt"));
		// Modify the content before the move
		testUtils.changeContentOfFile(project.getProject(), project
				.getProject().getFile("folder/file.txt"), "other text");
		project.getProject()
				.getFolder("folder")
				.delete(true, null);

		dirCache.read();
		// Unlike delete file, dircache is untouched... pretty illogical
		assertNotNull(dirCache.getEntry("folder/file.txt"));
		// Not moved file still there
		assertNotNull(dirCache.getEntry("folder2/file.txt"));
	}

	@Test(expected=CoreException.class)
	public void testDeleteProject() throws Exception {
		TestProject project = initRepoAboveProject("P", "Project-1");
		IProject project0 = project.getProject().getWorkspace().getRoot().getProject("P2");
		project0.delete(true, null);

		testUtils.addFileToProject(project.getProject(), "file.txt",
				"some text");
		AddToIndexOperation addToIndexOperation = new AddToIndexOperation(
				new IResource[] { project.getProject().getFile("file.txt") });
		addToIndexOperation.execute(null);

		project.getProject().delete(true, null);
		fail();
	}

	@Test
	public void testMoveFile() throws Exception {
		TestProject project = initRepoInsideProject();
		testUtils.addFileToProject(project.getProject(), "file.txt",
				"some text");
		testUtils.addFileToProject(project.getProject(), "file2.txt",
		"some  more text");
		AddToIndexOperation addToIndexOperation = new AddToIndexOperation(
				new IResource[] {
						project.getProject().getFile("file.txt"),
						project.getProject().getFile("file2.txt")
						});
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
		assertNotNull(dirCache.getEntry("data.txt"));
		// Same content in index as before the move
		assertEquals(oldContentId, dirCache.getEntry("data.txt").getObjectId());

		// Not moved file still in its old place
		assertNotNull(dirCache.getEntry("file2.txt"));
	}

	/**
	 * Rename "folder" to "dir".
	 * @throws Exception
	 */
	@Test
	public void testMoveFolder() throws Exception {
		TestProject project = initRepoInsideProject();
		testUtils.addFileToProject(project.getProject(), "folder/file.txt",
				"some text");
		testUtils.addFileToProject(project.getProject(), "folder2/file.txt",
		"some other text");
		AddToIndexOperation addToIndexOperation = new AddToIndexOperation(
				new IResource[] {
						project.getProject().getFile("folder/file.txt"),
						project.getProject().getFile("folder2/file.txt")
						});
		addToIndexOperation.execute(null);

		DirCache dirCache = DirCache.read(repository.getIndexFile(),
				FS.DETECTED);
		assertNotNull(dirCache.getEntry("folder/file.txt"));
		assertNotNull(dirCache.getEntry("folder2/file.txt"));
		assertNull(dirCache.getEntry("dir/file.txt"));
		assertFalse(project.getProject().getFile("dir/file.txt").exists());
		ObjectId oldContentId = dirCache.getEntry("folder/file.txt").getObjectId();
		// Modify the content before the move
		testUtils.changeContentOfFile(project.getProject(), project
				.getProject().getFile("folder/file.txt"), "other text");
		project.getProject()
				.getFolder("folder")
				.move(project.getProject().getFolder("dir").getFullPath(),
						false, null);

		dirCache.read();
		assertTrue(project.getProject().getFile("dir/file.txt").exists());
		assertNull(dirCache.getEntry("folder/file.txt"));
		assertNotNull(dirCache.getEntry("dir/file.txt"));
		// Same content in index as before the move
		assertEquals(oldContentId, dirCache.getEntry("dir/file.txt").getObjectId());
		// Not moved file still there
		assertNotNull(dirCache.getEntry("folder2/file.txt"));
	}

	/**
	 * Test a simple rename of a project.
	 * The project contains a Git repository, which will also be moved.
	 *
	 * FIXME <em>THIS DOES NOT WORK YEY</em<
	 *
	 * @throws Exception
	 */
	@Test(expected=ResourceException.class)
	public void testMoveProjectContainingGitRepo() throws Exception {
		TestProject project = initRepoInsideProject();
		testUtils.addFileToProject(project.getProject(), "file.txt",
				"some text");
		AddToIndexOperation addToIndexOperation = new AddToIndexOperation(
				new IResource[] { project.getProject().getFile("file.txt") });
		addToIndexOperation.execute(null);
		IProjectDescription description = project.getProject().getDescription();
		description.setName("P2");
		project.getProject().move(description, IResource.FORCE | IResource.SHALLOW, null);
		assertNotNull(RepositoryMapping.getMapping(project.getProject()));
		assertEquals("P2", RepositoryMapping.getMapping(project.getProject()).getRepository().getDirectory().getParentFile().getName());
		fail();
	}

	@Test
	public void testMoveProjectWithinGitRepoMoveAtSameTopLevel() throws Exception {
		dotestMoveProjectWithinRepo("","Project-1", "", "P2", "");
	}

	@Test
	public void testMoveProjectWithinGitRepoMoveFromTopOneLevelDown() throws Exception {
		dotestMoveProjectWithinRepo("","Project-1", "X/", "P2", "");
	}

	@Test
	public void testMoveProjectWithinGitRepoMoveFromOneLevelDownToTop() throws Exception {
		dotestMoveProjectWithinRepo("P/","Project-1", "", "P2", "");
	}

	@Test
	public void testMoveProjectWithinGitRepoMoveFromOneLevelDownToSameDepth() throws Exception {
		dotestMoveProjectWithinRepo("P/","Project-1", "X/", "P2", "");
	}

	@Test
	public void testMoveProjectWithinGitRepoMoveFromOneLevelDownOutsideTheRepo() throws Exception {
		dotestMoveProjectWithinRepo("P/","Project-1", "P/", "P2", "P/");
	}

	@Test(expected=ResourceException.class)
	//	TODO: Eclipse cannot do this without the Git plugin either, Bug 339814")
	public void testMoveProjectWithinGitRepoMoveFromLevelZeroDownOne() throws Exception {
		// In this case we'd expect the project to move, but not the repository
		dotestMoveProjectWithinRepo("P/","Project-1", "P/Project-1/", "P2", "P/Project-1/");
	}

	private void dotestMoveProjectWithinRepo(String srcParent, String srcProjectName, String dstParent, String dstProjecName, String gitDir) throws CoreException,
			IOException, Exception, CorruptObjectException {

		String gdRelativeSrcParent = srcParent + srcProjectName + "/";
		if (gdRelativeSrcParent.startsWith(gitDir))
			gdRelativeSrcParent = gdRelativeSrcParent.substring(gitDir.length());
		String gdRelativeDstParent = dstParent + dstProjecName + "/";
		if (gdRelativeDstParent.startsWith(gitDir))
			gdRelativeDstParent = gdRelativeDstParent.substring(gitDir.length());

		// Old cruft may be laying around
		TestProject project = initRepoAboveProject(srcParent,gitDir);
		IProject project0 = project.getProject().getWorkspace().getRoot().getProject("P2");
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
		ObjectId oldContentId = dirCache.getEntry(gdRelativeSrcParent + "file.txt").getObjectId();

		// Modify the content before the move, we want to see the staged content as it was before the move in the index
		testUtils.changeContentOfFile(project.getProject(), project
				.getProject().getFile("file.txt"), "other text");
		IProjectDescription description = project.getProject().getDescription();
		description.setName(dstProjecName);
		if (dstParent.length() > 0)
			description.setLocationURI(URIUtil.toURI(project.getProject().getWorkspace().getRoot().getLocation().append(dstParent + dstProjecName)));
		else
			description.setLocationURI(null);
		project.getProject().move(description, IResource.FORCE | IResource.SHALLOW, null);
		IProject project2 = project.getProject().getWorkspace().getRoot().getProject("P2");
		assertTrue(project2.exists());
		assertNotNull(RepositoryMapping.getMapping(project2));

		dirCache.read();
		assertTrue(project2.getFile("file.txt").exists());
		assertNotNull(dirCache.getEntry(gdRelativeDstParent + "file.txt"));
		// Same content in index as before the move
		assertEquals(oldContentId, dirCache.getEntry(gdRelativeDstParent + "file.txt").getObjectId());

	}
}
