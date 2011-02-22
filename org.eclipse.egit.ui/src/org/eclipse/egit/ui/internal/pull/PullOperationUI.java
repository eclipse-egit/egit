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
package org.eclipse.egit.ui.internal.pull;

import java.io.IOException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.core.op.PullOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.job.JobUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * UI wrapper for {@link PullOperation}
 */
public class PullOperationUI extends JobChangeAdapter implements
		IEGitOperation, IJobChangeListener {
	private final Repository repository;

	private final PullOperation pull;

	/**
	 * @param repository
	 */
	public PullOperationUI(Repository repository) {
		this.repository = repository;
		this.pull = new PullOperation(repository, Activator.getDefault()
				.getPreferenceStore().getInt(
						UIPreferences.REMOTE_CONNECTION_TIMEOUT));
	}

	/**
	 * Starts this operation asynchronously
	 */
	public void start() {
		String errorMessage = null;
		String branchName;
		try {
			try {
				branchName = repository.getFullBranch();
			} catch (IOException e) {
				Activator.logError(e.getMessage(), e);
				errorMessage = UIText.PullOperationUI_UnexpectedExceptionGettingBranchMessage;
				return;
			}
			if (!branchName.startsWith(Constants.R_HEADS)) {
				errorMessage = UIText.PullOperationUI_NoLocalBranchMessage;
				return;
			}

			String shortBranchName = branchName.substring(Constants.R_HEADS
					.length());

			String remoteBranchName = repository.getConfig().getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, shortBranchName,
					ConfigConstants.CONFIG_KEY_MERGE);
			if (remoteBranchName == null) {
				errorMessage = NLS
						.bind(
								UIText.PullOperationUI_BranchNotConfiguredForPullMessage,
								shortBranchName);
				return;
			}
			String repoName = Activator.getDefault().getRepositoryUtil()
					.getRepositoryName(repository);

			String jobName = NLS.bind(UIText.PullOperationUI_PullingTaskName,
					shortBranchName, repoName);
			JobUtil.scheduleUserJob(this, jobName, JobFamilies.PULL, this);

		} finally {
			if (errorMessage != null)
				MessageDialog.openError(PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell(),
						UIText.PullOperationUI_PullErrorWindowTitle,
						errorMessage);
		}
	}

	/**
	 * Starts this operation synchronously.
	 *
	 * Note that the asynchronous method has more elaborate checks providing a
	 * specific error dialog in case of missing configuration (see
	 * {@link #start()})
	 *
	 * @param monitor
	 * @throws CoreException
	 */
	public void execute(IProgressMonitor monitor) throws CoreException {
		pull.execute(monitor);
	}

	public void done(IJobChangeEvent event) {
		IStatus result = event.getJob().getResult();
		if (result.getSeverity() == IStatus.CANCEL) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					Shell shell = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getShell();
					MessageDialog
							.openInformation(
									shell,
									UIText.PullOperationUI_PullCanceledWindowTitle,
									UIText.PullOperationUI_PullOperationCanceledMessage);
				}
			});
		} else if (result.isOK()) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					Shell shell = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getShell();
					new PullResultDialog(shell, repository, pull.getResult())
							.open();
				}
			});
		}
	}

	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
}
