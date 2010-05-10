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
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Soft reset to selected revision
 */
public class SoftResetToRevisionAction extends AbstractRevCommitOperationAction {

	@Override
	protected IEGitOperation createOperation(List<RevCommit> selection) {
		return new ResetOperation(getActiveRepository(),
				selection.get(0).name(),
				ResetOperation.ResetType.SOFT);
	}

	@Override
	protected String getJobName() {
		return UIText.SoftResetToRevisionAction_softReset;
	}
}
