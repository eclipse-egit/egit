/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.actions;

import static org.eclipse.egit.gitflow.op.GitFlowOperation.SEP;
import static org.eclipse.egit.gitflow.ui.Activator.error;
import static org.eclipse.egit.gitflow.ui.internal.JobFamilies.GITFLOW_FAMILY;
import static org.eclipse.jgit.lib.Constants.DEFAULT_REMOTE_NAME;
import static org.eclipse.jgit.lib.Constants.R_REMOTES;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.settings.GitSettings;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.op.FeatureListOperation;
import org.eclipse.egit.gitflow.op.FeatureTrackOperation;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.gitflow.ui.internal.dialogs.FeatureBranchSelectionDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * git flow feature track
 */
public class FeatureTrackHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final GitFlowRepository gfRepo = GitFlowHandlerUtil.getRepository(event);
		if (gfRepo == null) {
			return error(UIText.Handlers_noGitflowRepositoryFound);
		}
		int timeout = GitSettings.getRemoteConnectionTimeout();
		final FeatureListOperation featureListOperation = new FeatureListOperation(
				gfRepo, timeout);
		JobUtil.scheduleUserWorkspaceJob(featureListOperation,
				UIText.FeatureTrackHandler_fetchingRemoteFeatures,
				GITFLOW_FAMILY, new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent jobChangeEvent) {
						if (!jobChangeEvent.getResult().isOK()) {
							return;
						}
						openFeatureSelectionDialog(gfRepo,
								featureListOperation.getResult(), timeout);
					}
				});
		return null;
	}

	private void openFeatureSelectionDialog(GitFlowRepository gfRepo,
			List<Ref> remoteFeatures, int timeout) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			IWorkbenchWindow window = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();
			if (window == null) {
				return;
			}
			Shell shell = window.getShell();
			if (remoteFeatures.isEmpty()) {
				MessageDialog.openInformation(shell,
						UIText.FeatureTrackHandler_noRemoteFeatures,
						UIText.FeatureTrackHandler_noRemoteFeaturesFoundOnTheConfiguredRemote);
				return;
			}
			String prefix = R_REMOTES + DEFAULT_REMOTE_NAME + SEP
					+ gfRepo.getConfig().getFeaturePrefix();
			FeatureBranchSelectionDialog dialog = new FeatureBranchSelectionDialog(
					shell, remoteFeatures, UIText.FeatureTrackHandler_ButtonOK,
					UIText.FeatureCheckoutHandler_selectFeature,
					UIText.FeatureTrackHandler_remoteFeatures, prefix, gfRepo);
			if (dialog.open() != Window.OK) {
				return;
			}
			Ref ref = dialog.getSelectedNode();
			JobUtil.scheduleUserWorkspaceJob(
					new FeatureTrackOperation(gfRepo, ref, timeout),
					UIText.FeatureTrackHandler_trackingFeature, GITFLOW_FAMILY);
		});
	}
}
