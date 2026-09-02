/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel <lars.vogel@vogella.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.workingsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.project.RepositoryMappingChangeListener;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

/**
 * Maintains one working set per Git repository that has projects in the
 * workspace, so that the Project Explorer can group projects by repository.
 * Enabled through {@link UIPreferences#REPOSITORY_WORKING_SETS}.
 */
public final class GitRepositoryWorkingSets {

	/** Id of the working set type, as declared in plugin.xml. */
	public static final String WORKING_SET_ID = "org.eclipse.egit.ui.gitRepositoryWorkingSet"; //$NON-NLS-1$

	private static final String NAME_PREFIX = "git:"; //$NON-NLS-1$

	private static final IAdaptable[] NO_ELEMENTS = new IAdaptable[0];

	private static final GitRepositoryWorkingSets INSTANCE = new GitRepositoryWorkingSets();

	private final AtomicBoolean installed = new AtomicBoolean();

	private final AtomicBoolean active = new AtomicBoolean();

	private final Map<IProject, Repository> mappedProjects = new HashMap<>();

	private final Job updateJob = new Job(
			UIText.GitRepositoryWorkingSets_jobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (!active.get()) {
				return Status.CANCEL_STATUS;
			}
			Map<Repository, Set<IProject>> byRepository = collect();
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			UIJob uiJob = new UIJob(UIText.GitRepositoryWorkingSets_jobName) {

				@Override
				public IStatus runInUIThread(IProgressMonitor uiMonitor) {
					if (active.get()) {
						update(byRepository);
					}
					return Status.OK_STATUS;
				}

				@Override
				public boolean belongsTo(Object family) {
					return JobFamilies.REPOSITORY_WORKING_SETS == family;
				}
			};
			uiJob.setSystem(true);
			uiJob.schedule();
			return Status.OK_STATUS;
		}

