/*******************************************************************************
 * Copyright (C) 2012, 2016 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test;

import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

/**
 * Utility for joining a job. Instead of just calling
 * {@link IJobManager#join(Object, org.eclipse.core.runtime.IProgressMonitor)},
 * it first waits until the job has been scheduled.
 * <p>
 * Usage:
 *
 * <pre>
 * JobJoiner jobJoiner = JobJoiner.startListening(jobFamily, 10, TimeUnit.SECONDS);
 * doThingThatSchedulesJob();
 * jobJoiner.join();
 * </pre>
 */
public class JobJoiner {

	private final Object jobFamily;
	private final long timeoutMillis;

	private Job scheduledJob = null;
	private boolean done = false;

	private final IJobChangeListener listener = new JobChangeAdapter() {
		@Override
		public void scheduled(IJobChangeEvent event) {
			if (event.getJob().belongsTo(jobFamily))
				scheduledJob = event.getJob();
		}

		@Override
		public void done(IJobChangeEvent event) {
			if (event.getJob() != null && event.getJob() == scheduledJob) {
				done = true;
				Job.getJobManager().removeJobChangeListener(this);
			}
		}
	};

	/**
	 * Start listening for the given job family and with the given timeout.
	 *
	 * @param jobFamily
	 * @param timeoutDuration
	 * @param timeoutUnit
	 * @return JobJoiner
	 */
	public static JobJoiner startListening(Object jobFamily, long timeoutDuration,
			TimeUnit timeoutUnit) {
		return new JobJoiner(jobFamily, timeoutUnit.toMillis(timeoutDuration));
	}

	private JobJoiner(Object jobFamily, long timeoutMillis) {
		this.jobFamily = jobFamily;
		this.timeoutMillis = timeoutMillis;
		Job.getJobManager().addJobChangeListener(listener);
	}

	/**
	 * Join the job. If the job is either not yet scheduled within the timeout
	 * or not yet done, an {@link AssertionError} is thrown.
	 *
	 * @return the joined job, if any, or {@code null}
	 */
	public Job join() {
		try {
			doJoin();
			return scheduledJob;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Thread was interrupted.", e);
		} finally {
			Job.getJobManager().removeJobChangeListener(listener);
		}
	}

	private void doJoin() throws AssertionError, InterruptedException {
		long timeSlept = 0;
		while (!done) {
			if (timeSlept > timeoutMillis) {
				if (scheduledJob == null)
					throw new AssertionError(
							"Job was not scheduled until timeout of "
									+ timeoutMillis + " ms.");
				else if (!done)
					throw new AssertionError(
							"Job was scheduled but not done until timeout of "
									+ timeoutMillis + " ms.");
			}
			Thread.sleep(100);
			timeSlept += 100;
		}
	}
}
