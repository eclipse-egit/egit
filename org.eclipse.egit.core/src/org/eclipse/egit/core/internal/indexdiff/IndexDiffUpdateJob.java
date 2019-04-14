/*******************************************************************************
 * Copyright (C) 2014, Andrey Loskutov <loskutov@gmx.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.indexdiff;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Job used to collect pending paths before starting actual index update. The
 * same job instance is intended to be used repeatedly, reducing management cost
 * and memory consumption.
 */
abstract class IndexDiffUpdateJob extends Job {

	private final long defaultDelay;

	private Object lock = new Object();

	private Set<String> files;

	private Set<IResource> resources;

	/**
	 * @param name
	 *            non null job name
	 * @param delay
	 *            default delay in milliseconds for job scheduling
	 */
	public IndexDiffUpdateJob(String name, long delay) {
		super(name);
		setSystem(true);
		defaultDelay = delay;
		cleanup();
	}

	/**
	 * Updates index diff on given resources
	 *
	 * @param filesToUpdate
	 *            non null paths set
	 * @param resourcesToUpdate
	 *            non null resources set
	 * @param monitor
	 *            non null progress callback
	 * @return non null status
	 */
	abstract IStatus updateIndexDiff(Collection<String> filesToUpdate,
			Collection<IResource> resourcesToUpdate, IProgressMonitor monitor);

	@Override
	protected final IStatus run(IProgressMonitor monitor) {
		Collection<String> filesToUpdate;
		Collection<IResource> resourcesToUpdate;
		synchronized (lock) {
			filesToUpdate = files;
			resourcesToUpdate = resources;
			cleanup();
			if (monitor.isCanceled() || filesToUpdate.isEmpty()) {
				return Status.CANCEL_STATUS;
			}
		}
		return updateIndexDiff(filesToUpdate, resourcesToUpdate, monitor);
	}

	/**
	 * Adds files to update and schedules the job to run after default delay
	 *
	 * @param filesToUpdate
	 *            non null repo relative paths to update
	 * @param resourcesToUpdate
	 *            non null resources to update
	 */
	protected void addChanges(Collection<String> filesToUpdate,
			Collection<IResource> resourcesToUpdate) {
		synchronized (lock) {
			files.addAll(filesToUpdate);
			resources.addAll(resourcesToUpdate);
		}
		if (!filesToUpdate.isEmpty()) {
			schedule(defaultDelay);
		}
	}

	@Override
	protected void canceling() {
		// always run cleanup on cancel
		cleanup();
	}

	void cleanupAndCancel() {
		// we must always run cleanup here because framework may not call
		// canceling() if the job was not started yet
		cleanup();
		cancel();
	}

	private void cleanup() {
		synchronized (lock) {
			files = new HashSet<>();
			resources = new HashSet<IResource>();
		}
	}

}
