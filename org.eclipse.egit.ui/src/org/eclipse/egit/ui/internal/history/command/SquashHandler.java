/*******************************************************************************
 * Copyright (c) 2014 Maik Schreiber
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Maik Schreiber - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

/**
 * Prompts to enter a commit message for multiple commits being squashed
 * together into one.
 */
public class SquashHandler extends AbstractHistoryCommandHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(event);
		List<RevCommit> commits = getSelectedCommits();

		try {
			if (!areCommitsInCurrentBranch(commits, repository)) {
				MessageDialog.openError(getPage().getSite().getShell(),
						UIText.SquashHandler_Error_Title,
						UIText.SquashHandler_CommitsNotOnCurrentBranch);
				return null;
			}
		} catch (IOException e) {
			throw new ExecutionException(
					UIText.SquashHandler_ErrorCheckingIfCommitsAreOnCurrentBranch, e);
		}

		List<RepositoryCommit> repositoryCommits = new ArrayList<RepositoryCommit>();
		for (RevCommit commit : commits)
			repositoryCommits.add(new RepositoryCommit(repository, commit));

		final IStructuredSelection selected = new StructuredSelection(
				repositoryCommits);
		CommonUtils.runCommand(
				org.eclipse.egit.ui.internal.commit.command.SquashHandler.ID,
				selected);

		return null;
	}

	/**
	 * Returns whether the commits are on the current branch, ie. if they are
	 * reachable from the current HEAD.
	 *
	 * @param commits
	 *            the commits to check
	 * @param repository
	 *            the repository
	 * @return true if the commits are reachable from HEAD
	 * @throws IOException
	 *             if there is an I/O error
	 */
	private boolean areCommitsInCurrentBranch(Collection<RevCommit> commits,
			Repository repository) throws IOException {

		RevWalk walk = new RevWalk(repository);
		ObjectId headCommitId = repository.resolve(Constants.HEAD);
		RevCommit headCommit = walk.parseCommit(headCommitId);

		for (final RevCommit commit : commits) {
			walk.reset();
			walk.markStart(headCommit);

			RevFilter revFilter = new RevFilter() {
				@Override
				public boolean include(RevWalk walker, RevCommit cmit)
						throws StopWalkException, MissingObjectException,
						IncorrectObjectTypeException, IOException {

					return cmit.equals(commit);
				}

				@Override
				public RevFilter clone() {
					return null;
				}
			};
			walk.setRevFilter(revFilter);

			if (walk.next() == null)
				return false;
		}
		return true;
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
