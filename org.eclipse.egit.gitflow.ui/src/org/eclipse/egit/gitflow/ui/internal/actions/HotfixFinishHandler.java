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
import org.eclipse.egit.gitflow.op.HotfixFinishOperation;
import org.eclipse.egit.gitflow.ui.internal.JobFamilies;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.osgi.util.NLS;

/**
 * git flow hotfix finish
 */
public class HotfixFinishHandler extends AbstractGitFlowHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final GitFlowRepository gfRepo = GitFlowHandlerUtil.getRepository(event);
		if (gfRepo == null) {
			return error(UIText.Handlers_noGitflowRepositoryFound);
		}

		HotfixFinishOperation hotfixFinishOperation;
		try {
			hotfixFinishOperation = new HotfixFinishOperation(gfRepo);
			String hotfixBranch = gfRepo.getRepository().getBranch();
			String develop = gfRepo.getConfig().getDevelop();

			JobUtil.scheduleUserWorkspaceJob(hotfixFinishOperation,
					UIText.HotfixFinishHandler_finishingHotfix,
					JobFamilies.GITFLOW_FAMILY);
			IJobManager jobMan = Job.getJobManager();
			jobMan.join(JobFamilies.GITFLOW_FAMILY, null);

			MergeResult mergeResult = hotfixFinishOperation.getMergeResult();
			MergeStatus mergeStatus = mergeResult.getMergeStatus();
			if (!MergeStatus.CONFLICTING.equals(mergeStatus)) {
				return null;
			}
			if (handleConflictsOnMaster(gfRepo)) {
				return null;
			}
			MultiStatus status = createMergeConflictInfo(develop, hotfixBranch, mergeResult);
			ErrorDialog.openError(null, UIText.HotfixFinishHandler_Conflicts, null, status);
		} catch (WrongGitFlowStateException | CoreException | IOException
				| OperationCanceledException | InterruptedException e) {
			return error(e.getMessage(), e);
		}

		return null;
	}

	private boolean handleConflictsOnMaster(GitFlowRepository gfRepo)
			throws IOException {
		if (!gfRepo.isMaster()) {
			return false;
		}
		String master = gfRepo.getConfig().getMaster();
		MessageDialog.openError(null, UIText.HotfixFinishHandler_Conflicts,
				NLS.bind(UIText.HotfixFinishOperation_unexpectedConflictsHotfixAborted,
						master));
		return true;
	}
}
