/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.compare.structuremergeviewer.Differencer.LEFT;
import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.ObjectId.fromString;
import static org.eclipse.jgit.lib.ObjectId.zeroId;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.BeforeClass;
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

	@Test public void shouldReturnEqualSameData1() throws Exception {
		// given
		GitModelBlob left = createGitModelBlob(zeroId(), getFile1Location());
		GitModelBlob right = createGitModelBlob(zeroId(), getFile1Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertTrue(actual);
	}

	@Test
	public void shouldBeSymetric() throws Exception {
		// given
		GitModelBlob left = createGitModelBlob(zeroId(), getFile1Location());
		GitModelBlob right = createGitModelBlob(zeroId(), getFile1Location());

		// when
		boolean actual1 = left.equals(right);
		boolean actual2 = right.equals(left);

		// then
		assertTrue(actual1 == actual2);
	}

	@Test
	public void shouldBeSymetric1() throws Exception {
		// given
		GitModelBlob left = createGitModelBlob(zeroId(), getFile1Location());
		GitModelCommit right = new GitModelCommit(createModelRepository(),
				getCommit(leftRepoFile, HEAD), LEFT);
		// GitModelBlob right = createGitModelBlob(zeroId(),
		// getFile1Location());

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
		GitModelBlob left = createGitModelBlob();
		GitModelCommit right = new GitModelCommit(createModelRepository(), getCommit(
				leftRepoFile, HEAD), LEFT);

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForBlobAndTree() throws Exception {
		// given
		GitModelBlob left = createGitModelBlob();
		GitModelTree right = new GitModelTree(createModelCommit(), getCommit(
				leftRepoFile, HEAD), null, null, null, null, getFile1Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForBlobAndCacheFile()
			throws Exception {
		// given
		GitModelBlob left = createGitModelBlob();
		GitModelCacheFile right = new GitModelCacheFile(createModelCommit(),
				getCommit(leftRepoFile, HEAD), null, null, getFile1Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForBlobAndWorkingFile()
			throws Exception {
		// given
		GitModelBlob left = createGitModelBlob();
		GitModelWorkingFile right = new GitModelWorkingFile(
				createModelCommit(), getCommit(leftRepoFile, HEAD), null,
				getFile1Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@BeforeClass public static void setupEnvironment() throws Exception {
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
		return new GitModelBlob(createModelCommit(),
				getCommit(leftRepoFile, HEAD), null, null, baseId,
				remoteId, location);
	}

}
