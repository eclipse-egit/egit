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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
