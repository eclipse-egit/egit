/*******************************************************************************
 * Copyright (c) 2017 EclipseSource Services GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Fleck - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import static org.eclipse.jgit.junit.JGitTestUtil.write;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Test;

/**
 * A test class for testing {@link DecoratableResourceMapping}.
 *
 * @author Martin Fleck <mfleck@eclipsesource.com>
 * @see DecoratableWorkingSetTest DecoratableWorkingSetTest for specific working
 *      set tests
 */
public class DecoratableResourceMappingTest
		extends GitLightweightDecoratorTest {

	private static final String RM_CONTENT_A_FILE_NAME = "TestFile.testrm_A";

	private static final String RM_CONTENT_B_FILE_NAME = "TestFile.testrm_B";

	private IProject project;

	private Git git;

	private IndexDiffCacheEntry indexDiffCacheEntry;

	private File gitDir;

	private ResourceMapping resourceMapping;

	private IFile rmContentA, rmContentB;

	@Before
	public void setUp() throws Exception {
		gitDir = createProjectAndCommitToRepository();
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1);

		rmContentA = createFile(project, RM_CONTENT_A_FILE_NAME,
				"Just some content.");
		rmContentB = createFile(project, RM_CONTENT_B_FILE_NAME,
				"Just some content.");
		resourceMapping = new TestResourceMapping(project, rmContentA,
				rmContentB);

		ResourceTraversal[] traversals = resourceMapping.getTraversals(null,
				null);
		assertEquals(1, traversals.length);
		ResourceTraversal traversal = traversals[0];
		assertEquals(2, traversal.getResources().length);
		assertTrue(traversal.contains(rmContentA));
		assertTrue(traversal.contains(rmContentB));

		Repository repo = lookupRepository(gitDir);
		git = new Git(repo);
		indexDiffCacheEntry = org.eclipse.egit.core.Activator.getDefault()
				.getIndexDiffCache().getIndexDiffCacheEntry(repo);
		waitForIndexDiff(false);
	}

	private IndexDiffData waitForIndexDiff(final boolean refreshCache)
			throws Exception {
		if (refreshCache)
			indexDiffCacheEntry.refresh();
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		return indexDiffCacheEntry.getIndexDiff();
	}

	@Test
	public void testNewResourceMappingIsUnstaged() throws Exception {
		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				newExpectedDecoratableResource(rmContentA),
				newExpectedDecoratableResource(rmContentB),
				newExpectedDecoratableResourceMapping() };

		IndexDiffData indexDiffData = waitForIndexDiff(true);
		IDecoratableResource[] actualDRs = {
				newDecoratableResource(indexDiffData, rmContentA),
				newDecoratableResource(indexDiffData, rmContentB),
				newDecoratableResourceMapping(resourceMapping) };

		assertArrayEquals(expectedDRs, actualDRs);
		assertDecorationUntracked(resourceMapping);
	}

	@Test
	public void testMixedStagingStateIsModified_AddedNotStaged()
			throws Exception {
		gitAdd(git, rmContentA);

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				newExpectedDecoratableResource(rmContentA).tracked().added(),
				newExpectedDecoratableResource(rmContentB),
				newExpectedDecoratableResourceMapping().tracked().modified() };

		IndexDiffData indexDiffData = waitForIndexDiff(true);
		IDecoratableResource[] actualDRs = {
				newDecoratableResource(indexDiffData, rmContentA),
				newDecoratableResource(indexDiffData, rmContentB),
				newDecoratableResourceMapping(resourceMapping) };

		assertArrayEquals(expectedDRs, actualDRs);
		assertDecorationStaged(resourceMapping);
	}

	@Test
	public void testMixedStagingStateIsModified_RemovedAdded()
			throws Exception {
		gitAdd(git, rmContentA);
		gitCommit(git);
		gitRemove(git, rmContentA);
		gitAdd(git, rmContentB);

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				newExpectedDecoratableResource(rmContentA).tracked().removed(),
				newExpectedDecoratableResource(rmContentB).tracked().added(),
				newExpectedDecoratableResourceMapping().tracked().modified() };

		IndexDiffData indexDiffData = waitForIndexDiff(true);
		IDecoratableResource[] actualDRs = {
				newDecoratableResource(indexDiffData, rmContentA),
				newDecoratableResource(indexDiffData, rmContentB),
				newDecoratableResourceMapping(resourceMapping) };

		assertArrayEquals(expectedDRs, actualDRs);
		assertDecorationStaged(resourceMapping);
	}

	@Test
	public void testSameStagingStateIsState_Added() throws Exception {
		gitAdd(git, rmContentA);
		gitAdd(git, rmContentB);

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				newExpectedDecoratableResource(rmContentA).tracked().added(),
				newExpectedDecoratableResource(rmContentB).tracked().added(),
				newExpectedDecoratableResourceMapping().tracked().added() };

		IndexDiffData indexDiffData = waitForIndexDiff(true);
		IDecoratableResource[] actualDRs = {
				newDecoratableResource(indexDiffData, rmContentA),
				newDecoratableResource(indexDiffData, rmContentB),
				newDecoratableResourceMapping(resourceMapping) };

		assertArrayEquals(expectedDRs, actualDRs);
		assertDecorationAdded(resourceMapping);
	}

	@Test
	public void testAnyTrackedIsTracked() throws Exception {
		gitAdd(git, rmContentA);
		gitCommit(git);

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				newExpectedDecoratableResource(rmContentA).tracked(),
				newExpectedDecoratableResource(rmContentB),
				newExpectedDecoratableResourceMapping().tracked() };
		IndexDiffData indexDiffData = waitForIndexDiff(true);
		IDecoratableResource[] actualDRs = {
				newDecoratableResource(indexDiffData, rmContentA),
				newDecoratableResource(indexDiffData, rmContentB),
				newDecoratableResourceMapping(resourceMapping) };

		assertArrayEquals(expectedDRs, actualDRs);
		assertDecorationTracked(resourceMapping);
	}

	@Test
	public void testAllIgnoredIsUnstaged() throws Exception {
		IFile gitignore = createFile(project, ".gitignore", "*.testrm_*");
		gitAdd(git, gitignore);
		gitAdd(git, rmContentA);
		gitAdd(git, rmContentB);
		gitCommit(git);

		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		rmContentA = findFile(project, RM_CONTENT_A_FILE_NAME);
		gitignore = findFile(project, ".gitignore");

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				newExpectedDecoratableResource(gitignore).tracked(),
				newExpectedDecoratableResource(rmContentA).ignored(),
				newExpectedDecoratableResource(rmContentB).ignored(),
				newExpectedDecoratableResourceMapping() };

		IndexDiffData indexDiffData = waitForIndexDiff(true);
		IDecoratableResource[] actualDRs = {
				newDecoratableResource(indexDiffData, gitignore),
				newDecoratableResource(indexDiffData, rmContentA),
				newDecoratableResource(indexDiffData, rmContentB),
				newDecoratableResourceMapping(resourceMapping) };

		assertArrayEquals(expectedDRs, actualDRs);
		assertDecorationUntracked(resourceMapping);
	}

	@Test
	public void testAnyDirtyIsDirty() throws Exception {
		gitAdd(git, rmContentA);
		gitAdd(git, rmContentB);
		gitCommit(git);
		write(rmContentA.getLocation().toFile(), "Changed content");

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				newExpectedDecoratableResource(rmContentA).tracked().dirty(),
				newExpectedDecoratableResource(rmContentB).tracked(),
				newExpectedDecoratableResourceMapping().tracked().dirty() };

		IndexDiffData indexDiffData = waitForIndexDiff(true);
		IDecoratableResource[] actualDRs = {
				newDecoratableResource(indexDiffData, rmContentA),
				newDecoratableResource(indexDiffData, rmContentB),
				newDecoratableResourceMapping(resourceMapping) };

		assertArrayEquals(expectedDRs, actualDRs);
		assertDecorationDirty(resourceMapping);
	}

	@Test
	public void testAnyConflictsIsConflicts() throws Exception {
		// commit changes on master
		gitAdd(git, rmContentA);
		gitAdd(git, rmContentB);
		RevCommit masterCommit = gitCommit(git);

		// add change on new branch first_topic
		git.checkout().setCreateBranch(true).setName("first_topic").call();
		rmContentA = findFile(project, RM_CONTENT_A_FILE_NAME);
		write(rmContentA.getLocation().toFile(), "First Topic Content");
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		gitAdd(git, rmContentA);
		RevCommit firstTopicCommit = gitCommit(git);

		// add change on new branch second_topic
		git.checkout().setCreateBranch(true).setStartPoint(masterCommit)
				.setName("second_topic").call();
		rmContentA = findFile(project, RM_CONTENT_A_FILE_NAME);
		write(rmContentA.getLocation().toFile(), "Second Topic Content");
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		gitAdd(git, rmContentA);
		gitCommit(git);

		// merge second_topic with first_topic
		MergeResult mergeResult = git.merge().include(firstTopicCommit).call();
		assertEquals(MergeStatus.CONFLICTING, mergeResult.getMergeStatus());

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				newExpectedDecoratableResource(rmContentA).tracked()
						.conflicts(),
				newExpectedDecoratableResource(rmContentB).tracked(),
				newExpectedDecoratableResourceMapping().tracked().conflicts() };
		IndexDiffData indexDiffData = waitForIndexDiff(true);
		IDecoratableResource[] actualDRs = {
				newDecoratableResource(indexDiffData, rmContentA),
				newDecoratableResource(indexDiffData, rmContentB),
				newDecoratableResourceMapping(resourceMapping) };

		assertArrayEquals(expectedDRs, actualDRs);
		assertDecorationConflicts(resourceMapping);
	}

	/**
	 * Tests that if the resource mapping is both conflicting and dirty, that
	 * the conflicting decoration image is used over the dirty decoration image.
	 *
	 * @throws Exception
	 */
	@Test
	public void testDecorationConflictingOverDirty() throws Exception {
		// commit changes on master
		gitAdd(git, rmContentA);
		gitAdd(git, rmContentB);
		RevCommit masterCommit = gitCommit(git);

		// add change on new branch first_topic
		git.checkout().setCreateBranch(true).setName("first_topic").call();
		rmContentA = findFile(project, RM_CONTENT_A_FILE_NAME);
		write(rmContentA.getLocation().toFile(), "First Topic Content");
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		gitAdd(git, rmContentA);
		RevCommit firstTopicCommit = gitCommit(git);

		// add change on new branch second_topic
		git.checkout().setCreateBranch(true).setStartPoint(masterCommit)
				.setName("second_topic").call();
		rmContentA = findFile(project, RM_CONTENT_A_FILE_NAME);
		write(rmContentA.getLocation().toFile(), "Second Topic Content");
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		gitAdd(git, rmContentA);
		gitCommit(git);

		// modify b to make it dirty
		write(rmContentB.getLocation().toFile(), "Changed content");

		// merge second_topic with first_topic
		MergeResult mergeResult = git.merge().include(firstTopicCommit).call();
		assertEquals(MergeStatus.CONFLICTING, mergeResult.getMergeStatus());

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				newExpectedDecoratableResource(rmContentA).tracked()
						.conflicts(),
				newExpectedDecoratableResource(rmContentB).tracked().dirty(),
				newExpectedDecoratableResourceMapping().tracked().dirty()
						.conflicts() };
		IndexDiffData indexDiffData = waitForIndexDiff(true);
		IDecoratableResource[] actualDRs = {
				newDecoratableResource(indexDiffData, rmContentA),
				newDecoratableResource(indexDiffData, rmContentB),
				newDecoratableResourceMapping(resourceMapping) };

		assertArrayEquals(expectedDRs, actualDRs);
		assertDecorationConflicts(resourceMapping);
	}
}