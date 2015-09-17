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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.NameConflictTreeWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.provider.ResourceDiff;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.junit.Before;
import org.junit.Test;

public class GitResourceVariantTreeSubscriberTest extends VariantsTestCase {
	private static final String BRANCH_CHANGES = "branch changes\n";

	private static final String MASTER_CHANGES = "\nsome changes";

	private static final String BASE = "base";

	private File file1;

	private File file2;

	private IFile iFile1;

	private IFile iFile2;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		file1 = testRepo.createFile(iProject, "file1");
		file2 = testRepo.createFile(iProject, "file2");

		iFile1 = testRepo.getIFile(iProject, file1);
		iFile2 = testRepo.getIFile(iProject, file2);
	}

	@Test
	public void testSubscriber() throws Exception {
		GitResourceVariantTreeProvider provider = createTreeProvider();
		GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				provider);

		assertTrue(subscriber.isSupervised(iProject));
		assertTrue(subscriber.isSupervised(iFile1));
		assertTrue(subscriber.isSupervised(iFile2));

		assertSame(provider.getBaseTree(), subscriber.getBaseTree());
		assertSame(provider.getRemoteTree(), subscriber.getRemoteTree());
		assertSame(provider.getSourceTree(), subscriber.getSourceTree());

		assertNotNull(subscriber.getDiff(iProject));
		assertNotNull(subscriber.getDiff(iFile1));
		assertNotNull(subscriber.getDiff(iFile2));

		assertNotNull(subscriber.getSyncInfo(iProject));
		assertNotNull(subscriber.getSyncInfo(iFile1));
		assertNotNull(subscriber.getSyncInfo(iFile2));

	}

	@Test
	public void testSyncInfo() throws Exception {
		GitResourceVariantTreeProvider provider = createTreeProvider();
		GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				provider);

		final SyncInfo projectInfo = subscriber.getSyncInfo(iProject);
		assertNotNull(projectInfo);
		assertEquals(SyncInfo.CONFLICTING | SyncInfo.CHANGE,
				projectInfo.getKind());

		final SyncInfo syncInfo1 = subscriber.getSyncInfo(iFile1);
		assertNotNull(syncInfo1);
		assertEquals(SyncInfo.OUTGOING | SyncInfo.CHANGE, syncInfo1.getKind());
		IResourceVariant baseVariant1 = syncInfo1.getBase();
		IResourceVariant remoteVariant1 = syncInfo1.getRemote();
		assertContentEquals(baseVariant1, INITIAL_CONTENT_1);
		assertContentEquals(remoteVariant1, INITIAL_CONTENT_1);

		final SyncInfo syncInfo2 = subscriber.getSyncInfo(iFile2);
		assertNotNull(syncInfo2);
		assertEquals(SyncInfo.INCOMING | SyncInfo.CHANGE, syncInfo2.getKind());
		IResourceVariant baseVariant2 = syncInfo2.getBase();
		IResourceVariant remoteVariant2 = syncInfo2.getRemote();
		assertContentEquals(baseVariant2, INITIAL_CONTENT_2);
		assertContentEquals(remoteVariant2, BRANCH_CHANGES + INITIAL_CONTENT_2);
	}

	@Test
	public void testDiff() throws Exception {
		GitResourceVariantTreeProvider provider = createTreeProvider();
		GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				provider);

		final IDiff diff1 = subscriber.getDiff(iFile1);
		assertTrue(diff1 instanceof IThreeWayDiff);
		assertEquals(IDiff.CHANGE, diff1.getKind());
		assertEquals(IThreeWayDiff.OUTGOING,
				((IThreeWayDiff) diff1).getDirection());
		final IDiff localDiff1 = ((IThreeWayDiff) diff1).getLocalChange();
		final IDiff remoteDiff1 = ((IThreeWayDiff) diff1).getRemoteChange();
		assertNull(remoteDiff1);
		assertTrue(localDiff1 instanceof ResourceDiff);
		final IFileRevision localState1 = ((ResourceDiff) localDiff1)
				.getAfterState();
		final IFileRevision baseState1 = ((ResourceDiff) localDiff1)
				.getBeforeState();
		assertNotNull(localState1);
		assertNotNull(baseState1);
		assertTrue(iFile1.getName().equals(localState1.getName()));
		assertTrue(iFile1.getName().equals(baseState1.getName()));
		final IStorage localStorage1 = localState1
				.getStorage(new NullProgressMonitor());
		final IStorage baseStorage1 = baseState1
				.getStorage(new NullProgressMonitor());
		assertContentEquals(localStorage1, INITIAL_CONTENT_1 + MASTER_CHANGES);
		assertContentEquals(baseStorage1, INITIAL_CONTENT_1);

		final IDiff diff2 = subscriber.getDiff(iFile2);
		assertTrue(diff2 instanceof IThreeWayDiff);
		assertEquals(IDiff.CHANGE, diff2.getKind());
		assertEquals(IThreeWayDiff.INCOMING,
				((IThreeWayDiff) diff2).getDirection());
		final IDiff localDiff2 = ((IThreeWayDiff) diff2).getLocalChange();
		final IDiff remoteDiff2 = ((IThreeWayDiff) diff2).getRemoteChange();
		assertTrue(remoteDiff2 instanceof ResourceDiff);
		assertNull(localDiff2);
		final IFileRevision remoteState2 = ((ResourceDiff) remoteDiff2)
				.getAfterState();
		final IFileRevision ancestorState2 = ((ResourceDiff) remoteDiff2)
				.getBeforeState();
		assertTrue(iFile2.getName().equals(ancestorState2.getName()));
		assertTrue(iFile2.getName().equals(remoteState2.getName()));
		final IStorage ancestorStorage2 = ancestorState2
				.getStorage(new NullProgressMonitor());
		final IStorage remoteStorage2 = remoteState2
				.getStorage(new NullProgressMonitor());
		assertContentEquals(ancestorStorage2, INITIAL_CONTENT_2);
		assertContentEquals(remoteStorage2, BRANCH_CHANGES + INITIAL_CONTENT_2);
	}

	@Test
	public void testAddLocalAndRemote() throws Exception {
		GitResourceVariantTreeProvider provider = createTreeProviderWithAdditions();
		GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				provider);

		final IDiff diff1 = subscriber.getDiff(iFile1);
		assertTrue(diff1 instanceof IThreeWayDiff);
		assertEquals(IDiff.ADD, diff1.getKind());
		assertEquals(IThreeWayDiff.OUTGOING,
				((IThreeWayDiff) diff1).getDirection());
		final IDiff localDiff1 = ((IThreeWayDiff) diff1).getLocalChange();
		final IDiff remoteDiff1 = ((IThreeWayDiff) diff1).getRemoteChange();
		assertTrue(localDiff1 instanceof ResourceDiff);
		assertNull(remoteDiff1);
		final IFileRevision ancestorState1 = ((ResourceDiff) localDiff1)
				.getBeforeState();
		final IFileRevision localState1 = ((ResourceDiff) localDiff1)
				.getAfterState();
		assertTrue(iFile1.getName().equals(localState1.getName()));
		assertNull(ancestorState1);
		final IStorage localStorage1 = localState1
				.getStorage(new NullProgressMonitor());
		assertContentEquals(localStorage1, INITIAL_CONTENT_1);

		final IDiff diff2 = subscriber.getDiff(iFile2);
		assertTrue(diff2 instanceof IThreeWayDiff);
		assertEquals(IDiff.ADD, diff2.getKind());
		assertEquals(IThreeWayDiff.INCOMING,
				((IThreeWayDiff) diff2).getDirection());
		final IDiff localDiff2 = ((IThreeWayDiff) diff2).getLocalChange();
		final IDiff remoteDiff2 = ((IThreeWayDiff) diff2).getRemoteChange();
		assertTrue(remoteDiff2 instanceof ResourceDiff);
		assertNull(localDiff2);
		final IFileRevision ancestorState2 = ((ResourceDiff) remoteDiff2)
				.getBeforeState();
		final IFileRevision remoteState2 = ((ResourceDiff) remoteDiff2)
				.getAfterState();
		assertNull(ancestorState2);
		assertTrue(iFile2.getName().equals(remoteState2.getName()));
		final IStorage remoteStorage2 = remoteState2
				.getStorage(new NullProgressMonitor());
		assertContentEquals(remoteStorage2, INITIAL_CONTENT_2);
	}

	@Test
	public void testRemoveLocalAndRemote() throws Exception {
		GitResourceVariantTreeProvider provider = createTreeProviderWithDeletions();
		GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				provider);

		// file1 has been removed locally
		final IDiff diff1 = subscriber.getDiff(iFile1);
		assertTrue(diff1 instanceof IThreeWayDiff);
		assertEquals(IDiff.REMOVE, diff1.getKind());
		assertEquals(IThreeWayDiff.OUTGOING,
				((IThreeWayDiff) diff1).getDirection());
		final IDiff localDiff1 = ((IThreeWayDiff) diff1).getLocalChange();
		final IDiff remoteDiff1 = ((IThreeWayDiff) diff1).getRemoteChange();
		assertTrue(localDiff1 instanceof ResourceDiff);
		assertNull(remoteDiff1);
		final IFileRevision ancestorState1 = ((ResourceDiff) localDiff1)
				.getBeforeState();
		final IFileRevision localState1 = ((ResourceDiff) localDiff1)
				.getAfterState();
		assertTrue(iFile1.getName().equals(ancestorState1.getName()));
		assertNull(localState1);
		final IStorage ancestorStorage1 = ancestorState1
				.getStorage(new NullProgressMonitor());
		assertContentEquals(ancestorStorage1, INITIAL_CONTENT_1);

		// file2 has been removed remotely
		final IDiff diff2 = subscriber.getDiff(iFile2);
		assertTrue(diff2 instanceof IThreeWayDiff);
		assertEquals(IDiff.REMOVE, diff2.getKind());
		assertEquals(IThreeWayDiff.INCOMING,
				((IThreeWayDiff) diff2).getDirection());
		final IDiff localDiff2 = ((IThreeWayDiff) diff2).getLocalChange();
		final IDiff remoteDiff2 = ((IThreeWayDiff) diff2).getRemoteChange();
		assertTrue(remoteDiff2 instanceof ResourceDiff);
		assertNull(localDiff2);
		final IFileRevision ancestorState2 = ((ResourceDiff) remoteDiff2)
				.getBeforeState();
		final IFileRevision remoteState2 = ((ResourceDiff) remoteDiff2)
				.getAfterState();
		assertTrue(iFile2.getName().equals(ancestorState2.getName()));
		assertNull(remoteState2);
		final IStorage rancestorStorage2 = ancestorState2
				.getStorage(new NullProgressMonitor());
		assertContentEquals(rancestorStorage2, INITIAL_CONTENT_2);
	}

	private GitResourceVariantTreeProvider createTreeProvider()
			throws Exception {
		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2,
				INITIAL_CONTENT_2, "second file - initial commit");
		testRepo.createBranch(MASTER, BASE);

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(testRepo, iFile2, BRANCH_CHANGES
				+ INITIAL_CONTENT_2, "branch commit");

		testRepo.checkoutBranch(MASTER);

		setContentsAndCommit(testRepo, iFile1, INITIAL_CONTENT_1
				+ MASTER_CHANGES, "master commit");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());

		// as if we tried to merge branch into master
		try (RevWalk walk = new RevWalk(repo)) {
			RevTree baseTree = walk.parseTree(repo.resolve(BASE));
			RevTree sourceTree = walk.parseTree(repo.resolve(MASTER));
			RevTree remoteTree = walk.parseTree(repo.resolve(BRANCH));
			TreeWalk treeWalk = new NameConflictTreeWalk(repo);
			treeWalk.addTree(baseTree);
			treeWalk.addTree(sourceTree);
			treeWalk.addTree(remoteTree);
			return new TreeWalkResourceVariantTreeProvider(repo, treeWalk, 0,
					1, 2);
		}
	}

	private GitResourceVariantTreeProvider createTreeProviderWithAdditions()
			throws Exception {
		testRepo.createBranch(MASTER, BASE);
		testRepo.createAndCheckoutBranch(MASTER, BRANCH);
		file2 = testRepo.createFile(iProject, "file2");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_2,
				"Creation of file2 in branch2.");

		testRepo.checkoutBranch(MASTER);
		file1 = testRepo.createFile(iProject, "file1");
		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_1,
				"Creation of file1 in branch1.");

		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());

		// as if we tried to merge branch3 into branch2
		try (RevWalk walk = new RevWalk(repo)) {
			RevTree baseTree = walk.parseTree(repo.resolve(BASE));
			RevTree sourceTree = walk.parseTree(repo.resolve(MASTER));
			RevTree remoteTree = walk.parseTree(repo.resolve(BRANCH));
			TreeWalk treeWalk = new NameConflictTreeWalk(repo);
			treeWalk.addTree(baseTree);
			treeWalk.addTree(sourceTree);
			treeWalk.addTree(remoteTree);
			return new TreeWalkResourceVariantTreeProvider(repo, treeWalk, 0,
					1, 2);
		}
	}

	private GitResourceVariantTreeProvider createTreeProviderWithDeletions()
			throws Exception {
		file1 = testRepo.createFile(iProject, "file1");
		testRepo.appendContentAndCommit(iProject, file1, INITIAL_CONTENT_1,
				"Creation of file1 in branch1.");
		file2 = testRepo.createFile(iProject, "file2");
		testRepo.appendContentAndCommit(iProject, file2, INITIAL_CONTENT_2,
				"Creation of file2 in branch2.");
		testRepo.createBranch(MASTER, BASE);

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);
		testRepo.untrack(file2);
		testRepo.commit("Removed file2 in branch.");

		testRepo.checkoutBranch(MASTER);
		testRepo.untrack(file1);
		testRepo.commit("Removed file1 in master.");

		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());

		// as if we tried to merge branch3 into branch2
		try (RevWalk walk = new RevWalk(repo)) {
			RevTree baseTree = walk.parseTree(repo.resolve(BASE));
			RevTree sourceTree = walk.parseTree(repo.resolve(MASTER));
			RevTree remoteTree = walk.parseTree(repo.resolve(BRANCH));
			TreeWalk treeWalk = new NameConflictTreeWalk(repo);
			treeWalk.addTree(baseTree);
			treeWalk.addTree(sourceTree);
			treeWalk.addTree(remoteTree);
			return new TreeWalkResourceVariantTreeProvider(repo, treeWalk, 0,
					1, 2);
		}
	}
}
