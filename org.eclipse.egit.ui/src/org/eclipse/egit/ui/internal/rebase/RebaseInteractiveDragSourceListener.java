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

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;

final class RebaseInteractiveDragSourceListener extends DragSourceAdapter {

	private final RebaseInteractiveView rebaseInteractiveView;

	RebaseInteractiveDragSourceListener(
			RebaseInteractiveView rebaseInteractiveView) {
		this.rebaseInteractiveView = rebaseInteractiveView;
	}

	@Override
	public void dragSetData(DragSourceEvent event) {
		if (!this.rebaseInteractiveView.isDragAndDropEnabled())
			return;
		IStructuredSelection selection = (IStructuredSelection) this.rebaseInteractiveView.planTreeViewer
				.getSelection();

		if (selection.isEmpty())
			return;

		if (LocalSelectionTransfer.getTransfer()
				.isSupportedType(event.dataType)) {
			LocalSelectionTransfer.getTransfer().setSelection(selection);
			return;
		}
	}

	@Override
	public void dragStart(DragSourceEvent event) {
		if (!this.rebaseInteractiveView.isDragAndDropEnabled()) {
			event.doit = false;
			return;
		}
		event.doit = !this.rebaseInteractiveView.planTreeViewer.getSelection()
				.isEmpty();
	}

	@Override
	public void dragFinished(DragSourceEvent event) {
		if (LocalSelectionTransfer.getTransfer()
				.isSupportedType(event.dataType))
			LocalSelectionTransfer.getTransfer().setSelection(null);
	}
}
