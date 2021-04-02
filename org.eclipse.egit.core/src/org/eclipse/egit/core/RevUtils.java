/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;

import org.eclipse.core.runtime.Assert;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.RawParseUtils;

/**
 * Utility class for obtaining Rev object instances.
 */
public class RevUtils {

	private RevUtils() {
		// non instantiable utility class
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
	 * Get the 'theirs' commit in a conflict state.
	 *
	 * @param repository
	 *            to get the commits from
	 * @return the commit
	 * @throws IOException
	 *             if the commit cannot be determined
	 */
	public static RevCommit getTheirs(Repository repository)
			throws IOException {
		try (RevWalk walk = new RevWalk(repository)) {
			return getTheirs(repository, walk);
		}
	}

	/**
	 * Get the 'theirs' commit in a conflict state.
	 *
	 * @param repository
	 *            to get the commits from
	 * @param walk
	 *            to use for parsing commits
	 * @return the commit
	 * @throws IOException
	 *             if the commit cannot be determined
	 */
	public static RevCommit getTheirs(Repository repository, RevWalk walk)
			throws IOException {
		String target;
		switch (repository.getRepositoryState()) {
		case MERGING:
			target = Constants.MERGE_HEAD;
			break;
		case CHERRY_PICKING:
			target = Constants.CHERRY_PICK_HEAD;
			break;
		case REBASING_INTERACTIVE:
			target = readFile(repository.getDirectory(),
					RebaseCommand.REBASE_MERGE + File.separatorChar
							+ RebaseCommand.STOPPED_SHA);
			break;
		case REVERTING:
			target = Constants.REVERT_HEAD;
			break;
		default:
			target = Constants.ORIG_HEAD;
			break;
		}
		ObjectId theirs = repository.resolve(target);
		if (theirs == null) {
			throw new IOException(MessageFormat.format(
					CoreText.ValidationUtils_CanNotResolveRefMessage, target));
		}
		return walk.parseCommit(theirs);
	}

	private static String readFile(File directory, String fileName)
			throws IOException {
		byte[] content = IO.readFully(new File(directory, fileName));
		// strip off the last LF
		int end = content.length;
		while (0 < end && content[end - 1] == '\n') {
			end--;
		}
		return RawParseUtils.decode(content, 0, end);
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
