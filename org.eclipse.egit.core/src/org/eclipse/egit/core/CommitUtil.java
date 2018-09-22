/*******************************************************************************
 * Copyright (C) 2014, Maik Schreiber <blizzy@blizzy.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Maik Schreiber - initial implementation
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

/**
 * Utility class for working with commits.
 */
public class CommitUtil {

	private CommitUtil() {
		// non instantiable utility class
	}

	/**
	 * Sorts commits in parent-first order.
	 *
	 * @param commits
	 *            the commits to sort
	 * @return a new list containing the sorted commits
	 */
	public static List<RevCommit> sortCommits(Collection<RevCommit> commits) {
		Map<RevCommit, RevCommit> parentToChild = new HashMap<>();
		RevCommit firstCommit = null;
		for (RevCommit commit : commits) {
			RevCommit parentCommit = commit.getParent(0);
			parentToChild.put(parentCommit, commit);
			if (!commits.contains(parentCommit))
				firstCommit = commit;
		}

		List<RevCommit> sortedCommits = new ArrayList<>();
		sortedCommits.add(firstCommit);
		RevCommit parentCommit = firstCommit;
		for (;;) {
			RevCommit childCommit = parentToChild.get(parentCommit);
			if (childCommit == null)
				break;
			sortedCommits.add(childCommit);
			parentCommit = childCommit;
		}

		return sortedCommits;
	}

	/**
	 * Returns whether a commit is on the current branch, ie. if it is reachable
	 * from the current HEAD.
	 *
	 * @param commit
	 *            the commit to check
	 * @param repository
	 *            the repository
	 * @return true if the commit is reachable from HEAD
	 * @throws IOException
	 *             if there is an I/O error
	 */
	public static boolean isCommitInCurrentBranch(RevCommit commit,
			Repository repository) throws IOException {
		return areCommitsInCurrentBranch(Collections.singleton(commit),
				repository);
	}

	/**
	 * Returns whether the commits are on the current branch, ie. if they are
	 * reachable from the current HEAD.
	 *
	 * @param commits
	 *            the commits to check
	 * @param repository
	 *            the repository
	 * @return true if the commits are reachable from HEAD
	 * @throws IOException
	 *             if there is an I/O error
	 */
	public static boolean areCommitsInCurrentBranch(
			Collection<RevCommit> commits, Repository repository)
			throws IOException {
		try (RevWalk walk = new RevWalk(repository)) {
			ObjectId headCommitId = repository.resolve(Constants.HEAD);
			RevCommit headCommit = walk.parseCommit(headCommitId);

			for (final RevCommit commit : commits) {
				walk.reset();
				walk.markStart(headCommit);

				RevFilter revFilter = new RevFilter() {
					@Override
					public boolean include(RevWalk walker, RevCommit cmit)
							throws StopWalkException, MissingObjectException,
							IncorrectObjectTypeException, IOException {

						return cmit.equals(commit);
					}

					@Override
					public RevFilter clone() {
						return null;
					}
				};
				walk.setRevFilter(revFilter);

				if (walk.next() == null)
					return false;
			}
			return true;
		}
	}
}
