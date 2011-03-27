/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.dialogs.RebaseTargetSelectionDialog;
import org.eclipse.egit.ui.internal.rebase.RebaseHelper;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;

/**
 * An action handler for rebase current branch to a given base branch.
 *
 * @see RebaseOperation
 */
public class RebaseActionHandler extends RepositoryActionHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repo = getRepository(true, event);
		if (repo == null)
			return null;

		Ref ref;
		Shell shell = getShell(event);
		RebaseTargetSelectionDialog selectionDialog = new RebaseTargetSelectionDialog(
				shell, repo);
		if (selectionDialog.open() == IDialogConstants.OK_ID) {
			String refName = selectionDialog.getRefName();
			try {
				ref = repo.getRef(refName);
			} catch (IOException e) {
				throw new ExecutionException(e.getMessage(), e);
			}
		} else
			return null;

		String jobname = NLS.bind(
				UIText.RebaseCurrentRefCommand_RebasingCurrentJobName, ref
						.getName());
		RebaseHelper.runRebaseJob(repo, jobname, ref);

		return null;
	}

}
