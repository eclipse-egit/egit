/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.eclipse.jface.wizard.WizardDialog;
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

	/**
	 * First the unconfigured repositories pop up to get configured, then all repositories are pushed.
	 *
	 * @param repositories the repositories to push
	 */
	public void configureAndPushRepositories(Repository[] repositories) {
		results = Collections
				.synchronizedMap(new LinkedHashMap<Repository, Object>());

		for (Repository repository : repositories) {
			results.put(repository, NOT_TRIED_STATUS);
		}

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getShell();

		configureRepositories(repositories, shell);
		pushRepositories(repositories, shell);
	}

	private void pushRepositories(final Repository[] repositories,
			final Shell shell) {

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
				for (Repository repository : repositories) {
					if (isRepositoryConfigured(repository)) {
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

	private void configureRepositories(final Repository[] repositories,
			final Shell shell) {

		List<Repository> unconfiguredRepositories = getUnconfiguredRepositories(repositories);

		for (Repository repository : unconfiguredRepositories) {
			Ref head = MultiPushOperationUI.getHeadIfSymbolic(repository);
			if (head != null) {
				PushBranchWizard pushBranchWizard = new PushBranchWizard(
						repository, head.getTarget(), true);

				WizardDialog dlg = new WizardDialog(shell, pushBranchWizard);
				dlg.setBlockOnOpen(true);
				dlg.open();
			}
		}
	}

	/**
	 * Repositories that don't has a configured remote or upstream branch.
	 *
	 * @param repositories
	 *            all unconfigured repositories
	 * @return a list of unconfigured repositories
	 */
	private List<Repository> getUnconfiguredRepositories(Repository[] repositories) {
		List<Repository> uncofiguredRepositories = new ArrayList<Repository>();

		for (Repository repository : repositories) {
			if (!isRepositoryConfigured(repository)) {
				uncofiguredRepositories.add(repository);
			}
		}
		return uncofiguredRepositories;
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
