/*******************************************************************************
 * Copyright (C) 2007, IBM Corporation and others
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2008, Tor Arne Vestbø <torarnv@gmail.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2011, Christian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2015, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Factored out from DecoratableResourceAdapter
 *                                           and GitLightweightDecorator
 *******************************************************************************/
package org.eclipse.egit.ui.internal.resources;

import static org.eclipse.jgit.lib.Repository.stripWorkDir;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.resources.IResourceState.StagingState;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Repository;

/**
 * Factory for creating {@link IResourceState}s.
 */
public class ResourceStateFactory {

	@NonNull
	private static final ResourceStateFactory INSTANCE = new ResourceStateFactory();

	/**
	 * Retrieves the singleton instance of the {@link ResourceStateFactory}.
	 *
	 * @return the factory singleton
	 */
	@NonNull
	public static ResourceStateFactory getInstance() {
		return INSTANCE;
	}

	/**
	 * Returns the {@link IndexDiffData} for a given {@link IResource}, provided
	 * the resource is not a phantom resource and belongs to a git-racked
	 * project.
	 *
	 * @param resource
	 *            context to get the repository to get the index diff data from
	 * @return the IndexDiffData, or {@code null} if none.
	 */
	@Nullable
	public IndexDiffData getIndexDiffDataOrNull(@Nullable IResource resource) {
		if (resource == null || resource.getType() == IResource.ROOT) {
			return null;
		}

		// Don't decorate non-existing resources
		if (!resource.exists() && !resource.isPhantom()) {
			return null;
		}

		// Make sure we're dealing with a project under Git revision control
		final RepositoryMapping mapping = RepositoryMapping
				.getMapping(resource);
		if (mapping == null) {
			return null;
		}

		Repository repo = mapping.getRepository();
		if (repo == null) {
			return null;
		}

		// For bare repository just return empty data
		if (repo.isBare()) {
			return new IndexDiffData();
		}

		// Cannot decorate linked resources
		if (mapping.getRepoRelativePath(resource) == null) {
			return null;
		}

		IndexDiffCacheEntry diffCacheEntry = org.eclipse.egit.core.Activator
				.getDefault().getIndexDiffCache().getIndexDiffCacheEntry(repo);
		if (diffCacheEntry == null) {
			return null;
		}
		return diffCacheEntry.getIndexDiff();

	}

	/**
	 * Computes an {@link IResourceState} for the given {@link IResource} from
	 * the given {@link IndexDiffData}.
	 *
	 * @param indexDiffData
	 *            to compute the state from
	 * @param resource
	 *            to get the state of
	 * @return the state
	 */
	@NonNull
	public IResourceState get(@NonNull IndexDiffData indexDiffData,
			@NonNull IResource resource) {
		ResourceState result = new ResourceState();
		switch (resource.getType()) {
		case IResource.FILE:
			extractResourceProperties(indexDiffData, resource, result);
			break;
		case IResource.PROJECT:
			//$FALL-THROUGH$
		case IResource.FOLDER:
			extractContainerProperties(indexDiffData, resource, result);
			break;
		default:
			// Nothing.
		}
		return result;
	}

	private void extractResourceProperties(@NonNull IndexDiffData indexDiffData,
			@NonNull IResource resource, @NonNull ResourceState state) {
		final RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
		if (mapping == null) {
			return;
		}
		Repository repository = mapping.getRepository();
		String repoRelativePath = makeRepositoryRelative(repository, resource);
		if (repoRelativePath == null) {
			return;
		}
		// ignored
		Set<String> ignoredFiles = indexDiffData.getIgnoredNotInIndex();
		boolean ignored = ignoredFiles.contains(repoRelativePath)
				|| containsPrefixPath(ignoredFiles, repoRelativePath);
		state.setIgnored(ignored);
		Set<String> untracked = indexDiffData.getUntracked();
		state.setTracked(!ignored && !untracked.contains(repoRelativePath));

		Set<String> added = indexDiffData.getAdded();
		Set<String> removed = indexDiffData.getRemoved();
		Set<String> changed = indexDiffData.getChanged();
		if (added.contains(repoRelativePath)) {
			state.setStagingState(StagingState.ADDED);
		} else if (removed.contains(repoRelativePath)) {
			state.setStagingState(StagingState.REMOVED);
		} else if (changed.contains(repoRelativePath)) {
			state.setStagingState(StagingState.MODIFIED);
		} else {
			state.setStagingState(StagingState.NOT_STAGED);
		}

		// conflicting
		Set<String> conflicting = indexDiffData.getConflicting();
		state.setConflicts(conflicting.contains(repoRelativePath));

		// locally modified
		Set<String> modified = indexDiffData.getModified();
		state.setDirty(modified.contains(repoRelativePath));
	}

