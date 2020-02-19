/*******************************************************************************
 * Copyright (C) 2020, Alexander Nittka <alex@nittka.de>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Compared the commits referenced by two refs in tree
 */
public class CompareInTreeCommand extends
		CompareCommand {

	@Override
	void compare(ExecutionEvent event, Repository repo,
			String commit1, String commit2) throws ExecutionException {
		try {
			CompareTreeView view = (CompareTreeView) PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage()
					.showView(CompareTreeView.ID);
			view.setInput(repo, commit1, commit2);
		} catch (PartInitException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
	}
}
