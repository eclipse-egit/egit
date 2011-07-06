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
package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.op.PushOperation;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.core.op.PushOperationSpecification;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;

/**
 * UI Wrapper for {@link PushOperation}
 */
public class PushOperationUI {
	/** The default RefSpec */
	public static final RefSpec DEFAULT_PUSH_REF_SPEC = new RefSpec(
			"refs/heads/*:refs/heads/*"); //$NON-NLS-1$

	private final Repository repository;

	private final int timeout;

	private final boolean dryRun;

	private final String destinationString;

	private final RemoteConfig config;

	private PushOperationSpecification spec;

	private CredentialsProvider credentialsProvider;

	private PushOperation op;

	private final String remoteName;

	/**
	 * @param repository
	 * @param remoteName
	 * @param timeout
	 * @param dryRun
	 *
	 */
	public PushOperationUI(Repository repository, String remoteName,
			int timeout, boolean dryRun) {
		this.repository = repository;
		this.spec = null;
		this.config = null;
		this.remoteName = remoteName;
		this.timeout = timeout;
		this.dryRun = dryRun;
		destinationString = NLS.bind("{0} - {1}", repository.getDirectory() //$NON-NLS-1$
				.getParentFile().getName(), remoteName);
	}


	/**
	 * @param repository
	 * @param config
	 * @param timeout
	 * @param dryRun
	 *
	 */
	public PushOperationUI(Repository repository, RemoteConfig config,
			int timeout, boolean dryRun) {
		this.repository = repository;
		this.spec = null;
		this.config = config;
		this.remoteName = null;
		this.timeout = timeout;
		this.dryRun = dryRun;
		destinationString = NLS.bind("{0} - {1}", repository.getDirectory() //$NON-NLS-1$
				.getParentFile().getName(), config.getName());
	}

	/**
	 * @param repository
	 * @param spec
	 * @param timeout
	 * @param dryRun
	 */
	public PushOperationUI(Repository repository,
			PushOperationSpecification spec, int timeout, boolean dryRun) {
		this.repository = repository;
		this.spec = spec;
		this.config = null;
		this.remoteName = null;
		this.timeout = timeout;
		this.dryRun = dryRun;
		if (spec.getURIsNumber() == 1)
			destinationString = spec.getURIs().iterator().next()
					.toPrivateString();
		else
			destinationString = NLS.bind(
					UIText.PushOperationUI_MultiRepositoriesDestinationString,
					Integer.valueOf(spec.getURIsNumber()));
	}

	/**
	 * @param credentialsProvider
	 */
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * Executes this directly, without showing a confirmation dialog
	 *
	 * @param monitor
	 * @return the result of the operation
	 * @throws CoreException
	 */

	public PushOperationResult execute(IProgressMonitor monitor)
			throws CoreException {
		createPushOperation();
		if (credentialsProvider != null)
			op.setCredentialsProvider(credentialsProvider);
		try {
			op.run(monitor);
			return op.getOperationResult();
		} catch (InvocationTargetException e) {
			throw new CoreException(Activator.createErrorStatus(e.getCause()
					.getMessage(), e.getCause()));
		}
	}


	private void createPushOperation() throws CoreException {
		if (remoteName != null) {
			op = new PushOperation(repository, remoteName, dryRun, timeout);
			return;
		}

		if (spec == null) {
			// spec == null => config was supplied in constructor
			// we don't use the configuration directly, as it may contain
			// unsaved changes and as we may need
			// to add the default push RefSpec here
			spec = new PushOperationSpecification();

			List<URIish> urisToPush = new ArrayList<URIish>();
			for (URIish uri : config.getPushURIs())
				urisToPush.add(uri);
			if (urisToPush.isEmpty() && !config.getURIs().isEmpty())
				urisToPush.add(config.getURIs().get(0));

			List<RefSpec> pushRefSpecs = new ArrayList<RefSpec>();
			pushRefSpecs.addAll(config.getPushRefSpecs());
			if (pushRefSpecs.isEmpty())
				// default push to all branches
				pushRefSpecs.add(DEFAULT_PUSH_REF_SPEC);

			for (URIish uri : urisToPush) {
				try {
					spec.addURIRefUpdates(uri, Transport.open(repository, uri)
							.findRemoteRefUpdatesFor(pushRefSpecs));
				} catch (NotSupportedException e) {
					throw new CoreException(Activator.createErrorStatus(
							e.getMessage(), e));
				} catch (IOException e) {
					throw new CoreException(Activator.createErrorStatus(
							e.getMessage(), e));
				}
			}
		}
		op = new PushOperation(repository, spec, dryRun, timeout);
	}

	/**
	 * Starts the operation asynchronously showing a confirmation dialog after
	 * completion
	 */
	public void start() {
		Job job = new Job(NLS.bind(UIText.PushOperationUI_PushJobName,
				destinationString)) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					execute(monitor);
				} catch (CoreException e) {
					return Activator.createErrorStatus(e.getStatus()
							.getMessage(), e);
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.PUSH.equals(family))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.schedule();
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK())
					PushResultDialog.show(repository, op.getOperationResult(),
							destinationString);
				else
					Activator.handleError(event.getResult().getMessage(), event
							.getResult().getException(), true);
			}
		});
	}

	/**
	 * @return the string denoting the remote source
	 */
	public String getDestinationString() {
		return destinationString;
	}
}
