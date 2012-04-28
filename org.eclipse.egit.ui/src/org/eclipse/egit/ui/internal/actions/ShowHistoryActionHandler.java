/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.UIText;
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

			Repository repo = null;
			for (IResource res : resources) {
				RepositoryMapping map = RepositoryMapping.getMapping(res);
				if (repo == null)
					repo = map.getRepository();
				if (repo != map.getRepository())
					// we need to make sure are resources are from the same
					// Repository
					throw new ExecutionException(
							UIText.AbstractHistoryCommanndHandler_NoUniqueRepository);

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
		return !getSelection().isEmpty() && !selectionContainsLinkedResources();
	}
}
