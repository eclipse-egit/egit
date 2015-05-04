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
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.internal.storage.AbstractGitResourceVariant;
import org.eclipse.egit.core.internal.storage.IndexResourceVariant;
import org.eclipse.egit.core.internal.storage.TreeParserResourceVariant;
import org.eclipse.egit.core.op.MergeOperation;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.Test;

public class ResourceVariantTest extends VariantsTestCase {
	private final static String BASE_BRANCH = "base";

	private final static String BRANCH_CHANGE = "branch changes\n";

	private final static String MASTER_CHANGE = "master changes\n";

	@Test
	public void testIndexVariants() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1");
		File file2 = testRepo.createFile(iProject, "file2");
		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);

		setupUnconflictingBranches();

		List<String> possibleNames = Arrays.asList(iFile1.getName(),
				iFile2.getName());
		DirCache cache = repo.readDirCache();
		for (int i = 0; i < cache.getEntryCount(); i++) {
			final DirCacheEntry entry = cache.getEntry(i);

			AbstractGitResourceVariant variant = IndexResourceVariant.create(
					repo, entry);

			assertEquals(entry.getObjectId().getName(),
					variant.getContentIdentifier());
			assertTrue(possibleNames.contains(variant.getName()));
			assertEquals(entry.getObjectId(), variant.getObjectId());
			assertEquals(entry.getRawMode(), variant.getRawMode());
			if (iFile1.getName().equals(variant.getName())) {
				assertContentEquals(variant, INITIAL_CONTENT_1 + MASTER_CHANGE);
			} else {
				assertContentEquals(variant, INITIAL_CONTENT_2 + MASTER_CHANGE);
			}
		}

		testRepo.checkoutBranch(BRANCH);

		cache = repo.readDirCache();
		for (int i = 0; i < cache.getEntryCount(); i++) {
			final DirCacheEntry entry = cache.getEntry(i);

			AbstractGitResourceVariant variant = IndexResourceVariant.create(
					repo, entry);
			assertEquals(entry.getObjectId().getName(),
					variant.getContentIdentifier());
			assertTrue(possibleNames.contains(variant.getName()));
			assertEquals(entry.getObjectId(), variant.getObjectId());
			assertEquals(entry.getRawMode(), variant.getRawMode());
			if (iFile1.getName().equals(variant.getName())) {
				assertContentEquals(variant, BRANCH_CHANGE + INITIAL_CONTENT_1);
			} else {
				assertContentEquals(variant, BRANCH_CHANGE + INITIAL_CONTENT_2);
			}
		}
	}

	@Test
	public void testIndexVariantsConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1");
		IFile iFile1 = testRepo.getIFile(iProject, file1);

		setupConflictingBranches();
		// end setup

		// create a conflict to force multiple stages
		new MergeOperation(repo, BRANCH).execute(null);

		DirCache cache = repo.readDirCache();
		// 3 stages for file 1, 2 stages for file 2
		assertEquals(5, cache.getEntryCount());
		for (int i = 0; i < cache.getEntryCount(); i++) {
			final DirCacheEntry entry = cache.getEntry(i);

			AbstractGitResourceVariant variant = IndexResourceVariant.create(
					repo, entry);
			assertEquals(entry.getObjectId().getName(),
					variant.getContentIdentifier());
			assertEquals(entry.getObjectId(), variant.getObjectId());
			assertEquals(entry.getRawMode(), variant.getRawMode());
			if (iFile1.getName().equals(variant.getName())) {
				switch (entry.getStage()) {
				case DirCacheEntry.STAGE_1:
					assertContentEquals(variant, INITIAL_CONTENT_1);
					break;
				case DirCacheEntry.STAGE_2:
					assertContentEquals(variant, INITIAL_CONTENT_1
							+ MASTER_CHANGE);
					break;
				case DirCacheEntry.STAGE_3:
					assertContentEquals(variant, BRANCH_CHANGE
							+ INITIAL_CONTENT_1);
					break;
				case DirCacheEntry.STAGE_0:
				default:
					fail("Unexpected entry stage " + entry.getStage()
							+ " in the index for file " + entry.getPathString());
					break;
				}
			} else {
				switch (entry.getStage()) {
				case DirCacheEntry.STAGE_2:
					assertContentEquals(variant, INITIAL_CONTENT_2
							+ MASTER_CHANGE);
					break;
				case DirCacheEntry.STAGE_3:
					assertContentEquals(variant, BRANCH_CHANGE
							+ INITIAL_CONTENT_2);
					break;
				case DirCacheEntry.STAGE_0:
				case DirCacheEntry.STAGE_1:
				default:
					fail("Unexpected entry stage " + entry.getStage()
							+ " in the index for file " + entry.getPathString());
					break;
				}
			}
		}
	}

	@Test
	public void testTreeWalkBranchVariants() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1");
		IFile iFile1 = testRepo.getIFile(iProject, file1);

		setupUnconflictingBranches();

		ObjectId baseId = repo.resolve(BRANCH);
		RevWalk walk = new RevWalk(repo);
		TreeWalk tw = new TreeWalk(repo);
		tw.addTree(walk.parseTree(baseId));

		while (tw.next()) {
			AbstractGitResourceVariant variant = TreeParserResourceVariant
					.create(repo, tw.getTree(0, CanonicalTreeParser.class));

			assertEquals(tw.getObjectId(0).getName(),
					variant.getContentIdentifier());
			assertEquals(tw.getObjectId(0), variant.getObjectId());
			assertEquals(tw.getRawMode(0), variant.getRawMode());
			if (iFile1.getName().equals(variant.getName())) {
				assertContentEquals(variant, BRANCH_CHANGE + INITIAL_CONTENT_1);
			} else if (!tw.isSubtree()) {
				assertContentEquals(variant, BRANCH_CHANGE + INITIAL_CONTENT_2);
			}

			if (tw.isSubtree()) {
				tw.enterSubtree();
			}
		}
	}

	@Test
	public void testTreeWalkMasterVariants() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1");
		IFile iFile1 = testRepo.getIFile(iProject, file1);

		setupUnconflictingBranches();

		ObjectId baseId = repo.resolve(MASTER);
		RevWalk walk = new RevWalk(repo);
		TreeWalk tw = new TreeWalk(repo);
		tw.addTree(walk.parseTree(baseId));

		while (tw.next()) {
			AbstractGitResourceVariant variant = TreeParserResourceVariant
					.create(repo, tw.getTree(0, CanonicalTreeParser.class));

			assertEquals(tw.getObjectId(0).getName(),
					variant.getContentIdentifier());
			assertEquals(tw.getObjectId(0), variant.getObjectId());
			assertEquals(tw.getRawMode(0), variant.getRawMode());
			if (iFile1.getName().equals(variant.getName())) {
				assertContentEquals(variant, INITIAL_CONTENT_1 + MASTER_CHANGE);
			} else if (!tw.isSubtree()) {
				assertContentEquals(variant, INITIAL_CONTENT_2 + MASTER_CHANGE);
			}

			if (tw.isSubtree()) {
				tw.enterSubtree();
			}
		}
	}

	@Test
	public void testTreeWalkBaseVariants() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1");
		IFile iFile1 = testRepo.getIFile(iProject, file1);

		setupUnconflictingBranches();

		ObjectId baseId = repo.resolve(BASE_BRANCH);
		RevWalk walk = new RevWalk(repo);
		TreeWalk tw = new TreeWalk(repo);
		tw.addTree(walk.parseTree(baseId));

		while (tw.next()) {
			AbstractGitResourceVariant variant = TreeParserResourceVariant
					.create(repo, tw.getTree(0, CanonicalTreeParser.class));

			assertEquals(tw.getObjectId(0).getName(),
					variant.getContentIdentifier());
			assertEquals(tw.getObjectId(0), variant.getObjectId());
			assertEquals(tw.getRawMode(0), variant.getRawMode());
			if (iFile1.getName().equals(variant.getName())) {
				assertContentEquals(variant, INITIAL_CONTENT_1);
			} else if (!tw.isSubtree()) {
				fail("file2 shouldn't exist in our base.");
			}

			if (tw.isSubtree()) {
				tw.enterSubtree();
			}
		}
	}

	private void setupUnconflictingBranches() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1");
		File file2 = testRepo.createFile(iProject, "file2");
		IFile iFile1 = testRepo.getIFile(iProject, file1);

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_1,
				"first file - initial commit");

		testRepo.createBranch(MASTER, BASE_BRANCH);
		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(testRepo, iFile1, BRANCH_CHANGE
				+ INITIAL_CONTENT_1, "branch commit");
		testRepo.appendContentAndCommit(iProject, file2, BRANCH_CHANGE
				+ INITIAL_CONTENT_2, "second file - initial commit - branch");

		testRepo.checkoutBranch(MASTER);

		setContentsAndCommit(testRepo, iFile1, INITIAL_CONTENT_1
				+ MASTER_CHANGE, "master commit - file1");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_2
				+ MASTER_CHANGE, "second file - initial commit - master");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
	}

	private void setupConflictingBranches() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1");
		File file2 = testRepo.createFile(iProject, "file2");
		IFile iFile1 = testRepo.getIFile(iProject, file1);

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_1,
				"first file - initial commit");

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(testRepo, iFile1, BRANCH_CHANGE
				+ INITIAL_CONTENT_1, "branch commit");
		testRepo.appendContentAndCommit(iProject, file2, BRANCH_CHANGE
				+ INITIAL_CONTENT_2, "second file - initial commit - branch");

		testRepo.checkoutBranch(MASTER);

		setContentsAndCommit(testRepo, iFile1, INITIAL_CONTENT_1
				+ MASTER_CHANGE, "master commit - file1");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_2
				+ MASTER_CHANGE, "second file - initial commit - master");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
	}
}
