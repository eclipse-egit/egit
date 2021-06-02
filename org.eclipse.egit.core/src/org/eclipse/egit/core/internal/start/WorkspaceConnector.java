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
 *    Thomas Wolf -- factored out of Activator
 *******************************************************************************/
package org.eclipse.egit.core.internal.start;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryInitializer;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.ResourceRefreshHandler;
import org.eclipse.egit.core.internal.efs.HiddenResources;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.op.IgnoreOperation;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryFinder;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.util.FS;
import org.eclipse.team.core.RepositoryProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * An OSGi component that sets up everything such that JGit can be used with the Eclipse workspace.
 */
@Component
public class WorkspaceConnector {

	private IWorkspace workspace;

	private IPreferencesService preferencesService;

	private RepositoryCache repositoryCache;

	private ListenerHandle refreshHandle;

	private LinkedList<GitResourcChangeListener> listeners = new LinkedList<>();

	private Job hiddenResourcesInitializer;

	@Reference
	void setWorkspace(IWorkspace workspace) {
		this.workspace = workspace;
	}

	@Reference
	void setPreferencesService(IPreferencesService service) {
		preferencesService = service;
	}

	@Reference
	void setRepositoryCache(RepositoryCache cache) {
		// We need the repository cache. The RepositoryInitializer publishes the
		// RepositoryCache as a service, so with this we can be sure and safe
		// that it _has_ already been set.
		repositoryCache = cache;
	}

	@Activate
	void start() {
		GitProjectData.attachToWorkspace();
		refreshHandle = repositoryCache.getGlobalListenerList()
				.addWorkingTreeModifiedListener(new ResourceRefreshHandler());
		registerAutoIgnoreDerivedResources();
		registerAutoShareProjects();
		registerPreDeleteResourceChangeListener();

		registerBuiltinLFS();
		hiddenResourcesInitializer = Job
				.createSystem("Initializing hidden EGit project", //$NON-NLS-1$
						HiddenResources.INSTANCE::initialize);
		hiddenResourcesInitializer.setUser(false);
		hiddenResourcesInitializer.schedule();
	}

	@Deactivate
	void shutDown() {
		if (listeners != null) {
			LinkedList<GitResourcChangeListener> toClose = listeners;
			listeners = null;
			toClose.forEach(workspace::removeResourceChangeListener);
			toClose.forEach(l -> l.stop());
			toClose.clear();
		}
		if (refreshHandle != null) {
			refreshHandle.remove();
			refreshHandle = null;
		}
		if (hiddenResourcesInitializer != null) {
			hiddenResourcesInitializer.cancel();
			try {
				hiddenResourcesInitializer.join();
			} catch (InterruptedException e) {
				Activator.logError(e.getLocalizedMessage(), e);
			}
			hiddenResourcesInitializer = null;
		}
		GitProjectData.detachFromWorkspace();
	}

	private void registerAutoShareProjects() {
		GitResourcChangeListener autoShareListener = new AutoShareProjects(
				preferencesService);
		listeners.push(autoShareListener);
		workspace.addResourceChangeListener(autoShareListener,
				IResourceChangeEvent.POST_CHANGE);
	}

	private static abstract class GitResourcChangeListener
			implements IResourceChangeListener {

		private final IPreferencesService preferencesService;

		protected GitResourcChangeListener(IPreferencesService service) {
			preferencesService = service;
		}

		protected boolean getBoolean(String pref, boolean defaultValue) {
			return preferencesService.getBoolean(Activator.PLUGIN_ID, pref,
					defaultValue, null);
		}

		public abstract void stop();
	}

	private static class AutoShareProjects extends GitResourcChangeListener {

		private static int INTERESTING_CHANGES = IResourceDelta.ADDED
				| IResourceDelta.OPEN;

		private final CheckProjectsToShare checkProjectsJob;

		public AutoShareProjects(IPreferencesService service) {
			super(service);
			checkProjectsJob = new CheckProjectsToShare();
		}

		@Override
		public void stop() {
			boolean isRunning = !checkProjectsJob.cancel();
			Job.getJobManager().cancel(JobFamilies.AUTO_SHARE);
			try {
				if (isRunning) {
					checkProjectsJob.join();
				}
				Job.getJobManager().join(JobFamilies.AUTO_SHARE,
						new NullProgressMonitor());
			} catch (OperationCanceledException e) {
				// Ignore
			} catch (InterruptedException e) {
				Activator.logError(e.getLocalizedMessage(), e);
			}
		}

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			if (!getBoolean(GitCorePreferences.core_autoShareProjects, true)) {
				return;
			}
			try {
				final Set<IProject> projectCandidates = new LinkedHashSet<>();
				event.getDelta().accept(new IResourceDeltaVisitor() {
					@Override
					public boolean visit(IResourceDelta delta)
							throws CoreException {
						return collectOpenedProjects(delta, projectCandidates);
					}
				});
				if (!projectCandidates.isEmpty()) {
					checkProjectsJob.addProjectsToCheck(projectCandidates);
				}
			} catch (CoreException e) {
				Activator.logError(e.getMessage(), e);
				return;
			}
		}

