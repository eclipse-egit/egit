/*******************************************************************************
 * Copyright (C) 2015 Obeo and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.merge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.NameConflictTreeWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.team.core.variants.IResourceVariant;
import org.junit.Test;

public class TreeWalkResourceVariantTreeProviderTest extends VariantsTestCase {
	@Test
	public void testTreeWalkTrees() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1");
		File file2 = testRepo.createFile(iProject, "file2");

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_1,
				"first file - initial commit");
		RevCommit baseCommit = testRepo.appendContentAndCommit(iProject, file2,
				INITIAL_CONTENT_2, "second file - initial commit");

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

		// as if we tried to merge branch into master
		RevWalk walk = new RevWalk(repo);
		RevTree baseTree = walk.parseTree(baseCommit.getId());
		RevTree sourceTree = walk.parseTree(repo.resolve(MASTER));
		RevTree remoteTree = walk.parseTree(repo.resolve(BRANCH));
		TreeWalk tw = new NameConflictTreeWalk(repo);
		tw.addTree(baseTree);
		tw.addTree(sourceTree);
		tw.addTree(remoteTree);
		TreeWalkResourceVariantTreeProvider treeProvider = new TreeWalkResourceVariantTreeProvider(
				repo, tw, 0, 1, 2);

		assertEquals(1, treeProvider.getRoots().size());
		assertTrue(treeProvider.getRoots().contains(iProject));

		assertTrue(treeProvider.getKnownResources().contains(iFile1));
		assertTrue(treeProvider.getKnownResources().contains(iFile2));

		IResourceVariant file1BaseVariant = treeProvider.getBaseTree()
				.getResourceVariant(iFile1);
		IResourceVariant file2BaseVariant = treeProvider.getBaseTree()
				.getResourceVariant(iFile2);
		assertContentEquals(file1BaseVariant, INITIAL_CONTENT_1);
		assertContentEquals(file2BaseVariant, INITIAL_CONTENT_2);

		IResourceVariant file1TheirsVariant = treeProvider.getRemoteTree()
				.getResourceVariant(iFile1);
		IResourceVariant file2TheirsVariant = treeProvider.getRemoteTree()
				.getResourceVariant(iFile2);
		assertContentEquals(file1TheirsVariant, INITIAL_CONTENT_1);
		assertContentEquals(file2TheirsVariant, branchChanges
				+ INITIAL_CONTENT_2);

		IResourceVariant file1OursVariant = treeProvider.getSourceTree()
				.getResourceVariant(iFile1);
		IResourceVariant file2OursVariant = treeProvider.getSourceTree()
				.getResourceVariant(iFile2);
		assertContentEquals(file1OursVariant, INITIAL_CONTENT_1 + masterChanges);
		assertContentEquals(file2OursVariant, INITIAL_CONTENT_2);
	}
}
