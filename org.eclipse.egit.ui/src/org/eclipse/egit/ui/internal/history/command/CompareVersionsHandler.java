/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.util.Iterator;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/**
 * Compare the file contents of two commits.
 */
public class CompareVersionsHandler extends AbstractHistoryCommanndHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		if (selection.size() == 2) {
			Iterator<?> it = selection.iterator();
			RevCommit commit1 = (RevCommit) it.next();
			RevCommit commit2 = (RevCommit) it.next();

			Object input = null;
			IWorkbenchPart part = getPart(event);
			if (part instanceof IHistoryView) {
				input = ((IHistoryView) part).getHistoryPage().getInput();
			}

			if (input != null && input instanceof IFile) {
				IFile resource = (IFile) input;
				final RepositoryMapping map = RepositoryMapping
						.getMapping(resource);
				final String gitPath = map.getRepoRelativePath(resource);

				final ITypedElement base = CompareUtils
						.getFileRevisionTypedElement(gitPath, commit1, map
								.getRepository());
				final ITypedElement next = CompareUtils
						.getFileRevisionTypedElement(gitPath, commit2, map
								.getRepository());
				CompareEditorInput in = new GitCompareFileRevisionEditorInput(
						base, next, null);
				openInCompare(event, in);
			}
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		try {
			IWorkbenchPart part = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage().getActivePart();
			if (!(part instanceof IHistoryView))
				return false;
			IHistoryView view = (IHistoryView) part;
			if (!(view.getHistoryPage().getInput() instanceof IFile))
				return false;
			IStructuredSelection sel = getSelection(null);
			Object[] selected = sel.toArray();
			return selected.length == 2 && selected[0] instanceof RevCommit
					&& selected[1] instanceof RevCommit;
		} catch (ExecutionException e) {
			Activator.handleError(e.getMessage(), e, false);
			return false;
		}
	}
}
