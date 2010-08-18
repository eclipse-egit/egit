/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.history.GitCreatePatchWizard;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 * Create a patch based on a commit.
 */
public class CreatePatchHandler extends AbstractHistoryCommanndHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(getPage());
		if (selection.size() == 1) {
			RevCommit commit = (RevCommit) selection.getFirstElement();
			Object input = getInput(event);
			if (!(input instanceof IResource))
				return null;
			RepositoryMapping mapping = RepositoryMapping
					.getMapping((IResource) getInput(event));

			TreeWalk fileWalker = new TreeWalk(mapping.getRepository());
			fileWalker.setRecursive(true);
			fileWalker.setFilter(TreeFilter.ANY_DIFF);
			GitCreatePatchWizard.run(getPart(event), commit, fileWalker, mapping.getRepository());
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		GitHistoryPage page = getPage();
		if (page == null)
			return false;
		IStructuredSelection selection = getSelection(page);
		if (selection.size() != 1)
			return false;
		RevCommit commit = (RevCommit) selection.getFirstElement();
		return (commit.getParentCount() == 1);
	}
}
