/*******************************************************************************
 * Copyright (c) 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;

/**
 *
 */
public class RebaseInteractiveInput {

	private final RebaseInteractivePlan plan;

	private final Repository repo;

	private boolean rebaseHasBeenInitializedButNotStartedYet = false;

	/**
	 * @param repo
	 *            {@link RebaseInteractiveInput#hasRebaseBeenInitializedButNotStartedYet()}
	 */
	RebaseInteractiveInput(Repository repo) {
		Assert.isNotNull(repo);
		this.repo = repo;
		this.plan = new RebaseInteractivePlan();
		this.setRebaseHasBeenInitializedButNotStartedYet(false);
		reload();
	}

	/**
	 * @return {@link RebaseInteractiveInput#hasRebaseBeenInitializedButNotStartedYet()}
	 */
	public boolean hasRebaseBeenInitializedButNotStartedYet() {
		return rebaseHasBeenInitializedButNotStartedYet;
	}

	/**
	 * @param rebaseHasBeenInitializedButNotStartedYet
	 */
	void setRebaseHasBeenInitializedButNotStartedYet(
			boolean rebaseHasBeenInitializedButNotStartedYet) {
		this.rebaseHasBeenInitializedButNotStartedYet = rebaseHasBeenInitializedButNotStartedYet;
	}

	/**
	 * @return the plan
	 */
	public final RebaseInteractivePlan getPlan() {
		return plan;
	}

	/**
	 * @return the repo
	 */
	public final Repository getRepo() {
		return repo;
	}

	/**
	 * @return true if rebasing interactively and the plan has been parsed
	 *         successfully, otherwise false
	 *
	 */
	public boolean reload() {
		return plan.parse(repo);
	}

	/**
	 * @return true if rebasing interactively and the plan has been written
	 *         successfully, otherwise false
	 */
	public boolean persist() {
		if (!checkState())
			return false;
		return plan.persist(repo);
	}

	/**
	 * @return true if this repository is in rebase interactive state, otherwise
	 *         false
	 */
	public boolean checkState() {
		return repo.getRepositoryState() == RepositoryState.REBASING_INTERACTIVE;
	}
}
