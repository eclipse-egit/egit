/******************************************************************************
 *  Copyright (c) 2010, 2018 SAP AG, GitHub Inc., and others
 *  and other copyright owners as documented in the project's IP log.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Moved all UI to CherryPickUI
 *****************************************************************************/
package org.eclipse.egit.ui.internal.commit.command;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.ui.internal.handler.SelectionHandler;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Handler to cherry-pick commits onto HEAD.
 */
public class CherryPickHandler extends SelectionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RevCommit> commits = getSelectedItems(RevCommit.class, event);
		if (commits.isEmpty()) {
			return null;
		}
		Repository repo = getSelectedItem(Repository.class, event);
		if (repo == null) {
			return null;
		}

		// Reverse the view selection order for a good initial order of the
		// commits to cherry-pick (parent-first order)
		Collections.reverse(commits);

		CherryPickUI ui = new CherryPickUI();
		try {
			ui.run(repo, commits, true);
		} catch (CoreException e) {
			throw new ExecutionException(e.getLocalizedMessage(), e);
		}
		return null;
	}
}
