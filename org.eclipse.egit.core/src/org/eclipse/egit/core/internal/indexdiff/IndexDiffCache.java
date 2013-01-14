/*******************************************************************************
 * Copyright (C) 2011, 2013 Jens Baumgart <jens.baumgart@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.indexdiff;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Repository;

/**
 * This class provides access to a cached {@link IndexDiff} for a given
 * repository
 */
public class IndexDiffCache {

	private Map<Repository, IndexDiffCacheEntry> entries = new HashMap<Repository, IndexDiffCacheEntry>();

	private Set<IndexDiffChangedListener> listeners = new HashSet<IndexDiffChangedListener>();

	private IndexDiffChangedListener globalListener;

	/**
	 * constructor
	 */
	public IndexDiffCache() {
		createGlobalListener();
	}

	/**
	 * @param repository
	 * @return cache entry
	 */
	public IndexDiffCacheEntry getIndexDiffCacheEntry(Repository repository) {
		IndexDiffCacheEntry entry;
		synchronized (entries) {
			entry = entries.get(repository);
			if (entry != null)
				return entry;
			if (repository.isBare())
				return null;
			entry = new IndexDiffCacheEntry(repository);
			entries.put(repository, entry);
		}
		entry.addIndexDiffChangedListener(globalListener);
		return entry;
	}

	/**
	 * Adds a listener for IndexDiff changes. Note that only caches are
	 * available for those repositories for which getIndexDiffCacheEntry was
	 * called.
	 *
	 * @param listener
	 */
	public void addIndexDiffChangedListener(IndexDiffChangedListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	/**
	 * @param listener
	 */
	public void removeIndexDiffChangedListener(IndexDiffChangedListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	private void createGlobalListener() {
		globalListener = new IndexDiffChangedListener() {
			public void indexDiffChanged(Repository repository,
					IndexDiffData indexDiffData) {
				notifyListeners(repository, indexDiffData);
			}
		};
	}

	private void notifyListeners(Repository repository,
			IndexDiffData indexDiffData) {
		IndexDiffChangedListener[] tmpListeners;
		synchronized (listeners) {
			tmpListeners = listeners
					.toArray(new IndexDiffChangedListener[listeners.size()]);
		}
		for (int i = 0; i < tmpListeners.length; i++) {
			try {
				tmpListeners[i].indexDiffChanged(repository, indexDiffData);
			} catch (RuntimeException e) {
				Activator.logError(
						"Exception occured in an IndexDiffChangedListener", e); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Used by {@link Activator}
	 */
	public void dispose() {
		for (IndexDiffCacheEntry entry : entries.values())
			entry.dispose();
		Job.getJobManager().cancel(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
	}

}
