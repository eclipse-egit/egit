/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FileUtils;

/**
 * Operation to delete a collection of (untracked) resources, even it they are
 * non-workspace resources.
 */
public class DeleteResourcesOperation implements IEGitOperation {

	private final Collection<IResource> resources;

	private final ISchedulingRule schedulingRule;

	/**
	 * @param resources
	 *            the files to delete
	 */
	public DeleteResourcesOperation(final Collection<IResource> resources) {
		this.resources = resources;

		IResource[] r = resources.toArray(new IResource[resources.size()]);
		schedulingRule = RuleUtil.getRuleForRepositories(r);
	}

	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor = (m != null) ? m : new NullProgressMonitor();
		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			public void run(IProgressMonitor actMonitor) throws CoreException {
				deleteResources(actMonitor);
			}
		};
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE, monitor);
	}

	public ISchedulingRule getSchedulingRule() {
		return schedulingRule;
	}

	private void deleteResources(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(CoreText.DeleteResourcesOperation_deletingResources,
				resources.size() + 1);
		boolean errorOccurred = false;

		List<IResource> refreshCacheResources = new ArrayList<IResource>();

		for (IResource resource : resources) {
			if (resource.exists())
				resource.delete(false, new SubProgressMonitor(monitor, 1));
			else {
				File file = resource.getFullPath().toFile();
				if (file.exists()) {
					try {
						FileUtils.delete(file);
					} catch (IOException e) {
						errorOccurred = true;
						String message = MessageFormat
								.format(CoreText.DeleteResourcesOperation_deleteFailed,
										file.getPath());
						Activator.logError(message, e);
					}
					refreshCacheResources.add(resource);
				}
				monitor.worked(1);
			}
		}

		if (!refreshCacheResources.isEmpty())
			refreshIndexDiffCache(refreshCacheResources);
		monitor.worked(1);

		monitor.done();

		if (errorOccurred) {
			IStatus status = Activator.error(
					CoreText.DeleteResourcesOperation_deleteFailedSeeLog, null);
			throw new CoreException(status);
		}
	}

	private void refreshIndexDiffCache(List<IResource> refreshCacheResources) {
		IResource[] r = refreshCacheResources.toArray(new IResource[refreshCacheResources.size()]);
		Map<Repository, Collection<String>> resourcesByRepository = ResourceUtil.splitResourcesByRepository(r);
		for (Map.Entry<Repository, Collection<String>> entry : resourcesByRepository.entrySet()) {
			Repository repository = entry.getKey();
			Collection<String> files = entry.getValue();

			IndexDiffCache cache = Activator.getDefault().getIndexDiffCache();
			IndexDiffCacheEntry cacheEntry = cache.getIndexDiffCacheEntry(repository);
			if (cacheEntry != null)
				cacheEntry.refreshFiles(files);
		}
	}
}
