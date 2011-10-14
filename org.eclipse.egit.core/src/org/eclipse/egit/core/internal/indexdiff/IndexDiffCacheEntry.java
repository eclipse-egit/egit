/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.indexdiff;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.IteratorService;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.trace.GitTraceLocation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.events.IndexChangedEvent;
import org.eclipse.jgit.events.IndexChangedListener;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.Team;

/**
 * This class caches the {@link IndexDiff} for a given repository. The cache
 * listens for changes in the related repository and notifies listeners about
 * changes.
 *
 */
public class IndexDiffCacheEntry {

	private static final String GITIGNORE_NAME = ".gitignore"; //$NON-NLS-1$

	private static final int RESOURCE_LIST_UPDATE_LIMIT = 1000;

	private Repository repository;

	private volatile IndexDiffData indexDiffData;

	private Job reloadJob;

	// used to serialize index diff update jobs
	private ReentrantLock lock = new ReentrantLock(true);

	private Set<IndexDiffChangedListener> listeners = new HashSet<IndexDiffChangedListener>();

	private IResourceChangeListener resourceChangeListener;

	/**
	 * Bit-mask describing interesting changes for IResourceChangeListener
	 * events
	 */
	private static int INTERESTING_CHANGES = IResourceDelta.CONTENT
			| IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO
			| IResourceDelta.OPEN | IResourceDelta.REPLACED
			| IResourceDelta.TYPE;

	/**
	 * @param repository
	 */
	public IndexDiffCacheEntry(Repository repository) {
		this.repository = repository;
		repository.getListenerList().addIndexChangedListener(
				new IndexChangedListener() {
					public void onIndexChanged(IndexChangedEvent event) {
						scheduleReloadJob();
					}
				});
		repository.getListenerList().addRefsChangedListener(
				new RefsChangedListener() {
					public void onRefsChanged(RefsChangedEvent event) {
						scheduleReloadJob();
					}
				});
		scheduleReloadJob();
		createResourceChangeListener();
	}

