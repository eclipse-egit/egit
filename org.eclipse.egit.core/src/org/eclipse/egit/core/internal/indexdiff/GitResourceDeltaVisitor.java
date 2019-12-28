/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Jens Baumgart <jens.baumgart@sap.com> - initial implementation in IndexDifCacheEntry
 *   Dariusz Luksza - extraction to separate class
 *   Andre Bossert <anb0s@anbos.de> - Cleaning up the DecoratableResourceAdapter
 *******************************************************************************/
package org.eclipse.egit.core.internal.indexdiff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;

/**
 * Git specific implementation of {@link IResourceDeltaVisitor} that ignores not
 * interesting resources. Also collects list of paths and resources to update
 */
public class GitResourceDeltaVisitor implements IResourceDeltaVisitor {

	/**
	 * Bit-mask describing interesting changes for IResourceChangeListener
	 * events
	 */
	private static int INTERESTING_CHANGES = IResourceDelta.CONTENT
			| IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO
			| IResourceDelta.OPEN | IResourceDelta.REPLACED
			| IResourceDelta.TYPE;

	private final Repository repository;

	private final Collection<String> filesToUpdate;

	private final Collection<IResource> resourcesToUpdate;

	private final Map<IProject, IPath> deletedProjects;

	private boolean gitIgnoreChanged = false;

	private boolean projectDeleted = false;

	/**
	 * Constructs {@link GitResourceDeltaVisitor}
	 *
	 * @param repository
	 *            which should be considered during visiting
	 *            {@link IResourceDelta}s
	 */
	public GitResourceDeltaVisitor(Repository repository) {
		this(repository, Collections.<IProject, IPath> emptyMap());
	}

	/**
	 * Constructs {@link GitResourceDeltaVisitor}
	 *
	 * @param repository
	 *            which should be considered during visiting
	 *            {@link IResourceDelta}s
	 * @param deletedProjects
	 *            possibly empty map of projects that were removed from the
	 *            workspace, with their (former) locations
	 */
	public GitResourceDeltaVisitor(Repository repository,
			Map<IProject, IPath> deletedProjects) {
		this.repository = repository;

		filesToUpdate = new HashSet<>();
		resourcesToUpdate = new HashSet<>();
		this.deletedProjects = deletedProjects;
	}

	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		final IResource resource = delta.getResource();
		if (resource.getType() == IResource.ROOT) {
			return true;
		}

		if (resource.getType() == IResource.PROJECT) {
			if (delta.getKind() == IResourceDelta.REMOVED) {
				IPath loc = deletedProjects.remove(resource);
				if (loc != null) {
					projectDeleted |= !loc.toFile().isDirectory();
				}
				return false;
			}
			// If the resource is not part of a project under
			// Git revision control or from a different repository
			if (!ResourceUtil.isSharedWithGit(resource)) {
				// Ignore the change for project and its children
				return false;
			}
			GitProjectData gitData = GitProjectData.get((IProject) resource);
			if (gitData == null) {
				return false;
			}
			RepositoryMapping mapping = gitData.getRepositoryMapping(resource);
			if (mapping == null || !gitData.hasInnerRepositories()
					&& mapping.getRepository() != repository) {
				return false;
			}
			// continue with children
			return true;
		}

		Repository repositoryOfResource = null;
		if (resource.isLinked()) {
			IPath location = resource.getLocation();
			if (location == null) {
				return false;
			}
			repositoryOfResource = ResourceUtil.getRepository(location);
			// Ignore linked files, folders and their children, if they're not
			// in the same repository
			if (repository != repositoryOfResource) {
				return false;
			}
		} else {
			repositoryOfResource = ResourceUtil.getRepository(resource);
		}

