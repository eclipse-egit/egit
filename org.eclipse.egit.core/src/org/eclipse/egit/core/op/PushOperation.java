/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.Transport;
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

	private final RemoteConfig rc;

	private final int timeout;

	private PushOperationResult operationResult;

	/**
	 * Create push operation for provided specification.
	 * <p>
	 * Operation is not performed within constructor,
	 * {@link #run(IProgressMonitor)} method must be called for that.
	 *
	 * @param localDb
	 *            local repository.
	 * @param specification
	 *            specification of ref updates for remote repositories.
	 * @param rc
	 *            optional remote config to apply on used transports. May be
	 *            null.
	 * @param dryRun
	 *            true if push operation should just check for possible result
	 *            and not really update remote refs, false otherwise - when push
	 *            should act normally.
	 * @param timeout the timeout in seconds (0 for no timeout)
	 */
	public PushOperation(final Repository localDb,
			final PushOperationSpecification specification,
			final boolean dryRun, final RemoteConfig rc, int timeout) {
		this.localDb = localDb;
		this.specification = specification;
		this.dryRun = dryRun;
		this.rc = rc;
		this.timeout = timeout;
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
	 * @return operation specification, as provided in constructor.
	 */
	public PushOperationSpecification getSpecification() {
		return specification;
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
	 * @throws InvocationTargetException
	 *             Cause of this exceptions may include
	 *             {@link TransportException}, {@link NotSupportedException} or
	 *             some unexpected {@link RuntimeException}.
	 */
	public void run(IProgressMonitor actMonitor) throws InvocationTargetException {

		if (operationResult != null)
			throw new IllegalStateException(CoreText.OperationAlreadyExecuted);

		for (URIish uri : this.specification.getURIs()) {
			for (RemoteRefUpdate update : this.specification.getRefUpdates(uri))
				if (update.getStatus() != Status.NOT_ATTEMPTED)
					throw new IllegalStateException(
							CoreText.RemoteRefUpdateCantBeReused);
		}
		IProgressMonitor monitor;
		if (actMonitor == null)
			monitor = new NullProgressMonitor();
		else
			monitor = actMonitor;

		final int totalWork = specification.getURIsNumber()
				* WORK_UNITS_PER_TRANSPORT;
		if (dryRun)
			monitor.beginTask(CoreText.PushOperation_taskNameDryRun, totalWork);
		else
			monitor.beginTask(CoreText.PushOperation_taskNameNormalRun,
					totalWork);

		operationResult = new PushOperationResult();

		for (final URIish uri : specification.getURIs()) {
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
				transport.setTimeout(this.timeout);

				if (rc != null)
					transport.applyConfig(rc);
				transport.setDryRun(dryRun);
				final EclipseGitProgressTransformer gitSubMonitor = new EclipseGitProgressTransformer(
						subMonitor);
				final PushResult pr = transport.push(gitSubMonitor,
						specification.getRefUpdates(uri));
				operationResult.addOperationResult(uri, pr);
				monitor.worked(WORK_UNITS_PER_TRANSPORT);
			} catch (final NoRemoteRepositoryException e) {
				operationResult.addOperationResult(uri, NLS.bind(
						CoreText.PushOperation_resultNoServiceError, e
								.getMessage()));
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
}
