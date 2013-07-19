/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Robin Rosenberg - Refactoring
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commands.shared.AbstractRebaseCommandHandler;
import org.eclipse.egit.ui.internal.commands.shared.AbstractRebaseCommandHandler.RebaseCommandFinishedListener;
import org.eclipse.egit.ui.internal.dialogs.ModifyCommitMessageInteractiveHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Shared functionality for rebase operations
 */
public class RebaseHelper {

	/**
	 * The default Message to be shown when the rebase is not successful and got
	 * canceled
	 */
	public static final String DEFAULT_CANCEL_DIALOG_MESSAGE = UIText.RebaseCurrentRefCommand_RebaseCanceledMessage;

	/**
	 * {@link RebaseHelper#runRebaseJob(Repository, String, Ref, Operation, boolean, String, List)
	 * runRebaseJob(Repository, String, Ref, RebaseCommand.Operation.BEGIN,
	 * false, String, null)}
	 *
	 * @param repository
	 * @param jobname
	 * @param ref
	 * @param dialogMessage
	 */
	public static void runRebaseJob(final Repository repository,
			String jobname, Ref ref, final String dialogMessage) {
		runRebaseJob(repository, jobname, ref, Operation.BEGIN, false,
				dialogMessage, null);
	}

	/**
	 * Shared implementation to be called from an implementation of
	 * {@link org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)}
	 * <p>
	 * Perform a rebase operation, moving the commits from the branch tip
	 * <code>commit</code> onto the currently checked out branch. The actual
	 * operation is deferred to a {@link RebaseOperation} executed as a
	 * {@link Job}.
	 * <p>
	 * If interactive is true, the rebase will be initialized in interactive
	 * mode. The rebase will return after the rebase interactive has been
	 * initialized.
	 *
	 * @param repository
	 * @param jobname
	 * @param ref
	 * @param operation
	 *            the operation to be performed
	 * @param interactive
	 *            runs interactively if true
	 * @param dialogMessage
	 * @param listeners
	 *            list of RebaseCommandFinishedListener to be notified when
	 *            command finished
	 */
	public static void runRebaseJob(final Repository repository,
			String jobname, final Ref ref, final Operation operation,
			boolean interactive,
			final String dialogMessage,
			final List<RebaseCommandFinishedListener> listeners) {

		final RebaseOperation rebase;

		if (operation == Operation.BEGIN) {
			if (ref == null)
				return; // throw RuntimeException instead?
			if (interactive) {
				InteractiveHandler handler = new ModifyCommitMessageInteractiveHandler(
						repository);
				rebase = new RebaseOperation(repository, ref, handler);
			} else {
				rebase = new RebaseOperation(repository, ref);
			}
		} else if (interactive) {
			InteractiveHandler handler = new ModifyCommitMessageInteractiveHandler(
					repository);
			rebase = new RebaseOperation(repository, operation, handler);
		} else {
			rebase = new RebaseOperation(repository, operation);
		}

		Job job = new Job(jobname) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					rebase.execute(monitor);
				} catch (final CoreException e) {
					if (!repository.getRepositoryState().equals(
							RepositoryState.SAFE)) {
						try {
							new RebaseOperation(repository, Operation.ABORT)
									.execute(monitor);
						} catch (CoreException e1) {
							return createMultiStatus(e, e1);
						}
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
											dialogMessage);
						}
					});
				} else if (result.isOK()) {
					RebaseResultDialog.show(rebase.getResult(), repository);
				}
				AbstractRebaseCommandHandler.fireRebaseCommandFinished(result,
						repository, ref, operation);
			}
		});
		job.schedule();
	}

	private static IStatus createMultiStatus(CoreException originalException,
			CoreException e) {
		IStatus childStatus = Activator.createErrorStatus(
				originalException.getMessage(), originalException);
		return new MultiStatus(Activator.getPluginId(), IStatus.ERROR,
				new IStatus[] { childStatus }, e.getMessage(), e);
	}

}