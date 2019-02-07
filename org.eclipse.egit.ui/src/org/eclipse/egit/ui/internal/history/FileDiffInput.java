/*******************************************************************************
 * Copyright (C) 2018, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.util.Collection;

import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Data object to use as input for the {@link CommitFileDiffViewer} in the
 * history page.
 */
public class FileDiffInput {

	private final Repository repository;

	private final TreeWalk walk;

	private final RevCommit commit;

	private Collection<String> interestingPaths;

	private final boolean selectMarked;

	/**
	 * Creates a new {@link FileDiffInput}.
	 *
	 * @param repository
	 * @param walk
	 * @param commit
	 * @param interestingPaths
	 * @param selectMarked
	 */
	public FileDiffInput(Repository repository, TreeWalk walk, RevCommit commit,
			Collection<String> interestingPaths, boolean selectMarked) {
		this.commit = commit;
		this.selectMarked = selectMarked;
		this.repository = repository;
		this.walk = walk;
		this.interestingPaths = interestingPaths;
	}

	/**
	 * @return the commit
	 */
	public RevCommit getCommit() {
		return commit;
	}

	/**
	 * @return whether the first marked entry shall be selected
	 */
	public boolean isSelectMarked() {
		return selectMarked;
	}

	/**
	 * @return the {@link Repository}
	 */
	public Repository getRepository() {
		return repository;
	}

	/**
	 * @return the {@link TreeWalk}
	 */
	public TreeWalk getTreeWalk() {
		return walk;
	}

	/**
	 * @return the interesting paths.
	 */
	public @Nullable Collection<String> getInterestingPaths() {
		return interestingPaths;
	}

	/**
	 * Sets the interesting paths.
	 * 
	 * @param interestingPaths
	 *            to set; may be {@code null} or empty
	 */
	public void setInterestingPaths(Collection<String> interestingPaths) {
		this.interestingPaths = interestingPaths;
	}
}
