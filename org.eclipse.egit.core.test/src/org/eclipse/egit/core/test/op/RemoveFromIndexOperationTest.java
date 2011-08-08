/*******************************************************************************
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.op.RemoveFromIndexOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// based on AddOperationTest
public class RemoveFromIndexOperationTest extends GitTestCase {

	TestRepository testRepo;
	private Repository repo;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		gitDir = new File(project.getProject()
				.getLocationURI().getPath(), Constants.DOT_GIT);
		testRepo = new TestRepository(gitDir);
		testRepo.connect(project.getProject());
		testRepo.commit("initial commit");

		repo = testRepo.getRepository();
	}

	@After
	public void tearDown() throws Exception {
		testRepo.dispose();
		super.tearDown();
	}

	@Test
	public void shouldUnTrackFile() throws Exception {
		// given
		IFile file1 = createFileInRepo("a.txt");
		IResource[] resources = new IResource[] { file1 };
		new AddToIndexOperation(asList(file1)).execute(null);

		// when
		new RemoveFromIndexOperation(repo, resources).execute(null);

		// then
		assertTrue(testRepo.removedFromIndex(file1.getLocation()
				.toPortableString()));
	}

	@Test
	public void shouldUnTrackFilesInFolder() throws Exception {
		// given
		IFile file1 = createFileInRepo("sub/a.txt");
		IFile file2 = createFileInRepo("sub/b.txt");
		IFolder container = project.getProject().getFolder("sub");
		IResource[] resources = new IResource[] { container };
		new AddToIndexOperation(asList(file1, file2)).execute(null);

		// when
		new RemoveFromIndexOperation(repo, resources).execute(null);

		// then
		assertTrue(testRepo.removedFromIndex(file1.getLocation().toPortableString()));
		assertTrue(testRepo.removedFromIndex(file2.getLocation().toPortableString()));
	}

	@Test
	public void shouldUnstExistingFile() throws Exception {
		// given
		IFile file1 = createFileInRepo("a.txt");
		IResource[] resources = new IResource[] { file1 };
		new AddToIndexOperation(asList(file1)).execute(null);

		testRepo.commit("first commit");

		Thread.sleep(1000);
		file1.setContents(
				new ByteArrayInputStream("other text".getBytes(project.project
						.getDefaultCharset())), 0, null);
		new AddToIndexOperation(asList(file1)).execute(null);

		// when
		new RemoveFromIndexOperation(repo, resources).execute(null);

		// then
		assertTrue(testRepo.removedFromIndex(file1.getLocation().toPortableString()));
	}

	@Test
	public void shouldUnstageFilesInFolder() throws Exception {
		// given
		IFile file1 = createFileInRepo("sub/a.txt");
		IFile file2 = createFileInRepo("sub/b.txt");
		IFolder container = project.getProject().getFolder("sub");
		IResource[] resources = new IResource[] { container };
		List<IFolder> addResources = asList(project.getProject().getFolder("sub"));
		new AddToIndexOperation(addResources).execute(null);

		testRepo.commit("first commit");

		Thread.sleep(1000);

		file1.setContents(
				new ByteArrayInputStream("other text".getBytes(project.project
						.getDefaultCharset())), 0, null);
		file2.setContents(
				new ByteArrayInputStream("other text".getBytes(project.project
						.getDefaultCharset())), 0, null);
		new AddToIndexOperation(addResources).execute(null);

		// when
		new RemoveFromIndexOperation(repo, resources).execute(null);

		// then
		assertTrue(testRepo.removedFromIndex(file1.getLocation().toPortableString()));
		assertTrue(testRepo.removedFromIndex(file2.getLocation().toPortableString()));
	}

	@Test
	public void shouldUnstageFilesInNestedFolder() throws Exception {
		// given
		IFile file1 = createFileInRepo("sub/next/a.txt");
		IFile file2 = createFileInRepo("sub/next/b.txt");
		IFolder container = project.getProject().getFolder("sub");
		IResource[] resources = new IResource[] { container };
		List<IFolder> addResources = asList(project.getProject().getFolder("sub"));
		new AddToIndexOperation(addResources).execute(null);

		testRepo.commit("first commit");

		Thread.sleep(1000);

		file1.setContents(
				new ByteArrayInputStream("other text".getBytes(project.project
						.getDefaultCharset())), 0, null);
		file2.setContents(
				new ByteArrayInputStream("other text".getBytes(project.project
						.getDefaultCharset())), 0, null);
		new AddToIndexOperation(addResources).execute(null);

		// when
		new RemoveFromIndexOperation(repo, resources).execute(null);

		// then
		assertTrue(testRepo.removedFromIndex(file1.getLocation().toPortableString()));
		assertTrue(testRepo.removedFromIndex(file2.getLocation().toPortableString()));
	}

	private IFile createFileInRepo(String fileName) throws Exception {
		return testUtils.addFileToProject(project.getProject(), fileName,
				"some text");
	}

}
