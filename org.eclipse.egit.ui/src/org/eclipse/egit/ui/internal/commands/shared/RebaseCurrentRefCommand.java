/*******************************************************************************
 * Copyright (c) 2010, 2016 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG)             - initial implementation
 *    Dariusz Luksza (dariusz@luksza.org)  - disable command when HEAD cannot be
 *                                           resolved
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 495777, 499482
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commands.shared;

import static org.eclipse.ui.handlers.HandlerUtil.getCurrentSelectionChecked;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.LaunchFinder;
import org.eclipse.egit.ui.internal.dialogs.BasicConfigurationDialog;
import org.eclipse.egit.ui.internal.dialogs.RebaseTargetSelectionDialog;
import org.eclipse.egit.ui.internal.rebase.RebaseInteractiveHandler;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.osgi.util.NLS;

/**
 * Implements "Rebase" to the currently checked out {@link Ref}
 */
public class RebaseCurrentRefCommand extends AbstractRebaseCommandHandler {

	private Ref ref;

	private boolean interactive;

	private boolean preserveMerges;

	/** */
	public RebaseCurrentRefCommand() {
		super(UIText.RebaseCurrentRefCommand_RebasingCurrentJobName,
				UIText.RebaseCurrentRefCommand_RebaseCanceledMessage);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// we need the ref from the event in createRebaseOperation
		setRef(event);
		if (ref == null)
			return null;
		return super.execute(event);
	}

	private void setRef(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = getCurrentSelectionChecked(event);
		if (currentSelection instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) currentSelection;
			Object selected = selection.getFirstElement();
			ref = getRef(selected);
		} else
			ref = null;

		Object context = event.getApplicationContext();
		if (!(context instanceof IEvaluationContext))
			return;

		final Repository repository = SelectionUtils
				.getRepository((IEvaluationContext) context);
		if (repository == null)
			return;

		BasicConfigurationDialog.show(repository);

		String currentFullBranch = getFullBranch(repository);
		if (ref != null && ref.getName().equals(currentFullBranch))
			ref = null;

		if (ref == null) {
			RebaseTargetSelectionDialog rebaseTargetSelectionDialog = new RebaseTargetSelectionDialog(
					getShell(event), repository);
			if (rebaseTargetSelectionDialog.open() == IDialogConstants.OK_ID) {
				String refName = rebaseTargetSelectionDialog.getRefName();
				try {
					ref = repository.findRef(refName);
				} catch (IOException e) {
					throw new ExecutionException(e.getMessage(), e);
				}
				interactive = rebaseTargetSelectionDialog.isInteractive();
				preserveMerges = rebaseTargetSelectionDialog.isPreserveMerges();
			} else {
				return;
			}
		} else {
			String branchName = Repository.shortenRefName(currentFullBranch);
			Config cfg = repository.getConfig();
			BranchRebaseMode rebase = cfg.getEnum(BranchRebaseMode.values(),
					ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
					ConfigConstants.CONFIG_KEY_REBASE, BranchRebaseMode.NONE);
			preserveMerges = rebase == BranchRebaseMode.PRESERVE;
			interactive = rebase == BranchRebaseMode.INTERACTIVE;
		}

		jobname = NLS.bind(
				UIText.RebaseCurrentRefCommand_RebasingCurrentJobName,
				Repository.shortenRefName(currentFullBranch), ref.getName());
	}

	@Override
	public void setEnabled(Object evaluationContext) {
		if (evaluationContext instanceof IEvaluationContext) {
			IEvaluationContext ctx = (IEvaluationContext) evaluationContext;
			Repository repo = SelectionUtils.getRepository(ctx);
			if (repo != null) {
				boolean enabled = isEnabledForState(repo,
						repo.getRepositoryState());
				setBaseEnabled(enabled);
			} else {
				setBaseEnabled(false);
			}
			return;
		}
		setBaseEnabled(true);
	}

	/**
	 * @param repo
	 * @param state
	 * @return whether this command is enabled for the repository state
	 */
	public static boolean isEnabledForState(Repository repo,
			RepositoryState state) {
		return state == RepositoryState.SAFE && hasHead(repo);
	}

	private static boolean hasHead(Repository repo) {
		try {
			Ref headRef = repo.exactRef(Constants.HEAD);
			return headRef != null && headRef.getObjectId() != null;
		} catch (IOException e) {
			return false;
		}
	}

	private String getFullBranch(Repository repository)
			throws ExecutionException {
		try {
			return repository.getFullBranch();
		} catch (IOException e) {
			throw new ExecutionException(
					UIText.RebaseCurrentRefCommand_ErrorGettingCurrentBranchMessage,
					e);
		}
	}

	@Override
	protected RebaseOperation createRebaseOperation(Repository repository)
			throws ExecutionException {
		if (LaunchFinder.shouldCancelBecauseOfRunningLaunches(repository,
				null)) {
			return null;
		}
		InteractiveHandler handler = interactive ? RebaseInteractiveHandler.INSTANCE
				: null;
		RebaseOperation operation = new RebaseOperation(repository, ref,
				handler);
		operation.setPreserveMerges(preserveMerges);
		return operation;
	}
}
