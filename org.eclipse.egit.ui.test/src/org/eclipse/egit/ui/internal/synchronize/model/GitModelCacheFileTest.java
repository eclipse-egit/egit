/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.BeforeClass;
import org.junit.Test;

public class GitModelCacheFileTest extends GitModelTestCase {

	@Test public void shouldReturnEqualForSameInstance() throws Exception {
		// given
		GitModelCacheFile left = createCacheFile(zeroId(), zeroId(),
				getFile1Location());

		// when
		boolean actual = left.equals(left);

		// then
		assertTrue(actual);
	}

	@Test public void shouldReturnEqualForSameObjectIdsAndLocation()
			throws Exception {
		// given
		GitModelCacheFile left = createCacheFile(zeroId(),
				fromString("390b6b146aa218a9c985e6ce9df2845eb575be48"),
				getFile1Location());
		GitModelCacheFile right = createCacheFile(zeroId(),
				fromString("390b6b146aa218a9c985e6ce9df2845eb575be48"),
				getFile1Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertTrue(actual);
	}
	
	@Test public void shouldReturnNotEqualForDifferentBaseIds()
			throws Exception {
		// given
		GitModelCacheFile left = createCacheFile(
				fromString("390b6b146aa218a9c985e6ce9df2845eb575be48"),
				fromString("390b6b146aa218a9c985e6ce9df2845eb0000000"),
				getFile1Location());
		GitModelCacheFile right = createCacheFile(zeroId(),
				fromString("390b6b146aa218a9c985e6ce9df2845eb575be48"),
				getFile1Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForDifferentCacheIds()
			throws Exception {
		// given
		GitModelCacheFile left = createCacheFile(zeroId(),
				fromString("390b6b146aa218a9c985e6ce9df2845eb575be48"),
				getFile1Location());
		GitModelCacheFile right = createCacheFile(zeroId(),
				fromString("000000006aa218a9c985e6ce9df2845eb575be48"),
				getFile1Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualForDifferentLocations()
			throws Exception {
		// given
		GitModelCacheFile left = createCacheFile(zeroId(),
				fromString("000000006aa218a9c985e6ce9df2845eb575be48"),
				getFile1Location());
		GitModelCacheFile right = createCacheFile(zeroId(),
				fromString("000000006aa218a9c985e6ce9df2845eb575be48"),
				getFile2Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualWhenComparingCacheFileAndBlob()
			throws Exception {
		// given
		GitModelCacheFile left = createCacheFile(zeroId(),
				fromString("000000006aa218a9c985e6ce9df2845eb575be48"),
				getFile1Location());
		GitModelBlob right = new GitModelBlob(createModelCommit(), getCommit(
				leftRepoFile, HEAD), null, null, null, null, getFile1Location());

		// when
		boolean actual = left.equals(right);

		// then
		assertFalse(actual);
	}

	@Test public void shouldReturnNotEqualWhenComparingCacheFileAndWorkingFile()
			throws Exception {
		// given
		GitModelCacheFile left = createCacheFile(zeroId(),
				fromString("000000006aa218a9c985e6ce9df2845eb575be48"),
				getFile1Location());
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

	private GitModelCacheFile createCacheFile(ObjectId repoId,
			ObjectId cacheId, IPath location) throws Exception {
		return new GitModelCacheFile(createModelCommit(),
				getCommit(leftRepoFile, HEAD), repoId, cacheId, location);
	}
	
}
