/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.List;

import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.core.op.ResetOperation;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Hard reset to selected revision
 */
public class HardResetToRevisionAction extends AbstractRevCommitOperationAction {

	@Override
	protected IEGitOperation createOperation(final List<RevCommit> commits) {
		return new ResetOperation(getActiveRepository(),
				commits.get(0).getName(),
				ResetOperation.ResetType.HARD);
	}

	@Override
	protected String getJobName() {
		return UIText.HardResetToRevisionAction_hardReset;
	}

	@Override
	public void run(IAction act) {
		if (!MessageDialog.openQuestion(wp.getSite().getShell(),
				UIText.ResetTargetSelectionDialog_ResetQuestion,
				UIText.ResetTargetSelectionDialog_ResetConfirmQuestion))
			return;
		super.run(act);
	}

}
