/*******************************************************************************
 * Copyright (C) 2012, Matthias Sohn <matthias.sohn@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestProject;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ProjectUtilTest extends GitTestCase {

	private TestRepository repository;

	private TestProject project2;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		repository = new TestRepository(gitDir);
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		if (project2 != null)
			project2.dispose();
	}

	@Test
	public void testGetValidOpenProjects() throws Exception {
		IProject[] projects = ProjectUtil.getValidOpenProjects(repository
				.getRepository());
		assertEquals(1, projects.length);
		IProject p = projects[0];
		assertEquals("Project-1", p.getDescription().getName());
	}

	@Test
	public void testGetProjectsContains() throws Exception {
		TestProject prj2 = new TestProject(true, "Project-1-sub");

		try {
			repository.createFile(project.getProject(), "xxx");
			repository.createFile(project.getProject(), "zzz");
			repository.createFile(prj2.getProject(), "zzz");

			project.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
			prj2.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);

			IProject[] projectsContaining = ProjectUtil.getProjectsContaining(
					repository.getRepository(),
					Collections.singleton("Project-1/xxx"));
			IProject[] projectsEmpty = ProjectUtil.getProjectsContaining(
					repository.getRepository(), Collections.singleton("yyy"));
			IProject[] projectSelf = ProjectUtil.getProjectsContaining(
					repository.getRepository(),
					Collections.singleton("Project-1"));
			Set<String> files = new TreeSet<String>();
			files.add("Project-1/xxx");
			files.add("Project-1/zzz");
			IProject[] multiFile = ProjectUtil.getProjectsContaining(
					repository.getRepository(), files);

			files.clear();
			files.add("Project-1/xxx");
			files.add("Project-1/dir/Project-1-sub/zzz");
			IProject[] multiProject = ProjectUtil.getProjectsContaining(
					repository.getRepository(), files);
			IProject[] nonExistProject = ProjectUtil.getProjectsContaining(
					repository.getRepository(),
					Collections.singleton("Project-2"));

			assertEquals(1, projectsContaining.length);
			assertEquals(0, projectsEmpty.length);
			assertEquals(1, projectSelf.length);
			assertEquals(1, multiFile.length);
			assertEquals(2, multiProject.length);
			assertEquals(0, nonExistProject.length);

			IProject p = projectsContaining[0];
			assertEquals("Project-1", p.getDescription().getName());
		} finally {
			prj2.dispose();
		}
	}

	@Test
	public void testGetNestedProjectsContains() throws Exception {
		TestProject prj2 = new TestProject(true, "Project-1/dir/Project-1-sub");

		try {
			repository.createFile(project.getProject(), "xxx");
			repository.createFile(project.getProject(), "zzz");
			repository.createFile(prj2.getProject(), "zzz");

			project.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
			prj2.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);

			IProject[] projectsContaining = ProjectUtil.getProjectsContaining(
					repository.getRepository(),
					Collections.singleton("Project-1/xxx"));
			IProject[] projectsEmpty = ProjectUtil.getProjectsContaining(
					repository.getRepository(), Collections.singleton("yyy"));
			IProject[] projectSelf = ProjectUtil.getProjectsContaining(
					repository.getRepository(),
					Collections.singleton("Project-1"));
			Set<String> files = new TreeSet<String>();
			files.add("Project-1/xxx");
			files.add("Project-1/zzz");
			IProject[] multiFile = ProjectUtil.getProjectsContaining(
					repository.getRepository(), files);

			files.clear();
			files.add("Project-1/dir/Project-1-sub/zzz");
			files.add("Project-1/xxx");
			IProject[] multiProject = ProjectUtil.getProjectsContaining(
					repository.getRepository(), files);
			IProject[] nonExistProject = ProjectUtil.getProjectsContaining(
					repository.getRepository(),
					Collections.singleton("Project-2"));

			assertEquals(1, projectsContaining.length);
			assertEquals(0, projectsEmpty.length);
			assertEquals(1, projectSelf.length);
			assertEquals(1, multiFile.length);
			assertEquals(2, multiProject.length);
			assertEquals(0, nonExistProject.length);

			IProject p = projectsContaining[0];
			assertEquals("Project-1", p.getDescription().getName());
		} finally {
			prj2.dispose();
		}
	}

	@Test
	public void testFindContainer() throws Exception {
		File tmp = new File("/tmp/file");
		File test1 = new File(project.getProject().getLocation().toFile(), "xxx");
		File test2 = new File(repository.getRepository().getWorkTree(), "xxx");

		assertNull(ProjectUtil.findContainer(tmp));
		assertEquals(project.getProject(), ProjectUtil.findContainer(test1));
		assertEquals(ResourcesPlugin.getWorkspace().getRoot(), ProjectUtil.findContainer(test2));
	}

	@Test
	public void testGetValidOpenProjectsClosedProject() throws Exception {
		project.getProject().close(new NullProgressMonitor());
		IProject[] projects = ProjectUtil.getValidOpenProjects(repository
				.getRepository());
		assertEquals(0, projects.length);
	}

	@Test
	public void testRefreshValidProjects() throws Exception {
		IProject p = mock(IProject.class);
		when(p.getLocation()).thenReturn(project.getProject().getLocation());
		IProject[] projects = new IProject[1];
		projects[0] = p;
		ProjectUtil.refreshValidProjects(projects, new NullProgressMonitor());
		verify(p).refreshLocal(eq(IResource.DEPTH_INFINITE),
				any(IProgressMonitor.class));
	}

	@Test
	public void testCloseMissingProject() throws Exception {
		IProject p = mock(IProject.class);
		File projectFile = project.getProject().getLocation()
				.append(".project").toFile();
		FileUtils.delete(projectFile);
		ProjectUtil.closeMissingProject(p, projectFile,
				new NullProgressMonitor());
		verify(p).close(any(IProgressMonitor.class));
	}

	@Test
	public void testRefreshResource() throws Exception {
		IResource r = mock(IResource.class);
		IResource[] resources = new IResource[1];
		resources[0] = r;
		ProjectUtil.refreshResources(resources, new NullProgressMonitor());
		verify(r).refreshLocal(eq(IResource.DEPTH_INFINITE),
				any(IProgressMonitor.class));
	}

	@Test
	public void testGetProjectsUnconnected() throws Exception {
		IProject[] projects = ProjectUtil.getProjects(repository
				.getRepository());
		assertEquals(0, projects.length);
	}

	@Test
	public void testGetProjects() throws Exception {
		ConnectProviderOperation operation = new ConnectProviderOperation(
				project.getProject(), gitDir);
		operation.execute(null);
		IProject[] projects = ProjectUtil.getProjects(repository
				.getRepository());
		assertEquals(1, projects.length);
		IProject p = projects[0];
		assertEquals("Project-1", p.getDescription().getName());
	}

	@Test
	public void testFindProjectFiles() {
		Collection<File> files = new ArrayList<File>();
		assertTrue(ProjectUtil.findProjectFiles(files, gitDir.getParentFile(),
				null, new NullProgressMonitor()));
	}

	@Test
	public void testFindProjectFilesNullDir() {
		Collection<File> files = new ArrayList<File>();
		assertFalse(ProjectUtil.findProjectFiles(files, null, null,
				new NullProgressMonitor()));
	}

	@Test
	public void testFindProjectFilesEmptyDir() throws Exception {
		Collection<File> files = new ArrayList<File>();
		File dir = new File(gitDir.getParentFile().getPath() + File.separator
				+ "xxx");
		FileUtils.mkdir(dir);
		assertFalse(ProjectUtil.findProjectFiles(files, dir, null,
				new NullProgressMonitor()));
	}

	@Test
	public void testFindProjectFilesNested() throws Exception {
		project2 = new TestProject(true, "Project-1/Project-Nested");
		File workingDir = gitDir.getParentFile();

		Collection<File> foundFiles = new ArrayList<File>();
		boolean found = ProjectUtil.findProjectFiles(foundFiles, workingDir,
				null, new NullProgressMonitor());

		assertTrue("Expected to find projects", found);
		assertThat(foundFiles, hasItem(new File(workingDir, "Project-1/.project")));
		assertThat(foundFiles, hasItem(new File(workingDir, "Project-1/Project-Nested/.project")));
	}
}
