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
package org.eclipse.egit.ui.internal.fetch;

import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.lib.Repository;

/**
 * Wizard for fetching a Github pull request.
 */
public class FetchGithubPullRequestWizard extends AbstractFetchFromHostWizard {

	/**
	 * @param repository
	 *            the repository
	 */
	public FetchGithubPullRequestWizard(Repository repository) {
		super(repository);
		setWindowTitle(UIText.FetchGithubPullRequestWizard_WizardTitle);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_FETCH);
	}

	/**
	 * @param repository
	 * @param refName initial value for the ref field
	 */
	public FetchGithubPullRequestWizard(Repository repository, String refName) {
		super(repository, refName);
		setWindowTitle(UIText.FetchGithubPullRequestWizard_WizardTitle);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_FETCH);
	}

	@Override
	protected AbstractFetchFromHostPage createPage(Repository repo,
			String initialText) {
		return new FetchChangeFromServerPage(GitServer.GITHUB, repo,
				initialText);
	}
}
