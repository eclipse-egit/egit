/*******************************************************************************
 * Copyright (C) 2026 EGit Contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.internal.credentials.EGitCredentialsProvider;
import org.eclipse.egit.core.op.FetchOperation;
import org.eclipse.egit.core.settings.GitSettings;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.fetch.FetchAllResultDialog;
import org.eclipse.egit.ui.internal.fetch.FetchResultEntry;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Action handler for "Fetch all" - fetches from every configured remote,
 * equivalent to {@code git fetch --all}.
 */
public class FetchAllActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(true, event);
		if (repository == null) {
			return null;
		}
		List<RemoteConfig> remotes;
		try {
			remotes = RemoteConfig.getAllRemoteConfigs(repository.getConfig());
		} catch (URISyntaxException e) {
			throw new ExecutionException(e.getLocalizedMessage(), e);
		}
		if (remotes.isEmpty()) {
			MessageDialog.openInformation(getShell(event),
					UIText.FetchAllActionHandler_NothingToFetchTitle,
					UIText.FetchAllActionHandler_NothingToFetchMessage);
			return null;
		}
		List<FetchResultEntry> results = Collections
				.synchronizedList(new ArrayList<>());
		List<String> errors = Collections.synchronizedList(new ArrayList<>());
		Job fetchAllJob = createFetchAllJobWithEntries(repository, remotes,
				results, errors);
		fetchAllJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent jobEvent) {
				IStatus result = jobEvent.getResult();
				if (result != null
						&& result.getSeverity() == IStatus.CANCEL) {
					return;
				}
				if (result != null && !result.isOK()) {
					Activator.handleStatus(result, true);
				}
				PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
					Shell shell = PlatformUI.getWorkbench()
							.getModalDialogShellProvider().getShell();
					new FetchAllResultDialog(shell, repository,
							new ArrayList<>(results), new ArrayList<>(errors))
									.open();
				});
			}
		});
		fetchAllJob.schedule();
		return null;
	}

	@Override
	public boolean isEnabled() {
		final Repository repository = getRepository();
		if (repository == null) {
			return false;
		}
		try {
			return !RemoteConfig
					.getAllRemoteConfigs(repository.getConfig()).isEmpty();
		} catch (URISyntaxException e) {
			return false;
		}
	}

	/**
	 * Fetches from all remotes of the given repository in a background job and
	 * returns the scheduled {@link Job} so callers can attach listeners.
	 *
	 * @param repository
	 *            the repository to fetch in
	 * @param remotes
	 *            list of remotes to fetch from
	 * @return the scheduled job
	 */
	public static Job startFetchAll(Repository repository,
			List<RemoteConfig> remotes) {
		return startFetchAll(repository, remotes, null);
	}

	/**
	 * Fetches from all remotes of the given repository in a background job and
	 * optionally collects successful fetch results.
	 *
	 * @param repository
	 *            the repository to fetch in
	 * @param remotes
	 *            list of remotes to fetch from
	 * @param results
	 *            optional list receiving successful fetch results
	 * @return the scheduled job
	 */
	public static Job startFetchAll(Repository repository,
			List<RemoteConfig> remotes, List<FetchResult> results) {
		Job job = createFetchAllJob(repository, remotes, results);
		job.schedule();
		return job;
	}

	/**
	 * Creates an unscheduled job that fetches from all remotes of the given
	 * repository.
	 *
	 * @param repository
	 *            the repository to fetch in
	 * @param remotes
	 *            list of remotes to fetch from
	 * @param results
	 *            optional list receiving successful fetch results
	 * @return the unscheduled job
	 */
	public static Job createFetchAllJob(Repository repository,
			List<RemoteConfig> remotes, List<FetchResult> results) {
		return createFetchAllJob(repository, remotes, results, null, null);
	}

	/**
	 * Creates an unscheduled job that fetches from all remotes and records
	 * display-ready results.
	 *
	 * @param repository
	 *            the repository to fetch in
	 * @param remotes
	 *            list of remotes to fetch from
	 * @param results
	 *            optional list receiving successful fetch results with labels
	 * @return the unscheduled job
	 */
	public static Job createFetchAllJobWithEntries(Repository repository,
			List<RemoteConfig> remotes, List<FetchResultEntry> results) {
		return createFetchAllJobWithEntries(repository, remotes, results, null);
	}

	/**
	 * Creates an unscheduled job that fetches from all remotes and records
	 * display-ready results.
	 *
	 * @param repository
	 *            the repository to fetch in
	 * @param remotes
	 *            list of remotes to fetch from
	 * @param results
	 *            optional list receiving successful fetch results with labels
	 * @param errors
	 *            optional list receiving error messages
	 * @return the unscheduled job
	 */
	public static Job createFetchAllJobWithEntries(Repository repository,
			List<RemoteConfig> remotes, List<FetchResultEntry> results,
			List<String> errors) {
		return createFetchAllJob(repository, remotes, null, results, errors);
	}

	private static Job createFetchAllJob(Repository repository,
			List<RemoteConfig> remotes, List<FetchResult> results,
			List<FetchResultEntry> resultEntries,
			List<String> errors) {
		Job job = new Job(UIText.FetchAllActionHandler_FetchAllJobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				SubMonitor sub = SubMonitor.convert(monitor, remotes.size());
				for (RemoteConfig rc : remotes) {
					if (sub.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					FetchOperation op = new FetchOperation(repository, rc,
							GitSettings.getRemoteConnectionTimeout(), false);
					op.setCredentialsProvider(new EGitCredentialsProvider());
					try {
						op.run(sub.newChild(1));
						if (results != null && op.getOperationResult() != null) {
							results.add(op.getOperationResult());
						}
						if (resultEntries != null
								&& op.getOperationResult() != null) {
							resultEntries.add(new FetchResultEntry(
									op.getOperationResult(),
									getSourceString(repository, rc)));
						}
					} catch (InvocationTargetException e) {
						Throwable cause = e.getCause() != null ? e.getCause()
								: e;
						if (errors != null) {
							errors.add(MessageFormat.format(
									UIText.FetchAllActionHandler_FetchFailed,
									rc.getName(), cause.getMessage()));
						}
						// Log errors for individual remotes but continue
						Activator.logError(
								"Fetch failed for remote '" + rc.getName() + "'", //$NON-NLS-1$ //$NON-NLS-2$
								cause);
					}
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.FETCH.equals(family)) {
					return true;
				}
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		return job;
	}

	private static String getSourceString(Repository repository,
			RemoteConfig remote) {
		return NLS.bind("{0} - {1}", repository.getDirectory() //$NON-NLS-1$
				.getParentFile().getName(), remote.getName());
	}
}
