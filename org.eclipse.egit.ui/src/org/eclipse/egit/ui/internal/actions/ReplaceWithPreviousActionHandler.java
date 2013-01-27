/*******************************************************************************
 * Copyright (C) 2012, Mathias Kinzler <mathias.kinzler@sap.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CommitSelectDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Replace with previous revision action handler.
 */
public class ReplaceWithPreviousActionHandler extends
		DiscardChangesActionHandler {

	@Override
	protected String gatherRevision(ExecutionEvent event)
			throws ExecutionException {
		IResource[] resources = getSelectedResources(event);
		if (resources.length != 1)
			throw new ExecutionException(
					"Unexpected number of selected Resources"); //$NON-NLS-1$
		try {
			List<PreviousCommit> pcs = findPreviousCommits();
			List<RevCommit> previousCommits = new ArrayList<RevCommit>();
			for (PreviousCommit pc: pcs)
				previousCommits.add(pc.commit);
			int parentCount = previousCommits.size();
			if (parentCount == 0) {
				MessageDialog
						.openError(
								getShell(event),
								UIText.ReplaceWithPreviousActionHandler_NoParentCommitDialogTitle,
								MessageFormat
										.format(UIText.ReplaceWithPreviousActionHandler_NoParentCommitDialogMessage,
												resources[0].getName()));
				throw new OperationCanceledException();
			} else if (parentCount > 1) {
				CommitSelectDialog dlg = new CommitSelectDialog(
						getShell(event), previousCommits);
				if (dlg.open() == Window.OK)
					return dlg.getSelectedCommit().getName();
				else
					throw new OperationCanceledException();
			} else
				return previousCommits.get(0).getName();
		} catch (IOException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
	}

	@Override
	public boolean isEnabled() {
		return super.isEnabled() && getSelectedResources().length == 1;
	}
}
