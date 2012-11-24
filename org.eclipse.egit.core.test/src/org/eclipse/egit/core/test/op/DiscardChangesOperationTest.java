/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2012, François Rey <eclipse.org_@_francois_._rey_._name>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.egit.core.test.DualRepositoryTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DiscardChangesOperationTest extends DualRepositoryTestCase {

	File workdir;

	IProject project;
	IProject project2;

	String projectName = "DiscardChangesTest";

	@Before
	public void setUp() throws Exception {

		workdir = testUtils.createTempDir("Repository1");

		repository1 = new TestRepository(new File(workdir, Constants.DOT_GIT));

		// now we create a project in repo1
		project = testUtils
				.createProjectInLocalFileSystem(workdir, projectName);
		testUtils.addFileToProject(project, "folder1/file1.txt", "Hello world 1");
		testUtils.addFileToProject(project, "folder1/file2.txt", "Hello world 2");

		repository1.connect(project);
		repository1.trackAllFiles(project);
		repository1.commit("Initial commit");

		File workdir2 = testUtils.createTempDir("Project2");
		// Project location is at root of repository
		project2 = testUtils.createProjectInLocalFileSystem(workdir2.getParentFile(), "Project2");
		testUtils.addFileToProject(project2, "file.txt", "initial");
		repository2 = new TestRepository(new File(workdir2, Constants.DOT_GIT));
		repository2.connect(project2);
		repository2.trackAllFiles(project2);
		repository2.commit("Initial commit");
	}

	@After
	public void tearDown() throws Exception {
		project.close(null);
		project.delete(false, false, null);
		project2.close(null);
		project2.delete(false, false, null);
		repository1.dispose();
		repository1 = null;
		repository2.dispose();
		repository2 = null;
		testUtils.deleteTempDirs();
	}

	@Test
	public void testDiscardChanges() throws Exception {
		IFile file1 = project.getFile(new Path("folder1/file1.txt"));
		String contents = testUtils.slurpAndClose(file1.getContents());
		assertEquals("Hello world 1", contents);
		setNewFileContent(file1, "changed 1");

		IFile file2 = project.getFile(new Path("folder1/file2.txt"));
		contents = testUtils.slurpAndClose(file2.getContents());
		assertEquals("Hello world 2", contents);
		setNewFileContent(file2, "changed 2");

		DiscardChangesOperation dcop = new DiscardChangesOperation(
				new IResource[] { file1, file2 });
		dcop.execute(new NullProgressMonitor());

		contents = testUtils.slurpAndClose(file1.getContents());
		assertEquals("Hello world 1", contents);

		contents = testUtils.slurpAndClose(file2.getContents());
		assertEquals("Hello world 2", contents);
	}

	@Test
	public void shouldWorkWhenProjectIsRootOfRepository() throws Exception {
		IFile file = project2.getFile(new Path("file.txt"));
		String contents = testUtils.slurpAndClose(file.getContents());
		assertEquals("initial", contents);
		setNewFileContent(file, "changed");

		DiscardChangesOperation dcop = new DiscardChangesOperation(new IResource[] { project2 });
		dcop.execute(new NullProgressMonitor());

		String replacedContents = testUtils.slurpAndClose(file.getContents());
		assertEquals("initial", replacedContents);
	}

	private void setNewFileContent(IFile file, String content) throws Exception {
		file.setContents(
				new ByteArrayInputStream(content.getBytes(project
						.getDefaultCharset())), 0, null);
		assertEquals(content, testUtils.slurpAndClose(file.getContents()));
	}
}
