/*******************************************************************************
 * Copyright (C) 2015 Obeo and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Iterator;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.op.MergeOperation;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This makes sure that the preferred merge strategy chosen bu the user ni the
 * preferences is actually used by MergeOperations to do its job.
 */
public class MergeWithPreferredStrategyTest extends GitTestCase {

	private static final String MASTER = Constants.R_HEADS + Constants.MASTER;

	private static final String SIDE = Constants.R_HEADS + "side";

	private TestRepository testRepository;

	private File file2;

	private File file3;

	/**
	 * Sets the preference "preferred merge strategy" to "ours", so that the
	 * MergeStrategy that will be used by any operation that needs a merge will
	 * be the {@link MergeStrategy#OURS}.
	 */
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		InstanceScope.INSTANCE.getNode(Activator.getPluginId()).put(
				GitCorePreferences.core_preferredMergeStrategy, "ours");
		gitDir = new File(project.getProject().getLocationURI().getPath(),
				Constants.DOT_GIT);
		testRepository = new TestRepository(gitDir);
		testRepository.connect(project.getProject());

		File file1 = testRepository.createFile(project.getProject(), "file1-1");
		testRepository.addAndCommit(project.getProject(), file1,
				"master commit 1");
		testRepository.createBranch(MASTER, SIDE);
		file2 = testRepository.createFile(project.getProject(), "file2");
		testRepository.appendFileContent(file2, "Content 2");
		testRepository.addAndCommit(project.getProject(), file2,
				"master commit 2");
		testRepository.checkoutBranch(SIDE);
		file3 = testRepository.createFile(project.getProject(), "file3");
		testRepository.appendFileContent(file3, "Content 3");
		testRepository.addAndCommit(project.getProject(), file3,
				"side commit 3");
		testRepository.checkoutBranch(MASTER);
	}

	/**
	 * Removes any preference about preferred merge strategy after any test.
	 */
	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		InstanceScope.INSTANCE.getNode(Activator.getPluginId()).remove(
				GitCorePreferences.core_preferredMergeStrategy);
	}

	@Test
	public void testMergeUsesPreferredStrategy() throws Exception {
		MergeOperation operation = new MergeOperation(
				testRepository.getRepository(), SIDE);
		operation.execute(new NullProgressMonitor());

		// With the MergeStrategy OURS, new files from branch 'side' should be
		// ignored and new files from branch 'master' should be present
		assertEquals(4, countCommitsInHead());
		assertTrue(testRepository.getIFile(project.getProject(), file2)
				.exists());
		assertFalse(testRepository.getIFile(project.getProject(), file3)
				.exists());
	}

	@Test
	public void testMergeUsesPreferredStrategyRegisteredWithExplicitName()
			throws Exception {
		InstanceScope.INSTANCE.getNode(Activator.getPluginId()).put(
				GitCorePreferences.core_preferredMergeStrategy,
				"org.eclipse.egit.core.test.theirs");
		MergeOperation operation = new MergeOperation(
				testRepository.getRepository(), SIDE);
		operation.execute(new NullProgressMonitor());

		// With the MergeStrategy THEIRS, new files from branch 'master' should
		// be ignored and new files from branch 'side' should be present
		assertEquals(4, countCommitsInHead());
		assertFalse(testRepository.getIFile(project.getProject(), file2)
				.exists());
		assertTrue(testRepository.getIFile(project.getProject(), file3)
				.exists());
	}

	@Test
	public void testStrategyCanBeOverridden() throws Exception {
		MergeOperation operation = new MergeOperation(
				testRepository.getRepository(), SIDE, "recursive");
		operation.execute(new NullProgressMonitor());

		// With the MergeStrategy RECURSIVE, new files from branch 'master' and
		// 'side' should be present
		assertEquals(4, countCommitsInHead());
		assertTrue(testRepository.getIFile(project.getProject(), file2)
				.exists());
		assertTrue(
				testRepository.getIFile(project.getProject(), file3).exists());
	}

	private int countCommitsInHead() throws GitAPIException {
		try (Git git = new Git(testRepository.getRepository())) {
			LogCommand log = git.log();
			Iterable<RevCommit> commits = log.call();
			int result = 0;
			for (Iterator i = commits.iterator(); i.hasNext();) {
				i.next();
				result++;
			}
			return result;
		}
	}
}
