/*******************************************************************************
 * Copyright (c) 2010, 2016 SAP AG and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christian Halstrick (SAP AG) - initial implementation
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Robin Rosenberg - Adoption for the history menu
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 495777
 *******************************************************************************/

package org.eclipse.egit.ui.internal.history.command;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.op.MergeOperation;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.actions.MergeActionHandler;
import org.eclipse.egit.ui.internal.branch.LaunchFinder;
import org.eclipse.egit.ui.internal.dialogs.BranchSelectionDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Executes the Merge
 */
public class MergeHandler extends AbstractHistoryCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ObjectId commitId = getSelectedCommitId(event);
		Repository repository = getRepository(event);
		if (repository == null) {
			return null;
		}
		Shell shell = HandlerUtil.getActiveShellChecked(event);

		if (!MergeActionHandler.checkMergeIsPossible(repository, shell)
				|| LaunchFinder.shouldCancelBecauseOfRunningLaunches(repository,
						null)) {
			return null;
		}

		List<Ref> nodes;
		try {
			nodes = getBranchesOfCommit(getSelection(event), repository, true);
		} catch (IOException e) {
			throw new ExecutionException(
					UIText.AbstractHistoryCommitHandler_cantGetBranches,
					e);
		}

		String refName;
		if (nodes.isEmpty()) {
			refName = commitId.getName();
		} else if (nodes.size() == 1) {
			refName = nodes.get(0).getName();
		} else {
			BranchSelectionDialog<Ref> dlg = new BranchSelectionDialog<>(
					shell, nodes,
					UIText.MergeHandler_MergeBranchDialogTitle,
					UIText.MergeHandler_MergeBranchDialogMessage,
					UIText.MergeHandler_MergeBranchDialogButton, SWT.SINGLE);
			if (dlg.open() == Window.OK) {
				refName = dlg.getSelectedNode().getName();
			} else {
				return null;
			}
		}
		MergeOperation op = new MergeOperation(repository, refName);
		MergeActionHandler.doMerge(repository, op, refName);
		return null;
	}

	@Override
	public boolean isEnabled() {
		Repository repository = getRepository(getPage());
		return repository != null
				&& repository.getRepositoryState().equals(RepositoryState.SAFE);
	}

}
