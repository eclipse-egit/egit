/*******************************************************************************
 * Copyright (C) 2011, Philipp Thun <philipp.thun@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.ContainerTreeIterator;
import org.eclipse.egit.core.ContainerTreeIterator.ResourceEntry;
import org.eclipse.egit.core.IteratorService;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.decorators.IDecoratableResource.Staged;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;

/**
 * Helper class to create decoratable resources
 *
 * @see IDecoratableResource
 */
public class DecoratableResourceHelper {

	static final int T_HEAD = 0;

	static final int T_INDEX = 1;

	static final int T_WORKSPACE = 2;

	private static final Map<Repository, DirCache> repoToDirCache = new WeakHashMap<Repository, DirCache>();

	/**
	 * Creates a list of decoratable resources for the given list of resources
	 *
	 * @param resources
	 *            the list of resources to be decorated
	 * @return the list of decoratable resources
	 * @throws IOException
	 */
	public static IDecoratableResource[] createDecoratableResources(
			final IResource[] resources) throws IOException {
		if (resources == null)
			return null;

		// Use first (available) resource to get repository mapping
		int i = 0;
		while (resources[i] == null) {
			i++;
			if (i >= resources.length)
				// Array only contains nulls
				return null;
		}
		final RepositoryMapping mapping = RepositoryMapping
				.getMapping(resources[i]);

		final IDecoratableResource[] decoratableResources = new IDecoratableResource[resources.length];

		ArrayList<String> resourcePaths = new ArrayList<String>();
		for (i = 0; i < resources.length; i++) {
			final IResource resource = resources[i];
			if (resource != null && resource.getProject().isOpen()) {
				switch (resource.getType()) {
				case IResource.FILE:
					// Add file path to list used for bulk decoration
					resourcePaths.add(mapping.getRepoRelativePath(resource));
					break;
				case IResource.FOLDER:
				case IResource.PROJECT:
					// Decorate folder and project node separately
					try {
						decoratableResources[i] = new DecoratableResourceAdapter(
								resource);
					} catch (IOException e) {
						// Ignore - decoratableResources[i] is null
					}
					resourcePaths.add(null);
					break;
				}
			} else {
				resourcePaths.add(null);
			}
		}

		// Check resource paths before proceeding with bulk decoration
		boolean containsAtLeastOnePath = false;
		for (final String p : resourcePaths) {
			if (p != null) {
				containsAtLeastOnePath = true;
				break;
			}
		}
		if (!containsAtLeastOnePath)
			return decoratableResources;

		final TreeWalk treeWalk = createThreeWayTreeWalk(mapping, resourcePaths);
		if (treeWalk != null)
			while (treeWalk.next()) {
				i = resourcePaths.indexOf(treeWalk.getPathString());
				if (i != -1) {
					try {
						if (decoratableResources[i] == null)
							decoratableResources[i] = decorateResource(
									new DecoratableResource(resources[i]),
									treeWalk);
					} catch (IOException e) {
						// Ignore - decoratableResources[i] is null
					}
				}
			}
		return decoratableResources;
	}

	/**
	 * Creates a temporary decoratable resource for the given project
	 *
	 * This temporary decoratable resource only contains the name of the
	 * repository and the current branch.
	 *
	 * @param project
	 *            the project to be decorated
	 * @return the decoratable resource
	 * @throws IOException
	 */
	static IDecoratableResource createTemporaryDecoratableResource(
			final IProject project) throws IOException {
		final DecoratableResource decoratableResource = new DecoratableResource(
				project);
		final Repository repository = RepositoryMapping.getMapping(project)
				.getRepository();
		decoratableResource.repositoryName = getRepositoryName(repository);
		decoratableResource.branch = getShortBranch(repository);
		decoratableResource.tracked = true;
		return decoratableResource;
	}

	static DirCache getDirCache(Repository repository) throws IOException {
		synchronized(repoToDirCache) {
			DirCache dirCache = repoToDirCache.get(repository);
			if (dirCache != null && !dirCache.isOutdated())
				return dirCache;
			dirCache = repository.readDirCache();
			repoToDirCache.put(repository, dirCache);
			return dirCache;
		}
	}

