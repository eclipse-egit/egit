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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.bitbucket.ChangedFile;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.internal.decorators.IProblemDecoratable;
import org.eclipse.jgit.lib.Repository;

/**
 * UI model for a changed file in a pull request
 */
public class PullRequestChangedFile implements IProblemDecoratable {

	/**
	 * Change type enum
	 */
	public enum ChangeType {
		/** File was added */
		ADDED,
		/** File was modified */
		MODIFIED,
		/** File was deleted */
		DELETED,
		/** File was renamed or moved */
		RENAMED
	}

	private final String path;

	private final String name;

	private final IPath parentPath;

	private final ChangeType changeType;

	private final String srcPath;

	private PullRequestFolderEntry parent;

	private Repository repository;

	/**
	 * Creates a new changed file entry
	 *
	 * @param path
	 *            the full repository-relative path
	 * @param name
	 *            the file name
	 * @param changeType
	 *            the type of change
	 * @param srcPath
	 *            the source path for renames, null otherwise
	 */
	public PullRequestChangedFile(String path, String name,
			ChangeType changeType, String srcPath) {
		this.path = path;
		this.name = name;
		this.changeType = changeType;
		this.srcPath = srcPath;

		IPath fullPath = new Path(path);
		this.parentPath = fullPath.segmentCount() > 1
				? fullPath.removeLastSegments(1)
				: Path.EMPTY;
	}

	/**
	 * @return the full repository-relative path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return the file name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the parent directory path
	 */
	public IPath getParentPath() {
		return parentPath;
	}

	/**
	 * @return the change type
	 */
	public ChangeType getChangeType() {
		return changeType;
	}

	/**
	 * @return the source path for renames, null otherwise
	 */
	public String getSrcPath() {
		return srcPath;
	}

	/**
	 * @return the old path for renames/moves, same as {@link #getSrcPath()}
	 */
	public String getOldPath() {
		return srcPath;
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
	 * Sets the Git repository for this changed file.
	 * <p>
	 * This allows for more accurate workspace file resolution by using the
	 * repository's working tree as a base path.
	 * </p>
	 *
	 * @param repository
	 *            the Git repository, or null
	 */
	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	/**
	 * @return the Git repository, or null if not set
	 */
	public Repository getRepository() {
		return repository;
	}

	@Override
	public int getProblemSeverity() {
		// No problem markers for remote files
		return SEVERITY_NONE;
	}

	/**
	 * Attempts to find a matching workspace file for this changed file.
	 * <p>
	 * This method searches all workspace projects for a file matching this
	 * changed file's path. The file is only returned if it exists and is
	 * accessible.
	 * </p>
	 *
	 * @return the workspace {@link IFile} if found and accessible, or
	 *         {@code null} if the file is not in the workspace (e.g., the PR
	 *         branch is not checked out locally)
	 */
	public IFile getWorkspaceFile() {
		// Use Repository-based resolution if available (preferred)
		if (repository != null) {
			return ResourceUtil.getFileForLocation(repository, path, false);
		}

		// Fallback: search all workspace projects
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (IProject project : projects) {
			if (!project.isAccessible()) {
				continue;
			}
			// Try to find the file relative to the project
			IResource member = project.findMember(path);
			if (member instanceof IFile && member.isAccessible()) {
				return (IFile) member;
			}
		}
		return null;
	}

	/**
	 * Gets the absolute file system path for this file, if it can be determined.
	 *
	 * @return the absolute {@link IPath} to the file, or {@code null} if it
	 *         cannot be determined
	 */
	public IPath getLocation() {
		IFile file = getWorkspaceFile();
		if (file != null) {
			return file.getLocation();
		}
		// Fallback: compute from repository work tree
		if (repository != null) {
			return new Path(repository.getWorkTree().getAbsolutePath())
					.append(path);
		}
		return null;
	}

	/**
	 * Returns the repository-relative path as an {@link IPath}.
	 *
	 * @return the repository-relative path
	 */
	public IPath getRepoRelativePath() {
		return new Path(path);
	}

	/**
	 * Converts a ChangedFile API model to UI model
	 *
	 * @param cf
	 *            the changed file from API
	 * @return the UI model instance
	 */
	public static PullRequestChangedFile fromChangedFile(ChangedFile cf) {
		ChangeType type;
		switch (cf.getType()) {
		case "ADD": //$NON-NLS-1$
			type = ChangeType.ADDED;
			break;
		case "DELETE": //$NON-NLS-1$
			type = ChangeType.DELETED;
			break;
		case "MOVE": //$NON-NLS-1$
			type = ChangeType.RENAMED;
			break;
		default:
			type = ChangeType.MODIFIED;
			break;
		}

		String srcPathStr = null;
		if (cf.getSrcPath() != null && cf.getSrcPath().getToString() != null) {
			srcPathStr = cf.getSrcPath().getToString();
		}

		return new PullRequestChangedFile(cf.getPath().getToString(),
				cf.getPath().getName(), type, srcPathStr);
	}

	@Override
	public String toString() {
		return "PullRequestChangedFile[" + path + ", " + changeType + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
