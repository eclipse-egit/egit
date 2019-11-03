/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2016, 2019 Thomas Wolf <thomas.wolf@paranor.ch>
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
import java.lang.ref.ReferenceQueue;
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
import org.eclipse.jgit.events.ConfigChangedEvent;
import org.eclipse.jgit.events.IndexChangedEvent;
import org.eclipse.jgit.events.ListenerList;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.WorkingTreeModifiedEvent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * Central cache for Repository instances.
 */
public class RepositoryCache {

	// EGit uses a weak-reference cache. In Eclipse, EGit can never be sure that
	// a repo instance isn't still used somewhere, and thus it never explicitly
	// closes repository instances. Instead, this cache wraps any repository
	// in a {@link RepositoryHandle} and returns that, and closes the wrapped
	// repository once that handle is garbage collected.

	private final ReferenceQueue<RepositoryHandle> queue = new ReferenceQueue<>();

	private final Map<File, RepositoryReference> repositoryCache = new HashMap<>();

	private final ListenerList globalListeners = new ListenerList();

	RepositoryCache() {
		new Closer(queue).start();
		// Set up listeners on the JGit global listener list to be able to
		// re-fire events with the correct repository.
		ListenerList global = Repository.getGlobalListenerList();
		global.addConfigChangedListener(event -> {
			Repository repo = getRepository(
					event.getRepository().getDirectory());
			if (repo == null || repo == event.getRepository()) {
				// Re-dispatch the original event
				globalListeners.dispatch(event);
			} else {
				ConfigChangedEvent newEvent = new ConfigChangedEvent();
				newEvent.setRepository(repo);
				globalListeners.dispatch(newEvent);
			}
		});
		global.addIndexChangedListener(event -> {
			Repository repo = getRepository(
					event.getRepository().getDirectory());
			if (repo == null || repo == event.getRepository()) {
				globalListeners.dispatch(event);
			} else {
				IndexChangedEvent newEvent = new IndexChangedEvent(
						event.isInternal());
				newEvent.setRepository(repo);
				globalListeners.dispatch(newEvent);
			}
		});
		global.addRefsChangedListener(event -> {
			Repository repo = getRepository(
					event.getRepository().getDirectory());
			if (repo == null || repo == event.getRepository()) {
				globalListeners.dispatch(event);
			} else {
				RefsChangedEvent newEvent = new RefsChangedEvent();
				newEvent.setRepository(repo);
				globalListeners.dispatch(newEvent);
			}
		});
		global.addWorkingTreeModifiedListener(event -> {
			Repository repo = getRepository(
					event.getRepository().getDirectory());
			if (repo == null || repo == event.getRepository()) {
				globalListeners.dispatch(event);
			} else {
				WorkingTreeModifiedEvent newEvent = new WorkingTreeModifiedEvent(
						event.getModified(), event.getDeleted());
				newEvent.setRepository(repo);
				globalListeners.dispatch(newEvent);
			}
		});
	}

