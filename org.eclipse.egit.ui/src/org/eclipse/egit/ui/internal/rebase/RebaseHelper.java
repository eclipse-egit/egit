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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.internal.op.RebaseOperation;
import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.MessageDialog;
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
	 * Shared implementation to be called from an implementation of
	 * {@link org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)}
	 * <p>
	 * Perform a rebase operation, moving the commits from the branch tip <code>commit</code> onto the
	 * currently checked out branch. The actual operation is deferred to a {@link RebaseOperation} executed
	 * as a {@link Job}.
	 * <p>
	 * @param repository
	 * @param jobname
	 * @param ref
	 */
	public static void runRebaseJob(final Repository repository, String jobname,
			Ref ref) {
		final RebaseOperation rebase = new RebaseOperation(repository, ref);
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
											UIText.RebaseCurrentRefCommand_RebaseCanceledMessage);
						}
					});
				} else if (result.isOK()) {
					RebaseResultDialog.show(rebase.getResult(), repository);
				}
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