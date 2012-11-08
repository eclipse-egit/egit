/*******************************************************************************
 * Copyright (c) 2010 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza (dariusz@luksza.org) - disable command when HEAD cannot be
 *    										resolved
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Implements "Show in History"
 */
public class ShowInHistoryCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		List<RepositoryTreeNode> selectedNodes = getSelectedNodes(event);
		if (selectedNodes.size() == 1) {
			final RepositoryTreeNode nodeToShow = selectedNodes.get(0);
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					try {
						IHistoryView part = (IHistoryView) PlatformUI
								.getWorkbench().getActiveWorkbenchWindow()
								.getActivePage().showView(IHistoryView.VIEW_ID);
						part.showHistoryFor(nodeToShow);
					} catch (PartInitException e1) {
						Activator.handleError(e1.getMessage(), e1, true);
					}
				}
			});
		}
		return null;
	}

	@Override
	public void setEnabled(Object evaluationContext) {
		enableWhenRepositoryHaveHead(evaluationContext);
	}

}
