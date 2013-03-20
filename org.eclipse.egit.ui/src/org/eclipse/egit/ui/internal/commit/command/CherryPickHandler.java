/******************************************************************************
 *  Copyright (c) 2010 SAP AG.
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.commit.command;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.op.CherryPickOperation;
import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.handler.SelectionHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.CherryPickResult;
import org.eclipse.jgit.api.CherryPickResult.CherryPickStatus;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Handler to cherry pick the commit onto the current branch
 */
public class CherryPickHandler extends SelectionHandler {

	/**
	 * Command id
	 */
	public static final String ID = "org.eclipse.egit.ui.commit.CherryPick"; //$NON-NLS-1$

	public Object execute(ExecutionEvent event) throws ExecutionException {
		RevCommit commit = getSelectedItem(RevCommit.class, event);
		if (commit == null)
			return null;
		Repository repo = getSelectedItem(Repository.class, event);
		if (repo == null)
			return null;
		final Shell parent = getPart(event).getSite().getShell();

		if (!confirmCherryPick(parent, repo, commit))
			return null;

		final CherryPickOperation op = new CherryPickOperation(repo, commit);

		Job job = new Job(MessageFormat.format(
				UIText.CherryPickHandler_JobName, commit.name())) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					op.execute(monitor);
					CherryPickResult cherryPickResult = op.getResult();
					RevCommit newHead = cherryPickResult.getNewHead();
					if (newHead != null
							&& cherryPickResult.getCherryPickedRefs().isEmpty())
						showNotPerformedDialog(parent);
					if (newHead == null) {
						CherryPickStatus status = cherryPickResult.getStatus();
						switch (status) {
						case CONFLICTING:
							showConflictDialog(parent);
							break;
						case FAILED:
							showFailure(cherryPickResult);
							break;
						case OK:
							break;
						}
					}
				} catch (CoreException e) {
					Activator.logError(
							UIText.CherryPickOperation_InternalError, e);
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.CHERRY_PICK.equals(family))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.setRule(op.getSchedulingRule());
		job.schedule();
		return null;
	}

	private boolean confirmCherryPick(final Shell shell,
			final Repository repository, final RevCommit commit)
			throws ExecutionException {
		final AtomicBoolean confirmed = new AtomicBoolean(false);
		final String message;
		try {
			message = MessageFormat.format(
					UIText.CherryPickHandler_ConfirmMessage,
					commit.abbreviate(7).name(), repository.getBranch());
		} catch (IOException e) {
			throw new ExecutionException(
					"Exception obtaining current repository branch", e); //$NON-NLS-1$
		}

		shell.getDisplay().syncExec(new Runnable() {

			public void run() {
				confirmed.set(MessageDialog.openConfirm(shell,
						UIText.CherryPickHandler_ConfirmTitle, message));
			}
		});
		return confirmed.get();
	}

	private void showNotPerformedDialog(final Shell shell) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			public void run() {
				MessageDialog.openWarning(shell,
						UIText.CherryPickHandler_NoCherryPickPerformedTitle,
						UIText.CherryPickHandler_NoCherryPickPerformedMessage);
			}
		});
	}

	private void showConflictDialog(final Shell shell) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			public void run() {
				MessageDialog.openWarning(shell,
						UIText.CherryPickHandler_CherryPickConflictsTitle,
						UIText.CherryPickHandler_CherryPickConflictsMessage);
			}
		});
	}

	private void showFailure(CherryPickResult result) {
		IStatus details = getErrorList(result.getFailingPaths());
		Activator.showErrorStatus(
				UIText.CherryPickHandler_CherryPickFailedMessage, details);
	}

	private IStatus getErrorList(Map<String, MergeFailureReason> failingPaths) {
		MultiStatus result = new MultiStatus(Activator.getPluginId(),
				IStatus.ERROR,
				UIText.CherryPickHandler_CherryPickFailedMessage, null);
		for (Entry<String, MergeFailureReason> entry : failingPaths.entrySet()) {
			String path = entry.getKey();
			String reason = getReason(entry.getValue());
			String errorMessage = NLS.bind(
					UIText.CherryPickHandler_ErrorMsgTemplate, path, reason);
			result.add(Activator.createErrorStatus(errorMessage));
		}
		return result;
	}

	private String getReason(MergeFailureReason mergeFailureReason) {
		switch (mergeFailureReason) {
		case COULD_NOT_DELETE:
			return UIText.CherryPickHandler_CouldNotDeleteFile;
		case DIRTY_INDEX:
			return UIText.CherryPickHandler_IndexDirty;
		case DIRTY_WORKTREE:
			return UIText.CherryPickHandler_WorktreeDirty;
		}
		return UIText.CherryPickHandler_unknown;
	}
}
