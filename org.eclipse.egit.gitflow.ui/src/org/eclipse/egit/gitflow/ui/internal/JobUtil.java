/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.IEGitOperation;

/**
 * Copied from: org.eclipse.egit.core.internal.job.JobUtil because of class loading issues.
 */
public class JobUtil {
	/**
	 * GitFlow Family
	 */
	public final static Object GITFLOW_FAMILY = new Object();

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
				if (jobFamily != null && family.equals(jobFamily)) {
					return true;
				}
				return super.belongsTo(family);
			}
		};
		job.setRule(op.getSchedulingRule());
		job.setUser(true);
		job.schedule();
	}

}
