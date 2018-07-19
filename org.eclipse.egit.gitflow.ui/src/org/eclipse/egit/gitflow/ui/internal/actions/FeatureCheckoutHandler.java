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

import static org.eclipse.egit.gitflow.ui.Activator.error;
import static org.eclipse.egit.gitflow.ui.internal.JobFamilies.GITFLOW_FAMILY;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.op.FeatureCheckoutOperation;
import org.eclipse.egit.gitflow.ui.internal.JobFamilies;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.gitflow.ui.internal.dialogs.FeatureBranchSelectionDialog;
import org.eclipse.egit.ui.internal.branch.CleanupUncomittedChangesDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * git flow feature checkout
 */
public class FeatureCheckoutHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final GitFlowRepository gfRepo = GitFlowHandlerUtil.getRepository(event);
		if (gfRepo == null) {
			return error(UIText.Handlers_noGitflowRepositoryFound);
		}

		Repository repository = gfRepo.getRepository();

		final List<Ref> refs = gfRepo.getFeatureBranches();

		FeatureBranchSelectionDialog dialog = new FeatureBranchSelectionDialog(
				HandlerUtil.getActiveShell(event), refs,
				UIText.FeatureCheckoutHandler_ButtonOK,
				UIText.FeatureCheckoutHandler_selectFeature,
				UIText.FeatureCheckoutHandler_localFeatures,
				Constants.R_HEADS + gfRepo.getConfig().getFeaturePrefix(), gfRepo);

		if (dialog.open() != Window.OK) {
			return null;
		}
		final Ref ref = dialog.getSelectedNode();

		try {
			String featureName = gfRepo.getFeatureBranchName(ref);
			// TODO: consider using BranchOperationUI because checkout can take
			// a long time on large repositories
			FeatureCheckoutOperation checkoutOperation = new FeatureCheckoutOperation(
					gfRepo, featureName);
			JobUtil.scheduleUserWorkspaceJob(checkoutOperation,
					UIText.FeatureCheckoutHandler_checkingOutFeature,
					JobFamilies.GITFLOW_FAMILY);
			IJobManager jobMan = Job.getJobManager();
			try {
				jobMan.join(GITFLOW_FAMILY, null);
			} catch (OperationCanceledException | InterruptedException e) {
				return error(e.getMessage(), e);
			}

			CheckoutResult result = checkoutOperation.getResult();
			if (!CheckoutResult.Status.OK.equals(result.getStatus())) {
				Shell shell = HandlerUtil.getActiveShell(event);
				if (!handleUncommittedFiles(gfRepo.getRepository(), shell,
						repository.getWorkTree().getName())) {
					return Status.CANCEL_STATUS;
				} else {
					JobUtil.scheduleUserWorkspaceJob(checkoutOperation,
							UIText.FeatureCheckoutHandler_checkingOutFeature,
							JobFamilies.GITFLOW_FAMILY);
				}
			}
		} catch (GitAPIException e) {
			throw new RuntimeException(e);
		}

		return null;
	}

	private boolean handleUncommittedFiles(Repository repo, Shell shell,
			String repoName) throws GitAPIException {
		try (Git git = new Git(repo)) {
			org.eclipse.jgit.api.Status status = git.status().call();

			if (status.hasUncommittedChanges()) {
				List<String> files = new ArrayList<String>(
						status.getUncommittedChanges());
				Collections.sort(files);
				CleanupUncomittedChangesDialog cleanupUncomittedChangesDialog = new CleanupUncomittedChangesDialog(
						shell,
						MessageFormat
								.format(UIText.FeatureCheckoutHandler_cleanupDialog_title,
										repoName),
						UIText.FeatureCheckoutHandler_cleanupDialog_text,
						repo, files);
				cleanupUncomittedChangesDialog.open();
				return cleanupUncomittedChangesDialog.shouldContinue();
			} else {
				return true;
			}
		}
	}
}
