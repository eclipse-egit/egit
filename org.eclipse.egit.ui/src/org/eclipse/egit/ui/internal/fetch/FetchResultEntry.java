/*******************************************************************************
 * Copyright (C) 2026 EGit Contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import org.eclipse.jgit.transport.FetchResult;

/**
 * A fetch result with the user-facing source label used to start the fetch.
 */
public class FetchResultEntry {

	private final FetchResult result;

	private final String sourceString;

	/**
	 * @param result
	 *            fetch result
	 * @param sourceString
	 *            user-facing source label
	 */
	public FetchResultEntry(FetchResult result, String sourceString) {
		this.result = result;
		this.sourceString = sourceString;
	}

	/**
	 * @return fetch result
	 */
	public FetchResult getResult() {
		return result;
	}

	/**
	 * @return user-facing source label
	 */
	public String getSourceString() {
		return sourceString;
	}
}
