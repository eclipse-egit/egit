/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.op.TrackOperation;
import org.eclipse.egit.core.op.UntrackOperation;
import org.eclipse.egit.core.test.DualRepositoryTestCase;
import org.eclipse.egit.core.test.TestRepository;
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

		workdir = testUtils.getTempDir("Repository1");

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
		testUtils.deleteRecursive(workdir);
	}

	@Test
	public void testTrackFiles() throws Exception {

		final ArrayList<IFile> files = new ArrayList<IFile>();

		assertEquals("Index should be empty", 0, repository1.getRepository()
				.getIndex().getMembers().length);

		project.accept(new IResourceVisitor() {

			public boolean visit(IResource resource) throws CoreException {
				if (resource instanceof IFile)
					files.add((IFile) resource);
				return true;
			}
		});

		IFile[] fileArr = files.toArray(new IFile[files.size()]);

		TrackOperation trop = new TrackOperation(fileArr);
		trop.execute(new NullProgressMonitor());

		assertEquals("Index should have two entries", 2, repository1
				.getRepository().getIndex().getMembers().length);

		UntrackOperation utop = new UntrackOperation(Arrays.asList(fileArr));
		utop.execute(new NullProgressMonitor());

		assertEquals("Index should be empty", 0, repository1.getRepository()
				.getIndex().getMembers().length);

	}

	@Test
	public void testTrackProject() throws Exception {

		final ArrayList<IContainer> containers = new ArrayList<IContainer>();
		containers.add(project);

		assertEquals("Index should be empty", 0, repository1.getRepository()
				.getIndex().getMembers().length);

		IContainer[] fileArr = containers.toArray(new IContainer[containers
				.size()]);

		TrackOperation trop = new TrackOperation(fileArr);
		trop.execute(new NullProgressMonitor());

		assertEquals("Index should have two entries", 2, repository1
				.getRepository().getIndex().getMembers().length);
	}

}
