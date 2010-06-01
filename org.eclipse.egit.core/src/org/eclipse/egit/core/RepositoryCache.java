/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.lib.Repository;

/**
 * Central cache for Repository instances
 *
 */
public class RepositoryCache {
	private final Map<File, Reference<Repository>> repositoryCache = new HashMap<File, Reference<Repository>>();

	RepositoryCache() {
		// package private constructor
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
		Reference<Repository> r = repositoryCache.get(gitDir);
		Repository d = r != null ? r.get() : null;
		if (d == null) {
			d = new Repository(gitDir);
			repositoryCache.put(gitDir, new WeakReference<Repository>(d));
		}
		prune(repositoryCache);
		return d;
	}

	/**
	 * @return all Repository instances contained in the cache
	 */
	public synchronized Repository[] getAllReposiotries() {
		List<Repository> result = new ArrayList<Repository>();
		Collection<Reference<Repository>> values = repositoryCache.values();
		for(Reference<Repository> ref:values) {
			Repository repo = ref.get();
			if(repo!=null)
				result.add(repo);
		}
		return result.toArray(new Repository[result.size()]);
	}

	private static <K, V> void prune(Map<K, Reference<V>> map) {
		for (final Iterator<Map.Entry<K, Reference<V>>> i = map.entrySet()
				.iterator(); i.hasNext();) {
			if (i.next().getValue().get() == null)
				i.remove();
		}
	}

}
