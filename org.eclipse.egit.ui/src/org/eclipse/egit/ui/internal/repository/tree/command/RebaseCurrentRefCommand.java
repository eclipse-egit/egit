/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.dialogs.RebaseTargetSelectionDialog;
import org.eclipse.egit.ui.internal.rebase.RebaseResultDialog;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;

/**
 * Implements "Rebase" to the currently checked out {@link Ref}
 */
public class RebaseCurrentRefCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {

		RepositoryTreeNode node = getSelectedNodes(event).get(0);

		final Repository repository = node.getRepository();

		Ref ref;
		if (node instanceof RefNode)
			ref = ((RefNode) node).getObject();
		else {
			RebaseTargetSelectionDialog rebaseTargetSelectionDialog = new RebaseTargetSelectionDialog(
					getShell(event), repository);
			if (rebaseTargetSelectionDialog.open() == IDialogConstants.OK_ID) {
				String refName = rebaseTargetSelectionDialog.getRefName();
				try {
					ref = repository.getRef(refName);
				} catch (IOException e) {
					throw new ExecutionException(e.getMessage(), e);
				}
			} else
				return null;
		}

		String jobname = NLS.bind(
				UIText.RebaseCurrentRefCommand_RebasingCurrentJobName, ref
						.getName());
		RevWalk rw = new RevWalk(repository);
		RevCommit commit;
		try {
			commit = rw.parseCommit(ref.getObjectId());
		} catch (MissingObjectException e) {
			throw new ExecutionException("Failed to resolve upstream " + ref, e); //$NON-NLS-1$ FIXME
		} catch (IncorrectObjectTypeException e) {
			throw new ExecutionException("Rebase failed " + ref, e); //$NON-NLS-1$ FIXME
		} catch (IOException e) {
			throw new ExecutionException("Rebase failed " + ref, e); //$NON-NLS-1$ FIXME
		}
		final RebaseOperation rebase = new RebaseOperation(repository, commit);
		Job job = new Job(jobname) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					rebase.execute(monitor);
				} catch (final CoreException e) {
					try {
						new RebaseOperation(repository, Operation.ABORT)
								.execute(monitor);
					} catch (CoreException e1) {
						return e1.getStatus();
					}
					return e.getStatus();
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.setRule(rebase.getSchedulingRule());
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent cevent) {
				IStatus result = cevent.getJob().getResult();
				if (result.getSeverity() == IStatus.CANCEL) {
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							// don't use getShell(event) here since
							// the active shell has changed since the
							// execution has been triggered.
							Shell shell = PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow().getShell();
							MessageDialog
									.openInformation(
											shell,
											UIText.RebaseCurrentRefCommand_RebaseCanceledTitle,
											UIText.RebaseCurrentRefCommand_RebaseCanceledMessage);
						}
					});
				} else if (result.isOK()) {
					RebaseResultDialog.show(rebase.getResult(), repository);
				}
			}
		});
		job.schedule();
		return null;
	}

	@Override
	public void setEnabled(Object evaluationContext) {
		if (evaluationContext instanceof IEvaluationContext) {
			IEvaluationContext ctx = (IEvaluationContext) evaluationContext;
			Object selection = ctx
					.getVariable(ISources.ACTIVE_MENU_SELECTION_NAME);
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection sel = (IStructuredSelection) selection;
				if (sel.getFirstElement() instanceof RefNode)
					setBaseEnabled(((RefNode) ((IStructuredSelection) selection)
							.getFirstElement()).getRepository()
							.getRepositoryState() == RepositoryState.SAFE);
				return;
			}
		}
		setBaseEnabled(true);
	}
}
