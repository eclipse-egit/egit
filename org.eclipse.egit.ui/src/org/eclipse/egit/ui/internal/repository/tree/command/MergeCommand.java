/*******************************************************************************
 * Copyright (c) 2010, 2016 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Mathias Kinzler (SAP AG) - move to command framework
 *    Dariusz Luksza (dariusz@luksza.org - set action disabled when HEAD cannot
 *    										be resolved
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 495777
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

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
import org.eclipse.egit.ui.internal.actions.MergeActionHandler;
import org.eclipse.egit.ui.internal.branch.LaunchFinder;
import org.eclipse.egit.ui.internal.dialogs.BasicConfigurationDialog;
import org.eclipse.egit.ui.internal.dialogs.MergeTargetSelectionDialog;
import org.eclipse.egit.ui.internal.merge.MergeResultDialog;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Implements "Merge"
 */
public class MergeCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		RepositoryTreeNode node = getSelectedNodes(event).get(0);
		final Repository repository = node.getRepository();

		if (!MergeActionHandler.checkMergeIsPossible(repository,
				getShell(event))
				|| LaunchFinder.shouldCancelBecauseOfRunningLaunches(repository,
						null)) {
			return null;
		}
		BasicConfigurationDialog.show(repository);

		String targetRef;
		if (node instanceof RefNode) {
			String refName = ((RefNode) node).getObject().getName();
			try {
				if (refName.equals(repository.getFullBranch()))
					targetRef = null;
				else
					targetRef = refName;
			} catch (IOException e) {
				targetRef = null;
			}
		} else if (node instanceof TagNode)
			targetRef = ((TagNode) node).getObject().getName();
		else
			targetRef = null;

		final String refName;
		final MergeOperation op;

		if (targetRef != null) {
			refName = targetRef;
			op = new MergeOperation(repository, refName);
		} else {
			MergeTargetSelectionDialog mergeTargetSelectionDialog = new MergeTargetSelectionDialog(
					getShell(event), repository);
			if (mergeTargetSelectionDialog.open() != IDialogConstants.OK_ID)
				return null;

			refName = mergeTargetSelectionDialog.getRefName();
			op = new MergeOperation(repository, refName);
			op.setSquash(mergeTargetSelectionDialog.isMergeSquash());
			op.setFastForwardMode(mergeTargetSelectionDialog
					.getFastForwardMode());
			op.setCommit(mergeTargetSelectionDialog.isCommit());
		}

		String jobname = NLS.bind(UIText.MergeAction_JobNameMerge, refName);
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
			public void done(IJobChangeEvent jobEvent) {
				IStatus result = jobEvent.getJob().getResult();
				if (result.getSeverity() == IStatus.CANCEL)
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							// don't use getShell(event) here since
							// the active shell has changed since the
							// execution has been triggered.
							Shell shell = PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow().getShell();
							MessageDialog.openInformation(shell,
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
							MergeResultDialog.getDialog(shell, repository, op.getResult()).open();
						}
					});
			}
		});
		job.schedule();

		return null;
	}

	@Override
	public boolean isEnabled() {
		return selectedRepositoryHasHead();
	}

}
