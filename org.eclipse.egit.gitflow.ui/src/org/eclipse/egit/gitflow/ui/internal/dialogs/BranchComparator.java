/*******************************************************************************
 * Copyright (C) 2016, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	private static final int DESCENDING = 1;

	private int direction = DESCENDING;

	private ColumnLabelProvider labelProvider;

	/**
	 * Direction indicator to be supplied to
	 * {@link org.eclipse.swt.widgets.Tree#setSortDirection(int)}
	 *
	 * @return one of <code>UP</code>, <code>DOWN</code> or <code>NONE</code>.
	 */
	public int getDirection() {
		return direction == 1 ? SWT.DOWN : SWT.UP;
	}

	/**
	 * Set to column to sort by, flipping sort direction, if the same column was
	 * set before.
	 *
	 * @param column
	 *            to sort by
	 * @param labelProvider 
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
		direction = -direction;
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		int rc = 0;

		if (direction == DESCENDING) {
			rc = super.compare(viewer, labelProvider.getText(e1), labelProvider.getText(e2));
		} else {
			rc = super.compare(viewer, labelProvider.getText(e2), labelProvider.getText(e1));
		}

		return rc;
	}
}