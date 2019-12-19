/*******************************************************************************
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AddOperationTest extends GitTestCase {

	private List<IResource> resources = new ArrayList<>();

	TestRepository testRepository;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		gitDir = new File(project.getProject()
				.getLocationURI().getPath(), Constants.DOT_GIT);
		testRepository = new TestRepository(gitDir);
		testRepository.connect(project.getProject());
	}

	@Override
	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		super.tearDown();
	}

	@Test
	public void testTrackFile() throws Exception {
		IFile file1 = testUtils.addFileToProject(project.getProject(), "a.txt",
				"some text");

		assertFalse(testRepository.inIndex(file1.getLocation()
				.toPortableString()));

		resources.add(file1);
		new AddToIndexOperation(resources).execute(null);

		assertTrue(testRepository.inIndex(file1.getLocation()
				.toPortableString()));
	}

	@Test
	public void testTrackFilesInFolder() throws Exception {
		IFile file1 = testUtils.addFileToProject(project.getProject(),
				"sub/a.txt", "some text");
		IFile file2 = testUtils.addFileToProject(project.getProject(),
				"sub/b.txt", "some text");

		assertFalse(testRepository.inIndex(file1.getLocation()
				.toPortableString()));
		assertFalse(testRepository.inIndex(file2.getLocation()
				.toPortableString()));

		resources.add(project.getProject().getFolder("sub"));
		new AddToIndexOperation(resources).execute(null);

		assertTrue(testRepository.inIndex(file1.getLocation()
				.toPortableString()));
		assertTrue(testRepository.inIndex(file2.getLocation()
				.toPortableString()));
	}

	@Test
	public void testAddFile() throws Exception {
		IFile file1 = testUtils.addFileToProject(project.getProject(), "a.txt",
				"some text");

		resources.add(file1);
		new AddToIndexOperation(resources).execute(null);

		testRepository.commit("first commit");

		assertEquals(file1.getLocalTimeStamp() / 1000,
				testRepository.lastModifiedInIndex(
						file1.getLocation().toPortableString()) / 1000);

		Thread.sleep(1000);
		file1.setContents(
				new ByteArrayInputStream("other text".getBytes(project.project
						.getDefaultCharset())), 0, null);

		assertFalse(file1.getLocalTimeStamp() / 1000 == testRepository
				.lastModifiedInIndex(file1.getLocation().toPortableString())
				/ 1000);

		new AddToIndexOperation(resources).execute(null);

		assertTrue(testRepository.inIndex(file1.getLocation()
				.toPortableString()));
		// does not work yet due to the racy git problem: DirCache.writeTo
		// smudges the
		// timestamp of an added file
		assertEquals(file1.getLocalTimeStamp() / 1000,
				testRepository.lastModifiedInIndex(
						file1.getLocation().toPortableString()) / 1000);
	}

	@Test
	public void testAddFilesInFolder() throws Exception {
		IFile file1 = testUtils.addFileToProject(project.getProject(),
				"sub/a.txt", "some text");
		IFile file2 = testUtils.addFileToProject(project.getProject(),
				"sub/b.txt", "some text");

		resources.add(project.getProject().getFolder("sub"));
		new AddToIndexOperation(resources).execute(null);

		testRepository.commit("first commit");

		assertEquals(file1.getLocalTimeStamp() / 1000,
				testRepository.lastModifiedInIndex(file1.getLocation()
						.toPortableString()) / 1000);

		Thread.sleep(1000);

		file1.setContents(
				new ByteArrayInputStream("other text".getBytes(project.project
						.getDefaultCharset())), 0, null);
		file2.setContents(
				new ByteArrayInputStream("other text".getBytes(project.project
						.getDefaultCharset())), 0, null);

		assertFalse(file1.getLocalTimeStamp() == testRepository
				.lastModifiedInIndex(file1.getLocation().toPortableString()));
		assertFalse(file2.getLocalTimeStamp() == testRepository
				.lastModifiedInIndex(file1.getLocation().toPortableString()));

		new AddToIndexOperation(resources).execute(null);

		assertTrue(testRepository.inIndex(file1.getLocation()
				.toPortableString()));
		assertTrue(testRepository.inIndex(file2.getLocation()
				.toPortableString()));

		assertEquals(file1.getLocalTimeStamp() / 1000,
				testRepository.lastModifiedInIndex(
						file1.getLocation().toPortableString()) / 1000);
		assertEquals(file2.getLocalTimeStamp() / 1000,
				testRepository.lastModifiedInIndex(
						file2.getLocation().toPortableString()) / 1000);
	}
}
