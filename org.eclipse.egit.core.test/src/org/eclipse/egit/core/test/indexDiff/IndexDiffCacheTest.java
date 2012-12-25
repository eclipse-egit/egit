/*******************************************************************************
 * Copyright (C) 2011, 2012 Jens Baumgart <jens.baumgart@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test.indexDiff;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IFile;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffChangedListener;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IndexDiffCacheTest extends GitTestCase {

	TestRepository testRepository;

	Repository repository;

	private AtomicBoolean listenerCalled;

	private AtomicReference<IndexDiffData> indexDiffDataResult;

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
		prepareCacheEntry();
		waitForListenerCalled();
		final String fileName = "aFile";
		// This call should trigger an indexDiffChanged event (triggered via
		// resource changed event)
		project.createFile(fileName, "content".getBytes("UTF-8"));
		IndexDiffData indexDiffData = waitForListenerCalled();
		String path = project.project.getFile(fileName).getFullPath()
				.toString().substring(1);
		if (!indexDiffData.getUntracked().contains(path))
			fail("IndexDiffData did not contain aFile as untracked");
		// This call should trigger an indexDiffChanged event
		new Git(repository).add().addFilepattern(path).call();
		IndexDiffData indexDiffData2 = waitForListenerCalled();
		if (indexDiffData2.getUntracked().contains(path))
			fail("IndexDiffData contains aFile as untracked");
		if (!indexDiffData2.getAdded().contains(path))
			fail("IndexDiffData does not contain aFile as added");
	}

	@Test
	public void testAddFileFromUntrackedFolder() throws Exception {
		testRepository.connect(project.project);
		testRepository.addToIndex(project.project);
		testRepository.createInitialCommit("testAddFileFromUntrackedFolder\n\nfirst commit\n");
		prepareCacheEntry();

		project.createFolder("folder");
		project.createFolder("folder/a");
		project.createFolder("folder/b");
		IFile fileA = project.createFile("folder/a/file", new byte[] {});
		project.createFile("folder/b/file", new byte[] {});

		IndexDiffData data1 = waitForListenerCalled();
		assertThat(data1.getUntrackedFolders(), hasItem("Project-1/folder/"));

		testRepository.track(fileA.getLocation().toFile());

		IndexDiffData data2 = waitForListenerCalled();
		assertThat(data2.getAdded(), hasItem("Project-1/folder/a/file"));
		assertThat(data2.getUntrackedFolders(), not(hasItem("Project-1/folder/")));
		assertThat(data2.getUntrackedFolders(), not(hasItem("Project-1/folder/a")));
		assertThat(data2.getUntrackedFolders(), hasItem("Project-1/folder/b/"));
	}

	@Test
	public void testAddIgnoredFolder() throws Exception {
		testRepository.connect(project.project);
		project.createFile(".gitignore", "ignore\n".getBytes("UTF-8"));
		project.createFolder("ignore");
		project.createFile("ignore/file.txt", new byte[] {});
		project.createFolder("sub");
		testRepository.addToIndex(project.project);
		testRepository.createInitialCommit("testAddFileInIgnoredFolder\n\nfirst commit\n");
		prepareCacheEntry();

		IndexDiffData data1 = waitForListenerCalled();
		assertThat(data1.getIgnoredNotInIndex(), hasItem("Project-1/ignore"));

		project.createFolder("sub/ignore");

		IndexDiffData data2 = waitForListenerCalled();
		assertThat(data2.getIgnoredNotInIndex(), hasItem("Project-1/ignore"));
		assertThat(data2.getIgnoredNotInIndex(), hasItem("Project-1/sub/ignore"));

		// Must not change anything (ignored path starts with this string, but
		// it's not a prefix path of it)
		project.createFile("sub/ignorenot", new byte[] {});

		IndexDiffData data3 = waitForListenerCalled();
		assertThat(data3.getUntracked(), hasItem("Project-1/sub/ignorenot"));
		assertThat(data3.getIgnoredNotInIndex(), hasItem("Project-1/ignore"));
		assertThat(data3.getIgnoredNotInIndex(), hasItem("Project-1/sub/ignore"));
	}

	@Test
	public void testRemoveIgnoredFile() throws Exception {
		testRepository.connect(project.project);
		project.createFile(".gitignore", "ignore\n".getBytes("UTF-8"));
		project.createFolder("sub");
		IFile file = project.createFile("sub/ignore", new byte[] {});
		testRepository.addToIndex(project.project);
		testRepository.createInitialCommit("testRemoveIgnoredFile\n\nfirst commit\n");
		prepareCacheEntry();

		IndexDiffData data1 = waitForListenerCalled();
		assertThat(data1.getIgnoredNotInIndex(), hasItem("Project-1/sub/ignore"));

		// Must not change anything (ignored path starts with this string, but
		// it's not a prefix path of it)
		project.createFile("sub/ignorenot", new byte[] {});

		IndexDiffData data2 = waitForListenerCalled();
		assertThat(data2.getIgnoredNotInIndex(), hasItem("Project-1/sub/ignore"));

		file.delete(false, null);

		IndexDiffData data3 = waitForListenerCalled();
		assertThat(data3.getIgnoredNotInIndex(), not(hasItem("Project-1/sub/ignore")));
	}

	private void prepareCacheEntry() {
		IndexDiffCache indexDiffCache = Activator.getDefault()
				.getIndexDiffCache();
		// This call should trigger an indexDiffChanged event
		IndexDiffCacheEntry cacheEntry = indexDiffCache
				.getIndexDiffCacheEntry(repository);
		listenerCalled = new AtomicBoolean(false);
		indexDiffDataResult = new AtomicReference<IndexDiffData>(
				null);
		cacheEntry.addIndexDiffChangedListener(new IndexDiffChangedListener() {
			public void indexDiffChanged(Repository repo,
					IndexDiffData indexDiffData) {
				listenerCalled.set(true);
				indexDiffDataResult.set(indexDiffData);
			}
		});
	}

	private IndexDiffData waitForListenerCalled() throws InterruptedException {
		long time = 0;
		while (!listenerCalled.get() && time < 60000) {
			Thread.sleep(100);
			time += 100;
		}
		assertTrue("indexDiffChanged was not called after " + time + " ms", listenerCalled.get());
		listenerCalled.set(false);
		return indexDiffDataResult.get();
	}

}
