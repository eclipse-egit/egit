/*******************************************************************************
 * Copyright (c) 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;

final class RebaseInteractiveDropTargetListener extends ViewerDropAdapter {

	private final RebaseInteractiveView rebaseInteractiveView;

	RebaseInteractiveDropTargetListener(
			RebaseInteractiveView rebaseInteractiveView, Viewer viewer) {
		super(viewer);
		this.rebaseInteractiveView = rebaseInteractiveView;
	}

	public boolean performDrop(Object data) {
		if (!this.rebaseInteractiveView.isDragAndDropEnabled())
			return false;

		DropTargetEvent event = getCurrentEvent();
		RebaseInteractivePlan.PlanElement sourceElement = null;
		if (event.data instanceof IStructuredSelection) {
			final IStructuredSelection structuredSelection = (IStructuredSelection) data;
			if (structuredSelection.size() > 1)
				return false;
			if (structuredSelection.getFirstElement() instanceof RebaseInteractivePlan.PlanElement)
				sourceElement = (RebaseInteractivePlan.PlanElement) structuredSelection
						.getFirstElement();
		}

		if (sourceElement == null)
			return false;

		Object targetObj = getCurrentTarget();
		if (!(targetObj instanceof RebaseInteractivePlan.PlanElement))
			return false;
		RebaseInteractivePlan.PlanElement targetElement = (RebaseInteractivePlan.PlanElement) targetObj;

		boolean before = false;
		switch (getCurrentLocation()) {
		case ViewerDropAdapter.LOCATION_BEFORE:
			before = true;
			break;
		case ViewerDropAdapter.LOCATION_NONE:
			return false;
		}

		if (RebaseInteractivePreferences.isOrderReversed())
			before = !before;

		rebaseInteractiveView.getCurrentPlan().moveTodoEntry(sourceElement,
				targetElement, before);
		return true;
	}

	@Override
	public void dragOver(DropTargetEvent event) {
		super.dragOver(event);
	}

	@Override
	public boolean validateDrop(Object target, int operation,
			TransferData transferType) {
		if (!this.rebaseInteractiveView.isDragAndDropEnabled())
			return false;
		if (getCurrentTarget() instanceof RebaseInteractivePlan.PlanElement) {
			switch (getCurrentLocation()) {
			case ViewerDropAdapter.LOCATION_AFTER:
				return true;
			case ViewerDropAdapter.LOCATION_ON:
				return false;
			case ViewerDropAdapter.LOCATION_BEFORE:
				return true;
			case ViewerDropAdapter.LOCATION_NONE:
				return false;
			}
		}
		return false;
	}
}