/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza (dariusz@luksza.org) - disable command when HEAD cannot be
 *    										resolved
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commands.shared;

import static org.eclipse.ui.handlers.HandlerUtil.getCurrentSelectionChecked;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.BasicConfigurationDialog;
import org.eclipse.egit.ui.internal.dialogs.RebaseTargetSelectionDialog;
import org.eclipse.egit.ui.internal.rebase.RebaseHelper;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.osgi.util.NLS;

/**
 * Implements "Rebase" to the currently checked out {@link Ref}
 */
public class RebaseCurrentRefCommand extends AbstractRebaseCommandHandler {
	/** */
	public RebaseCurrentRefCommand() {
		super(null, null, null);
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		Ref ref;
		ISelection currentSelection = getCurrentSelectionChecked(event);
		if (currentSelection instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) currentSelection;
			Object selected = selection.getFirstElement();
			ref = getRef(selected);
		} else
			ref = null;

		final Repository repository = getRepository(event);
		if (repository == null)
			return null;

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
					ref = repository.getRef(refName);
				} catch (IOException e) {
					throw new ExecutionException(e.getMessage(), e);
				}
			} else
				return null;
		}

		String jobname = NLS.bind(
				UIText.RebaseCurrentRefCommand_RebasingCurrentJobName,
				Repository.shortenRefName(currentFullBranch), ref.getName());
		RebaseHelper.runRebaseJob(repository, jobname, ref);
		return null;
	}

	@Override
	public void setEnabled(Object evaluationContext) {
		if (evaluationContext instanceof IEvaluationContext) {
			IEvaluationContext ctx = (IEvaluationContext) evaluationContext;
			Object selection = getSelection(ctx);
			if (selection instanceof ISelection) {
				Repository repo = getRepository((ISelection) selection, getActiveEditorInput(ctx));
				if (repo != null) {
					boolean enabled = isEnabledForState(repo,
							repo.getRepositoryState());
					setBaseEnabled(enabled);
				} else
					setBaseEnabled(false);
				return;
			}
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
			Ref headRef = repo.getRef(Constants.HEAD);
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
}
