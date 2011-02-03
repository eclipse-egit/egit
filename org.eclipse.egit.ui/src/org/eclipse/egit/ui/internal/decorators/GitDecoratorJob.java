/*******************************************************************************
 * Copyright (C) 2011, Philipp Thun <philipp.thun@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Job decorating Git resources asynchronously
 */
public class GitDecoratorJob extends Job {

	/**
	 * Constant defining the delay between two runs of the GitDecoratorJob in
	 * milliseconds
	 */
	private static final long DELAY = 10L;

	/**
	 * There is one dedicated job for each repository
	 */
	private static HashMap<String, GitDecoratorJob> jobs = new HashMap<String, GitDecoratorJob>();

	/**
	 * Get the job dedicated for a given repository
	 *
	 * @param gitDir
	 *            the .git directory's full path used as unique identifier of a
	 *            repository
	 * @return GitDecoratorJob the job dedicated for the given repository
	 */
	public static synchronized GitDecoratorJob getJobForRepository(
			final String gitDir) {
		GitDecoratorJob job = jobs.get(gitDir);
		if (job == null) {
			job = new GitDecoratorJob("GitDecoratorJob[" + gitDir + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			job.setSystem(true);
			job.setPriority(DECORATE);
			jobs.put(gitDir, job);
		}
		return job;
	}

	private GitDecoratorJob(final String name) {
		super(name);
	}

	private HashSet<Object> elementList = new HashSet<Object>();

	/**
	 * Add an element to the queue of decoration requests
	 *
	 * @param element
	 *            the element to be decorated
	 */
	public synchronized void addDecorationRequest(Object element) {
		if (elementList.add(element)) {
			// Schedule job
			if (getState() == NONE)
				schedule(DELAY);
		}
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		while (!elementList.isEmpty()) {
			final Object[] elements;
			synchronized (this) {
				// Get decoration requests as array and clear the queue
				elements = elementList.toArray(new Object[elementList.size()]);
				elementList.clear();
			}
			// Call GitLightweightDecorator to process the decoration requests
			GitLightweightDecorator.processDecoration(elements);
		}
		return Status.OK_STATUS;
	}
}
