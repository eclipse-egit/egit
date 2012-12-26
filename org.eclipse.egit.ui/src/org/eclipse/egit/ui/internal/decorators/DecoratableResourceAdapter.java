/*******************************************************************************
 * Copyright (C) 2007, IBM Corporation and others
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2008, Tor Arne Vestbø <torarnv@gmail.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2011, Christian Halstrick <christian.halstrick@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.decorators;

import static org.eclipse.jgit.lib.Repository.stripWorkDir;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FS;

class DecoratableResourceAdapter extends DecoratableResource {

	private final RepositoryMapping mapping;

	private final Repository repository;

	private final boolean trace;

	private IndexDiffData indexDiffData;

	public DecoratableResourceAdapter(IndexDiffData indexDiffData, IResource resourceToWrap)
			throws IOException {
		super(resourceToWrap);
		this.indexDiffData = indexDiffData;
		trace = GitTraceLocation.DECORATION.isActive();
		long start = 0;
		if (trace) {
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.DECORATION.getLocation(),
					"Decorate " + resource.getFullPath()); //$NON-NLS-1$
			start = System.currentTimeMillis();
		}
		try {
			mapping = RepositoryMapping.getMapping(resource);
			if (mapping == null) {
				repository = null;
				return;
			}
			repository = mapping.getRepository();
			if (repository == null)
				return;

			File file = resource.getLocation().toFile();
			if (FS.DETECTED.isSymLink(file))
				extractResourceProperties();
			else {
				switch (resource.getType()) {
				case IResource.FILE:
					extractResourceProperties();
					break;
				case IResource.PROJECT:
					// We only need this very expensive info for project decoration
					repositoryName = DecoratableResourceHelper
							.getRepositoryName(repository);
					branch = DecoratableResourceHelper.getShortBranch(repository);
					branchStatus = DecoratableResourceHelper.getBranchStatus(repository);
					tracked = true;
					//$FALL-THROUGH$
				case IResource.FOLDER:
					extractContainerProperties();
					break;
				}
			}
		} finally {
			if (trace)
				GitTraceLocation
						.getTrace()
						.trace(GitTraceLocation.DECORATION.getLocation(),
								"Decoration took " + (System.currentTimeMillis() - start) //$NON-NLS-1$
										+ " ms"); //$NON-NLS-1$
		}
	}

	@Override
	public String toString() {
		return "DecoratableResourceAdapter[" + getName() + (isTracked() ? ", tracked" : "") + (isIgnored() ? ", ignored" : "") + (isDirty() ? ", dirty" : "") + (hasConflicts() ? ",conflicts" : "") + ", staged=" + staged + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$//$NON-NLS-7$//$NON-NLS-8$//$NON-NLS-9$//$NON-NLS-10$//$NON-NLS-11$
	}

	private void extractResourceProperties() {
		String repoRelativePath = makeRepoRelative(resource);

		// ignored
		Set<String> ignoredFiles = indexDiffData.getIgnoredNotInIndex();
		ignored = ignoredFiles.contains(repoRelativePath)
				|| containsPrefixPath(ignoredFiles, repoRelativePath);
		Set<String> untracked = indexDiffData.getUntracked();
		if (!untracked.contains(repoRelativePath) && !ignored) {
			if (indexDiffData.getAdded().contains(repoRelativePath))
				tracked = true;
			else {
				try {
					TreeWalk tw = TreeWalk.forPath(repository, repoRelativePath, repository.resolve("HEAD^{tree}")); //$NON-NLS-1$
					if (tw == null)
						tracked = false;
					else
						tracked = true;
				} catch (Exception e) {
					tracked = false;
				}
			}
		}
		Set<String> added = indexDiffData.getAdded();
		Set<String> removed = indexDiffData.getRemoved();
		Set<String> changed = indexDiffData.getChanged();
		if (added.contains(repoRelativePath)) // added
			staged = Staged.ADDED;
		else if (removed.contains(repoRelativePath)) // removed
			staged = Staged.REMOVED;
		else if (changed.contains(repoRelativePath)) // changed and added into index
			staged = Staged.MODIFIED;
		else
			staged = Staged.NOT_STAGED;

		// conflicting
		Set<String> conflicting = indexDiffData.getConflicting();
		conflicts = conflicting.contains(repoRelativePath);

		// locally modified
		Set<String> modified = indexDiffData.getModified();
		dirty = modified.contains(repoRelativePath);
	}

	private void extractContainerProperties() {
		String repoRelativePath = makeRepoRelative(resource) + "/"; //$NON-NLS-1$

		Set<String> ignoredFiles = indexDiffData.getIgnoredNotInIndex();
		Set<String> untrackedFolders = indexDiffData.getUntrackedFolders();
		ignored = containsPrefixPath(ignoredFiles, repoRelativePath);

		if (ignored)
			tracked = false;
		else
			tracked = !containsPrefixPath(untrackedFolders, repoRelativePath);

		// containers are marked as staged whenever file was added, removed or
		// changed
		Set<String> changed = new HashSet<String>(indexDiffData.getChanged());
		changed.addAll(indexDiffData.getAdded());
		changed.addAll(indexDiffData.getRemoved());
		if (containsPrefix(changed, repoRelativePath))
			staged = Staged.MODIFIED;
		else
			staged = Staged.NOT_STAGED;

		// conflicting
		Set<String> conflicting = indexDiffData.getConflicting();
		conflicts = containsPrefix(conflicting, repoRelativePath);

		// locally modified / untracked
		Set<String> modified = indexDiffData.getModified();
		Set<String> untracked = indexDiffData.getUntracked();
		Set<String> missing = indexDiffData.getMissing();
		dirty = containsPrefix(modified, repoRelativePath)
				|| containsPrefix(untracked, repoRelativePath)
				|| containsPrefix(missing, repoRelativePath);
	}

	private String makeRepoRelative(IResource res) {
		return stripWorkDir(repository.getWorkTree(), res.getLocation()
				.toFile());
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

}
