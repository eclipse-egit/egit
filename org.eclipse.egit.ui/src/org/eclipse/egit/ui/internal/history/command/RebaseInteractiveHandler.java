/*******************************************************************************
 * Copyright (c) 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *******************************************************************************/

package org.eclipse.egit.ui.internal.history.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.rebase.RebaseInteractiveView;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Ref.Storage;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Executes the Rebase
 */
public class RebaseInteractiveHandler extends AbstractHistoryCommandHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {

		PlotCommit commit = (PlotCommit) getSelection(getPage())
				.getFirstElement();
		if (commit == null)
			return null;
		final Repository repository = getRepository(event);
		if (repository == null)
			return null;

		if (hasCommitAlreadyBeenPushedToRemotes(repository, commit)) {
			// TODO: user confirmation
			// see RECOVERING FROM UPSTREAM REBASE at
			// https://www.kernel.org/pub/software/scm/git/docs/git-rebase.html
			boolean rebaseAnyway = MessageDialog
					.openQuestion(
							HandlerUtil.getActiveShellChecked(event),
							UIText.RebaseInteractiveHandler_CommitAlreadyPushed,
							UIText.RebaseInteractiveHandler_CommitAlreadyPushed_Message);
			if (!rebaseAnyway)
				return null;
		}

		try {
			RebaseInteractiveView rebaseInteractiveView = (RebaseInteractiveView) HandlerUtil
					.getActiveWorkbenchWindow(event).getActivePage()
					.showView(RebaseInteractiveView.VIEW_ID);

			Ref ref = getRef(commit);
			rebaseInteractiveView.startRebaseInteractiveAndOpen(repository, ref);

		} catch (Exception e) {
			Activator.handleError(e.getMessage(), e, true);
		}
		return null;
	}

	private boolean hasCommitAlreadyBeenPushedToRemotes(Repository repository,
			RevCommit commit) {
		// TODO: implementation; return true if commit has already been pushed
		// to some remote repos
		return false;
	}

	private Ref getRef(PlotCommit commit) {
		return new ObjectIdRef.Unpeeled(Storage.LOOSE, commit.getName(), commit);
	}
}
