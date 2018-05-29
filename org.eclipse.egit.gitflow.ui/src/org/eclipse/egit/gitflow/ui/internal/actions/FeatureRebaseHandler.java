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
import static org.eclipse.jgit.api.RebaseResult.Status.STOPPED;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.op.FeatureRebaseOperation;
import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.egit.gitflow.ui.internal.JobFamilies;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * git flow feature rebase
 */
public class FeatureRebaseHandler extends AbstractGitFlowHandler {
	private static final String INTERACTIVE_REBASE_VIEW_ID = "org.eclipse.egit.ui.InteractiveRebaseView"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final GitFlowRepository gfRepo = GitFlowHandlerUtil.getRepository(event);

		FeatureRebaseOperation rebaseOperation = new FeatureRebaseOperation(gfRepo);
		JobUtil.scheduleUserWorkspaceJob(rebaseOperation,
				UIText.FeatureRebaseHandler_rebasingFeature,
				JobFamilies.GITFLOW_FAMILY);
		IJobManager jobMan = Job.getJobManager();
		try {
			jobMan.join(GITFLOW_FAMILY, null);
		} catch (OperationCanceledException | InterruptedException e) {
			return error(e.getMessage(), e);
		}

		RebaseResult operationResult = rebaseOperation.getOperationResult();
		RebaseResult.Status status = operationResult.getStatus();

		if (status.isSuccessful()) {
			return null;
		}

		if (STOPPED.equals(status)) {
			try {
				showInteractiveRebaseView(event);
			} catch (PartInitException e) {
				return error(e.getMessage(), e);
			}
		}

		openWarning(operationResult);

		return null;
	}

	private void showInteractiveRebaseView(ExecutionEvent event) throws PartInitException, ExecutionException {
		HandlerUtil.getActiveWorkbenchWindowChecked(event).getActivePage()
				.showView(INTERACTIVE_REBASE_VIEW_ID);
	}

	private void openWarning(RebaseResult operationResult) {
		RebaseResult.Status status = operationResult.getStatus();
		String pluginId = Activator.getPluginId();
		MultiStatus info = new MultiStatus(pluginId, 1,
				UIText.FeatureRebaseHandler_problemsOcccurredDuringRebase, null);
		info.add(new Status(IStatus.WARNING, pluginId, NLS.bind(
				UIText.FeatureRebaseHandler_statusWas, status.name())));
		if (operationResult.getConflicts() != null && !operationResult.getConflicts().isEmpty()) {
			MultiStatus warning = createRebaseConflictWarning(operationResult);
			info.addAll(warning);
		}
		ErrorDialog.openError(null, UIText.FeatureRebaseHandler_problemsOccurred, null, info);
	}
}
