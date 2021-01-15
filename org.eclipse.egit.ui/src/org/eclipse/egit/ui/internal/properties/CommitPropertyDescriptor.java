/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.properties;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.PropertyDescriptor;

/**
 * A {@link PropertyDescriptor} for commit IDs. It provides a read-only cell
 * editor with two buttons for opening the commit in the EGit commit viewer or
 * to show the commit in the EGit history view.
 */
public class CommitPropertyDescriptor extends GitPropertyDescriptor {

	static final int COLUMN_INDEX = 1;

	private final RepositoryCommit commit;

	/**
	 * Creates a new {@link CommitPropertyDescriptor}.
	 *
	 * @param id
	 *            for the property
	 * @param label
	 *            for the property
	 * @param commit
	 *            to open
	 */
	public CommitPropertyDescriptor(Object id, String label,
			RepositoryCommit commit) {
		super(id, label);
		this.commit = commit;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent) {
		return new ButtonCellEditor(parent);
	}

	private class ButtonCellEditor extends CellEditor {

		private Composite editor;

		private Object content;

		private Button openCommit;

		private Button showInHistory;

		private FocusListener listener;

		private Listener parentListener;

		ButtonCellEditor(Composite parent) {
			super(parent);
		}

		@Override
		protected Control createControl(Composite parent) {
			editor = new Composite(parent, SWT.NONE);
			openCommit = new Button(editor, SWT.PUSH);
			openCommit.setImage(UIIcons.getImage(
					Activator.getDefault().getResourceManager(),
					UIIcons.OPEN_COMMIT));
			openCommit.setToolTipText(
					UIText.CommitPropertyDescriptor_OpenCommitLabel);
			openCommit.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent evenet) {
					try {
						CommitEditor.open(commit);
					} catch (PartInitException e) {
						Activator.showError(e.getLocalizedMessage(), e);
					}
				}
			});
			showInHistory = new Button(editor, SWT.PUSH);
			showInHistory.setImage(UIIcons.getImage(
					Activator.getDefault().getResourceManager(),
					UIIcons.HISTORY));
			showInHistory.setToolTipText(
					UIText.CommitPropertyDescriptor_ShowInHistoryLabel);
			showInHistory.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent event) {
					try {
						IHistoryView view = (IHistoryView) PlatformUI
								.getWorkbench().getActiveWorkbenchWindow()
								.getActivePage().showView(IHistoryView.VIEW_ID);
						view.showHistoryFor(commit);
					} catch (PartInitException e) {
						Activator.showError(e.getLocalizedMessage(), e);
					}
				}
			});
			KeyListener closeEditor = new KeyAdapter() {

				@Override
				public void keyReleased(KeyEvent e) {
					if (e.character == SWT.ESC) {
						fireCancelEditor();
					}
				}
			};
			openCommit.addKeyListener(closeEditor);
			showInHistory.addKeyListener(closeEditor);
			Tree tree = (Tree) parent;
			TreeItem[] selected = tree.getSelection();
			TreeItem item = selected == null || selected.length == 0 ? null
					: selected[0];
			editor.setLayout(new ToolbarLayout(item, COLUMN_INDEX));
			return editor;
		}

		@Override
		protected Object doGetValue() {
			return content;
		}

		@Override
		protected void doSetValue(Object value) {
			content = value;
		}

		@Override
		protected void doSetFocus() {
			if (listener == null) {
				listener = new FocusListener() {

					@Override
					public void focusGained(FocusEvent e) {
						// Nothing
					}

					@Override
					public void focusLost(FocusEvent e) {
						editor.getDisplay().asyncExec(() -> {
							if (!openCommit.isFocusControl()
									&& !showInHistory.isFocusControl()) {
								ButtonCellEditor.this.focusLost();
							}
						});
					}
				};
			}
			openCommit.addFocusListener(listener);
			showInHistory.addFocusListener(listener);
			if (parentListener == null) {
				parentListener = event -> {
					switch (event.type) {
					case SWT.Collapse:
					case SWT.Expand:
						fireCancelEditor();
						break;
					default:
						break;
					}
				};
			}
			editor.getParent().addListener(SWT.Collapse, parentListener);
			editor.getParent().addListener(SWT.Expand, parentListener);
			editor.setFocus();
		}

		@Override
		public void deactivate() {
			if (listener != null) {
				openCommit.removeFocusListener(listener);
				showInHistory.removeFocusListener(listener);
			}
			if (parentListener != null) {
				editor.getParent().removeListener(SWT.Collapse, parentListener);
				editor.getParent().removeListener(SWT.Expand, parentListener);
			}
			super.deactivate();
		}

		private class ToolbarLayout extends Layout {

			private final TreeItem item;

			private final int idx;

			ToolbarLayout(TreeItem item, int columnIndex) {
				this.item = item;
				this.idx = columnIndex;
			}

			@Override
			public void layout(Composite cellEditor, boolean force) {
				Point size = openCommit.computeSize(SWT.DEFAULT, SWT.DEFAULT,
						force);
				Point size2 = showInHistory.computeSize(SWT.DEFAULT,
						SWT.DEFAULT, force);
				int width = size.x + size2.x;
				int height = Math.max(size.y, size2.y);
				// Adjust the cellEditor's bounds as needed
				Rectangle editorBounds = cellEditor.getBounds();
				if (item != null) {
					Point textSize;
					GC gc = null;
					try {
						gc = new GC(cellEditor.getDisplay());
						gc.setFont(item.getFont(idx));
						textSize = gc.stringExtent(item.getText(idx));
					} finally {
						if (gc != null) {
							gc.dispose();
						}
					}
					if (textSize.x >= 0 && textSize.x < editorBounds.width) {
						editorBounds.width = textSize.x == 0 ? 0
								: textSize.x + LayoutConstants.getSpacing().x;
						editorBounds.width += width;
					}
				}
				editorBounds.x += editorBounds.width - width;
				editorBounds.width = width;
				if (height > editorBounds.height) {
					editorBounds.y -= (height - editorBounds.height) / 2;
					editorBounds.height = height;
				} else if (height < editorBounds.height) {
					// Center vertically
					editorBounds.y += (editorBounds.height - height) / 2;
					editorBounds.height = height;
				}
				cellEditor.setBounds(editorBounds);
				openCommit.setBounds(0, 0, size.x, size.y);
				showInHistory.setBounds(size.x, 0, size2.x, size2.y);
			}

			@Override
			public Point computeSize(Composite cellEditor, int wHint, int hHint,
					boolean force) {
				if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT) {
					return new Point(wHint, hHint);
				}
				Point size = openCommit.computeSize(SWT.DEFAULT, SWT.DEFAULT,
						force);
				Point size2 = showInHistory.computeSize(SWT.DEFAULT,
						SWT.DEFAULT, force);
				return new Point(size.x + size2.x, Math.max(size.y, size2.y));
			}
		}
	}
}
