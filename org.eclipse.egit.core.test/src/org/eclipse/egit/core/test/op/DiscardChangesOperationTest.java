/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2012, François Rey <eclipse.org_@_francois_._rey_._name>
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.egit.core.op.DiscardChangesOperation.Stage;
import org.eclipse.egit.core.test.DualRepositoryTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.IO;
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
	public void testDiscardChangesWithPath() throws Exception {
		IFile file1 = project.getFile(new Path("folder1/file1.txt"));
		setNewFileContent(file1, "changed 1");

		DiscardChangesOperation operation = new DiscardChangesOperation(
				Arrays.asList(file1.getLocation()));
		operation.execute(new NullProgressMonitor());

		assertEquals("Hello world 1",
				testUtils.slurpAndClose(file1.getContents()));
	}

	@Test
	public void testDiscardChangesWithStage() throws Exception {
		Git git = Git.wrap(repository1.getRepository());
		File file = new File(repository1.getRepository().getWorkTree(),
				"conflict.txt");
		repository1.appendFileContent(file, "base", false);
		git.add().addFilepattern("conflict.txt").call();
		git.commit().setMessage("commit").call();

		git.checkout().setCreateBranch(true).setName("side").call();
		repository1.appendFileContent(file, "side", false);
		git.add().addFilepattern("conflict.txt").call();
		RevCommit side = git.commit().setMessage("commit on side").call();

		git.checkout().setName("master").call();
		repository1.appendFileContent(file, "master", false);
		git.add().addFilepattern("conflict.txt").call();
		git.commit().setMessage("commit on master").call();

		git.merge().include(side).call();

		DirCache dirCache = repository1.getRepository().readDirCache();
		assertEquals(1, dirCache.getEntry("conflict.txt").getStage());

		IPath path = new Path(file.getAbsolutePath());
		DiscardChangesOperation operation = new DiscardChangesOperation(
				Arrays.asList(path));
		operation.setStage(Stage.THEIRS);
		operation.execute(new NullProgressMonitor());

		DirCache dirCacheAfter = repository1.getRepository().readDirCache();
		assertEquals("Expected index to be unmodified", 1, dirCacheAfter
				.getEntry("conflict.txt").getStage());

		assertEquals("side", new String(IO.readFully(file), "UTF-8"));
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
