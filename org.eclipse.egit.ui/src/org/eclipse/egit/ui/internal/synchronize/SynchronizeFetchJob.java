/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.net.URISyntaxException;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.credentials.EGitCredentialsProvider;
import org.eclipse.egit.ui.internal.fetch.FetchOperationUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

class SynchronizeFetchJob extends WorkspaceJob {

	private final int timeout;

	private final GitSynchronizeDataSet gsdSet;

	SynchronizeFetchJob(GitSynchronizeDataSet gsdSet) {
		super(UIText.SynchronizeFetchJob_JobName);
		this.gsdSet = gsdSet;
		timeout = Activator.getDefault().getPreferenceStore()
				.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, gsdSet.size());
		progress.setTaskName(UIText.SynchronizeFetchJob_TaskName);

		for (GitSynchronizeData gsd : gsdSet) {
			Repository repo = gsd.getRepository();
			StoredConfig repoConfig = repo.getConfig();
			String remoteName = gsd.getDstRemoteName();
			if (remoteName == null) {
				progress.worked(1);
				continue;
			}

			progress.subTask(NLS.bind(UIText.SynchronizeFetchJob_SubTaskName,
					remoteName));

			RemoteConfig config;
			try {
				config = new RemoteConfig(repoConfig, remoteName);
			} catch (URISyntaxException e) {
				Activator.logError(e.getMessage(), e);
				progress.worked(1);
				continue;
			}

			FetchOperationUI fetchOperationUI = new FetchOperationUI(repo,
					config, timeout, false);
			fetchOperationUI.setCredentialsProvider(new EGitCredentialsProvider());
			try {
				fetchOperationUI.execute(progress.newChild(1));
				gsd.updateRevs();
			} catch (Exception e) {
				showInformationDialog(remoteName);
				Activator.logError(e.getMessage(), e);
			}
		}
		return Status.OK_STATUS;
	}

	private void showInformationDialog(final String remoteName) {
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openInformation(display.getActiveShell(), NLS
						.bind(UIText.SynchronizeFetchJob_FetchFailedTitle,
								remoteName), NLS.bind(
						UIText.SynchronizeFetchJob_FetchFailedMessage,
						UIText.GitPreferenceRoot_fetchBeforeSynchronization));
			}
		});
	}

}
