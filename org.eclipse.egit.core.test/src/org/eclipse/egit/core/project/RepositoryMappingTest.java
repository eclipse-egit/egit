/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

public class RepositoryMappingTest extends GitTestCase {

	private Repository repository;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		TestRepository testRepo = new TestRepository(gitDir);
		testRepo.connect(project.project);
		repository = testRepo.getRepository();
	}

	@Test
	public void shouldReturnMappingForResourceInProject() throws Exception {
		IFile file = project.createFile("inproject.txt", new byte[] {});

		RepositoryMapping mapping = RepositoryMapping.getMapping(file);

		assertNotNull(mapping);
		assertEquals(repository, mapping.getRepository());
		assertEquals(project.getProject(), mapping.getContainer());
		assertEquals(project.getProject().getName() + "/inproject.txt", mapping.getRepoRelativePath(file));
	}

	@Test
	public void shouldNotReturnMappingForResourceOutsideOfProject() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IPath filePath = getWorkTreePath().append("outside.txt");
		IFile file = root.getFile(filePath);
		assertFalse(file.exists());

		RepositoryMapping mapping = RepositoryMapping.getMapping(file);

		assertNull(mapping);
	}

	@Test
	public void shouldReturnMappingForPathOutsideOfProject() {
		IPath filePath = getWorkTreePath().append("outside.txt");

		RepositoryMapping mapping = RepositoryMapping.getMapping(filePath);

		assertNotNull(mapping);
		assertEquals(repository, mapping.getRepository());
		assertEquals("outside.txt", mapping.getRepoRelativePath(filePath));
	}

	@Test
	public void shouldReturnMappingWhenPathIsRepository() {
		IPath workTreePath = getWorkTreePath();

		RepositoryMapping mapping = RepositoryMapping.getMapping(workTreePath);

		assertNotNull(mapping);
		assertEquals(repository, mapping.getRepository());
		assertEquals("", mapping.getRepoRelativePath(workTreePath));
	}

	@Test
	public void shouldNotReturnMappingWhenPathIsOutsideRepository() {
		IPath workTreePath = getWorkTreePath();

		assertNull(RepositoryMapping
				.getMapping(new Path("D:/some/made/up/path")));
		assertNull(RepositoryMapping.getMapping(new Path("/some/made/up/path")));
		assertNull(RepositoryMapping.getMapping(new Path(
				"/thereshouldnever/be/something/here")));

		if (workTreePath.getDevice() == null)
			assertNull(RepositoryMapping.getMapping(workTreePath
					.setDevice("C:")));
	}

	@Test
	public void shouldFindRepositoryMappingForRepository() {
		RepositoryMapping mapping = RepositoryMapping.findRepositoryMapping(repository);

		assertNotNull(mapping);
		assertEquals(repository, mapping.getRepository());
	}

	private IPath getWorkTreePath() {
		return new Path(repository.getWorkTree().getAbsolutePath());
	}
}
