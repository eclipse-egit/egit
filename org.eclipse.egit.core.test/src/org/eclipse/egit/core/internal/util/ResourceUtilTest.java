/*******************************************************************************
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.util;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestProject;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * NB: most of the tests here will break after bug 476585 will be fixed in
 * Eclipse 4.6, since the Resources API will always return inner most project
 * per default.
 */
public class ResourceUtilTest extends GitTestCase {

	private Repository repository;

	@Before
	public void before() throws Exception {
		repository = FileRepositoryBuilder.create(gitDir);
		repository.create();
		connect(project.getProject());
	}

	@After
	public void after() throws Exception {
		repository.close();
		RepositoryCache.INSTANCE.clear();
		testUtils.deleteTempDirs();
	}

	@Test
	public void getResourceForLocationShouldReturnFile() throws Exception {
		IFile file = project.createFile("file", new byte[] {});
		IResource resource = ResourceUtil.getResourceForLocation(file.getLocation(), false);
		assertThat(resource, instanceOf(IFile.class));
	}

	@Test
	public void getResourceForLocationShouldReturnFolder() throws Exception {
		IFolder folder = project.createFolder("folder");
		IResource resource = ResourceUtil.getResourceForLocation(folder.getLocation(), false);
		assertThat(resource, instanceOf(IFolder.class));
	}

	@Test
	public void getResourceForLocationShouldReturnNullForInexistentFile() throws Exception {
		IPath location = project.getProject().getLocation().append("inexistent");
		IResource resource = ResourceUtil.getResourceForLocation(location, false);
		assertThat(resource, nullValue());
	}

	@Test
	public void getFileForLocationShouldReturnExistingFileInCaseOfNestedProject()
			throws Exception {
		TestProject nested = new TestProject(true, "Project-1/Project-2");
		connect(nested.getProject());
		IFile file = nested.createFile("a.txt", new byte[] {});
		IPath location = file.getLocation();

		IFile result = ResourceUtil.getFileForLocation(location, false);
		assertThat(result, notNullValue());
		assertTrue("Returned IFile should exist", result.exists());
		assertThat(result.getProject(), is(nested.getProject()));

		result = ResourceUtil.getFileForLocation(location, true);
		assertThat(result, notNullValue());
		assertTrue("Returned IFile should exist", result.exists());
		assertThat(result.getProject(), is(nested.getProject()));
	}

	@Test
	public void getFileForLocationShouldReturnExistingFileInCaseOfNestedNotClosedProject()
			throws Exception {
		TestProject nested = new TestProject(true, "Project-1/Project-2");
		connect(nested.getProject());
		TestProject nested2 = new TestProject(true,
				"Project-1/Project-2/Project-3");
		connect(nested2.getProject());
		IFile file = nested2.createFile("a.txt", new byte[] {});
		IPath location = file.getLocation();
		nested2.project.close(new NullProgressMonitor());
		IFile result = ResourceUtil.getFileForLocation(location, false);
		assertThat(result, notNullValue());
		assertTrue("Returned IFile should exist", result.exists());
		assertThat(result.getProject(), is(nested.getProject()));

		result = ResourceUtil.getFileForLocation(location, true);
		assertThat(result, notNullValue());
		assertTrue("Returned IFile should exist", result.exists());
		assertThat(result.getProject(), is(nested.getProject()));
	}

	@Test
	public void getFileForLocationShouldNotUseFilesWithoutRepositoryMapping()
			throws Exception {
		TestProject nested = new TestProject(true, "Project-1/Project-2");
		IFile file = nested.createFile("a.txt", new byte[] {});
		IPath location = file.getLocation();

		IFile result = ResourceUtil.getFileForLocation(location, false);
		assertThat(result, notNullValue());
		assertTrue("Returned IFile should exist", result.exists());
		assertThat(result.getProject(), is(project.getProject()));

		result = ResourceUtil.getFileForLocation(location, true);
		assertThat(result, notNullValue());
		assertTrue("Returned IFile should exist", result.exists());
		assertThat(result.getProject(), is(project.getProject()));

		connect(nested.getProject());

		result = ResourceUtil.getFileForLocation(location, false);
		assertThat(result, notNullValue());
		assertTrue("Returned IFile should exist", result.exists());
		assertThat(result.getProject(), is(nested.getProject()));

		result = ResourceUtil.getFileForLocation(location, true);
		assertThat(result, notNullValue());
		assertTrue("Returned IFile should exist", result.exists());
		assertThat(result.getProject(), is(nested.getProject()));
	}

	// A resource located inside a nested repository (e.g. a submodule) that is
	// not connected as a separate project must resolve to that inner
	// repository, not to the outer repository the containing project is mapped
	// to.
	@Test
	public void getRepositoryReturnsInnermostNestedRepository()
			throws Exception {
		IFolder sub = project.getProject().getFolder("sub");
		sub.create(true, true, null);
		File nestedWorkTree = sub.getLocation().toFile();
		// Use a git directory outside the working tree, so that no ".git"
		// resource appears in the workspace which would auto-create a mapping
		// for the nested repository (mimicking a submodule's gitlink that is
		// not connected as a project).
		File nestedGitDir = testUtils.createTempDir("nested.git");
		try (Repository nested = new FileRepositoryBuilder()
				.setGitDir(nestedGitDir).setWorkTree(nestedWorkTree).build()) {
			nested.create();
			StoredConfig config = nested.getConfig();
			config.setString(ConfigConstants.CONFIG_CORE_SECTION, null,
					ConfigConstants.CONFIG_KEY_WORKTREE,
					nestedWorkTree.getAbsolutePath());
			config.save();
		}
		RepositoryCache.INSTANCE.lookupRepository(nestedGitDir);

		IFile innerFile = project.getProject().getFile("sub/inner.txt");
		innerFile.create(new ByteArrayInputStream(new byte[] {}), true, null);
		IFile outerFile = project.createFile("outer.txt", new byte[] {});

		// Precondition: the project mapping resolves to the outer repository.
		RepositoryMapping mapping = RepositoryMapping.getMapping(innerFile);
		assertThat(mapping, notNullValue());
		assertThat(mapping.getRepository().getDirectory().getCanonicalFile(),
				is(gitDir.getCanonicalFile()));

		// The resource really belongs to the inner repository.
		Repository innerRepository = ResourceUtil.getRepository(innerFile);
		assertThat(innerRepository, notNullValue());
		assertThat(innerRepository.getDirectory().getCanonicalFile(),
				is(nestedGitDir.getCanonicalFile()));

		// A resource outside the nested working tree still resolves to the
		// outer repository.
		Repository outerRepository = ResourceUtil.getRepository(outerFile);
		assertThat(outerRepository, notNullValue());
		assertThat(outerRepository.getDirectory().getCanonicalFile(),
				is(gitDir.getCanonicalFile()));
	}

	private void connect(IProject p) throws CoreException {
		ConnectProviderOperation operation = new ConnectProviderOperation(p,
				gitDir);
		operation.execute(null);
	}
}
