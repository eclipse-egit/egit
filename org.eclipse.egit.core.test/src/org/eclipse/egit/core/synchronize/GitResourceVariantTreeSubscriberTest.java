/*******************************************************************************
 * Copyright (C) 2010, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
	private static final String MASTER = Constants.R_HEADS + Constants.MASTER;

	private static final String BRANCH = Constants.R_HEADS + "branch";

	private TestRepository testRepo;

	private RevCommit initialCommit;

	private RevCommit commitBranch;

	private RevCommit commitMaster;

	private IFile changedFile;

	@Override
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

		testRepo.createAndCheckoutBranch(Constants.HEAD, BRANCH);
		commitBranch = testRepo.appendContentAndCommit(project.getProject(),
				file, "// test 1", "second commit");

		testRepo.checkoutBranch(MASTER);
		commitMaster = testRepo.appendContentAndCommit(project.getProject(),
				file, "// test 2", "third commit");
	}

	@After
	public void clearGitResources() throws Exception {
		testRepo.dispose();
		super.tearDown();
	}

	@Test
	public void testSyncLocalAndBranch() throws Exception {
		// Note that HEAD is on master
		GitResourceVariantTreeSubscriber grvts = createGitResourceVariantTreeSubscriber(
				Constants.HEAD, BRANCH, true);
		grvts.init(new NullProgressMonitor());

		IResourceVariant actualSource = getSourceVariant(grvts, changedFile,
				true);
		IResourceVariant actualBase = getBaseVariant(grvts, changedFile);
		IResourceVariant actualRemote = getRemoteVariant(grvts, changedFile);

		assertVariantIsLocal(actualSource, changedFile);
		assertVariantMatchCommit(actualBase, initialCommit);
		assertVariantMatchCommit(actualRemote, commitBranch);
	}

	@Test
	public void testSyncMasterAndBranch() throws Exception {
		GitResourceVariantTreeSubscriber grvts = createGitResourceVariantTreeSubscriber(
				MASTER, BRANCH, false);
		grvts.init(new NullProgressMonitor());

		IResourceVariant actualSource = getSourceVariant(grvts, changedFile,
				false);
		IResourceVariant actualBase = getBaseVariant(grvts, changedFile);
		IResourceVariant actualRemote = getRemoteVariant(grvts, changedFile);

		assertVariantMatchCommit(actualSource, commitMaster);
		assertVariantMatchCommit(actualBase, initialCommit);
		assertVariantMatchCommit(actualRemote, commitBranch);
	}

	@Test
	public void testSyncBranchAndMaster() throws Exception {
		GitResourceVariantTreeSubscriber grvts = createGitResourceVariantTreeSubscriber(
				BRANCH, MASTER, false);
		grvts.init(new NullProgressMonitor());

		IResourceVariant actualSource = getSourceVariant(grvts, changedFile,
				false);
		IResourceVariant actualBase = getBaseVariant(grvts, changedFile);
		IResourceVariant actualRemote = getRemoteVariant(grvts, changedFile);

		assertVariantMatchCommit(actualSource, commitBranch);
		assertVariantMatchCommit(actualBase, initialCommit);
		assertVariantMatchCommit(actualRemote, commitMaster);
	}

	private void assertVariantIsLocal(IResourceVariant variant, IResource local) {
		assertTrue(variant instanceof GitLocalResourceVariant);
		assertEquals(local, ((GitLocalResourceVariant) variant).getResource());
	}

	private void assertVariantMatchCommit(IResourceVariant variant,
			RevCommit commit) {
		assertTrue(variant instanceof GitRemoteResource);
		assertEquals(commit, ((GitRemoteResource) variant).getCommitId());
	}

	private GitResourceVariantTreeSubscriber createGitResourceVariantTreeSubscriber(
			String src, String dst, boolean includeLocal) throws IOException {
		GitSynchronizeData gsd = new GitSynchronizeData(
				testRepo.getRepository(), src, dst, includeLocal);
		GitSynchronizeDataSet gsds = new GitSynchronizeDataSet(gsd);
		return new GitResourceVariantTreeSubscriber(gsds);
	}

	private IResourceVariant getSourceVariant(
			GitResourceVariantTreeSubscriber subscriber, IResource resource,
			boolean includeLocal) throws TeamException {
		IResourceVariantTree tree = subscriber.getSourceTree();
		assertNotNull(tree);
		assertTrue(tree instanceof GitSourceResourceVariantTree);
		IResourceVariant resourceVariant = tree.getResourceVariant(resource);
		assertNotNull(resourceVariant);
		if (includeLocal)
			assertTrue(resourceVariant instanceof GitLocalResourceVariant);
		else
			assertTrue(resourceVariant instanceof GitRemoteResource);
		return resourceVariant;
	}

	private IResourceVariant getBaseVariant(
			GitResourceVariantTreeSubscriber subscriber, IResource resource)
			throws TeamException {
		IResourceVariantTree tree = subscriber.getBaseTree();
		assertNotNull(tree);
		assertTrue(tree instanceof GitBaseResourceVariantTree);
		IResourceVariant resourceVariant = tree
				.getResourceVariant(resource);
		assertNotNull(resourceVariant);
		assertTrue(resourceVariant instanceof GitRemoteResource);
		return resourceVariant;
	}

	private IResourceVariant getRemoteVariant(
			GitResourceVariantTreeSubscriber subscriber, IResource resource)
			throws TeamException {
		IResourceVariantTree tree = subscriber.getRemoteTree();
		assertNotNull(tree);
		assertTrue(tree instanceof GitRemoteResourceVariantTree);
		IResourceVariant resourceVariant = tree.getResourceVariant(resource);
		assertNotNull(resourceVariant);
		assertTrue(resourceVariant instanceof GitRemoteResource);
		return resourceVariant;
	}
}
