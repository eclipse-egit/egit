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
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.history.GitCreatePatchWizard;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Create a patch based on a commit.
 */
public class CreatePatchHandler extends AbstractHistoryCommanndHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		if (selection.size() == 1) {
			RevCommit commit = (RevCommit) selection.getFirstElement();
			Object input = getInput(event);
			if (!(input instanceof IResource))
				return null;
			RepositoryMapping mapping = RepositoryMapping
					.getMapping((IResource) getInput(event));

			GitCreatePatchWizard.run(getPart(event), commit, new TreeWalk(
					mapping.getRepository()), mapping.getRepository());
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		try {
			IStructuredSelection selection = getSelection(null);
			if (selection.size() != 1)
				return false;
			RevCommit commit = (RevCommit) selection.getFirstElement();
			return (commit.getParentCount() == 1);
		} catch (ExecutionException e) {
			Activator.handleError(e.getMessage(), e, false);
			return false;
		}
	}
}
