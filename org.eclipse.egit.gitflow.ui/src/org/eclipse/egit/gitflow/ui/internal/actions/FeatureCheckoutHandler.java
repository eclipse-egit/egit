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
import static org.eclipse.jgit.lib.Constants.R_HEADS;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.op.FeatureCheckoutOperation;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.gitflow.ui.internal.dialog.AbstractSelectionDialog;
import org.eclipse.egit.ui.internal.branch.CleanupUncomittedChangesDialog;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.UIJob;

/**
 * git flow feature checkout
 */
@SuppressWarnings("restriction")
public class FeatureCheckoutHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil
				.getCurrentSelection(event);

		final Repository repository = SelectionUtils.getRepository(selection);

		final GitFlowRepository gfRepo = new GitFlowRepository(repository);

		final List<Ref> refs = gfRepo.getFeatureBranches();

		AbstractSelectionDialog<Ref> dialog = new AbstractSelectionDialog<Ref>(
				HandlerUtil.getActiveShell(event), refs,
				UIText.FeatureCheckoutHandler_selectFeature,
				UIText.FeatureCheckoutHandler_localFeatures) {
			@Override
			protected String getPrefix() {
				return R_HEADS + gfRepo.getFeaturePrefix();
			}
		};
		if (dialog.open() != Window.OK) {
			return null;
		}
		final Ref ref = dialog.getSelectedNode();

		UIJob trackingJob = new UIJob(
				UIText.FeatureCheckoutHandler_checkingOutFeature) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				try {
					String featureName = gfRepo.getFeatureBranchName(ref);
					FeatureCheckoutOperation checkoutOperation = new FeatureCheckoutOperation(
							gfRepo, featureName);
					checkoutOperation.execute(monitor);
					CheckoutResult result = checkoutOperation.getResult();
					if (!CheckoutResult.Status.OK.equals(result.getStatus())) {
						Shell shell = getDisplay().getActiveShell();
						if (!handleUncommittedFiles(gfRepo.getRepository(),
								shell, repository.getWorkTree().getName())) {
							return Status.CANCEL_STATUS;
						} else {
							checkoutOperation.execute(monitor);
						}
					}
				} catch (CoreException e) {
					return error(e.getMessage(), e);
				} catch (GitAPIException e) {
					throw new RuntimeException(e);
				}
				return Status.OK_STATUS;
			}
		};
		trackingJob.setUser(true);
		trackingJob.schedule();

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
								.format(UIText.AbstractRebaseCommandHandler_cleanupDialog_title,
										repoName),
						UIText.AbstractRebaseCommandHandler_cleanupDialog_text,
						repo, files);
				cleanupUncomittedChangesDialog.open();
				return cleanupUncomittedChangesDialog.shouldContinue();
			} else {
				return true;
			}
		}
	}
}
