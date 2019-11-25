/*******************************************************************************
 * Copyright (C) 2019 Simon Muschel <smuschel@gmx.de> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Simon Muschel <smuschel@gmx.de> - Bug 345466
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.jgit.revwalk.RevFlag;

/**
 * Provides relevant information to {@link SearchBar} implementations.
 */
interface ICommitsProvider {

	/**
	 * Returns the search context the {@link SearchBar} is using. An example on
	 * how to use the context can be found in {@link GitHistoryPage}.
	 *
	 * @return the current search context
	 */
	Object getSearchContext();

	/**
	 * Returns a list of commits to be searched.
	 *
	 * @return array of commits
	 */
	SWTCommit[] getCommits();

	/**
	 * Returns the RevFlag to be used as highlight marker for matching commits.
	 *
	 * @return the highlight RevFlag
	 */
	RevFlag getHighlight();
}