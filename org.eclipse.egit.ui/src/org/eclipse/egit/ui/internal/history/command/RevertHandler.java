/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.dialogs.BasicConfigurationDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Executes the RevertCommit
 */
public class RevertHandler extends AbstractHistoryCommandHandler {
	/**
	 * Command id
	 */
	public static final String ID = "org.eclipse.egit.ui.history.Revert"; //$NON-NLS-1$

	public Object execute(ExecutionEvent event) throws ExecutionException {
		final RevCommit commit = getSelectedCommit(event);
		Repository repo = getRepository(event);
		if (repo == null)
			return null;
		BasicConfigurationDialog.show(repo);

		final IStructuredSelection selected = new StructuredSelection(
				new RepositoryCommit(repo, commit));
		CommonUtils
				.runCommand(
						org.eclipse.egit.ui.internal.commit.command.RevertHandler.ID,
						selected);
		return null;
	}
}
