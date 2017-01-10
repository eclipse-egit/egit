/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.branch;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

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
 * 2. Call {@link #save(IMemento)} after the new branch has been successfully
 * checked out with the memento returned from step 1
 * <p>
 * 3. Call {@link #restore(IProgressMonitor)} to restore the projects for the
 * newly checked out branch
 *
 */
class BranchProjectTracker {

	private static final String PREFIX = "BranchProjectTracker_"; //$NON-NLS-1$

	private static final String KEY_PROJECTS = "projects"; //$NON-NLS-1$

	private static final String KEY_PROJECT = "project"; //$NON-NLS-1$

	private static final String KEY_BRANCH = "branch"; //$NON-NLS-1$

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
	 * Get preference key for branch. This will be unique to the repository and
	 * branch.
	 *
	 * @param branch
	 * @return key
	 */
	public String getPreference(final String branch) {
		if (branch == null)
			throw new IllegalArgumentException("Branch cannot be null"); //$NON-NLS-1$
		if (branch.length() == 0)
			throw new IllegalArgumentException("Branch cannot be empty"); //$NON-NLS-1$

		return PREFIX + '_' + repository.getDirectory().getAbsolutePath() + '_'
				+ branch;
	}

	/**
	 * Snapshot the projects currently associated with the repository
	 * <p>
	 * The memento returned can be later passed to {@link #save(IMemento)} to
	 * persist it
	 *
	 * @see #save(IMemento)
	 * @return memento, will be null on failures
	 */
	public IMemento snapshot() {
		String branch = getBranch();
		if (branch == null)
			return null;

		IProject[] projects;
		try {
			projects = ProjectUtil.getValidOpenProjects(repository);
		} catch (CoreException e) {
			return null;
		}
		XMLMemento memento = XMLMemento.createWriteRoot(KEY_PROJECTS);
		memento.putString(KEY_BRANCH, branch);
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
				IMemento child = memento.createChild(KEY_PROJECT);
				child.putTextData(relative);
			}
		}
		return memento;
	}

	/**
	 * Associate projects with branch. The specified memento must the one
	 * previously returned from a call to {@link #snapshot()}.
	 *
	 * @see #snapshot()
	 * @param memento
	 * @return this tracker
	 */
	public BranchProjectTracker save(final IMemento memento) {
		if (!(memento instanceof XMLMemento))
			throw new IllegalArgumentException("Invalid memento"); //$NON-NLS-1$

		String branch = memento.getString(KEY_BRANCH);
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		String pref = getPreference(branch);
		StringWriter writer = new StringWriter();
		try {
			((XMLMemento) memento).save(writer);
			store.setValue(pref, writer.toString());
		} catch (IOException e) {
			Activator.logError("Error writing branch-project associations", e); //$NON-NLS-1$
		}
		return this;
	}

	/**
	 * Load the project paths associated with the currently checked out branch.
	 * These paths will be relative to the repository root.
	 *
	 * @return non-null but possibly empty array of projects
	 */
	public String[] getProjectPaths() {
		String branch = getBranch();
		if (branch == null)
			return new String[0];
		return getProjectPaths(branch);
	}

	/**
	 * Load the project paths associated with the given branch. These paths will
	 * be relative to the repository root.
	 *
	 * @param branch
	 * @return non-null but possibly empty array of projects
	 */
	public String[] getProjectPaths(final String branch) {
		String pref = getPreference(branch);
		String value = Activator.getDefault().getPreferenceStore()
				.getString(pref);
		if (value.length() == 0)
			return new String[0];
		XMLMemento memento;
		try {
			memento = XMLMemento.createReadRoot(new StringReader(value));
		} catch (WorkbenchException e) {
			Activator.logError("Error reading branch-project associations", e); //$NON-NLS-1$
			return new String[0];
		}
		IMemento[] children = memento.getChildren(KEY_PROJECT);
		if (children.length == 0)
			return new String[0];
		List<String> projects = new ArrayList<>(children.length);
		for (int i = 0; i < children.length; i++) {
			String path = children[i].getTextData();
			if (path != null && path.length() > 0)
				projects.add(path);
		}
		return projects.toArray(new String[projects.size()]);
	}

	/**
	 * Restore projects associated with the currently checked out branch to the
	 * workspace
	 *
	 * @param monitor
	 */
	public void restore(final IProgressMonitor monitor) {
		String branch = getBranch();
		if (branch != null)
			restore(branch, monitor);
	}

	/**
	 * Restore projects associated with the given branch to the workspace
	 *
	 * @param branch
	 * @param monitor
	 */
	public void restore(final String branch, final IProgressMonitor monitor) {
		String[] paths = getProjectPaths(branch);
		if (paths.length == 0)
			return;

		Set<ProjectRecord> records = new LinkedHashSet<>();
		File parent = repository.getWorkTree();
		for (String path : paths) {
			File root;
			if (!REPO_ROOT.equals(path))
				root = new File(parent, path);
			else
				root = parent;

			if (!root.isDirectory())
				continue;
			File projectDescription = new File(root,
					IProjectDescription.DESCRIPTION_FILE_NAME);
			if (!projectDescription.isFile())
				continue;
			records.add(new ProjectRecord(projectDescription));
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
