/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.egit.core.synchronize.GitCommitsModelCache.RIGHT;
import static org.eclipse.egit.core.synchronize.GitCommitsModelCache.calculateAndSetChangeKind;
import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.FileMode.MISSING;
import static org.eclipse.jgit.lib.FileMode.TREE;
import static org.eclipse.jgit.lib.ObjectId.zeroId;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.MutableObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Builds list of changes in git staging area.
 */
public class StagedChangeCache {

	/**
	 * @param repo
	 *            repository which should be scanned
	 * @return list of changes in git staging area
	 */
	public static Map<String, Change> build(Repository repo) {
		try (TreeWalk tw = new TreeWalk(repo)) {
			tw.addTree(new DirCacheIterator(repo.readDirCache()));
			ObjectId headId = repo.resolve(HEAD);
			RevCommit headCommit = null;
			if (headId != null) {
				try (RevWalk rw = new RevWalk(repo)) {
					headCommit = rw.parseCommit(headId);
				}
			}

			AbbreviatedObjectId commitId;
			if (headCommit != null) {
				tw.addTree(headCommit.getTree());
				commitId = AbbreviatedObjectId.fromObjectId(headCommit);
			} else {
				tw.addTree(new EmptyTreeIterator());
				commitId =AbbreviatedObjectId.fromObjectId(zeroId());
			}

			tw.setRecursive(true);
			headCommit = null;

			MutableObjectId idBuf = new MutableObjectId();
			Map<String, Change> result = new HashMap<>();
			while(tw.next()) {
				if (!shouldIncludeEntry(tw))
					continue;

				Change change = new Change();
				change.name = tw.getNameString();
				change.remoteCommitId = commitId;

				tw.getObjectId(idBuf, 0);
				change.objectId = AbbreviatedObjectId.fromObjectId(idBuf);
				tw.getObjectId(idBuf, 1);
				change.remoteObjectId = AbbreviatedObjectId.fromObjectId(idBuf);

				calculateAndSetChangeKind(RIGHT, change);

				result.put(tw.getPathString(), change);
			}

			return result;
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
			return new HashMap<>(0);
		}
	}

	private static boolean shouldIncludeEntry(TreeWalk tw) {
		final int mHead = tw.getRawMode(1);
		final int mCache = tw.getRawMode(0);

		return mHead == MISSING.getBits() // initial add to cache
				|| mCache == MISSING.getBits() // removed from cache
				|| (mHead != mCache || (mCache != TREE.getBits() && !tw
						.idEqual(1, 0))); // modified
	}

}
