/*******************************************************************************
 * Copyright (C) 2014, Maik Schreiber <blizzy@blizzy.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Maik Schreiber - initial implementation
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.egit.core.CommitUtil;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CommitUtilTest extends GitTestCase {
	private TestRepository testRepository;

	private RevCommit commit1;

	private RevCommit commit2;

	private RevCommit commit3;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		gitDir = new File(project.getProject().getLocationURI().getPath(),
				Constants.DOT_GIT);
		testRepository = new TestRepository(gitDir);
		testRepository.connect(project.getProject());
		testRepository.createInitialCommit("initial");

		File file = testRepository.createFile(project.getProject(), "file-1");
		commit1 = testRepository.addAndCommit(project.getProject(), file,
				"commit 1");
		testRepository.appendFileContent(file, "file-2");
		commit2 = testRepository.addAndCommit(project.getProject(), file,
				"commit 2");
		testRepository.appendFileContent(file, "file-3");
		commit3 = testRepository.addAndCommit(project.getProject(), file,
				"commit 3");
	}

	@Override
	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		super.tearDown();
	}

	@Test
	public void sortCommits() {
		List<RevCommit> commits = Arrays.asList(commit2, commit1, commit3);
		List<RevCommit> sortedCommits = CommitUtil.sortCommits(commits);
		List<RevCommit> expectedCommits = Arrays.asList(commit1, commit2,
				commit3);
		assertEquals(expectedCommits, sortedCommits);
	}
}
