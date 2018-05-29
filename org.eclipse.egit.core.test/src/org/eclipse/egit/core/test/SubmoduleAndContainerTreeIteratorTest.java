/*******************************************************************************
 * Copyright (C) 2014, Christian Halstrick <christian.halstrick@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.Activator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SubmoduleAndContainerTreeIteratorTest {
	private TestUtils testUtils;

	private IProject childProject;

	private File childProjectDir;

	private TestRepository childRepository;

	private IProject parentProject;

	private File parentProjectDir;

	private TestRepository parentRepository;

	private IFile parentFile;

	private FileTreeIterator treeIt;

	@Before
	public void setUp() throws Exception {
		testUtils = new TestUtils();

		// Create child repo and project
		childProject = testUtils.createProjectInLocalFileSystem("child");
		childProjectDir = childProject.getRawLocation().toFile();
		childRepository = new TestRepository(new File(childProjectDir,
				Constants.DOT_GIT));
		testUtils.addFileToProject(childProject, "child.txt", "Hello world");
		childRepository.connect(childProject);
		// We have to wait after until the filesystem timer has advanced to
		// avoid smudged states
		RepositoryTestCase.fsTick(null);
		childRepository.trackAllFiles(childProject);
		childRepository.commit("Initial commit");
		// Create parent repo and project
		parentProject = testUtils.createProjectInLocalFileSystem("parent");
		parentProjectDir = parentProject.getRawLocation().toFile();
		parentRepository = new TestRepository(new File(parentProjectDir,
				Constants.DOT_GIT));
		parentFile = testUtils.addFileToProject(parentProject, "parent.txt",
				"Hello world");
		Git.wrap(parentRepository.getRepository()).submoduleAdd()
				.setPath("children/child")
				.setURI(childProjectDir.toURI().toString()).call();
		parentRepository.connect(parentProject);
		RepositoryTestCase.fsTick(null);
		parentRepository.trackAllFiles(parentProject);
		parentRepository.commit("Initial commit");

		treeIt = new FileTreeIterator(parentRepository.getRepository());
	}

	@After
	public void tearDown() throws Exception {
		childRepository.dispose();
		parentRepository.dispose();
		childProject.delete(true, true, null);
		parentProject.delete(true, true, null);
		Activator.getDefault().getRepositoryCache().clear();
		testUtils.deleteTempDirs();
	}

	@Test
	public void testCleanStateAfterInit() throws NoWorkTreeException,
			GitAPIException {
		Git parentGit = Git.wrap(parentRepository.getRepository());

		Status status = parentGit.status().setWorkingTreeIt(treeIt).call();
		assertTrue(status.isClean());
	}

	@Test
	public void testCleanStateFirstCommit() throws NoWorkTreeException,
			GitAPIException, IOException, CoreException, InterruptedException {
		testUtils.changeContentOfFile(parentProject, parentFile, "new content");
		RepositoryTestCase.fsTick(null);
		parentRepository.trackAllFiles(parentProject);
		parentRepository.commit("modified parent.txt");
		Git parentGit = Git.wrap(parentRepository.getRepository());

		Status status = parentGit.status().setWorkingTreeIt(treeIt).call();
		assertTrue(status.isClean());
	}
}
