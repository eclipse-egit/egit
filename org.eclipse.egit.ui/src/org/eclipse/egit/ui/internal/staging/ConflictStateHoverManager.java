/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.text.AbstractHoverInformationControlManager;
import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jgit.lib.IndexDiff.StageState;

/**
 * Shows longer conflict state information in a hover over the last column of
 * the unstaged viewer.
 */
class ConflictStateHoverManager extends AbstractHoverInformationControlManager {

	private final TreeViewer viewer;

	protected ConflictStateHoverManager(TreeViewer viewer) {
		super(DefaultInformationControl::new);
		this.viewer = viewer;
		setAnchor(AbstractInformationControlManager.ANCHOR_BOTTOM);
	}

	@Override
	protected void computeInformation() {
		String information = null;
		ViewerCell cell = viewer.getCell(getHoverEventLocation());
		if (cell != null && cell.getColumnIndex() == 1) {
			Object item = cell.getElement();
			if (item instanceof StagingEntry) {
				StagingEntry entry = (StagingEntry) item;
				if (entry.hasConflicts()) {
					StageState conflictType = entry.getConflictType();
					switch (conflictType) {
					case DELETED_BY_THEM:
						information = UIText.StagingView_Conflict_MD_long;
						break;
					case DELETED_BY_US:
						information = UIText.StagingView_Conflict_DM_long;
						break;
					case BOTH_MODIFIED:
						information = UIText.StagingView_Conflict_M_long;
						break;
					case BOTH_ADDED:
						information = UIText.StagingView_Conflict_A_long;
						break;
					default:
						break;
					}
				}
			}
		}
		if (information != null && cell != null) {
			setInformation(information, cell.getBounds());
		} else {
			setInformation(null, null);
		}
	}
}
