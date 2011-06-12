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
package org.eclipse.egit.ui.internal.history.command;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.ui.PartInitException;

/**
 * Opens a {@link RevCommit} in the commit editor
 */
public class OpenInCommitViewerHandler extends AbstractHistoryCommandHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(event);
		if (repository != null) {
			RevWalk revWalk = new RevWalk(repository);
			for (Object selected : getSelection(getPage()).toList())
				try {
					RevCommit selectedCommit = (RevCommit) selected;

					// Re-parse commit to clear effects of TreeFilter
					RevCommit reparsedCommit = revWalk.parseCommit(selectedCommit.getId());

					CommitEditor.open(new RepositoryCommit(repository, reparsedCommit));
				} catch (IOException e) {
					Activator.showError("Error opening commit viewer", e); //$NON-NLS-1$
				} catch (PartInitException e) {
					Activator.showError("Error opening commit viewer", e); //$NON-NLS-1$
				}
		}
		return null;
	}
}
