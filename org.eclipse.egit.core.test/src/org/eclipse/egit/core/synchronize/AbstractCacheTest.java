/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.egit.core.synchronize.GitCommitsModelCache.ADDITION;
import static org.eclipse.egit.core.synchronize.GitCommitsModelCache.CHANGE;
import static org.eclipse.egit.core.synchronize.GitCommitsModelCache.DELETION;
import static org.eclipse.egit.core.synchronize.GitCommitsModelCache.RIGHT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.junit.LocalDiskRepositoryTestCase;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;

@SuppressWarnings("boxing")
public abstract class AbstractCacheTest extends LocalDiskRepositoryTestCase {

	protected Repository db;

	protected static final String INITIAL_TAG = "initial-tag";

	protected static final AbbreviatedObjectId ZERO_ID = AbbreviatedObjectId.fromObjectId(ObjectId.zeroId());

	@Before
	@Override
	// copied from org.eclipse.jgit.lib.RepositoryTestCase
	public void setUp() throws Exception {
		super.setUp();
		db = createWorkRepository();
		try (Git git = new Git(db)) {
			git.commit().setMessage("initial commit").call();
			git.tag().setName(INITIAL_TAG).call();
		}
	}

	protected void assertFileAddition(Map<String, Change> result, String path, String fileName) {
		commonFileAsserts(result, path, fileName);
		assertThat(result.get(path).getKind(), is(RIGHT | ADDITION));
		assertThat(result.get(path).getObjectId(), not(ZERO_ID));
		assertNull(result.get(path).getRemoteObjectId());
	}

	protected void assertFileDeletion(Map<String, Change> result, String path, String fileName) {
		commonFileAsserts(result, path, fileName);
		assertThat(result.get(path).getKind(), is(RIGHT | DELETION));
		assertThat(result.get(path).getRemoteObjectId(), not(ZERO_ID));
		assertNull(result.get(path).getObjectId());
	}

	protected void assertFileChange(Map<String, Change> result, String path, String fileName) {
		commonFileAsserts(result, path, fileName);
		assertThat(result.get(path).getKind(), is(RIGHT | CHANGE));
		assertThat(result.get(path).getObjectId(), not(ZERO_ID));
		assertThat(result.get(path).getRemoteObjectId(), not(ZERO_ID));
	}

	private void commonFileAsserts(Map<String, Change> result, String path,
			String fileName) {
		assertTrue(result.containsKey(path));
		assertThat(result.get(path).getName(), is(fileName));
	}
}
