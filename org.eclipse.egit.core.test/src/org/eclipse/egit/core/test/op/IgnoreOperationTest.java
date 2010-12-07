/*******************************************************************************
 * Copyright (C) 2010, Benjamin Muskalla <bmuskalla@eclipsesource.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.op.IgnoreOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IgnoreOperationTest extends GitTestCase {

	private TestRepository testRepository;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		testRepository = new TestRepository(gitDir);
		testRepository.connect(project.getProject());
	}

	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		// delete gitignore file in workspace folder
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		File rootFile = root.getRawLocation().toFile();
		File ignoreFile = new File(rootFile, Constants.GITIGNORE_FILENAME);
		if (ignoreFile.exists()) {
			FileUtils.deleteRepeated(ignoreFile);
			assert !ignoreFile.exists();
		}
		super.tearDown();
	}

	@Test
	public void testIgnoreFolder() throws Exception {
		IFolder binFolder = project.getProject().getFolder("bin");
		IgnoreOperation operation = new IgnoreOperation(
				new IResource[] { binFolder });
		operation.execute(new NullProgressMonitor());

		String content = project.getFileContent(Constants.GITIGNORE_FILENAME);
		assertEquals("/bin\n", content);
		assertFalse(operation.isGitignoreOutsideWSChanged());
	}

	@Test
	public void testIgnoreFileCancel() throws Exception {
		IFolder binFolder = project.getProject().getFolder("bin");
		IgnoreOperation operation = new IgnoreOperation(
				new IResource[] { binFolder });
		NullProgressMonitor monitor = new NullProgressMonitor();
		monitor.setCanceled(true);
		operation.execute(monitor);

		assertFalse(project.getProject().getFile(Constants.GITIGNORE_FILENAME).exists());
	}


	@Test
	public void testSchedulingRule() throws Exception {
		IFolder binFolder = project.getProject().getFolder("bin");
		IgnoreOperation operation = new IgnoreOperation(
				new IResource[] { binFolder });
		NullProgressMonitor monitor = new NullProgressMonitor();
		operation.execute(monitor);

		assertNotNull(operation.getSchedulingRule());
	}

	@Test
	public void testIgnoreMultiFile() throws Exception {
		project.createSourceFolder();
		IFolder binFolder = project.getProject().getFolder("bin");
		IFolder srcFolder = project.getProject().getFolder("src");
		IgnoreOperation operation = new IgnoreOperation(
				new IResource[] { binFolder });
		operation.execute(new NullProgressMonitor());

		String content = project.getFileContent(Constants.GITIGNORE_FILENAME);
		assertEquals("/bin\n", content);

		operation = new IgnoreOperation(
				new IResource[] { srcFolder });
		operation.execute(new NullProgressMonitor());

		content = project.getFileContent(Constants.GITIGNORE_FILENAME);
		assertEquals("/bin\n/src\n", content);
	}

	@Test
	public void testIgnoreProject() throws Exception {
		IgnoreOperation operation = new IgnoreOperation(
				new IResource[] { project.getProject() });
		operation.execute(new NullProgressMonitor());

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		File rootFile = root.getRawLocation().toFile();
		File ignoreFile = new File(rootFile, Constants.GITIGNORE_FILENAME);
		String content = testUtils.slurpAndClose(ignoreFile.toURL()
				.openStream());
		assertEquals("/" + project.getProject().getName() + "\n", content);
		assertTrue(operation.isGitignoreOutsideWSChanged());
	}

}
