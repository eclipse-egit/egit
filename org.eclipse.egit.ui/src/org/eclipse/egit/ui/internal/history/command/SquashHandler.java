/*******************************************************************************
 * Copyright (c) 2014 Maik Schreiber
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Maik Schreiber - initial implementation
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
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Prompts to enter a commit message for multiple commits being squashed
 * together into one.
 */
public class SquashHandler extends AbstractHistoryCommandHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(event);
		List<RevCommit> commits = getSelectedCommits(event);

		try {
			if (!CommitUtil.areCommitsInCurrentBranch(commits, repository)) {
				MessageDialog.openError(getPart(event).getSite().getShell(),
						UIText.SquashHandler_Error_Title,
						UIText.SquashHandler_CommitsNotOnCurrentBranch);
				return null;
			}
		} catch (IOException e) {
			throw new ExecutionException(
					UIText.SquashHandler_ErrorCheckingIfCommitsAreOnCurrentBranch, e);
		}

		List<RepositoryCommit> repositoryCommits = new ArrayList<>();
		for (RevCommit commit : commits)
			repositoryCommits.add(new RepositoryCommit(repository, commit));

		final IStructuredSelection selected = new StructuredSelection(
				repositoryCommits);
		CommonUtils.runCommand(
				org.eclipse.egit.ui.internal.commit.command.SquashHandler.ID,
				selected);

		return null;
	}

	@Override
	public boolean isEnabled() {
		GitHistoryPage page = getPage();
		if (page == null)
			return false;
		IStructuredSelection selection = getSelection(page);
		if (selection.isEmpty())
			return false;

		Repository repository = getRepository(page);
		if (repository.getRepositoryState() != RepositoryState.SAFE)
			return false;

		List elements = selection.toList();
		int parentsNotSelected = 0;
		for (Object element : elements) {
			RevCommit commit = (RevCommit) element;

			// disable action if a selected commit does not have exactly
			// one parent (this includes the root commit)
			if (commit.getParentCount() != 1)
				return false;

			RevCommit parentCommit = commit.getParent(0);
			if (!elements.contains(parentCommit))
				parentsNotSelected++;

			// disable action if there is more than one selected commit
			// whose parent has not been selected, this ensures a
			// contiguous selection of commits
			if (parentsNotSelected > 1)
				return false;
		}

		// disable action if there is not exactly one commit whose parent
		// has not been selected
		if (parentsNotSelected != 1)
			return false;

		return true;
	}

}
