/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.operations;

import java.util.Collection;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.op.IgnoreOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.jgit.lib.Repository;

/**
 * UI for ignoring paths (both resources that are part of projects and
 * non-workspace paths).
 */
public class IgnoreOperationUI {

	private final Collection<IPath> paths;

	/**
	 * Create the operation with the resources to ignore.
	 *
	 * @param paths
	 *            to ignore
	 */
	public IgnoreOperationUI(final Collection<IPath> paths) {
		this.paths = paths;
	}

	/**
	 * Run the operation.
	 */
	public void run() {
		final IgnoreOperation operation = new IgnoreOperation(paths);
		String jobname = UIText.IgnoreActionHandler_addToGitignore;
		Job job = new Job(jobname) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					operation.execute(monitor);
				} catch (CoreException e) {
					return Activator.createErrorStatus(e.getStatus()
							.getMessage(), e);
				}
				if (operation.isGitignoreOutsideWSChanged())
					refresh();
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.setRule(operation.getSchedulingRule());
		job.schedule();
	}

	private void refresh() {
		Map<Repository, Collection<String>> pathsByRepository =
				ResourceUtil.splitPathsByRepository(paths);
		for (Repository repository : pathsByRepository.keySet()) {
			IndexDiffCache cache = org.eclipse.egit.core.Activator.getDefault().getIndexDiffCache();
			IndexDiffCacheEntry entry = cache.getIndexDiffCacheEntry(repository);
			if (entry != null)
				entry.refresh();
		}
		GitLightweightDecorator.refresh();
	}
}
