/*******************************************************************************
 * Copyright (C) 2011, 2012 Jens Baumgart <jens.baumgart@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.indexdiff;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.OutputStream;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.Activator;
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

	private IndexDiffChangedListener indexDiffListener;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		testRepository = new TestRepository(gitDir);
		repository = testRepository.getRepository();
		listenerCalled = new AtomicBoolean(false);
		indexDiffDataResult = new AtomicReference<>(null);
		indexDiffListener = new IndexDiffChangedListener() {
			@Override
			public void indexDiffChanged(Repository repo,
					IndexDiffData indexDiffData) {
				listenerCalled.set(true);
				indexDiffDataResult.set(indexDiffData);
			}
		};
	}

	@Override
	@After
	public void tearDown() throws Exception {
		IndexDiffCache indexDiffCache = Activator.getDefault()
				.getIndexDiffCache();
		indexDiffCache.removeIndexDiffChangedListener(indexDiffListener);
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
		try (Git git = new Git(repository)) {
			git.add().addFilepattern(path).call();
		}
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

		IFile[] fileA = { null };
		runInWorkspace(() -> {
			project.createFolder("folder");
			project.createFolder("folder/a");
			project.createFolder("folder/b");
			fileA[0] = project.createFile("folder/a/file", new byte[] {});
			project.createFile("folder/b/file", new byte[] {});
			return null;
		});

		prepareCacheEntry();

		IndexDiffData data1 = waitForListenerCalled();
		assertThat(data1.getUntrackedFolders(), hasItem("Project-1/folder/"));

		testRepository.track(fileA[0].getLocation().toFile());

		IndexDiffData data2 = waitForListenerCalled();
		assertThat(data2.getAdded(), hasItem("Project-1/folder/a/file"));
		assertThat(data2.getUntrackedFolders(), not(hasItem("Project-1/folder/")));
		assertThat(data2.getUntrackedFolders(), not(hasItem("Project-1/folder/a")));
		assertThat(data2.getUntrackedFolders(), hasItem("Project-1/folder/b/"));
	}

	@Test
	public void testAddIgnoredFolder() throws Exception {
		testRepository.connect(project.project);
		runInWorkspace(() -> {
			project.createFile(".gitignore", "ignore\n".getBytes("UTF-8"));
			project.createFolder("ignore");
			project.createFile("ignore/file.txt", new byte[] {});
			project.createFolder("sub");
			return null;
		});
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
		IFile file[] = { null };
		runInWorkspace(() -> {
			project.createFile(".gitignore", "ignore\n".getBytes("UTF-8"));
			project.createFolder("sub");
			file[0] = project.createFile("sub/ignore", new byte[] {});
			return null;
		});
		testRepository.addToIndex(project.project);
		testRepository.createInitialCommit("testRemoveIgnoredFile\n\nfirst commit\n");
		IndexDiffCacheEntry entry = prepareCacheEntry();

		IndexDiffData data1 = waitForListenerCalled();
		assertThat(data1.getIgnoredNotInIndex(), hasItem("Project-1/sub/ignore"));

		// Must not change anything (ignored path starts with this string, but
		// it's not a prefix path of it)
		project.createFile("sub/ignorenot", new byte[] {});

		IndexDiffData data2 = waitForListenerCalled();
		assertThat(data2.getIgnoredNotInIndex(), hasItem("Project-1/sub/ignore"));

		file[0].delete(false, null);

		waitForListenerNotCalled();
		entry.refresh(); // need explicit as ignored file shall not trigger.
		IndexDiffData data3 = waitForListenerCalled();
		assertThat(data3.getIgnoredNotInIndex(), not(hasItem("Project-1/sub/ignore")));
	}

	@Test
	public void testAddAndRemoveGitIgnoreFileToIgnoredDir() throws Exception {
		testRepository.connect(project.project);
		runInWorkspace(() -> {
			project.createFile(".gitignore", "ignore\n".getBytes("UTF-8"));
			project.createFolder("sub");
			project.createFile("sub/ignore", new byte[] {});
			return null;
		});
		testRepository.addToIndex(project.project);
		testRepository
				.createInitialCommit("testRemoveIgnoredFile\n\nfirst commit\n");
		prepareCacheEntry();

		IndexDiffData data1 = waitForListenerCalled();
		assertThat(data1.getIgnoredNotInIndex(),
				hasItem("Project-1/sub/ignore"));

		project.createFile("sub/ignored", "Ignored".getBytes("UTF-8"));

		waitForListenerCalled();

		// adding this file will trigger a refresh, so no manual refresh must be
		// required.
		project.createFile("sub/.gitignore", "ignored\n".getBytes("UTF-8"));

		IndexDiffData data2 = waitForListenerCalled();
		assertThat(data2.getIgnoredNotInIndex(),
				hasItem("Project-1/sub/ignored"));

		// removing must also trigger the listener
		project.getProject().getFile("sub/.gitignore").delete(true, null);

		IndexDiffData data3 = waitForListenerCalled();
		assertThat(data3.getUntracked(), hasItem("Project-1/sub/ignored"));
	}

	@Test
	public void testAddAndRemoveFileToIgnoredDir() throws Exception {
		testRepository.connect(project.project);
		runInWorkspace(() -> {
			project.createFile(".gitignore", "sub\n".getBytes("UTF-8"));
			project.createFolder("sub");
			project.createFile("sub/ignore", new byte[] {});
			return null;
		});
		testRepository.addToIndex(project.project);
		testRepository
				.createInitialCommit("testRemoveIgnoredFile\n\nfirst commit\n");
		prepareCacheEntry();

		IndexDiffData data1 = waitForListenerCalled();
		assertThat(data1.getIgnoredNotInIndex(), hasItem("Project-1/sub"));

		// creating a file in an ignored directory will not trigger the listener
		project.createFile("sub/ignored", "Ignored".getBytes("UTF-8"));
		waitForListenerNotCalled();

		// removing must also not trigger the listener
		project.getProject().getFile("sub/ignored").delete(true, null);
		waitForListenerNotCalled();
	}

	@Test
	public void testModifyFileInIgnoredDir() throws Exception {
		testRepository.connect(project.project);
		runInWorkspace(() -> {
			project.createFile(".gitignore", "ignore\n".getBytes("UTF-8"));
			project.createFolder("sub");
			project.createFile("sub/ignore", new byte[] {});
			return null;
		});
		testRepository.addToIndex(project.project);
		testRepository
				.createInitialCommit("testRemoveIgnoredFile\n\nfirst commit\n");
		prepareCacheEntry();

		IndexDiffData data1 = waitForListenerCalled();
		assertThat(data1.getIgnoredNotInIndex(),
				hasItem("Project-1/sub/ignore"));

		IFile file = project.getProject().getFile("sub/ignore");
		try (OutputStream str = Files
				.newOutputStream((file.getLocation().toFile().toPath()))) {
			str.write("other contents".getBytes("UTF-8"));
		}

		// no job should be triggered for that change.
		waitForListenerNotCalled();
	}

	private void runInWorkspace(Callable<Void> action) throws CoreException {
		ResourcesPlugin.getWorkspace().run(m -> {
			try {
				action.call();
			} catch (Exception e) {
				throw new CoreException(
						Activator.error("Test preparation error", e));
			}
		}, project.getProject(), IWorkspace.AVOID_UPDATE, null);
	}

	private IndexDiffCacheEntry prepareCacheEntry() {
		listenerCalled.set(false);
		indexDiffDataResult.set(null);

		IndexDiffCache indexDiffCache = Activator.getDefault()
				.getIndexDiffCache();
		indexDiffCache.addIndexDiffChangedListener(indexDiffListener);
		// This call should trigger an indexDiffChanged event
		IndexDiffCacheEntry cacheEntry = indexDiffCache
				.getIndexDiffCacheEntry(repository);
		return cacheEntry;
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

	private void waitForListenerNotCalled() throws InterruptedException {
		long time = 0;
		while (!listenerCalled.get() && time < 1000) {
			Thread.sleep(100);
			time += 100;
		}
		assertTrue("indexDiffChanged was called where it shouldn't have been",
				!listenerCalled.get());
	}

}
