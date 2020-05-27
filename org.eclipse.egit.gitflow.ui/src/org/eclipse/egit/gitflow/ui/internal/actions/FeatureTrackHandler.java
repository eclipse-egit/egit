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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.ui.handlers.HandlerUtil;

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
		final List<Ref> refs = new ArrayList<>();
		Shell activeShell = HandlerUtil.getActiveShell(event);

		int timeout = GitSettings.getRemoteConnectionTimeout();
		FeatureListOperation featureListOperation = new FeatureListOperation(
				gfRepo, timeout);
		JobUtil.scheduleUserWorkspaceJob(featureListOperation,
				UIText.FeatureTrackHandler_fetchingRemoteFeatures,
				GITFLOW_FAMILY);
		IJobManager jobMan = Job.getJobManager();
		try {
			jobMan.join(GITFLOW_FAMILY, null);
		} catch (OperationCanceledException | InterruptedException e) {
			return error(e.getMessage(), e);
		}

		List<Ref> remoteFeatures = featureListOperation.getResult();
		if (remoteFeatures.isEmpty()) {
			MessageDialog.openInformation(activeShell, UIText.FeatureTrackHandler_noRemoteFeatures,
					UIText.FeatureTrackHandler_noRemoteFeaturesFoundOnTheConfiguredRemote);
		}
		refs.addAll(remoteFeatures);

		FeatureBranchSelectionDialog dialog = new FeatureBranchSelectionDialog(
				HandlerUtil.getActiveShell(event), refs,
				UIText.FeatureTrackHandler_ButtonOK,
				UIText.FeatureCheckoutHandler_selectFeature,
				UIText.FeatureTrackHandler_remoteFeatures,
				R_REMOTES + DEFAULT_REMOTE_NAME + SEP + gfRepo.getConfig().getFeaturePrefix(), gfRepo);

		if (dialog.open() != Window.OK) {
			return Status.CANCEL_STATUS;
		}

		Ref ref = dialog.getSelectedNode();
		FeatureTrackOperation featureTrackOperation = new FeatureTrackOperation(
				gfRepo, ref, timeout);
		JobUtil.scheduleUserWorkspaceJob(featureTrackOperation,
				UIText.FeatureTrackHandler_trackingFeature, GITFLOW_FAMILY);


		return null;
	}
}
