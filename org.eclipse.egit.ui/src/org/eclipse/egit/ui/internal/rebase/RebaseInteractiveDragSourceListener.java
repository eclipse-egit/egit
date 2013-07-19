package org.eclipse.egit.ui.internal.rebase;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;

final class RebaseInteractiveDragSourceListener extends
		DragSourceAdapter {
	/**
	 *
	 */
	private final RebaseInteractiveView rebaseInteractiveView;

	/**
	 * @param rebaseInteractiveView
	 */
	RebaseInteractiveDragSourceListener(
			RebaseInteractiveView rebaseInteractiveView) {
		this.rebaseInteractiveView = rebaseInteractiveView;
	}

	public void dragSetData(DragSourceEvent event) {
		if (!this.rebaseInteractiveView.isDragAndDropEnabled())
			return;
		IStructuredSelection selection = (IStructuredSelection) this.rebaseInteractiveView.planTreeViewer
				.getSelection();

		if (selection.isEmpty())
			return;

		if (LocalSelectionTransfer.getTransfer()
				.isSupportedType(event.dataType)) {
			LocalSelectionTransfer.getTransfer().setSelection(
					selection);
			return;
		}

	}

	@Override
	public void dragStart(DragSourceEvent event) {
		if (!this.rebaseInteractiveView.isDragAndDropEnabled()) {
			event.doit = false;
			return;
		}
		event.doit = !this.rebaseInteractiveView.planTreeViewer.getSelection().isEmpty();
	}

	@Override
	public void dragFinished(DragSourceEvent event) {
		if (LocalSelectionTransfer.getTransfer()
				.isSupportedType(event.dataType))
			LocalSelectionTransfer.getTransfer().setSelection(
					null);
	}
}