/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.core.internal.op;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.lib.Repository;

/**
 * Base operation that supports adding pre/post tasks
 */
abstract class BaseOperation implements IEGitOperation {

	protected final Repository repository;

	protected Collection<PreExecuteTask> preTasks;

	protected Collection<PostExecuteTask> postTasks;

	BaseOperation(final Repository repository) {
		this.repository = repository;
	}

	/**
	 * Invoke all pre-execute tasks
	 *
	 * @param monitor
	 * @throws CoreException
	 */
	protected void preExecute(IProgressMonitor monitor) throws CoreException {
		synchronized (this) {
			if (preTasks != null)
				for (PreExecuteTask task : preTasks)
					task.preExecute(repository, monitor);
		}
	}

	/**
	 * Invoke all post-execute tasks
	 *
	 * @param monitor
	 * @throws CoreException
	 */
	protected void postExecute(IProgressMonitor monitor) throws CoreException {
		synchronized (this) {
			if (postTasks != null)
				for (PostExecuteTask task : postTasks)
					task.postExecute(repository, monitor);
		}
	}

	/**
	 * @param task
	 *            to be performed before execution
	 */
	public synchronized void addPreExecuteTask(final PreExecuteTask task) {
		if (preTasks == null)
			preTasks = new ArrayList<PreExecuteTask>();
		preTasks.add(task);
	}

	/**
	 * @param task
	 *            to be performed after execution
	 */
	public synchronized void addPostExecuteTask(PostExecuteTask task) {
		if (postTasks == null)
			postTasks = new ArrayList<PostExecuteTask>();
		postTasks.add(task);
	}
}
