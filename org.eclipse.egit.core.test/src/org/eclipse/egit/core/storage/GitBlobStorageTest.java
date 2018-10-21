/*******************************************************************************
 * Copyright (C) 2010, Robin Rosenberg
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.storage.GitFileHistoryProvider;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.op.DisconnectProviderOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.team.core.history.IFileHistory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GitBlobStorageTest extends GitTestCase {

	Repository repository;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		repository = FileRepositoryBuilder.create(gitDir);
		repository.create();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		repository.close();
		super.tearDown();
	}

	@Test
	public void testOk() throws Exception {
		ObjectId id = createFile(repository, project.getProject(), "file", "data");
		GitBlobStorage blobStorage = new GitBlobStorage(repository, "p/file",
				id, null);
		assertEquals("file", blobStorage.getName());
		assertEquals("data", testUtils.slurpAndClose(blobStorage.getContents()));
		assertEquals(Path.fromPortableString("p/file").toOSString(), blobStorage.getFullPath().toOSString());

	}

	@Test
	public void testGitFileHistorySingleProjectOk() throws Exception {
		IProgressMonitor progress = new NullProgressMonitor();
		TestProject singleRepoProject = new TestProject(true, "Project-2");
		IProject proj = singleRepoProject.getProject();
		File singleProjectGitDir = new File(proj.getLocation().toFile(), Constants.DOT_GIT);
		if (singleProjectGitDir.exists())
			FileUtils.delete(singleProjectGitDir, FileUtils.RECURSIVE | FileUtils.RETRY);

		Repository singleProjectRepo = FileRepositoryBuilder
				.create(singleProjectGitDir);
		singleProjectRepo.create();

		// Repository must be mapped in order to test the GitFileHistory
		Activator.getDefault().getRepositoryUtil().addConfiguredRepository(singleProjectGitDir);
		ConnectProviderOperation connectOp = new ConnectProviderOperation(proj, singleProjectGitDir);
		connectOp.execute(progress);

		try (Git git = new Git(singleProjectRepo)) {
			IFile file = proj.getFile("file");
			file.create(new ByteArrayInputStream("data".getBytes("UTF-8")), 0,
					progress);
			git.add().addFilepattern(".").call();
			RevCommit commit = git.commit().setAuthor("JUnit", "junit@jgit.org").setAll(true).setMessage("First commit").call();

			GitFileHistoryProvider fhProvider = new GitFileHistoryProvider();
			IFileHistory fh = fhProvider.getFileHistoryFor(singleRepoProject.getProject(), 0, null);
			assertNotNull(fh);
			assertEquals(fh.getFileRevisions().length, 1);
			assertNotNull(fh.getFileRevision(commit.getId().getName()));
		} finally {
			DisconnectProviderOperation disconnectOp = new DisconnectProviderOperation(Collections.singletonList(proj));
			disconnectOp.execute(progress);
			Activator.getDefault().getRepositoryUtil().removeDir(singleProjectGitDir);
			singleProjectRepo.close();
			singleRepoProject.dispose();
		}
	}

	@Test
	public void testFailNotFound() throws Exception {
		GitBlobStorage blobStorage = new GitBlobStorage(repository, "file",
				ObjectId.fromString("0123456789012345678901234567890123456789"),
				null);
		assertEquals("file", blobStorage.getName());
		try {
			blobStorage.getContents();
			fail("We should not be able to read this 'blob'");
		} catch (CoreException e) {
			assertEquals("Git blob 0123456789012345678901234567890123456789 with path file not found", e.getMessage());
		}
	}

	@Test
	public void testFailWrongType() throws Exception {
		GitBlobStorage blobStorage = new GitBlobStorage(repository, "file",
				ObjectId.fromString("4b825dc642cb6eb9a060e54bf8d69288fbee4904"),
				null);
		assertEquals("file", blobStorage.getName());
		try {
			blobStorage.getContents();
			fail("We should not be able to read this blob");
		} catch (CoreException e) {
			assertEquals("Git blob 4b825dc642cb6eb9a060e54bf8d69288fbee4904 with path file not found", e.getMessage());
		}
	}

	@Test
	public void testFailCorrupt() throws Exception {
		try {
			createFileCorruptShort(repository, project.getProject(), "file", "data");
			GitBlobStorage blobStorage = new GitBlobStorage(repository, "file",
					ObjectId.fromString(
							"6320cd248dd8aeaab759d5871f8781b5c0505172"),
					null);
			assertEquals("file", blobStorage.getName());
			blobStorage.getContents();
			fail("We should not be able to read this blob");
		} catch (CoreException e) {
			assertEquals("IO error reading Git blob 6320cd248dd8aeaab759d5871f8781b5c0505172 with path file", e.getMessage());
		}
	}

	@Test
	public void testFailCorrupt2() throws Exception {
		try {
			createFileCorruptShort(repository, project.getProject(), "file", "datjhjhjhjhjhjhjjkujioedfughjuop986rdfghjhiu7867586redtfguy675r6tfguhyo76r7tfa");
			GitBlobStorage blobStorage = new GitBlobStorage(repository, "file",
					ObjectId.fromString(
							"526ef34fc76ab0c35ccee343bda1a626efbd4134"),
					null);
			assertEquals("file", blobStorage.getName());
			blobStorage.getContents();
			fail("We should not be able to read this blob");
		} catch (CoreException e) {
			assertEquals("IO error reading Git blob 526ef34fc76ab0c35ccee343bda1a626efbd4134 with path file", e.getMessage());
		}
	}
}
