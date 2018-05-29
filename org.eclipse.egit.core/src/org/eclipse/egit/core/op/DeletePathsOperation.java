/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2015, Stephan Hackstedt <stephan.hackstedt@googlemail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FileUtils;

/**
 * Operation to delete a collection of (untracked) paths, even it they are
 * non-workspace resources.
 */
public class DeletePathsOperation implements IEGitOperation {

	private final Collection<IPath> paths;

	private final ISchedulingRule schedulingRule;

	/**
	 * @param paths
	 *            the files to delete
	 */
	public DeletePathsOperation(final Collection<IPath> paths) {
		this.paths = paths;
		schedulingRule = calculateSchedulingRule();
	}

	@Override
	public void execute(IProgressMonitor m) throws CoreException {
		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor actMonitor) throws CoreException {
				deletePaths(actMonitor);
			}
		};
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE, m);
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return schedulingRule;
	}

	private void deletePaths(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor,
				CoreText.DeleteResourcesOperation_deletingResources,
				paths.size() + 1);
		boolean errorOccurred = false;

		boolean refreshAll = false;
		List<IPath> refreshCachePaths = new ArrayList<IPath>();

		for (IPath path : paths) {
			IResource resource = ResourceUtil.getResourceForLocation(path, false);
			if (resource != null && resource.exists())
				resource.delete(false, progress.newChild(1));
			else {
				File file = path.toFile();
				if (file.exists()) {
					try {
						FileUtils.delete(file, FileUtils.RECURSIVE);
					} catch (IOException e) {
						errorOccurred = true;
						String message = MessageFormat
								.format(CoreText.DeleteResourcesOperation_deleteFailed,
										file.getPath());
						Activator.logError(message, e);
					}
					refreshCachePaths.add(path);
					// Selectively refreshing an IndexDiffCacheEntry only works for files,
					// so refresh all in case of a directory
					if (file.isDirectory())
						refreshAll = true;
				}
				progress.worked(1);
			}
		}

		if (!refreshCachePaths.isEmpty())
			refreshIndexDiffCache(refreshCachePaths, refreshAll);
		progress.worked(1);

		if (errorOccurred) {
			IStatus status = Activator.error(
					CoreText.DeleteResourcesOperation_deleteFailedSeeLog, null);
			throw new CoreException(status);
		}
	}

	private ISchedulingRule calculateSchedulingRule() {
		return RuleUtil.getRuleForContainers(paths);
	}

	private void refreshIndexDiffCache(List<IPath> refreshCachePaths, boolean refreshAll) {
		Map<Repository, Collection<String>> resourcesByRepository = ResourceUtil.splitPathsByRepository(refreshCachePaths);
		for (Map.Entry<Repository, Collection<String>> entry : resourcesByRepository.entrySet()) {
			Repository repository = entry.getKey();
			Collection<String> files = entry.getValue();

			IndexDiffCache cache = Activator.getDefault().getIndexDiffCache();
			IndexDiffCacheEntry cacheEntry = cache.getIndexDiffCacheEntry(repository);
			if (cacheEntry != null)
				if (refreshAll)
					cacheEntry.refresh();
				else
					cacheEntry.refreshFiles(files);
		}
	}
}
