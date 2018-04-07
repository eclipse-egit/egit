/******************************************************************************
 *  Copyright (c) 2010, 2018 SAP AG, GitHub Inc., and others
 *  and other copyright owners as documented in the project's IP log.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Moved all UI to CherryPickUI
 *****************************************************************************/
package org.eclipse.egit.ui.internal.commit.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.ui.internal.handler.SelectionHandler;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Handler to cherry-pick the commit onto HEAD.
 */
public class CherryPickHandler extends SelectionHandler {

	/**
	 * Command id
	 */
	public static final String ID = "org.eclipse.egit.ui.commit.CherryPick"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RevCommit commit = getSelectedItem(RevCommit.class, event);
		if (commit == null) {
			return null;
		}
		Repository repo = getSelectedItem(Repository.class, event);
		if (repo == null) {
			return null;
		}
		CherryPickUI ui = new CherryPickUI();
		try {
			ui.run(repo, commit, true);
		} catch (CoreException e) {
			throw new ExecutionException(e.getLocalizedMessage(), e);
		}
		return null;
	}
}
