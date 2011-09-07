/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.indexdiff;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Repository;

/**
 * This class provides access to a cached {@link IndexDiff} for a given
 * repository
 */
public class IndexDiffCache {

	private Map<Repository, IndexDiffCacheEntry> entries = new HashMap<Repository, IndexDiffCacheEntry>();

	/**
	 * @param repository
	 * @return cache entry
	 */
	public IndexDiffCacheEntry getIndexDiffCacheEntry(Repository repository) {
		IndexDiffCacheEntry entry;
		synchronized(entries) {
			entry = entries.get(repository);
			if (entry != null)
				return entry;
			entry = new IndexDiffCacheEntry(repository);
			entries.put(repository, entry);
		}
		return entry;
	}
}
