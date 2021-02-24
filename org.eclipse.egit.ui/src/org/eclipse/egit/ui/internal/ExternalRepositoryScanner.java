/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thomas Wolf - factored out of Activator
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.internal.ResourceRefreshHandler;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jgit.events.IndexChangedListener;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.WorkingTreeModifiedEvent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * A component that scans for external changes made to git repositories.
 * Depending on user preference setting, this scanning is done only when the
 * workbench is active.
 */
@Component(property = EventConstants.EVENT_TOPIC + '='
		+ ApplicationActiveListener.TOPIC_APPLICATION_ACTIVE)
public class ExternalRepositoryScanner implements EventHandler {

	private AtomicBoolean isActive = new AtomicBoolean();

	private ResourceRefreshJob refreshJob;

	private RepositoryChangeScanner scanner;

	@Override
	public void handleEvent(Event event) {
		if (ApplicationActiveListener.TOPIC_APPLICATION_ACTIVE
				.equals(event.getTopic())) {
			Object value = event.getProperty(IEventBroker.DATA);
			if (value instanceof Boolean) {
				boolean newValue = ((Boolean) value).booleanValue();
				if (isActive.compareAndSet(!newValue, newValue) && newValue) {
					scanner.schedule();
				}
			}
		}
	}

	@Activate
	void startUp() {
		refreshJob = new ResourceRefreshJob();
		scanner = new RepositoryChangeScanner(refreshJob, isActive);
		Activator.getDefault().getPreferenceStore()
				.addPropertyChangeListener(scanner);
	}

