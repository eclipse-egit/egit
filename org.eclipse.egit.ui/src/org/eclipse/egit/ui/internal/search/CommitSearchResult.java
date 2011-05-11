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

	public String getLabel() {
		int matches = getMatchCount();
		if (matches != 1)
			return MessageFormat.format(UIText.CommitSearchResult_LabelPlural,
					Integer.valueOf(matches));
		else
			return UIText.CommitSearchResult_LabelSingle;
	}

	public String getTooltip() {
		return getLabel();
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public ISearchQuery getQuery() {
		return this.query;
	}

	public IEditorMatchAdapter getEditorMatchAdapter() {
		return null;
	}

	public IFileMatchAdapter getFileMatchAdapter() {
		return null;
	}

	public Object[] getChildren(Object o) {
		return getElements();
	}

	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

	public String getLabel(Object o) {
		return getLabel();
	}

	public Object getParent(Object o) {
		return null;
	}

}
