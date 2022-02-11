/*******************************************************************************
 * Copyright (C) 2011, 2022 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.DeleteBranchOperationUI;
import org.eclipse.egit.ui.internal.dialogs.BranchSelectionDialog;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

/**
 * Delete branches pointing to a commit.
 */
public class DeleteBranchOnCommitHandler extends AbstractHistoryCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(event);
		if (repository == null) {
			return null;
		}
		IStructuredSelection selection = getSelection(event);

		int totalBranchCount;
		List<Ref> branchesOfCommit;
		try {
			totalBranchCount = getBranchesOfCommit(selection, repository, false)
					.size();
			branchesOfCommit = getBranchesOfCommit(selection, repository, true);
		} catch (IOException e) {
			throw new ExecutionException(
					UIText.AbstractHistoryCommitHandler_cantGetBranches,
					e);
		}
		// this should have been checked by isEnabled()
		if (branchesOfCommit.isEmpty()) {
			return null;
		}
		final Shell shell = getPart(event).getSite().getShell();

		final List<Ref> branchesToDelete;
		// we will show the dialog if there are either multiple branches that might be
		// deleted or if one of the branches is the current head (which we can't delete);
		// in the latter case, we may show the dialog even if there is only one branch to
		// delete instead of quietly deleting an unexpected one, for example a remote
		// tracking branch
		if (totalBranchCount > 1) {
			BranchSelectionDialog<Ref> dlg = new BranchSelectionDialog<>(
					shell,
					branchesOfCommit,
					UIText.DeleteBranchOnCommitHandler_DeleteBranchesDialogTitle,
					UIText.DeleteBranchOnCommitHandler_DeleteBranchesDialogMessage,
					UIText.DeleteBranchOnCommitHandler_DeleteBranchesDialogButton,
					SWT.MULTI);
			if (dlg.open() != Window.OK) {
				return null;
			}
			branchesToDelete = dlg.getSelectedNodes();
		} else {
			branchesToDelete = branchesOfCommit;
		}

		if (!branchesToDelete.isEmpty()) {
			try {
				DeleteBranchOperationUI.deleteBranches(repository,
						branchesToDelete);
			} catch (InvocationTargetException e) {
				throw new ExecutionException(
						UIText.RepositoriesView_BranchDeletionFailureMessage,
						e.getCause());
			}
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		GitHistoryPage page = getPage();

		Repository repository = getRepository(page);
		if (repository == null)
			return false;

		List<Ref> branchesOfCommit;
		try {
			branchesOfCommit = getBranchesOfCommit(getSelection(page),
					repository, true);
		} catch (IOException e) {
			Activator.logError(
					UIText.AbstractHistoryCommitHandler_cantGetBranches,
					e);
			return false;
		}
		return !branchesOfCommit.isEmpty();
	}
}
