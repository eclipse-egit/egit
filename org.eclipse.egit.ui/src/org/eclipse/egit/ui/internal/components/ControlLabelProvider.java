/*******************************************************************************
 * Copyright (c) 2019 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Specialized label provider to put native SWT {@link Control}s into a column
 * of a JFace {@link org.eclipse.jface.viewers.TableViewer TableViewer}.
 */
public abstract class ControlLabelProvider extends ColumnLabelProvider {

	private final Map<TableItem, Editor> editors = new HashMap<>();

	@Override
	public void dispose() {
		editors.clear();
		super.dispose();
	}

	@Override
	public String getText(Object element) {
		return null;
	}

	@Override
	public void update(ViewerCell cell) {
		super.update(cell);
		Object obj = cell.getElement();
		Widget w = cell.getViewerRow().getItem();
		if (w instanceof TableItem) {
			TableItem item = (TableItem) w;
			Table table = item.getParent();
			Editor editor = editors.get(item);
			if (editor == null) {
				Control control = setEditor(cell, table, null, obj);
				if (control == null) {
					return;
				}
				control.pack();
				editor = new Editor(table);
				editor.horizontalAlignment = SWT.CENTER;
				editor.verticalAlignment = SWT.CENTER;
				Point size = control.getSize();
				editor.minimumWidth = size.x;
				editor.minimumHeight = size.y;
				editors.put(item, editor);
				editor.setEditor(control, item, cell.getColumnIndex());
				editor.connect();
			} else {
				Control existing = editor.getEditor();
				Control control = setEditor(cell, table, existing, obj);
				if (control != existing) {
					if (!existing.isDisposed()) {
						existing.dispose();
					}
					if (control == null) {
						editor.dispose();
						return;
					}
					control.pack();
					editor.setEditor(control);
				}
			}
		}
	}

	/**
	 * Creates a new {@link Control}, or updates an existing one.
	 *
	 * @param cell
	 *            that is being dealt with
	 * @param parent
	 *            to use if a new control is created
	 * @param control
	 *            an already existing widget to be updated, or {@code null} if a
	 *            new widget should be created
	 * @param element
	 *            being shown
	 * @return the {@link Control} to show, or {@code null} if no control is to
	 *         be shown.
	 */
	public abstract Control setEditor(ViewerCell cell, Composite parent,
			Control control, Object element);

	private class Editor extends TableEditor {

		private DisposeListener disposer;

		private final Table table;

		public Editor(Table table) {
			super(table);
			this.table = table;
		}

		@Override
		public void layout() {
			if (Util.isGtk() && SWT.getVersion() <= 4924) {
				// Layout is relative to the table's clientArea, which includes
				// the header if one is shown. Results in editors being shown
				// over the column headers.
				//
				// This is a work-around for bug 535978; would be needed only on
				// SWT versions < 4924r7.
				TableItem item = getItem();
				if (item != null) {
					Rectangle rect = item.getBounds();
					if (table.getHeaderVisible()) {
						Control editor = getEditor();
						if (editor != null && !editor.isDisposed()) {
							editor.setVisible(
									rect.y >= table.getHeaderHeight());
						}
					}
				}
			}
			super.layout();
		}

		public void connect() {
			TableItem item = getItem();
			if (item != null) {
				disposer = e -> {
					Control editor = getEditor();
					if (editor != null && !editor.isDisposed()) {
						editor.dispose();
					}
					dispose();
				};
				item.addDisposeListener(disposer);
			}
		}

		private void disconnect() {
			if (disposer != null) {
				TableItem item = getItem();
				if (item != null) {
					editors.remove(item);
					item.removeDisposeListener(disposer);
				}
				disposer = null;
			}
		}

		@Override
		public void dispose() {
			disconnect();
			super.dispose();
		}
	}
}
