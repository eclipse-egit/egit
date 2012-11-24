/*******************************************************************************
 * Copyright (C) 2010, 2012 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Mathias Kinzler - initial version
 *   François Rey - refactoring as part of gracefully ignoring linked resources
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.internal.history.HistoryPageInput;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * An action to show the history for a resource.
 */
public class ShowHistoryActionHandler extends RepositoryActionHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repo = getRepository(true, event);
		// assert all resources map to the same repository
		if (repo == null)
			return null;
		IHistoryView view;
		try {
			view = (IHistoryView) PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage().showView(
							IHistoryView.VIEW_ID);
			IResource[] resources = getSelectedResources(event);
			if (resources.length == 1) {
				view.showHistoryFor(resources[0]);
				return null;
			}
			HistoryPageInput list = new HistoryPageInput(repo, resources);
			view.showHistoryFor(list);
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
