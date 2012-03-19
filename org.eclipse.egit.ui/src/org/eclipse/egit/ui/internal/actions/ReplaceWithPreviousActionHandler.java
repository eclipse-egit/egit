/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.dialogs.CommitSelectDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
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
		try {
			final RevWalk walk = new RevWalk(repository);
			ObjectId commitId = repository.resolve(repository.getFullBranch());
			RevCommit commit = walk.parseCommit(commitId);
			int parentCount = commit.getParentCount();
			if (parentCount == 0) {
				MessageDialog
						.openError(
								getShell(event),
								UIText.ReplaceWithPreviousActionHandler_NoParentCommitDialogTitle,
								UIText.ReplaceWithPreviousActionHandler_NoParentCommitDialogMessage);
				throw new OperationCanceledException();
			} else if (parentCount > 1) {
				List<RevCommit> commits = new ArrayList<RevCommit>();
				for (int i = 0; i < parentCount; i++)
					commits.add(walk.parseCommit(commit.getParent(i)));
				CommitSelectDialog dlg = new CommitSelectDialog(
						getShell(event), commits);
				if (dlg.open() == Window.OK)
					return dlg.getSelectedCommit().getName();
				else
					throw new OperationCanceledException();
			} else
				return commit.getParent(0).getName();
		} catch (IOException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
	}

}
