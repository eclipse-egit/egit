/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2015, Stephan Hackstedt <stephan.hackstedt@googlemail.com>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;

/**
 * Push operation: pushing from local repository to one or many remote ones.
 */
public class PushOperation {

	private final Repository localDb;

	private final PushOperationSpecification specification;

	private final boolean dryRun;

	private final String remoteName;

	private final int timeout;

	private OutputStream out;

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
		this(localDb, null, specification, dryRun, timeout);
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
		this(localDb, remoteName, null, dryRun, timeout);
	}

	private PushOperation(final Repository localDb, final String remoteName,
			PushOperationSpecification specification, final boolean dryRun,
			int timeout) {
		this.localDb = localDb;
		this.specification = specification;
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
	 * @return the operation's credentials provider
	 */
	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
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

		final int totalWork;
		if (specification != null) {
			totalWork = specification.getURIsNumber();
		} else {
			totalWork = 1;
		}

		String taskName = dryRun ? CoreText.PushOperation_taskNameDryRun
				: CoreText.PushOperation_taskNameNormalRun;
		SubMonitor progress = SubMonitor.convert(actMonitor, totalWork);
		progress.setTaskName(taskName);

		operationResult = new PushOperationResult();
		try (Git git = new Git(localDb)) {
			if (specification != null)
				for (final URIish uri : specification.getURIs()) {
					if (progress.isCanceled()) {
						operationResult.addOperationResult(uri,
								CoreText.PushOperation_resultCancelled);
						progress.worked(1);
						continue;
					}

					Collection<RemoteRefUpdate> refUpdates = specification
							.getRefUpdates(uri);
					final EclipseGitProgressTransformer gitSubMonitor = new EclipseGitProgressTransformer(
							progress.newChild(1));

					try (Transport transport = Transport.open(localDb, uri)) {
						transport.setDryRun(dryRun);
						transport.setTimeout(timeout);
						if (credentialsProvider != null) {
							transport.setCredentialsProvider(
									credentialsProvider);
						}
						PushResult result = transport.push(gitSubMonitor,
								refUpdates, out);

						operationResult.addOperationResult(result.getURI(),
								result);
						specification.addURIRefUpdates(result.getURI(),
								result.getRemoteUpdates());
					} catch (JGitInternalException e) {
						String errorMessage = e.getCause() != null
								? e.getCause().getMessage() : e.getMessage();
						String userMessage = NLS.bind(
								CoreText.PushOperation_InternalExceptionOccurredMessage,
								errorMessage);
						handleException(uri, e, userMessage);
					} catch (Exception e) {
						handleException(uri, e, e.getMessage());
					}
				}
			else {
				final EclipseGitProgressTransformer gitMonitor = new EclipseGitProgressTransformer(
						progress.newChild(totalWork));
				try {
					Iterable<PushResult> results = git.push()
							.setRemote(remoteName).setDryRun(dryRun)
							.setTimeout(timeout).setProgressMonitor(gitMonitor)
							.setCredentialsProvider(credentialsProvider)
							.setOutputStream(out).call();
					for (PushResult result : results) {
						operationResult.addOperationResult(result.getURI(),
								result);
					}
				} catch (JGitInternalException e) {
					String errorMessage = e.getCause() != null
							? e.getCause().getMessage() : e.getMessage();
					String userMessage = NLS.bind(
							CoreText.PushOperation_InternalExceptionOccurredMessage,
							errorMessage);
					URIish uri = getPushURIForErrorHandling();
					handleException(uri, e, userMessage);
				} catch (Exception e) {
					URIish uri = getPushURIForErrorHandling();
					handleException(uri, e, e.getMessage());
				}
			}
		}
	}

	private void handleException(final URIish uri, Exception e,
			String userMessage) {
		String uriString;
		if (uri != null) {
			operationResult.addOperationResult(uri, userMessage);
			uriString = uri.toString();
		} else
			uriString = "retrieving URI failed"; //$NON-NLS-1$

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

	/**
	 * Sets the output stream this operation will write sideband messages to.
	 *
	 * @param out
	 *            the outputstream to write to
	 * @since 3.0
	 */
	public void setOutputStream(OutputStream out) {
		this.out = out;
	}
}
