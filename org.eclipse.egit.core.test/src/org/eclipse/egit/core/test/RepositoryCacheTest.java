/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Andre Dietisheim - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RepositoryCacheTest extends GitTestCase {

	private TestRepository testRepository;
	private Repository repository;
	private RepositoryCache cache;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		this.testRepository = new TestRepository(gitDir);
		this.repository = testRepository.getRepository();
		this.cache = Activator.getDefault().getRepositoryCache();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		repository = null;
		super.tearDown();
	}

	@Test
	public void shouldNotContainDeletedRepository() throws IOException {
		cache.lookupRepository(repository.getDirectory());
		assertThat(repository, isIn(cache.getAllRepositories()));
		FileUtils.delete(repository.getDirectory(), FileUtils.RECURSIVE);
		assertThat(repository, not(isIn(cache.getAllRepositories())));
	}

	@Test
	public void findsRepositoryForOpenProject() throws Exception {
		IFile a = testUtils.addFileToProject(project.getProject(),
				"folder1/a.txt", "a");
		assertEquals(repository, cache.getRepository(a));
	}

	@Test
	public void findsRepositoryForClosedProject() throws Exception {
		IFile a = testUtils.addFileToProject(project.getProject(),
				"folder1/a.txt", "a");
		project.getProject().close(null);
		assertEquals(repository, cache.getRepository(a));
	}

	@Test
	public void findsNestedRepositoryForClosedProject()
			throws Exception {
		IFile a = testUtils.addFileToProject(project.getProject(),
				"folder1/a.txt", "a");

		// now we create a second project2 in a nested repository2
		File workdir = project.createFolder("nested").getLocation().toFile();
		TestRepository repository2 = new TestRepository(new File(workdir,
				Constants.DOT_GIT));

		String projectName = "project2";
		IProject project2 = testUtils.createProjectInLocalFileSystem(workdir,
				projectName);
		IFile b = testUtils.addFileToProject(project2, "folder1/b.txt",
				"Hello world");
		repository2.connect(project2);

		project.getProject().close(null);
		project2.getProject().close(null);

		// assert that we don't get confused by nesting repositories
		assertEquals(repository, cache.getRepository(a));
		assertEquals(repository2.repository, cache.getRepository(b));
	}
}
