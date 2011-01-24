/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.job.JobUtil;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

/**
 * Push to a {@link RemoteConfig}
 */
public class PushConfiguredRemoteAction extends JobChangeAdapter implements
		IEGitOperation {
	/** The default RefSpec */
	public static final RefSpec DEFAULT_PUSH_REF_SPEC = new RefSpec(
			"refs/heads/*:refs/heads/*"); //$NON-NLS-1$

	private static final int WORK_UNITS_PER_TRANSPORT = 10;

	private final Repository localDb;

	private final RemoteConfig rc;

	private boolean dryRun = false;

	private final int timeout;

	private PushOperationResult operationResult;

	private CredentialsProvider credentialsProvider;

	/**
	 * Create push operation for provided specification.
	 *
	 * @param localDb
	 *            local repository
	 * @param rc
	 *            remote configuration
	 * @param timeout
	 *            the timeout in seconds (0 for no timeout)
	 */
	public PushConfiguredRemoteAction(final Repository localDb,
			final RemoteConfig rc, int timeout) {
		this.localDb = localDb;
		this.rc = rc;
		this.timeout = timeout;
	}

	/**
	 * Sets a credentials provider
	 *
	 * @param credentialsProvider
	 */
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * @param dryRun
	 */
	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}

	/**
	 * @return push operation result.
	 */
	public PushOperationResult getOperationResult() {
		if (operationResult == null)
			throw new IllegalStateException(CoreText.OperationNotYetExecuted);
		return operationResult;
	}

	/**
	 * Execute operation and store result. Operation is executed independently
	 * on each remote repository.
	 * <p>
	 *
	 * @param actMonitor
	 *            the monitor to be used for reporting progress and responding
	 *            to cancellation. The monitor is never <code>null</code>
	 *
	 */
	public void execute(IProgressMonitor actMonitor) {
		if (operationResult != null)
			throw new IllegalStateException(CoreText.OperationAlreadyExecuted);

		List<URIish> urisToPush = new ArrayList<URIish>();
		urisToPush.addAll(rc.getPushURIs());
		if (urisToPush.isEmpty() && !rc.getURIs().isEmpty())
			urisToPush.add(rc.getURIs().get(0));

		IProgressMonitor monitor;
		if (actMonitor == null)
			monitor = new NullProgressMonitor();
		else
			monitor = actMonitor;

		final int totalWork = urisToPush.size() * WORK_UNITS_PER_TRANSPORT;
		if (dryRun)
			monitor.beginTask(CoreText.PushOperation_taskNameDryRun, totalWork);
		else
			monitor.beginTask(CoreText.PushOperation_taskNameNormalRun,
					totalWork);

		operationResult = new PushOperationResult();

		for (final URIish uri : urisToPush) {
			final SubProgressMonitor subMonitor = new SubProgressMonitor(
					monitor, WORK_UNITS_PER_TRANSPORT,
					SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
			Transport transport = null;
			try {
				if (monitor.isCanceled()) {
					operationResult.addOperationResult(uri,
							CoreText.PushOperation_resultCancelled);
					continue;
				}
				transport = Transport.open(localDb, uri);
				if (credentialsProvider != null)
					transport.setCredentialsProvider(credentialsProvider);
				transport.setTimeout(this.timeout);

				// default: if not refSpec, update all branches
				if (rc.getPushRefSpecs().isEmpty())
					rc.addPushRefSpec(DEFAULT_PUSH_REF_SPEC);

				transport.applyConfig(rc);
				transport.setDryRun(dryRun);

				final EclipseGitProgressTransformer gitSubMonitor = new EclipseGitProgressTransformer(
						subMonitor);
				final PushResult pr = transport.push(gitSubMonitor, null);
				operationResult.addOperationResult(uri, pr);
				monitor.worked(WORK_UNITS_PER_TRANSPORT);
			} catch (final TransportException e) {
				operationResult.addOperationResult(uri, NLS.bind(
						CoreText.PushOperation_resultTransportError, e
								.getMessage()));
			} catch (final NotSupportedException e) {
				operationResult.addOperationResult(uri, NLS.bind(
						CoreText.PushOperation_resultNotSupported, e
								.getMessage()));
			} finally {
				if (transport != null) {
					transport.close();
				}
				// Dirty trick to get things always working.
				subMonitor.beginTask("", WORK_UNITS_PER_TRANSPORT); //$NON-NLS-1$
				subMonitor.done();
				subMonitor.done();
			}
		}
		monitor.done();
	}

	/**
	 * Run asynchronously
	 */
	public void start() {
		String jobName = NLS.bind(
				CoreText.PushConfiguredRemoteAction_PushJobName, Activator
						.getDefault().getRepositoryUtil().getRepositoryName(
								localDb), rc.getName());
		JobUtil.scheduleUserJob(this, jobName, JobFamilies.PUSH, this);
	}

	public void done(IJobChangeEvent event) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				final Dialog dialog = new PushResultDialog(PlatformUI
						.getWorkbench().getDisplay().getActiveShell(), localDb,
						getOperationResult(), Activator.getDefault()
								.getRepositoryUtil().getRepositoryName(localDb)
								+ " - " + rc.getName()); //$NON-NLS-1$
				dialog.open();
			}
		});
	}

	public ISchedulingRule getSchedulingRule() {
		return null;
	}
}