		/*
		 * This method should not use RepositoryMapping.getMapping(project) or
		 * RepositoryProvider.getProvider(project) which can trigger
		 * RepositoryProvider.map(project) and deadlock current thread. See
		 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=468270
		 */
		private boolean collectOpenedProjects(IResourceDelta delta,
				Set<IProject> projects) {
			if (delta.getKind() == IResourceDelta.CHANGED
					&& (delta.getFlags() & INTERESTING_CHANGES) == 0) {
				return true;
			}
			final IResource resource = delta.getResource();
			if (resource.getType() == IResource.ROOT) {
				return true;
			}
			if (resource.getType() != IResource.PROJECT) {
				return false;
			}
			if (!resource.isAccessible() || resource.getLocation() == null) {
				return false;
			}
			projects.add((IProject) resource);
			return false;
		}

	}

	private static class CheckProjectsToShare extends Job {
		private Object lock = new Object();

		private Set<IProject> projectCandidates;

		public CheckProjectsToShare() {
			super(CoreText.Activator_AutoShareJobName);
			this.projectCandidates = new LinkedHashSet<>();
			setUser(false);
			setSystem(true);
		}

		public void addProjectsToCheck(Set<IProject> projects) {
			synchronized (lock) {
				this.projectCandidates.addAll(projects);
				if (!projectCandidates.isEmpty()) {
					schedule(100);
				}
			}
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Set<IProject> projectsToCheck;
			synchronized (lock) {
				projectsToCheck = projectCandidates;
				projectCandidates = new LinkedHashSet<>();
			}
			if (projectsToCheck.isEmpty()) {
				return Status.OK_STATUS;
			}

			final Map<IProject, File> projects = new HashMap<>();
			for (IProject project : projectsToCheck) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				if (project.isAccessible()) {
					try {
						visitConnect(project, projects);
					} catch (CoreException e) {
						Activator.logError(e.getMessage(), e);
					}
				}
			}
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			if (projects.size() > 0) {
				ConnectProviderOperation op = new ConnectProviderOperation(
						projects);
				op.setRefreshResources(false);
				JobUtil.scheduleUserJob(op, CoreText.Activator_AutoShareJobName,
						JobFamilies.AUTO_SHARE);
			}
			return Status.OK_STATUS;
		}

		private void visitConnect(IProject project,
				final Map<IProject, File> projects) throws CoreException {

			if (RepositoryMapping.getMapping(project) != null) {
				return;
			}
			RepositoryProvider provider = RepositoryProvider
					.getProvider(project);
			// respect if project is already shared with another
			// team provider
			if (provider != null) {
				return;
			}
			RepositoryFinder f = new RepositoryFinder(project);
			f.setFindInChildren(false);
			List<RepositoryMapping> mappings = f
					.find(new NullProgressMonitor());
			if (mappings.isEmpty()) {
				return;
			}
			RepositoryMapping m = mappings.get(0);
			IPath gitDirPath = m.getGitDirAbsolutePath();
			if (gitDirPath == null || !isValidRepositoryPath(gitDirPath)) {
				return;
			}

			// connect
			File repositoryDir = gitDirPath.toFile();
			projects.put(project, repositoryDir);

			Set<String> configured = RepositoryUtil.getInstance()
					.getRepositories();
			if (configured.contains(gitDirPath.toString())) {
				return;
			}
			int nofMappings = mappings.size();
			if (nofMappings > 1) {
				// We don't want to add submodules, that would only lead to
				// problems when a configured repository is deleted. Walk up the
				// hierarchy of nested repositories found. If we hit an already
				// configured repository, we're done anyway. Otherwise add the
				// topmost not yet configured repository that has a valid path.
				IPath lastPath = gitDirPath;
				for (int i = 1; i < nofMappings; i++) {
					IPath nextPath = mappings.get(i).getGitDirAbsolutePath();
					if (nextPath == null) {
						continue;
					}
					if (configured.contains(nextPath.toString())) {
						return;
					} else if (!isValidRepositoryPath(nextPath)) {
						break;
					}
					lastPath = nextPath;
				}
				repositoryDir = lastPath.toFile();
			}
			try {
				RepositoryUtil.getInstance()
						.addConfiguredRepository(repositoryDir);
			} catch (IllegalArgumentException e) {
				Activator.logError(CoreText.Activator_AutoSharingFailed, e);
			}
		}

		private boolean isValidRepositoryPath(@NonNull IPath gitDirPath) {
			if (gitDirPath.segmentCount() == 0) {
				return false;
			}
			IPath workingDir = gitDirPath.removeLastSegments(1);
			// Don't connect "/" or "C:\"
			if (workingDir.isRoot()) {
				return false;
			}
			File userHome = FS.DETECTED.userHome();
			if (userHome != null) {
				Path userHomePath = new Path(userHome.getAbsolutePath());
				// Don't connect "/home" or "/home/username"
				if (workingDir.isPrefixOf(userHomePath)) {
					return false;
				}
			}
			return true;
		}
	}

	private void registerAutoIgnoreDerivedResources() {
		GitResourcChangeListener ignoreDerivedResourcesListener = new IgnoreDerivedResources(
				preferencesService);
		listeners.push(ignoreDerivedResourcesListener);
		workspace.addResourceChangeListener(ignoreDerivedResourcesListener,
				IResourceChangeEvent.POST_CHANGE);
	}

	private static class IgnoreDerivedResources
			extends GitResourcChangeListener {

		public IgnoreDerivedResources(IPreferencesService service) {
			super(service);
		}

		@Override
		public void stop() {
			Job.getJobManager().cancel(JobFamilies.AUTO_IGNORE);
			try {
				Job.getJobManager().join(JobFamilies.AUTO_IGNORE,
						new NullProgressMonitor());
			} catch (OperationCanceledException e) {
				// Ignore
			} catch (InterruptedException e) {
				Activator.logError(e.getLocalizedMessage(), e);
			}
		}

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			try {
				IResourceDelta d = event.getDelta();
				if (d == null || !getBoolean(
						GitCorePreferences.core_autoIgnoreDerivedResources,
						true)) {
					return;
				}
				final Set<IPath> toBeIgnored = new LinkedHashSet<>();

				d.accept(new IResourceDeltaVisitor() {

					@Override
					public boolean visit(IResourceDelta delta)
							throws CoreException {
						if ((delta.getKind() & (IResourceDelta.ADDED
								| IResourceDelta.CHANGED)) == 0)
							return false;
						int flags = delta.getFlags();
						if ((flags != 0) && ((flags
								& IResourceDelta.DERIVED_CHANGED) == 0))
							return false;

						final IResource r = delta.getResource();
						// don't consider resources contained in a project not
						// shared with Git team provider
						if ((r.getProject() != null)
								&& (RepositoryMapping.getMapping(r) == null))
							return false;
						if (r.isTeamPrivateMember())
							return false;

						if (r.isDerived()) {
							try {
								IPath location = r.getLocation();
								if (RepositoryUtil.canBeAutoIgnored(location)) {
									toBeIgnored.add(location);
								}
							} catch (IOException e) {
								Activator.logError(MessageFormat.format(
										CoreText.Activator_ignoreResourceFailed,
										r.getFullPath()), e);
							}
							return false;
						}
						return true;
					}
				});
				if (toBeIgnored.size() > 0)
					JobUtil.scheduleUserJob(new IgnoreOperation(toBeIgnored),
							CoreText.Activator_autoIgnoreDerivedResources,
							JobFamilies.AUTO_IGNORE);
			} catch (CoreException e) {
				Activator.logError(e.getMessage(), e);
				return;
			}
		}
	}

	private void registerPreDeleteResourceChangeListener() {
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=395488
		GitResourcChangeListener deleteProjectListener = new GitReleaseListener();
		listeners.push(deleteProjectListener);
		workspace.addResourceChangeListener(deleteProjectListener,
				IResourceChangeEvent.PRE_DELETE);
	}

	private static class GitReleaseListener extends GitResourcChangeListener {

		public GitReleaseListener() {
			super(null);
		}

		@Override
		public void stop() {
			// Nothing to do
		}

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			IResource resource = event.getResource();
			if (resource instanceof IProject) {
				IProject project = (IProject) resource;
				if (project.isAccessible()) {
					if (ResourceUtil.isSharedWithGit(project)) {
						IResource dotGit = project
								.findMember(Constants.DOT_GIT);
						if (dotGit != null
								&& dotGit.getType() == IResource.FOLDER) {
							RepositoryInitializer.reconfigureWindowCache();
						}
					}
				} else {
					// bug 419706: project is closed - use java.io API
					IPath locationPath = project.getLocation();
					if (locationPath != null) {
						File locationDir = locationPath.toFile();
						File dotGit = new File(locationDir, Constants.DOT_GIT);
						if (dotGit.isDirectory()) {
							RepositoryInitializer.reconfigureWindowCache();
						}
					}
				}
			}
		}
	}

	private void registerBuiltinLFS() {
		if (Platform.getBundle("org.eclipse.jgit.lfs") != null) { //$NON-NLS-1$
			Class<?> lfs;
			try {
				lfs = Class.forName("org.eclipse.jgit.lfs.BuiltinLFS"); //$NON-NLS-1$
				if (lfs != null) {
					lfs.getMethod("register").invoke(null); //$NON-NLS-1$
				}
			} catch (ClassNotFoundException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e1) {
				Activator.logWarning(
						CoreText.Activator_noBuiltinLfsSupportDetected, e1);
			}
		}
	}

}
