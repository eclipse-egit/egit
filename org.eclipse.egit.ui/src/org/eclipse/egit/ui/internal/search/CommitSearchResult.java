/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.search;

import java.text.MessageFormat;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Commit search result class.
 */
public class CommitSearchResult extends AbstractTextSearchResult implements
		IWorkbenchAdapter {

	private ISearchQuery query;

	/**
	 * Create commit search result
	 *
	 * @param query
	 */
	public CommitSearchResult(ISearchQuery query) {
		this.query = query;
	}

	/**
	 * Add commit to result
	 *
	 * @param commit
	 * @return this result
	 */
	public CommitSearchResult addResult(RepositoryCommit commit) {
		if (commit != null)
			addMatch(new CommitMatch(commit));
		return this;
	}

	/**
	 * @see org.eclipse.search.ui.ISearchResult#getLabel()
	 */
	@Override
	public String getLabel() {
		int matches = getMatchCount();
		String pattern = ((CommitSearchQuery) query).getPattern();
		if (matches != 1)
			return MessageFormat.format(UIText.CommitSearchResult_LabelPlural,
					pattern, Integer.valueOf(matches));
		else
			return MessageFormat.format(UIText.CommitSearchResult_LabelSingle,
					pattern);
	}

	/**
	 * @see org.eclipse.search.ui.ISearchResult#getTooltip()
	 */
	@Override
	public String getTooltip() {
		return getLabel();
	}

	/**
	 * @see org.eclipse.search.ui.ISearchResult#getImageDescriptor()
	 */
	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	/**
	 * @see org.eclipse.search.ui.ISearchResult#getQuery()
	 */
	@Override
	public ISearchQuery getQuery() {
		return this.query;
	}

	/**
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#getEditorMatchAdapter()
	 */
	@Override
	public IEditorMatchAdapter getEditorMatchAdapter() {
		return null;
	}

	/**
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#getFileMatchAdapter()
	 */
	@Override
	public IFileMatchAdapter getFileMatchAdapter() {
		return null;
	}

	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object o) {
		return getElements();
	}

	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
	 */
	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	@Override
	public String getLabel(Object o) {
		return getLabel();
	}

	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object o) {
		return null;
	}

}
