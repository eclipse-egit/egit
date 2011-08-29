/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantTree;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class GitResourceVariantTreeSubscriberTest extends GitTestCase {

	private Repository repo;

	private IProject iProject;

	private TestRepository testRepo;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		iProject = project.getProject();
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

	/**
	 * This test simulates that user work and made some changes on branch 'test'
	 * and then try to synchronize "test" and 'master' branch.
	 *
	 * @throws Exception
	 */
	@Test
	public void shouldReturnSrcBranchAsBase() throws Exception {
		// when
		String fileName = "Main.java";
		File file = testRepo.createFile(iProject, fileName);
		RevCommit commit = testRepo.appendContentAndCommit(iProject, file,
				"class Main {}", "initial commit");
		IFile mainJava = testRepo.getIFile(iProject, file);
		testRepo.createAndCheckoutBranch(Constants.HEAD, Constants.R_HEADS
				+ "test");
		testRepo.appendContentAndCommit(iProject, file, "// test1",
				"secont commit");

		// given
		GitResourceVariantTreeSubscriber grvts = createGitResourceVariantTreeSubscriber(
				Constants.HEAD, Constants.R_HEADS + Constants.MASTER);
		grvts.init(new NullProgressMonitor());
		IResourceVariantTree baseTree = grvts.getBaseTree();

		// then
		IResourceVariant actual = commonAssertionsForBaseTree(baseTree,
				mainJava);
		assertEquals(commit.abbreviate(7).name() + "...",
				actual.getContentIdentifier());
	}

	/**
	 * Both source and destination branches has some different commits but they
	 * has also common ancestor. This situation is described more detailed in
	 * bug #317934
	 *
	 * This test passes when it is run as a stand alone test case, but it fails
	 * when it is run as part of test suite. It throws NPE when it try's to
	 * checkout master branch.
	 *
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void shouldReturnCommonAncestorAsBase() throws Exception {
		// when
		String fileName = "Main.java";
		File file = testRepo.createFile(iProject, fileName);
		RevCommit commit = testRepo.appendContentAndCommit(iProject, file,
				"class Main {}", "initial commit");
		IFile mainJava = testRepo.getIFile(iProject, file);
		// this should be our common ancestor
		ObjectId fileId = findFileId(commit, mainJava);

		testRepo.createAndCheckoutBranch(Constants.HEAD, Constants.R_HEADS
				+ "test");
		testRepo.appendContentAndCommit(iProject, file, "// test 1",
				"second commit");

		testRepo.checkoutBranch(Constants.R_HEADS + Constants.MASTER);
		testRepo.appendContentAndCommit(iProject, file, "// test 2",
				"third commit");

		// given
		GitResourceVariantTreeSubscriber grvts = createGitResourceVariantTreeSubscriber(
				Constants.HEAD, Constants.R_HEADS + "test");
		grvts.getBaseTree();
		IResourceVariantTree baseTree = grvts.getBaseTree();

		// then
		IResourceVariant actual = commonAssertionsForBaseTree(baseTree,
				mainJava);
		assertEquals(fileId.getName(), actual.getContentIdentifier());
	}

	/**
	 * This test passes when it is run as a stand alone test case, but it fails
	 * when it is run as part of test suite. It throws NPE when it try's to
	 * checkout master branch.
	 *
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void shouldReturnRemoteTree() throws Exception {
		// when
		String fileName = "Main.java";
		File file = testRepo.createFile(iProject, fileName);
		testRepo.appendContentAndCommit(iProject, file,
				"class Main {}", "initial commit");
		IFile mainJava = testRepo.getIFile(iProject, file);

		testRepo.createAndCheckoutBranch(Constants.HEAD, Constants.R_HEADS
				+ "test");
		RevCommit commit = testRepo.appendContentAndCommit(iProject, file, "// test 1",
				"second commit");
		ObjectId fileId = findFileId(commit, mainJava);

		// given
		GitResourceVariantTreeSubscriber grvts = createGitResourceVariantTreeSubscriber(
				Constants.HEAD, "test");
		grvts.getBaseTree();
		IResourceVariantTree remoteTree = grvts.getRemoteTree();

		// then
		assertNotNull(remoteTree);
		assertTrue(remoteTree instanceof GitRemoteResourceVariantTree);
		IResourceVariant resourceVariant = remoteTree
				.getResourceVariant(mainJava);
		assertNotNull(resourceVariant);
		assertTrue(resourceVariant instanceof GitRemoteResource);
		assertEquals(fileId.getName(), resourceVariant.getContentIdentifier());
	}

	private GitResourceVariantTreeSubscriber createGitResourceVariantTreeSubscriber(
			String src, String dst) throws IOException {
		GitSynchronizeData gsd = new GitSynchronizeData(repo, src, dst, false);
		GitSynchronizeDataSet gsds = new GitSynchronizeDataSet(gsd);
		new GitResourceVariantTreeSubscriber(gsds);
		return new GitResourceVariantTreeSubscriber(gsds);
	}

	private ObjectId findFileId(RevCommit commit, IFile mainJava)
			throws Exception {
		TreeWalk tw = new TreeWalk(repo);
		tw.reset();
		tw.setRecursive(true);
		String path = Repository.stripWorkDir(repo.getWorkTree(), mainJava
				.getLocation().toFile());
		tw.setFilter(PathFilter.create(path));
		int nth = tw.addTree(commit.getTree());
		tw.next();

		return tw.getObjectId(nth);
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

}
