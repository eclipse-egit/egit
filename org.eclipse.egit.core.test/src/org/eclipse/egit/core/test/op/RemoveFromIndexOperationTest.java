/*******************************************************************************
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.op.RemoveFromIndexOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestProject;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// based on AddOperationTest
public class RemoveFromIndexOperationTest extends GitTestCase {

	private TestRepository testRepo;

	private File gitDir2;
	private TestRepository testRepo2;
	private TestProject project2;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		gitDir = new File(project.getProject()
				.getLocationURI().getPath(), Constants.DOT_GIT);
		testRepo = new TestRepository(gitDir);
		testRepo.connect(project.getProject());
		testRepo.commit("initial commit");

		project2 = new TestProject(true, "Project-2");
		gitDir2 = new File(project2.getProject()
				.getLocationURI().getPath(), Constants.DOT_GIT);
		testRepo2 = new TestRepository(gitDir2);
		testRepo2.connect(project2.getProject());
		testRepo2.commit("initial commit repo 2");
	}

	@Override
	@After
	public void tearDown() throws Exception {
		testRepo.dispose();
		testRepo2.dispose();
		project2.dispose();
		if (gitDir2.exists())
			FileUtils.delete(gitDir2, FileUtils.RECURSIVE | FileUtils.RETRY);
		super.tearDown();
	}

	@Test
	public void shouldUnTrackFile() throws Exception {
		// given
		IFile file1 = createFileInRepo("a.txt");
		new AddToIndexOperation(asList(file1)).execute(null);

		// when
		new RemoveFromIndexOperation(Arrays.asList(file1.getLocation())).execute(null);

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
		new AddToIndexOperation(asList(file1, file2)).execute(null);

		// when
		new RemoveFromIndexOperation(asList(container.getLocation())).execute(null);

		// then
		assertTrue(testRepo.removedFromIndex(file1.getLocation().toPortableString()));
		assertTrue(testRepo.removedFromIndex(file2.getLocation().toPortableString()));
	}

	@Test
	public void shouldUnstageExistingFile() throws Exception {
		// given
		IFile file1 = createFileInRepo("a.txt");
		new AddToIndexOperation(asList(file1)).execute(null);

		testRepo.commit("first commit");

		Thread.sleep(1000);
		file1.setContents(
				new ByteArrayInputStream("other text".getBytes(project.project
						.getDefaultCharset())), 0, null);
		new AddToIndexOperation(asList(file1)).execute(null);

		// when
		new RemoveFromIndexOperation(asList(file1.getLocation())).execute(null);

		// then
		assertTrue(testRepo.removedFromIndex(file1.getLocation().toPortableString()));
	}

	@Test
	public void shouldUnstageFilesInFolder() throws Exception {
		// given
		IFile file1 = createFileInRepo("sub/a.txt");
		IFile file2 = createFileInRepo("sub/b.txt");
		IFolder container = project.getProject().getFolder("sub");
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
		new RemoveFromIndexOperation(asList(container.getLocation())).execute(null);

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
		new RemoveFromIndexOperation(asList(container.getLocation())).execute(null);

		// then
		assertTrue(testRepo.removedFromIndex(file1.getLocation().toPortableString()));
		assertTrue(testRepo.removedFromIndex(file2.getLocation().toPortableString()));
	}

	@Test
	public void shouldUnstageFilesFromMultipleRepositories() throws Exception {
		// given
		IFile fileRepo1 = testUtils.addFileToProject(project.getProject(), "1.txt", "content");
		IFile fileRepo2 = testUtils.addFileToProject(project2.getProject(), "2.txt", "content");
		new AddToIndexOperation(asList(fileRepo1)).execute(null);
		new AddToIndexOperation(asList(fileRepo2)).execute(null);

		// when
		new RemoveFromIndexOperation(Arrays.asList(fileRepo1.getLocation(), fileRepo2.getLocation())).execute(null);

		// then
		assertTrue(testRepo.removedFromIndex(fileRepo1.getLocation()
				.toPortableString()));
		assertTrue(testRepo.removedFromIndex(fileRepo2.getLocation()
				.toPortableString()));
	}

	@Test
	public void shouldRemoveFromIndexOnInitialCommit() throws Exception {
		testRepo.dispose();
		FileUtils.delete(gitDir, FileUtils.RECURSIVE | FileUtils.RETRY);
		testRepo = new TestRepository(gitDir);
		testRepo.connect(project.getProject());

		IFile file = testUtils.addFileToProject(project.getProject(), "file.txt", "content");
		new AddToIndexOperation(asList(file)).execute(null);

		assertTrue(testRepo.inIndex(file.getLocation().toString()));

		new RemoveFromIndexOperation(Arrays.asList(file.getLocation())).execute(null);

		assertFalse(testRepo.inIndex(file.getLocation().toString()));
		assertTrue(file.getLocation().toFile().exists());
	}

	private IFile createFileInRepo(String fileName) throws Exception {
		return testUtils.addFileToProject(project.getProject(), fileName,
				"some text");
	}

}
