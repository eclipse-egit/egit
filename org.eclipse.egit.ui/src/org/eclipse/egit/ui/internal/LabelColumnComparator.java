/*******************************************************************************
 * Copyright (C) 2018, Luís Copetti <lhcopetti@gmail.com>
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
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * A lexicographical label column comparator. It depends on the
 * {@link ColumnLabelProvider} provided by the {@link TableViewer} to retrieve
 * the label for the given column and supports {@link #ASCENDING},
 * {@link #DESCENDING} and also the default ordering {@link #NONE}. Clicking on
 * the column header repeatedly alternates between the previously mentioned
 * ordering modes.
 */
public class LabelColumnComparator extends ViewerComparator {

	private static final int ASCENDING = SWT.DOWN;

	private static final int NONE = SWT.NONE;

	private static final int DESCENDING = SWT.UP;

	private final TableColumn column;

	private final int columnIndex;

	private final TableViewer tv;

	private int direction;

	/**
	 *
	 * @param tableViewer
	 * @param column
	 * @param columnIndex
	 */
	public LabelColumnComparator(TableViewer tableViewer, TableColumn column,
			int columnIndex) {
		super(null);
		this.tv = tableViewer;
		this.column = column;
		this.columnIndex = columnIndex;
		column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (tv.getComparator() == LabelColumnComparator.this) {
					if (direction == ASCENDING) {
						setDirection(DESCENDING);
					} else {
						setDirection(NONE);
					}
				} else {
					setDirection(ASCENDING);
				}
			}
		});
	}

	private void setDirection(int newDirection) {
		direction = newDirection;
		Table table = column.getParent();
		table.setSortDirection(direction);
		if (direction == NONE) {
			table.setSortColumn(null);
			tv.setComparator(null);
		} else {
			table.setSortColumn(column);
			if (tv.getComparator() == this) {
				tv.refresh();
			} else {
				tv.setComparator(this);
			}
		}
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		ColumnLabelProvider labelProvider = (ColumnLabelProvider) tv
				.getLabelProvider(columnIndex);
		String label1 = labelProvider.getText(e1);
		String label2 = labelProvider.getText(e2);
		if (direction == ASCENDING) {
			return label1.compareTo(label2);
		} else {
			return label2.compareTo(label1);
		}
	}
}