	private void extractContainerProperties(
			@NonNull IndexDiffData indexDiffData, @NonNull IResource resource,
			@NonNull ResourceState state) {
		final RepositoryMapping mapping = RepositoryMapping
				.getMapping(resource);
		if (mapping == null) {
			return;
		}
		Repository repository = mapping.getRepository();
		if (repository == null) {
			return;
		}
		String repoRelative = makeRepositoryRelative(repository, resource);
		if (repoRelative == null) {
			return;
		}
		String repoRelativePath = repoRelative + "/"; //$NON-NLS-1$

		if (ResourceUtil.isSymbolicLink(repository, repoRelativePath)) {
			extractResourceProperties(indexDiffData, resource, state);
			return;
		}

		Set<String> ignoredFiles = indexDiffData.getIgnoredNotInIndex();
		Set<String> untrackedFolders = indexDiffData.getUntrackedFolders();
		boolean ignored = containsPrefixPath(ignoredFiles, repoRelativePath)
				|| !hasContainerAnyFiles(resource);
		state.setIgnored(ignored);
		state.setTracked(!ignored
				&& !containsPrefixPath(untrackedFolders, repoRelativePath));

		// containers are marked as staged whenever file was added, removed or
		// changed
		Set<String> changed = new HashSet<String>(indexDiffData.getChanged());
		changed.addAll(indexDiffData.getAdded());
		changed.addAll(indexDiffData.getRemoved());
		if (containsPrefix(changed, repoRelativePath)) {
			state.setStagingState(StagingState.MODIFIED);
		} else {
			state.setStagingState(StagingState.NOT_STAGED);
		}
		// conflicting
		Set<String> conflicting = indexDiffData.getConflicting();
		state.setConflicts(containsPrefix(conflicting, repoRelativePath));

		// locally modified / untracked
		Set<String> modified = indexDiffData.getModified();
		Set<String> untracked = indexDiffData.getUntracked();
		Set<String> missing = indexDiffData.getMissing();
		state.setDirty(containsPrefix(modified, repoRelativePath)
				|| containsPrefix(untracked, repoRelativePath)
				|| containsPrefix(missing, repoRelativePath));
	}

	@Nullable
	private String makeRepositoryRelative(Repository repository,
			IResource res) {
		IPath location = res.getLocation();
		if (location == null) {
			return null;
		}
		if (repository.isBare()) {
			return null;
		}
		File workTree = repository.getWorkTree();
		return stripWorkDir(workTree, location.toFile());
	}

	private boolean containsPrefix(Set<String> collection, String prefix) {
		// when prefix is empty we are handling repository root, therefore we
		// should return true whenever collection isn't empty
		if (prefix.length() == 1 && !collection.isEmpty())
			return true;

		for (String path : collection)
			if (path.startsWith(prefix))
				return true;
		return false;
	}

	private boolean containsPrefixPath(Set<String> collection, String path) {
		for (String entry : collection) {
			String entryPath;
			if (entry.endsWith("/")) //$NON-NLS-1$
				entryPath = entry;
			else
				entryPath = entry + "/"; //$NON-NLS-1$
			if (path.startsWith(entryPath))
				return true;
		}
		return false;
	}

	private boolean hasContainerAnyFiles(IResource resource) {
		if (resource instanceof IContainer) {
			IContainer container = (IContainer) resource;
			try {
				return anyFile(container.members());
			} catch (CoreException e) {
				// if can't get any info, treat as with file
				return true;
			}
		}
		throw new IllegalArgumentException("Expected a container resource."); //$NON-NLS-1$
	}

	private boolean anyFile(IResource[] members) {
		for (IResource member : members) {
			if (member.getType() == IResource.FILE)
				return true;
			else if (member.getType() == IResource.FOLDER)
				if (hasContainerAnyFiles(member))
					return true;
		}
		return false;
	}

}
