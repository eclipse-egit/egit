/*******************************************************************************
 * Copyright (C) 2010, 2012 Mathias Kinzler <mathias.kinzler@sap.com> and others.
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
 *   François Rey - gracefully ignore linked resources
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ShowInContext;

/**
 * An action to show the repositories view for a resource.
 */
public class ShowRepositoriesViewActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoriesView view;
		try {
			// remember selection before activating a new view
			IStructuredSelection selection = getSelection(event);

			view = (RepositoriesView) PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage().showView(
							RepositoriesView.VIEW_ID);
			ShowInContext ctx = new ShowInContext(ResourcesPlugin.getWorkspace().getRoot(), selection);
			view.show(ctx);
		} catch (PartInitException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		return getSelection().size() == 1 && selectionMapsToSingleRepository();
	}
}