	/**
	 * Gets a global listener list that fires events from any repository. EGit
	 * uses this instead of {@link Repository#getGlobalListenerList()} so that
	 * we can be sure to only ever pass around {@link RepositoryHandle}s instead
	 * of the underlying real repositories.
	 *
	 * @return the global listener list
	 */
	public ListenerList getGlobalListenerList() {
		return globalListeners;
	}

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
		// Make sure we have a normalized path without .. segments here.
		File normalizedGitDir = new Path(gitDir.getAbsolutePath()).toFile();
		synchronized (repositoryCache) {
			RepositoryReference r = repositoryCache.get(normalizedGitDir);
			if (r == null) {
				Repository inner = FileRepositoryBuilder
						.create(normalizedGitDir);
				RepositoryHandle result = new RepositoryHandle(inner);
				repositoryCache.put(normalizedGitDir,
						new RepositoryReference(result, inner, queue));
				return result;
			} else {
				Repository result = r.get();
				if (result != null && result.getDirectory().exists()) {
					return result;
				} else {
					Closer.closeReference(
							repositoryCache.remove(normalizedGitDir));
				}
			}
		}
		// If we get here, we found a stale repository. We must remove
		// a possibly still existing IndexDiffCache outside the synchronized
		// block, otherwise we may run into a deadlock due to lock inversion
		// between our repositoryCache and IndexDiffCache.entries.
		IndexDiffCache cache = Activator.getDefault().getIndexDiffCache();
		if (cache != null) {
			cache.remove(normalizedGitDir);
		}
		return lookupRepository(gitDir);
	}

	/**
	 * A {@link WeakReference} that keeps the link to the underlying real
	 * repository of a {@link RepositoryHandle}. This is necessary because the
	 * referent (the handle) is already nulled out by the time the weak
	 * reference is returned from the {@link ReferenceQueue}.
	 */
	private static class RepositoryReference
			extends WeakReference<RepositoryHandle> {

		private Repository inner;

		public RepositoryReference(RepositoryHandle handle, Repository delegate,
				ReferenceQueue<RepositoryHandle> queue) {
			super(handle, queue);
			inner = delegate;
		}

		public Repository getRepository() {
			return inner;
		}

		public void clearRepository() {
			inner = null;
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
		File normalizedGitDir = new Path(gitDir.getAbsolutePath()).toFile();
		synchronized (repositoryCache) {
			RepositoryReference r = repositoryCache.get(normalizedGitDir);
			if (r == null) {
				return null;
			}
			Repository result = r.get();
			if (result != null && result.getDirectory().exists()) {
				return result;
			}
			Closer.closeReference(repositoryCache.remove(normalizedGitDir));
		}
		IndexDiffCache cache = Activator.getDefault().getIndexDiffCache();
		if (cache != null) {
			cache.remove(normalizedGitDir);
		}
		return null;
	}

	/**
	 * @return all Repository instances contained in the cache
	 */
	public Repository[] getAllRepositories() {
		List<Repository> repositories = new ArrayList<>();
		List<File> toRemove = new ArrayList<>();
		synchronized (repositoryCache) {
			for (Iterator<Map.Entry<File, RepositoryReference>> i = repositoryCache
					.entrySet().iterator(); i.hasNext();) {
				Map.Entry<File, RepositoryReference> entry = i.next();
				Repository repository = entry.getValue().get();
				if (repository == null || !repository.getDirectory().exists()) {
					i.remove();
					Closer.closeReference(entry.getValue());
					toRemove.add(entry.getKey());
				} else {
					repositories.add(repository);
				}
			}
		}
		removeIndexDiffCaches(toRemove);
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
		return location == null ? null : getRepository(location);
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
		if (location == null) {
			return null;
		}
		Repository repository = null;
		int largestSegmentCount = 0;
		List<File> toRemove = new ArrayList<>();
		synchronized (repositoryCache) {
			for (Iterator<Map.Entry<File, RepositoryReference>> i = repositoryCache
					.entrySet().iterator(); i.hasNext();) {
				Map.Entry<File, RepositoryReference> entry = i.next();
				Repository repo = entry.getValue().get();
				if (repo == null) {
					i.remove();
					Closer.closeReference(entry.getValue());
					toRemove.add(entry.getKey());
					continue;
				}
				if (repo.isBare()) {
					continue;
				}
				IPath repoPath = new Path(repo.getWorkTree().getAbsolutePath());
				if (repoPath.isPrefixOf(location)) {
					if (repository == null
							|| repoPath.segmentCount() > largestSegmentCount) {
						if (!repo.getDirectory().exists()) {
							i.remove();
							Closer.closeReference(entry.getValue());
							toRemove.add(entry.getKey());
							continue;
						}
						repository = repo;
						largestSegmentCount = repoPath.segmentCount();
					}
				}
			}
		}
		removeIndexDiffCaches(toRemove);
		return repository;
	}

	/**
	 * Removes all cached repositories and their IndexDiffCache entries.
	 */
	public void clear() {
		List<File> gitDirs;
		List<RepositoryReference> references;
		synchronized (repositoryCache) {
			gitDirs = new ArrayList<>(repositoryCache.keySet());
			references = new ArrayList<>(repositoryCache.values());
			repositoryCache.clear();
		}
		removeIndexDiffCaches(gitDirs);
		references.forEach(Closer::closeReference);
	}

	private void removeIndexDiffCaches(List<File> gitDirs) {
		if (!gitDirs.isEmpty()) {
			IndexDiffCache cache = Activator.getDefault().getIndexDiffCache();
			if (cache != null) {
				for (File f : gitDirs) {
					cache.remove(f);
				}
			}
		}
	}

	/**
	 * Closes the real repository behind the {@link RepositoryHandle}s handed
	 * out by this cache when the handle is garbage collected.
	 */
	private static class Closer extends Thread {

		private final ReferenceQueue<RepositoryHandle> queue;

		public Closer(ReferenceQueue<RepositoryHandle> queue) {
			this.queue = queue;
			setDaemon(true);
			setName("Git Repository Closer"); //$NON-NLS-1$
		}

		@Override
		public void run() {
			try {
				for (;;) {
					Reference<?> stale = queue.remove();
					if (stale instanceof RepositoryReference) {
						closeReference((RepositoryReference) stale);
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		public static void closeReference(RepositoryReference stale) {
			Repository repository = stale.getRepository();
			if (repository != null) {
				repository.close();
			}
			stale.clearRepository();
		}
	}
}
