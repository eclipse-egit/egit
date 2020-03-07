/*******************************************************************************
 * Copyright (C) 2020, Alexander Nittka <alex@nittka.de>.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

/**
 * Compares the commits referenced by two refs in tree.
 */
public class CompareInTreeCommand extends CompareCommand {

	@Override
	protected void compare(IWorkbenchPage page, Repository repo,
			String compareCommit, String baseCommit) throws ExecutionException {
		try {
			CompareTreeView view = (CompareTreeView) page
					.showView(CompareTreeView.ID);
			view.setInput(repo, compareCommit, baseCommit);
		} catch (PartInitException e) {
			throw new ExecutionException(e.getLocalizedMessage(), e);
		}
	}
}
