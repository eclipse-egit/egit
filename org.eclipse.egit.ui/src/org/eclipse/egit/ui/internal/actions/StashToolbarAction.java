/*******************************************************************************
 * Copyright (C) 2014 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate;

/**
 * Action for the "Stash" toolbar item, for stashing changes and listing
 * existing stashes.
 */
public class StashToolbarAction extends RepositoryAction implements
		IWorkbenchWindowPulldownDelegate {

	private Menu menu;
	private final StashesMenu stashesMenu = new StashesMenu();

	/**
	 * Create the action
	 */
	public StashToolbarAction() {
		super(ActionCommands.STASH_CREATE, new StashCreateHandler() {
			/*
			 * We need to override this because the toolbar action and its menu
			 * have the same "enabled" state. The menu needs to be enabled even
			 * when creating a new stash is not currently possible.
			 */
			@Override
			public boolean isEnabled() {
				return getRepository() != null;
			}
		});
	}

	@Override
	public Menu getMenu(Control parent) {
		stashesMenu.initialize(getServiceLocator());
		if (menu != null)
			menu.dispose();
		menu = new Menu(parent);
		stashesMenu.fill(menu, 0);
		return menu;
	}

	@Override
	public void dispose() {
		if (menu != null)
			menu.dispose();
		super.dispose();
	}

}
