/*******************************************************************************
 * Copyright (C) 2019 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.util.Collection;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

/**
 * Specialized {@link Action} intended to be used in a
 * {@link org.eclipse.jface.action.IContributionManager IContributionManager}
 * for actions with a drop-down menu. In a tool bar, selecting the button itself
 * will by default also show the menu; if that is not desired, override
 * {@link #runWithEvent(Event)} or {@link #run()}.
 */
public abstract class DropDownMenuAction extends Action
		implements IWorkbenchAction, IMenuCreator {

	private Menu controlMenu;

	private Menu subMenu;

	private boolean showMenu;

	/**
	 * Creates a new {@link DropDownMenuAction}.
	 *
	 * @param title
	 *            for the action
	 */
	public DropDownMenuAction(String title) {
		super(title, IAction.AS_DROP_DOWN_MENU);
	}

	@Override
	public void run() {
		showMenu = true;
	}

	@Override
	public void runWithEvent(Event event) {
		if (!isEnabled()) {
			return;
		}
		// Show the menu also when the button is clicked, unless run() is
		// overridden (and not called via super).
		showMenu = false;
		run();
		Widget widget = event.widget;
		if (showMenu && (widget instanceof ToolItem)) {
			ToolItem item = (ToolItem) widget;
			Rectangle bounds = item.getBounds();
			event.detail = SWT.ARROW;
			event.x = bounds.x;
			event.y = bounds.y + bounds.height;
			item.notifyListeners(SWT.Selection, event);
		}
	}

	@Override
	public IMenuCreator getMenuCreator() {
		return this;
	}

	private Menu fillMenu(Menu m) {
		for (IContributionItem item : getActions()) {
			item.fill(m, -1);
		}
		return m;
	}

	private Menu dispose(Menu m) {
		if (m != null) {
			if (!m.isDisposed()) {
				m.dispose();
			}
		}
		return null;
	}

	@Override
	public Menu getMenu(Menu parent) {
		subMenu = dispose(subMenu);
		subMenu = fillMenu(new Menu(parent));
		return subMenu;
	}

	@Override
	public Menu getMenu(Control parent) {
		controlMenu = dispose(controlMenu);
		controlMenu = fillMenu(new Menu(parent));
		return controlMenu;
	}

	/**
	 * Obtains the items to display in the drop-down menu. Might be action
	 * contributions or separators.
	 *
	 * @return the items
	 */
	protected abstract Collection<IContributionItem> getActions();

	@Override
	public void dispose() {
		controlMenu = dispose(controlMenu);
		subMenu = dispose(subMenu);
	}

}
