/*******************************************************************************
 * Copyright (C) 2026, Vector Informatik GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.internal.staging.StagingView.StagingViewReloadJob;
import org.junit.After;
import org.junit.Test;

/**
 * Tests for {@link StagingViewReloadJob}.
 */
public class StagingViewReloadJobTest {

	private static final long TIMEOUT_SECONDS = 10;

	private final AtomicBoolean disposed = new AtomicBoolean();

	private final StagingViewReloadJob job = new StagingViewReloadJob(
			disposed::get);

	private final List<String> runs = new CopyOnWriteArrayList<>();

	@After
	public void tearDown() throws Exception {
		disposed.set(true);
		job.cancel();
		job.join();
	}

	@Test
	public void testBurstIsCoalescedIntoLastRequest() throws Exception {
		CountDownLatch release = blockWithFirstUpdate();
		for (int i = 0; i < 8; i++) {
			String name = "update" + i;
			job.request(changed -> runs.add(name + ':' + changed), i == 0);
		}
		CountDownLatch done = new CountDownLatch(1);
		job.request(changed -> {
			runs.add("last:" + changed);
			done.countDown();
		}, false);
		release.countDown();
		assertTrue(done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
		job.join();
		// One of the dropped requests switched the repository; that must
		// survive coalescing
		assertEquals(List.of("first:false", "last:true"), runs);
	}

	@Test
	public void testRepositoryChangedIsResetAfterRun() throws Exception {
		CountDownLatch first = new CountDownLatch(1);
		job.request(changed -> {
			runs.add("first:" + changed);
			first.countDown();
		}, true);
		assertTrue(first.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
		job.join();
		CountDownLatch second = new CountDownLatch(1);
		job.request(changed -> {
			runs.add("second:" + changed);
			second.countDown();
		}, false);
		assertTrue(second.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
		assertEquals(List.of("first:true", "second:false"), runs);
	}

	@Test
	public void testSingleRequestRunsImmediately() throws Exception {
		CountDownLatch done = new CountDownLatch(1);
		job.request(changed -> {
			runs.add("update");
			done.countDown();
		}, false);
		assertTrue(done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
		job.join();
		assertEquals(List.of("update"), runs);
	}

	@Test
	public void testNoRunWhenDisposed() throws Exception {
		disposed.set(true);
		job.request(changed -> runs.add("update"), false);
		job.join();
		assertEquals(Job.NONE, job.getState());
		assertTrue(runs.isEmpty());
	}

	// Runs a first update that blocks the UI thread, like a slow rebuild of
	// the view would, until the returned latch is released
	private CountDownLatch blockWithFirstUpdate() throws Exception {
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		job.request(changed -> {
			runs.add("first:" + changed);
			started.countDown();
			try {
				release.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, false);
		assertTrue(started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
		return release;
	}
}
