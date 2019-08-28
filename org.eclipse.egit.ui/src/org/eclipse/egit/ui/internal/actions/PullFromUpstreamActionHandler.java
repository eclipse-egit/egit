/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.pull.PullOperationUI;
import org.eclipse.egit.ui.internal.selection.RepositoryStateCache;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;

/**
 * Action for pulling into the currently checked-out branch.
 */
public class PullFromUpstreamActionHandler extends RepositoryActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository[] repos = getRepositories(event);
		if (repos.length == 0)
			return null;
		Set<Repository> repositories = new LinkedHashSet<>(
				Arrays.asList(repos));
		new PullOperationUI(repositories).start();
		return null;
	}

	@Override
	public boolean isEnabled() {
		// we don't do the full canMerge check here, but
		// ensure that a branch is checked out
		Repository[] repos = getRepositories();
		for (Repository repo : repos) {
			String fullBranch = RepositoryStateCache.INSTANCE
					.getFullBranchName(repo);
			if (fullBranch == null
					|| !fullBranch.startsWith(Constants.R_REFS)) {
				return false;
			}
			if (RepositoryStateCache.INSTANCE.getHead(repo) == null) {
				return false;
			}
		}
		return repos.length > 0;
	}
}
