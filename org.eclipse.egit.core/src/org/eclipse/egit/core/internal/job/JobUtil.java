/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.job;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.IEGitOperation;

/**
 * Utility class for scheduling jobs
 */
public class JobUtil {

	/**
	 * Schedule a user job that executes an EGit operation
	 *
	 * @param op
	 *            EGit operation to run
	 * @param jobName
	 * @param jobFamily
	 */
	public static void scheduleUserJob(final IEGitOperation op, String jobName,
			final Object jobFamily) {
		scheduleUserJob(op, jobName, jobFamily, null);
	}

	/**
	 * Schedule a user job that executes an EGit operation
	 *
	 * @param op
	 *            EGit operation to run
	 * @param jobName
	 * @param jobFamily
	 * @param jobChangeListener
	 */
	public static void scheduleUserJob(final IEGitOperation op, String jobName,
			final Object jobFamily, IJobChangeListener jobChangeListener) {
		Job job = new Job(jobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					op.execute(monitor);
				} catch (CoreException e) {
					return e.getStatus();
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (jobFamily != null && family.equals(jobFamily))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setRule(op.getSchedulingRule());
		job.setUser(true);
		if (jobChangeListener != null)
			job.addJobChangeListener(jobChangeListener);
		job.schedule();
	}

	/**
	 * Schedule a user workspace job that executes an EGit operation which
	 * ensures that resource change events are batched until the job is finished
	 *
	 * @param op
	 *            EGit operation to run
	 * @param jobName
	 * @param jobFamily
	 */
	public static void scheduleUserWorkspaceJob(final IEGitOperation op,
			String jobName, final Object jobFamily) {
		scheduleUserWorkspaceJob(op, jobName, jobFamily, null);
	}

	/**
	 * Schedule a user workspace job that executes an EGit operation which
	 * ensures that resource change events are batched until the job is finished
	 *
	 * @param op
	 *            EGit operation to run
	 * @param jobName
	 * @param jobFamily
	 * @param jobChangeListener
	 */
	public static void scheduleUserWorkspaceJob(final IEGitOperation op,
			String jobName, final Object jobFamily,
			IJobChangeListener jobChangeListener) {
		Job job = new WorkspaceJob(jobName) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				try {
					op.execute(monitor);
				} catch (CoreException e) {
					return e.getStatus();
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (jobFamily != null && family.equals(jobFamily))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setRule(op.getSchedulingRule());
		job.setUser(true);
		if (jobChangeListener != null)
			job.addJobChangeListener(jobChangeListener);
		job.schedule();
	}
}
