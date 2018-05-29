/*******************************************************************************
 * Copyright (C) 2016, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.dialogs;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * Comparator for {@link FilteredBranchesWidget}.
 *
 */
public class BranchComparator extends ViewerComparator {
	private TreeColumn currentColumn;

	private static final int DESCENDING = SWT.DOWN;

	private static final int ASCENDING = SWT.UP;

	private int direction = DESCENDING;

	private ColumnLabelProvider labelProvider;

	/**
	 * Direction indicator to be supplied to
	 * {@link org.eclipse.swt.widgets.Tree#setSortDirection(int)}
	 *
	 * @return one of <code>UP</code>, <code>DOWN</code> or <code>NONE</code>.
	 */
	public int getDirection() {
		return direction;
	}

	/**
	 * Set the column to sort by, flipping sort direction, if the same column
	 * was set before.
	 *
	 * @param column
	 *            to sort by
	 * @param labelProvider
	 *            to convert cells from selected column into text
	 */
	public void setColumn(TreeColumn column, ColumnLabelProvider labelProvider) {
		this.labelProvider = labelProvider;
		if (column.equals(currentColumn)) {
			flipSortDirection();
		} else {
			currentColumn = column;
			direction = DESCENDING;
		}
	}

	private void flipSortDirection() {
		direction = (direction == DESCENDING) ? ASCENDING : DESCENDING;
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		int rc = 0;

		String firstCell = labelProvider.getText(e1).toLowerCase();
		String secondCell = labelProvider.getText(e2).toLowerCase();
		if (direction == DESCENDING) {
			rc = secondCell.compareTo(firstCell);
		} else {
			rc = firstCell.compareTo(secondCell);
		}

		return rc;
	}
}
