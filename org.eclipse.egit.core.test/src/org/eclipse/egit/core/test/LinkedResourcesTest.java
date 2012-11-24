/*******************************************************************************
 * Copyright (C) 2012, François Rey <eclipse.org_@_francois_._rey_._name>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    François Rey - First implementation as part of handling linked resources
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.ContainerTreeIterator;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.IteratorService;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.team.core.RepositoryProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LinkedResourcesTest {

	TestUtils testUtils;
	File project1Dir;
	File project2Dir;
	TestRepository repository1;
	TestRepository repository2;
	String project1Name = "project1";
	String project2Name = "project2";
	IProject project1;
	IProject project2;
	GitResourceDeltaTestHelper resourceDeltaTestHelper;


	@Before
	public void setUp() throws Exception {
		testUtils = new TestUtils();
		// Create first repo and project
		project1 = testUtils.createProjectInLocalFileSystem(project1Name);
		project1Dir = project1.getRawLocation().toFile();
		repository1 = new TestRepository(new File(project1Dir, Constants.DOT_GIT));
		testUtils.addFileToProject(project1,
				"project1folder1/project1folder1file1.txt", "Hello world");
		repository1.connect(project1);
		repository1.trackAllFiles(project1);
		repository1.commit("Initial commit");
		// Create 2nd repo and project
		project2 = testUtils.createProjectInLocalFileSystem(project2Name);
		project2Dir = project2.getRawLocation().toFile();
		repository2 = new TestRepository(new File(project2Dir, Constants.DOT_GIT));
		testUtils.addFileToProject(project2,
				"project2folder1/project2folder1file1.txt", "Hello world");
		repository2.connect(project2);
		repository2.trackAllFiles(project2);
		repository2.commit("Initial commit");
		// Set up git delta listener
		resourceDeltaTestHelper = new GitResourceDeltaTestHelper();
		resourceDeltaTestHelper.setUp();
	}

	@After
	public void tearDown() throws Exception {
		if (resourceDeltaTestHelper!=null)
			resourceDeltaTestHelper.tearDown();
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
	public void testGitProviderCanHandleLinkedResources()
			throws Exception {
		GitProvider provider = (GitProvider)RepositoryProvider.getProvider(project1);
		assertTrue(provider.canHandleLinkedResourceURI());
	}

	@Test
	public void testLinkedResourcesIgnoredByGitResourceDeltaVisitor()
			throws Exception {
		// Create linked folder in project1 that points to project2
		IFolder folder = project1.getFolder("link2project2");
		folder.createLink(project2.getLocation(), IResource.ALLOW_MISSING_LOCAL, null);
		// Create linked file in project1 that points to a file in project2
		IFile file = project1.getFile("link2project2folder1file1.txt");
		file.createLink(
			project2.getFile("project2folder1/project2folder1file1.txt").getLocation(),
			IResource.ALLOW_MISSING_LOCAL, null);
		// Add file to project2
		testUtils.addFileToProject(project2, "project2folder1/project2folder1file2.txt", "Hello world");
		//resourceDeltaTestHelper.printChangedResources();
		resourceDeltaTestHelper.assertChangedResources(new String[] {
				"/project1/.project", // Links are written to project file
				"/project2/project2folder1/project2folder1file2.txt"
				});
	}

	@Test
	public void testLinkedResourcesIgnoredByIteratorService()
			throws Exception {
		// Create linked folder in project1 that points to project2
		IFolder folder = project1.getFolder("link2project2");
		folder.createLink(project2.getLocation(), IResource.ALLOW_MISSING_LOCAL, null);
		// Linked resources are ignored when searching the container of a folder
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IContainer container = IteratorService.findContainer(root, folder.getRawLocation().makeAbsolute().toFile());
		assertTrue(project2.equals(container));
		// Also test when the only project left is the one linking to the folder
		repository2.disconnect(project2);
		container = IteratorService.findContainer(root, folder.getRawLocation().makeAbsolute().toFile());
		assertNull(container);
	}

	@Test
	public void testLinkedResourcesIgnoredByContainerTreeIterator()
			throws Exception {
		// Create linked folder in project1 that points to project2
		IFolder folder = project1.getFolder("link2project2");
		folder.createLink(project2.getLocation(), IResource.ALLOW_MISSING_LOCAL, null);
		// Create linked file in project1 that points to a file in project2
		IFile file = project1.getFile("link2project2folder1file1.txt");
		file.createLink(
			project2.getFile("project2folder1/project2folder1file1.txt").getLocation(),
			IResource.ALLOW_MISSING_LOCAL, null);
		// Test iterator
		WorkingTreeIterator iterator = IteratorService.createInitialIterator(repository1.repository);
		assertTrue(iterator instanceof ContainerTreeIterator);
		while (!iterator.eof()) {
			assertFalse(iterator.getEntryPathString().startsWith("link2"));
			iterator.next(1);
		}

	}

}