	/**
	 * Use this method to register an {@link IndexDiffChangedListener}. The
	 * listener is notified when a new index diff is available.
	 *
	 * @param listener
	 */
	public void addIndexDiffChangedListener(IndexDiffChangedListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	/**
	 * @param listener
	 */
	public void removeIndexDiffChangedListener(IndexDiffChangedListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	/**
	 * Trigger a new index diff calculation manually
	 */
	public void refresh() {
		scheduleReloadJob();
	}

	/**
	 * The method returns the current index diff or null. Null is returned if
	 * the first index diff calculation has not completed yet.
	 *
	 * @return index diff
	 */
	public IndexDiffData getIndexDiff() {
		return indexDiffData;
	}

	private void scheduleReloadJob() {
		if (reloadJob != null)
			reloadJob.cancel();
		if (!checkRepository())
			return;
		reloadJob = new Job(getReloadJobName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				waitForWorkspaceLock();
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				lock.lock();
				try {
					long startTime = System.currentTimeMillis();
					IndexDiff result = calcIndexDiff(monitor, getName());
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;
					indexDiffData = new IndexDiffData(result);
					if (GitTraceLocation.INDEXDIFFCACHE.isActive()) {
						long time = System.currentTimeMillis() - startTime;
						StringBuilder message = new StringBuilder(NLS.bind(
								"Updated IndexDiffData in {0} ms\n", //$NON-NLS-1$
								new Long(time)));
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.INDEXDIFFCACHE.getLocation(),
								message.append(indexDiffData.toString())
										.toString());
					}
					notifyListeners();
					return Status.OK_STATUS;
				} catch (RuntimeException e) {
					if (GitTraceLocation.INDEXDIFFCACHE.isActive()) {
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.INDEXDIFFCACHE.getLocation(),
								"Calculating IndexDiff failed", e); //$NON-NLS-1$
					}
					scheduleReloadJob();
					return Status.OK_STATUS;
				} finally {
					lock.unlock();
				}
			}

			@Override
			public boolean belongsTo(Object family) {
				if (family.equals(JobFamilies.INDEX_DIFF_CACHE_UPDATE))
					return true;
				return super.belongsTo(family);
			}

		};
		reloadJob.schedule();
	}

	private boolean checkRepository() {
		if (Activator.getDefault() == null)
			return false;
		if (!repository.getDirectory().exists())
			return false;
		return true;
	}

	private void waitForWorkspaceLock() {
		// Wait for the workspace lock to avoid starting the calculation
		// of an IndexDiff while the workspace changes (e.g. due to a
		// branch switch).
		// The index diff calculation jobs do not lock the workspace
		// during execution to avoid blocking the workspace.
		try {
			ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {

				public void run(IProgressMonitor monitor) throws CoreException {
					// empty
				}
			}, new NullProgressMonitor());
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	private void scheduleUpdateJob(final Collection<String> filesToUpdate,
			final Collection<IFile> fileResourcesToUpdate) {
		if (!checkRepository())
			return;
		Job job = new Job(getReloadJobName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				waitForWorkspaceLock();
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				lock.lock();
				try {
					long startTime = System.currentTimeMillis();
					IndexDiffData result = calcIndexDiffData(monitor,
							getName(), filesToUpdate, fileResourcesToUpdate);
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;
					indexDiffData = result;
					if (GitTraceLocation.INDEXDIFFCACHE.isActive()) {
						long time = System.currentTimeMillis() - startTime;
						StringBuilder message = new StringBuilder(
								NLS.bind(
										"Updated IndexDiffData based on resource list (length = {0}) in {1} ms\n", //$NON-NLS-1$
										new Integer(fileResourcesToUpdate
												.size()), new Long(time)));
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.INDEXDIFFCACHE.getLocation(),
								message.append(indexDiffData.toString())
								.toString());
					}
					notifyListeners();
					return Status.OK_STATUS;
				} catch (RuntimeException e) {
					if (GitTraceLocation.INDEXDIFFCACHE.isActive()) {
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.INDEXDIFFCACHE.getLocation(),
								"Calculating IndexDiff failed", e); //$NON-NLS-1$
					}
					scheduleReloadJob();
					return Status.OK_STATUS;
				} finally {
					lock.unlock();
				}
			}
			@Override
			public boolean belongsTo(Object family) {
				if (family.equals(JobFamilies.INDEX_DIFF_CACHE_UPDATE))
					return true;
				return super.belongsTo(family);
			}

		};
		job.schedule();
	}

	private IndexDiffData calcIndexDiffData(IProgressMonitor monitor,
			String jobName, Collection<String> filesToUpdate,
			Collection<IFile> fileResourcesToUpdate) {
		EclipseGitProgressTransformer jgitMonitor = new EclipseGitProgressTransformer(
				monitor);
		final IndexDiff diffForChangedResources;
		try {
			WorkingTreeIterator iterator = IteratorService
					.createInitialIterator(repository);
			diffForChangedResources = new IndexDiff(repository, Constants.HEAD,
					iterator);
			diffForChangedResources.setFilter(PathFilterGroup
					.createFromStrings(filesToUpdate));
			diffForChangedResources.diff(jgitMonitor, 0, 0, jobName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new IndexDiffData(indexDiffData, filesToUpdate,
				fileResourcesToUpdate, diffForChangedResources);
	}

	private void notifyListeners() {
		IndexDiffChangedListener[] tmpListeners;
		synchronized (listeners) {
			tmpListeners = listeners
					.toArray(new IndexDiffChangedListener[listeners.size()]);
		}
		for (int i = 0; i < tmpListeners.length; i++)
			tmpListeners[i].indexDiffChanged(repository, indexDiffData);
	}

	private IndexDiff calcIndexDiff(IProgressMonitor monitor, String jobName) {
		EclipseGitProgressTransformer jgitMonitor = new EclipseGitProgressTransformer(
				monitor);

		IndexDiff newIndexDiff;
		try {
			WorkingTreeIterator iterator = IteratorService
					.createInitialIterator(repository);
			newIndexDiff = new IndexDiff(repository, Constants.HEAD, iterator);
			newIndexDiff.diff(jgitMonitor, 0, 0, jobName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return newIndexDiff;
	}

	private String getReloadJobName() {
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		return MessageFormat.format(CoreText.IndexDiffCacheEntry_reindexing, repoName);
	}

	private void createResourceChangeListener() {
		resourceChangeListener = new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {
				final Collection<String> filesToUpdate = new HashSet<String>();
				final Collection<IFile> fileResourcesToUpdate = new HashSet<IFile>();
				final boolean[] gitIgnoreChanged = new boolean[1];
				gitIgnoreChanged[0] = false;

				try {
					event.getDelta().accept(new IResourceDeltaVisitor() {
						public boolean visit(IResourceDelta delta)
								throws CoreException {
							final IResource resource = delta.getResource();
							// Don't include ignored resources
							if (Team.isIgnoredHint(resource))
								return false;

							// If the file has changed but not in a way that we
							// care about (e.g. marker changes to files) then
							// ignore
							if (delta.getKind() == IResourceDelta.CHANGED
									&& (delta.getFlags() & INTERESTING_CHANGES) == 0)
								return true;

							// skip any non-FILE resources
							if (resource.getType() != IResource.FILE)
								return true;

							// If the resource is not part of a project under
							// Git revision control
							final RepositoryMapping mapping = RepositoryMapping
									.getMapping(resource);
							if (mapping == null
									|| mapping.getRepository() != repository)
								// Ignore the change
								return true;

							if (resource.getName().equals(GITIGNORE_NAME)) {
								gitIgnoreChanged[0] = true;
								return false;
							}

							String repoRelativePath = mapping
									.getRepoRelativePath(resource);
							filesToUpdate.add(repoRelativePath);
							fileResourcesToUpdate.add((IFile) resource);

							return true;
						}
					});
				} catch (CoreException e) {
					Activator.logError(e.getMessage(), e);
					return;
				}

				if (gitIgnoreChanged[0] || indexDiffData == null)
					scheduleReloadJob();
				else if (!filesToUpdate.isEmpty())
					if (filesToUpdate.size() < RESOURCE_LIST_UPDATE_LIMIT)
						scheduleUpdateJob(filesToUpdate, fileResourcesToUpdate);
					else
						// Calculate new IndexDiff if too many resources changed
						// This happens e.g. when a project is opened
						scheduleReloadJob();
			}

		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
	}

}
