/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christian Halstrick (SAP AG) - initial implementation
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Robin Rosenberg - Adoption for the history menu
 *******************************************************************************/

package org.eclipse.egit.ui.internal.history.command;

import java.io.IOException;
import java.util.List;

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
import org.eclipse.egit.ui.internal.actions.MergeActionHandler;
import org.eclipse.egit.ui.internal.dialogs.BranchSelectionDialog;
import org.eclipse.egit.ui.internal.merge.MergeResultDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Executes the Merge
 */
public class MergeHandler extends AbstractHistoryCommandHandler {

	@Override
	public boolean isEnabled() {
		final Repository repository = getRepository(getPage());
		if (repository == null)
			return false;
		return repository.getRepositoryState().equals(RepositoryState.SAFE);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ObjectId commitId = getSelectedCommitId(event);
		final Repository repository = getRepository(event);
		if (repository == null)
			return null;

		if (!MergeActionHandler.checkMergeIsPossible(repository, getShell(event)))
			return null;

		List<Ref> nodes;
		try {
			nodes = getBranchesOfCommit(getSelection(event), repository, true);
		} catch (IOException e) {
			throw new ExecutionException(
					UIText.AbstractHistoryCommitHandler_cantGetBranches,
					e);
		}

		MergeStrategy mergeStrategy = org.eclipse.egit.core.Activator.getDefault().getPreferredMergeStrategy();
		String refName;
		if (nodes.isEmpty()) {
			refName = commitId.getName();
		} else if (nodes.size() == 1) {
			refName = nodes.get(0).getName();
		} else {
			BranchSelectionDialog<Ref> dlg = new BranchSelectionDialog<>(
					HandlerUtil.getActiveShellChecked(event), nodes,
					UIText.MergeHandler_SelectBranchTitle,
					UIText.MergeHandler_SelectBranchMessage, SWT.SINGLE, true);
			if (dlg.open() == Window.OK) {
				refName = dlg.getSelectedNode().getName();
				mergeStrategy = dlg.getSelectedStrategy();
			} else {
				return null;
			}
		}
		String jobname = NLS.bind(UIText.MergeAction_JobNameMerge, refName);
		final MergeOperation op = new MergeOperation(repository, refName);
		op.setMergeStrategy(mergeStrategy);
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
		return null;
	}

	/**
	 * @param event
	 * @return the shell for the event
	 */
	public Shell getShell(ExecutionEvent event) {
		return HandlerUtil.getActiveShell(event);
	}

}
