/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.eclipse.osgi.util.NLS;

/**
 * Push operation: pushing from local repository to one or many remote ones.
 */
public class PushOperation {
	private static final int WORK_UNITS_PER_TRANSPORT = 10;

	private final Repository localDb;

	private final PushOperationSpecification specification;

	private final boolean dryRun;

	private final String remoteName;

	private final int timeout;

	private PushOperationResult operationResult;

	private CredentialsProvider credentialsProvider;

	/**
	 * Create push operation for provided specification.
	 *
	 * @param localDb
	 *            local repository.
	 * @param specification
	 *            specification of ref updates for remote repositories.
	 * @param dryRun
	 *            true if push operation should just check for possible result
	 *            and not really update remote refs, false otherwise - when push
	 *            should act normally.
	 * @param timeout
	 *            the timeout in seconds (0 for no timeout)
	 */
	public PushOperation(final Repository localDb,
			final PushOperationSpecification specification,
			final boolean dryRun, int timeout) {
		this.localDb = localDb;
		this.specification = specification;
		this.dryRun = dryRun;
		this.remoteName = null;
		this.timeout = timeout;
	}

	/**
	 * Creates a push operation for a remote configuration.
	 *
	 * @param localDb
	 * @param remoteName
	 * @param dryRun
	 * @param timeout
	 */
	public PushOperation(final Repository localDb, final String remoteName,
			final boolean dryRun, int timeout) {
		this.localDb = localDb;
		this.specification = null;
		this.dryRun = dryRun;
		this.remoteName = remoteName;
		this.timeout = timeout;
	}

	/**
	 * @param credentialsProvider
	 */
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * @return push operation result
	 */
	public PushOperationResult getOperationResult() {
		if (operationResult == null)
			throw new IllegalStateException(CoreText.OperationNotYetExecuted);
		return operationResult;
	}

	/**
	 * @return operation specification, as provided in constructor (may be
	 *         <code>null</code>)
	 */
	public PushOperationSpecification getSpecification() {
		return specification;
	}

	/**
	 * @param actMonitor
	 *            may be <code>null</code> if progress monitoring is not desired
	 * @throws InvocationTargetException
	 *             not really used: failure is communicated via the result (see
	 *             {@link #getOperationResult()})
	 */
	public void run(IProgressMonitor actMonitor)
			throws InvocationTargetException {

		if (operationResult != null)
			throw new IllegalStateException(CoreText.OperationAlreadyExecuted);

		if (this.specification != null)
			for (URIish uri : this.specification.getURIs()) {
				for (RemoteRefUpdate update : this.specification
						.getRefUpdates(uri))
					if (update.getStatus() != Status.NOT_ATTEMPTED)
						throw new IllegalStateException(
								CoreText.RemoteRefUpdateCantBeReused);
			}
		IProgressMonitor monitor;
		if (actMonitor == null)
			monitor = new NullProgressMonitor();
		else
			monitor = actMonitor;

		final int totalWork;
		if (specification != null)
			totalWork = specification.getURIsNumber()
					* WORK_UNITS_PER_TRANSPORT;
		else
			totalWork = 1;
		if (dryRun)
			monitor.beginTask(CoreText.PushOperation_taskNameDryRun, totalWork);
		else
			monitor.beginTask(CoreText.PushOperation_taskNameNormalRun,
					totalWork);

		operationResult = new PushOperationResult();
		Git git = new Git(localDb);

		if (specification != null)
			for (final URIish uri : specification.getURIs()) {
				final SubProgressMonitor subMonitor = new SubProgressMonitor(
						monitor, WORK_UNITS_PER_TRANSPORT,
						SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);

				try {
					List<RefSpec> specs = new ArrayList<RefSpec>(3);
					for (RemoteRefUpdate update : specification
							.getRefUpdates(uri)) {
						RefSpec spec = new RefSpec();
						spec = spec.setSourceDestination(update.getSrcRef(),
								update.getRemoteName());
						spec = spec.setForceUpdate(update.isForceUpdate());
						specs.add(spec);
					}
					if (monitor.isCanceled()) {
						operationResult.addOperationResult(uri,
								CoreText.PushOperation_resultCancelled);
						continue;
					}

					final EclipseGitProgressTransformer gitSubMonitor = new EclipseGitProgressTransformer(
							subMonitor);

					try {
						Iterable<PushResult> results = git.push().setRemote(
								uri.toPrivateString()).setRefSpecs(specs)
								.setDryRun(dryRun).setTimeout(timeout)
								.setProgressMonitor(gitSubMonitor)
								.setCredentialsProvider(credentialsProvider)
								.call();
						for (PushResult result : results) {
							operationResult.addOperationResult(result.getURI(),
									result);
							specification.addURIRefUpdates(result.getURI(),
									result.getRemoteUpdates());
						}
					} catch (JGitInternalException e) {
						String errorMessage = e.getCause() != null ? e
								.getCause().getMessage() : e.getMessage();
						String userMessage = NLS.bind(
										CoreText.PushOperation_InternalExceptionOccurredMessage,
										errorMessage);
						handleException(uri, e, userMessage);
					} catch (InvalidRemoteException e) {
						handleException(uri, e, e.getMessage());
					}

					monitor.worked(WORK_UNITS_PER_TRANSPORT);
				} finally {
					// Dirty trick to get things always working.
					subMonitor.beginTask("", WORK_UNITS_PER_TRANSPORT); //$NON-NLS-1$
					subMonitor.done();
					subMonitor.done();
				}
			}
		else {
			final EclipseGitProgressTransformer gitMonitor = new EclipseGitProgressTransformer(
					monitor);
			try {
				Iterable<PushResult> results = git.push().setRemote(
						remoteName).setDryRun(dryRun).setTimeout(timeout)
						.setProgressMonitor(gitMonitor).setCredentialsProvider(
								credentialsProvider).call();
				for (PushResult result : results) {
					operationResult.addOperationResult(result.getURI(), result);
				}
			} catch (JGitInternalException e) {
				String errorMessage = e.getCause() != null ? e.getCause()
						.getMessage() : e.getMessage();
				String userMessage = NLS.bind(
						CoreText.PushOperation_InternalExceptionOccurredMessage,
						errorMessage);
				URIish uri = getPushURIForErrorHandling();
				handleException(uri, e, userMessage);
			} catch (InvalidRemoteException e) {
				URIish uri = getPushURIForErrorHandling();
				handleException(uri, e, e.getMessage());
			}
		}
		monitor.done();
	}

	private void handleException(final URIish uri, Exception e,
			String userMessage) {
		if (uri != null)
			operationResult.addOperationResult(uri, userMessage);
		String uriString;
		if (uri == null)
			uriString = "retrieving URI failed"; //$NON-NLS-1$
		else
			uriString = uri.toString();
		String userMessageForUri = NLS.bind(
				CoreText.PushOperation_ExceptionOccurredDuringPushOnUriMessage,
				uriString, userMessage);
		Activator.logError(userMessageForUri, e);
	}

	private URIish getPushURIForErrorHandling() {
		RemoteConfig rc = null;
		try {
			rc = new RemoteConfig(localDb.getConfig(), remoteName);
			return rc.getPushURIs().isEmpty() ? rc.getURIs().get(0) : rc
					.getPushURIs().get(0);
		} catch (URISyntaxException e) {
			// should not happen
			Activator.logError("Reading RemoteConfig failed", e); //$NON-NLS-1$
			return null;
		}
	}
}
