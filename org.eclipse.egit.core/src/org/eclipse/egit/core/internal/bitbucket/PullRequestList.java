/*******************************************************************************
 * Copyright (C) 2026, Eclipse EGit contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.bitbucket;

import java.util.List;

/**
 * Domain model representing a paginated list of pull requests from Bitbucket
 * Data Center
 */
public class PullRequestList {

	private int size;

	private int limit;

	private boolean isLastPage;

	private List<PullRequest> values;

	private int start;

	private int nextPageStart;

	/**
	 * @return the number of items in this page
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @param size the number of items in this page
	 */
	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * @return the page limit
	 */
	public int getLimit() {
		return limit;
	}

	/**
	 * @param limit the page limit
	 */
	public void setLimit(int limit) {
		this.limit = limit;
	}

	/**
	 * @return whether this is the last page
	 */
	public boolean isLastPage() {
		return isLastPage;
	}

	/**
	 * @param isLastPage whether this is the last page
	 */
	public void setLastPage(boolean isLastPage) {
		this.isLastPage = isLastPage;
	}

	/**
	 * @return the list of pull requests
	 */
	public List<PullRequest> getValues() {
		return values;
	}

	/**
	 * @param values the list of pull requests
	 */
	public void setValues(List<PullRequest> values) {
		this.values = values;
	}

	/**
	 * @return the start index
	 */
	public int getStart() {
		return start;
	}

	/**
	 * @param start the start index
	 */
	public void setStart(int start) {
		this.start = start;
	}

	/**
	 * @return the next page start index
	 */
	public int getNextPageStart() {
		return nextPageStart;
	}

	/**
	 * @param nextPageStart the next page start index
	 */
	public void setNextPageStart(int nextPageStart) {
		this.nextPageStart = nextPageStart;
	}
}
