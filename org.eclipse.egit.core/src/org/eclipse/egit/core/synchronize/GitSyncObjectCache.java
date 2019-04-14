/*******************************************************************************
 * Copyright (C) 2011, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.synchronize.ThreeWayDiffEntry.ChangeType;
import org.eclipse.osgi.util.NLS;

/**
 * Thin cache object. It contains list of object members, object name and
 * {@link ThreeWayDiffEntry} data.
 */
class GitSyncObjectCache {

	private final String name;

	private ThreeWayDiffEntry diffEntry;

	private Map<String, GitSyncObjectCache> members;

	/**
	 * Creates node and leaf element
	 *
	 * @param name
	 *            entry name
	 * @param diffEntry
	 *            entry meta data
	 */
	GitSyncObjectCache(String name, ThreeWayDiffEntry diffEntry) {
		this.name = name;
		this.diffEntry = diffEntry;
	}

	/**
	 * @return name of this object
	 */
	public String getName() {
		return name;
	}

	/**
	 *
	 * @return entry meta data
	 */
	public ThreeWayDiffEntry getDiffEntry() {
		return diffEntry;
	}

	/**
	 * Store given {@code entry} in cache. It assumes that parent of
	 * {@code entry} is already in cache, if not {@link RuntimeException} will
	 * be thrown.
	 *
	 * @param entry
	 *            that should be stored in cache
	 * @throws RuntimeException
	 *             when cannot find parent of given {@code entry} in cache
	 */
	public void addMember(ThreeWayDiffEntry entry) {
		String memberPath = entry.getPath();

		if (members == null)
			members = new HashMap<>();

		int start = -1;
		Map<String, GitSyncObjectCache> parent = members;
		int separatorIdx = memberPath.indexOf("/"); //$NON-NLS-1$
		while (separatorIdx > 0) {
			String key = memberPath.substring(start + 1, separatorIdx);
			GitSyncObjectCache cacheObject = parent.get(key);
			if (cacheObject == null)
				throw new RuntimeException(NLS.bind(
						CoreText.GitSyncObjectCache_noData, key));

			start = separatorIdx;
			separatorIdx = memberPath.indexOf("/", separatorIdx + 1); //$NON-NLS-1$
			if (cacheObject.members == null)
				cacheObject.members = new HashMap<>();

			parent = cacheObject.members;
		}

		String newName;
		if (start > 0)
			newName = memberPath.substring(start + 1);
		else
			newName = memberPath;

		GitSyncObjectCache obj = new GitSyncObjectCache(newName, entry);
		parent.put(newName, obj);
	}

	/**
	 * @param childPath
	 *            repository relative path of entry that should be obtained
	 * @return cached object, or {@code null} when cache doen't contain object
	 *         for given path
	 */
	public GitSyncObjectCache get(String childPath) {
		if (childPath.length() == 0)
			return this;
		if (childPath
				.substring(childPath.lastIndexOf("/") + 1, childPath.length()).equals(name)) //$NON-NLS-1$
			return this;
		if (members == null)
			return null;

		int start = -1;
		Map<String, GitSyncObjectCache> parent = members;
		int separatorIdx = childPath.indexOf("/"); //$NON-NLS-1$
		while (separatorIdx > 0) {
			String key = childPath.substring(start + 1, separatorIdx);

			GitSyncObjectCache childObject = parent.get(key);
			if (childObject == null)
				return null;

			start = separatorIdx;
			separatorIdx = childPath.indexOf("/", separatorIdx + 1); //$NON-NLS-1$
			parent = childObject.members;
			if (parent == null)
				return null;
		}

		return parent.get(childPath.subSequence(
				childPath.lastIndexOf("/") + 1, childPath.length())); //$NON-NLS-1$
	}

	/**
	 * @return number of cached members
	 */
	public int membersCount() {
		return members != null ? members.size() : 0;
	}

	/**
	 * @return list of all cached members or {@code null} when there this object
	 *         doesn't contain members
	 */
	public Collection<GitSyncObjectCache> members() {
		return members != null ? members.values() : null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("entry: ").append(diffEntry).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		if (members != null) {
			builder.append("members: "); //$NON-NLS-1$
			for (GitSyncObjectCache obj : members.values())
				builder.append(obj.toString()).append("\n"); //$NON-NLS-1$
		}

		return builder.toString();
	}

	void merge(GitSyncObjectCache other, Set<String> filterPaths) {
		if (other.members != null) {
			if (members == null)
				members = new HashMap<>();

			for (Entry<String, GitSyncObjectCache> entry : members.entrySet()) {
				String key = entry.getKey();
				if (!other.members.containsKey(key)) {
					GitSyncObjectCache obj = entry.getValue();
					String entryPath = obj.getDiffEntry().getPath();
					if (containsPathOrParent(filterPaths, entryPath))
						obj.getDiffEntry().changeType = ChangeType.IN_SYNC;
				}
			}

			for (Entry<String, GitSyncObjectCache> entry : other.members
					.entrySet()) {
				String key = entry.getKey();
				GitSyncObjectCache obj = entry.getValue();
				if (members.containsKey(key)) {
					members.get(key).merge(obj, filterPaths);
				} else {
					members.put(key, obj);
				}
			}
		} else if (members != null) {
			for (GitSyncObjectCache obj : members.values()) {
				String entryPath = obj.getDiffEntry().getPath();
				if (containsPathOrParent(filterPaths, entryPath))
					obj.getDiffEntry().changeType = ChangeType.IN_SYNC;
			}
		} else {
			// we are on a leaf entry, use the newer diff entry (need to update
			// changeType, direction and other fields)
			diffEntry = other.diffEntry;
		}
	}

	private static boolean containsPathOrParent(Set<String> filterPaths,
			String pathToTest) {
		if (filterPaths.contains(pathToTest))
			return true;
		// "" is the root directory of the repository, so it matches everything
		if (filterPaths.contains("")) //$NON-NLS-1$
			return true;
		String path = pathToTest;
		int lastSlash = path.lastIndexOf('/');
		while (lastSlash != -1) {
			path = path.substring(0, lastSlash);
			if (filterPaths.contains(path))
				return true;
			lastSlash = path.lastIndexOf('/');
		}
		return false;
	}
}
