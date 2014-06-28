/*******************************************************************************
 * Copyright (C) 2014 Ericsson and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc Khouzam (Ericsson) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Compare the file contents of a commit with its parent in the
 * {@link CompareTreeView}.
 */
public class CompareWithPreviousInTreeHandler extends
		AbstractHistoryCompareCommandHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RevCommit commit = getSelectedCommit(event);

		if (commit.getParentCount() == 1) {
			compareInTree(commit, commit.getParent(0), event);
		}

		return null;
	}

	@Override
	public boolean isEnabled() {
		GitHistoryPage page = getPage();
		if (page == null)
			return false;
		if (getSelection(page).size() == 1) {
			RevCommit commit = (RevCommit) getSelection(page).getFirstElement();
			if (commit.getParentCount() == 1)
				return true;
		}
		return false;
	}
}
