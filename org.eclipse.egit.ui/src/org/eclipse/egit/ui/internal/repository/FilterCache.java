/*******************************************************************************
 * Copyright (C) 2020, Thomas Wolf <thomas.wolf@paranor.ch> and others.
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
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.InvalidPathException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.FilterableNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

/**
 * Cache for filters in the {@link RepositoriesView}.
 */
class FilterCache {

	/** The singleton instance. */
	public static final FilterCache INSTANCE = new FilterCache();

	private static final String TAG_REPO = "repo"; //$NON-NLS-1$

	private static final String ATTR_DIR = "dir"; //$NON-NLS-1$

	private static final String TAG_FILTER = "filter"; //$NON-NLS-1$

	private static final String ATTR_NODE = "tag"; //$NON-NLS-1$

	private static final String ATTR_FILTER = "filter"; //$NON-NLS-1$

	private final Map<File, Map<RepositoryTreeNodeType, String>> cache = new ConcurrentHashMap<>();

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
				k -> new ConcurrentHashMap<>()).put(node.getType(), filter);
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

	/**
	 * Loads the cache from the EGit UI preferences.
	 */
	public void load() {
		String data = Activator.getDefault().getPreferenceStore()
				.getString(UIPreferences.REPOSITORIES_VIEW_FILTERS);
		if (StringUtils.isEmptyOrNull(data)) {
			return;
		}
		XMLMemento memento;
		try (StringReader reader = new StringReader(data)) {
			memento = XMLMemento.createReadRoot(reader);
		} catch (WorkbenchException e) {
			Activator.logError(UIText.RepositoriesView_FilterErrorRead, e);
			return;
		}
		RepositoryUtil util = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryUtil();
		for (IMemento repo : memento.getChildren(TAG_REPO)) {
			String repoId = repo.getString(ATTR_DIR);
			if (repoId == null || repoId.isEmpty()) {
				continue;
			}
			try {
				repoId = util.getAbsoluteRepositoryPath(repoId);
			} catch (InvalidPathException e) {
				continue; // Skip invalid paths
			}
			Map<RepositoryTreeNodeType, String> values = new ConcurrentHashMap<>();
			for (IMemento entry : repo.getChildren(TAG_FILTER)) {
				String tag = entry.getString(ATTR_NODE);
				if (StringUtils.isEmptyOrNull(tag)) {
					continue;
				}
				String filter = entry.getString(ATTR_FILTER);
				if (StringUtils.isEmptyOrNull(filter)) {
					continue;
				}
				RepositoryTreeNodeType type = null;
				try {
					type = RepositoryTreeNodeType.valueOf(tag);
				} catch (RuntimeException e) {
					continue; // Just skip unknown values
				}
				values.put(type, filter);
			}
			if (!values.isEmpty()) {
				cache.put(new File(repoId), values);
			}
		}
	}

	/**
	 * Persists the cache in the EGit UI preferences.
	 */
	public void save() {
		XMLMemento memento = XMLMemento
				.createWriteRoot(getClass().getSimpleName());
		RepositoryUtil util = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryUtil();
		cache.entrySet().stream().forEach(entry -> {
			Map<RepositoryTreeNodeType, String> values = entry.getValue();
			if (values.isEmpty()) {
				return;
			}
			IMemento repo = memento.createChild(TAG_REPO);
			String path = entry.getKey().getAbsolutePath();
			assert path != null;
			repo.putString(ATTR_DIR, util.relativizeToWorkspace(path));
			values.entrySet().stream().forEach(e -> {
				String filter = e.getValue();
				if (StringUtils.isEmptyOrNull(filter)) {
					return;
				}
				IMemento child = repo.createChild(TAG_FILTER);
				child.putString(ATTR_NODE, e.getKey().name());
				child.putString(ATTR_FILTER, filter);
			});
		});
		try (StringWriter writer = new StringWriter()) {
			memento.save(writer);
			Activator.getDefault().getPreferenceStore().setValue(
					UIPreferences.REPOSITORIES_VIEW_FILTERS, writer.toString());
		} catch (IOException e) {
			Activator.logError(UIText.RepositoriesView_FilterErrorSave, e);
		}
	}
}
