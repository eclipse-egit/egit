/*******************************************************************************
 * Copyright (c) 2014 Vadim Dmitriev.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Vadim Dmitriev - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import java.util.Iterator;

import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.PlanElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

class PlanContextMenuActionListener extends Action {

	private RebaseInteractivePlan.ElementAction action;
	private TreeViewer planViewer;
	private RebaseInteractiveStepActionToolBarProvider actionToolbarProvider;

	public PlanContextMenuActionListener(String text, ImageDescriptor image,
			RebaseInteractivePlan.ElementAction action, TreeViewer planViewer,
			RebaseInteractiveStepActionToolBarProvider actionToolbarProvider) {
		super(text, image);
		this.action = action;
		this.planViewer = planViewer;
		this.actionToolbarProvider = actionToolbarProvider;
	}

	@Override
	public void run() {
		ISelection selection = planViewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			Iterator selectionIterator = structuredSelection.iterator();
			while (selectionIterator.hasNext()) {
				Object nextSelection = selectionIterator.next();
				if (!(nextSelection instanceof PlanElement)) {
					continue;
				}
				PlanElement planElement = (PlanElement) nextSelection;
				planElement.setPlanElementAction(action);
			}
			actionToolbarProvider.mapActionItemsToSelection(selection);
		}
	}
}