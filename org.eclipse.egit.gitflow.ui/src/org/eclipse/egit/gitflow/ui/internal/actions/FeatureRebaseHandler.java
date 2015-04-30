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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.op.FeatureRebaseOperation;
import org.eclipse.egit.gitflow.ui.internal.JobFamilies;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * git flow feature rebase
 */
public class FeatureRebaseHandler extends AbstractHandler {
	private static final String INTERACTIVE_REBASE_VIEW_ID = "org.eclipse.egit.ui.InteractiveRebaseView"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final GitFlowRepository gfRepo = GitFlowHandlerUtil.getRepository(event);

		FeatureRebaseOperation featureRebaseOperation = new FeatureRebaseOperation(gfRepo);
		JobUtil.scheduleUserWorkspaceJob(featureRebaseOperation,
				UIText.FeatureRebaseHandler_rebasingFeature,
				JobFamilies.GITFLOW_FAMILY);

		RebaseResult.Status status = featureRebaseOperation
				.getOperationResult().getStatus();
		if (RebaseResult.Status.FAILED.equals(status)) {
			return error(UIText.FeatureRebaseHandler_rebaseFailed);
		}
		if (!RebaseResult.Status.CONFLICTS.equals(status)) {
			return null;
		}
		MessageDialog.openInformation(HandlerUtil.getActiveShell(event),
				UIText.FeatureRebaseHandler_conflicts,
				UIText.FeatureRebaseHandler_resolveConflictsManually);
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().showView(INTERACTIVE_REBASE_VIEW_ID);
		} catch (PartInitException e) {
			return error(e.getMessage(), e);
		}

		return null;
	}
}
