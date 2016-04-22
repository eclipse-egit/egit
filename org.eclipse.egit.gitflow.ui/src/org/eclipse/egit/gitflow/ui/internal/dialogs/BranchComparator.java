package org.eclipse.egit.gitflow.ui.internal.dialogs;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TreeColumn;

class BranchComparator extends ViewerComparator {
	private TreeColumn currentColumn;

	private static final int DESCENDING = 1;

	private int direction = DESCENDING;

	BranchComparator() {
		this.currentColumn = null;
		direction = DESCENDING;
	}

	int getDirection() {
		return direction == 1 ? SWT.DOWN : SWT.UP;
	}

	void setColumn(TreeColumn column) {
		if (column.equals(currentColumn)) {
			flipSortDirection();
		} else {
			currentColumn = column;
			direction = DESCENDING;
		}
	}

	private void flipSortDirection() {
		direction = -direction;
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		int rc = 0;

		rc = super.compare(viewer, ((Ref) e1).getName(), ((Ref) e2).getName());

		if (direction == DESCENDING) {
			rc = -rc;
		}

		return rc;
	}
}