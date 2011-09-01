/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.compare.structuremergeviewer.Differencer.LEFT;
import static org.eclipse.egit.core.synchronize.CheckedInCommitsCache.calculateAndSetChangeKind;
import static org.eclipse.jgit.lib.Constants.HEAD;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.egit.core.synchronize.CheckedInCommitsCache.Change;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.MutableObjectId;
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
	 *            repository with should be scanned
	 * @return list of changes in git staging area
	 * @throws IOException
	 */
	public static Map<String, Change> build(Repository repo) throws IOException {
		TreeWalk tw = new TreeWalk(repo);
		tw.addTree(new DirCacheIterator(repo.readDirCache()));
		RevCommit headCommit = new RevWalk(repo)
				.parseCommit(repo.resolve(HEAD));
		if (headCommit != null)
			tw.addTree(headCommit.getTree());
		else
			tw.addTree(new EmptyTreeIterator());

		tw.setRecursive(true);
		AbbreviatedObjectId commitId = AbbreviatedObjectId
				.fromObjectId(headCommit);
		headCommit = null;

		MutableObjectId idBuf = new MutableObjectId();
		Map<String, Change> result = new HashMap<String, Change>();
		while(tw.next()) {
			Change change = new Change();
			change.name = tw.getNameString();
			change.remoteCommitId = commitId;

			tw.getObjectId(idBuf, 0);
			change.objectId = AbbreviatedObjectId.fromObjectId(idBuf);
			tw.getObjectId(idBuf, 1);
			change.remoteObjectId = AbbreviatedObjectId.fromObjectId(idBuf);

			calculateAndSetChangeKind(LEFT, change);

			result.put(tw.getPathString(), change);
		}
		tw.release();

		return result;
	}

}
