/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commands.shared;

import java.net.URISyntaxException;

import org.eclipse.egit.core.internal.hosts.GitHosts;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.clone.GitSelectRepositoryPage;
import org.eclipse.egit.ui.internal.fetch.FetchGithubPullRequestWizard;
import org.eclipse.egit.ui.internal.gerrit.FilteredSelectRepositoryPage;
import org.eclipse.egit.ui.internal.selection.SelectionRepositoryStateCache;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;

/**
 * Fetch a pull request from Github.
 */
public class FetchPullRequestFromGithubCommand
		extends AbstractFetchFromHostCommand {

	@Override
	protected GitSelectRepositoryPage createSelectionPage() {
		return new FilteredSelectRepositoryPage(
				UIText.GithubSelectRepositoryPage_PageTitle,
				UIIcons.WIZBAN_FETCH) {

			@Override
			protected boolean includeRepository(Repository repo) {
				try {
					return GitHosts.hasGithubConfig(
							SelectionRepositoryStateCache.INSTANCE
									.getConfig(repo));
				} catch (URISyntaxException e) {
					return false;
				}
			}
		};
	}

	@Override
	protected Wizard createFetchWizard(Repository repository, String clipText) {
		return new FetchGithubPullRequestWizard(repository, clipText);
	}
}
