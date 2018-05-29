/*******************************************************************************
 * Copyright (c) 2013 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;

/**
 * Removes or deletes a repository, depending on user input from dialog.
 */
public class RemoveOrDeleteRepositoryCommand extends RemoveCommand {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RepositoryNode> selectedNodes = getSelectedNodes(event);
		if (selectedNodes.size() != 1) {
			return null;
		}
		Repository repository = selectedNodes.get(0).getObject();
		if (repository == null) {
			return null;
		}
		String repositoryName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);

		String message = MessageFormat.format(
				UIText.RemoveOrDeleteRepositoryCommand_DialogMessage, repositoryName);
		String[] buttonLabels = {
				UIText.RemoveOrDeleteRepositoryCommand_RemoveFromViewButton,
				UIText.RemoveOrDeleteRepositoryCommand_DeleteRepositoryButton,
				IDialogConstants.CANCEL_LABEL };
		MessageDialog dialog = new MessageDialog(
				getShell(event),
				UIText.RemoveOrDeleteRepositoryCommand_DialogTitle,
				null,
				message,
				MessageDialog.QUESTION, buttonLabels, 0);

		int result = dialog.open();
		if (result == 0) {
			// Remove from View
			super.removeRepository(event, false);
		} else if (result == 1) {
			// Delete Repository...
			super.removeRepository(event, true);
		}
		return null;
	}
}
