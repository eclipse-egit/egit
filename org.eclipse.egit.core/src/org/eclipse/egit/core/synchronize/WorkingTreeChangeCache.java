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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.MutableObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.IndexDiffFilter;

/**
 * Builds list of working tree changes.
 */
public class WorkingTreeChangeCache {

	/**
	 * @param repo
	 *            with should be scanned
	 * @return list of changes in working tree
	 */
	public static Map<String, Change> build(Repository repo) {
		try (TreeWalk tw = new TreeWalk(repo)) {
			int fileNth = tw.addTree(new FileTreeIterator(repo));
			int cacheNth = tw.addTree(new DirCacheIterator(repo.readDirCache()));
			tw.setFilter(new IndexDiffFilter(cacheNth, fileNth));
			tw.setRecursive(true);

			Map<String, Change> result = new HashMap<>();
			MutableObjectId idBuf = new MutableObjectId();
			while (tw.next()) {
				Change change = new Change();
				change.name = tw.getNameString();
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

}
