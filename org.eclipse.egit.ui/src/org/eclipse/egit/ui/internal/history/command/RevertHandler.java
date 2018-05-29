/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christian Halstrick (SAP AG) - initial implementation
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/

package org.eclipse.egit.ui.internal.history.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.CommitUtil;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.dialogs.BasicConfigurationDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Executes the RevertCommit
 */
public class RevertHandler extends AbstractHistoryCommandHandler {
	/**
	 * Command id
	 */
	public static final String ID = "org.eclipse.egit.ui.history.Revert"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final List<RevCommit> commits = getSelectedCommits(event);
		Repository repo = getRepository(event);
		if (repo == null)
			return null;

		try {
			boolean allOnCurrentBranch = true;
			for (RevCommit commit : commits) {
				if (!CommitUtil.isCommitInCurrentBranch(commit, repo)) {
					allOnCurrentBranch = false;
					break;
				}
			}
			if (!allOnCurrentBranch) {
				MessageDialog.openError(
						HandlerUtil.getActiveShellChecked(event),
						UIText.RevertHandler_Error_Title,
						UIText.RevertHandler_CommitsNotOnCurrentBranch);
				return null;
			}
		} catch (IOException e) {
			throw new ExecutionException(
					UIText.RevertHandler_ErrorCheckingIfCommitsAreOnCurrentBranch,
					e);
		}

		BasicConfigurationDialog.show(repo);

		List<RepositoryCommit> repositoryCommits = new ArrayList<>();
		for (RevCommit commit : commits)
			repositoryCommits.add(new RepositoryCommit(repo, commit));
		final IStructuredSelection selected = new StructuredSelection(
				repositoryCommits);
		CommonUtils
				.runCommand(
						org.eclipse.egit.ui.internal.commit.command.RevertHandler.ID,
						selected);
		return null;
	}
}
