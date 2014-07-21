/*******************************************************************************
 * Copyright (C) 2014 Obeo and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.merge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.egit.core.test.models.ModelTestCase;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff.StageState;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StrategyRecursiveModelTest extends ModelTestCase {
	private static final String MASTER = Constants.R_HEADS + Constants.MASTER;

	private static final String BRANCH = Constants.R_HEADS + "branch";

	private static final String INITIAL_CONTENT_FILE1 = "some content for the first file";

	private static final String INITIAL_CONTENT_FILE2 = "some content for the second file";

	// The Team merger uses this when merging, regardless of what exists in the
	// file
	private static final String SYSTEM_EOL = System.getProperty("line.separator");

	private static final String BRANCH_CHANGE = "branch changes" + SYSTEM_EOL;

	private static final String MASTER_CHANGE = "master changes" + SYSTEM_EOL;


	private Repository repo;

	private IProject iProject;

	private TestRepository testRepo;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		iProject = project.project;
		testRepo = new TestRepository(gitDir);
		testRepo.connect(iProject);
		repo = RepositoryMapping.getMapping(iProject).getRepository();

		// make initial commit
		new Git(repo).commit().setAuthor("JUnit", "junit@jgit.org")
				.setMessage("Initial commit").call();
	}

	@After
	public void clearGitResources() throws Exception {
		testRepo.disconnect(iProject);
		testRepo.dispose();
		repo = null;

		super.tearDown();
	}

	/**
	 * This test will initialize a repository with two branches with a few
	 * changes each, then try to merge the branch into master.
	 * <p>
	 * The repository will contain two files, file1.sample and file2.sample,
	 * both being in the same container and thus considered to be components of
	 * a single logical model by the SampleModelProvider.
	 * </p>
	 * <p>
	 * file1 will be modified on both master and the branch, these changes being
	 * considered as an unresolveable conflict by git (and JGit), but considered
	 * as an auto-mergeable conflict by the SampleResourceMappingMerger. file2
	 * will only be modified on the branch : it will be deleted.
	 * </p>
	 * <p>
	 * We expect the merge to end successfully, ending with a repository that
	 * has no uncommited change.
	 * </p>
	 *
	 * @throws Exception
	 */
	@Test
	public void mergeModelWithDeletedRemoteNoConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_FILE1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_FILE2,
				"second file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1, BRANCH_CHANGE
				+ INITIAL_CONTENT_FILE1, "branch commit");
		iFile2.delete(true, new NullProgressMonitor());
		testRepo.addAndCommit(iProject, file2, "branch commit - deleted file2."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.checkoutBranch(MASTER);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1,
				INITIAL_CONTENT_FILE1 + MASTER_CHANGE, "master commit");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		merge(repo, BRANCH);

		final Status status = status(repo);
		assertFalse(status.hasUncommittedChanges());
		assertTrue(status.getConflicting().isEmpty());

		assertContentEquals(iFile1, BRANCH_CHANGE + INITIAL_CONTENT_FILE1
				+ MASTER_CHANGE);
		assertFalse(iFile2.exists());
	}

	/**
	 * This test will initialize a repository with two branches with a few
	 * changes each, then try to merge the branch into master.
	 * <p>
	 * The repository will contain two files, file1.sample and file2.sample,
	 * both being in the same container and thus considered to be components of
	 * a single logical model by the SampleModelProvider.
	 * </p>
	 * <p>
	 * file1 will be modified on both master and the branch, these changes being
	 * considered as an unresolveable conflict by git (and JGit), but considered
	 * as an auto-mergeable conflict by the SampleResourceMappingMerger. file2
	 * will only be modified on master where it will be deleted.
	 * </p>
	 * <p>
	 * We expect the merge to end successfully, ending with a repository that
	 * has no uncommited change.
	 * </p>
	 *
	 * @throws Exception
	 */
	@Test
	public void mergeModelWithDeletedLocalNoConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_FILE1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_FILE2,
				"second file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1, BRANCH_CHANGE
				+ INITIAL_CONTENT_FILE1, "branch commit");

		testRepo.checkoutBranch(MASTER);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1,
				INITIAL_CONTENT_FILE1 + MASTER_CHANGE, "master commit");
		iFile2.delete(true, new NullProgressMonitor());
		testRepo.addAndCommit(iProject, file2, "master commit - deleted file2."
				+ SAMPLE_FILE_EXTENSION);
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		merge(repo, BRANCH);

		final Status status = status(repo);
		assertFalse(status.hasUncommittedChanges());
		assertTrue(status.getConflicting().isEmpty());

		assertContentEquals(iFile1, BRANCH_CHANGE + INITIAL_CONTENT_FILE1
				+ MASTER_CHANGE);
		assertFalse(iFile2.exists());
	}

	/**
	 * This test will initialize a repository with two branches with a few
	 * changes each, then try to merge the branch into master.
	 * <p>
	 * The repository will contain two files, file1.sample and file2.sample,
	 * both being in the same container and thus considered to be components of
	 * a single logical model by the SampleModelProvider.
	 * </p>
	 * <p>
	 * file1 will be modified on both master and the branch, these changes being
	 * considered as an unresolveable conflict by git (and JGit), but considered
	 * as an auto-mergeable conflict by the SampleResourceMappingMerger. file2
	 * will be deleted on both master and the branch. This is a pseudo-conflict
	 * and should thus be automatically merged.
	 * </p>
	 * <p>
	 * We expect the merge to end successfully, ending with a repository that
	 * has no uncommited change.
	 * </p>
	 *
	 * @throws Exception
	 */
	@Test
	public void mergeModelWithPseudoConflictDeletion() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_FILE1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_FILE2,
				"second file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1, BRANCH_CHANGE
				+ INITIAL_CONTENT_FILE1, "branch commit");
		iFile2.delete(true, new NullProgressMonitor());
		testRepo.addAndCommit(iProject, file2, "branch commit - deleted file2."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.checkoutBranch(MASTER);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1,
				INITIAL_CONTENT_FILE1 + MASTER_CHANGE, "master commit");
		iFile2.delete(true, new NullProgressMonitor());
		testRepo.addAndCommit(iProject, file2, "master commit - deleted file2."
				+ SAMPLE_FILE_EXTENSION);
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		merge(repo, BRANCH);

		final Status status = status(repo);
		assertFalse(status.hasUncommittedChanges());
		assertTrue(status.getConflicting().isEmpty());

		assertContentEquals(iFile1, BRANCH_CHANGE + INITIAL_CONTENT_FILE1
				+ MASTER_CHANGE);
		assertFalse(iFile2.exists());
	}

	/**
	 * This test will initialize a repository with two branches with a few
	 * changes each, then try to merge the branch into master.
	 * <p>
	 * The repository will contain two files, file1.sample and file2.sample,
	 * both being in the same container and thus considered to be components of
	 * a single logical model by the SampleModelProvider.
	 * </p>
	 * <p>
	 * file1 will be modified on both master and the branch in such a way that
	 * it will be an unresolveable conflict for both JGit and the model merger.
	 * file2 will be deleted from the branch.
	 * </p>
	 * <p>
	 * The merge must end in a conflict. The SampleResourceMappingMerger
	 * pre-merges what can be, so file2 will be deleted from the working tree
	 * while file1 will be left untouched. file2 will be added to the index, but
	 * file 1 will be marked as a conflict.
	 * </p>
	 *
	 * @throws Exception
	 */
	@Test
	public void mergeModelWithDeletedRemoteModelConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_FILE1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_FILE2,
				"second file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());
		String repoRelativePath2 = testRepo.getRepoRelativePath(iFile2
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1, BRANCH_CHANGE
				+ INITIAL_CONTENT_FILE1, "branch commit");
		iFile2.delete(true, new NullProgressMonitor());
		testRepo.addAndCommit(iProject, file2, "branch commit - deleted file2."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.checkoutBranch(MASTER);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1, MASTER_CHANGE
				+ INITIAL_CONTENT_FILE1, "master commit");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		merge(repo, BRANCH);

		final Status status = status(repo);
		assertTrue(status.hasUncommittedChanges());
		assertFalse(status.getConflicting().isEmpty());
		assertTrue(status.getConflicting().contains(repoRelativePath1));
		assertTrue(status.getRemoved().contains(repoRelativePath2));

		assertContentEquals(iFile1, MASTER_CHANGE + INITIAL_CONTENT_FILE1);
		assertFalse(iFile2.exists());

		Map<String, StageState> map = status.getConflictingStageState();
		assertEquals(StageState.BOTH_MODIFIED, map.get(repoRelativePath1));
	}

	/**
	 * This test will initialize a repository with two branches with a few
	 * changes each, then try to merge the branch into master.
	 * <p>
	 * The repository will contain two files, file1.sample and file2.sample,
	 * both being in the same container and thus considered to be components of
	 * a single logical model by the SampleModelProvider.
	 * </p>
	 * <p>
	 * file1 will be modified on both master and the branch in such a way that
	 * it will be an unresolveable conflict for both JGit and the model merger.
	 * file2 will be deleted from master.
	 * </p>
	 * <p>
	 * The merge must end in a conflict. Since file2 has been deleted locally
	 * and has not been changed on the remote, it will not be seen as a part of
	 * the logical model (it won't even be part of the merge operation). Thus,
	 * file1 will be marked as a conflict, untouched as compared to its
	 * previous (master) state, and file2 will not be part of the index.
	 * </p>
	 *
	 * @throws Exception
	 */
	@Test
	public void mergeModelWithDeletedLocalModelConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_FILE1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_FILE2,
				"second file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());
		String repoRelativePath2 = testRepo.getRepoRelativePath(iFile2
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1, BRANCH_CHANGE
				+ INITIAL_CONTENT_FILE1, "branch commit");

		testRepo.checkoutBranch(MASTER);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1, MASTER_CHANGE
				+ INITIAL_CONTENT_FILE1, "master commit");
		iFile2.delete(true, new NullProgressMonitor());
		testRepo.addAndCommit(iProject, file2, "master commit - deleted file2."
				+ SAMPLE_FILE_EXTENSION);
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		merge(repo, BRANCH);

		final Status status = status(repo);
		assertTrue(status.hasUncommittedChanges());
		assertFalse(status.getConflicting().isEmpty());
		assertTrue(status.getConflicting().contains(repoRelativePath1));
		assertFalse(status.getConflicting().contains(repoRelativePath2));

		assertContentEquals(iFile1, MASTER_CHANGE + INITIAL_CONTENT_FILE1);
		assertFalse(iFile2.exists());

		Map<String, StageState> map = status.getConflictingStageState();
		assertEquals(StageState.BOTH_MODIFIED, map.get(repoRelativePath1));
	}

	/**
	 * This test will initialize a repository with two branches with a few
	 * changes each, then try to merge the branch into master.
	 * <p>
	 * The repository will contain two files, file1.sample and file2.sample,
	 * both being in the same container and thus considered to be components of
	 * a single logical model by the SampleModelProvider.
	 * </p>
	 * <p>
	 * file1 will be modified on both master and the branch, these changes being
	 * considered as an unresolveable conflict by git (and JGit), but considered
	 * as an auto-mergeable conflict by the SampleResourceMappingMerger. file2
	 * will be deleted from the branch, but modified on master.
	 * </p>
	 * <p>
	 * The merge will end in conflict, file1 being pre-merged since it does not
	 * present a conflict, and file2 should remain in its "master" state. file1
	 * will have been added to the index, but file2 will be marked as
	 * conflicting.
	 * </p>
	 *
	 * @throws Exception
	 */
	@Test
	public void mergeModelWithDeletedRemoteFileConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_FILE1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_FILE2,
				"second file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());
		String repoRelativePath2 = testRepo.getRepoRelativePath(iFile2
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1, BRANCH_CHANGE
				+ INITIAL_CONTENT_FILE1, "branch commit");
		iFile2.delete(true, new NullProgressMonitor());
		testRepo.addAndCommit(iProject, file2, "branch commit - deleted file2."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.checkoutBranch(MASTER);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1,
				INITIAL_CONTENT_FILE1 + MASTER_CHANGE, "master commit - file1");
		setContentsAndCommit(testRepo, repoRelativePath2, iFile2,
				INITIAL_CONTENT_FILE2 + MASTER_CHANGE, "master commit - file2");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		merge(repo, BRANCH);

		final Status status = status(repo);
		assertTrue(status.hasUncommittedChanges());
		assertTrue(status.getChanged().contains(repoRelativePath1));
		assertTrue(status.getConflicting().contains(repoRelativePath2));

		assertContentEquals(iFile1, BRANCH_CHANGE + INITIAL_CONTENT_FILE1
				+ MASTER_CHANGE);
		assertContentEquals(iFile2, INITIAL_CONTENT_FILE2 + MASTER_CHANGE);

		Map<String, StageState> map = status.getConflictingStageState();
		assertEquals(StageState.DELETED_BY_THEM, map.get(repoRelativePath2));
	}

	/**
	 * This test will initialize a repository with two branches with a few
	 * changes each, then try to merge the branch into master.
	 * <p>
	 * The repository will contain two files, file1.sample and file2.sample,
	 * both being in the same container and thus considered to be components of
	 * a single logical model by the SampleModelProvider.
	 * </p>
	 * <p>
	 * file1 will be modified on both master and the branch, these changes being
	 * considered as an unresolveable conflict by git (and JGit), but considered
	 * as an auto-mergeable conflict by the SampleResourceMappingMerger. file2
	 * will be deleted from master, but modified on the branch.
	 * </p>
	 * <p>
	 * The merge will end in conflict. Since file2 has been deleted locally, it
	 * will not be seen as a part of the logical model. file1 will be pre-merged
	 * since it does not present a conflict, and file2 will be checked out in
	 * its "branch" state since we won't detect its logical model. Only file2
	 * will be marked as a conflict.
	 * </p>
	 *
	 * @throws Exception
	 */
	@Test
	public void mergeModelWithDeletedLocalFileConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_FILE1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_FILE2,
				"second file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());
		String repoRelativePath2 = testRepo.getRepoRelativePath(iFile2
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1, BRANCH_CHANGE
				+ INITIAL_CONTENT_FILE1, "branch commit");
		setContentsAndCommit(testRepo, repoRelativePath2, iFile2, BRANCH_CHANGE
				+ INITIAL_CONTENT_FILE2, "branch commit");

		testRepo.checkoutBranch(MASTER);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1,
				INITIAL_CONTENT_FILE1 + MASTER_CHANGE, "master commit");
		iFile2.delete(true, new NullProgressMonitor());
		testRepo.addAndCommit(iProject, file2, "master commit - deleted file2."
				+ SAMPLE_FILE_EXTENSION);

		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		merge(repo, BRANCH);

		final Status status = status(repo);
		assertTrue(status.hasUncommittedChanges());
		assertFalse(status.getConflicting().contains(repoRelativePath1));
		assertTrue(status.getConflicting().contains(repoRelativePath2));

		assertContentEquals(iFile1, BRANCH_CHANGE + INITIAL_CONTENT_FILE1
				+ MASTER_CHANGE);
		assertContentEquals(iFile2, BRANCH_CHANGE + INITIAL_CONTENT_FILE2);

		Map<String, StageState> map = status.getConflictingStageState();
		assertEquals(StageState.DELETED_BY_US, map.get(repoRelativePath2));
	}

	/**
	 * This test will initialize a repository with two branches with a few
	 * changes each, then try to merge the branch into master.
	 * <p>
	 * The repository will contain two files, file1.sample and file2.sample,
	 * both being in the same container and thus considered to be components of
	 * a single logical model by the SampleModelProvider.
	 * </p>
	 * <p>
	 * file1 will be modified on both master and the branch, these changes being
	 * considered as an unresolveable conflict by git (and JGit), but considered
	 * as an auto-mergeable conflict by the SampleResourceMappingMerger. file2
	 * will only exist on the branch since we'll add it there.
	 * </p>
	 * <p>
	 * We expect the merge to end successfully, ending with a repository that
	 * has no uncommited change.
	 * </p>
	 *
	 * @throws Exception
	 */
	@Test
	public void mergeModelWithAddedRemoteNoConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_FILE1,
				"first file - initial commit");
		IFile iFile1 = testRepo.getIFile(iProject, file1);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1, BRANCH_CHANGE
				+ INITIAL_CONTENT_FILE1, "branch commit");
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_FILE2,
				"second file - initial commit on branch");
		IFile iFile2 = testRepo.getIFile(iProject, file2);

		testRepo.checkoutBranch(MASTER);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1,
				INITIAL_CONTENT_FILE1 + MASTER_CHANGE, "master commit");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		merge(repo, BRANCH);

		final Status status = status(repo);
		assertFalse(status.hasUncommittedChanges());
		assertTrue(status.getConflicting().isEmpty());

		assertContentEquals(iFile1, BRANCH_CHANGE + INITIAL_CONTENT_FILE1
				+ MASTER_CHANGE);
		assertContentEquals(iFile2, INITIAL_CONTENT_FILE2);
	}

	/**
	 * This test will initialize a repository with two branches with a few
	 * changes each, then try to merge the branch into master.
	 * <p>
	 * The repository will contain two files, file1.sample and file2.sample,
	 * both being in the same container and thus considered to be components of
	 * a single logical model by the SampleModelProvider.
	 * </p>
	 * <p>
	 * file1 will be modified on both master and the branch, these changes being
	 * considered as an unresolveable conflict by git (and JGit), but considered
	 * as an auto-mergeable conflict by the SampleResourceMappingMerger. file2
	 * will be added to master after the branch has diverged.
	 * </p>
	 * <p>
	 * We expect the merge to end successfully, ending with a repository that
	 * has no uncommited change.
	 * </p>
	 *
	 * @throws Exception
	 */
	@Test
	public void mergeModelWithAddedLocalNoConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_FILE1,
				"first file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1, BRANCH_CHANGE
				+ INITIAL_CONTENT_FILE1, "branch commit");

		testRepo.checkoutBranch(MASTER);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1,
				INITIAL_CONTENT_FILE1 + MASTER_CHANGE, "master commit");
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_FILE2,
				"second file - initial commit on master");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		merge(repo, BRANCH);

		final Status status = status(repo);
		assertFalse(status.hasUncommittedChanges());
		assertTrue(status.getConflicting().isEmpty());

		assertContentEquals(iFile1, BRANCH_CHANGE + INITIAL_CONTENT_FILE1
				+ MASTER_CHANGE);
		assertContentEquals(iFile2, INITIAL_CONTENT_FILE2);
	}

	/**
	 * This test will initialize a repository with two branches with a few
	 * changes each, then try to merge the branch into master.
	 * <p>
	 * The repository will contain two files, file1.sample and file2.sample,
	 * both being in the same container and thus considered to be components of
	 * a single logical model by the SampleModelProvider.
	 * </p>
	 * <p>
	 * file1 will be modified on both master and the branch, these changes being
	 * considered as an unresolveable conflict by git (and JGit), but considered
	 * as an auto-mergeable conflict by the SampleResourceMappingMerger. file2
	 * will be added on both master and the branch with the same content. This
	 * is a pseudo-conflict but the default text merger (TextStorageMerger)
	 * cannot handle such cases. file2 will thus be marked as a conflict.
	 * </p>
	 * <p>
	 * We expect the merge to end successfully, ending with a repository that
	 * has no uncommited change.
	 * </p>
	 *
	 * @throws Exception
	 */
	@Test
	public void mergeModelWithPseudoConflictAddition() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_FILE1,
				"first file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1, BRANCH_CHANGE
				+ INITIAL_CONTENT_FILE1, "branch commit");
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_FILE2,
				"second file - initial commit on branch");

		testRepo.checkoutBranch(MASTER);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1,
				INITIAL_CONTENT_FILE1 + MASTER_CHANGE, "master commit");
		file2 = testRepo.createFile(iProject, "file2." + SAMPLE_FILE_EXTENSION);
		iFile2 = testRepo.getIFile(iProject, file2);
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_FILE2,
				"second file - initial commit on master");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		String repoRelativePath2 = testRepo.getRepoRelativePath(iFile2
				.getLocation().toPortableString());
		// end setup

		merge(repo, BRANCH);

		final Status status = status(repo);
		assertTrue(status.hasUncommittedChanges());
		assertTrue(status.getConflicting().contains(repoRelativePath2));

		assertContentEquals(iFile1, BRANCH_CHANGE + INITIAL_CONTENT_FILE1
				+ MASTER_CHANGE);
		assertContentEquals(iFile2, INITIAL_CONTENT_FILE2);

		Map<String, StageState> map = status.getConflictingStageState();
		assertEquals(StageState.BOTH_ADDED, map.get(repoRelativePath2));
	}

	/**
	 * This test will initialize a repository with two branches with a few
	 * changes each, then try to merge the branch into master.
	 * <p>
	 * The repository will contain two files, file1.sample and file2.sample,
	 * both being in the same container and thus considered to be components of
	 * a single logical model by the SampleModelProvider.
	 * </p>
	 * <p>
	 * file1 will be modified on both master and the branch in such a way that
	 * it will be an unresolveable conflict for both JGit and the model merger.
	 * file2 will be added to the branch.
	 * </p>
	 * <p>
	 * The merge must end in a conflict. The SampleResourceMappingMerger
	 * pre-merges what can be, so file2 will be added to the working tree while
	 * file1 will be left untouched. file2 will be added to the index, but file
	 * 1 will be marked as a conflict.
	 * </p>
	 *
	 * @throws Exception
	 */
	@Test
	public void mergeModelWithAddedRemoteModelConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_FILE1,
				"first file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1, BRANCH_CHANGE
				+ INITIAL_CONTENT_FILE1, "branch commit");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_FILE2,
				"second file - initial commit - branch");
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		String repoRelativePath2 = testRepo.getRepoRelativePath(iFile2
				.getLocation().toPortableString());

		testRepo.checkoutBranch(MASTER);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1, MASTER_CHANGE
				+ INITIAL_CONTENT_FILE1, "master commit");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		merge(repo, BRANCH);

		final Status status = status(repo);
		assertTrue(status.hasUncommittedChanges());
		assertFalse(status.getConflicting().isEmpty());
		assertTrue(status.getConflicting().contains(repoRelativePath1));
		assertTrue(status.getAdded().contains(repoRelativePath2));

		assertContentEquals(iFile1, MASTER_CHANGE + INITIAL_CONTENT_FILE1);
		assertContentEquals(iFile2, INITIAL_CONTENT_FILE2);

		Map<String, StageState> map = status.getConflictingStageState();
		assertEquals(StageState.BOTH_MODIFIED, map.get(repoRelativePath1));
	}

	/**
	 * This test will initialize a repository with two branches with a few
	 * changes each, then try to merge the branch into master.
	 * <p>
	 * The repository will contain two files, file1.sample and file2.sample,
	 * both being in the same container and thus considered to be components of
	 * a single logical model by the SampleModelProvider.
	 * </p>
	 * <p>
	 * file1 will be modified on both master and the branch in such a way that
	 * it will be an unresolveable conflict for both JGit and the model merger.
	 * file2 will be added to master.
	 * </p>
	 * <p>
	 * The merge must end in a conflict. file1 will be marked as a conflict,
	 * untouched as compared to its previous (master) state. file2 will also be
	 * marked as conflicting, since the default merger doesn't tell us that
	 * "added by us" files are to be made in sync.
	 * </p>
	 *
	 * @throws Exception
	 */
	@Test
	public void mergeModelWithAddedLocalModelConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_FILE1,
				"first file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1, BRANCH_CHANGE
				+ INITIAL_CONTENT_FILE1, "branch commit");

		testRepo.checkoutBranch(MASTER);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1, MASTER_CHANGE
				+ INITIAL_CONTENT_FILE1, "master commit");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_FILE2,
				"second file - initial commit");
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		String repoRelativePath2 = testRepo.getRepoRelativePath(iFile2
				.getLocation().toPortableString());
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		merge(repo, BRANCH);

		final Status status = status(repo);
		assertTrue(status.hasUncommittedChanges());
		assertFalse(status.getConflicting().isEmpty());
		assertTrue(status.getConflicting().contains(repoRelativePath1));
		assertTrue(status.getConflicting().contains(repoRelativePath2));

		assertContentEquals(iFile1, MASTER_CHANGE + INITIAL_CONTENT_FILE1);
		assertContentEquals(iFile2, INITIAL_CONTENT_FILE2);

		Map<String, StageState> map = status.getConflictingStageState();
		assertEquals(StageState.BOTH_MODIFIED, map.get(repoRelativePath1));
		assertEquals(StageState.ADDED_BY_US, map.get(repoRelativePath2));
	}

	/**
	 * This test will initialize a repository with two branches with a few
	 * changes each, then try to merge the branch into master.
	 * <p>
	 * The repository will contain two files, file1.sample and file2.sample,
	 * both being in the same container and thus considered to be components of
	 * a single logical model by the SampleModelProvider.
	 * </p>
	 * <p>
	 * file1 will be modified on both master and the branch, these changes being
	 * considered as an unresolveable conflict by git (and JGit), but considered
	 * as an auto-mergeable conflict by the SampleResourceMappingMerger. file2
	 * will be added to both master and the branch, with distinct content.
	 * </p>
	 * <p>
	 * The merge will end in conflict, file1 being pre-merged since it does not
	 * present a conflict, and file2 should remain in its "master" state. file1
	 * will have been added to the index, but file2 will be marked as
	 * conflicting.
	 * </p>
	 *
	 * @throws Exception
	 */
	@Test
	public void mergeModelWithConflictAddition() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_FILE1,
				"first file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1, BRANCH_CHANGE
				+ INITIAL_CONTENT_FILE1, "branch commit");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_FILE2
				+ "branch", "second file - initial commit - branch");

		testRepo.checkoutBranch(MASTER);

		setContentsAndCommit(testRepo, repoRelativePath1, iFile1,
				INITIAL_CONTENT_FILE1 + MASTER_CHANGE, "master commit - file1");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_FILE2
				+ "master", "second file - initial commit - master");
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		String repoRelativePath2 = testRepo.getRepoRelativePath(iFile2
				.getLocation().toPortableString());
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		merge(repo, BRANCH);

		final Status status = status(repo);
		assertTrue(status.hasUncommittedChanges());
		assertTrue(status.getChanged().contains(repoRelativePath1));
		assertTrue(status.getConflicting().contains(repoRelativePath2));

		assertContentEquals(iFile1, BRANCH_CHANGE + INITIAL_CONTENT_FILE1
				+ MASTER_CHANGE);
		assertContentEquals(iFile2, INITIAL_CONTENT_FILE2 + "master");

		Map<String, StageState> map = status.getConflictingStageState();
		assertEquals(StageState.BOTH_ADDED, map.get(repoRelativePath2));
	}
}
