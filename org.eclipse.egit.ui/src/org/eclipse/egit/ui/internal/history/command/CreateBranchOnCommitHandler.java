/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.ValidationUtils;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Create a branch based on a commit.
 */
public class CreateBranchOnCommitHandler extends AbstractHistoryCommanndHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			PlotCommit commit = (PlotCommit) getSelection(event)
					.getFirstElement();
			ObjectId startAt = commit.getId();
			Repository repo = getRepository(event);
			String prompt = NLS.bind(
					UIText.CreateBranchHandler_CreatePromptMessage, startAt
							.name(), Constants.R_HEADS);

			InputDialog dlg = new InputDialog(HandlerUtil
					.getActiveShellChecked(event),
					UIText.BranchSelectionDialog_QuestionNewBranchTitle,
					prompt, "", ValidationUtils //$NON-NLS-1$
							.getRefNameInputValidator(repo, Constants.R_HEADS));
			if (dlg.open() != Window.OK)
				return null;
			RefUpdate updateRef = repo.updateRef(Constants.R_HEADS
					+ dlg.getValue());
			updateRef.setNewObjectId(startAt);
			updateRef.setRefLogMessage(
					"branch: Created from " + startAt.name(), false); //$NON-NLS-1$
			updateRef.update();
		} catch (IOException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		try {
			IStructuredSelection sel = getSelection(null);
			return sel.size() == 1
					&& sel.getFirstElement() instanceof RevCommit;
		} catch (ExecutionException e) {
			Activator.handleError(e.getMessage(), e, false);
			return false;
		}
	}
}
