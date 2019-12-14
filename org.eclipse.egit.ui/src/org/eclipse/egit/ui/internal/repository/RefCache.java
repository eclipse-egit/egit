/*******************************************************************************
 * Copyright (c) 2019 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;

/**
 * A global cache of {@link Ref}s per {@link Repository}. Used to avoid that the
 * content provider or decorator has to re-read the info all the time from the
 * repository, which would be relatively expensive and incur file system
 * accesses at least for checking file time stamps.
 * <p>
 * The cache is refreshed on {@link org.eclipse.jgit.events.RefsChangedEvent
 * RefsChangedEvents} and {@link org.eclipse.jgit.events.IndexChangedEvent
 * IndexChangedEvents}.
 * </p>
 */
final class RefCache {

	private static final RefCache INSTANCE = new RefCache();

	private final Map<Repository, Map<String, Ref>> branchRefs = new WeakHashMap<>();

	private final Map<Repository, List<Ref>> additionalRefs = new WeakHashMap<>();

	private final Map<Repository, ListenerHandle> refsChangedListeners = new WeakHashMap<>();

	private final Map<Repository, ListenerHandle> indexChangedListeners = new WeakHashMap<>();

	private long refCount;

	private RefCache() {
		// Singleton
	}

	protected synchronized boolean isLoaded(Repository repository) {
		return branchRefs.get(repository) != null;
	}

	protected synchronized Map<String, Ref> byPrefix(Repository repository,
			String prefix) throws IOException {
		Map<String, Ref> allRefs = branchRefs.get(repository);
		if (allRefs == null) {
			allRefs = repository.getRefDatabase().getRefs(RefDatabase.ALL);
			branchRefs.put(repository, allRefs);
			if (refsChangedListeners.get(repository) == null) {
				refsChangedListeners.put(repository, repository
						.getListenerList().addRefsChangedListener(event -> {
							synchronized (this) {
								branchRefs.remove(event.getRepository());
								additionalRefs.remove(event.getRepository());
							}
						}));
			}
		}
		if (prefix.equals(RefDatabase.ALL)) {
			return allRefs;
		}
		Map<String, Ref> filtered = new HashMap<>();
		for (Map.Entry<String, Ref> entry : allRefs.entrySet()) {
			if (entry.getKey().startsWith(prefix)) {
				filtered.put(entry.getKey(), entry.getValue());
			}
		}
		return filtered;
	}

	protected synchronized List<Ref> additional(Repository repository)
			throws IOException {
		List<Ref> result = additionalRefs.get(repository);
		if (result == null) {
			result = repository.getRefDatabase().getAdditionalRefs();
			additionalRefs.put(repository, result);
			if (indexChangedListeners.get(repository) == null) {
				indexChangedListeners.put(repository, repository
						.getListenerList().addIndexChangedListener(event -> {
							synchronized (this) {
								additionalRefs.remove(event.getRepository());
							}
						}));
			}
		}
		return result;
	}

	protected synchronized void unregister() {
		if (refCount == 0) {
			return;
		}
		if (--refCount == 0) {
			refsChangedListeners.values().forEach(ListenerHandle::remove);
			refsChangedListeners.clear();
			indexChangedListeners.values().forEach(ListenerHandle::remove);
			indexChangedListeners.clear();
			branchRefs.clear();
		}
	}

	private synchronized Cache register() {
		refCount++;
		return new CacheAccessor();
	}

	public static Cache get() {
		return INSTANCE.register();
	}

	static interface Cache {

		boolean isLoaded(Repository repository);

		default Ref exact(Repository repository, String fullName)
				throws IOException {
			return byPrefix(repository, RefDatabase.ALL).get(fullName);
		}

		Map<String, Ref> byPrefix(Repository repository, String prefix)
				throws IOException;

		List<Ref> additional(Repository repository) throws IOException;

		default Ref findAdditional(Repository repository, String name)
				throws IOException {
			Ref ref = exact(repository, name);
			if (ref != null) {
				return ref;
			}
			for (Ref additional : additional(repository)) {
				if (additional.getName().equals(name)) {
					return additional;
				}
			}
			return null;

		}

		void dispose();
	}

	static class CacheAccessor implements Cache {

		private boolean disposed;

		@Override
		public boolean isLoaded(Repository repository) {
			return INSTANCE.isLoaded(repository);
		}

		@Override
		public Map<String, Ref> byPrefix(Repository repository, String prefix)
				throws IOException {
			if (disposed) {
				return Collections.emptyMap();
			}
			return INSTANCE.byPrefix(repository, prefix);
		}

		@Override
		public List<Ref> additional(Repository repository) throws IOException {
			if (disposed) {
				return Collections.emptyList();
			}
			return INSTANCE.additional(repository);
		}

		@Override
		public void dispose() {
			if (!disposed) {
				disposed = true;
				INSTANCE.unregister();
			}
		}
	}
}
