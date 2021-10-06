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
import org.eclipse.jgit.lib.Repository;

/**
 * Wizard for fetching a change from a git server.
 */
public class FetchChangeFromServerWizard extends AbstractFetchFromHostWizard {

	private final GitServer server;

	/**
	 * Creates a new {@link FetchChangeFromServerWizard}.
	 *
	 * @param server
	 *            {@link GitServer} describing the kind of server to fetch a
	 *            change from
	 * @param repository
	 *            {@link Repository} to fetch into
	 */
	public FetchChangeFromServerWizard(GitServer server,
			Repository repository) {
		this(server, repository, null);
	}

	/**
	 * Creates a new {@link FetchChangeFromServerWizard}.
	 *
	 * @param server
	 *            {@link GitServer} describing the kind of server to fetch a
	 *            change from
	 * @param repository
	 *            {@link Repository} to fetch into
	 * @param refName
	 *            initial text to try to extract a change ref from
	 */
	public FetchChangeFromServerWizard(GitServer server, Repository repository,
			String refName) {
		super(repository, refName);
		this.server = server;
		setWindowTitle(server.getWizardTitle());
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_FETCH);
	}

	@Override
	protected AbstractFetchFromHostPage createPage(Repository repo,
			String initialText) {
		return new FetchChangeFromServerPage(server, repo, initialText);
	}
}
