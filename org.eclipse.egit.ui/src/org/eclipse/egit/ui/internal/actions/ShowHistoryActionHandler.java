/*******************************************************************************
 * Copyright (C) 2010, 2016 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Mathias Kinzler - initial version
 *   François Rey - refactoring as part of gracefully ignoring linked resources
 *   Thomas Wolf <thomas.wolf@paranor.ch> - Bug 492336
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.internal.history.HistoryPageInput;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * An action to show the history for a resource.
 */
public class ShowHistoryActionHandler extends RepositoryActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repo = getRepository(true, event);
		// assert all resources map to the same repository
		if (repo == null) {
			return null;
		}
		try {
			IWorkbenchWindow activeWorkbenchWindow = HandlerUtil
					.getActiveWorkbenchWindow(event);
			if (activeWorkbenchWindow != null) {
				IWorkbenchPage page = activeWorkbenchWindow.getActivePage();
				if (page != null) {
					IResource[] resources = getSelectedResources(event);
					IHistoryView view = (IHistoryView) page
							.showView(IHistoryView.VIEW_ID);
					if (resources.length == 1) {
						view.showHistoryFor(resources[0]);
						return null;
					}
					HistoryPageInput list = new HistoryPageInput(repo,
							resources);
					view.showHistoryFor(list);
				}
			}
		} catch (PartInitException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		return selectionMapsToSingleRepository();
	}
}
