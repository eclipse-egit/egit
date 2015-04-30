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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.op.FeatureRebaseOperation;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.UIJob;

/**
 * git flow feature rebase
 */
@SuppressWarnings("restriction")
public class FeatureRebaseHandler extends AbstractHandler {
	private static final String INTERACTIVE_REBASE_VIEW_ID = "org.eclipse.egit.ui.InteractiveRebaseView"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil
				.getCurrentSelection(event);
		final Repository repository = SelectionUtils.getRepository(selection);
		final GitFlowRepository gfRepo = new GitFlowRepository(repository);

		Job job = new UIJob(HandlerUtil.getActiveShell(event).getDisplay(),
				UIText.FeatureRebaseHandler_rebasingFeature) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				try {
					FeatureRebaseOperation featureRebaseOperation = new FeatureRebaseOperation(
							gfRepo);
					featureRebaseOperation.execute(monitor);
					RebaseResult.Status status = featureRebaseOperation
							.getOperationResult().getStatus();
					if (RebaseResult.Status.FAILED.equals(status)) {
						return error(UIText.FeatureRebaseHandler_rebaseFailed);
					}
					if (RebaseResult.Status.CONFLICTS.equals(status)) {
						MessageDialog
								.openInformation(
										getDisplay().getActiveShell(),
										UIText.FeatureRebaseHandler_conflicts,
										UIText.FeatureRebaseHandler_resolveConflictsManually);
						PlatformUI.getWorkbench().getActiveWorkbenchWindow()
								.getActivePage()
								.showView(INTERACTIVE_REBASE_VIEW_ID);
						return Status.OK_STATUS;
					}
				} catch (CoreException e) {
					return error(e.getMessage(), e);
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();

		return null;
	}
}
