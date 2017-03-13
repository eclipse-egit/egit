/*******************************************************************************
 * Copyright (C) 2013 Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.eclipse.core.resources.IStorage;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.IO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IndexFileRevisionTest extends GitTestCase {
	private Repository repository;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		repository = FileRepositoryBuilder.create(gitDir);
		repository.create();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		repository.close();
		super.tearDown();
	}

	@Test
	public void stage0WithoutOtherStages() throws Exception {
		buildIndex(entry("file", 0, "data"));

		IndexFileRevision revision = new IndexFileRevision(repository, "file");
		assertEquals("data", readContents(revision));
	}

	@Test
	public void stage1WithOtherStages() throws Exception {
		buildIndex(entry("other", 0, ""), entry("file", 1, "right"),
				entry("file", 2, "wrong"));

		IndexFileRevision revision = new IndexFileRevision(repository, "file",
				1);
		assertEquals("right", readContents(revision));
	}

	@Test
	public void stage3With2Missing() throws Exception {
		buildIndex(entry("file", 1, "wrong"), entry("file", 3, "right"));

		IndexFileRevision revision = new IndexFileRevision(repository, "file",
				3);
		assertEquals("right", readContents(revision));
	}

	@Test
	public void missingStage() throws Exception {
		buildIndex(entry("file", 1, "wrong"), entry("file", 2, "wrong"));

		IndexFileRevision revision = new IndexFileRevision(repository, "file",
				3);
		assertEquals("", readContents(revision));
	}

	@Test
	public void missingEntry() throws Exception {
		buildIndex(entry("other", 0, "wrong"));

		IndexFileRevision revision = new IndexFileRevision(repository, "file",
				0);
		assertEquals("", readContents(revision));
	}

	@Test
	// This is to maintain compatibility with the current behavior
	public void shouldUseFirstWhenNotSpecified() throws Exception {
		buildIndex(entry("file", 1, "right"));

		IndexFileRevision revision = new IndexFileRevision(repository, "file");
		assertEquals("right", readContents(revision));
	}

	private void buildIndex(DirCacheEntry... entries) throws IOException {
		DirCache dirCache = repository.lockDirCache();
		DirCacheBuilder builder = dirCache.builder();
		try {
			for (DirCacheEntry entry : entries)
				builder.add(entry);
			builder.commit();
		} finally {
			dirCache.unlock();
		}
	}

	private DirCacheEntry entry(String path, int stage, String data)
			throws IOException {
		DirCacheEntry entry = new DirCacheEntry(path, stage);
		entry.setFileMode(FileMode.REGULAR_FILE);
		try (ObjectInserter inserter = repository.newObjectInserter()) {
			ObjectId blob = inserter.insert(Constants.OBJ_BLOB,
					data.getBytes("UTF-8"));
			entry.setObjectId(blob);
			inserter.flush();
		}
		return entry;
	}

	private static String readContents(IndexFileRevision revision)
			throws Exception {
		IStorage storage = revision.getStorage(null);
		InputStream in = storage.getContents();
		ByteBuffer buffer = IO.readWholeStream(in, 10);
		return new String(buffer.array(), 0, buffer.limit(), "UTF-8");
	}
}
