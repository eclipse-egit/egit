/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.operations;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.op.IgnoreOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * UI for ignoring paths (both resources that are part of projects and
 * non-workspace paths).
 */
public class IgnoreOperationUI {

	/**
	 * Adding many files to .gitignore files may slow down all subsequent JGit
	 * operations because all those .gitignore entries will need to be checked
	 * for matches during all subsequent working tree traversals. We therefore
	 * warn the user if there are many files to be ignored.
	 */
	private static final int WARNING_THRESHOLD = 500;

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
		if (paths.size() > WARNING_THRESHOLD) {
			Shell shell = PlatformUI.getWorkbench()
					.getModalDialogShellProvider().getShell();
			if (!MessageDialog.openQuestion(shell,
					MessageFormat.format(
							UIText.IgnoreActionHandler_manyFilesToBeIgnoredTitle,
							Integer.toString(paths.size())),
					UIText.IgnoreActionHandler_manyFilesToBeIgnoredQuestion)) {
				return;
			}
		}
		final IgnoreOperation operation = new IgnoreOperation(paths);
		String jobname = UIText.IgnoreActionHandler_addToGitignore;
		Job job = new WorkspaceJob(jobname) {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor)
					throws CoreException {
				try {
					try {
						operation.execute(monitor);
					} catch (CoreException e) {
						return Activator.createErrorStatus(
								e.getStatus().getMessage(), e);
					}
					if (operation.isGitignoreOutsideWSChanged()) {
						refresh();
					}
					if (monitor.isCanceled()) {
						throw new OperationCanceledException();
					}
					return Status.OK_STATUS;
				} finally {
					monitor.done();
				}
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
