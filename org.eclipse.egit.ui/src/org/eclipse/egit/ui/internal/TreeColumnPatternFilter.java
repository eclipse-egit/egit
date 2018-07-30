/*******************************************************************************
 * Copyright (C) 2018 Michael Keppler <michael.keppler@gmx.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * Pattern filter based on the column label providers of a {@link TreeViewer}.
 */
public class TreeColumnPatternFilter extends PatternFilter {

	/**
	 * Creates a new tree column based pattern filter with a leading wildcard
	 * character included in the search string.
	 */
	public TreeColumnPatternFilter() {
		setIncludeLeadingWildcard(true);
	}

	@Override
	protected boolean isLeafMatch(Viewer viewer, Object element) {
		TreeViewer treeViewer = (TreeViewer) viewer;
		int numberOfColumns = treeViewer.getTree().getColumnCount();
		for (int columnIndex = 0; columnIndex < numberOfColumns; columnIndex++) {
			ColumnLabelProvider labelProvider = (ColumnLabelProvider) treeViewer
					.getLabelProvider(columnIndex);
			String labelText = labelProvider.getText(element);
			if (wordMatches(labelText)) {
				return true;
			}
		}
		return false;
	}

}
