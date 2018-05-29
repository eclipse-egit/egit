/*******************************************************************************
 *  Copyright (c) 2014 Maik Schreiber and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Maik Schreiber - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.CommitUtil;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.rebase.RebaseInteractiveView;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.UIJob;

/** Checks out a commit (in interactive rebase mode) for editing. */
public class EditHandler extends AbstractHistoryCommandHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(event);
		final RevCommit commit = getSelectedCommit(event);
		final Shell shell = HandlerUtil.getActiveShellChecked(event);

		try {
			if (!CommitUtil.isCommitInCurrentBranch(commit, repository)) {
				MessageDialog.openError(shell,
						UIText.EditHandler_Error_Title,
						UIText.EditHandler_CommitNotOnCurrentBranch);
				return null;
			}
		} catch (IOException e) {
			throw new ExecutionException(
					UIText.EditHandler_ErrorCheckingIfCommitIsOnCurrentBranch,
					e);
		}

		boolean success = org.eclipse.egit.ui.internal.commit.command.EditHandler
				.editCommit(commit, repository, shell);
		if (success)
			openStagingAndRebaseInteractiveViews(repository);

		return null;
	}

	private void openStagingAndRebaseInteractiveViews(final Repository repository) {
		Job job = new UIJob(UIText.EditHandler_OpenStagingAndRebaseInteractiveViews) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				try {
					IWorkbenchPage workbenchPage = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage();
					final StagingView stagingView = (StagingView) workbenchPage
							.showView(StagingView.VIEW_ID);
					stagingView.reload(repository);
					stagingView.setAmending(true);
					RebaseInteractiveView rebaseView = (RebaseInteractiveView) workbenchPage
							.showView(RebaseInteractiveView.VIEW_ID);
					rebaseView.setInput(repository);
				} catch (PartInitException e) {
					Activator.logError(e.getMessage(), e);
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(RuleUtil.getRule(repository));
		job.setUser(true);
		job.schedule();
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
