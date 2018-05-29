/*******************************************************************************
 * Copyright (C) 2013, François Rey <eclipse.org_@_francois_._rey_._name>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    François Rey - First implementation as part of handling linked resources
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.IteratorService;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.team.core.RepositoryProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LinkedResourcesTest {

	TestUtils testUtils;

	File project1Dir;

	File project2Dir;

	File otherFolder;

	File rootFile;

	TestRepository repository1;

	TestRepository repository2;

	String project1Name = "project1";

	String project2Name = "project2";

	IProject project1;

	IProject project2;

	GitResourceDeltaTestHelper resourceDeltaTestHelper1;

	GitResourceDeltaTestHelper resourceDeltaTestHelper2;

	@Before
	public void setUp() throws Exception {
		testUtils = new TestUtils();
		// Create first repo and project
		File rootDir = testUtils.createTempDir("FirstRepository");
		project1 = testUtils.createProjectInLocalFileSystem(rootDir,
				project1Name);
		project1Dir = project1.getRawLocation().toFile();
		otherFolder = new File(rootDir, "other_folder");
		assertTrue(otherFolder.mkdirs());
		File otherFile = new File(otherFolder, "otherFile.txt");
		rootFile = new File(rootDir, "rootFile.txt");
		Files.write(otherFile.toPath(),
				Arrays.<String> asList("Hello", "otherFile"),
				Charset.defaultCharset());
		Files.write(rootFile.toPath(), Arrays.<String> asList("Hi", "rootFile"),
				Charset.defaultCharset());
		repository1 = new TestRepository(new File(rootDir,
				Constants.DOT_GIT));
		testUtils.addFileToProject(project1,
				"project1folder1/project1folder1file1.txt", "Hello world");
		repository1.connect(project1);
		repository1.trackAllFiles(project1);
		repository1.track(otherFile);
		repository1.track(rootFile);
		repository1.commit("Initial commit");
		// Create 2nd repo and project
		project2 = testUtils.createProjectInLocalFileSystem(project2Name);
		project2Dir = project2.getRawLocation().toFile();
		repository2 = new TestRepository(new File(project2Dir,
				Constants.DOT_GIT));
		testUtils.addFileToProject(project2,
				"project2folder1/project2folder1file1.txt", "Hello world");
		repository2.connect(project2);
		repository2.trackAllFiles(project2);
		repository2.commit("Initial commit");
		// Set up git delta listener
		resourceDeltaTestHelper1 = new GitResourceDeltaTestHelper(
				repository1.getRepository());
		resourceDeltaTestHelper1.setUp();
		resourceDeltaTestHelper2 = new GitResourceDeltaTestHelper(
				repository2.getRepository());
		resourceDeltaTestHelper2.setUp();
	}

	@After
	public void tearDown() throws Exception {
		if (resourceDeltaTestHelper1 != null)
			resourceDeltaTestHelper1.tearDown();
		if (resourceDeltaTestHelper2 != null)
			resourceDeltaTestHelper2.tearDown();
		project1.delete(true, null);
		project2.delete(true, null);
		project1 = null;
		project2 = null;
		repository1.dispose();
		repository2.dispose();
		repository1 = null;
		repository2 = null;
		testUtils.deleteTempDirs();
		testUtils = null;
		Activator.getDefault().getRepositoryCache().clear();
	}

	@Test
	public void testGitProviderCanHandleLinkedResources() throws Exception {
		GitProvider provider = (GitProvider) RepositoryProvider
				.getProvider(project1);
		assertTrue(provider.canHandleLinkedResourceURI());
	}

	@Test
	public void testLinkedResourcesIgnoredByGitResourceDeltaVisitor()
			throws Exception {
		// Create linked folder in project1 that points to project2
		IFolder folder = project1.getFolder("link2project2");
		folder.createLink(project2.getLocation(), 0, null);
		// Create linked file in project1 that points to a file in project2
		IFile file = project1.getFile("link2project2folder1file1.txt");
		file.createLink(
				project2.getFile("project2folder1/project2folder1file1.txt")
						.getLocation(), 0, null);
		// Make sure linked folder is refreshed
		folder.refreshLocal(IResource.DEPTH_INFINITE, null);
		project2.getFile("project2folder1/project2folder1file1.txt")
				.touch(null);

		// Links are written to the .project file
		resourceDeltaTestHelper1
				.assertChangedResources(new String[] { "/project1/.project" });


		// Changes to linked resources are reported against their repository
		resourceDeltaTestHelper2.assertChangedResources(new String[] {
				"/project2/project2folder1/project2folder1file1.txt" });
	}

	@Test
	public void testLinkTargetsInSameRepositoryNotIgnoredByGitResourceDeltaVisitor()
			throws Exception {
		IFile file = project1.getFile("link2rootFile");
		file.createLink(Path.fromOSString(rootFile.getAbsolutePath()), 0, null);
		IFolder folder = project1.getFolder("link2otherFolder");
		folder.createLink(Path.fromOSString(otherFolder.getAbsolutePath()), 0,
				null);
		project1.refreshLocal(IResource.DEPTH_INFINITE, null);
		project1.getFile("link2rootFile").touch(null);
		project1.getFile("link2otherFolder/otherFile.txt").touch(null);
		resourceDeltaTestHelper1.assertChangedResources(
				new String[] { "/project1/.project", "/project1/link2rootFile",
						"/project1/link2otherFolder/otherFile.txt" });
	}

	@Test
	public void testLinkedResourcesIgnoredByIteratorService() throws Exception {
		// Create linked folder in project1 that points to project2
		IFolder folder = project1.getFolder("link2project2");
		folder.createLink(project2.getLocation(),
				IResource.ALLOW_MISSING_LOCAL, null);
		// Linked resources are ignored when searching the container of a folder
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IContainer container = IteratorService.findContainer(root, folder
				.getRawLocation().makeAbsolute().toFile());
		assertTrue(project2.equals(container));
		// Also test when the only project left is the one linking to the folder
		repository2.disconnect(project2);
		container = IteratorService.findContainer(root, folder.getRawLocation()
				.makeAbsolute().toFile());
		assertNull(container);
	}

	@Test
	public void testLinkedResourcesIgnoredByContainerTreeIterator()
			throws Exception {
		// Create linked folder in project1 that points to project2
		IFolder folder = project1.getFolder("link2project2");
		folder.createLink(project2.getLocation(),
				IResource.ALLOW_MISSING_LOCAL, null);
		// Create linked file in project1 that points to a file in project2
		IFile file = project1.getFile("link2project2folder1file1.txt");
		file.createLink(
				project2.getFile("project2folder1/project2folder1file1.txt")
						.getLocation(), IResource.ALLOW_MISSING_LOCAL, null);
		// Test iterator
		WorkingTreeIterator iterator = IteratorService
				.createInitialIterator(repository1.repository);
		assertTrue(iterator instanceof FileTreeIterator);
		while (!iterator.eof()) {
			assertFalse(iterator.getEntryPathString().startsWith("link2"));
			iterator.next(1);
		}
	}
}
