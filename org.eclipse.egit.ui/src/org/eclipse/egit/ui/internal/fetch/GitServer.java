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

import org.eclipse.egit.core.internal.hosts.GitHosts.ServerType;
import org.eclipse.egit.ui.internal.UIText;

/**
 * A {@link GitServer} encapsulates some UI labels for a {@link ServerType}.
 */
public enum GitServer {

	/**
	 * A {@link GitServer} describing Github git servers.
	 */
	GITHUB(ServerType.GITHUB) {

		@Override
		public String getName() {
			return "Github"; //$NON-NLS-1$
		}

		@Override
		public String getProposalLabel() {
			return UIText.GitServer_PullRequestContentAssistLabel;
		}

		@Override
		public String getBranchName() {
			return UIText.GitServer_PullRequestRefNameSuggestion;
		}

		@Override
		public String getChangeLabel() {
			return UIText.GitServer_PullRequestLabel;
		}

		@Override
		public String getChangeNameSingular() {
			return UIText.GitServer_PullRequestSingular;
		}

		@Override
		public String getChangeNamePlural() {
			return UIText.GitServer_PullRequestPlural;
		}
	};

	private final ServerType serverType;

	private GitServer(ServerType serverType) {
		this.serverType = serverType;
	}

	/**
	 * Retrieves the {@link ServerType} of this {@link GitServer}.
	 *
	 * @return the {@link ServerType}
	 */
	public ServerType getType() {
		return serverType;
	}

	/**
	 * Retrieves a human-readable name for this kind of git server.
	 *
	 * @return the name
	 */
	public abstract String getName();

	/**
	 * Retrieves a message for creating content assist proposals, with a "{0}"
	 * placeholder for the change ID.
	 *
	 * @return the message
	 */
	public abstract String getProposalLabel();

	/**
	 * Retrieves a message to construct a valid branch or tag name, with a "{0}"
	 * placeholder for the change ID.
	 *
	 * @return the message
	 */
	public abstract String getBranchName();

	/**
	 * Retrieves the label for the "change ID" field of the fetch wizard page.
	 *
	 * @return the label string, with a hot key marked with '&'
	 */
	public abstract String getChangeLabel();

	/**
	 * Retrieves the UI text for the term describing a change (like "change", or
	 * "pull request").
	 *
	 * @return the UI text
	 */
	public abstract String getChangeNameSingular();

	/**
	 * Retrieves the UI text for the term describing multiple changes (like
	 * "changes", or "pull requests").
	 *
	 * @return the UI text
	 */
	public abstract String getChangeNamePlural();
}
