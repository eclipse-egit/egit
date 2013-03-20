/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

/**
 * Utility class for obtaining Rev object instances.
 */
public class RevUtils {

	private RevUtils() {
		// non instanciable utility class
	}

	/**
	 * Finds and returns instance of common ancestor commit for given to
	 * commit's
	 *
	 * @param repo repository in which common ancestor should be searched, cannot be null
	 * @param commit1 left commit id, cannot be null
	 * @param commit2 right commit id, cannot be null
	 * @return common ancestor for commit1 and commit2 parameters
	 * @throws IOException
	 */
	public static RevCommit getCommonAncestor(Repository repo,
			AnyObjectId commit1, AnyObjectId commit2) throws IOException {
		Assert.isNotNull(repo);
		Assert.isNotNull(commit1);
		Assert.isNotNull(commit2);

		RevWalk rw = new RevWalk(repo);
		try {
			rw.setRetainBody(false);
			rw.setRevFilter(RevFilter.MERGE_BASE);

			RevCommit srcRev = rw.lookupCommit(commit1);
			RevCommit dstRev = rw.lookupCommit(commit2);

			rw.markStart(dstRev);
			rw.markStart(srcRev);

			RevCommit result = rw.next();
			if (result != null) {
				rw.parseBody(result);
				return result;
			} else {
				return null;
			}
		} finally {
			rw.release();
		}
	}

	/**
	 * Check if commit is contained in any of the passed refs.
	 *
	 * @param repo
	 *            the repo the commit is in
	 * @param commitId
	 *            the commit ID to search for
	 * @param refs
	 *            the refs to check
	 * @return true if the commit is contained, false otherwise
	 * @throws IOException
	 */
	public static boolean isContainedInAnyRef(Repository repo,
			ObjectId commitId, Collection<Ref> refs) throws IOException {
		// It's likely that we don't have to walk commits at all, so
		// check refs directly first.
		for (Ref ref : refs)
			if (commitId.equals(ref.getObjectId()))
				return true;

		RevWalk walk = new RevWalk(repo);
		try {
			RevCommit commit = walk.parseCommit(commitId);
			for (Ref ref : refs) {
				RevCommit refCommit = walk.parseCommit(ref.getObjectId());
				boolean contained = walk.isMergedInto(commit, refCommit);
				if (contained)
					return true;
			}
		} finally {
			walk.dispose();
		}
		return false;
	}

}
