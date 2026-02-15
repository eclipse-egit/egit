/*******************************************************************************
 * Copyright (C) 2026, Eclipse EGit contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.pullrequest;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.ui.internal.decorators.IProblemDecoratable;

/**
 * Folder entry in the pull request changes tree.
 * <p>
 * Implements {@link IAdaptable} to support Eclipse platform features like
 * property pages, "Show In" menus, and context menu contributions.
 * </p>
 */
public class PullRequestFolderEntry implements IProblemDecoratable, IAdaptable {

	private final IPath path;

	private String label;

	private PullRequestFolderEntry parent;

	private Object[] children;

	/**
	 * Creates a new folder entry
	 *
	 * @param path
	 *            the repository-relative path
	 * @param label
	 *            the display label
	 */
	public PullRequestFolderEntry(IPath path, String label) {
		this.path = path;
		this.label = label;
	}

	/**
	 * @return the repository-relative path
	 */
	public IPath getPath() {
		return path;
	}

	/**
	 * @return the display label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Sets the display label
	 *
	 * @param label
	 *            the new display label
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * @return the parent folder entry
	 */
	public PullRequestFolderEntry getParent() {
		return parent;
	}

	/**
	 * @param parent
	 *            the parent folder entry
	 */
	public void setParent(PullRequestFolderEntry parent) {
		this.parent = parent;
	}

	/**
	 * @return the child nodes (files or folders)
	 */
	public Object[] getChildren() {
		return children;
	}

	/**
	 * @param children
	 *            the child nodes
	 */
	public void setChildren(Object[] children) {
		this.children = children;
	}

	@Override
	public int getProblemSeverity() {
		// No problem markers for remote folders
		return SEVERITY_NONE;
	}

	/**
	 * Attempts to find a matching workspace container for this folder.
	 * <p>
	 * This method searches all workspace projects for a folder matching this
	 * entry's path. The container is only returned if it exists and is
	 * accessible.
	 * </p>
	 *
	 * @return the workspace {@link IContainer} if found and accessible, or
	 *         {@code null} if the folder is not in the workspace
	 */
	public IContainer getContainer() {
		// Try to find a matching folder in any workspace project
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (IProject project : projects) {
			if (!project.isAccessible()) {
				continue;
			}
			// Try to find the folder relative to the project
			IResource member = project.findMember(path);
			if (member instanceof IContainer && member.isAccessible()) {
				return (IContainer) member;
			}
		}
		return null;
	}

	/**
	 * Gets the absolute file system path for this folder, if it can be determined.
	 *
	 * @return the absolute {@link IPath} to the folder, or {@code null} if it
	 *         cannot be determined
	 */
	public IPath getLocation() {
		IContainer container = getContainer();
		if (container != null) {
			return container.getLocation();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IResource.class || adapter == IContainer.class) {
			return (T) getContainer();
		} else if (adapter == IPath.class) {
			IPath location = getLocation();
			if (location != null) {
				return (T) location;
			}
			// Fall back to repo-relative path
			return (T) path;
		}
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PullRequestFolderEntry) {
			return ((PullRequestFolderEntry) obj).getPath().equals(getPath());
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return getPath().hashCode();
	}

	@Override
	public String toString() {
		return "PullRequestFolderEntry[" + path + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
