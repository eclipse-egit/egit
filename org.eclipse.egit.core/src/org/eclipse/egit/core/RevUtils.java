/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 * Utility class for obtaining Rev object instances.
 */
public class RevUtils {

	private RevUtils() {
		// non instanciable utility class
	}

	/**
	 * Finds and returns instance of common ancestor commit for given two
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

		try (RevWalk rw = new RevWalk(repo)) {
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
		}
	}

	/**
	 * @param repository
	 * @param path
	 *            repository-relative path of file with conflicts
	 * @return an object with the interesting commits for this path
	 * @throws IOException
	 */
	public static ConflictCommits getConflictCommits(Repository repository,
			String path) throws IOException {
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit ourCommit;
			RevCommit theirCommit = null;
			walk.setTreeFilter(AndTreeFilter.create(
					PathFilterGroup.createFromStrings(path),
					TreeFilter.ANY_DIFF));

			RevCommit head = walk.parseCommit(repository
					.resolve(Constants.HEAD));
			walk.markStart(head);

			ourCommit = walk.next();

			RepositoryState state = repository.getRepositoryState();
			if (state == RepositoryState.REBASING
					|| state == RepositoryState.CHERRY_PICKING) {
				ObjectId cherryPickHead = repository.readCherryPickHead();
				if (cherryPickHead != null) {
					RevCommit cherryPickCommit = walk.parseCommit(cherryPickHead);
					theirCommit = cherryPickCommit;
				}
			} else if (state == RepositoryState.MERGING) {
				List<ObjectId> mergeHeads = repository.readMergeHeads();
				Assert.isNotNull(mergeHeads);
				if (mergeHeads.size() == 1) {
					ObjectId mergeHead = mergeHeads.get(0);
					RevCommit mergeCommit = walk.parseCommit(mergeHead);
					walk.reset();
					walk.markStart(mergeCommit);
					theirCommit = walk.next();
				}
			}

			return new ConflictCommits(ourCommit, theirCommit);
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

		final int skew = 24 * 60 * 60; // one day clock skew

		try (RevWalk walk = new RevWalk(repo)) {
			RevCommit commit = walk.parseCommit(commitId);
			for (Ref ref : refs) {
				RevCommit refCommit = walk.parseCommit(ref.getObjectId());

				// if commit is in the ref branch, then the tip of ref should be
				// newer than the commit we are looking for. Allow for a large
				// clock skew.
				if (refCommit.getCommitTime() + skew < commit.getCommitTime())
					continue;

				boolean contained = walk.isMergedInto(commit, refCommit);
				if (contained)
					return true;
			}
			walk.dispose();
		}
		return false;
	}

	/**
	 * The interesting commits from ours/theirs for a file in case of a
	 * conflict.
	 */
	public static class ConflictCommits {
		private final RevCommit ourCommit;
		private final RevCommit theirCommit;

		private ConflictCommits(RevCommit ourCommit, RevCommit theirCommit) {
			this.ourCommit = ourCommit;
			this.theirCommit = theirCommit;
		}

		/**
		 * @return the commit from "ours" that last modified a file, or
		 *         {@code null} if none found
		 */
		public RevCommit getOurCommit() {
			return ourCommit;
		}

		/**
		 * @return the commit from "theirs" that last modified a file, or
		 *         {@code null} if none found
		 */
		public RevCommit getTheirCommit() {
			return theirCommit;
		}
	}
}
