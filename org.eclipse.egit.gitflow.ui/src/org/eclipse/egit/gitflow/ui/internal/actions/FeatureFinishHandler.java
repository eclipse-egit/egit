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
import org.eclipse.egit.gitflow.ui.internal.JobFamilies;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;

/**
 * git flow feature finish
 */
public class FeatureFinishHandler extends AbstractFinishHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final GitFlowRepository gfRepo = GitFlowHandlerUtil.getRepository(event);
		try {
			FeatureFinishOperation operation = new FeatureFinishOperation(gfRepo);
			String featureBranch = gfRepo.getRepository().getBranch();
			String develop = gfRepo.getConfig().getDevelop();

			JobUtil.scheduleUserWorkspaceJob(operation,
					UIText.FeatureFinishHandler_finishingFeature,
					JobFamilies.GITFLOW_FAMILY);
			IJobManager jobMan = Job.getJobManager();
			jobMan.join(JobFamilies.GITFLOW_FAMILY, null);

			MergeResult mergeResult = operation.getMergeResult();
			MergeStatus mergeStatus = mergeResult.getMergeStatus();
			if (MergeStatus.CONFLICTING.equals(mergeStatus)) {
				MultiStatus warning = createConflictWarning(develop, featureBranch, mergeResult);
				ErrorDialog.openError(null, UIText.FeatureFinishHandler_Conflicts, null, warning);
			}
		} catch (WrongGitFlowStateException | CoreException | IOException
				| OperationCanceledException | InterruptedException e) {
			return error(e.getMessage(), e);
		}

		return null;
	}
}
