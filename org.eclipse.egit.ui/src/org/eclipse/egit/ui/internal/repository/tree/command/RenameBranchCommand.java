/*******************************************************************************
 * Copyright (c) 2010 EclipseSource.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Benjamin Muskalla (EclipseSource) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.RenameBranchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.ValidationUtils;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;

/**
 * Renames a branch
 */
public class RenameBranchCommand extends
		RepositoriesViewCommandHandler<RefNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final List<RefNode> nodes = getSelectedNodes(event);
		RefNode refNode = nodes.get(0);

		Shell shell = getShell(event);
		String oldName = refNode.getObject().getName();
		String prefix;
		if (oldName.startsWith(Constants.R_HEADS))
			prefix = Constants.R_HEADS;
		else if (oldName.startsWith(Constants.R_REMOTES))
			prefix = Constants.R_REMOTES;
		else
			throw new ExecutionException(NLS.bind(
					UIText.RenameBranchCommand_WrongNameMessage, oldName));
		Repository db = refNode.getRepository();
		IInputValidator inputValidator = ValidationUtils
				.getRefNameInputValidator(db, prefix, true);
		String defaultValue = Repository.shortenRefName(oldName);
		InputDialog newNameDialog = new InputDialog(shell,
				UIText.RepositoriesView_RenameBranchTitle, NLS.bind(
						UIText.RepositoriesView_RenameBranchMessage,
						defaultValue), defaultValue, inputValidator);
		if (newNameDialog.open() == Window.OK) {
			try {
				String newName = newNameDialog.getValue();
				new RenameBranchOperation(db, refNode.getObject(), newName)
						.execute(null);
			} catch (CoreException e) {
				Activator.handleError(
						UIText.RepositoriesView_RenameBranchFailure, e, true);
			}
		}
		return null;
	}
}
