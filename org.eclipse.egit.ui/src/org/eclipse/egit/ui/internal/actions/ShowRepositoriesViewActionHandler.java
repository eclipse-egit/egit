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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ShowInContext;

/**
 * An action to show the repositories view for a resource.
 */
public class ShowRepositoriesViewActionHandler extends RepositoryActionHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoriesView view;
		try {
			view = (RepositoriesView) PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage().showView(
							RepositoriesView.VIEW_ID);
			ShowInContext ctx = new ShowInContext(ResourcesPlugin.getWorkspace().getRoot(), getSelection(event));
			view.show(ctx);
		} catch (PartInitException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null)
			return false;
		ISelectionService srv = (ISelectionService) activeWorkbenchWindow.getService(ISelectionService.class);
		if (srv.getSelection() instanceof StructuredSelection) {
			return ((StructuredSelection) srv.getSelection()).size() == 1;
		}
		return false;
	}
}
