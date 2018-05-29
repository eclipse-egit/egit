/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.egit.ui.internal.dialogs.CheckoutDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;

/**
 * Action for selecting a branch and checking it out.
 *
 * @see BranchOperationUI
 */
public class BranchActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(true, event);
		if (repository == null) {
			return null;
		}
		CheckoutDialog dialog = new CheckoutDialog(getShell(event), repository);
		if (dialog.open() == Window.OK) {
			BranchOperationUI.checkout(repository, dialog.getRefName()).start();
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		Repository repo = getRepository();
		return repo != null && containsHead(repo);
	}
}
