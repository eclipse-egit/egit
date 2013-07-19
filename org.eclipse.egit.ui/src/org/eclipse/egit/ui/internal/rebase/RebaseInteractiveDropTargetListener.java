package org.eclipse.egit.ui.internal.rebase;

import org.eclipse.egit.ui.internal.rebase.RebaseInteractivePlan.PlanEntry;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;

final class RebaseInteractiveDropTargetListener extends
		ViewerDropAdapter {
	/**
	 *
	 */
	private final RebaseInteractiveView rebaseInteractiveView;

	RebaseInteractiveDropTargetListener(RebaseInteractiveView rebaseInteractiveView, Viewer viewer) {
		super(viewer);
		this.rebaseInteractiveView = rebaseInteractiveView;
	}

	public boolean performDrop(Object data) {
		if (!this.rebaseInteractiveView.isDragAndDropEnabled())
			return false;
		DropTargetEvent event = getCurrentEvent();
		PlanEntry source = null;

		if (event.data instanceof IStructuredSelection) {
			final IStructuredSelection structuredSelection = (IStructuredSelection) data;
			if (structuredSelection.size() > 1)
				return false;
			if (structuredSelection.getFirstElement() instanceof PlanEntry) {
				source = (PlanEntry) structuredSelection
						.getFirstElement();
			}
		}

		if (source == null) {
			return false;
		}
		Object target = getCurrentTarget();

		if (!(target instanceof PlanEntry))
			return false;

		int targetIndex = this.rebaseInteractiveView.input.getPlan().getTodo()
				.indexOf(target);
		final int sourceIndex = this.rebaseInteractiveView.input.getPlan().getTodo()
				.indexOf(source);

		if (targetIndex == -1 || sourceIndex == -1
				|| targetIndex == sourceIndex) {
			return false;
		}

		boolean moveUp = targetIndex < sourceIndex;

		switch (getCurrentLocation()) {
		case ViewerDropAdapter.LOCATION_AFTER:
			if (!moveUp) {
				targetIndex--;
			}
			break;
		case ViewerDropAdapter.LOCATION_ON:
			break;
		case ViewerDropAdapter.LOCATION_BEFORE:
			if (moveUp) {
				targetIndex++;
			}
			break;
		case ViewerDropAdapter.LOCATION_NONE:
			return false;
		}

		this.rebaseInteractiveView.input.getPlan().move(sourceIndex, targetIndex);
		this.rebaseInteractiveView.input.persist();
		this.rebaseInteractiveView.planTreeViewer.refresh(true);
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
		if (getCurrentTarget() instanceof PlanEntry) {

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