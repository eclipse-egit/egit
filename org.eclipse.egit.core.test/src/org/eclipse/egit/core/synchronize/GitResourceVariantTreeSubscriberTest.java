/*******************************************************************************
 * Copyright (C) 2010, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantTree;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GitResourceVariantTreeSubscriberTest extends GitTestCase {

	private TestRepository testRepo;

	private RevCommit initialCommit;

	private RevCommit commitBranch;

	private RevCommit commitMaster;

	private IFile changedFile;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		testRepo = new TestRepository(gitDir);
		testRepo.connect(project.getProject());

		String fileName = "Main.java";
		File file = testRepo.createFile(project.getProject(), fileName);
		initialCommit = testRepo.appendContentAndCommit(project.getProject(),
				file, "class Main {}", "initial commit");
		changedFile = testRepo.getIFile(project.getProject(), file);

		testRepo.createAndCheckoutBranch(Constants.HEAD, Constants.R_HEADS
				+ "test");
		commitBranch = testRepo.appendContentAndCommit(project.getProject(),
				file, "// test 1", "second commit");

		testRepo.checkoutBranch(Constants.R_HEADS + Constants.MASTER);
		commitMaster = testRepo.appendContentAndCommit(project.getProject(),
				file, "// test 2", "third commit");
	}

	@After
	public void clearGitResources() throws Exception {
		testRepo.dispose();
		super.tearDown();
	}

	/**
	 * This test simulates that user work and made some changes on branch 'test'
	 * and then try to synchronize "test" and 'master' branch.
	 *
	 * @throws Exception
	 */
	@Test
	public void testSyncMasterAndBranch() throws Exception {
		// Note that "HEAD" is master
		GitResourceVariantTreeSubscriber grvts = createGitResourceVariantTreeSubscriber(
				Constants.HEAD, Constants.R_HEADS + "test");
		grvts.init(new NullProgressMonitor());

		IResourceVariant actualBase = commonAssertionsForBaseTree(
				grvts.getBaseTree(), changedFile);
		IResourceVariant actualRemote = commonAssertionsForRemoteTree(
				grvts.getRemoteTree(), changedFile);

		// our common ancestor is the initial commit
		assertEquals(initialCommit.abbreviate(7).name() + "... (J. Git)",
				actualBase.getContentIdentifier());

		// while the remote should be the branch
		assertEquals(commitBranch.abbreviate(7).name() + "... (J. Git)",
				actualRemote.getContentIdentifier());
	}

	/**
	 * This is the same test as testSyncMasterAndBranch... but synchronizing the
	 * other way around.
	 *
	 * @throws Exception
	 */
	@Test
	public void testSyncBranchAndMaster() throws Exception {
		// Note that "HEAD" is master
		GitResourceVariantTreeSubscriber grvts = createGitResourceVariantTreeSubscriber(
				Constants.R_HEADS + "test", Constants.HEAD);
		grvts.init(new NullProgressMonitor());

		IResourceVariant actualBase = commonAssertionsForBaseTree(
				grvts.getBaseTree(), changedFile);
		IResourceVariant actualRemote = commonAssertionsForRemoteTree(
				grvts.getRemoteTree(), changedFile);

		// our common ancestor is the initial commit
		assertEquals(initialCommit.abbreviate(7).name() + "... (J. Git)",
				actualBase.getContentIdentifier());

		// while the remote should be master
		assertEquals(commitMaster.abbreviate(7).name() + "... (J. Git)",
				actualRemote.getContentIdentifier());
	}

	private GitResourceVariantTreeSubscriber createGitResourceVariantTreeSubscriber(
			String src, String dst) throws IOException {
		GitSynchronizeData gsd = new GitSynchronizeData(
				testRepo.getRepository(), src, dst, false);
		GitSynchronizeDataSet gsds = new GitSynchronizeDataSet(gsd);
		new GitResourceVariantTreeSubscriber(gsds);
		return new GitResourceVariantTreeSubscriber(gsds);
	}

	private IResourceVariant commonAssertionsForBaseTree(
			IResourceVariantTree baseTree, IResource resource)
			throws TeamException {
		assertNotNull(baseTree);
		assertTrue(baseTree instanceof GitBaseResourceVariantTree);
		IResourceVariant resourceVariant = baseTree
				.getResourceVariant(resource);
		assertNotNull(resourceVariant);
		assertTrue(resourceVariant instanceof GitRemoteResource);
		return resourceVariant;
	}

	private IResourceVariant commonAssertionsForRemoteTree(
			IResourceVariantTree baseTree, IResource resource)
			throws TeamException {
		assertNotNull(baseTree);
		assertTrue(baseTree instanceof GitRemoteResourceVariantTree);
		IResourceVariant resourceVariant = baseTree
				.getResourceVariant(resource);
		assertNotNull(resourceVariant);
		assertTrue(resourceVariant instanceof GitRemoteResource);
		return resourceVariant;
	}
}