	private static TreeWalk createThreeWayTreeWalk(
			final RepositoryMapping mapping,
			final ArrayList<String> resourcePaths) throws IOException {
		final Repository repository = mapping.getRepository();
		final TreeWalk treeWalk = new TreeWalk(repository);

		// Copy path list...
		final ArrayList<String> paths = new ArrayList<String>(resourcePaths);
		while (paths.remove(null)) {
			// ... and remove nulls
		}

		treeWalk.setFilter(PathFilterGroup.createFromStrings(paths, treeWalk.getPathEncoding()));
		treeWalk.setRecursive(true);
		treeWalk.reset();

		// Repository
		final ObjectId headId = repository.resolve(Constants.HEAD);
		if (headId != null)
			treeWalk.addTree(new RevWalk(repository).parseTree(headId));
		else
			treeWalk.addTree(new EmptyTreeIterator());

		// Index
		treeWalk.addTree(new DirCacheIterator(getDirCache(repository)));

		// Working directory
		treeWalk.addTree(IteratorService.createInitialIterator(repository));

		return treeWalk;
	}

	static DecoratableResource decorateResource(
			final DecoratableResource decoratableResource,
			final TreeWalk treeWalk) throws IOException {
		final WorkingTreeIterator workingTreeIterator = treeWalk.getTree(
				T_WORKSPACE, WorkingTreeIterator.class);
		if (workingTreeIterator == null)
			return null;
		if (!(workingTreeIterator instanceof ContainerTreeIterator))
			return null;
		final ContainerTreeIterator workspaceIterator = (ContainerTreeIterator) workingTreeIterator;
		final ResourceEntry resourceEntry = workspaceIterator
				.getResourceEntry();

		if (resourceEntry == null)
			return null;

		if (workspaceIterator.isEntryIgnored())
			decoratableResource.ignored = true;

		final int mHead = treeWalk.getRawMode(T_HEAD);
		final int mIndex = treeWalk.getRawMode(T_INDEX);

		if (mHead == FileMode.MISSING.getBits()
				&& mIndex == FileMode.MISSING.getBits())
			return decoratableResource;
		else
			// tracked files are never ignored
			decoratableResource.ignored = false;

		decoratableResource.tracked = true;

		if (mHead == FileMode.MISSING.getBits()) {
			decoratableResource.staged = Staged.ADDED;
		} else if (mIndex == FileMode.MISSING.getBits()) {
			decoratableResource.staged = Staged.REMOVED;
		} else if (mHead != mIndex
				|| (mIndex != FileMode.TREE.getBits() && !treeWalk.idEqual(
						T_HEAD, T_INDEX))) {
			decoratableResource.staged = Staged.MODIFIED;
		} else {
			decoratableResource.staged = Staged.NOT_STAGED;
		}

		final DirCacheIterator indexIterator = treeWalk.getTree(T_INDEX,
				DirCacheIterator.class);
		final DirCacheEntry indexEntry = indexIterator != null ? indexIterator
				.getDirCacheEntry() : null;

		if (indexEntry == null)
			return decoratableResource;

		if (indexEntry.getStage() > 0)
			decoratableResource.conflicts = true;

		if (indexEntry.isAssumeValid()) {
			decoratableResource.dirty = false;
			decoratableResource.assumeValid = true;
		} else {
			if (workspaceIterator.isModified(indexEntry, true))
				decoratableResource.dirty = true;
		}
		return decoratableResource;
	}

	static String getRepositoryName(Repository repository) {
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		RepositoryState state = repository.getRepositoryState();
		if (state != RepositoryState.SAFE)
			return repoName + '|' + state.getDescription();
		else
			return repoName;
	}

	static String getShortBranch(Repository repository) throws IOException {
		Ref head = repository.getRef(Constants.HEAD);
		if (head != null && !head.isSymbolic()) {
			String refString = Activator
					.getDefault()
					.getRepositoryUtil()
					.mapCommitToRef(repository, repository.getFullBranch(),
							false);
			if (refString != null) {
				return repository.getFullBranch().substring(0, 7)
						+ "... (" + refString + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			} else
				return repository.getFullBranch().substring(0, 7) + "..."; //$NON-NLS-1$
		}

		if (head == null || head.getObjectId() == null)
			return UIText.DecoratableResourceHelper_noHead;

		return repository.getBranch();
	}
}
