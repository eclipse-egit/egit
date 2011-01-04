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
import org.eclipse.egit.core.CoreText;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RevertCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Executes the RevertCommit
 */
public class RevertHandler extends AbstractHistoryCommanndHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RevCommit commit = (RevCommit) getSelection(getPage()).getFirstElement();
		RevCommit newHead;
		Repository repo = getRepository(event);

		RevertCommand cherryPick;
		Git git = new Git(repo);
		try {
			cherryPick = git.revert().include(commit.getId());
			newHead = cherryPick.call();
		} catch (Exception e) {
			throw new ExecutionException(CoreText.RevertOperation_InternalError, e);
		}
		if (newHead == null)
			throw new ExecutionException(CoreText.RevertOperation_Failed);
		return null;
	}
}
