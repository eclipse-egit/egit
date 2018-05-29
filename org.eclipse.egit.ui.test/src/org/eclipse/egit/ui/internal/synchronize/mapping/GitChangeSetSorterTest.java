/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Commit;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCache;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelTree;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelWorkingTree;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.navigator.CommonViewer;
import org.junit.Test;

/**
 * Test for {@link GitChangeSetSorter#compare(Viewer, Object, Object)}.
 */
public class GitChangeSetSorterTest {

	/*
	 * Tests for GitModelWorkingTree
	 */
	@Test public void workingTreeShouldBeLessThanCacheTree() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelCache cache = mock(GitModelCache.class);
		GitModelWorkingTree workingTree = mock(GitModelWorkingTree.class);

		// when
		int actual = sorter.compare(viewer, workingTree, cache);

		// then
		assertTrue(actual < 0);
	}

	@Test public void workingTreeShouldBeLessThanCommit() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelCommit commit = mock(GitModelCommit.class);
		GitModelWorkingTree workingTree = mock(GitModelWorkingTree.class);

		// when
		int actual = sorter.compare(viewer, workingTree, commit);

		// then
		assertTrue(actual < 0);
	}

	@Test public void workingTreeShouldBeLessThanTree() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelTree tree = mock(GitModelTree.class);
		GitModelWorkingTree workingTree = mock(GitModelWorkingTree.class);

		// when
		int actual = sorter.compare(viewer, workingTree, tree);

		// then
		assertTrue(actual < 0);
	}

	@Test public void workingTreeShouldBeLessThanBlob() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelBlob blob = mock(GitModelBlob.class);
		GitModelWorkingTree workingTree = mock(GitModelWorkingTree.class);

		// when
		int actual = sorter.compare(viewer, workingTree, blob);

		// then
		assertTrue(actual < 0);
	}

	/*
	 * Tests for GitModelCache
	 */
	@Test public void cacheTreeShouldBeGreaterThanWorkingTree() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelCache cache = mock(GitModelCache.class);
		GitModelWorkingTree workingTree = mock(GitModelWorkingTree.class);

		// when
		int actual = sorter.compare(viewer, cache, workingTree);

		// then
		assertTrue(actual > 0);
	}

	@Test public void cacheTreeShouldBeLessThanCommit() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelCache cache = mock(GitModelCache.class);
		GitModelCommit commit = mock(GitModelCommit.class);

		// when
		int actual = sorter.compare(viewer, cache, commit);

		// then
		assertTrue(actual < 0);
	}

	@Test public void cacheTreeShouldBeLessThanTree() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelCache cache = mock(GitModelCache.class);
		GitModelTree tree = mock(GitModelTree.class);

		// when
		int actual = sorter.compare(viewer, cache, tree);

		// then
		assertTrue(actual < 0);
	}

	@Test public void cacheTreeShouldBeLessThanBlob() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelCache cache = mock(GitModelCache.class);
		GitModelBlob blob = mock(GitModelBlob.class);

		// when
		int actual = sorter.compare(viewer, cache, blob);

		// then
		assertTrue(actual < 0);
	}

	/*
	 * Tests for GitModelCommit
	 */
	@Test public void commitTreeShouldBeGreaterThanWorkingTree() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelCommit commit = mock(GitModelCommit.class);
		GitModelWorkingTree workingTree = mock(GitModelWorkingTree.class);

		// when
		int actual = sorter.compare(viewer, commit, workingTree);

		// then
		assertTrue(actual > 0);
	}

	@Test public void commitTreeShouldBeGreaterThanCache() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelCommit commit = mock(GitModelCommit.class);
		GitModelCache cache = mock(GitModelCache.class);

		// when
		int actual = sorter.compare(viewer, commit, cache);

		// then
		assertTrue(actual > 0);
	}

	@Test public void commitTreeShouldBeLessThanTree() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelTree tree = mock(GitModelTree.class);
		GitModelBlob blob = mock(GitModelBlob.class);

		// when
		int actual = sorter.compare(viewer, tree, blob);

		// then
		assertTrue(actual < 0);
	}

	@Test public void commitTreeShouldBeLessThanBlob() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelCommit commit = mock(GitModelCommit.class);
		GitModelBlob blob = mock(GitModelBlob.class);

		// when
		int actual = sorter.compare(viewer, commit, blob);

		// then
		assertTrue(actual < 0);
	}

	/*
	 * Tests for GitModelTree
	 */
	@Test public void treeShouldBeGreaterThanWorkingTree() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelTree tree = mock(GitModelTree.class);
		GitModelWorkingTree workingTree = mock(GitModelWorkingTree.class);

		// when
		int actual = sorter.compare(viewer, tree, workingTree);

		// then
		assertTrue(actual > 0);
	}

	@Test public void treeShouldBeGreaterThanCache() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelTree tree = mock(GitModelTree.class);
		GitModelCache cache = mock(GitModelCache.class);

		// when
		int actual = sorter.compare(viewer, tree, cache);

		// then
		assertTrue(actual > 0);
	}

	@Test public void treeShouldBeGreaterThanCommit() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelTree tree = mock(GitModelTree.class);
		GitModelCommit commit = mock(GitModelCommit.class);

		// when
		int actual = sorter.compare(viewer, tree, commit);

		// then
		assertTrue(actual > 0);
	}

	@Test public void treeShouldBeLessThanBlob() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelTree tree = mock(GitModelTree.class);
		GitModelBlob blob = mock(GitModelBlob.class);

		// when
		int actual = sorter.compare(viewer, tree, blob);

		// then
		assertTrue(actual < 0);
	}

	/*
	 * Tests for GitModelBlob
	 */
	@Test public void blobShouldBeGreaterThanWorkingTree() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelBlob blob = mock(GitModelBlob.class);
		GitModelWorkingTree workingTree = mock(GitModelWorkingTree.class);

		// when
		int actual = sorter.compare(viewer, blob, workingTree);

		// then
		assertTrue(actual > 0);
	}

	@Test public void blobShouldBeGreaterThanCache() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelBlob blob = mock(GitModelBlob.class);
		GitModelCache cache = mock(GitModelCache.class);

		// when
		int actual = sorter.compare(viewer, blob, cache);

		// then
		assertTrue(actual > 0);
	}

	@Test public void blobShouldBeGreaterThanCommit() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelBlob blob = mock(GitModelBlob.class);
		GitModelCommit commit = mock(GitModelCommit.class);

		// when
		int actual = sorter.compare(viewer, blob, commit);

		// then
		assertTrue(actual > 0);
	}

	@Test public void blobShouldBeGreaterThanTree() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelBlob blob = mock(GitModelBlob.class);
		GitModelTree tree = mock(GitModelTree.class);

		// when
		int actual = sorter.compare(viewer, blob, tree);

		// then
		assertTrue(actual > 0);
	}

	/*
	 * Tests for alphabetical order
	 */
	@Test
	public void shouldOrderTreesAlphabetically() {
		// given
		CommonViewer viewer = mock(CommonViewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelTree tree1 = mock(GitModelTree.class);
		GitModelTree tree2 = mock(GitModelTree.class);
		ILabelProvider labelProvider = mock(ILabelProvider.class);
		when(labelProvider.getText(tree1)).thenReturn("aaa");
		when(labelProvider.getText(tree2)).thenReturn("zzz");
		when(viewer.getLabelProvider()).thenReturn(labelProvider);

		// when
		int actual1 = sorter.compare(viewer, tree1, tree2);
		int actual2 = sorter.compare(viewer, tree2, tree1);

		// then
		assertTrue(actual1 < 0);
		assertTrue(actual2 > 0);
	}

	@Test public void shouldOrderBlobsAlphabetically() {
		// given
		CommonViewer viewer = mock(CommonViewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelBlob blob1 = mock(GitModelBlob.class);
		GitModelBlob blob2 = mock(GitModelBlob.class);
		ILabelProvider labelProvider = mock(ILabelProvider.class);
		when(labelProvider.getText(blob1)).thenReturn("aaa");
		when(labelProvider.getText(blob2)).thenReturn("zzz");
		when(viewer.getLabelProvider()).thenReturn(labelProvider);

		// when
		int actual1 = sorter.compare(viewer, blob1, blob2);
		int actual2 = sorter.compare(viewer, blob2, blob1);

		// then
		assertTrue(actual1 < 0);
		assertTrue(actual2 > 0);
	}

	/*
	 * Test for commit chronological order
	 */
	@Test public void shouldOrderCommitsByCommitDate() {
		// given
		Viewer viewer = mock(Viewer.class);
		GitChangeSetSorter sorter = new GitChangeSetSorter();
		GitModelCommit commit1 = mock(GitModelCommit.class);
		GitModelCommit commit2 = mock(GitModelCommit.class);
		Commit mockCommit1 = mock(Commit.class);
		Commit mockCommit2 = mock(Commit.class);
		when(mockCommit1.getCommitDate()).thenReturn(new Date(333333L));
		when(mockCommit2.getCommitDate()).thenReturn(new Date(555555L));
		when(commit1.getCachedCommitObj()).thenReturn(mockCommit1);
		when(commit2.getCachedCommitObj()).thenReturn(mockCommit2);

		// when
		int actual1 = sorter.compare(viewer, commit1, commit2);
		int actual2 = sorter.compare(viewer, commit2, commit1);

		// then
		assertTrue(actual1 > 0);
		assertTrue(actual2 < 0);
	}

}
