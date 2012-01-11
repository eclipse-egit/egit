/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.operations;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.jgit.errors.LockFailedException;

/**
 * Operation that wraps another operation and handles lock failures
 */
public class LockOperation implements IEGitOperation {

	private final IEGitOperation op;

	/**
	 * Create lock operation
	 *
	 * @param op
	 */
	public LockOperation(final IEGitOperation op) {
		this.op = op;
	}

	/**
	 * Unwind a {@link CoreException} to a possibly nested
	 * {@link LockFailedException}
	 *
	 * @param e
	 * @return lock failed exception or null if no cause found
	 */
	public LockFailedException unwind(CoreException e) {
		Throwable cause = e.getCause();
		while (!(cause instanceof LockFailedException)) {
			Throwable parent = cause.getCause();
			if (parent == null || parent == cause)
				return null;
			else
				cause = parent;
		}
		if (cause instanceof LockFailedException)
			return (LockFailedException) cause;
		else
			return null;
	}

	public void execute(IProgressMonitor monitor) throws CoreException {
		try {
			op.execute(monitor);
		} catch (CoreException e) {
			LockFailedException lockException = unwind(e);
			if (lockException != null)
				UIUtils.promptToDeleteLock(lockException);
			else
				throw e;
		}
	}

	public ISchedulingRule getSchedulingRule() {
		return op.getSchedulingRule();
	}
}
