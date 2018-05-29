/*******************************************************************************
 * Copyright (C) 2014, Obeo
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileDeleteHookTest extends GitTestCase {
	private Repository repo;

	private IProject iProject;

	private TestRepository testRepo;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		iProject = project.project;
		testRepo = new TestRepository(gitDir);
		testRepo.connect(iProject);
		repo = RepositoryMapping.getMapping(iProject).getRepository();
	}

	@After
	public void clearGitResources() throws Exception {
		testRepo.disconnect(iProject);
		testRepo.dispose();
		repo = null;
		super.tearDown();
	}

	@Test
	public void deleteTest() throws Exception {
		DirCache dirCache = repo.lockDirCache();

		IFile file1 = iProject.getFile(new Path("file1"));
		file1.create(new ByteArrayInputStream(new byte[0]), false,
				new NullProgressMonitor());

		try {
			file1.delete(false, new NullProgressMonitor());
		} finally {
			// Delete will fail in LockFailedException, unlock so that
			// #clearGitResources() doesn't fail to cleanup
			dirCache.unlock();
		}
	}
}
