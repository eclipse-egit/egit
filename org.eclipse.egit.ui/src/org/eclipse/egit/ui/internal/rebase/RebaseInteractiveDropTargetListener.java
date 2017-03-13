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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

	@Override
	public boolean performDrop(Object data) {
		if (!this.rebaseInteractiveView.isDragAndDropEnabled())
			return false;

		if (!(data instanceof IStructuredSelection))
			return false;

		final IStructuredSelection structuredSelection = (IStructuredSelection) data;
		List selectionList = structuredSelection.toList();

		if (selectionList.contains(getCurrentTarget()))
			return false;

		List<RebaseInteractivePlan.PlanElement> sourceElements = new ArrayList<>();
		for (Object obj : selectionList) {
			if (obj instanceof RebaseInteractivePlan.PlanElement)
				sourceElements.add((RebaseInteractivePlan.PlanElement) obj);
		}

		if (sourceElements.isEmpty())
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

		if (!before)
			Collections.reverse(sourceElements);

		if (RebaseInteractivePreferences.isOrderReversed())
			before = !before;

		for (RebaseInteractivePlan.PlanElement element : sourceElements)
			rebaseInteractiveView.getCurrentPlan().moveTodoEntry(element,
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
