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
package org.eclipse.egit.ui.internal.fetch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.op.FetchOperationResult;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.job.JobUtil;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

/**
 * Fetches from a remote as configured
 */
public class FetchConfiguredRemoteAction extends JobChangeAdapter implements
		IEGitOperation {
	private final Repository repository;

	private final RemoteConfig remote;

	private FetchOperationResult operationResult;

	private boolean dryRun;

	private final int timeout;

	/**
	 * @param repository
	 *            a {@link Repository}
	 * @param config
	 *            the {@link RemoteConfig} to fetch from
	 * @param timeout
	 */
	public FetchConfiguredRemoteAction(Repository repository,
			RemoteConfig config, int timeout) {
		this.repository = repository;
		this.remote = config;
		this.timeout = timeout;
	}

	/**
	 * @param dryRun
	 */
	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}

	public void execute(IProgressMonitor monitor) throws CoreException {
		if (operationResult != null)
			throw new IllegalStateException(CoreText.OperationAlreadyExecuted);

		if (remote.getURIs().isEmpty()) {
			// TODO should we check this here?
			IStatus error = Activator.createErrorStatus(NLS.bind(
					UIText.FetchConfiguredRemoteAction_NoUrisDefinedMessage,
					remote.getName()), null);
			throw new CoreException(error);
		}

		IProgressMonitor actMonitor = monitor;
		if (actMonitor == null)
			actMonitor = new NullProgressMonitor();

		try {
			final Transport transport = Transport.open(repository, remote);
			transport.setDryRun(dryRun);
			transport.setTimeout(timeout);
			FetchResult fetchRes = transport.fetch(
					new EclipseGitProgressTransformer(actMonitor), remote
							.getFetchRefSpecs());
			operationResult = new FetchOperationResult(remote.getURIs().get(0),
					fetchRes);
		} catch (final NotSupportedException e) {
			throw new CoreException(Activator.createErrorStatus(e.getMessage(),
					e));
		} catch (final TransportException e) {
			if (actMonitor.isCanceled())
				throw new CoreException(Status.CANCEL_STATUS);
			String errorMessage = NLS
					.bind(
							UIText.FetchConfiguredRemoteAction_TransportErrorDuringFetchMessage,
							e.getMessage());
			operationResult = new FetchOperationResult(remote.getURIs().get(0),
					errorMessage);
		}
	}

	public ISchedulingRule getSchedulingRule() {
		return null;
	}

	/**
	 * Start asynchronously
	 */
	public void start() {
		String jobName = NLS.bind(
				UIText.FetchConfiguredRemoteAction_FetchJobName, repository
						.getDirectory().getParentFile().getName(), remote
						.getName());
		JobUtil.scheduleUserJob(this, jobName, JobFamilies.FETCH, this);
	}

	/**
	 * @return the result, null if {@link #equals(Object)} was not yet executed
	 */
	public FetchOperationResult getOperationResult() {
		return operationResult;
	}

	@Override
	public void done(IJobChangeEvent event) {
		if (event.getResult().getSeverity() == IStatus.CANCEL) {
			return;
		}
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				String repoName = Activator.getDefault().getRepositoryUtil()
						.getRepositoryName(repository);
				Dialog dialog = new FetchResultDialog(PlatformUI.getWorkbench()
						.getDisplay().getActiveShell(), repository,
						operationResult, repoName + " - " + remote.getName()); //$NON-NLS-1$
				dialog.open();
			}
		});
	}
}
