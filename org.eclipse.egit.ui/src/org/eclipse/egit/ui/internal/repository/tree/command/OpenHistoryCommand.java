/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Implements "Open History"
 */
public class OpenHistoryCommand extends
		RepositoriesViewCommandHandler<RepositoryNode> {
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		RepositoryNode node = getSelectedNodes(event).get(0);
		final Repository repo = node.getRepository();
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {
					IHistoryView part = (IHistoryView) PlatformUI
							.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage().showView(IHistoryView.VIEW_ID);
					part.showHistoryFor(repo);
				} catch (PartInitException e1) {
					// TODO
				}
			}
		});
		return null;
	}
}
