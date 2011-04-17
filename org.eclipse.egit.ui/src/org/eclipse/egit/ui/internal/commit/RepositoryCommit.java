/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.io.IOException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 * Class that encapsulates a particular {@link Repository} instance and
 * {@link RevCommit} instance.
 *
 * This class also provides access to the {@link FileDiff} objects introduced by
 * the commit.
 *
 * @author Kevin Sawicki (kevin@github.com)
 */
public class RepositoryCommit {

	/**
	 * NAME_LENGTH
	 */
	public static final int NAME_LENGTH = 7;

	private Repository repository;

	private RevCommit commit;

	private FileDiff[] diffs;

	/**
	 * Create a repository commit
	 *
	 * @param repository
	 * @param commit
	 */
	public RepositoryCommit(Repository repository, RevCommit commit) {
		Assert.isNotNull(repository, "Repository cannot be null"); //$NON-NLS-1$
		Assert.isNotNull(commit, "Commit cannot be null"); //$NON-NLS-1$
		this.repository = repository;
		this.commit = commit;
	}

	/**
	 * Abbreviate commit id to {@link #NAME_LENGTH} size.
	 *
	 * @return abbreviated commit id
	 */
	public String abbreviate() {
		return this.commit.abbreviate(NAME_LENGTH).name();
	}

	/**
	 * Get repository name
	 *
	 * @return repo name
	 */
	public String getRepositoryName() {
		return this.repository.getDirectory().getParentFile().getName();
	}

	/**
	 * Get repository
	 *
	 * @return repository
	 */
	public Repository getRepository() {
		return this.repository;
	}

	/**
	 * Get rev commit
	 *
	 * @return rev commit
	 */
	public RevCommit getRevCommit() {
		return this.commit;
	}

	/**
	 * Get the file diffs introduced by this commit
	 *
	 * @return non-null but possibly empty array of {@link FileDiff} objects
	 */
	public FileDiff[] getDiffs() {
		if (this.diffs == null) {
			TreeWalk walk = new TreeWalk(this.repository);
			walk.setRecursive(true);
			walk.setFilter(TreeFilter.ANY_DIFF);
			FileDiff[] computed = null;
			try {
				computed = FileDiff.compute(walk, this.commit);
			} catch (IOException e) {
				computed = new FileDiff[0];
			} finally {
				walk.release();
			}
			this.diffs = computed;
		}
		return this.diffs;
	}

}