		if (resource.getType() == IResource.FOLDER) {
			GitProjectData gitData = GitProjectData.get(resource.getProject());
			if (gitData == null) {
				return false;
			}
			if (repositoryOfResource == null || !gitData.isProtected(resource)
					&& repositoryOfResource != repository) {
				return false;
			}
			if (delta.getKind() == IResourceDelta.ADDED) {
				IPath repoRelativePath = ResourceUtil.getRepositoryRelativePath(
						resource.getLocation(), repository);
				if (repoRelativePath == null) {
					return false;
				}
				if (!repoRelativePath.isEmpty()) {
					String path = repoRelativePath.toPortableString() + "/"; //$NON-NLS-1$
					if (isIgnoredInOldIndex(path)) {
						return true; // keep going to catch .gitignore files.
					}
					filesToUpdate.add(path);
					resourcesToUpdate.add(resource);
				}
			}

			// continue with children
			return true;
		}

		if (repositoryOfResource != repository) {
			return false;
		}

		if (!isInteresting(delta)) {
			return false;
		}

		if (resource.getName().equals(Constants.DOT_GIT_IGNORE)) {
			gitIgnoreChanged = true;
			return false;
		}

		IPath repoRelativePath = ResourceUtil
				.getRepositoryRelativePath(resource.getLocation(), repository);
		if (repoRelativePath == null) {
			resourcesToUpdate.add(resource);
			return true;
		}

		String path = repoRelativePath.toPortableString();
		if (isIgnoredInOldIndex(path)) {
			// This file is ignored in the old index, and ignore rules did not
			// change: ignore the delta to avoid unnecessary index updates
			return false;
		}

		filesToUpdate.add(path);
		resourcesToUpdate.add(resource);
		return true;
	}

	/**
	 * If the file has changed but not in a way that we care about (e.g. marker
	 * changes to files) then ignore
	 *
	 * @param delta
	 * @return true - if change is interesting
	 */
	static boolean isInteresting(IResourceDelta delta) {
		if (delta.getKind() == IResourceDelta.CHANGED
				&& (delta.getFlags() & INTERESTING_CHANGES) == 0) {
			return false;
		}
		return true;
	}

	/**
	 * @param path
	 *            the repository relative path of the resource to check
	 * @return whether the given path is ignored by the given
	 *         {@link IndexDiffCacheEntry}
	 */
	private boolean isIgnoredInOldIndex(String path) {
		if (gitIgnoreChanged) {
			return false;
		}
		IndexDiffCacheEntry entry = null;
		IndexDiffCache cache = Activator.getDefault().getIndexDiffCache();
		if (cache != null) {
			entry = cache.getIndexDiffCacheEntry(repository);
		}
		// fall back to processing all changes as long as there is no old index.
		if (entry == null) {
			return false;
		}

		IndexDiffData indexDiff = entry.getIndexDiff();
		if (indexDiff == null) {
			return false;
		}

		String p = path;
		Set<String> ignored = indexDiff.getIgnoredNotInIndex();
		while (p != null) {
			if (ignored.contains(p)) {
				return true;
			}

			p = skipLastSegment(p);
		}

		return false;
	}

	private String skipLastSegment(String path) {
		int slashPos = path.lastIndexOf('/');
		return slashPos == -1 ? null : path.substring(0, slashPos);
	}

	/**
	 * @return collection of files to update
	 */
	public Collection<IFile> getFileResourcesToUpdate() {
		Collection<IFile> result = new ArrayList<>();
		for (IResource resource : resourcesToUpdate)
			if (resource instanceof IFile)
				result.add((IFile) resource);
		return result;
	}

	/**
	 * @return collection of resources to update
	 */
	public Collection<IResource> getResourcesToUpdate() {
		return resourcesToUpdate;
	}

	/**
	 * @return collection of files / folders to update. Folder paths end with /
	 */
	public Collection<String> getFilesToUpdate() {
		return filesToUpdate;
	}

	/**
	 * @return {@code true} when content .gitignore file changed, {@code false}
	 *         otherwise
	 */
	public boolean getGitIgnoreChanged() {
		return gitIgnoreChanged;
	}

	/**
	 * Returns whether a project was deleted.
	 *
	 * @return {@code true} if a project in the repository was deleted,
	 *         {@code false} otherwise
	 */
	public boolean isProjectDeleted() {
		return projectDeleted;
	}
}
