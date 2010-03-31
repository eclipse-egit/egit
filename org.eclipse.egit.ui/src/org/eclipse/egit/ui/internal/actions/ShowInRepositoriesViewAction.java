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
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Shows a Project in the Repositories View
 */
public class ShowInRepositoriesViewAction implements IObjectActionDelegate {

	private IResource selectedResource = null;

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// nothing here
	}

	public void run(IAction action) {

		if (selectedResource == null) {
			return;
		}

		IViewPart part;
		try {
			part = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().showView(RepositoriesView.VIEW_ID);
		} catch (PartInitException e) {
			Activator.getDefault().getLog().log(e.getStatus());
			return;
		}

		RepositoriesView view = (RepositoriesView) part;
		view.showResource(selectedResource);

	}

	public void selectionChanged(IAction action, ISelection selection) {
		try {
			selectedResource = (IResource) ((IStructuredSelection) selection)
					.getFirstElement();
		} catch (Exception e) {
			Activator.getDefault().getLog().log(
					new Status(IStatus.ERROR, Activator.getPluginId(), e
							.getMessage(), e));
		}
		action.setEnabled(selectedResource != null);
	}

}
