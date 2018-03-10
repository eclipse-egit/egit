/*******************************************************************************
 * Copyright (C) 2010, Benjamin Muskalla <bmuskalla@eclipsesource.com>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
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
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.IgnoreOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestProject;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IgnoreOperationTest extends GitTestCase {

	private TestRepository testRepository;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		testRepository = new TestRepository(gitDir);
		testRepository.connect(project.getProject());
	}

	@Override
	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		// delete gitignore file in workspace folder
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		File rootFile = root.getRawLocation().toFile();
		File ignoreFile = new File(rootFile, Constants.GITIGNORE_FILENAME);
		if (ignoreFile.exists()) {
			FileUtils.delete(ignoreFile, FileUtils.RETRY);
			assert !ignoreFile.exists();
		}
		super.tearDown();
	}

	@Test
	public void testIgnoreFolder() throws Exception {
		IFolder binFolder = project.getProject().getFolder("bin");
		IgnoreOperation operation = executeIgnore(binFolder.getLocation());

		String content = project.getFileContent(Constants.GITIGNORE_FILENAME);
		assertEquals("/bin/\n", content);
		assertFalse(operation.isGitignoreOutsideWSChanged());
	}

	@Test
	public void testIgnoreFile() throws Exception {
		IFile aFile = project.createFile("aFile.txt", new byte[0]);
		IgnoreOperation operation = executeIgnore(aFile.getLocation());
		String content = project.getFileContent(Constants.GITIGNORE_FILENAME);
		assertEquals("/aFile.txt\n", content);
		assertFalse(operation.isGitignoreOutsideWSChanged());
	}

	@Test
	public void testIgnoreFileCancel() throws Exception {
		IFolder binFolder = project.getProject().getFolder("bin");
		IgnoreOperation operation = new IgnoreOperation(Arrays.asList(binFolder.getLocation()));
		NullProgressMonitor monitor = new NullProgressMonitor();
		monitor.setCanceled(true);
		operation.execute(monitor);

		assertFalse(project.getProject().getFile(Constants.GITIGNORE_FILENAME).exists());
	}


	@Test
	public void testSchedulingRule() throws Exception {
		IFolder binFolder = project.getProject().getFolder("bin");
		IgnoreOperation operation = executeIgnore(binFolder.getLocation());

		assertNotNull(operation.getSchedulingRule());
	}

	@Test
	public void testIgnoreMultiFolders() throws Exception {
		project.createSourceFolder();
		IFolder binFolder = project.getProject().getFolder("bin");
		IFolder srcFolder = project.getProject().getFolder("src");
		executeIgnore(binFolder.getLocation());

		String content = project.getFileContent(Constants.GITIGNORE_FILENAME);
		assertEquals("/bin/\n", content);

		executeIgnore(srcFolder.getLocation());

		content = project.getFileContent(Constants.GITIGNORE_FILENAME);
		assertEquals("/bin/\n/src/\n", content);
	}

	@Test
	public void testIgnoreProject() throws Exception {
		IgnoreOperation operation = executeIgnore(
				project.getProject().getLocation());

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		File rootFile = root.getRawLocation().toFile();
		File ignoreFile = new File(rootFile, Constants.GITIGNORE_FILENAME);
		String content = testUtils.slurpAndClose(ignoreFile.toURI().toURL()
				.openStream());
		assertEquals("/.metadata/\n/" + project.getProject().getName() + "/\n",
				content);
		assertTrue(operation.isGitignoreOutsideWSChanged());
	}

	@Test
	public void testIgnoreNoTrailingNewline() throws Exception {
		String existing = "/nonewline";
		IFile ignore = project.getProject().getFile(
				Constants.GITIGNORE_FILENAME);
		assertFalse(ignore.exists());
		ignore.create(new ByteArrayInputStream(existing.getBytes("UTF-8")),
				IResource.FORCE, new NullProgressMonitor());

		IFolder binFolder = project.getProject().getFolder("bin");
		IgnoreOperation operation = executeIgnore(binFolder.getLocation());

		String content = project.getFileContent(Constants.GITIGNORE_FILENAME);
		assertEquals(existing + "\n/bin/\n", content);
		assertFalse(operation.isGitignoreOutsideWSChanged());
	}

	@Test
	public void testIgnoreWithResource() throws Exception {
		IFolder binFolder = project.getProject().getFolder("bin");
		Collection<IPath> c = Collections
				.singletonList(binFolder.getLocation());
		IgnoreOperation operation = new IgnoreOperation(c);
		operation.execute(new NullProgressMonitor());

		String content = project.getFileContent(Constants.GITIGNORE_FILENAME);
		assertEquals("/bin/\n", content);
	}

	@Test
	public void testWithNestedProjects() throws Exception {
		TestProject nested = new TestProject(true, "Project-1/Project-2");
		try {
			// Use Project-1 to create folder, Project-2 to get file to try to
			// confuse any caches in workspace root (location -> IResource).
			project.createFolder("Project-2/please");
			IFile ignoreme = nested.createFile("please/ignoreme", new byte[0]);
			IgnoreOperation operation = executeIgnore(ignoreme.getLocation());
			String content = nested.getFileContent("please/.gitignore");
			assertEquals("/ignoreme\n", content);
			assertFalse(operation.isGitignoreOutsideWSChanged());
		} finally {
			nested.dispose();
		}
	}

	private IgnoreOperation executeIgnore(IPath... paths) throws Exception {
		final IgnoreOperation operation = new IgnoreOperation(Arrays.asList(paths));
		Job job = new Job("Ignoring resources for test") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					operation.execute(monitor);
				} catch (CoreException e) {
					return e.getStatus();
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(operation.getSchedulingRule());
		job.schedule();
		job.join();
		if (!job.getResult().isOK())
			fail("Ignore job failed: " + job.getResult());
		return operation;
	}
}
