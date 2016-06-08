/*******************************************************************************
 * Copyright (c) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressConstants;

/**
 * A {@link Job} operating (solely) on a repository, reporting some result
 * beyond a mere {@link IStatus} back to the user via an {@link IAction}. If the
 * job is running in a dialog when its {@link #performJob(IProgressMonitor)}
 * method returns, the action is invoked directly in the display thread,
 * otherwise {@link IProgressConstants#ACTION_PROPERTY} is used to associate the
 * action with the finished job and eventual display of the result is left to
 * the progress reporting framework.
 */
public abstract class RepositoryJob extends Job {

	/**
	 * Creates a new {@link RepositoryJob}.
	 *
	 * @param name
	 *            of the job.
	 */
	public RepositoryJob(String name) {
		super(name);
	}

	@Override
	protected final IStatus run(IProgressMonitor monitor) {
		IStatus status = performJob(monitor);
		if (status == null) {
			return Activator
					.createErrorStatus(
							MessageFormat.format(
									UIText.RepositoryJob_NullStatus, getName()),
							new NullPointerException());
		} else if (!status.isOK()) {
			return status;
		}
		IAction action = getAction();
		if (action != null) {
			if (isModal()) {
				showResult(action);
			} else {
				setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
				setProperty(IProgressConstants.ACTION_PROPERTY, action);
				return new Status(IStatus.OK, Activator.getPluginId(),
						IStatus.OK, action.getText(), null);
			}
		}
		return status;
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
}
