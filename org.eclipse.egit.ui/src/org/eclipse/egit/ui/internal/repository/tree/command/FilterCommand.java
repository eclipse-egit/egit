/*******************************************************************************
 * Copyright (C) 2020, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.FilterCache;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.repository.RepositoriesView.RepositoriesCommonViewer;
import org.eclipse.egit.ui.internal.repository.tree.FilterableNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.progress.UIJob;

/**
 * Expands the selected node and pops up a search field allowing the user to
 * filter the children. The filter is persisted in the node.
 */
public class FilterCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryTreeNode node = getSelectedNodes(event).get(0);
		if (node instanceof FilterableNode) {
			RepositoriesView view = getView(event);
			CommonViewer rawViewer = view.getCommonViewer();
			if (!(rawViewer instanceof RepositoriesCommonViewer)) {
				return null;
			}
			RepositoriesCommonViewer viewer = (RepositoriesCommonViewer) rawViewer;
			IContentProvider rawProvider = viewer.getContentProvider();
			if (!(rawProvider instanceof ITreeContentProvider)) {
				return null;
			}
			ITreeContentProvider provider = (ITreeContentProvider) rawProvider;
			if (!provider.hasChildren(node)) {
				return null;
			}
			if (!viewer.getExpandedState(node)) {
				viewer.setExpandedState(node, true);
			}
			// Pop up search field with initial pattern set; update pattern and
			// request a refresh of the node on each change with a small delay.
			ViewerCell cell = viewer.getCell(node, 0);
			if (cell == null) {
				return null;
			}
			FilterableNode filterNode = (FilterableNode) node;
			String pattern = filterNode.getFilter();
			Rectangle cellBounds = cell.getBounds();
			Rectangle area = viewer.getTree().getClientArea();
			cellBounds.width = Math.min(cellBounds.width, area.width);
			cellBounds.x = 0;
			Rectangle onDisplay = viewer.getTree().getDisplay()
					.map(viewer.getTree(), null, cellBounds);
			PopupDialog popup = new PopupDialog(viewer.getTree().getShell(),
					SWT.TOOL, true, false, false, false, false, null, null) {

				private Text field;

				private UIJob refresher;

				@Override
				protected Composite createDialogArea(Composite parent) {
					Composite container = new Composite(parent, SWT.NONE);
					GridLayoutFactory.fillDefaults().applyTo(container);
					field = new Text(container,
							SWT.SEARCH | SWT.ICON_CANCEL | SWT.ICON_SEARCH);
					GridData textData = GridDataFactory.fillDefaults()
							.grab(true, false).create();
					textData.minimumWidth = 150;
					field.setLayoutData(textData);
					if (pattern != null) {
						field.setText(pattern);
						field.selectAll();
					}
					field.addVerifyListener(
							e -> e.text = Utils.firstLine(e.text));
					AtomicReference<String> currentPattern = new AtomicReference<>();
					refresher = new UIJob(UIText.RepositoriesView_FilterJob) {

						@Override
						public IStatus runInUIThread(IProgressMonitor monitor) {
							if (!monitor.isCanceled()) {
								filter(currentPattern.get());
								return Status.OK_STATUS;
							}
							return Status.CANCEL_STATUS;
						}

					};
					refresher.setUser(false);
					field.addModifyListener(e -> {
						refresher.cancel();
						currentPattern.set(field.getText());
						refresher.schedule(200L);
					});
					field.addKeyListener(new KeyAdapter() {

						@Override
						public void keyPressed(KeyEvent e) {
							int key = SWTKeySupport
									.convertEventToUnmodifiedAccelerator(e);
							if (key == SWT.CR || key == SWT.LF
									|| e.character == '\r'
									|| e.character == '\n') {
								// Character tests catch NUMPAD-ENTER
								close();
							}
						}
					});
					return container;
				}

				private void filter(String filter) {
					FilterCache.INSTANCE.set(filterNode, filter);
					try {
						viewer.getTree().setRedraw(false);
						viewer.refresh(filterNode);
					} finally {
						viewer.getTree().setRedraw(true);
					}
				}

				@Override
				protected Point getDefaultLocation(Point initialSize) {
					return new Point(
							Math.max(0,
									onDisplay.x + onDisplay.width
											- initialSize.x),
							onDisplay.y);
				}

				@Override
				protected Control getFocusControl() {
					return field;
				}

			};
			popup.open();
		}
		return null;
	}

}
