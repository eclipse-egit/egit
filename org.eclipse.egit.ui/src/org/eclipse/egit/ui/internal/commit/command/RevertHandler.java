/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tomasz Zarna (IBM) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit.command;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.CommitUtil;
import org.eclipse.egit.core.internal.IRepositoryCommit;
import org.eclipse.egit.core.op.RevertCommitOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.BasicConfigurationDialog;
import org.eclipse.egit.ui.internal.dialogs.RevertFailureDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Executes the {@link RevertCommitOperation}
 */
public class RevertHandler extends CommitCommandHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		List<IRepositoryCommit> repoCommits = getCommits(event);
		Repository repo = repoCommits.get(0).getRepository();
		// Sanity checking: all commits from the same repository, which is not
		// bare
		if (repo.isBare()) {
			return null;
		}
		if (repoCommits.stream().anyMatch(c -> repo != c.getRepository())) {
			return null;
		}

		List<RevCommit> commits = repoCommits.stream()
				.map(IRepositoryCommit::getRevCommit)
				.collect(Collectors.toList());

		try {
			if (!CommitUtil.areCommitsInCurrentBranch(commits, repo)) {
				MessageDialog.openError(
						HandlerUtil.getActiveShellChecked(event),
						UIText.RevertHandler_Error_Title,
						UIText.RevertHandler_CommitsNotOnCurrentBranch);
				return null;
			}
		} catch (IOException e) {
			throw new ExecutionException(
					UIText.RevertHandler_ErrorCheckingIfCommitsAreOnCurrentBranch,
					e);
		}

		BasicConfigurationDialog.show(repo);

		final RevertCommitOperation op = new RevertCommitOperation(repo,
				commits);

		Job job = new WorkspaceJob(MessageFormat.format(
				UIText.RevertHandler_JobName, Integer.valueOf(commits.size()))) {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				try {
					op.execute(monitor);
					RevCommit newHead = op.getNewHead();
					List<Ref> revertedRefs = op.getRevertedRefs();
					if (newHead != null) {
						if (revertedRefs.isEmpty()) {
							showRevertedDialog();
						}
					} else {
						RevCommit newestUnmergedCommit = null;
						for (RevCommit commit : commits) {
							if (!contains(revertedRefs, commit)) {
								newestUnmergedCommit = commit;
								break;
							}
						}
						showFailureDialog(newestUnmergedCommit,
								op.getFailingResult());
					}
				} catch (CoreException e) {
					Activator.handleError(UIText.RevertOperation_InternalError,
							e, true);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return JobFamilies.REVERT_COMMIT.equals(family)
						|| super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.setRule(op.getSchedulingRule());
		job.schedule();
		return null;
	}

	private boolean contains(List<Ref> refs, RevCommit commit) {
		for (Ref ref : refs) {
			ObjectId objectId = ref.getObjectId();
			if (objectId != null && objectId.equals(commit.getId())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Shows a dialog with failing result of revert.
	 *
	 * @param commit
	 *            the commit
	 * @param result
	 *            the failing result
	 */
	private static void showFailureDialog(RevCommit commit,
			MergeResult result) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			RevertFailureDialog.show(PlatformUI.getWorkbench()
					.getModalDialogShellProvider().getShell(), commit, result);
		});
	}

	/**
	 * Shows the "No revert performed" dialog.
	 */
	private static void showRevertedDialog() {
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			MessageDialog.openWarning(
					PlatformUI.getWorkbench().getModalDialogShellProvider()
							.getShell(),
					UIText.RevertHandler_NoRevertTitle,
					UIText.RevertHandler_AlreadyRevertedMessage);
		});
	}
}
