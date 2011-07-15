/*******************************************************************************
 * Copyright (c) 2011 Benjamin Muskalla.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Benjamin Muskalla (Tasktop Technologies Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.reflog.ReflogView;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Implements "Show in History"
 */
public class ShowInReflogCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	public Object execute(final ExecutionEvent event) throws ExecutionException {

		List<RepositoryTreeNode> selectedNodes = getSelectedNodes(event);
		Repository repo = null;
		if (selectedNodes.size() == 1) {
			RepositoryTreeNode selectedNode = selectedNodes.get(0);
			if (selectedNode.getType() == RepositoryTreeNodeType.REPO) {
				repo = selectedNode.getRepository();
			}

			final Repository repoToShow = repo;
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					try {
						ReflogView part = (ReflogView) PlatformUI
								.getWorkbench().getActiveWorkbenchWindow()
								.getActivePage().showView(ReflogView.VIEW_ID);
						part.showReflogFor(repoToShow);
					} catch (PartInitException e1) {
						Activator.handleError(e1.getMessage(), e1, true);
					}
				}
			});
		}
		return null;
	}

	// @Override
	// public void setEnabled(Object evaluationContext) {
	// enableWhenRepositoryHaveHead(evaluationContext);
	// }

}
