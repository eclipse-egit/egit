/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test.indexDiff;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffChangedListener;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.events.IndexChangedEvent;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IndexDiffCacheTest extends GitTestCase {

	TestRepository testRepository;

	Repository repository;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		testRepository = new TestRepository(gitDir);
		repository = testRepository.getRepository();
	}

	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		repository = null;
		super.tearDown();
	}

	@Test
	public void testAddingAFile() throws Exception {
		new ConnectProviderOperation(project.project, repository.getDirectory())
				.execute(null);
		// create first commit containing a dummy file
		testRepository
				.createInitialCommit("testBranchOperation\n\nfirst commit\n");
		IndexDiffCache indexDiffCache = Activator.getDefault()
				.getIndexDiffCache();
		// This call should trigger an indexDiffChanged event
		IndexDiffCacheEntry cacheEntry = indexDiffCache
				.getIndexDiffCacheEntry(repository);
		final AtomicBoolean listenerCalled = new AtomicBoolean(false);
		final AtomicReference<IndexDiffData> resultDiff = new AtomicReference<IndexDiffData>(
				null);
		cacheEntry.addIndexDiffChangedListener(new IndexDiffChangedListener() {
			public void indexDiffChanged(Repository repo,
					IndexDiffData indexDiffData) {
				listenerCalled.set(true);
				resultDiff.set(indexDiffData);
			}
		});
		waitForListenerCalled(listenerCalled);
		final String fileName = "aFile";
		// This call should trigger an indexDiffChanged event (triggered via
		// resource changed event)
		project.createFile(fileName, "content".getBytes("UTF-8"));
		waitForListenerCalled(listenerCalled);
		IndexDiffData indexDiffData = resultDiff.get();
		String path = project.project.getFile(fileName).getFullPath()
				.toString().substring(1);
		if (!indexDiffData.getUntracked().contains(path))
			fail("IndexDiffData did not contain aFile as untracked");
		new Git(repository).add().addFilepattern(path).call();
		// This call should trigger an indexDiffChanged event
		repository.fireEvent(new IndexChangedEvent());
		waitForListenerCalled(listenerCalled);
		indexDiffData = resultDiff.get();
		if (indexDiffData.getUntracked().contains(path))
			fail("IndexDiffData contains aFile as untracked");
		if (!indexDiffData.getAdded().contains(path))
			fail("IndexDiffData does not contain aFile as added");
	}

	private void waitForListenerCalled(final AtomicBoolean listenerCalled)
			throws InterruptedException {
		long time = 0;
		while (!listenerCalled.get() && time < 10000) {
			Thread.sleep(1);
			time++;
		}
		assertTrue("indexDiffChanged was not called", listenerCalled.get());
		listenerCalled.set(false);
	}

}
