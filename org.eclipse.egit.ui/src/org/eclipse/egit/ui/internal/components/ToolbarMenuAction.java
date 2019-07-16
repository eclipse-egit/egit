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
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jgit.annotations.NonNull;
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
 * {@link org.eclipse.jface.action.ToolBarManager ToolBarManager} for buttons
 * with a drop-down menu. By default selecting the button itself will also show
 * the menu; if that is not desired, override {@link #runWithEvent(Event)} or
 * {@link #run()}.
 */
public abstract class ToolbarMenuAction extends Action
		implements IWorkbenchAction, IMenuCreator {

	private Menu menu;

	private boolean showMenu;

	/**
	 * Creates a new {@link ToolbarMenuAction}.
	 *
	 * @param title
	 *            for the action
	 */
	public ToolbarMenuAction(String title) {
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

	@Override
	public Menu getMenu(Menu parent) {
		// Not used
		return null;
	}

	@Override
	public Menu getMenu(Control parent) {
		if (menu != null) {
			menu.dispose();
			menu = null;
		}
		if (isEnabled()) {
			menu = new Menu(parent);
			for (IAction action : getActions()) {
				ActionContributionItem item = new ActionContributionItem(
						action);
				item.fill(menu, -1);
			}
		}
		return menu;
	}

	/**
	 * Obtains the actions to display in the drop-down menu.
	 *
	 * @return the actions
	 */
	@NonNull
	protected abstract Collection<IAction> getActions();

	@Override
	public void dispose() {
		if (menu != null) {
			menu.dispose();
			menu = null;
		}
	}

}
