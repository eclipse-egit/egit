/*******************************************************************************
 * Copyright (C) 2015 Obeo and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.merge;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.op.MergeOperation;
import org.junit.Test;

public class DirCacheResourceVariantTreeProviderTest extends VariantsTestCase {
	@Test
	public void testDirCacheAddToIndex() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1");
		IFile iFile1 = testRepo.getIFile(iProject, file1);

		testRepo.appendFileContent(file1, INITIAL_CONTENT_1);

		// untracked file : not part of the index
		DirCacheResourceVariantTreeProvider treeProvider = new DirCacheResourceVariantTreeProvider(
				repo);
		assertTrue(treeProvider.getKnownResources().isEmpty());
		assertFalse(treeProvider.getBaseTree().hasResourceVariant(iFile1));
		assertFalse(treeProvider.getSourceTree().hasResourceVariant(iFile1));
		assertFalse(treeProvider.getRemoteTree().hasResourceVariant(iFile1));

		testRepo.addToIndex(iFile1);

		// We now have a stage 0, but this isn't represented in the resource
		// variant tree provider
		treeProvider = new DirCacheResourceVariantTreeProvider(repo);
		assertTrue(treeProvider.getKnownResources().isEmpty());
		assertFalse(treeProvider.getBaseTree().hasResourceVariant(iFile1));
		assertFalse(treeProvider.getSourceTree().hasResourceVariant(iFile1));
		assertFalse(treeProvider.getRemoteTree().hasResourceVariant(iFile1));
	}

	@Test
	public void testDirCacheTreesNoConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1");
		File file2 = testRepo.createFile(iProject, "file2");

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_2,
				"second file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		final String branchChanges = "branch changes\n";
		setContentsAndCommit(testRepo, iFile2, branchChanges
				+ INITIAL_CONTENT_2, "branch commit");

		testRepo.checkoutBranch(MASTER);

		final String masterChanges = "\nsome changes";
		setContentsAndCommit(testRepo, iFile1, INITIAL_CONTENT_1
				+ masterChanges, "master commit");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		// try and merge the branch into master
		new MergeOperation(repo, BRANCH).execute(null);

		// no conflict on either file : nothing in the trees
		DirCacheResourceVariantTreeProvider treeProvider = new DirCacheResourceVariantTreeProvider(
				repo);
		assertTrue(treeProvider.getKnownResources().isEmpty());

		assertFalse(treeProvider.getBaseTree().hasResourceVariant(iFile1));
		assertFalse(treeProvider.getBaseTree().hasResourceVariant(iFile2));

		assertFalse(treeProvider.getSourceTree().hasResourceVariant(iFile1));
		assertFalse(treeProvider.getSourceTree().hasResourceVariant(iFile2));

		assertFalse(treeProvider.getRemoteTree().hasResourceVariant(iFile1));
		assertFalse(treeProvider.getRemoteTree().hasResourceVariant(iFile2));
	}

	@Test
	public void testDirCacheTreesConflictOnOne() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1");
		File file2 = testRepo.createFile(iProject, "file2");

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_2,
				"second file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		final String branchChanges = "branch changes\n";
		setContentsAndCommit(testRepo, iFile1, branchChanges
				+ INITIAL_CONTENT_1, "branch commit");
		setContentsAndCommit(testRepo, iFile2, branchChanges
				+ INITIAL_CONTENT_2, "branch commit");

		testRepo.checkoutBranch(MASTER);

		final String masterChanges = "\nsome changes";
		setContentsAndCommit(testRepo, iFile1, INITIAL_CONTENT_1
				+ masterChanges, "master commit");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		// try and merge the branch into master
		new MergeOperation(repo, BRANCH).execute(null);

		// conflict on file 1 : present in all three trees
		// no conflict on file 2 : not present in any tree
		DirCacheResourceVariantTreeProvider treeProvider = new DirCacheResourceVariantTreeProvider(
				repo);
		assertTrue(treeProvider.getKnownResources().contains(iFile1));
		assertFalse(treeProvider.getKnownResources().contains(iFile2));

		assertTrue(treeProvider.getBaseTree().hasResourceVariant(iFile1));
		assertFalse(treeProvider.getBaseTree().hasResourceVariant(iFile2));

		assertTrue(treeProvider.getSourceTree().hasResourceVariant(iFile1));
		assertFalse(treeProvider.getSourceTree().hasResourceVariant(iFile2));

		assertTrue(treeProvider.getRemoteTree().hasResourceVariant(iFile1));
		assertFalse(treeProvider.getRemoteTree().hasResourceVariant(iFile2));
	}

	@Test
	public void testDirCacheTreesConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1");
		File file2 = testRepo.createFile(iProject, "file2");

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_1,
				"first file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		final String branchChanges = "branch changes\n";
		setContentsAndCommit(testRepo, iFile1, branchChanges
				+ INITIAL_CONTENT_1, "branch commit");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_2
				+ "branch", "second file - initial commit - branch");

		testRepo.checkoutBranch(MASTER);

		final String masterChanges = "some changes\n";
		setContentsAndCommit(testRepo, iFile1, INITIAL_CONTENT_1
				+ masterChanges, "master commit - file1");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_2
				+ "master", "second file - initial commit - master");
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		// try and merge the branch into master
		new MergeOperation(repo, BRANCH).execute(null);

		// conflict on file 1 : file 1 has three stages.
		// conflict on file 2, but was not in the base : only stage 2 and 3
		DirCacheResourceVariantTreeProvider treeProvider = new DirCacheResourceVariantTreeProvider(
				repo);
		assertTrue(treeProvider.getKnownResources().contains(iFile1));
		assertTrue(treeProvider.getKnownResources().contains(iFile2));

		assertTrue(treeProvider.getBaseTree().hasResourceVariant(iFile1));
		assertFalse(treeProvider.getBaseTree().hasResourceVariant(iFile2));

		assertTrue(treeProvider.getSourceTree().hasResourceVariant(iFile1));
		assertTrue(treeProvider.getSourceTree().hasResourceVariant(iFile2));

		assertTrue(treeProvider.getRemoteTree().hasResourceVariant(iFile1));
		assertTrue(treeProvider.getRemoteTree().hasResourceVariant(iFile2));
	}
}
