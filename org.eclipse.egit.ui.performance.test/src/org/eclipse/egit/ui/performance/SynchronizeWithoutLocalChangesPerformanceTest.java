/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.performance;

import static org.eclipse.egit.ui.performance.LocalRepositoryTestCase.REPO1;
import static org.eclipse.egit.ui.performance.LocalRepositoryTestCase.getTestDirectory;
import static org.eclipse.jgit.lib.Constants.DOT_GIT;

import java.io.File;

import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepository;

public class SynchronizeWithoutLocalChangesPerformanceTest extends
		AbstractSynchronizeViewPerformanceTest {

	public SynchronizeWithoutLocalChangesPerformanceTest() {
		super(false);
	}

	protected void fillRepository() throws Exception {
		// add some commits
		File repoRoot = new File(getTestDirectory(), REPO1);
		FileRepository db = new FileRepository(new File(repoRoot, DOT_GIT));
		TestRepository<FileRepository> tr = new TestRepository<FileRepository>(
				db);
		DirCacheEntry file = tr.file("test.txt", tr.blob("first file"));
		RevCommit root = tr.commit(tr.tree(file));

		int fileCount = 50;
		DirCacheEntry[] files = new DirCacheEntry[fileCount];
		for (int i = 0; i < 1500; i++) {
			for (int j = 0; j < fileCount; j++)
				files[j] = tr.file("t" + (i + j), tr.blob("x " + (i + j)));
			tr.tick(5);
			root = tr.commit(tr.tree(files), root);
		}
		tr.update("master", root);
	}

}
