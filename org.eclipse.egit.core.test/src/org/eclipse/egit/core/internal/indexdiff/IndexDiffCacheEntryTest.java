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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.egit.core.test.TestUtils;
import org.eclipse.jgit.lib.Repository;
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
		entry.dispose();
		testRepository.dispose();
		repository = null;
		super.tearDown();
	}

	private static class IndexDiffCacheEntry2 extends IndexDiffCacheEntry {

		boolean reloadScheduled;

		boolean updateScheduled;

		public IndexDiffCacheEntry2(Repository repository) {
			super(repository, null);
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