		@Override
		public boolean belongsTo(Object family) {
			return JobFamilies.REPOSITORY_WORKING_SETS == family;
		}
	};

	private final IResourceChangeListener resourceListener = this::resourceChanged;

	private final RepositoryMappingChangeListener mappingListener = mapping -> triggerUpdate();

	private final IPropertyChangeListener preferenceListener = event -> {
		if (UIPreferences.REPOSITORY_WORKING_SETS
				.equals(event.getProperty())) {
			if (isEnabled()) {
				start();
			} else {
				stop(true);
			}
		}
	};

	private GitRepositoryWorkingSets() {
		updateJob.setSystem(true);
	}

	/**
	 * @return the singleton
	 */
	public static GitRepositoryWorkingSets getInstance() {
		return INSTANCE;
	}

	/**
	 * @return whether repository working sets are enabled in the preferences
	 */
	public static boolean isEnabled() {
		return Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.REPOSITORY_WORKING_SETS);
	}

	/**
	 * Returns the working set of a repository, if it exists.
	 *
	 * @param repository
	 *            to look up
	 * @return the working set, or {@code null}
	 */
	public static IWorkingSet getWorkingSet(Repository repository) {
		IWorkingSet workingSet = PlatformUI.getWorkbench()
				.getWorkingSetManager().getWorkingSet(nameOf(repository));
		if (workingSet != null
				&& WORKING_SET_ID.equals(workingSet.getId())) {
			return workingSet;
		}
		return null;
	}

	/**
	 * Starts tracking the preference and, if enabled, the workspace. Safe to
	 * call more than once.
	 */
	public void install() {
		if (!installed.compareAndSet(false, true)) {
			return;
		}
		Activator.getDefault().getPreferenceStore()
				.addPropertyChangeListener(preferenceListener);
		if (isEnabled()) {
			start();
		}
	}

	/**
	 * Stops tracking the preference and the workspace, leaving existing
	 * working sets untouched.
	 */
	public void uninstall() {
		if (!installed.compareAndSet(true, false)) {
			return;
		}
		Activator activator = Activator.getDefault();
		if (activator != null) {
			IPreferenceStore store = activator.getPreferenceStore();
			store.removePropertyChangeListener(preferenceListener);
		}
		stop(false);
	}

	private void start() {
		if (!active.compareAndSet(false, true)) {
			return;
		}
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				resourceListener, IResourceChangeEvent.POST_CHANGE);
		GitProjectData.addRepositoryChangeListener(mappingListener);
		triggerUpdate();
	}

	private void stop(boolean removeWorkingSets) {
		if (!active.compareAndSet(true, false)) {
			return;
		}
		GitProjectData.removeRepositoryChangeListener(mappingListener);
		ResourcesPlugin.getWorkspace()
				.removeResourceChangeListener(resourceListener);
		updateJob.cancel();
		synchronized (mappedProjects) {
			mappedProjects.clear();
		}
		if (removeWorkingSets) {
			UIJob job = new UIJob(UIText.GitRepositoryWorkingSets_jobName) {

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					removeAll();
					return Status.OK_STATUS;
				}

				@Override
				public boolean belongsTo(Object family) {
					return JobFamilies.REPOSITORY_WORKING_SETS == family;
				}
			};
			job.setSystem(true);
			job.schedule();
		}
	}

	/**
	 * Recomputes all repository working sets in the background.
	 */
	public void triggerUpdate() {
		if (active.get()) {
			updateJob.cancel();
			updateJob.schedule(500L);
		}
	}

	private void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if (delta == null) {
			return;
		}
		for (IResourceDelta child : delta.getAffectedChildren()) {
			if (child.getKind() != IResourceDelta.CHANGED
					|| mappingChanged((IProject) child.getResource())) {
				triggerUpdate();
				return;
			}
		}
	}

	private boolean mappingChanged(IProject project) {
		Repository repository = repositoryOf(project);
		synchronized (mappedProjects) {
			return repository != mappedProjects.get(project);
		}
	}

	private static Repository repositoryOf(IProject project) {
		if (!project.isAccessible()) {
			return null;
		}
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		return mapping == null ? null : mapping.getRepository();
	}

	private Map<Repository, Set<IProject>> collect() {
		Map<Repository, Set<IProject>> byRepository = new HashMap<>();
		Map<IProject, Repository> current = new HashMap<>();
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot()
				.getProjects()) {
			Repository repository = repositoryOf(project);
			if (repository != null && !repository.isBare()) {
				byRepository.computeIfAbsent(repository, r -> new HashSet<>())
						.add(project);
				current.put(project, repository);
			}
		}
		synchronized (mappedProjects) {
			mappedProjects.clear();
			mappedProjects.putAll(current);
		}
		return byRepository;
	}

	private void update(Map<Repository, Set<IProject>> byRepository) {
		IWorkingSetManager manager = PlatformUI.getWorkbench()
				.getWorkingSetManager();
		Map<Repository, String> labels = labelsOf(byRepository.keySet());
		Set<String> wanted = new HashSet<>();
		for (Map.Entry<Repository, Set<IProject>> entry : byRepository
				.entrySet()) {
			String name = nameOf(entry.getKey());
			wanted.add(name);
			IAdaptable[] elements = sorted(entry.getValue());
			String label = labels.get(entry.getKey());
			IWorkingSet workingSet = manager.getWorkingSet(name);
			if (workingSet == null) {
				workingSet = manager.createWorkingSet(name, elements);
				workingSet.setId(WORKING_SET_ID);
				workingSet.setLabel(label);
				manager.addWorkingSet(workingSet);
			} else if (WORKING_SET_ID.equals(workingSet.getId())) {
				if (!label.equals(workingSet.getLabel())) {
					workingSet.setLabel(label);
				}
				if (!sameElements(workingSet.getElements(), elements)) {
					workingSet.setElements(elements);
				}
			}
		}
		for (IWorkingSet workingSet : manager.getAllWorkingSets()) {
			if (WORKING_SET_ID.equals(workingSet.getId())
					&& !wanted.contains(workingSet.getName())) {
				manager.removeWorkingSet(workingSet);
			}
		}
	}

	private static void removeAll() {
		IWorkingSetManager manager = PlatformUI.getWorkbench()
				.getWorkingSetManager();
		for (IWorkingSet workingSet : manager.getAllWorkingSets()) {
			if (WORKING_SET_ID.equals(workingSet.getId())) {
				manager.removeWorkingSet(workingSet);
			}
		}
	}

	private static String nameOf(Repository repository) {
		return NAME_PREFIX + repository.getWorkTree().getAbsolutePath();
	}

	// Repositories sharing a directory name get the path appended.
	private static Map<Repository, String> labelsOf(
			Collection<Repository> repositories) {
		Map<String, List<Repository>> byName = new HashMap<>();
		for (Repository repository : repositories) {
			byName.computeIfAbsent(
					RepositoryUtil.INSTANCE.getRepositoryName(repository),
					n -> new ArrayList<>()).add(repository);
		}
		Map<Repository, String> labels = new HashMap<>();
		for (Map.Entry<String, List<Repository>> entry : byName.entrySet()) {
			for (Repository repository : entry.getValue()) {
				String label = entry.getKey();
				if (entry.getValue().size() > 1) {
					label += " (" + repository.getWorkTree().getAbsolutePath() //$NON-NLS-1$
							+ ')';
				}
				labels.put(repository, label);
			}
		}
		return labels;
	}

	private static IAdaptable[] sorted(Set<IProject> projects) {
		if (projects.isEmpty()) {
			return NO_ELEMENTS;
		}
		IProject[] result = projects.toArray(new IProject[0]);
		Arrays.sort(result, Comparator.comparing(IProject::getName));
		return result;
	}

	private static boolean sameElements(IAdaptable[] a, IAdaptable[] b) {
		return new HashSet<>(Arrays.asList(a))
				.equals(new HashSet<>(Arrays.asList(b)));
	}
}
