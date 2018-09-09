/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.branch;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.clone.ProjectRecord;
import org.eclipse.egit.ui.internal.clone.ProjectUtils;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.StringUtils;

/**
 * Class to track which projects are imported for each branch.
 * <p>
 * A unique preference is set for each repository/branch combination that is
 * persisted that includes information to be able to restore projects when the
 * branch is later checked out.
 *
 * <p>
 * The workflow is as follows:
 * <p>
 * 1. Call {@link #snapshot()} to get the current projects for the currently
 * checked out branch
 * <p>
 * 2. Call {@link #save(ProjectTrackerMemento)} after the new branch has been
 * successfully checked out with the memento returned from step 1
 * <p>
 * 3. Call {@link #restore(IProgressMonitor)} to restore the projects for the
 * newly checked out branch
 *
 */
class BranchProjectTracker {

	private static final String REPO_ROOT = "/"; //$NON-NLS-1$

	private final Repository repository;

	/**
	 * Create tracker for repository
	 *
	 * @param repository
	 */
	public BranchProjectTracker(final Repository repository) {
		this.repository = repository;
	}

	private String getBranch() {
		try {
			return repository.getBranch();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Snapshot the projects currently associated with the repository
	 * <p>
	 * The memento returned can be later passed to
	 * {@link #save(ProjectTrackerMemento)} to persist it
	 *
	 * @see #save(ProjectTrackerMemento)
	 * @return memento
	 */
	public ProjectTrackerMemento snapshot() {

		ProjectTrackerMemento memento = new ProjectTrackerMemento();

		ProjectTrackerPreferenceSnapshot snapshot = takeSnapshot();
		if (snapshot != null) {
			memento.addSnapshot(snapshot);
		}

		return memento;
	}

	private ProjectTrackerPreferenceSnapshot takeSnapshot() {

		String branch = getBranch();
		if (StringUtils.isEmptyOrNull(branch))
			return null;

		List<String> projectPaths = getAssociatedProjectsPaths();
		if (projectPaths.isEmpty()) {
			return null;
		}

		return new ProjectTrackerPreferenceSnapshot(repository, branch,
				projectPaths);
	}

	@NonNull
	private List<String> getAssociatedProjectsPaths() {

		IProject[] projects = getValidOpenProjects();
		if (projects == null) {
			return Collections.emptyList();
		}

		List<String> projectPaths = new ArrayList<>();

		final String workDir = repository.getWorkTree().getAbsolutePath();
		for (IProject project : projects) {
			IPath path = project.getLocation();
			if (path == null) {
				continue;
			}
			// Only remember mapped projects
			if (!ResourceUtil.isSharedWithGit(project)) {
				continue;
			}
			String fullPath = path.toOSString();
			if (fullPath.startsWith(workDir)) {
				String relative = fullPath.substring(workDir.length());
				if (relative.length() == 0) {
					relative = REPO_ROOT;
				}
				projectPaths.add(relative);
			}
		}
		return projectPaths;
	}

	private IProject[] getValidOpenProjects() {
		try {
			return ProjectUtil.getValidOpenProjects(repository);
		} catch (CoreException e) {
			return null;
		}
	}

	/**
	 * Associate projects with branch. The specified memento must the one
	 * previously returned from a call to {@link #snapshot()}.
	 *
	 * @see #snapshot()
	 * @param snapshot
	 * @return this tracker
	 */
	public BranchProjectTracker save(ProjectTrackerMemento snapshot) {

		snapshot.getSnapshots().stream()
				.forEach(BranchProjectTracker::savePreference);
		return this;
	}

	private static void savePreference(ProjectTrackerPreferenceSnapshot snapshot) {

		Repository repo = snapshot.getRepository();
		String branch = snapshot.getBranch();
		List<String> projects = snapshot.getAssociatedProjects();
		ProjectTrackerPreferenceHelper.saveToPreferences(repo, branch,
				projects);
	}

	/**
	 * Restore projects associated with the currently checked out branch to the
	 * workspace
	 *
	 * @param monitor
	 */
	public void restore(final IProgressMonitor monitor) {
		String branch = getBranch();
		if (branch != null) {
			restore(branch, monitor);
		}
	}

	/**
	 * Restore projects associated with the given branch to the workspace
	 *
	 * @param branch
	 * @param monitor
	 */
	public void restore(final String branch, final IProgressMonitor monitor) {
		List<String> paths = ProjectTrackerPreferenceHelper
				.restoreFromPreferences(repository, branch);
		if (paths.size() == 0)
			return;

		Set<ProjectRecord> records = new LinkedHashSet<>();
		File parent = repository.getWorkTree();
		for (String path : paths) {
			File root;
			if (!REPO_ROOT.equals(path)) {
				root = new File(parent, path);
			} else {
				root = parent;
			}
			if (!root.isDirectory()) {
				continue;
			}
			File projectDescription = new File(root,
					IProjectDescription.DESCRIPTION_FILE_NAME);
			if (!projectDescription.isFile()) {
				continue;
			}
			ProjectRecord record = new ProjectRecord(projectDescription);
			if (record.getProjectDescription() == null) {
				continue;
			}
			records.add(record);
		}
		if (records.isEmpty()) {
			return;
		}
		try {
			ProjectUtils.createProjects(records, true, null, monitor);
		} catch (InvocationTargetException e) {
			Activator
					.logError("Error restoring branch-project associations", e); //$NON-NLS-1$
		} catch (InterruptedException ignored) {
			// Ignored
		}
	}
}
