/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2012, Markus Duft <markus.duft@salomon.at>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.indexdiff;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.IteratorService;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.trace.GitTraceLocation;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.events.IndexChangedEvent;
import org.eclipse.jgit.events.IndexChangedListener;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.InterIndexDiffFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.osgi.util.NLS;

/**
 * This class caches the {@link IndexDiff} for a given repository. The cache
 * listens for changes in the related repository and notifies listeners about
 * changes.
 *
 */
public class IndexDiffCacheEntry {

	private static final int RESOURCE_LIST_UPDATE_LIMIT = 1000;

	private Repository repository;

	private volatile IndexDiffData indexDiffData;

	private Job reloadJob;

	private volatile boolean reloadJobIsInitializing;

	private List<Job> updateJobs = new Vector<Job>();

	private DirCache lastIndex;

	// used to serialize index diff update jobs
	private ReentrantLock lock = new ReentrantLock(true);

	private Set<IndexDiffChangedListener> listeners = new HashSet<IndexDiffChangedListener>();

	private final ListenerHandle indexChangedListenerHandle;
	private final ListenerHandle refsChangedListenerHandle;
	private IResourceChangeListener resourceChangeListener;

	/**
	 * @param repository
	 */
	public IndexDiffCacheEntry(Repository repository) {
		this.repository = repository;
		indexChangedListenerHandle = repository.getListenerList().addIndexChangedListener(
				new IndexChangedListener() {
					public void onIndexChanged(IndexChangedEvent event) {
						refreshIndexDelta();
					}
				});
		refsChangedListenerHandle = repository.getListenerList().addRefsChangedListener(
				new RefsChangedListener() {
					public void onRefsChanged(RefsChangedEvent event) {
						scheduleReloadJob("RefsChanged"); //$NON-NLS-1$
					}
				});
		scheduleReloadJob("IndexDiffCacheEntry construction"); //$NON-NLS-1$
		createResourceChangeListener();
		if (!repository.isBare()) {
			try {
				lastIndex = repository.readDirCache();
			} catch (IOException ex) {
				Activator
						.error(MessageFormat
								.format(CoreText.IndexDiffCacheEntry_errorCalculatingIndexDelta,
										repository), ex);
			}
		}
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
	 * This method starts a Job that refreshes all open projects related to the
	 * repository and afterwards triggers the (asynchronous) recalculation of
	 * the IndexDiff. This ensures that the IndexDiff calculation is not working
	 * on out-dated resources.
	 *
	 */
	public void refreshResourcesAndIndexDiff() {
		String repositoryName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		String jobName = MessageFormat
				.format(CoreText.IndexDiffCacheEntry_refreshingProjects,
						repositoryName);
		Job job = new Job(jobName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IProject[] validOpenProjects = ProjectUtil
							.getValidOpenProjects(repository);
					ProjectUtil.refreshResources(validOpenProjects, monitor);
				} catch (CoreException e) {
					return Activator.error(e.getMessage(), e);
				}
				refresh();
				return Status.OK_STATUS;
			}

		};
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.schedule();
	}

	/**
	 * Trigger a new index diff calculation manually
	 */
	public void refresh() {
		scheduleReloadJob("Refresh called"); //$NON-NLS-1$
	}

	/**
	 * Trigger a new index diff calculation manually for the passed files.
	 *
	 * @param filesToRefresh (repository-relative paths)
	 */
	public void refreshFiles(final Collection<String> filesToRefresh) {
		List<IResource> resources = Collections.emptyList();
		scheduleUpdateJob(filesToRefresh, resources);
	}

	/**
	 * Refreshes all resources that changed in the index since the last call to
	 * this method. This is suitable for incremental updates on index changed
	 * events
	 *
	 * For bare repositories this does nothing.
	 */
	private void refreshIndexDelta() {
		if (repository.isBare())
			return;

		try {
			DirCache currentIndex = repository.readDirCache();
			DirCache oldIndex = lastIndex;

			lastIndex = currentIndex;

			if (oldIndex == null) {
				refresh(); // full refresh in case we have no data to compare.
				return;
			}

			Set<String> paths = new TreeSet<String>();
			TreeWalk walk = new TreeWalk(repository);

			try {
				walk.addTree(new DirCacheIterator(oldIndex));
				walk.addTree(new DirCacheIterator(currentIndex));
				walk.setFilter(new InterIndexDiffFilter());

				while (walk.next()) {
					if (walk.isSubtree())
						walk.enterSubtree();
					else
						paths.add(walk.getPathString());
				}
			} finally {
				walk.release();
			}

			if (!paths.isEmpty())
				refreshFiles(paths);

		} catch (IOException ex) {
			Activator.error(MessageFormat.format(
					CoreText.IndexDiffCacheEntry_errorCalculatingIndexDelta,
					repository), ex);
			scheduleReloadJob("Exception while calculating index delta, doing full reload instead"); //$NON-NLS-1$
		}
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

	private void scheduleReloadJob(final String trigger) {
		if (reloadJob != null) {
			if (reloadJobIsInitializing)
				return;
			reloadJob.cancel();
		}
		for (Job updateJob : updateJobs)
			updateJob.cancel();

		if (!checkRepository())
			return;
		reloadJob = new Job(getReloadJobName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					reloadJobIsInitializing = true;
					waitForWorkspaceLock(monitor);
					lock.lock();
				} finally {
					reloadJobIsInitializing = false;
				}
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				try {
					long startTime = System.currentTimeMillis();
					IndexDiffData result = calcIndexDiffDataFull(monitor, getName());
					if (monitor.isCanceled() || (result == null))
						return Status.CANCEL_STATUS;
					indexDiffData = result;
					if (GitTraceLocation.INDEXDIFFCACHE.isActive()) {
						long time = System.currentTimeMillis() - startTime;
						StringBuilder message = new StringBuilder(
								getTraceMessage(time));
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.INDEXDIFFCACHE.getLocation(),
								message.append(indexDiffData.toString())
										.toString());
					}
					notifyListeners();
					return Status.OK_STATUS;
				} catch (IOException e) {
					if (GitTraceLocation.INDEXDIFFCACHE.isActive())
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.INDEXDIFFCACHE.getLocation(),
								"Calculating IndexDiff failed", e); //$NON-NLS-1$
					return Status.OK_STATUS;
				} finally {
					lock.unlock();
				}
			}

			private String getTraceMessage(long time) {
				return NLS
						.bind("\nUpdated IndexDiffData in {0} ms\nReason: {1}\nRepository: {2}\n", //$NON-NLS-1$
						new Object[] { Long.valueOf(time), trigger,
								repository.getWorkTree().getName() });
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

	private void waitForWorkspaceLock(IProgressMonitor monitor) {
		// Wait for the workspace lock to avoid starting the calculation
		// of an IndexDiff while the workspace changes (e.g. due to a
		// branch switch).
		// The index diff calculation jobs do not lock the workspace
		// during execution to avoid blocking the workspace.
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		try {
			Job.getJobManager().beginRule(root, monitor);
		} catch (OperationCanceledException e) {
			return;
		} finally {
			Job.getJobManager().endRule(root);
		}
	}

	private void scheduleUpdateJob(final Collection<String> filesToUpdate,
			final Collection<IResource> resourcesToUpdate) {
		if (!checkRepository())
			return;
		if (reloadJob != null && reloadJobIsInitializing)
			return;
		Job job = new Job(getUpdateJobName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				waitForWorkspaceLock(monitor);
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				lock.lock();
				try {
					long startTime = System.currentTimeMillis();
					IndexDiffData result = calcIndexDiffDataIncremental(monitor,
							getName(), filesToUpdate, resourcesToUpdate);
					if (monitor.isCanceled() || (result == null))
						return Status.CANCEL_STATUS;
					indexDiffData = result;
					if (GitTraceLocation.INDEXDIFFCACHE.isActive()) {
						long time = System.currentTimeMillis() - startTime;
						StringBuilder message = new StringBuilder(
								NLS.bind(
										"Updated IndexDiffData based on resource list (length = {0}) in {1} ms\n", //$NON-NLS-1$
										Integer.valueOf(resourcesToUpdate
												.size()), Long.valueOf(time)));
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.INDEXDIFFCACHE.getLocation(),
								message.append(indexDiffData.toString())
								.toString());
					}
					notifyListeners();
					return Status.OK_STATUS;
				} catch (IOException e) {
					if (GitTraceLocation.INDEXDIFFCACHE.isActive())
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.INDEXDIFFCACHE.getLocation(),
								"Calculating IndexDiff failed", e); //$NON-NLS-1$
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
		updateJobs.add(job);
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				updateJobs.remove(event.getJob());
			}
		});
		job.schedule();
	}

	private IndexDiffData calcIndexDiffDataIncremental(IProgressMonitor monitor,
			String jobName, Collection<String> filesToUpdate,
			Collection<IResource> resourcesToUpdate) throws IOException {
		if (indexDiffData == null)
			// Incremental update not possible without prior indexDiffData
			// -> do full refresh instead
			return calcIndexDiffDataFull(monitor, jobName);

		EclipseGitProgressTransformer jgitMonitor = new EclipseGitProgressTransformer(
				monitor);

		List<String> treeFilterPaths = calcTreeFilterPaths(filesToUpdate);

		WorkingTreeIterator iterator = IteratorService.createInitialIterator(repository);
		if (iterator == null)
			return null; // workspace is closed
		IndexDiff diffForChangedResources = new IndexDiff(repository,
				Constants.HEAD, iterator);
		diffForChangedResources.setFilter(PathFilterGroup
				.createFromStrings(treeFilterPaths));
		diffForChangedResources.diff(jgitMonitor, 0, 0, jobName);
		return new IndexDiffData(indexDiffData, filesToUpdate,
				resourcesToUpdate, diffForChangedResources);
	}

	/*
	 * In the case when a file to update was in a folder that was untracked
	 * before, we need to visit more that just the file. E.g. when the file is
	 * now tracked, the folder is no longer untracked but maybe some sub folders
	 * have become newly untracked.
	 */
	private List<String> calcTreeFilterPaths(Collection<String> filesToUpdate) {
		List<String> paths = new ArrayList<String>();
		for (String fileToUpdate : filesToUpdate) {
			for (String untrackedFolder : indexDiffData.getUntrackedFolders())
				if (fileToUpdate.startsWith(untrackedFolder))
					paths.add(untrackedFolder);
			paths.add(fileToUpdate);
		}
		return paths;
	}

	private void notifyListeners() {
		IndexDiffChangedListener[] tmpListeners;
		synchronized (listeners) {
			tmpListeners = listeners
					.toArray(new IndexDiffChangedListener[listeners.size()]);
		}
		for (int i = 0; i < tmpListeners.length; i++)
			try {
				tmpListeners[i].indexDiffChanged(repository, indexDiffData);
			} catch (RuntimeException e) {
				Activator.logError(
						"Exception occured in an IndexDiffChangedListener", e); //$NON-NLS-1$
			}
	}

	private IndexDiffData calcIndexDiffDataFull(IProgressMonitor monitor, String jobName)
			throws IOException {
		EclipseGitProgressTransformer jgitMonitor = new EclipseGitProgressTransformer(
				monitor);

		IndexDiff newIndexDiff;
		WorkingTreeIterator iterator = IteratorService
				.createInitialIterator(repository);
		if (iterator == null)
			return null; // workspace is closed
		newIndexDiff = new IndexDiff(repository, Constants.HEAD, iterator);
		newIndexDiff.diff(jgitMonitor, 0, 0, jobName);
		return new IndexDiffData(newIndexDiff);
	}

	private String getReloadJobName() {
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		return MessageFormat.format(CoreText.IndexDiffCacheEntry_reindexing, repoName);
	}

	private String getUpdateJobName() {
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		return MessageFormat.format(
				CoreText.IndexDiffCacheEntry_reindexingIncrementally, repoName);
	}

	private void createResourceChangeListener() {
		resourceChangeListener = new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {
				GitResourceDeltaVisitor visitor = new GitResourceDeltaVisitor(repository);
				try {
					event.getDelta().accept(visitor);
				} catch (CoreException e) {
					Activator.logError(e.getMessage(), e);
					return;
				}
				Collection<String> filesToUpdate = visitor.getFilesToUpdate();
				if (visitor.getGitIgnoreChanged())
					scheduleReloadJob("A .gitignore changed"); //$NON-NLS-1$
				else if (indexDiffData == null)
					scheduleReloadJob("Resource changed, no diff available"); //$NON-NLS-1$
				else if (!filesToUpdate.isEmpty())
					if (filesToUpdate.size() < RESOURCE_LIST_UPDATE_LIMIT)
						scheduleUpdateJob(filesToUpdate, visitor.getResourcesToUpdate());
					else
						// Calculate new IndexDiff if too many resources changed
						// This happens e.g. when a project is opened
						scheduleReloadJob("Too many resources changed"); //$NON-NLS-1$
			}

		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
	}

	/**
	 * Dispose cache entry by removing listeners.
	 */
	public void dispose() {
		indexChangedListenerHandle.remove();
		refsChangedListenerHandle.remove();
		if (resourceChangeListener != null)
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
	}

}
