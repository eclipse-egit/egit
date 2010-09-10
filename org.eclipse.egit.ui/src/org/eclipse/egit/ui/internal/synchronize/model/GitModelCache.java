/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.compare.structuremergeviewer.Differencer.RIGHT;
import static org.eclipse.jgit.lib.FileMode.MISSING;
import static org.eclipse.jgit.lib.FileMode.TREE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Git cache representation in EGit Change Set
 */
public class GitModelCache extends GitModelObjectContainer {

	private final Map<String, GitModelCacheTree> cacheTreeMap;

	/**
	 * Constructs model node that represents current status of Git cache.
	 *
	 * @param parent
	 * @param baseCommit
	 * @throws IOException
	 */
	public GitModelCache(GitModelObject parent, RevCommit baseCommit)
			throws IOException {
		super(parent, baseCommit, RIGHT);
		cacheTreeMap = new HashMap<String, GitModelCacheTree>();
	}

	@Override
	public String getName() {
		return UIText.GitModelIndex_index;
	}

	protected GitModelObject[] getChildrenImpl() {
		List<GitModelObject> result = new ArrayList<GitModelObject>();

		try {
			TreeWalk tw = createTreeWalk();
			tw.setRecursive(true);
			tw.addTree(baseCommit.getTree());

			Repository repo = getRepository();
			DirCache index = repo.readDirCache();
			ObjectId headId = repo.getRef(Constants.HEAD).getObjectId();
			int cacheNth = tw.addTree(new DirCacheIterator(index));
			int repoNth = tw.addTree(new RevWalk(repo).parseTree(headId));

			while (tw.next()) {
				GitModelObject entry = extractFromCache(tw, repoNth, cacheNth);
				if (entry == null)
					continue;

				result.add(entry);
			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		return result.toArray(new GitModelObject[result.size()]);
	}

	private GitModelObject extractFromCache(TreeWalk tw, int repoNth, int cacheNth)
			throws IOException {
		DirCacheIterator cacheIterator = tw.getTree(cacheNth,
				DirCacheIterator.class);
		if (cacheIterator == null)
			return null;

		DirCacheEntry cacheEntry = cacheIterator.getDirCacheEntry();
		if (cacheEntry == null)
			return null;

		if (shouldIncludeEntry(tw, cacheNth, repoNth)) {
			String path = new String(tw.getRawPath());
			ObjectId repoId = tw.getObjectId(repoNth);
			ObjectId cacheId = tw.getObjectId(cacheNth);

			if (path.split("/").length > 1) //$NON-NLS-1$
				return handleCacheTree(repoId, cacheId, path);

			return new GitModelBlob(this, remoteCommit, repoId, repoId, cacheId, path);
		}

		return null;
	}

	private boolean shouldIncludeEntry(TreeWalk tw, int cacheNth, int repoNth) {
		final int mHead = tw.getRawMode(repoNth);
		final int mCache = tw.getRawMode(cacheNth);

		return mHead == MISSING.getBits() // initial add to cache
				|| mCache == MISSING.getBits() // removed from cache
				|| (mHead != mCache || (mCache != TREE.getBits() && !tw
						.idEqual(repoNth, cacheNth))); // modified
	}

	private GitModelObject handleCacheTree(ObjectId repoId, ObjectId cacheId,
			String path) throws IOException {
		String pathKey = path.split("/")[0]; //$NON-NLS-1$
		GitModelCacheTree cacheTree = cacheTreeMap.get(pathKey);
		if (cacheTree == null) {
			cacheTree = new GitModelCacheTree(this, remoteCommit, repoId,
					cacheId, pathKey);
			cacheTreeMap.put(pathKey, cacheTree);
		}

		cacheTree.addChild(repoId, cacheId, path.substring(path.indexOf('/') + 1));

		return cacheTree;
	}

}
