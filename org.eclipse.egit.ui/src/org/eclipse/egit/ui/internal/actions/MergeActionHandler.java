/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/

package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.op.MergeOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.BasicConfigurationDialog;
import org.eclipse.egit.ui.internal.dialogs.MergeTargetSelectionDialog;
import org.eclipse.egit.ui.internal.merge.MergeResultDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Action for selecting a commit and merging it with the current branch.
 */
public class MergeActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(true, event);
		if (repository == null)
			return null;

		if (!checkMergeIsPossible(repository, getShell(event)))
			return null;
		BasicConfigurationDialog.show(repository);
		MergeTargetSelectionDialog mergeTargetSelectionDialog = new MergeTargetSelectionDialog(
				getShell(event), repository);
		if (mergeTargetSelectionDialog.open() == IDialogConstants.OK_ID) {

			final String refName = mergeTargetSelectionDialog.getRefName();

			String jobname = NLS.bind(UIText.MergeAction_JobNameMerge, refName);
			final MergeOperation op = new MergeOperation(repository, refName);
			op.setSquash(mergeTargetSelectionDialog.isMergeSquash());
			op.setFastForwardMode(mergeTargetSelectionDialog.getFastForwardMode());
			op.setCommit(mergeTargetSelectionDialog.isCommit());
			op.setMergeStrategy(mergeTargetSelectionDialog.getMergeStrategy());
			Job job = new WorkspaceJob(jobname) {

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) {
					try {
						op.execute(monitor);
					} catch (final CoreException e) {
						return e.getStatus();
					}
					return Status.OK_STATUS;
				}
			};
			job.setUser(true);
			job.setRule(op.getSchedulingRule());
			job.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent cevent) {
					IStatus result = cevent.getJob().getResult();
					if (result.getSeverity() == IStatus.CANCEL)
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								// don't use getShell(event) here since
								// the active shell has changed since the
								// execution has been triggered.
								Shell shell = PlatformUI.getWorkbench()
										.getActiveWorkbenchWindow().getShell();
								MessageDialog
										.openInformation(
												shell,
												UIText.MergeAction_MergeCanceledTitle,
												UIText.MergeAction_MergeCanceledMessage);
							}
						});
					else if (!result.isOK())
						Activator.handleError(result.getMessage(), result
								.getException(), true);
					else
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								Shell shell = PlatformUI.getWorkbench()
										.getActiveWorkbenchWindow().getShell();
								MergeResultDialog.getDialog(shell, repository, op
										.getResult()).open();
							}
						});
				}
			});
			job.schedule();
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		Repository repo = getRepository();
		return repo != null
				&& repo.getRepositoryState() == RepositoryState.SAFE
				&& isLocalBranchCheckedout(repo);
	}

	/**
	 * Checks if merge is possible:
	 * <ul>
	 * <li>HEAD must point to a branch</li>
	 * <li>Repository State must be SAFE</li>
	 * </ul>
	 * Shows an error dialog if a merge is not possible.
	 *
	 * @param repository
	 *            the repository used for the merge
	 * @param shell
	 *            used to show a dialog in the error case
	 * @return true if a merge is possible on the current HEAD
	 */
	public static boolean checkMergeIsPossible(Repository repository, Shell shell) {
		String message = null;
		try {
			Ref head = repository.exactRef(Constants.HEAD);
			if (head == null || !head.isSymbolic())
				message = UIText.MergeAction_HeadIsNoBranch;
			else if (!repository.getRepositoryState().equals(
					RepositoryState.SAFE))
				message = NLS.bind(UIText.MergeAction_WrongRepositoryState,
						repository.getRepositoryState());
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
			message = e.getMessage();
		}

		if (message != null)
			MessageDialog.openError(shell, UIText.MergeAction_CannotMerge, message);
		return (message == null);
	}
}
