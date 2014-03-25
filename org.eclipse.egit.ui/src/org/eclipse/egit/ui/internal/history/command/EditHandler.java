/*******************************************************************************
 *  Copyright (c) 2014 Maik Schreiber
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Maik Schreiber - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.CommitUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.rebase.RebaseInteractiveView;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

/** Checks out a commit (in interactive rebase mode) for editing. */
public class EditHandler extends AbstractHistoryCommandHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(event);
		RevCommit commit = (RevCommit) getSelection(getPage())
				.getFirstElement();

		try {
			if (!CommitUtil.isCommitInCurrentBranch(commit, repository)) {
				MessageDialog.openError(getPage().getSite().getShell(),
						UIText.EditHandler_Error_Title,
						UIText.EditHandler_CommitNotOnCurrentBranch);
				return null;
			}
		} catch (IOException e) {
			throw new ExecutionException(
					UIText.EditHandler_ErrorCheckingIfCommitIsOnCurrentBranch,
					e);
		}

		final IStructuredSelection selected = new StructuredSelection(
				new RepositoryCommit(repository, commit));
		CommonUtils.runCommand(
				org.eclipse.egit.ui.internal.commit.command.EditHandler.ID,
				selected);

		IWorkbenchPage workbenchPage = getPage().getSite().getPage();
		try {
			final StagingView stagingView = (StagingView) workbenchPage
					.showView(StagingView.VIEW_ID);
			stagingView.reload(repository);
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					stagingView.setAmending(true);
				}
			});

			RebaseInteractiveView rebaseView = (RebaseInteractiveView) workbenchPage
					.showView(RebaseInteractiveView.VIEW_ID);
			rebaseView.setInput(repository);
		} catch (PartInitException e) {
			Activator.logError(e.getMessage(), e);
		}

		return null;
	}

	@Override
	public boolean isEnabled() {
		GitHistoryPage page = getPage();
		if (page == null)
			return false;
		IStructuredSelection selection = getSelection(page);
		if (selection.size() != 1)
			return false;
		Repository repository = getRepository(page);
		if (repository.getRepositoryState() != RepositoryState.SAFE)
			return false;
		RevCommit commit = (RevCommit) selection.getFirstElement();
		return (commit.getParentCount() == 1);
	}
}
