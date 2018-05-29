/*******************************************************************************
 * Copyright (c) 2014, 2016 Vadim Dmitriev and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Vadim Dmitriev - initial implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 460595
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.PlanElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;

class PlanContextMenuAction extends Action {

	private RebaseInteractivePlan.ElementAction action;
	private TreeViewer planViewer;
	private RebaseInteractiveStepActionToolBarProvider actionToolbarProvider;

	public PlanContextMenuAction(String text, ImageDescriptor image,
			RebaseInteractivePlan.ElementAction action, TreeViewer planViewer,
			RebaseInteractiveStepActionToolBarProvider actionToolbarProvider) {
		super(text, image);
		int accelerator = actionToolbarProvider.getActionAccelerators()
				.get(action).intValue();
		if (accelerator == SWT.DEL) {
			// setText() with an accelerator text will set the accelerator.
			// So make sure that the real accelerator gets set later, otherwise
			// if may not be what is expected. And if we don't do this here,
			// the "DEL" accelerator will not be shown in the context menu.
			setText(text + '\t'
					+ SWTKeySupport.getKeyFormatterForPlatform()
							.format(SWTKeySupport.convertAcceleratorToKeyStroke(
									accelerator)));
		}
		setAccelerator(accelerator);
		this.action = action;
		this.planViewer = planViewer;
		this.actionToolbarProvider = actionToolbarProvider;
	}

	@Override
	public void run() {
		ISelection selection = planViewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			for (Object selectedRow : structuredSelection.toList()) {
				if (selectedRow instanceof PlanElement) {
					PlanElement planElement = (PlanElement) selectedRow;
					planElement.setPlanElementAction(action);
				}
			}
			actionToolbarProvider.mapActionItemsToSelection(selection);
		}
	}
}
