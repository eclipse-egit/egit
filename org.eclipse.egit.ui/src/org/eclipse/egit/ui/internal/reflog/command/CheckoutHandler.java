/*******************************************************************************
 * Copyright (C) 2011, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.reflog.command;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.ReflogEntry;

/**
 * Checkout handler
 */
public class CheckoutHandler extends AbstractReflogCommandHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ReflogEntry entry = (ReflogEntry) getSelection(getView())
				.getFirstElement();
		Repository repo = getRepository(event);
		RevCommit commit = null;
		try {
			RevWalk w = new RevWalk(repo);
			commit = w.parseCommit(entry.getNewId());
		} catch (MissingObjectException e) {
			throw new ExecutionException(e.getMessage(), e);
		} catch (IncorrectObjectTypeException e) {
			throw new ExecutionException(e.getMessage(), e);
		} catch (IOException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
		if (commit != null) {
			final BranchOperationUI op = BranchOperationUI.checkout(repo,
					commit.name());
			if (op != null)
				op.start();
		}
		return null;
	}
}
