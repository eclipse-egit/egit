/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

/**
 * Utility for testing if a job for a specific family has been scheduled.
 * <p>
 * Must be created before the test code is run, and then tested afterwards using
 * {@link #assertScheduled(String)}.
 * <p>
 * This is more robust than using {@link IJobManager#find(Object)} after running
 * the test code because it registers a listener and is not prone to the job
 * being done already at the time of calling <code>find</code>.
 */
public class JobSchedulingAssert extends JobChangeAdapter {

	private final Object jobFamily;

	private boolean scheduled = false;

	public static JobSchedulingAssert forFamily(Object jobFamily) {
		return new JobSchedulingAssert(jobFamily);
	}

	private JobSchedulingAssert(Object jobFamily) {
		this.jobFamily = jobFamily;
		Job.getJobManager().addJobChangeListener(this);
	}

	@Override
	public void scheduled(IJobChangeEvent event) {
		if (event.getJob().belongsTo(jobFamily))
			scheduled = true;
	}

	/**
	 * Assert that the job has indeed been scheduled.
	 *
	 * @param messageForFailure
	 *            message for when the assertion should fail
	 */
	public void assertScheduled(String messageForFailure) {
		Job.getJobManager().removeJobChangeListener(this);
		assertTrue(messageForFailure, scheduled);
	}
}
