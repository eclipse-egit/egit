/*******************************************************************************
 * Copyright (c) 2011, 2017 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 495512
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.FetchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.credentials.EGitCredentialsProvider;
import org.eclipse.egit.ui.internal.jobs.RepositoryJob;
import org.eclipse.egit.ui.internal.jobs.RepositoryJobResultAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.SubmoduleConfig.FetchRecurseSubmodulesMode;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

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
	 * @param recurseSubmodules
	 */
	public void setRecurseSubmodules(
			FetchRecurseSubmodulesMode recurseSubmodules) {
		op.setRecurseSubmodules(recurseSubmodules);
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
			if (op.getCredentialsProvider() == null)
				op.setCredentialsProvider(new EGitCredentialsProvider());
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
		final Repository repo = repository;
		if (repo == null) {
			return;
		}
		Job job = new RepositoryJob(NLS.bind(
				UIText.FetchOperationUI_FetchJobName,
				sourceString), UIPreferences.SHOW_FETCH_POPUP_SUCCESS) {

			private FetchResult result;

			@Override
			public IStatus performJob(IProgressMonitor monitor) {
				try {
					result = execute(monitor);
				} catch (CoreException e) {
					return Activator.createErrorStatus(e.getStatus()
							.getMessage(), e);
				}
				return Status.OK_STATUS;
			}

			@Override
			protected IAction getAction() {
				return new ShowResultAction(repo, result, sourceString);
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
		job.schedule();
	}

	/**
	 * @return the string denoting the remote source
	 */
	public String getSourceString(){
		return sourceString;
	}

	private static class ShowResultAction extends RepositoryJobResultAction {

		private final FetchResult result;

		private final String source;

		public ShowResultAction(@NonNull Repository repository,
				FetchResult result, String source) {
			super(repository, UIText.FetchOperationUI_ShowFetchResult);
			this.result = result;
			this.source = source;
		}

		@Override
		protected void showResult(@NonNull Repository repository) {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getShell();
			FetchResultDialog dialog = new FetchResultDialog(shell, repository,
					result, source);
			dialog.open();
		}
	}

}