	@Deactivate
	void shutDown() {
		if (GitTraceLocation.REPOSITORYCHANGESCANNER.isActive()) {
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.REPOSITORYCHANGESCANNER.getLocation(),
					"Trying to cancel " + scanner.getName() + " job"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		Activator.getDefault().getPreferenceStore()
				.removePropertyChangeListener(scanner);
		scanner.setReschedule(false);
		scanner.cancel();
		refreshJob.cancel();

		try {
			scanner.join();
			refreshJob.join();
			if (GitTraceLocation.REPOSITORYCHANGESCANNER.isActive()) {
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.REPOSITORYCHANGESCANNER.getLocation(),
						"Jobs terminated"); //$NON-NLS-1$
			}
		} catch (InterruptedException e) {
			if (GitTraceLocation.REPOSITORYCHANGESCANNER.isActive()) {
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.REPOSITORYCHANGESCANNER.getLocation(),
						"Jobs termination interrupted"); //$NON-NLS-1$
			}
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * A Job that looks at the repository meta data and triggers a refresh of
	 * the resources in the affected projects.
	 */
	private static class RepositoryChangeScanner extends Job
			implements IPropertyChangeListener {

		// volatile in order to ensure thread synchronization
		private volatile boolean doReschedule;

		private volatile int interval;

		private final ResourceRefreshJob refresher;

		private final AtomicBoolean workbenchActive;

		private final RepositoryCache repositoryCache;

		private Collection<WorkingTreeModifiedEvent> events;

		private final IndexChangedListener listener = event -> {
			if (event.isInternal()) {
				return;
			}
			Repository repository = event.getRepository();
			if (repository.isBare()) {
				return;
			}
			List<String> directories = new ArrayList<>();
			for (IProject project : RuleUtil.getProjects(repository)) {
				if (project.isAccessible()) {
					RepositoryMapping mapping = RepositoryMapping
							.getMapping(project);
					if (mapping != null
							&& repository == mapping.getRepository()) {
						String repoRelativePath = mapping
								.getRepoRelativePath(project);
						if (repoRelativePath == null) {
							continue;
						}
						if (GitTraceLocation.REPOSITORYCHANGESCANNER
								.isActive()) {
							GitTraceLocation.getTrace().trace(
									GitTraceLocation.REPOSITORYCHANGESCANNER
											.getLocation(),
									"Scanning project " + project.getName()); //$NON-NLS-1$
						}
						try (TreeWalk w = new TreeWalk(repository)) {
							w.addTree(new FileTreeIterator(repository));
							if (!repoRelativePath.isEmpty()) {
								w.setFilter(PathFilterGroup
										.createFromStrings(repoRelativePath));
							} else {
								directories.add("/"); //$NON-NLS-1$
							}
							w.setRecursive(false);
							while (w.next()) {
								if (w.isSubtree()) {
									FileTreeIterator iter = w.getTree(0,
											FileTreeIterator.class);
									if (iter != null
											&& !iter.isEntryIgnored()) {
										directories
												.add(w.getPathString() + '/');
										w.enterSubtree();
									}
								}
							}
						} catch (IOException e) {
							// Ignore.
						}
						if (GitTraceLocation.REPOSITORYCHANGESCANNER
								.isActive()) {
							GitTraceLocation.getTrace().trace(
									GitTraceLocation.REPOSITORYCHANGESCANNER
											.getLocation(),
									"Scanned project " + project.getName()); //$NON-NLS-1$
						}
					}
				}
			}
			if (directories.isEmpty()) {
				return;
			}
			WorkingTreeModifiedEvent evt = new WorkingTreeModifiedEvent(
					directories, null);
			evt.setRepository(repository);
			events.add(evt);
		};

		public RepositoryChangeScanner(ResourceRefreshJob refresher,
				AtomicBoolean workbenchActive) {
			super(UIText.Activator_repoScanJobName);
			this.refresher = refresher;
			this.workbenchActive = workbenchActive;
			setRule(new RepositoryCacheRule());
			setSystem(true);
			setUser(false);
			repositoryCache = org.eclipse.egit.core.Activator.getDefault()
					.getRepositoryCache();
			updateRefreshInterval();
		}

		@Override
		public boolean shouldSchedule() {
			return doReschedule;
		}

		@Override
		public boolean shouldRun() {
			return doReschedule;
		}

		public void setReschedule(boolean reschedule) {
			doReschedule = reschedule;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			// When people use Git from the command line a lot of changes
			// may happen. Don't scan when inactive depending on the user's
			// choice.
			if (Activator.getDefault().getPreferenceStore()
					.getBoolean(UIPreferences.REFRESH_ONLY_WHEN_ACTIVE)
					&& !workbenchActive.get()) {
				monitor.done();
				return Status.OK_STATUS;
			}

			Repository[] repos = repositoryCache.getAllRepositories();
			if (repos.length == 0) {
				schedule(interval);
				return Status.OK_STATUS;
			}

			monitor.beginTask(UIText.Activator_scanningRepositories,
					repos.length);
			try {
				events = new ArrayList<>();
				for (Repository repo : repos) {
					if (monitor.isCanceled()) {
						break;
					}
					if (GitTraceLocation.REPOSITORYCHANGESCANNER.isActive()) {
						GitTraceLocation.getTrace()
								.trace(GitTraceLocation.REPOSITORYCHANGESCANNER
										.getLocation(),
										"Scanning " + repo + " for changes"); //$NON-NLS-1$ //$NON-NLS-2$
					}

					if (!repo.isBare()) {
						// Set up index change listener for the repo and tear it
						// down afterwards
						ListenerHandle handle = null;
						try {
							handle = repo.getListenerList()
									.addIndexChangedListener(listener);
							repo.scanForRepoChanges();
						} finally {
							if (handle != null) {
								handle.remove();
							}
						}
					}
					monitor.worked(1);
				}
				if (!monitor.isCanceled()) {
					refresher.trigger(events);
				}
				events.clear();
			} catch (IOException e) {
				if (GitTraceLocation.REPOSITORYCHANGESCANNER.isActive()) {
					GitTraceLocation.getTrace().trace(
							GitTraceLocation.REPOSITORYCHANGESCANNER
									.getLocation(),
							"Stopped rescheduling " + getName() + " job"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				return Activator.createErrorStatus(UIText.Activator_scanError,
						e);
			} finally {
				monitor.done();
			}
			if (GitTraceLocation.REPOSITORYCHANGESCANNER.isActive()) {
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.REPOSITORYCHANGESCANNER.getLocation(),
						"Rescheduling " + getName() + " job"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			schedule(interval);
			return Status.OK_STATUS;
		}

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (!UIPreferences.REFRESH_INDEX_INTERVAL
					.equals(event.getProperty())) {
				return;
			}
			updateRefreshInterval();
		}

		private void updateRefreshInterval() {
			interval = getRefreshIndexInterval();
			setReschedule(interval > 0);
			cancel();
			schedule(interval);
		}

		/**
		 * @return interval in milliseconds for automatic index check, 0 is if
		 *         check should be disabled
		 */
		private static int getRefreshIndexInterval() {
			return 1000 * Activator.getDefault().getPreferenceStore()
					.getInt(UIPreferences.REFRESH_INDEX_INTERVAL);
		}
	}

	/**
	 * Refreshes parts of the workspace changed by JGit operations. This will
	 * not refresh any git-ignored resources since those are not reported in the
	 * {@link WorkingTreeModifiedEvent}.
	 */
	private static class ResourceRefreshJob extends Job {

		public ResourceRefreshJob() {
			super(UIText.Activator_refreshJobName);
			setUser(false);
			setSystem(true);
		}

		/**
		 * Internal helper class to record batched accumulated results from
		 * several {@link WorkingTreeModifiedEvent}s.
		 */
		private static class WorkingTreeChanges {

			private final File workTree;

			private final Set<String> modified;

			private final Set<String> deleted;

			public WorkingTreeChanges(WorkingTreeModifiedEvent event) {
				workTree = event.getRepository().getWorkTree()
						.getAbsoluteFile();
				modified = new HashSet<>(event.getModified());
				deleted = new HashSet<>(event.getDeleted());
			}

			public File getWorkTree() {
				return workTree;
			}

			public Set<String> getModified() {
				return modified;
			}

			public Set<String> getDeleted() {
				return deleted;
			}

			public boolean isEmpty() {
				return modified.isEmpty() && deleted.isEmpty();
			}

			public WorkingTreeChanges merge(WorkingTreeModifiedEvent event) {
				modified.removeAll(event.getDeleted());
				deleted.removeAll(event.getModified());
				modified.addAll(event.getModified());
				deleted.addAll(event.getDeleted());
				return this;
			}
		}

		private Map<File, WorkingTreeChanges> repositoriesChanged = new LinkedHashMap<>();

		@Override
		public IStatus run(IProgressMonitor monitor) {
			try {
				List<WorkingTreeChanges> changes;
				synchronized (repositoriesChanged) {
					if (repositoriesChanged.isEmpty()) {
						return Status.OK_STATUS;
					}
					changes = new ArrayList<>(repositoriesChanged.values());
					repositoriesChanged.clear();
				}

				SubMonitor progress = SubMonitor.convert(monitor,
						changes.size());
				try {
					for (WorkingTreeChanges change : changes) {
						if (progress.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						ResourceRefreshHandler handler = new ResourceRefreshHandler();
						handler.refreshRepository(new WorkingTreeModifiedEvent(
								change.getModified(), change.getDeleted()),
								change.getWorkTree(), progress.newChild(1));
					}
				} catch (OperationCanceledException oe) {
					return Status.CANCEL_STATUS;
				} catch (CoreException e) {
					Activator.handleError(UIText.Activator_refreshFailed, e,
							false);
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							e.getMessage());
				}

				if (!monitor.isCanceled()) {
					// re-schedule if we got some changes in the meantime
					synchronized (repositoriesChanged) {
						if (!repositoriesChanged.isEmpty()) {
							schedule(100);
						}
					}
				}
			} finally {
				monitor.done();
			}
			return Status.OK_STATUS;
		}

		/**
		 * Record which projects have changes. Initiate a resource refresh job
		 * if the user settings allow it.
		 *
		 * @param events
		 *            The {@link WorkingTreeModifiedEvent}s that triggered this
		 *            refresh
		 */
		public void trigger(Collection<WorkingTreeModifiedEvent> events) {
			boolean haveChanges = false;
			for (WorkingTreeModifiedEvent event : events) {
				if (event.isEmpty()) {
					continue;
				}
				Repository repo = event.getRepository();
				if (repo == null || repo.isBare()) {
					continue; // Should never occur
				}
				File gitDir = repo.getDirectory();
				synchronized (repositoriesChanged) {
					WorkingTreeChanges changes = repositoriesChanged
							.get(gitDir);
					if (changes == null) {
						repositoriesChanged.put(gitDir,
								new WorkingTreeChanges(event));
					} else {
						changes.merge(event);
						if (changes.isEmpty()) {
							// Actually, this cannot happen.
							repositoriesChanged.remove(gitDir);
						}
					}
				}
				haveChanges = true;
			}
			if (haveChanges) {
				schedule();
			}
		}
	}
}
