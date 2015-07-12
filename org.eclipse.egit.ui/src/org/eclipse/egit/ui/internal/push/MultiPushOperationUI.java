/*******************************************************************************
 * Copyright (C) 2015, Peter Karena <peter.karena@arcor.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Extends {@link PushOperationUI} to handle multi push operations.
 */
public class MultiPushOperationUI {
	/** push results per repository */
	protected Map<Repository, Object> results;

	private static final IStatus NOT_TRIED_STATUS = new Status(IStatus.ERROR,
			Activator.getPluginId(),
			UIText.PushUpstreamOrBranchActionHandler_NotTriedMessage);

	private static final IStatus NOT_CONFIGURED_REPOSITORY = new Status(
			IStatus.ERROR, Activator.getPluginId(),
			UIText.PushUpstreamOrBranchActionHandler_NotConfiguredRepository);

	/**
	 * Set all repositories to untried and push the repositories.
	 *
	 * @param repositories
	 *            the repositories to push
	 */
	public void setStatusAndPush(Repository[] repositories) {
		setRepositoriesNotTried(repositories);
		pushRepositories(repositories);
	}

	private void setRepositoriesNotTried(Repository[] repositories) {
		results = Collections
				.synchronizedMap(new LinkedHashMap<Repository, Object>());

		for (Repository repository : repositories) {
			if (!isRepositoryConfigured(repository)) {
				results.put(repository, NOT_CONFIGURED_REPOSITORY);
			} else {
				results.put(repository, NOT_TRIED_STATUS);
			}
		}
	}

	private void pushRepositories(final Repository[] repositories) {
		final Shell shell = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getShell();

		final String jobName = getJobName(repositories);

		Job job = new Job(NLS.bind(jobName,
				Integer.valueOf(repositories.length))) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					pushRepositories(monitor);

				} catch (CoreException e) {
					return Activator.createErrorStatus(e.getStatus()
							.getMessage(), e);
				}
				return Status.OK_STATUS;
			}

			private void pushRepositories(IProgressMonitor monitor)
					throws CoreException {

				for (Map.Entry<Repository, Object> entry : results.entrySet()) {
					Repository repository = entry.getKey();
					if (!entry.getValue().equals(NOT_CONFIGURED_REPOSITORY)) {
						RemoteConfig config = SimpleConfigurePushDialog
								.getConfiguredRemote(repository);

						PushOperationUI op = new PushOperationUI(repository,
								config.getName(), false);
						PushOperationResult execute = op.execute(monitor);
						results.put(repository, execute);
					}
				}
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
				Display display = shell.getDisplay();
				display.asyncExec(new Runnable() {
					public void run() {
						new MultiPushResultDialog(shell, results).open();
					}
				});
			}
		});
	}

	private String getJobName(final Repository[] repositories) {
		String jobName;

		if (repositories.length == 1) {
			Repository repository = repositories[0];
			String shortBranchName = getBranchName(repository);
			jobName = NLS.bind(
					UIText.PushUpstreamOrBranchActionHandler_PushingTaskName,
					shortBranchName, repository);
		} else {
			jobName = UIText.PushUpstreamOrBranchActionHandler_PushingMultipleTaskName;
		}
		return jobName;
	}

	private String getBranchName(Repository repository) {
		String shortBranchName;
		try {
			shortBranchName = repository.getBranch();
		} catch (IOException e) {
			// ignore here
			shortBranchName = ""; //$NON-NLS-1$
		}
		return shortBranchName;
	}

	private boolean isRepositoryConfigured(Repository repository) {
		final RemoteConfig config = SimpleConfigurePushDialog
				.getConfiguredRemote(repository);
		if (null == config) {
			return false;
		}
		return true;
	}

	/**
	 * @param repository
	 * @return the symbolic head of the repository
	 */
	public static Ref getHeadIfSymbolic(Repository repository) {
		try {
			Ref head = repository.getRef(Constants.HEAD);
			if (head != null && head.isSymbolic())
				return head;
			else
				return null;
		} catch (IOException e) {
			return null;
		}
	}
}
