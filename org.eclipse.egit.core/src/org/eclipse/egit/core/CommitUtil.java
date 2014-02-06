/*******************************************************************************
 * Copyright (C) 2014, Maik Schreiber <blizzy@blizzy.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Utility class for working with commits.
 */
public class CommitUtil {

	private CommitUtil() {
		// non instanciable utility class
	}

	/**
	 * Sorts commits in parent-first order.
	 *
	 * @param commits
	 *            the commits to sort
	 * @return a new list containing the sorted commits
	 */
	public static List<RevCommit> sortCommits(Collection<RevCommit> commits) {
		Map<RevCommit, RevCommit> parentToChild = new HashMap<RevCommit, RevCommit>();
		RevCommit firstCommit = null;
		for (RevCommit commit : commits) {
			RevCommit parentCommit = commit.getParent(0);
			parentToChild.put(parentCommit, commit);
			if (!commits.contains(parentCommit))
				firstCommit = commit;
		}

		List<RevCommit> sortedCommits = new ArrayList<RevCommit>();
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
}
