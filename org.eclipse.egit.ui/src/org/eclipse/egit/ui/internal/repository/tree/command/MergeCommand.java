/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Mathias Kinzler (SAP AG) - move to command framework
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.op.MergeOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.dialogs.MergeTargetSelectionDialog;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

/**
 * Implements "Merge"
 */
public class MergeCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	public Object execute(final ExecutionEvent event) throws ExecutionException {

		RepositoryTreeNode node = getSelectedNodes(event).get(0);

		final FileRepository repository = node.getRepository();

		if (!canMerge(repository))
			return null;

		String targetRef;
		if (node instanceof RefNode) {
			String refName = ((RefNode) node).getObject().getName();
			try {
				if (repository.getFullBranch().equals(refName))
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
		if (targetRef != null)
			refName = targetRef;
		else {
			MergeTargetSelectionDialog mergeTargetSelectionDialog = new MergeTargetSelectionDialog(
					getShell(event), repository);
			if (mergeTargetSelectionDialog.open() == IDialogConstants.OK_ID) {
				refName = mergeTargetSelectionDialog.getRefName();
			} else {
				return null;
			}
		}

		String jobname = NLS.bind(UIText.MergeAction_JobNameMerge, refName);
		final MergeOperation op = new MergeOperation(repository, refName);
		Job job = new Job(jobname) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					op.execute(monitor);
				} catch (final CoreException e) {
					return e.getStatus();
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent jobEvent) {
				IStatus result = jobEvent.getJob().getResult();
				if (result.getSeverity() == IStatus.CANCEL) {
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							try {
								MessageDialog
										.openInformation(
												getActiveShell(event),
												UIText.MergeAction_MergeCanceledTitle,
												UIText.MergeAction_MergeCanceledMessage);
							} catch (ExecutionException e) {
								Activator
										.handleError(
												UIText.MergeAction_MergeCanceledMessage,
												null, true);
							}
						}
					});
				} else if (!result.isOK()) {
					Activator.handleError(result.getMessage(), result
							.getException(), true);
				} else {
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							try {
								MessageDialog.openInformation(
										getActiveShell(event),
										UIText.MergeAction_MergeResultTitle, op
												.getResult().toString());
							} catch (ExecutionException e) {
								Activator.handleError(
										op.getResult().toString(), null, true);
							}
						}
					});
				}
			}
		});
		job.schedule();

		return null;
	}

	private boolean canMerge(final Repository repository) {
		String message = null;
		Exception ex = null;
		try {
			Ref head = repository.getRef(Constants.HEAD);
			if (head == null || !head.isSymbolic())
				message = UIText.MergeAction_HeadIsNoBranch;
			else if (!repository.getRepositoryState().equals(
					RepositoryState.SAFE))
				message = NLS.bind(UIText.MergeAction_WrongRepositoryState,
						repository.getRepositoryState());
		} catch (IOException e) {
			message = e.getMessage();
			ex = e;
		}

		if (message != null) {
			Activator.handleError(UIText.MergeAction_CannotMerge, ex, true);
		}
		return (message == null);
	}
}
