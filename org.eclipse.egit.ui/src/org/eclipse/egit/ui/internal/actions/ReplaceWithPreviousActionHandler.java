/*******************************************************************************
 * Copyright (C) 2012, 2016 Mathias Kinzler <mathias.kinzler@sap.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CommitSelectDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
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
		try {
			List<RevCommit> pcs = findPreviousCommits(Arrays.asList(resources));

			int parentCount = pcs.size();
			if (parentCount == 0) {
				final String resourceNames = CommonUtils
						.getResourceNames(Arrays.asList(resources));

				MessageDialog
						.openError(
								getShell(event),
								UIText.ReplaceWithPreviousActionHandler_NoParentCommitDialogTitle,
								MessageFormat
										.format(UIText.ReplaceWithPreviousActionHandler_NoParentCommitDialogMessage,
												resourceNames));
				throw new OperationCanceledException();
			} else if (parentCount > 1) {
				CommitSelectDialog dlg = new CommitSelectDialog(
						getShell(event), pcs);
				if (dlg.open() == Window.OK)
					return dlg.getSelectedCommit().getName();
				else
					throw new OperationCanceledException();
			} else
				return pcs.get(0).getName();
		} catch (IOException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
	}

	@Override
	public boolean isEnabled() {
		IStructuredSelection selection = getSelection();
		return super.isEnabled() && selection.size() == 1
				&& selectionMapsToSingleRepository();
	}
}
