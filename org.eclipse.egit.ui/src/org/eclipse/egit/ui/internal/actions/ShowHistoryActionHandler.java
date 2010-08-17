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
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * An action to show the history for a resource.
 */
public class ShowHistoryActionHandler extends RepositoryActionHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IHistoryView view;
		try {
			IViewPart part = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage().findView(
							IHistoryView.VIEW_ID);
			// workaround for GenericHistoryView selection provider issue
			// described in bug 322751:
			// if there is not already a GitHistory page, we hide the view
			// after initializing the page (during the call to
			// showHistoryViewFor()) and reopen that same view again
			// which will eventually result in a call to
			// AbstractSelectionService.setActivePart()
			boolean reuse = false;
			if (part != null) {
				if (((IHistoryView) part).getHistoryPage() instanceof GitHistoryPage)
					reuse = true;
			}
			view = (IHistoryView) PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage().showView(
							IHistoryView.VIEW_ID);
			view.showHistoryFor(getSelection(event).getFirstElement());
			if (!reuse) {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage().hideView((IViewPart) view);
				PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage().showView(IHistoryView.VIEW_ID);
			}
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
			return false; // During Eclipse shutdown there is no active window
		ISelectionService srv = (ISelectionService) activeWorkbenchWindow.getService(ISelectionService.class);
		if (srv.getSelection() instanceof StructuredSelection) {
			return ((StructuredSelection) srv.getSelection()).size() == 1;
		}
		return false;
	}
}
