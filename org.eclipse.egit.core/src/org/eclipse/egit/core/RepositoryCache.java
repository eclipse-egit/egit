/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * Central cache for Repository instances.
 */
public class RepositoryCache {
	private final Map<File, Reference<Repository>> repositoryCache = new HashMap<File, Reference<Repository>>();

	/**
	 * Looks in the cache for a {@link Repository} matching the given git
	 * directory. If there is no such Repository instance in the cache, one is
	 * created.
	 *
	 * @param gitDir
	 * @return an existing instance of Repository for <code>gitDir</code> or a
	 *         new one if no Repository instance for <code>gitDir</code> exists
	 *         in the cache.
	 * @throws IOException
	 */
	public Repository lookupRepository(final File gitDir) throws IOException {
		prune();
		// Make sure we have a normalized path without .. segments here.
		File normalizedGitDir = new Path(gitDir.getAbsolutePath()).toFile();
		synchronized (repositoryCache) {
			Reference<Repository> r = repositoryCache.get(normalizedGitDir);
			Repository d = r != null ? r.get() : null;
			if (d == null) {
				d = FileRepositoryBuilder.create(normalizedGitDir);
				repositoryCache.put(normalizedGitDir,
						new WeakReference<Repository>(d));
			}
			return d;
		}
	}

	/**
	 * Looks in the cache for a {@link Repository} matching the given git
	 * directory.
	 *
	 * @param gitDir
	 * @return the cached repository, if any, or {@code null} if node found in
	 *         the cache.
	 */
	public Repository getRepository(final File gitDir) {
		if (gitDir == null) {
			return null;
		}
		prune();
		File normalizedGitDir = new Path(gitDir.getAbsolutePath()).toFile();
		synchronized (repositoryCache) {
			Reference<Repository> r = repositoryCache.get(normalizedGitDir);
			return r != null ? r.get() : null;
		}
	}

	/**
	 * @return all Repository instances contained in the cache
	 */
	public Repository[] getAllRepositories() {
		prune();
		List<Repository> repositories = new ArrayList<Repository>();
		synchronized (repositoryCache) {
			for (Reference<Repository> reference : repositoryCache.values()) {
				Repository repository = reference.get();
				if (repository != null) {
					repositories.add(repository);
				}
			}
		}
		return repositories.toArray(new Repository[0]);
	}

	/**
	 * Lookup the closest git repository with a working tree containing the
	 * given resource. If there are repositories nested above in the file system
	 * hierarchy we select the closest one above the given resource.
	 *
	 * @param resource
	 *            the resource to find the repository for
	 * @return the git repository which has the given resource in its working
	 *         tree, or null if none found
	 * @since 3.2
	 */
	public Repository getRepository(final IResource resource) {
		IPath location = resource.getLocation();
		if (location == null)
			return null;
		return getRepository(location);
	}

	/**
	 * Lookup the closest git repository with a working tree containing the
	 * given file location. If there are repositories nested above in the file
	 * system hierarchy we select the closest one above the given location.
	 *
	 * @param location
	 *            the file location to find the repository for
	 * @return the git repository which has the given location in its working
	 *         tree, or null if none found
	 * @since 3.2
	 */
	public Repository getRepository(final IPath location) {
		Repository[] repositories = getAllRepositories();
		Repository repository = null;
		int largestSegmentCount = 0;
		for (Repository r : repositories) {
			if (!r.isBare()) {
				IPath repoPath = new Path(r.getWorkTree().getAbsolutePath());
				if (location != null && repoPath.isPrefixOf(location)) {
					if (repository == null
							|| repoPath.segmentCount() > largestSegmentCount) {
						repository = r;
						largestSegmentCount = repoPath.segmentCount();
					}
				}
			}
		}
		return repository;
	}

	private void prune() {
		List<File> toRemove = new ArrayList<>();
		synchronized (repositoryCache) {
			for (Iterator<Map.Entry<File, Reference<Repository>>> i = repositoryCache
					.entrySet().iterator(); i.hasNext();) {
				Map.Entry<File, Reference<Repository>> entry = i.next();
				Repository repository = entry.getValue().get();
				if (repository == null || !repository.getDirectory().exists()) {
					i.remove();
					toRemove.add(entry.getKey());
				}
			}
		}
		IndexDiffCache cache = Activator.getDefault().getIndexDiffCache();
		if (cache != null) {
			for (File f : toRemove) {
				cache.remove(f);
			}
		}
	}

	/**
	 * Removes all cached repositories and their IndexDiffCache entries.
	 */
	public void clear() {
		List<File> gitDirs;
		synchronized (repositoryCache) {
			gitDirs = new ArrayList<>(repositoryCache.keySet());
			repositoryCache.clear();
		}
		IndexDiffCache cache = Activator.getDefault().getIndexDiffCache();
		if (cache != null) {
			for (File f : gitDirs) {
				cache.remove(f);
			}
		}
	}

}
