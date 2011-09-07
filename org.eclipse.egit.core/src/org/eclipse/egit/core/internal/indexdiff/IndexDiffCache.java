package org.eclipse.egit.core.internal.indexdiff;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.lib.Repository;

/**
 *
 *
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
