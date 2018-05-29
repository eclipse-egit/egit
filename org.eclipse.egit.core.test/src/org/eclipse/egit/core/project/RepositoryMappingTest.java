/*******************************************************************************
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
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

	@Test
	public void shouldResolveRelativePathRelativeToContainer() {
		IPath projectPath = project.getProject().getLocation();
		RepositoryMapping mapping = RepositoryMapping
				.create(project.getProject(), new File(".git"));
		assertEquals(projectPath.append(".git"),
				mapping.getGitDirAbsolutePath());
	}

	/**
	 * Tests that a {@link RepositoryMapping} internally uses a relative path if
	 * at all possible.
	 */
	@Test
	public void shouldResolveAsRelativePath() {
		IProject proj = project.getProject();
		IPath projectPath = proj.getLocation()
				.removeTrailingSeparator();
		String gitHereTest = ".git";
		String gitTest = "../../.git";
		String gitSubdirTest = "foobar/.git";
		String gitSubmoduleTest = "../../.git/modules/submodule";
		// Construct an absolute path different from the project location:
		// should be preserved. upToSegment ensures we don't loose the root
		// component.
		String gitAbsolute = projectPath.uptoSegment(0)
				.append(projectPath.segment(0) + "fake").append(".git")
				.toPortableString();
		String parents = "";
		while (projectPath.segmentCount() > 2) {
			String pathString = projectPath.toOSString();
			assertRepoMappingPath(proj, pathString, gitHereTest, parents);
			assertRepoMappingPath(proj, pathString, gitTest, parents);
			assertRepoMappingPath(proj, pathString, gitSubdirTest, parents);
			assertRepoMappingPath(proj, pathString, gitSubmoduleTest, parents);
			assertRepoMappingPath(proj, pathString, gitAbsolute, "");
			projectPath = projectPath.removeLastSegments(1);
			parents += "../";
		}
	}

	private void assertRepoMappingPath(IProject testProject, String pathOS,
			String testDirPortable, String parentsPortable) {
		String testDirOS = testDirPortable.replace('/', File.separatorChar);
		File testFile = new File(testDirOS);
		if (!testFile.isAbsolute()) {
			testFile = new File(new File(pathOS), testDirOS);
		}
		RepositoryMapping mapping = RepositoryMapping.create(testProject,
				testFile);
		assertEquals(parentsPortable + testDirPortable,
				mapping.getGitDirPath().toPortableString());
	}

	private IPath getWorkTreePath() {
		return new Path(repository.getWorkTree().getAbsolutePath());
	}
}
