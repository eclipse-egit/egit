/*******************************************************************************
 * Copyright (C) 2012, Mathias Kinzler <mathias.kinzler@sap.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.dialogs.CommitSelectDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * Replace with previous revision action handler.
 */
public class ReplaceWithPreviousActionHandler extends
		DiscardChangesActionHandler {

	@Override
	protected String gatherRevision(ExecutionEvent event)
			throws ExecutionException {
		Repository repository = getRepository(true, event);
		IResource[] resources = getSelectedResources(event);
		if (resources.length != 1)
			throw new ExecutionException(
					"Unexpected number of selected Resources"); //$NON-NLS-1$
		try {
			List<RevCommit> previousCommits = findPreviousCommits(repository,
					resources[0]);
			int parentCount = previousCommits.size();
			if (parentCount == 0) {
				MessageDialog
						.openError(
								getShell(event),
								UIText.ReplaceWithPreviousActionHandler_NoParentCommitDialogTitle,
								UIText.ReplaceWithPreviousActionHandler_NoParentCommitDialogMessage);
				throw new OperationCanceledException();
			} else if (parentCount > 1) {
				CommitSelectDialog dlg = new CommitSelectDialog(
						getShell(event), previousCommits);
				if (dlg.open() == Window.OK)
					return dlg.getSelectedCommit().getName();
				else
					throw new OperationCanceledException();
			} else
				return previousCommits.get(0).getName();
		} catch (IOException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
	}

	@Override
	public boolean isEnabled() {
		return super.isEnabled() && getSelectedResources().length == 1;
	}

	private List<RevCommit> findPreviousCommits(Repository repository,
			IResource resource) throws IOException {
		List<RevCommit> result = new ArrayList<RevCommit>();
		RevWalk rw = new RevWalk(repository);
		try {
			String path = getRepositoryPath(resource);
			if (path.length() > 0)
				rw.setTreeFilter(FollowFilter.create(path));
			RevCommit headCommit = rw.parseCommit(repository.getRef(
					Constants.HEAD).getObjectId());
			rw.markStart(headCommit);
			headCommit = rw.next();
			if (headCommit != null) {
				RevCommit[] headParents = headCommit.getParents();
				for (int i = 0; i < 2; i++) {
					RevCommit possibleParent = rw.next();
					for (RevCommit parent : headParents)
						if (parent.equals(possibleParent))
							result.add(possibleParent);
				}
			}
		} finally {
			rw.dispose();
		}
		return result;
	}

	private String getRepositoryPath(IResource resource) {
		return RepositoryMapping.getMapping(resource.getProject())
				.getRepoRelativePath(resource);
	}

}
