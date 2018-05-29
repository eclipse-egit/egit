/*******************************************************************************
 * Copyright (c) 2016, 2017 Thomas Wolf <thomas.wolf@paranor.ch>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.jobs;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.action.IAction;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * A {@link Job} operating (solely) on a repository, reporting some result
 * beyond a mere {@link IStatus} back to the user via an {@link IAction}. If a
 * dialog preference is given, the result dialog is shown only if it is
 * {@code true}, otherwise the job's {@link IProgressConstants#ACTION_PROPERTY}
 * is used to associate the action with the finished job and eventual display of
 * the result is left to the progress reporting framework.
 */
public abstract class RepositoryJob extends Job {

	private final String dialogPreference;

	/**
	 * Creates a new {@link RepositoryJob}.
	 *
	 * @param name
	 *            of the job.
	 * @param dialogPreference
	 *            key of the preference governing the showing of the result
	 *            dialog on success; may be {@code null} if the dialog shall be
	 *            shown unconditionally
	 */
	public RepositoryJob(String name, String dialogPreference) {
		super(name);
		this.dialogPreference = dialogPreference;
	}

	@Override
	protected final IStatus run(IProgressMonitor monitor) {
		try {
			IStatus status = performJob(monitor);
			if (status == null) {
				return Activator.createErrorStatus(MessageFormat
						.format(UIText.RepositoryJob_NullStatus, getName()),
						new NullPointerException());
			} else if (!status.isOK()) {
				return status;
			}
			IAction action = getAction();
			if (action != null) {
				boolean showDialog = dialogPreference == null
						|| Activator.getDefault().getPreferenceStore()
								.getBoolean(dialogPreference);
				if (showDialog) {
					if (isModal()) {
						showResult(action);
					} else {
						showResultDeferred(action);
					}
				} else {
					setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
					setProperty(IProgressConstants.ACTION_PROPERTY, action);
					IStatus finalStatus = getDeferredStatus();
					String msg = finalStatus.getMessage();
					if (msg == null || msg.isEmpty()) {
						return new Status(finalStatus.getSeverity(),
								finalStatus.getPlugin(), finalStatus.getCode(),
								action.getText(), finalStatus.getException());
					}
					return finalStatus;
				}
			}
			return status;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Performs the actual work of the job.
	 *
	 * @param monitor
	 *            for progress reporting and cancellation.
	 * @return an {@link IStatus} describing the outcome of the job
	 */
	abstract protected IStatus performJob(IProgressMonitor monitor);

	/**
	 * Obtains an {@link IAction} to report the full job result if
	 * {@link #performJob(IProgressMonitor)} returned an {@link IStatus#isOK()
	 * isOK()} status.
	 *
	 * @return the action, or {@code null} if no action is to be taken
	 */
	abstract protected IAction getAction();

	/**
	 * Obtains an {@link IStatus} describing the final outcome of the operation.
	 * This default implementation returns an {@link IStatus#OK OK} status.
	 *
	 * @return an {@link IStatus} describing the outcome of the job
	 */
	@NonNull
	protected IStatus getDeferredStatus() {
		return new Status(IStatus.OK, Activator.getPluginId(), IStatus.OK, "", //$NON-NLS-1$
				null);
	}

	private boolean isModal() {
		Boolean modal = (Boolean) getProperty(
				IProgressConstants.PROPERTY_IN_DIALOG);
		return modal != null && modal.booleanValue();
	}

	private void showResult(final IAction action) {
		final Display display = PlatformUI.getWorkbench().getDisplay();
		if (display != null) {
			display.asyncExec(new Runnable() {

				@Override
				public void run() {
					if (!display.isDisposed()) {
						action.run();
					}
				}
			});
		}
	}

	private void showResultDeferred(final IAction action) {
		WorkbenchJob dialogJob = new WorkbenchJob(action.getText()) {

			private boolean isModal(Shell shell) {
				return (shell.getStyle() & (SWT.APPLICATION_MODAL
						| SWT.PRIMARY_MODAL | SWT.SYSTEM_MODAL)) != 0;
			}

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				Shell shell = PlatformUI.getWorkbench()
						.getModalDialogShellProvider().getShell();
				if (shell == null) {
					return Status.CANCEL_STATUS;
				}
				if (isModal(shell)) {
					// Don't try to show the result dialog now -- it might
					// produce a UI deadlock. Try again after a short while.
					schedule(PlatformUI.getWorkbench().getProgressService()
							.getLongOperationTime());
					return Status.CANCEL_STATUS;
				}
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				// There is no modal shell: it's safe to open the result dialog.
				action.run();
				return Status.OK_STATUS;
			}
		};
		dialogJob.setSystem(true);
		dialogJob.schedule();
	}
}
