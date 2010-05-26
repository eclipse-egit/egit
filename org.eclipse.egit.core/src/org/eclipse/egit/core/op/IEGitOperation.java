/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * interface for EGit operations
 *
 */
public interface IEGitOperation {
	/**
	 * Executes the operation
	 *
	 * @param monitor
	 *            monitor for reporting progress. Do not pass this monitor to
	 *            other API methods. Create a SubProgressMonitor from this
	 *            monitor instead.
	 * @throws CoreException
	 * @see SubProgressMonitor
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
}
