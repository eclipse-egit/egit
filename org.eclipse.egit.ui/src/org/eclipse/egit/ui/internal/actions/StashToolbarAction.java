/*******************************************************************************
 * Copyright (C) 2014 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate;

/**
 * Action for the "Stash" toolbar item, for stashing changes and listing
 * existing stashes.
 */
public class StashToolbarAction extends RepositoryAction implements
		IWorkbenchWindowPulldownDelegate {

	private final MenuManager menuManager = new MenuManager();

	private final StashesMenu stashesMenu = new StashesMenu();

	/**
	 *
	 */
	public StashToolbarAction() {
		super(ActionCommands.STASH_CREATE, new StashCreateHandler());

		menuManager.add(stashesMenu);
	}

	public Menu getMenu(Control parent) {
		stashesMenu.initialize(getServiceLocator());
		return menuManager.createContextMenu(parent);
	}

	@Override
	public void dispose() {
		menuManager.dispose();
		super.dispose();
	}

}
