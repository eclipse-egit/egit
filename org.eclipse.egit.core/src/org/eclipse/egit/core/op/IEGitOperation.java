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
package org.eclipse.egit.core.op;

import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jgit.lib.Repository;

/**
 * interface for EGit operations
 *
 */
public interface IEGitOperation {
	/**
	 * Executes the operation
	 *
	 * @param monitor
	 *            a progress monitor, or <code>null</code> if progress reporting
	 *            and cancellation are not desired
	 * @throws CoreException
	 */
	void execute(IProgressMonitor monitor) throws CoreException;

	/**
	 * @return the rule needed to execute this operation.
	 * <code>null</code> if no rule is required.
	 * A rule is required if the operation changes resources.
	 * It can also be useful to use a rule for reading resources to avoid
	 * changes on the resources by other threads while the operation is running.
	 * @see IResourceRuleFactory
	 */
	ISchedulingRule getSchedulingRule();

	/**
	 * A task to be performed before execution begins
	 */
	interface PreExecuteTask {

		/**
		 * Executes the task
		 *
		 * @param repository
		 *            the git repository
		 *
		 * @param monitor
		 *            a progress monitor, or <code>null</code> if progress
		 *            reporting and cancellation are not desired
		 * @throws CoreException
		 */
		void preExecute(Repository repository, IProgressMonitor monitor)
				throws CoreException;
	}

	/**
	 * A task to be performed after execution completes
	 */
	interface PostExecuteTask {

		/**
		 * Executes the task
		 *
		 * @param repository
		 *            the git repository
		 *
		 * @param monitor
		 *            a progress monitor, or <code>null</code> if progress
		 *            reporting and cancellation are not desired
		 * @throws CoreException
		 */
		void postExecute(Repository repository, IProgressMonitor monitor)
				throws CoreException;
	}
}
