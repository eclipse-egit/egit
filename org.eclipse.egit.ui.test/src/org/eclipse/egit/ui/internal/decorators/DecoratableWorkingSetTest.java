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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.model.WorkingSetResourceMapping;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A test class for testing {@link DecoratableWorkingSet}.
 *
 * @author Martin Fleck <mfleck@eclipsesource.com>
 * @see DecoratableResourceMappingTest DecoratableResourceMappingTest for
 *      general resource mappings
 */
@SuppressWarnings("restriction")
public class DecoratableWorkingSetTest extends GitLightweightDecoratorTest {

	private static final String WORKING_SET_NAME = "TestWorkingSet";

	private static final IWorkingSetManager WORKING_SET_MANAGER = PlatformUI
			.getWorkbench().getWorkingSetManager();

	private static IWorkingSet WORKING_SET;
	static {
		WORKING_SET = WORKING_SET_MANAGER.getWorkingSet(WORKING_SET_NAME);
		if (WORKING_SET == null) {
			WORKING_SET = WORKING_SET_MANAGER.createWorkingSet(WORKING_SET_NAME,
					new IAdaptable[0]);
		}
	}

	private static final String TEST_FILE = "TestFile";

	private static final String TEST_FILE2 = "TestFile2";

	private ResourceMapping resourceMapping;

	private File gitDir;

	private IProject project1, project2;

	private Git git;

	private IndexDiffCacheEntry indexDiffCacheEntry;

	@BeforeClass
	public static void beforeClassBase() throws Exception {
		showAllIcons();
		WORKING_SET_MANAGER.addWorkingSet(WORKING_SET);
	}

	@AfterClass
	public static void afterClassBase() {
		WORKING_SET_MANAGER.removeWorkingSet(WORKING_SET);
	}

