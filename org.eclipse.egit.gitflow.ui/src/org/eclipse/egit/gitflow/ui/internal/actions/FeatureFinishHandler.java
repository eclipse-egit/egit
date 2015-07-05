/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.actions;

import static org.eclipse.egit.gitflow.ui.Activator.error;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.WrongGitFlowStateException;
import org.eclipse.egit.gitflow.op.FeatureFinishOperation;
import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.egit.gitflow.ui.internal.JobFamilies;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.gitflow.ui.internal.dialogs.FinishFeatureDialog;
import org.eclipse.egit.ui.internal.UIRepositoryUtils;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * git flow feature finish
 */
public class FeatureFinishHandler extends AbstractGitFlowHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final GitFlowRepository gfRepo = GitFlowHandlerUtil.getRepository(event);
		String featureBranch;
		Repository repo = gfRepo.getRepository();
		try {
			featureBranch = repo.getBranch();
		} catch (IOException e) {
			return error(e.getMessage(), e);
		}

		Shell activeShell = HandlerUtil.getActiveShell(event);
		FinishFeatureDialog dialog = new FinishFeatureDialog(activeShell,
				featureBranch);
		if (dialog.open() != Window.OK) {
			return null;
		}
		boolean squash = dialog.isSquash();

		try {
			try {
				if (squash && !UIRepositoryUtils.handleUncommittedFiles(repo, activeShell))
					return null;
			} catch (GitAPIException e) {
				Activator.logError(e.getMessage(), e);
				return null;
			}

			FeatureFinishOperation operation = new FeatureFinishOperation(
					gfRepo);
			operation.setSquash(squash);
			String develop = gfRepo.getConfig().getDevelop();

			JobUtil.scheduleUserWorkspaceJob(operation,
					UIText.FeatureFinishHandler_finishingFeature,
					JobFamilies.GITFLOW_FAMILY);
			IJobManager jobMan = Job.getJobManager();
			jobMan.join(JobFamilies.GITFLOW_FAMILY, null);

			MergeResult mergeResult = operation.getMergeResult();
			MergeStatus mergeStatus = mergeResult.getMergeStatus();
			if (MergeStatus.CONFLICTING.equals(mergeStatus)) {
				MultiStatus status = createMergeConflictInfo(develop, featureBranch, mergeResult);
				ErrorDialog.openError(null, UIText.FeatureFinishHandler_Conflicts, null, status);
			}
		} catch (WrongGitFlowStateException | CoreException | IOException
				| OperationCanceledException | InterruptedException e) {
			return error(e.getMessage(), e);
		}

		return null;
	}
}
