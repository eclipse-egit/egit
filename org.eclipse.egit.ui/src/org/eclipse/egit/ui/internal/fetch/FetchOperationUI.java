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
package org.eclipse.egit.ui.internal.fetch;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.internal.op.FetchOperation;
import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;

/**
 * UI Wrapper for {@link FetchOperation}
 */
public class FetchOperationUI {
	private final Repository repository;

	private final FetchOperation op;

	private final String sourceString;

	/**
	 * @param repository
	 * @param config
	 * @param timeout
	 * @param dryRun
	 *
	 */
	public FetchOperationUI(Repository repository, RemoteConfig config,
			int timeout, boolean dryRun) {
		this.repository = repository;
		op = new FetchOperation(repository, config, timeout, dryRun);
		sourceString = NLS.bind("{0} - {1}", repository.getDirectory() //$NON-NLS-1$
				.getParentFile().getName(), config.getName());

	}

	/**
	 * @param repository
	 * @param uri
	 * @param specs
	 * @param timeout
	 * @param dryRun
	 */
	public FetchOperationUI(Repository repository, URIish uri,
			List<RefSpec> specs, int timeout, boolean dryRun) {
		this.repository = repository;
		op = new FetchOperation(repository, uri, specs, timeout, dryRun);
		sourceString = uri.toPrivateString();
	}

	/**
	 * @param credentialsProvider
	 */
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		op.setCredentialsProvider(credentialsProvider);
	}

	/**
	 * @param tagOpt
	 */
	public void setTagOpt(TagOpt tagOpt) {
		op.setTagOpt(tagOpt);
	}

	/**
	 * Executes this directly, without showing a confirmation dialog
	 *
	 * @param monitor
	 * @return the result of the operation
	 * @throws CoreException
	 */
	public FetchResult execute(IProgressMonitor monitor) throws CoreException {
		try {
			op.run(monitor);
			return op.getOperationResult();
		} catch (InvocationTargetException e) {
			throw new CoreException(Activator.createErrorStatus(e.getCause()
					.getMessage(), e.getCause()));
		}
	}

	/**
	 * Starts the operation asynchronously showing a confirmation dialog after
	 * completion
	 */
	public void start() {
		Job job = new Job(NLS.bind(UIText.FetchOperationUI_FetchJobName,
				sourceString)) {
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
				if (JobFamilies.FETCH.equals(family))
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
					FetchResultDialog.show(repository, op.getOperationResult(),
							sourceString);
				else
					Activator.handleError(event.getResult().getMessage(), event
							.getResult().getException(), true);
			}
		});
	}

	/**
	 * @return the string denoting the remote source
	 */
	public String getSourceString(){
		return sourceString;
	}
}
