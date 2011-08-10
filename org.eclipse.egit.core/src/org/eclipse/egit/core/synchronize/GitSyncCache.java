/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.NotIgnoredFilter;

/**
 * Simple and thin tree cache for git meta data about resources in repository.
 */
class GitSyncCache {

	private final Map<File, GitSyncObjectCache> cache;

	public static GitSyncCache getAllData(GitSynchronizeDataSet gsds,
			IProgressMonitor monitor) {
		GitSyncCache cache = new GitSyncCache();
		for (GitSynchronizeData gsd : gsds) {
			Repository repo = gsd.getRepository();
			GitSyncObjectCache repoCache = cache.put(repo);

			loadDataFromGit(gsd, repoCache);
			monitor.worked(1);
		}

		return cache;
	}

	private static void loadDataFromGit(GitSynchronizeData gsd,
			GitSyncObjectCache repoCache) {
		Repository repo = gsd.getRepository();
		TreeWalk tw = new TreeWalk(repo);
		if (gsd.getPathFilter() != null)
			tw.setFilter(gsd.getPathFilter());

		try {
			// setup local tree
			if (gsd.shouldIncludeLocal()) {
				tw.addTree(new FileTreeIterator(repo));
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

			List<ThreeWayDiffEntry> diffEntrys = ThreeWayDiffEntry.scan(tw/*, true*/);
			for (ThreeWayDiffEntry diffEntry : diffEntrys)
				repoCache.addMember(diffEntry);
		} catch (Exception e) {
			Activator.logError(e.getMessage(), e);
		}
	}

	private GitSyncCache() {
		cache = new HashMap<File, GitSyncObjectCache>();
	}

	/**
	 * @param repo
	 *            instance of {@link Repository} for with mapping should be
	 *            obtained
	 * @return instance of {@link GitSyncObjectCache} connected associated with
	 *         given repository or {@code null} when such mapping wasn't found
	 */
	public GitSyncObjectCache get(Repository repo) {
		return cache.get(repo.getDirectory());
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
	 * @return new mapping object associated with given {@link Repository}
	 */
	private GitSyncObjectCache put(Repository repo) {
		GitSyncObjectCache objectCache = new GitSyncObjectCache();
		// use hash code of repository directory to reduce cache size
		cache.put(repo.getDirectory(), objectCache);

		return objectCache;
	}

}
