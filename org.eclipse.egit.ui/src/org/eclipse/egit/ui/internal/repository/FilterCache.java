/*******************************************************************************
 * Copyright (C) 2020, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.egit.ui.internal.repository.tree.FilterableNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;

/**
 * Cache for filters in the {@link RepositoriesView}.
 */
class FilterCache {

	/** The singleton instance. */
	public static final FilterCache INSTANCE = new FilterCache();

	private final Map<File, Map<RepositoryTreeNodeType, String>> cache = new HashMap<>();

	private FilterCache() {
		// Singleton
	}

	/**
	 * Records a filter for a node.
	 *
	 * @param node
	 *            to record the filter for
	 * @param filter
	 *            to record
	 */
	public void set(FilterableNode node, String filter) {
		cache.computeIfAbsent(node.getRepository().getDirectory(),
				k -> new HashMap<>()).put(node.getType(), filter);
		node.setFilter(filter);
	}

	/**
	 * Retrieves the cached filter, if any, for a node.
	 *
	 * @param node
	 *            to get the filter of
	 * @return the filter, or {@code null} if none cached
	 */
	public String get(FilterableNode node) {
		Map<RepositoryTreeNodeType, String> filters = cache
				.get(node.getRepository().getDirectory());
		return filters == null ? null : filters.get(node.getType());
	}

	/** Clears the cache. */
	public void clear() {
		cache.clear();
	}

	/**
	 * Clears all cached entries from repositories not in the given collection.
	 *
	 * @param gitDirs
	 *            collection of git directories of repositories for which to
	 *            keep the cached filters
	 */
	public void keepOnly(Collection<File> gitDirs) {
		cache.keySet().retainAll(gitDirs);
	}
}
