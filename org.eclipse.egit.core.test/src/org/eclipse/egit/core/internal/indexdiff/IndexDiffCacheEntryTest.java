/*******************************************************************************
 * Copyright (C) 2015 Andrey Loskutov <loskutov@gmx.de> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.indexdiff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.function.BooleanSupplier;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.core.runtime.jobs.ProgressProvider;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.egit.core.test.TestUtils;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IndexDiffCacheEntryTest extends GitTestCase {

	// trigger reload if more than one file is changed
	private static final int MAX_FILES_FOR_UPDATE = 1;

	private static final long MAX_WAIT_TIME = 10 * 1000;


	private TestRepository testRepository;

	private Repository repository;

	private IndexDiffCacheEntry2 entry;

	private HoldingReloads reloads;

	private final List<Cache> caches = new ArrayList<>();

	@Test
	public void basicTest() throws Exception {
		prepareCacheEntry();

		entry.refresh();
		TestUtils.waitForJobs(MAX_WAIT_TIME, JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		// on refresh, full reload is triggered
		assertTrue(entry.reloadScheduled);
		assertFalse(entry.updateScheduled);
		cleanEntryFlags();

		entry.refreshFiles(Arrays.asList("a"));
		TestUtils.waitForJobs(MAX_WAIT_TIME, JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		// one single file: no reload
		assertFalse(entry.reloadScheduled);
		assertTrue(entry.updateScheduled);
		cleanEntryFlags();

		entry.refreshFiles(Arrays.asList("a", "b"));
		TestUtils.waitForJobs(MAX_WAIT_TIME, JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		// two files: update is triggered, but decides to run full reload
		assertTrue(entry.reloadScheduled);
		assertTrue(entry.updateScheduled);
		cleanEntryFlags();

		entry.getUpdateJob().addChanges(Arrays.asList("a", "b"),
				Collections.<IResource> emptyList());
		TestUtils.waitForJobs(MAX_WAIT_TIME, JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		// two files: update is not triggered (we call through the job directly)
		// but this calls full reload
		assertTrue(entry.reloadScheduled);
		assertFalse(entry.updateScheduled);
		cleanEntryFlags();
	}

	@Test
	public void testProjectDeletion() throws Exception {
		prepareCacheEntry();

		testRepository.connect(project.project);
		TestUtils.waitForJobs(MAX_WAIT_TIME, JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		// Should have something from the project
		String projectName = project.project.getName();
		assertTrue(containsItemsStartingWith(
				entry.getIndexDiff().getUntracked(), projectName + '/'));

		project.project.delete(true, null);
		TestUtils.waitForJobs(MAX_WAIT_TIME, JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		assertFalse(containsItemsStartingWith(
				entry.getIndexDiff().getUntracked(), projectName + '/'));
	}

	@Test
	public void testReloadAndUpdate() throws Exception {
		prepareCacheEntry();

		testRepository.connect(project.project);
		TestUtils.waitForJobs(MAX_WAIT_TIME, JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		// on a simple connect, nothing should be called
		assertFalse(entry.reloadScheduled);
		assertFalse(entry.updateScheduled);
		cleanEntryFlags();

		// adds .project and .classpath files: more than limit of 1,
		// so update redirects to reload
		testRepository.addToIndex(project.project);
		TestUtils.waitForJobs(MAX_WAIT_TIME, JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		assertTrue(entry.reloadScheduled);
		assertTrue(entry.updateScheduled);
		cleanEntryFlags();

		testRepository.createInitialCommit("first commit\n");
		TestUtils.waitForJobs(MAX_WAIT_TIME, JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		// RefsChangedEvent causes always full update
		assertTrue(entry.reloadScheduled);
		// single "dummy" file from commit
		assertTrue(entry.updateScheduled);
		cleanEntryFlags();

		ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				try {
					project.createFile("bla", "bla\n".getBytes("UTF-8"));
					project.createFile("blup", "blup\n".getBytes("UTF-8"));
				} catch (Exception e) {
					throw new CoreException(Activator.error("Failure", e));
				}

			}
		}, null);
		TestUtils.waitForJobs(MAX_WAIT_TIME, JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		// adds 2 files: more than limit of 1,
		// so update redirects to reload
		assertTrue(entry.reloadScheduled);
		assertTrue(entry.updateScheduled);
		cleanEntryFlags();

		ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				try {
					project.createFile("blip", "blip\n".getBytes("UTF-8"));
				} catch (Exception e) {
					throw new CoreException(Activator.error("Failure", e));
				}

			}
		}, null);
		TestUtils.waitForJobs(MAX_WAIT_TIME, JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		// adds 1 file: less than limit of 1, so no reload
		assertFalse(entry.reloadScheduled);
		assertTrue(entry.updateScheduled);
		cleanEntryFlags();

		ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				try {
					project.createFile(".gitignore", "\n".getBytes("UTF-8"));
				} catch (Exception e) {
					throw new CoreException(Activator.error("Failure", e));
				}

			}
		}, null);
		TestUtils.waitForJobs(MAX_WAIT_TIME, JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		// adds .gitignore file: always full reload
		assertTrue(entry.reloadScheduled);
		assertFalse(entry.updateScheduled);
		cleanEntryFlags();
	}

	// The reload jobs share a job group that allows two of them at a time. In
	// the following tests two reloads are held in the middle of their
	// calculation while a third one waits for a slot. The tests differ in what
	// happens next: which reload fails or is canceled, and by whom.

	@Test
	public void testFailedReloadDoesNotBlockOtherReloads() throws Exception {
		Cache first = startReload("first");
		Cache second = startReload("second");
		Cache third = queueReload("third", true);
		second.entry.refresh();

		// The first reload ends, the third one takes its slot and fails
		reloads.proceed(0);
		assertTrue(reloads.awaitHeld(3));
		reloads.proceed(2);
		assertTrue(reloads.awaitDone(2));
		reloads.proceedAll();

		assertReloaded(first, 1);
		assertReloaded(second, 2);
		assertReloaded(third, 0);
	}

	@Test
	public void testCanceledWaitingReloadDoesNotBlockItsEntry()
			throws Exception {
		Cache first = startReload("first");
		Cache second = startReload("second");
		Cache third = queueReload("third", false);

		waitingReload().cancel();
		reloads.proceedAll();

		assertReloaded(first, 1);
		assertReloaded(second, 1);
		assertReloaded(third, 0);
		third.entry.refresh();
		assertReloaded(third, 1);
	}

	@Test
	public void testCanceledRunningReloadDoesNotBlockOtherReloads()
			throws Exception {
		Cache first = startReload("first");
		Cache second = startReload("second");
		Cache third = queueReload("third", false);

		// The third reload takes the slot of the canceled one
		reloads.cancel(0);
		assertTrue(reloads.awaitHeld(3));
		reloads.proceedAll();

		assertReloaded(first, 0);
		assertReloaded(second, 1);
		assertReloaded(third, 1);
	}

	@Test
	public void testCanceledReloadsDoNotBlockLaterReloads() throws Exception {
		Cache first = startReload("first");
		Cache second = startReload("second");
		Cache third = queueReload("third", false);

		// As IndexDiffCache.dispose() does
		Job.getJobManager().cancel(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		reloads.proceedAll();

		assertReloaded(first, 0);
		assertReloaded(second, 0);
		assertReloaded(third, 0);
		for (Cache cache : Arrays.asList(first, second, third)) {
			cache.entry.refresh();
			assertReloaded(cache, 1);
		}
	}

	@Test
	public void testCanceledReloadGroupDoesNotBlockLaterReloads()
			throws Exception {
		Cache first = startReload("first");
		Cache second = startReload("second");
		Cache third = queueReload("third", false);

		JobGroup group = waitingReload().getJobGroup();
		group.cancel();
		assertTrue(waitFor(() -> group.getState() == JobGroup.NONE));
		reloads.proceedAll();

		assertReloaded(first, 0);
		assertReloaded(second, 0);
		assertReloaded(third, 0);
		for (Cache cache : Arrays.asList(first, second, third)) {
			cache.entry.refresh();
			assertReloaded(cache, 1);
		}
	}

	private Cache startReload(String name) throws Exception {
		if (reloads == null) {
			reloads = new HoldingReloads();
			Job.getJobManager().setProgressProvider(reloads);
		}
		int held = reloads.heldCount();
		Cache cache = createCache(name, false);
		assertTrue("Reload of " + name + " did not start",
				reloads.awaitHeld(held + 1));
		return cache;
	}

	private Cache queueReload(String name, boolean unreadableIndex)
			throws Exception {
		Cache cache = createCache(name, unreadableIndex);
		assertTrue("Reload of " + name + " is not waiting",
				waitForJobCount(Job.WAITING, 1));
		return cache;
	}

	private Cache createCache(String name, boolean unreadableIndex)
			throws IOException {
		TestRepository testRepo = new TestRepository(
				new File(testUtils.createTempDir(name), Constants.DOT_GIT));
		if (unreadableIndex) {
			// An index that is a directory cannot be read
			FileUtils.mkdir(testRepo.getRepository().getIndexFile());
		}
		Semaphore notifications = new Semaphore(0);
		IndexDiffCacheEntry cacheEntry = new IndexDiffCacheEntry(
				testRepo.getRepository(),
				(repo, data) -> notifications.release());
		Cache cache = new Cache(name, testRepo, cacheEntry, notifications);
		caches.add(cache);
		return cache;
	}

	private void assertReloaded(Cache cache, int times) throws Exception {
		TestUtils.waitForJobs(MAX_WAIT_TIME,
				JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		assertEquals("Reloads of " + cache.name, times,
				cache.notifications.availablePermits());
	}

	private static Job waitingReload() {
		for (Job job : Job.getJobManager()
				.find(JobFamilies.INDEX_DIFF_CACHE_UPDATE)) {
			if (job.getState() == Job.WAITING) {
				return job;
			}
		}
		throw new AssertionError("No waiting reload");
	}

	private static boolean waitForJobCount(int state, int expected)
			throws InterruptedException {
		return waitFor(() -> {
			int count = 0;
			for (Job job : Job.getJobManager()
					.find(JobFamilies.INDEX_DIFF_CACHE_UPDATE)) {
				if (job.getState() == state) {
					count++;
				}
			}
			return count == expected;
		});
	}

	private static boolean waitFor(BooleanSupplier condition)
			throws InterruptedException {
		long end = System.currentTimeMillis() + MAX_WAIT_TIME;
		while (!condition.getAsBoolean()) {
			if (System.currentTimeMillis() > end) {
				return false;
			}
			Thread.sleep(50);
		}
		return true;
	}

	private void cleanEntryFlags() {
		entry.reloadScheduled = false;
		entry.updateScheduled = false;
	}

	private IndexDiffCacheEntry2 prepareCacheEntry() throws Exception {
		entry = new IndexDiffCacheEntry2(repository);
		TestUtils.waitForJobs(MAX_WAIT_TIME, JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		// on creation, full reload is triggered
		assertTrue(entry.reloadScheduled);
		assertFalse(entry.updateScheduled);
		cleanEntryFlags();
		return entry;
	}

	private boolean containsItemsStartingWith(Collection<String> values,
			String prefix) {
		for (String item : values) {
			if (item.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		testRepository = new TestRepository(gitDir);
		repository = testRepository.getRepository();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		if (reloads != null) {
			reloads.proceedAll();
		}
		for (Cache cache : caches) {
			cache.entry.dispose();
			cache.repository.dispose();
		}
		if (!caches.isEmpty()) {
			testUtils.deleteTempDirs();
		}
		if (reloads != null) {
			Job.getJobManager().setProgressProvider(null);
		}
		if (entry != null) {
			entry.dispose();
		}
		testRepository.dispose();
		repository = null;
		super.tearDown();
	}

	private static class Cache {

		final String name;

		final TestRepository repository;

		final IndexDiffCacheEntry entry;

		// One permit per notification of the entry's listener
		final Semaphore notifications;

		Cache(String name, TestRepository repository, IndexDiffCacheEntry entry,
				Semaphore notifications) {
			this.name = name;
			this.repository = repository;
			this.entry = entry;
			this.notifications = notifications;
		}
	}

	/**
	 * Holds each reload job at the beginning of its calculation until it is
	 * told to proceed or is canceled. The jobs are numbered in the order in
	 * which they start.
	 */
	private static class HoldingReloads extends ProgressProvider {

		private final List<HeldReload> started = new CopyOnWriteArrayList<>();

		private volatile boolean proceedAll;

		@Override
		public IProgressMonitor createMonitor(Job job) {
			if (job.getJobGroup() == null
					|| !job.belongsTo(JobFamilies.INDEX_DIFF_CACHE_UPDATE)) {
				return null;
			}
			HeldReload reload = new HeldReload(job);
			started.add(reload);
			return reload;
		}

		/**
		 * Counts the reloads that have been held so far.
		 *
		 * @return the number of reloads held so far
		 */
		int heldCount() {
			int count = 0;
			for (HeldReload reload : started) {
				if (reload.held) {
					count++;
				}
			}
			return count;
		}

		/**
		 * Waits until the given number of reloads have been held.
		 *
		 * @param count
		 *            the number of reloads held so far to wait for
		 * @return {@code false} if the wait timed out
		 * @throws InterruptedException
		 *             if interrupted while waiting
		 */
		boolean awaitHeld(int count) throws InterruptedException {
			return waitFor(() -> heldCount() == count);
		}

		/**
		 * Waits until a reload has ended.
		 *
		 * @param index
		 *            number of the reload, in the order the reloads started
		 * @return {@code false} if the wait timed out
		 * @throws InterruptedException
		 *             if interrupted while waiting
		 */
		boolean awaitDone(int index) throws InterruptedException {
			return waitFor(() -> started.get(index).job.getState() == Job.NONE);
		}

		/**
		 * Lets a held reload continue.
		 *
		 * @param index
		 *            number of the reload, in the order the reloads started
		 */
		void proceed(int index) {
			started.get(index).proceed = true;
		}

		/** Lets all held and all future reloads continue. */
		void proceedAll() {
			proceedAll = true;
		}

		/**
		 * Cancels a reload.
		 *
		 * @param index
		 *            number of the reload, in the order the reloads started
		 */
		void cancel(int index) {
			started.get(index).job.cancel();
		}

		private class HeldReload extends NullProgressMonitor {

			final Job job;

			volatile boolean held;

			volatile boolean proceed;

			HeldReload(Job job) {
				this.job = job;
			}

			@Override
			public boolean isCanceled() {
				if (!held && Job.getJobManager().currentJob() == job) {
					held = true;
					while (!proceed && !proceedAll && !super.isCanceled()) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							break;
						}
					}
				}
				return super.isCanceled();
			}
		}
	}

	private static class IndexDiffCacheEntry2 extends IndexDiffCacheEntry {

		boolean reloadScheduled;

		boolean updateScheduled;

		public IndexDiffCacheEntry2(Repository repository) {
			super(repository, null);
		}

		public IndexDiffCacheEntry2(Repository repository,
				IndexDiffChangedListener listener) {
			super(repository, listener);
		}

		@Override
		protected void scheduleReloadJob(String trigger) {
			reloadScheduled = true;
			super.scheduleReloadJob(trigger);
		}

		@Override
		protected void scheduleUpdateJob(Collection<String> filesToUpdate,
				Collection<IResource> resourcesToUpdate) {
			updateScheduled = true;
			super.scheduleUpdateJob(filesToUpdate, resourcesToUpdate);
		}

		@Override
		protected boolean shouldReload(Collection<String> filesToUpdate) {
			return filesToUpdate.size() > MAX_FILES_FOR_UPDATE;
		}

		@Override
		public IndexDiffUpdateJob getUpdateJob() {
			return super.getUpdateJob();
		}
	}

}
