/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Stephan Hackstedt <stephan.hackstedt@googlemail.com> - Bug 477695
 *****************************************************************************/
package org.eclipse.egit.core.op;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
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
			if (preTasks != null) {
				SubMonitor progress = SubMonitor.convert(monitor,
						preTasks.size());
				for (PreExecuteTask task : preTasks)
					task.preExecute(repository, progress.newChild(1));
			}
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
			if (postTasks != null) {
				SubMonitor progress = SubMonitor.convert(monitor,
						postTasks.size());
				for (PostExecuteTask task : postTasks)
					task.postExecute(repository, progress.newChild(1));
			}
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