	@Before
	public void setUp() throws Exception {
		gitDir = createProjectAndCommitToRepository(REPO1, PROJ1, null);
		project1 = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1);
		project2 = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ2);
		project2.create(null);
		project2.open(null);
		TestUtil.waitForJobs(50, 5000);
		assertTrue("Project is not accessible: " + project2,
				project2.isAccessible());

		setWorkingSet(project1, project2);
		resourceMapping = createResourceMapping();

		Repository repo = lookupRepository(gitDir);
		git = new Git(repo);
		indexDiffCacheEntry = org.eclipse.egit.core.Activator.getDefault()
				.getIndexDiffCache().getIndexDiffCacheEntry(repo);
		waitForIndexDiff(false);
	}

	@After
	public void cleanUp() {
		clearWorkingSet();
	}

	private void clearWorkingSet() {
		WORKING_SET.setElements(new IAdaptable[0]);
	}

	private void setWorkingSet(IAdaptable... elements) {
		WORKING_SET.setElements(elements);
	}

	private IndexDiffData waitForIndexDiff(final boolean refreshCache)
			throws Exception {
		if (refreshCache)
			indexDiffCacheEntry.refresh();
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		return indexDiffCacheEntry.getIndexDiff();
	}

	private void assertHasUnstagedChanges(boolean expected,
			IDecoratableResource... decoratableResources) {
		for (IDecoratableResource d : decoratableResources) {
			assertTrue(d.hasUnstagedChanges() == expected);
		}
	}

	protected ResourceMapping createResourceMapping() {
		return new WorkingSetResourceMapping(WORKING_SET);
	}

	/**
	 * Tests that working sets do not show the untracked decoration, but instead
	 * they are just undecorated.
	 *
	 * @throws Exception
	 */
	@Test
	public void testEmptyWorkingSetIsUndecorated() throws Exception {
		clearWorkingSet();

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				newExpectedDecoratableWorkingSet(WORKING_SET) };
		IDecoratableResource[] actualDRs = {
				newDecoratableWorkingSet(resourceMapping) };

		assertArrayEquals(expectedDRs, actualDRs);
		assertUndecorated(resourceMapping);
	}

	@Test
	public void testUntrackedContentIsUndecorated() throws Exception {
		setWorkingSet(project2);

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				newExpectedDecoratableResource(project2),
				newExpectedDecoratableWorkingSet(WORKING_SET) };

		IndexDiffData indexDiffData = waitForIndexDiff(true);
		IDecoratableResource[] actualDRs = {
				newDecoratableResource(indexDiffData, project2),
				newDecoratableWorkingSet(resourceMapping) };

		assertArrayEquals(expectedDRs, actualDRs);
		assertUndecorated(resourceMapping);
	}

	@Test
	public void testAnyTrackedIsTracked() throws Exception {
		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				newExpectedDecoratableResource(project1).tracked(),
				newExpectedDecoratableResource(project2),
				newExpectedDecoratableWorkingSet(WORKING_SET).tracked() };

		IndexDiffData indexDiffData = waitForIndexDiff(true);
		IDecoratableResource[] actualDRs = {
				newDecoratableResource(indexDiffData, project1),
				newDecoratableResource(indexDiffData, project2),
				newDecoratableWorkingSet(resourceMapping) };

		assertArrayEquals(expectedDRs, actualDRs);
		assertDecorationTracked(resourceMapping);
	}

	@Test
	public void testStagingStateHasNoInfluence_Modified() throws Exception {
		IFile file = createFile(project1, TEST_FILE, "Something");
		gitAdd(git, file);

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				newExpectedDecoratableResource(project1).tracked().modified(),
				newExpectedDecoratableResource(file).tracked().added(),
				newExpectedDecoratableResource(project2),
				newExpectedDecoratableWorkingSet(WORKING_SET).tracked() };

		IndexDiffData indexDiffData = waitForIndexDiff(true);
		IDecoratableResource[] actualDRs = {
				newDecoratableResource(indexDiffData, project1),
				newDecoratableResource(indexDiffData, file),
				newDecoratableResource(indexDiffData, project2),
				newDecoratableWorkingSet(resourceMapping) };

		assertArrayEquals(expectedDRs, actualDRs);
		assertDecorationTracked(resourceMapping);
	}

	@Test
	public void testAnyDirtyIsDirty() throws Exception {
		// Create new file and commit
		IFile file = createFile(project1, TEST_FILE, "Something");
		gitAdd(git, file);
		gitCommit(git);

		// change file content to make it dirty
		write(file.getLocation().toFile(), "Change");

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				newExpectedDecoratableResource(project1).tracked().dirty(),
				newExpectedDecoratableResource(file).tracked().dirty(),
				newExpectedDecoratableWorkingSet(WORKING_SET).tracked()
						.dirty() };

		waitForIndexDiff(true);
		IndexDiffData indexDiffData = indexDiffCacheEntry.getIndexDiff();
		IDecoratableResource[] actualDRs = {
				newDecoratableResource(indexDiffData, project1),
				newDecoratableResource(indexDiffData, file),
				newDecoratableWorkingSet(resourceMapping) };

		assertArrayEquals(expectedDRs, actualDRs);
		assertHasUnstagedChanges(true, actualDRs);
		assertDecorationDirty(resourceMapping);
	}

	@Test
	public void testAnyConflictsIsConflicts() throws Exception {
		// Create new file and commit
		IFile file = createFile(project1, TEST_FILE, "Something");
		gitAdd(git, file);
		RevCommit masterCommit = gitCommit(git);

		// Create and checkout new branch, change file content, add and commit
		// file
		git.checkout().setCreateBranch(true).setName("first_topic").call();
		file = findFile(project1, TEST_FILE);
		write(file.getLocation().toFile(), "First Topic Content");
		project1.refreshLocal(IResource.DEPTH_INFINITE, null);
		gitAdd(git, file);
		RevCommit firstTopicCommit = gitCommit(git);

		// Create and checkout new branch (from master), change file content,
		// add and commit file
		git.checkout().setCreateBranch(true).setStartPoint(masterCommit)
				.setName("second_topic").call();
		file = findFile(project1, TEST_FILE);
		write(file.getLocation().toFile(), "Second Topic Content");
		project1.refreshLocal(IResource.DEPTH_INFINITE, null);
		gitAdd(git, file);
		gitCommit(git);

		// merge second_topic with first_topic
		MergeResult mergeResult = git.merge().include(firstTopicCommit).call();
		assertEquals(MergeStatus.CONFLICTING, mergeResult.getMergeStatus());

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				newExpectedDecoratableResource(project1).tracked()
						.conflicts(),
				newExpectedDecoratableResource(file).tracked().conflicts(),
				newExpectedDecoratableWorkingSet(WORKING_SET).tracked()
						.conflicts() };
		IndexDiffData indexDiffData = waitForIndexDiff(true);
		IDecoratableResource[] actualDRs = {
				newDecoratableResource(indexDiffData, project1),
				newDecoratableResource(indexDiffData, file),
				newDecoratableWorkingSet(resourceMapping) };

		assertArrayEquals(expectedDRs, actualDRs);
		assertHasUnstagedChanges(true, actualDRs);
		assertDecorationConflicts(resourceMapping);
	}

	@Test
	public void testDecorationConflictingOverDirty() throws Exception {
		// Create new file and commit
		IFile file = createFile(project1, TEST_FILE, "Something");
		IFile file2 = createFile(project1, TEST_FILE2, "Another Something");
		gitAdd(git, file);
		gitAdd(git, file2);
		RevCommit masterCommit = gitCommit(git);

		// Create and checkout new branch, change file content, add and commit
		// file
		git.checkout().setCreateBranch(true).setName("first_topic").call();
		file = findFile(project1, TEST_FILE);
		write(file.getLocation().toFile(), "First Topic Content");
		project1.refreshLocal(IResource.DEPTH_INFINITE, null);
		gitAdd(git, file);
		RevCommit firstTopicCommit = gitCommit(git);

		// Create and checkout new branch (from master), change file content,
		// add and commit file
		git.checkout().setCreateBranch(true).setStartPoint(masterCommit)
				.setName("second_topic").call();
		file = findFile(project1, TEST_FILE);
		write(file.getLocation().toFile(), "Second Topic Content");
		project1.refreshLocal(IResource.DEPTH_INFINITE, null);
		gitAdd(git, file);
		gitCommit(git);

		// modify file2 to make it dirty
		write(file2.getLocation().toFile(), "Changed content");

		// merge second_topic with first_topic
		MergeResult mergeResult = git.merge().include(firstTopicCommit).call();
		assertEquals(MergeStatus.CONFLICTING, mergeResult.getMergeStatus());

		IDecoratableResource[] expectedDRs = new IDecoratableResource[] {
				newExpectedDecoratableResource(project1).tracked().dirty()
						.conflicts(),
				newExpectedDecoratableResource(file).tracked().conflicts(),
				newExpectedDecoratableResource(file2).tracked().dirty(),
				newExpectedDecoratableWorkingSet(WORKING_SET).tracked().dirty()
						.conflicts() };
		IndexDiffData indexDiffData = waitForIndexDiff(true);
		IDecoratableResource[] actualDRs = {
				newDecoratableResource(indexDiffData, project1),
				newDecoratableResource(indexDiffData, file),
				newDecoratableResource(indexDiffData, file2),
				newDecoratableWorkingSet(resourceMapping) };

		assertArrayEquals(expectedDRs, actualDRs);
		assertHasUnstagedChanges(true, actualDRs);
		assertDecorationConflicts(resourceMapping);
	}
}
