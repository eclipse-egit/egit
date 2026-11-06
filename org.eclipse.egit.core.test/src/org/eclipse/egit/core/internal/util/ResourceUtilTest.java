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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Locale;

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.internal.util.ResourceUtil.ContainerLocationResolver;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestProject;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
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
	public void after() {
		repository.close();
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

	@Test
	public void containerLocationResolverShouldResolveLikeResourceUtil()
			throws Exception {
		project.createFolder("folder");
		project.createFolder("folder/sub");
		TestProject nested = new TestProject(true, "Project-1/Project-2");
		TestProject closed = null;
		try {
			connect(nested.getProject());
			nested.createFolder("inner");
			closed = new TestProject(true, "Project-1/Project-2/Project-3");
			connect(closed.getProject());
			closed.createFolder("x");
			closed.getProject().close(new NullProgressMonitor());

			IPath base = project.getProject().getLocation();
			ContainerLocationResolver resolver = new ContainerLocationResolver(
					base);
			IPath nestedLocation = nested.getProject().getLocation();
			IPath closedLocation = closed.getProject().getLocation();
			IPath[] locations = { base, base.append("folder"),
					base.append("folder/sub"), base.append("inexistent"),
					nestedLocation, nestedLocation.append("inner"),
					closedLocation, closedLocation.append("x") };
			for (IPath location : locations) {
				assertThat(location.toString(), resolver.getContainer(location),
						is(ResourceUtil.getContainerForLocation(location,
								false)));
			}
			assertThat(resolver.getContainer(nestedLocation.append("inner")),
					is(nested.getProject().getFolder("inner")));
			assertThat(resolver.getContainer(base.append("folder/sub")),
					is(project.getProject().getFolder("folder/sub")));
		} finally {
			// Connected projects must be gone before the repository is
			// deleted, or they keep it open
			if (closed != null) {
				closed.dispose();
			}
			nested.dispose();
		}
	}

	@Test
	public void containerLocationResolverShouldFindHandlesLikeWorkspaceRoot()
			throws Exception {
		project.createFolder("folder");
		project.createFile("folder/a.txt", new byte[] {});
		TestProject nested = new TestProject(true, "Project-1/Project-2");
		TestProject closed = null;
		try {
			nested.createFile("b.txt", new byte[] {});
			closed = new TestProject(true, "Project-1/Project-2/Project-3");
			closed.createFile("c.txt", new byte[] {});
			closed.getProject().close(new NullProgressMonitor());

			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IPath base = project.getProject().getLocation();
			ContainerLocationResolver resolver = new ContainerLocationResolver(
					base);
			IPath nestedLocation = nested.getProject().getLocation();
			IPath closedLocation = closed.getProject().getLocation();
			IPath[] locations = { base, base.append("folder"),
					base.append("folder/"), base.append("folder/a.txt"),
					base.append("inexistent"), base.append("inexistent/d.txt"),
					nestedLocation, nestedLocation.append("b.txt"),
					closedLocation, closedLocation.append("c.txt") };
			for (IPath location : locations) {
				assertThat(location.toString(),
						resolver.findContainer(location),
						is(root.getContainerForLocation(location)));
				assertThat(location.toString(), resolver.findFile(location),
						is(root.getFileForLocation(location)));
				String device = location.getDevice();
				if (device != null) {
					// Devices are compared ignoring case
					IPath other = location
							.setDevice(device.toLowerCase(Locale.ROOT));
					assertThat(other.toString(), resolver.findContainer(other),
							is(root.getContainerForLocation(other)));
					assertThat(other.toString(), resolver.findFile(other),
							is(root.getFileForLocation(other)));
				}
			}
			assertThat(resolver.findFile(nestedLocation.append("b.txt")),
					is(nested.getProject().getFile("b.txt")));
			assertThat(resolver.findFile(nestedLocation),
					is(project.getProject().getFile("Project-2")));
		} finally {
			if (closed != null) {
				closed.dispose();
			}
			nested.dispose();
		}
	}

	@Test
	public void containerLocationResolverShouldFindWorkspaceRoot()
			throws Exception {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IPath rootLocation = root.getLocation();
		ContainerLocationResolver resolver = new ContainerLocationResolver(
				rootLocation);
		assertThat(resolver.findContainer(rootLocation),
				is(root.getContainerForLocation(rootLocation)));
		assertThat(resolver.findFile(rootLocation),
				is(root.getFileForLocation(rootLocation)));
	}

	@Test
	public void containerLocationResolverFindFileShouldReturnFilteredFile()
			throws Exception {
		project.createFolder("folder");
		project.createFile("folder/ignored.txt", new byte[] {});
		IProject eclipseProject = project.getProject();
		eclipseProject.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL
						| IResourceFilterDescription.FILES
						| IResourceFilterDescription.INHERITABLE,
				new FileInfoMatcherDescription("org.eclipse.ui.ide.multiFilter", //$NON-NLS-1$
						"1.0-name-matches-false-false-ignored.txt"), //$NON-NLS-1$
				0, new NullProgressMonitor());
		eclipseProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		IPath location = eclipseProject.getLocation()
				.append("folder/ignored.txt");
		assertThat(ResourcesPlugin.getWorkspace().getRoot()
				.getFileForLocation(location), nullValue());
		IFile file = new ContainerLocationResolver(eclipseProject.getLocation())
				.findFile(location);
		assertThat(file, notNullValue());
		assertFalse(file.exists());
	}

	@Test
	public void containerLocationResolverShouldResolveLikeResourceUtilOutsideOfWorkspace()
			throws Exception {
		File parent = testUtils.createTempDir("resolverRepo");
		TestProject imported = new TestProject(true, "repo/Project-Outside",
				false, parent);
		Repository outsideRepo = FileRepositoryBuilder
				.create(new File(parent, "repo/" + Constants.DOT_GIT));
		try {
			outsideRepo.create();
			new ConnectProviderOperation(imported.getProject(),
					outsideRepo.getDirectory()).execute(null);
			imported.createFolder("inner");

			IPath workTree = new Path(
					outsideRepo.getWorkTree().getAbsolutePath());
			IPath projectLocation = imported.getProject().getLocation();
			ContainerLocationResolver resolver = new ContainerLocationResolver(
					workTree);
			// The working tree also contains locations outside of any project
			IPath[] locations = { workTree, workTree.append("outside"),
					projectLocation, projectLocation.append("inner") };
			for (IPath location : locations) {
				assertThat(location.toString(), resolver.getContainer(location),
						is(ResourceUtil.getContainerForLocation(location,
								false)));
			}
			assertThat(resolver.getContainer(workTree.append("outside")),
					nullValue());
			assertThat(resolver.getContainer(projectLocation.append("inner")),
					is(imported.getProject().getFolder("inner")));
		} finally {
			// Connected projects must be gone before the repository is
			// deleted, or they keep it open
			imported.dispose();
			outsideRepo.close();
			RepositoryCache.INSTANCE.clear();
			testUtils.deleteTempDirs();
		}
	}

	@Test
	public void containerLocationResolverShouldFindLinkedFolders()
			throws Exception {
		TestProject other = new TestProject(true, "Project-Linked");
		try {
			connect(other.getProject());
			IPath base = project.getProject().getLocation();
			// Missing below base, so only reachable through the linked folder
			IPath target = base.append("missing");
			IFolder link = other.getProject().getFolder("link");
			link.createLink(target, IResource.ALLOW_MISSING_LOCAL, null);
			assertTrue(link.exists());

			ContainerLocationResolver resolver = new ContainerLocationResolver(
					base);
			assertThat(resolver.getContainer(target),
					is(ResourceUtil.getContainerForLocation(target, false)));
			assertThat(resolver.getContainer(target), is(link));
		} finally {
			other.dispose();
		}
	}

	private void connect(IProject p) throws CoreException {
		ConnectProviderOperation operation = new ConnectProviderOperation(p,
				gitDir);
		operation.execute(null);
	}
}
