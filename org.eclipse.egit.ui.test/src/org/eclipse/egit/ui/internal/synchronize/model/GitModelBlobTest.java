/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.ObjectId.fromString;
import static org.eclipse.jgit.lib.ObjectId.zeroId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class GitModelBlobTest extends GitModelTestCase {

	@Test public void shouldReturnEqualForSameInstance() throws Exception {
		// given
		GitModelBlob left = createGitModelBlob();

		// when
		boolean actual = left.equals(left);

		// then
		assertTrue(actual);
	}

	@Test public void shouldReturnNotEqualForDifferentLocation()
			throws Exception {
		// given
		GitModelBlob left = createGitModelBlob(zeroId(), getFile1Location());
		GitModelBlob right = createGitModelBlob(zeroId(), getFile2Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Ignore
	// this test case relies on
	// org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change#equals()
	// implementation. Unfortunately in mockito we can't execute real
	// implementation of equals() method, therefore this test will fail
	@Test public void shouldReturnEqualForSameData() throws Exception {
		// given
		GitModelBlob left = createGitModelBlob(zeroId(), zeroId(),
				getFile1Location());
		GitModelBlob right = createGitModelBlob(zeroId(), zeroId(),
				getFile1Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertTrue(actual);
	}

	@Ignore
	// this test case relies on
	// org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change#equals()
	// implementation. Unfortunately in mockito we can't execute real
	// implementation of equals() method, therefore this test will fail
	@Test public void shouldReturnEqualSameData1() throws Exception {
		// given
		GitModelBlob left = createGitModelBlob(zeroId(), getFile1Location());
		GitModelBlob right = createGitModelBlob(zeroId(), getFile1Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertTrue(actual);
	}

	@SuppressWarnings("boxing")
	@Test
	public void shouldBeSymmetric() throws Exception {
		// given
		GitModelBlob left = createGitModelBlob(zeroId(), getFile1Location());
		GitModelBlob right = createGitModelBlob(zeroId(), getFile1Location());

		// when
		boolean actual1 = left.equals(right);
		boolean actual2 = right.equals(left);

		// then
		assertEquals(actual1, actual2);
	}

	@Test
	public void shouldBeSymmetric1() throws Exception {
		// given
		GitModelObject left = createGitModelBlob(zeroId(), getFile1Location());
		GitModelObject right = new GitModelCommit(createModelRepository(),
				lookupRepository(leftRepoFile), getCommit(leftRepoFile, HEAD),
				null);

		// when
		boolean actual1 = left.equals(right);
		boolean actual2 = right.equals(left);

		// then
		assertTrue(!actual1);
		assertTrue(!actual2);
	}

	@Test public void shouldReturnNotEqualForDifferentFiles()
			throws Exception {
		// given
		GitModelBlob left = createGitModelBlob();
		GitModelBlob right = createGitModelBlob(zeroId(), getFile2Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForDifferentBaseObjectId()
			throws Exception {
		// given
		GitModelBlob left = createGitModelBlob(zeroId(), getFile1Location());
		GitModelBlob right = createGitModelBlob(
				fromString("4c879313cd1332e594b1ad20b1485bdff9533034"),
				getFile1Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForDifferentBaseObjectId2()
			throws Exception {
		// given
		GitModelBlob left = createGitModelBlob(zeroId(), zeroId(),
				getFile1Location());
		GitModelBlob right = createGitModelBlob(
				fromString("4c879313cd1332e594b1ad20b1485bdff9533034"),
				null, getFile1Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForDifferentBaseObjectId3()
			throws Exception {
		// given
		GitModelBlob left = createGitModelBlob(zeroId(), ObjectId.zeroId(),
				getFile1Location());
		GitModelBlob right = createGitModelBlob(
				fromString("4c879313cd1332e594b1ad20b1485bdff9533034"),
				fromString("4c879313cd1332e0000000000000000111122233"),
				getFile2Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForBlobAndCommit() throws Exception {
		// given
		GitModelObject left = createGitModelBlob();
		GitModelObject right = new GitModelCommit(createModelRepository(),
				lookupRepository(leftRepoFile), getCommit(leftRepoFile, HEAD),
				null);

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForBlobAndTree() throws Exception {
		// given
		GitModelObject left = createGitModelBlob();
		GitModelObject right = mock(GitModelTree.class);

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForBlobAndCacheFile()
			throws Exception {
		// given
		GitModelBlob left = createGitModelBlob();
		GitModelCacheFile right = mock(GitModelCacheFile.class);

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForBlobAndWorkingFile()
			throws Exception {
		// given
		GitModelBlob left = createGitModelBlob();
		GitModelWorkingFile right = mock(GitModelWorkingFile.class);

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldActAsResourceProvider()
			throws Exception {
		// given
		GitModelBlob left = createGitModelBlob();

		// then
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFile(new Path("folder/test.txt"));
		IPath leftLocation = left.getResource().getLocation();
		assertEquals(file.getLocation(), leftLocation);
	}

	@Before
	public void setupEnvironment() throws Exception {
		leftRepoFile = createProjectAndCommitToRepository();

		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(leftRepoFile);
	}

	private GitModelBlob createGitModelBlob() throws Exception {
		return createGitModelBlob(null, getFile1Location());
	}

	private GitModelBlob createGitModelBlob(ObjectId baseId, IPath location)
			throws IOException, Exception {
		return createGitModelBlob(baseId, null, location);
	}

	private GitModelBlob createGitModelBlob(ObjectId baseId, ObjectId remoteId,
			IPath location) throws Exception {
		Change change = mock(Change.class);
		if (baseId != null)
			when(change.getObjectId()).thenReturn(
					AbbreviatedObjectId.fromObjectId(baseId));
		if (remoteId != null)
			when(change.getRemoteObjectId()).thenReturn(
					AbbreviatedObjectId.fromObjectId(remoteId));

		return new GitModelBlob(createModelCommit(),
				lookupRepository(leftRepoFile), change, location);
	}

}
