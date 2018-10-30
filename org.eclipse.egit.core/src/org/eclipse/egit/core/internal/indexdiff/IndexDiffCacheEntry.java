/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2012, Markus Duft <markus.duft@salomon.at>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.indexdiff;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.IteratorService;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.SafeRunnable;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.trace.GitTraceLocation;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.IndexReadException;
import org.eclipse.jgit.events.IndexChangedEvent;
import org.eclipse.jgit.events.IndexChangedListener;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleWalk;
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

	private final File repositoryGitDir;

	private final String repositoryName;

	private volatile IndexDiffData indexDiffData;

	private Job reloadJob;

	private volatile boolean reloadJobIsInitializing;

	private IndexDiffUpdateJob updateJob;

	private DirCache lastIndex;

	// used to serialize index diff update jobs
	private ReentrantLock lock = new ReentrantLock(true);

	private CopyOnWriteArrayList<IndexDiffChangedListener> listeners = new CopyOnWriteArrayList<>();

	private final IndexChangedListener indexChangedListener = new IndexChangedListener() {
		@Override
		public void onIndexChanged(IndexChangedEvent event) {
			refreshIndexDelta();
		}
	};

	private final RefsChangedListener refsChangedListener = new RefsChangedListener() {
		@Override
		public void onRefsChanged(RefsChangedEvent event) {
			scheduleReloadJob("RefsChanged"); //$NON-NLS-1$
		}
	};

	private final Set<ListenerHandle> listenerHandles = new HashSet<>();

	/**
	 * Keep hard references to submodules -- we need them in the cache at least
	 * as long as the parent repository.
	 */
	private final Map<Repository, String> submodules = new HashMap<>();

	private final IndexDiffChangedListener submoduleListener = (submodule,
			diffData) -> {
		String path = submodules.get(submodule);
		if (path != null) {
			scheduleUpdateJob(Collections.singletonList(path),
					Collections.emptyList());
		}
	};

	private IResourceChangeListener resourceChangeListener;

	private static Semaphore parallelism = new Semaphore(2);

	/**
	 * @param repository
	 * @param listener
	 *            can be null
	 */
	public IndexDiffCacheEntry(Repository repository,
			@Nullable IndexDiffChangedListener listener) {
		this.repositoryGitDir = repository.getDirectory();
		this.repositoryName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		if (listener != null) {
			addIndexDiffChangedListener(listener);
		}

		listenerHandles.add(repository.getListenerList()
				.addIndexChangedListener(indexChangedListener));
		listenerHandles.add(repository.getListenerList()
				.addRefsChangedListener(refsChangedListener));
		// Add a listener also to all submodules in order to be notified when
		// a branch switch or so occurs in a submodule.
		try (SubmoduleWalk walk = SubmoduleWalk.forIndex(repository)) {
			while (walk.next()) {
				Repository submodule = walk.getRepository();
				if (submodule != null && !submodule.isBare()) {
					Repository cached = org.eclipse.egit.core.Activator
							.getDefault().getRepositoryCache().lookupRepository(
									submodule.getDirectory().getAbsoluteFile());
					submodules.put(cached, walk.getPath());
					IndexDiffCacheEntry submoduleCache = org.eclipse.egit.core.Activator
							.getDefault().getIndexDiffCache()
							.getIndexDiffCacheEntry(cached);
					if (submoduleCache != null) {
						submoduleCache
								.addIndexDiffChangedListener(submoduleListener);
					}
					submodule.close();
				}
			}
		} catch (IOException ex) {
			Activator.logError(MessageFormat.format(
					CoreText.IndexDiffCacheEntry_errorCalculatingIndexDelta,
					repository), ex);
		}
		scheduleReloadJob("IndexDiffCacheEntry construction"); //$NON-NLS-1$
		createResourceChangeListener();
		if (!repository.isBare()) {
			try {
				lastIndex = DirCache.read(repository.getIndexFile(),
						repository.getFS());
			} catch (IOException ex) {
				Activator.logError(MessageFormat.format(
						CoreText.IndexDiffCacheEntry_errorCalculatingIndexDelta,
						repository), ex);
			}
		}
	}

	private @Nullable Repository getRepository() {
		if (Activator.getDefault() == null) {
			return null;
		}
		Repository repository = Activator.getDefault().getRepositoryCache()
				.getRepository(repositoryGitDir);
		if (repository == null) {
			return null;
		}
		File directory = repository.getDirectory();
		if (directory == null || !directory.exists()) {
			return null;
		}
		return repository;
	}

	/**
	 * Use this method to register an {@link IndexDiffChangedListener}. The
	 * listener is notified when a new index diff is available.
	 *
	 * @param listener
	 */
	public void addIndexDiffChangedListener(IndexDiffChangedListener listener) {
		listeners.addIfAbsent(listener);
	}

	/**
	 * @param listener
	 */
	public void removeIndexDiffChangedListener(IndexDiffChangedListener listener) {
		listeners.remove(listener);
	}

	/**
	 * This method creates (but does not start) a {@link Job} that refreshes all
	 * open projects related to the repository and afterwards triggers the
	 * (asynchronous) recalculation of the {@link IndexDiff}. This ensures that
	 * the {@link IndexDiff} calculation is not working on out-dated resources.
	 *
	 * @return new job ready to be scheduled, never null
	 */
	public Job createRefreshResourcesAndIndexDiffJob() {
		String jobName = MessageFormat
				.format(CoreText.IndexDiffCacheEntry_refreshingProjects,
						repositoryName);
		Job job = new WorkspaceJob(jobName) {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				Repository repository = getRepository();
				if (repository == null) {
					return Status.CANCEL_STATUS;
				}
				final long start = System.currentTimeMillis();
				ISchedulingRule rule = RuleUtil.getRule(repository);
				try {
					Job.getJobManager().beginRule(rule, monitor);
					try {
						IProject[] validOpenProjects = ProjectUtil
								.getValidOpenProjects(repository);
						repository = null;
						ResourcesPlugin.getWorkspace()
								.run(pm -> ProjectUtil.refreshResources(
										validOpenProjects, pm), null,
										IWorkspace.AVOID_UPDATE, monitor);
					} catch (CoreException e) {
						return Activator.error(e.getMessage(), e);
					}
					if (Activator.getDefault().isDebugging()) {
						final long refresh = System.currentTimeMillis();
						Activator.logInfo("Resources refresh took " //$NON-NLS-1$
								+ (refresh - start) + " ms for " //$NON-NLS-1$
								+ repositoryName);

					}
				} catch (OperationCanceledException e) {
					return Status.CANCEL_STATUS;
				} finally {
					Job.getJobManager().endRule(rule);
					repository = null;
				}
				refresh();
				Job next = reloadJob;
				if (next != null) {
					try {
						next.join();
					} catch (InterruptedException e) {
						return Status.CANCEL_STATUS;
					}
				}
				if (Activator.getDefault().isDebugging()) {
					final long refresh = System.currentTimeMillis();
					Activator.logInfo("Diff took " + (refresh - start) //$NON-NLS-1$
							+ " ms for " + repositoryName); //$NON-NLS-1$

				}
				return Status.OK_STATUS;
			}

		};
		return job;
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
		Repository repository = getRepository();
		if (repository == null || repository.isBare()) {
			return;
		}
		try {
			DirCache currentIndex = DirCache.read(repository.getIndexFile(),
					repository.getFS());
			DirCache oldIndex = lastIndex;

			lastIndex = currentIndex;

			if (oldIndex == null) {
				refresh(); // full refresh in case we have no data to compare.
				return;
			}

			Set<String> paths = new TreeSet<>();
			try (TreeWalk walk = new TreeWalk(repository)) {
				walk.addTree(new DirCacheIterator(oldIndex));
				walk.addTree(new DirCacheIterator(currentIndex));
				walk.setFilter(new InterIndexDiffFilter());

				while (walk.next()) {
					if (walk.isSubtree())
						walk.enterSubtree();
					else
						paths.add(walk.getPathString());
				}
			}

			if (!paths.isEmpty())
				refreshFiles(paths);

		} catch (IOException ex) {
			Activator.logError(MessageFormat.format(
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

	/**
	 * THIS METHOD IS PROTECTED FOR TESTS ONLY!
	 *
	 * @param trigger
	 */
	protected void scheduleReloadJob(final String trigger) {
		if (reloadJob != null) {
			if (reloadJobIsInitializing) {
				return;
			}
			reloadJob.cancel();
		}
		if (updateJob != null) {
			updateJob.cleanupAndCancel();
		}

		if (getRepository() == null) {
			return;
		}
		reloadJob = new Job(getReloadJobName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					reloadJobIsInitializing = true;
					waitForWorkspaceLock(monitor);
				} finally {
					reloadJobIsInitializing = false;
				}
				lock.lock();
				try {
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					parallelism.acquire();
					long startTime = System.currentTimeMillis();
					Repository repository = getRepository();
					if (repository == null) {
						return Status.CANCEL_STATUS;
					}
					IndexDiffData result = calcIndexDiffDataFull(monitor,
							getName(), repository);
					if (monitor.isCanceled() || (result == null)) {
						return Status.CANCEL_STATUS;
					}
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
					notifyListeners(repository);
					return Status.OK_STATUS;
				} catch (IndexReadException e) {
					return Activator.error(CoreText.IndexDiffCacheEntry_cannotReadIndex, e);
				} catch (IOException e) {
					if (GitTraceLocation.INDEXDIFFCACHE.isActive())
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.INDEXDIFFCACHE.getLocation(),
								"Calculating IndexDiff failed", e); //$NON-NLS-1$
					return Status.OK_STATUS;
				} catch (InterruptedException e) {
					return Status.CANCEL_STATUS;
				} finally {
					lock.unlock();
					parallelism.release();
				}
			}

			private String getTraceMessage(long time) {
				return NLS
						.bind("\nUpdated IndexDiffData in {0} ms\nReason: {1}\nRepository: {2}\n", //$NON-NLS-1$
						new Object[] { Long.valueOf(time), trigger,
								repositoryGitDir });
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.INDEX_DIFF_CACHE_UPDATE.equals(family)) {
					return true;
				}
				return super.belongsTo(family);
			}

		};
		reloadJob.setSystem(true);
		reloadJob.schedule();
	}

	/**
	 * Jobs accessing this code should be configured as "system" jobs, to not
	 * interrupt autobuild jobs, see bug 474003
	 *
	 * @param monitor
	 */
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

	/**
	 * THIS METHOD IS PROTECTED FOR TESTS ONLY!
	 *
	 * @param filesToUpdate
	 * @param resourcesToUpdate
	 */
	protected void scheduleUpdateJob(final Collection<String> filesToUpdate,
			final Collection<IResource> resourcesToUpdate) {
		if (getRepository() == null) {
			return;
		}
		if (reloadJob != null && reloadJobIsInitializing)
			return;

		if (shouldReload(filesToUpdate)) {
			// Calculate new IndexDiff if too many resources changed
			// This happens e.g. when a project is opened
			scheduleReloadJob("Too many resources changed: " + filesToUpdate.size()); //$NON-NLS-1$
			return;
		}

		if (updateJob != null) {
			updateJob.addChanges(filesToUpdate, resourcesToUpdate);
			return;
		}
		updateJob = new IndexDiffUpdateJob(getUpdateJobName(), 10) {
			@Override
			protected IStatus updateIndexDiff(Collection<String> files,
					Collection<IResource> resources,
					IProgressMonitor monitor) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}

				// second check here is required because we collect changes
				if (shouldReload(files)) {
					// Calculate new IndexDiff if too many resources changed
					// This happens e.g. when a project is opened
					scheduleReloadJob("Too many resources changed: " + files.size()); //$NON-NLS-1$
					return Status.CANCEL_STATUS;
				}

				waitForWorkspaceLock(monitor);

				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				lock.lock();
				try {
					long startTime = System.currentTimeMillis();
					Repository repository = getRepository();
					if (repository == null) {
						return Status.CANCEL_STATUS;
					}
					IndexDiffData result = calcIndexDiffDataIncremental(monitor,
							getName(), repository, files, resources);
					if (monitor.isCanceled() || (result == null)) {
						return Status.CANCEL_STATUS;
					}
					indexDiffData = result;
					if (GitTraceLocation.INDEXDIFFCACHE.isActive()) {
						long time = System.currentTimeMillis() - startTime;
						StringBuilder message = new StringBuilder(
								NLS.bind(
										"Updated IndexDiffData based on resource list (length = {0}) in {1} ms\n", //$NON-NLS-1$
										Integer.valueOf(resources
												.size()), Long.valueOf(time)));
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.INDEXDIFFCACHE.getLocation(),
								message.append(indexDiffData.toString())
								.toString());
					}
					notifyListeners(repository);
					return Status.OK_STATUS;
				} catch (IOException e) {
					if (GitTraceLocation.INDEXDIFFCACHE.isActive()) {
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.INDEXDIFFCACHE.getLocation(),
								"Calculating IndexDiff failed", e); //$NON-NLS-1$
					}
					return Status.OK_STATUS;
				} finally {
					lock.unlock();
				}
			}
			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.INDEX_DIFF_CACHE_UPDATE.equals(family)) {
					return true;
				}
				return super.belongsTo(family);
			}

		};

		updateJob.addChanges(filesToUpdate, resourcesToUpdate);
	}

	/**
	 * Check if the index update or reload is recommended for given files
	 *
	 * @param filesToUpdate
	 * @return true if the reload operation is preferred
	 */
	protected boolean shouldReload(final Collection<String> filesToUpdate) {
		return filesToUpdate.size() > RESOURCE_LIST_UPDATE_LIMIT;
	}

	private IndexDiffData calcIndexDiffDataIncremental(IProgressMonitor monitor,
			String jobName, Repository repository,
			Collection<String> filesToUpdate,
			Collection<IResource> resourcesToUpdate) throws IOException {
		if (indexDiffData == null)
			// Incremental update not possible without prior indexDiffData
			// -> do full refresh instead
			return calcIndexDiffDataFull(monitor, jobName, repository);

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
		IndexDiffData previous = indexDiffData;
		if (previous == null) {
			// Can happen when the index diff cache entry is already disposed,
			// but the updateJob is still running (and about to cancel).
			return null;
		}
		return new IndexDiffData(previous, filesToUpdate,
				resourcesToUpdate, diffForChangedResources);
	}

	/*
	 * In the case when a file to update was in a folder that was untracked
	 * before, we need to visit more that just the file. E.g. when the file is
	 * now tracked, the folder is no longer untracked but maybe some sub folders
	 * have become newly untracked.
	 */
	private List<String> calcTreeFilterPaths(Collection<String> filesToUpdate) {
		List<String> paths = new ArrayList<>();
		for (String fileToUpdate : filesToUpdate) {
			for (String untrackedFolder : indexDiffData.getUntrackedFolders()) {
				if (fileToUpdate.startsWith(untrackedFolder)
						&& !fileToUpdate.equals(untrackedFolder)) {
					paths.add(untrackedFolder);
				}
			}
			paths.add(fileToUpdate);
		}
		return paths;
	}

	private void notifyListeners(Repository repository) {
		for (IndexDiffChangedListener listener : listeners) {
			SafeRunnable.run(
					() -> listener.indexDiffChanged(repository, indexDiffData));
		}
	}

	private IndexDiffData calcIndexDiffDataFull(IProgressMonitor monitor,
			String jobName, Repository repository)
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
		return MessageFormat.format(CoreText.IndexDiffCacheEntry_reindexing,
				repositoryName);
	}

	private String getUpdateJobName() {
		return MessageFormat.format(
				CoreText.IndexDiffCacheEntry_reindexingIncrementally,
				repositoryName);
	}

	private void createResourceChangeListener() {
		resourceChangeListener = new IResourceChangeListener() {

			private final Map<IProject, IPath> deletedProjects = new HashMap<>();

			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				if (event.getDelta() != null) {
					SkipNotInterestingDeltaVisitor skipNotInterestingVisitor = new SkipNotInterestingDeltaVisitor();
					try {
						event.getDelta().accept(skipNotInterestingVisitor);
						if (!skipNotInterestingVisitor
								.hasAtLeastOneInterestingDelta()) {
							return;
						}
					} catch (CoreException e) {
						Activator.logError(e.getMessage(), e);
					}
				}

				Repository repository = getRepository();
				if (repository == null) {
					ResourcesPlugin.getWorkspace()
							.removeResourceChangeListener(this);
					resourceChangeListener = null;
					return;
				}
				if (event.getType() == IResourceChangeEvent.PRE_DELETE) {
					// Deletion of a project.
					IResource resource = event.getResource();
					if (resource.getType() == IResource.PROJECT) {
						IPath projectPath = resource.getLocation();
						if (projectPath != null) {
							IPath repoPath = ResourceUtil
									.getRepositoryRelativePath(projectPath,
											repository);
							if (repoPath != null) {
								deletedProjects.put((IProject) resource,
										projectPath);
							}
						}
					}
					// Recomputing the index diff on PRE_DELETE might still find
					// the files/resources. We'll handle it in the POST_CHANGE
					// event for the deletion.
					return;
				}
				GitResourceDeltaVisitor visitor = new GitResourceDeltaVisitor(
						repository, deletedProjects);
				try {
					event.getDelta().accept(visitor);
				} catch (CoreException e) {
					Activator.logError(e.getMessage(), e);
					return;
				}
				if (visitor.getGitIgnoreChanged()) {
					scheduleReloadJob("A .gitignore changed"); //$NON-NLS-1$
				} else if (visitor.isProjectDeleted()) {
					scheduleReloadJob("A project was deleted"); //$NON-NLS-1$
				} else if (indexDiffData == null) {
					scheduleReloadJob("Resource changed, no diff available"); //$NON-NLS-1$
				} else {
					Collection<String> filesToUpdate = visitor
							.getFilesToUpdate();
					Collection<IResource> resourcesToUpdate = visitor
							.getResourcesToUpdate();
					if (!filesToUpdate.isEmpty()) {
						scheduleUpdateJob(filesToUpdate, resourcesToUpdate);
					}
				}
			}

		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				resourceChangeListener, IResourceChangeEvent.POST_CHANGE
						| IResourceChangeEvent.PRE_DELETE);
	}

	/**
	 * FOR TESTS ONLY
	 *
	 * @return job used to schedule incremental updates
	 */
	protected IndexDiffUpdateJob getUpdateJob() {
		return updateJob;
	}

	/**
	 * Dispose cache entry by removing listeners. Pending update or reload jobs
	 * are canceled.
	 */
	public void dispose() {
		for (ListenerHandle h : listenerHandles) {
			h.remove();
		}
		listenerHandles.clear();
		submodules.clear();
		if (resourceChangeListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		}
		listeners.clear();
		if (reloadJob != null) {
			reloadJob.cancel();
			reloadJob = null;
		}
		if (updateJob != null) {
			updateJob.cleanupAndCancel();
			updateJob = null;
		}
		indexDiffData = null;
		lastIndex = null;
	}

}
