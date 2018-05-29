/*******************************************************************************
 * Copyright (C) 2011, 2015 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.NotIgnoredFilter;
import org.eclipse.jgit.treewalk.filter.OrTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 * Simple and thin tree cache for git meta data about resources in repository.
 */
class GitSyncCache {

	private final Map<File, GitSyncObjectCache> cache;

	public static GitSyncCache getAllData(GitSynchronizeDataSet gsds,
			IProgressMonitor monitor) {
		Map<GitSynchronizeData, Collection<String>> updateRequests = new HashMap<GitSynchronizeData, Collection<String>>();
		for (GitSynchronizeData data : gsds)
			updateRequests.put(data, Collections.<String> emptyList());
		return getAllData(updateRequests, monitor);
	}

	public static GitSyncCache getAllData(
			Map<GitSynchronizeData, Collection<String>> updateRequests,
			IProgressMonitor monitor) {
		GitSyncCache cache = new GitSyncCache();
		mergeAllDataIntoCache(updateRequests, monitor, cache);
		return cache;
	}

	public static void mergeAllDataIntoCache(
			Map<GitSynchronizeData, Collection<String>> updateRequests,
			IProgressMonitor monitor, GitSyncCache cache) {
		SubMonitor m = SubMonitor.convert(monitor, updateRequests.size());

		for (Entry<GitSynchronizeData, Collection<String>> entry : updateRequests
				.entrySet()) {
			Collection<String> paths = entry.getValue();
			GitSyncCache partialCache = getAllData(entry.getKey(), paths);
			cache.merge(partialCache, new HashSet<String>(paths));
			m.worked(1);
		}

		m.done();
	}

	private static GitSyncCache getAllData(GitSynchronizeData gsd,
			Collection<String> paths) {
		GitSyncCache cache = new GitSyncCache();
		TreeFilter filter = paths.isEmpty() ? null : createPathFilter(paths);

		Repository repo = gsd.getRepository();
		ObjectId baseTree = getTree(gsd.getSrcRevCommit());
		ObjectId remoteTree = getTree(gsd.getDstRevCommit());
		GitSyncObjectCache repoCache = cache.put(repo, baseTree, remoteTree);

		TreeFilter gsdFilter = gsd.getPathFilter();
		if (filter == null)
			loadDataFromGit(gsd, gsdFilter, repoCache);
		else if (gsdFilter == null)
			loadDataFromGit(gsd, filter, repoCache);
		else
			loadDataFromGit(gsd, AndTreeFilter.create(filter, gsdFilter),
					repoCache);
		return cache;
	}

	private static TreeFilter createPathFilter(Collection<String> paths) {
		// do not use PathFilterGroup to create the filter, see bug 362430
		List<TreeFilter> filters = new ArrayList<TreeFilter>(paths.size());
		for (String path : paths) {
			if (path.length() == 0)
				return null;
			filters.add(PathFilter.create(path));
		}
		if (filters.size() == 1)
			return filters.get(0);
		return OrTreeFilter.create(filters);
	}

	static boolean loadDataFromGit(GitSynchronizeData gsd,
			TreeFilter filter, GitSyncObjectCache repoCache) {
		Repository repo = gsd.getRepository();

		try (TreeWalk tw = new TreeWalk(repo)) {
			if (filter != null)
				tw.setFilter(filter);
			// setup local tree
			FileTreeIterator fti = null;
			if (gsd.shouldIncludeLocal()) {
				fti = new FileTreeIterator(repo);
				tw.addTree(fti);
				if (filter != null)
					tw.setFilter(AndTreeFilter.create(filter,
							new NotIgnoredFilter(0)));
				else
					tw.setFilter(new NotIgnoredFilter(0));
			} else if (gsd.getSrcRevCommit() != null)
				tw.addTree(gsd.getSrcRevCommit().getTree());
			else
				tw.addTree(new EmptyTreeIterator());

			// setup base tree
			if (gsd.getCommonAncestorRev() != null)
				tw.addTree(gsd.getCommonAncestorRev().getTree());
			else
				tw.addTree(new EmptyTreeIterator());

			// setup remote tree
			if (gsd.getDstRevCommit() != null)
				tw.addTree(gsd.getDstRevCommit().getTree());
			else
				tw.addTree(new EmptyTreeIterator());

			DirCacheIterator dci = null;
			if (fti != null) {
				dci = new DirCacheIterator(DirCache.read(repo));
				tw.addTree(dci);
				fti.setDirCacheIterator(tw, 3);
			}
			List<ThreeWayDiffEntry> diffEntrys = ThreeWayDiffEntry
					.scan(tw, gsd);
			for (ThreeWayDiffEntry diffEntry : diffEntrys)
				repoCache.addMember(diffEntry);
		} catch (Exception e) {
			Activator.logError(e.getMessage(), e);
			return false;
		}
		return true;
	}

	private static ObjectId getTree(RevCommit commit) {
		if (commit != null)
			return commit.getTree();
		else {
			return ObjectId.zeroId();
		}
	}

	private GitSyncCache() {
		cache = new HashMap<File, GitSyncObjectCache>();
	}

	/**
	 * @param repo
	 *            instance of {@link Repository} for which mapping should be
	 *            obtained
	 * @return instance of {@link GitSyncObjectCache} connected associated with
	 *         given repository or {@code null} when such mapping wasn't found
	 */
	public GitSyncObjectCache get(Repository repo) {
		return cache.get(repo.getDirectory());
	}

	public void merge(GitSyncCache other, Set<String> filterPaths) {
		for (Entry<File, GitSyncObjectCache> entry : other.cache.entrySet()) {
			File key = entry.getKey();
			if (cache.containsKey(key))
				cache.get(key).merge(entry.getValue(), filterPaths);
			else
				cache.put(key, entry.getValue());
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Entry<File, GitSyncObjectCache> entry : cache.entrySet())
			builder.append(entry.getKey().getPath())
					.append(": ").append(entry.getValue()); //$NON-NLS-1$

		return builder.toString();
	}

	/**
	 * Create mapping for given repository and returns object associated with
	 * this repository. Any other mapping will be overwritten.
	 *
	 * @param repo
	 * @param remoteTree
	 * @param baseTree
	 * @return new mapping object associated with given {@link Repository}
	 */
	private GitSyncObjectCache put(Repository repo, ObjectId baseTree,
			ObjectId remoteTree) {
		ThreeWayDiffEntry entry = new ThreeWayDiffEntry();
		entry.baseId = AbbreviatedObjectId.fromObjectId(baseTree);
		entry.remoteId = AbbreviatedObjectId.fromObjectId(remoteTree);
		GitSyncObjectCache objectCache = new GitSyncObjectCache("", entry); //$NON-NLS-1$
		cache.put(repo.getDirectory(), objectCache);

		return objectCache;
	}

}
