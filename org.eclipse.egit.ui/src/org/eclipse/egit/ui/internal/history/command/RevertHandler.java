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
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.dialogs.BasicConfigurationDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RevertCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Executes the RevertCommit
 */
public class RevertHandler extends AbstractHistoryCommandHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repo = getRepository(event);
		BasicConfigurationDialog.show(repo);
		RevCommit commit = (RevCommit) getSelection(getPage()).getFirstElement();
		RevCommit newHead;

		RevertCommand revert;
		Git git = new Git(repo);
		try {
			revert = git.revert().include(commit.getId());
			newHead = revert.call();
			if (newHead != null && revert.getRevertedRefs().isEmpty())
				MessageDialog.openWarning(getPart(event).getSite().getShell(),
						UIText.RevertHandler_NoRevertTitle,
						UIText.RevertHandler_AlreadyRevertedMessae);
		} catch (Exception e) {
			Activator.handleError(UIText.RevertOperation_InternalError, e, true);
			return null;
		}
		if (newHead == null)
			Activator.showError(UIText.RevertOperation_Failed, null);
		return null;
	}
}
