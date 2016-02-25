/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * Central cache for Repository instances.
 */
public class RepositoryCache {
	private final Map<File, Reference<Repository>> repositoryCache = new HashMap<File, Reference<Repository>>();

	private final IPreferenceChangeListener configuredRepositoriesListener = new IPreferenceChangeListener() {

		@Override
		public void preferenceChange(PreferenceChangeEvent event) {
			if (!RepositoryUtil.PREFS_DIRECTORIES.equals(event.getKey())) {
				return;
			}
			prune(Activator.getDefault().getRepositoryUtil().getRepositories());
		}

	};

	RepositoryCache() {
		InstanceScope.INSTANCE.getNode(Activator.getPluginId())
				.addPreferenceChangeListener(configuredRepositoriesListener);
	}

	void dispose() {
		InstanceScope.INSTANCE.getNode(Activator.getPluginId())
				.removePreferenceChangeListener(configuredRepositoriesListener);
	}

	/**
	 *
	 * @param gitDir
	 * @return an existing instance of Repository for <code>gitDir</code> or a
	 *         new one if no Repository instance for <code>gitDir</code> exists
	 *         in the cache.
	 * @throws IOException
	 */
	public synchronized Repository lookupRepository(final File gitDir)
			throws IOException {
		prune(repositoryCache);
		// Make sure we have a normalized path without .. segments here.
		File normalizedGitDir = new Path(gitDir.getAbsolutePath()).toFile();
		Reference<Repository> r = repositoryCache.get(normalizedGitDir);
		Repository d = r != null ? r.get() : null;
		if (d == null) {
			d = FileRepositoryBuilder.create(normalizedGitDir);
			repositoryCache.put(normalizedGitDir,
					new WeakReference<Repository>(d));
		}
		return d;
	}

	/**
	 * @return all Repository instances contained in the cache
	 */
	public synchronized Repository[] getAllRepositories() {
		prune(repositoryCache);
		List<Repository> repositories = new ArrayList<Repository>();
		for (Reference<Repository> reference : repositoryCache.values()) {
			repositories.add(reference.get());
		}
		return repositories.toArray(new Repository[repositories.size()]);
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

	private static void prune(Map<File, Reference<Repository>> map) {
		for (final Iterator<Map.Entry<File, Reference<Repository>>> i = map.entrySet()
				.iterator(); i.hasNext();) {
			Repository repository = i.next().getValue().get();
			if (repository == null || !repository.getDirectory().exists()) {
				i.remove();
			}
		}
	}

	private synchronized void prune(Set<String> configuredRepositories) {
		Iterator<File> iterator = repositoryCache.keySet().iterator();
		while (iterator.hasNext()) {
			File gitDir = iterator.next();
			if (!configuredRepositories.contains(gitDir.getAbsolutePath())) {
				iterator.remove();
			}
		}
	}

	/**
	 * TESTING ONLY!
	 * Unit tests can use this method to get a clean beginning state
	 */
	public synchronized void clear() {
		repositoryCache.clear();
	}

}
