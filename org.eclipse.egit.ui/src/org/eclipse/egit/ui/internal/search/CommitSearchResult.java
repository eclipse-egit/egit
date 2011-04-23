/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.search;

import java.text.MessageFormat;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;
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
	 * Add result
	 *
	 * @param object
	 * @return this result
	 */
	public CommitSearchResult addResult(Object object) {
		if (object != null)
			addMatch(new Match(object, 0, 0));
		return this;
	}

	/**
	 * @see org.eclipse.search.ui.ISearchResult#getLabel()
	 */
	public String getLabel() {
		int matches = getMatchCount();
		if (matches != 1)
			return UIText.CommitSearchResult_LabelSingle;
		else
			return MessageFormat.format(UIText.CommitSearchResult_LabelPlural,
					Integer.valueOf(matches));
	}

	/**
	 * @see org.eclipse.search.ui.ISearchResult#getTooltip()
	 */
	public String getTooltip() {
		return getLabel();
	}

	/**
	 * @see org.eclipse.search.ui.ISearchResult#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	/**
	 * @see org.eclipse.search.ui.ISearchResult#getQuery()
	 */
	public ISearchQuery getQuery() {
		return this.query;
	}

	/**
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#getEditorMatchAdapter()
	 */
	public IEditorMatchAdapter getEditorMatchAdapter() {
		return null;
	}

	/**
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#getFileMatchAdapter()
	 */
	public IFileMatchAdapter getFileMatchAdapter() {
		return null;
	}

	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object o) {
		return getElements();
	}

	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
	 */
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	public String getLabel(Object o) {
		return getLabel();
	}

	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	public Object getParent(Object o) {
		return null;
	}

}
