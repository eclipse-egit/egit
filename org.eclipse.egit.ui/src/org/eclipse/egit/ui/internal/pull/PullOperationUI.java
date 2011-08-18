/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.pull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.op.PullOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * UI wrapper for {@link PullOperation}
 */
public class PullOperationUI extends JobChangeAdapter implements
		IJobChangeListener {
	private static final IStatus NOT_TRIED_STATUS = new Status(IStatus.ERROR,
			Activator.getPluginId(), UIText.PullOperationUI_NotTriedMessage);

	private final Repository[] repositories;

	private final Map<Repository, Object> results = new HashMap<Repository, Object>();

	private final PullOperation pullOperation;

	/**
	 * @param repositories
	 */
	public PullOperationUI(Set<Repository> repositories) {
		this.repositories = repositories.toArray(new Repository[repositories
				.size()]);
		int timeout = Activator.getDefault().getPreferenceStore().getInt(
				UIPreferences.REMOTE_CONNECTION_TIMEOUT);
		pullOperation = new PullOperation(repositories, timeout);
		for (Repository repository : repositories)
			results.put(repository, NOT_TRIED_STATUS);
	}

	/**
	 * Starts this operation asynchronously
	 */
	public void start() {
		// figure out a job name
		String jobName;
		if (this.repositories.length == 1) {
			String repoName = Activator.getDefault().getRepositoryUtil()
					.getRepositoryName(repositories[0]);
			String shortBranchName;
			try {
				shortBranchName = repositories[0].getBranch();
			} catch (IOException e) {
				// ignore here
				shortBranchName = ""; //$NON-NLS-1$
			}
			jobName = NLS.bind(UIText.PullOperationUI_PullingTaskName,
					shortBranchName, repoName);
		} else
			jobName = UIText.PullOperationUI_PullingMultipleTaskName;
		Job job = new Job(jobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				execute(monitor);
				// we always return OK and handle display of errors on our own
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.PULL.equals(family))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.setUser(true);
		job.addJobChangeListener(this);
		job.schedule();
	}

	/**
	 * Starts this operation synchronously.
	 *
	 * @param monitor
	 */
	public void execute(IProgressMonitor monitor) {
		try {
			pullOperation.execute(monitor);
			results.putAll(pullOperation.getResults());
		} catch (CoreException e) {
			if (e.getStatus().getSeverity() == IStatus.CANCEL)
				results.putAll(pullOperation.getResults());
			else
				Activator.handleError(e.getMessage(), e, true);
		}
	}

	public void done(IJobChangeEvent event) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				Shell shell = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell();
				showResults(shell);
			}
		});

	}

	private void showResults(final Shell shell) {
		if (this.results.isEmpty())
			// shouldn't really happen, but just in case...
			return;
		else if (this.results.size() == 1) {
			Entry<Repository, Object> entry = this.results.entrySet()
					.iterator().next();
			if (entry.getValue() instanceof PullResult)
				new PullResultDialog(shell, entry.getKey(), (PullResult) entry
						.getValue()).open();
			else {
				IStatus status = (IStatus) entry.getValue();
				if (status == NOT_TRIED_STATUS) {
					MessageDialog
							.openInformation(
									shell,
									UIText.PullOperationUI_PullCanceledWindowTitle,
									UIText.PullOperationUI_PullOperationCanceledMessage);
				} else if (status.getException() instanceof TransportException) {
					ErrorDialog.openError(shell,
							UIText.PullOperationUI_PullFailed,
							UIText.PullOperationUI_ConnectionProblem,
							status);
				} else
					Activator.handleError(status.getMessage(), status
							.getException(), true);
			}
		} else
			new MultiPullResultDialog(shell, results).open();
	}
}
