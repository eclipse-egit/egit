/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.ResetOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ResetOperationTest extends GitTestCase {

	TestRepository testRepository;

	Repository repository;

	// members filled by setupRepository()
	RevCommit initialCommit;

	File projectFile;

	IFile untrackedFile;

	IFile fileInIndex;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		testRepository = new TestRepository(gitDir);
		repository = testRepository.getRepository();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		repository = null;
		super.tearDown();
	}

	@Test
	public void testHardReset() throws Exception {
		setupRepository();
		String fileInIndexPath = fileInIndex.getLocation().toPortableString();
		new ResetOperation(repository, initialCommit.getName(), ResetType.HARD)
				.execute(null);
		// .project must disappear, related Eclipse project must be deleted
		assertFalse(projectFile.exists());
		assertFalse(project.getProject().exists());
		// check if HEAD points to initial commit now
		assertTrue(repository.resolve("HEAD").equals(initialCommit));
		// check if files were removed
		assertFalse(untrackedFile.exists());
		assertFalse(fileInIndex.exists());
		// fileInIndex must no longer be in HEAD and in the index
		assertFalse(testRepository.inHead(fileInIndexPath));
		assertFalse(testRepository.inIndex(fileInIndexPath));
	}

	@Test
	public void testSoftReset() throws Exception {
		setupRepository();
		String fileInIndexPath = fileInIndex.getLocation().toPortableString();
		new ResetOperation(repository, initialCommit.getName(), ResetType.SOFT)
				.execute(null);
		// .project must remain
		assertTrue(projectFile.exists());
		assertTrue(project.getProject().exists());
		// check if HEAD points to initial commit now
		assertTrue(repository.resolve("HEAD").equals(initialCommit));
		// untrackedFile and fileInIndex must still exist
		assertTrue(untrackedFile.exists());
		assertTrue(fileInIndex.exists());
		// fileInIndex must no longer be in HEAD
		assertFalse(testRepository.inHead(fileInIndexPath));
		// fileInIndex must exist in the index
		assertTrue(testRepository.inIndex(fileInIndexPath));
	}

	@Test
	public void testMixedReset() throws Exception {
		setupRepository();
		String fileInIndexPath = fileInIndex.getLocation().toPortableString();
		new ResetOperation(repository, initialCommit.getName(), ResetType.MIXED)
				.execute(null);
		// .project must remain
		assertTrue(projectFile.exists());
		assertTrue(project.getProject().exists());
		// check if HEAD points to initial commit now
		assertTrue(repository.resolve("HEAD").equals(initialCommit));
		// untrackedFile and fileInIndex must still exist
		assertTrue(untrackedFile.exists());
		assertTrue(fileInIndex.exists());
		// fileInIndex must no longer be in HEAD
		assertFalse(testRepository.inHead(fileInIndexPath));
		// fileInIndex must not in the index
		assertFalse(testRepository.inIndex(fileInIndexPath));
	}

	private void setupRepository() throws Exception {
		// create first commit containing a dummy file
		initialCommit = testRepository
				.createInitialCommit("testResetOperation\n\nfirst commit\n");
		// add .project to version control
		String path = project.getProject().getLocation().append(".project")
				.toOSString();
		projectFile = new File(path);
		testRepository.track(projectFile);
		// add fileInIndex to version control
		fileInIndex = createFile("fileInIndex");
		testRepository.track(new File(fileInIndex.getLocation().toOSString()));
		testRepository.commit("Add .project file");
		// modify fileInIndex and add it to the index
		InputStream stream = new ByteArrayInputStream(new byte[] { 'I', 'n',
				'd', 'e', 'x' });
		fileInIndex.setContents(stream, 0, null);
		testRepository.addToIndex(fileInIndex);
		// create an untracked file
		untrackedFile = createFile("untrackedFile");
	}

	/**
	 * create a file with the given name in the root folder of testproject
	 *
	 * @param name
	 *            name of file
	 * @return new file
	 * @throws CoreException
	 */
	private IFile createFile(String name) throws CoreException {
		IFile file = project.project.getFile(name);
		file.create(
				new ByteArrayInputStream(new byte[] { 'T', 'e', 's', 't' }),
				true, null);
		return file;
	}
}
