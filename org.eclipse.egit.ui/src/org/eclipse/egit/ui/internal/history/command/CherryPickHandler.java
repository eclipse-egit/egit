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
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.CherryPickCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Executes the CherryPick
 */
public class CherryPickHandler extends AbstractHistoryCommandHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RevCommit commit = (RevCommit) getSelection(getPage()).getFirstElement();
		RevCommit newHead;
		Repository repo = getRepository(event);

		CherryPickCommand cherryPick;
		Git git = new Git(repo);
		try {
			cherryPick = git.cherryPick().include(commit.getId());
			newHead = cherryPick.call();
			if (newHead != null && cherryPick.getCherryPickedRefs().isEmpty())
				MessageDialog.openWarning(getPart(event).getSite().getShell(),
						UIText.CherryPickHandler_NoCherryPickPerformedTitle,
						UIText.CherryPickHandler_NoCherryPickPerformedMessage);
		} catch (Exception e) {
			throw new ExecutionException(UIText.CherryPickOperation_InternalError, e);
		}
		if (newHead == null)
			throw new ExecutionException(UIText.CherryPickOperation_Failed);
		return null;
	}
}
