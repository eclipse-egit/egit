/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.op.UntrackOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.test.DualRepositoryTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TrackUntrackOperationTest extends DualRepositoryTestCase {

	File workdir;

	IProject project;

	String projectName = "TrackTest";

	@Before
	public void setUp() throws Exception {

		workdir = testUtils.createTempDir("Repository1");

		repository1 = new TestRepository(new File(workdir, Constants.DOT_GIT));

		// now we create a project in repo1
		project = testUtils
				.createProjectInLocalFileSystem(workdir, projectName);
		testUtils.addFileToProject(project, "folder1/file1.txt", "Hello world");

		repository1.connect(project);
	}

	@After
	public void tearDown() throws Exception {
		project.close(null);
		project.delete(false, false, null);
		repository1.dispose();
		repository1 = null;
		testUtils.deleteTempDirs();
	}

	@Test
	public void testTrackFiles() throws Exception {

		final ArrayList<IFile> files = new ArrayList<>();

		project.accept(new IResourceVisitor() {

			@Override
			public boolean visit(IResource resource) throws CoreException {
				if (resource instanceof IFile)
					files.add((IFile) resource);
				return true;
			}
		});

		IFile[] fileArr = files.toArray(new IFile[0]);

		assertTrackedState(fileArr, false);

		AddToIndexOperation trop = new AddToIndexOperation(files);
		trop.execute(new NullProgressMonitor());

		assertTrackedState(fileArr, true);

		UntrackOperation utop = new UntrackOperation(Arrays.asList(fileArr));

		utop.execute(new NullProgressMonitor());

		assertTrackedState(fileArr, false);
	}

	@SuppressWarnings("boxing")
	private void assertTrackedState(IFile[] fileArr, boolean expectedState)
			throws IOException {
		DirCache cache = repository1.getRepository().readDirCache();
		for (IFile file : fileArr) {
			RepositoryMapping rm = RepositoryMapping.getMapping(file);
			String fileDir = rm.getRepoRelativePath(file);
			boolean tracked = cache.findEntry(fileDir) > -1;
			assertEquals("Wrong tracking state", expectedState, tracked);
		}
	}

	@Test
	public void testTrackProject() throws Exception {

		final ArrayList<IContainer> containers = new ArrayList<>();
		containers.add(project);

		final ArrayList<IFile> files = new ArrayList<>();

		project.accept(new IResourceVisitor() {

			@Override
			public boolean visit(IResource resource) throws CoreException {
				if (resource instanceof IFile)
					files.add((IFile) resource);
				return true;
			}
		});

		IFile[] fileArr = files.toArray(new IFile[0]);

		assertTrackedState(fileArr, false);

		AddToIndexOperation trop = new AddToIndexOperation(containers);
		trop.execute(new NullProgressMonitor());

		assertTrackedState(fileArr, true);

		UntrackOperation utrop = new UntrackOperation(containers);
		utrop.execute(new NullProgressMonitor());

		assertTrackedState(fileArr, false);
	}

}
