/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import java.util.Iterator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Tooltips for the staging viewer. On hover over selected staging entries, pop
 * up a toolbar giving quick access to the most common actions.
 */
public class StagingViewTooltips extends FixedJFaceToolTip {

	private final ColumnViewer viewer;

	private final IAction[] actions;

	/**
	 * Enables the tooltips for the given viewer. The tooltips will appear when
	 * the mouse pointer hovers over a selected staging entry or folder and will
	 * display a {@link ToolBar} with the given actions.
	 *
	 * @param viewer
	 *            to show the tooltips for
	 * @param actions
	 *            to show in the toolbar; must be neither {@code null} nor
	 *            empty, and all actions must have an image descriptor set and
	 *            must <em>not</em> have style
	 *            {@link IAction#AS_DROP_DOWN_MENU}.
	 */
	protected StagingViewTooltips(ColumnViewer viewer, IAction... actions) {
		super(viewer.getControl(), ToolTip.NO_RECREATE, false);
		Assert.isLegal(actions != null && actions.length > 0);
		for (IAction action : actions) {
			Assert.isNotNull(action.getImageDescriptor());
			Assert.isLegal(
					(action.getStyle() & IAction.AS_DROP_DOWN_MENU) == 0);
		}
		this.viewer = viewer;
		this.actions = actions;
		setHideOnMouseDown(false);
	}

	@Override
	protected ViewerCell getToolTipArea(Event event) {
		return viewer.getCell(new Point(event.x, event.y));
	}

	@Override
	protected boolean shouldCreateToolTip(Event event) {
		return super.shouldCreateToolTip(event) && isSelected(event);
	}

	private boolean isSelected(Event event) {
		ViewerCell currentCell = getToolTipArea(event);
		if (currentCell == null) {
			return false;
		}
		Object item = currentCell.getElement();
		if (!(item instanceof StagingEntry)
				&& !(item instanceof StagingFolderEntry)) {
			return false;
		}
		ISelection selection = viewer.getSelection();
		if (selection.isEmpty()
				|| !(selection instanceof IStructuredSelection)) {
			return false;
		}
		Iterator<?> selectedObjects = ((IStructuredSelection) selection)
				.iterator();
		while (selectedObjects.hasNext()) {
			if (item == selectedObjects.next()) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected Composite createToolTipContentArea(Event event,
			Composite parent) {
		ToolBar bar = new ToolBar(parent, SWT.FLAT | SWT.HORIZONTAL);
		for (IAction action : actions) {
			ToolItem item = new ToolItem(bar, SWT.PUSH);
			item.setImage(UIIcons.getImage(
					Activator.getDefault().getResourceManager(),
					action.getImageDescriptor()));
			String tooltip = action.getToolTipText();
			if (tooltip == null || tooltip.isEmpty()) {
				tooltip = action.getText();
			}
			item.setToolTipText(tooltip);
			item.setEnabled(true);
			item.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					hide();
					if (action.isEnabled()) { // Double-check...
						action.run();
					}
				}
			});
		}
		return bar;
	}

}
