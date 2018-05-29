/*******************************************************************************
 * Copyright (c) 2011, 2016 SAP AG and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 495777
 *******************************************************************************/
package org.eclipse.egit.ui.internal.pull;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.op.PullOperation;
import org.eclipse.egit.core.op.PullOperation.PullReferenceConfig;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.CleanupUncomittedChangesDialog;
import org.eclipse.egit.ui.internal.branch.LaunchFinder;
import org.eclipse.egit.ui.internal.credentials.EGitCredentialsProvider;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * UI wrapper for {@link PullOperation}
 */
public class PullOperationUI extends JobChangeAdapter {
	private static final IStatus NOT_TRIED_STATUS = new Status(IStatus.ERROR,
			Activator.getPluginId(), UIText.PullOperationUI_NotTriedMessage);

	private final Repository[] repositories;

	/**
	 * Holds the number of PullOperationUIs that need to complete before the
	 * pull results can be shown. The number includes this instance of
	 * PullOperationUI and all subtasks spawned by it.
	 */
	private final AtomicInteger tasksToWaitFor = new AtomicInteger(1);

	/** pull results per repository */
	protected final Map<Repository, Object> results = Collections
			.synchronizedMap(new LinkedHashMap<Repository, Object>());

	private final PullOperation pullOperation;

	private boolean checkForLaunches = true;

	/**
	 * @param repositories
	 */
	public PullOperationUI(Set<Repository> repositories) {
		this.repositories = repositories.toArray(new Repository[repositories
				.size()]);
		int timeout = Activator.getDefault().getPreferenceStore().getInt(
				UIPreferences.REMOTE_CONNECTION_TIMEOUT);
		pullOperation = new PullOperation(repositories, timeout);
		pullOperation.setCredentialsProvider(new EGitCredentialsProvider());
		for (Repository repository : repositories)
			results.put(repository, NOT_TRIED_STATUS);
	}

	/**
	 * @param configs
	 */
	public PullOperationUI(Map<Repository, PullReferenceConfig> configs) {
		this.repositories = configs.keySet()
				.toArray(new Repository[configs.size()]);
		int timeout = Activator.getDefault().getPreferenceStore()
				.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
		pullOperation = new PullOperation(configs, timeout);
		pullOperation.setCredentialsProvider(new EGitCredentialsProvider());
		for (Repository repository : repositories) {
			results.put(repository, NOT_TRIED_STATUS);
		}
	}

	/**
	 * Starts this operation asynchronously
	 */
	public void start() {
		start(this);
	}

	private void start(IJobChangeListener jobChangeListener) {
		if (checkForLaunches
				&& LaunchFinder.shouldCancelBecauseOfRunningLaunches(
						Arrays.asList(repositories), null)) {
			return;
		}
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
		Job job = new WorkspaceJob(jobName) {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				execute(monitor, false);
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
		job.setRule(RuleUtil.getRuleForRepositories(Arrays.asList(repositories)));
		job.setUser(true);
		job.addJobChangeListener(jobChangeListener);
		job.schedule();
	}

	/**
	 * Starts this operation synchronously.
	 *
	 * @param monitor
	 */
	public void execute(IProgressMonitor monitor) {
		execute(monitor, true);
	}

	private void execute(IProgressMonitor monitor, boolean launchCheck) {
		SubMonitor progress = SubMonitor.convert(monitor,
				launchCheck ? 11 : 10);
		if (launchCheck && LaunchFinder.shouldCancelBecauseOfRunningLaunches(
				Arrays.asList(repositories), progress.newChild(1))) {
			return;
		}
		try {
			pullOperation.execute(progress.newChild(10));
			results.putAll(pullOperation.getResults());
		} catch (CoreException e) {
			if (e.getStatus().getSeverity() == IStatus.CANCEL) {
				results.putAll(pullOperation.getResults());
			} else {
				Activator.handleError(e.getMessage(), e, true);
			}
		}

	}

	@Override
	public void done(IJobChangeEvent event) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				Map<Repository, Object> res = new LinkedHashMap<>(
						PullOperationUI.this.results);
				Shell shell = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell();
				handlePullResults(res, shell);
			}
		});
	}

	/**
	 * Post-process the pull results, allowing the user to deal with uncommitted
	 * changes and re-pull if the initial pull failed because of these changes
	 *
	 * @param resultsMap
	 *            a copy of the initial pull results
	 * @param shell
	 */
	private void handlePullResults(final Map<Repository, Object> resultsMap,
			Shell shell) {
		for (Entry<Repository, Object> entry : resultsMap.entrySet()) {
			Object result = entry.getValue();
			if (result instanceof PullResult) {
				PullResult pullResult = (PullResult) result;
				if (pullResult.getRebaseResult() != null
						&& RebaseResult.Status.UNCOMMITTED_CHANGES == pullResult
								.getRebaseResult().getStatus()) {
					handleUncommittedChanges(entry.getKey(), pullResult
							.getRebaseResult().getUncommittedChanges(), shell);
				}
			}
		}

		if (tasksToWaitFor.decrementAndGet() == 0 && !results.isEmpty())
			showResults(shell);
	}

	/**
	 * @param repository
	 * @param files
	 *            uncommitted files that were in the way for the rebase
	 * @param shell
	 */
	private void handleUncommittedChanges(final Repository repository,
			final List<String> files, Shell shell) {
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		CleanupUncomittedChangesDialog cleanupUncomittedChangesDialog = new CleanupUncomittedChangesDialog(
				shell,
				MessageFormat
						.format(UIText.AbstractRebaseCommandHandler_cleanupDialog_title,
								repoName),
				UIText.AbstractRebaseCommandHandler_cleanupDialog_text,
				repository, files);
		cleanupUncomittedChangesDialog.open();
		if (cleanupUncomittedChangesDialog.shouldContinue()) {
			final PullOperationUI parentOperation = this;
			final PullOperationUI pullOperationUI = new PullOperationUI(
					Collections.singleton(repository));
			pullOperationUI.checkForLaunches = false;
			tasksToWaitFor.incrementAndGet();
			pullOperationUI.start(new JobChangeAdapter() {

				@Override
				public void done(IJobChangeEvent event) {
					// put the result from this subtask into the result map of
					// the outer PullOperationUI and display the results if all
					// subtasks have reported back
					parentOperation.results.putAll(pullOperationUI.results);
					int missing = parentOperation.tasksToWaitFor.decrementAndGet();
					if (missing == 0)
						parentOperation.showResults();
				}
			});
		}
	}

	private void showResults() {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
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
