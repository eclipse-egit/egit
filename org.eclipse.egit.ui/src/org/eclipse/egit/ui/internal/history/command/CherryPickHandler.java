/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP AG
 * and other copyright owners as documented in the project's IP log.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christian Halstrick (SAP AG) - initial implementation
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/

package org.eclipse.egit.ui.internal.history.command;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Executes a cherry pick on the currently selected commit
 */
public class CherryPickHandler extends AbstractHistoryCommandHandler {

	@Override
	public boolean isEnabled() {
		final Repository repository = getRepository(getPage());
		if (repository == null)
			return false;
		return repository.getRepositoryState().equals(RepositoryState.SAFE);
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RevCommit> commits = getSelectedCommits(event);
		Repository repo = getRepository(event);
		if (repo == null)
			return null;

		List<RepositoryCommit> repositoryCommits = new ArrayList<RepositoryCommit>();
		for (RevCommit commit : commits)
			repositoryCommits.add(new RepositoryCommit(repo, commit));

		final IStructuredSelection selected = new StructuredSelection(
				repositoryCommits);
		CommonUtils
				.runCommand(
						org.eclipse.egit.ui.internal.commit.command.CherryPickHandler.ID,
						selected);

		return null;
	}
}
