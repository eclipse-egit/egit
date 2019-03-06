/*******************************************************************************
 * Copyright (C) 2011, 2013 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.io.File;
import java.util.Iterator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.history.HistoryPageInput;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Compare the file contents of two commits in the {@link CompareTreeView}.
 */
public class CompareVersionsInTreeHandler extends
		AbstractHistoryCommandHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		if (selection.size() == 2) {
			Iterator<?> it = selection.iterator();
			RevCommit commit1 = (RevCommit) it.next();
			RevCommit commit2 = (RevCommit) it.next();

			HistoryPageInput pageInput = getPage(event).getInputInternal();
			Object input = pageInput.getSingleItem();
			Repository repository = pageInput.getRepository();
			IWorkbenchPage workbenchPage = HandlerUtil
					.getActiveWorkbenchWindowChecked(event).getActivePage();
			// IFile and File just for compatibility; the action should not be
			// available in this case in the UI
			if (input instanceof IFile) {
				IFile resource = (IFile) input;
				final RepositoryMapping map = RepositoryMapping
						.getMapping(resource);
				if (map != null) {
					final String gitPath = map.getRepoRelativePath(resource);
					final String commit1Path = getRenamedPath(gitPath, commit1);
					final String commit2Path = getRenamedPath(gitPath, commit2);

					CompareUtils.openInCompare(commit1, commit2, commit1Path,
							commit2Path, map.getRepository(), workbenchPage);
				}
			} else if (input instanceof File) {
				File fileInput = (File) input;
				Repository repo = getRepository(event);
				final String gitPath = getRepoRelativePath(repo, fileInput);
				final String commit1Path = getRenamedPath(gitPath, commit1);
				final String commit2Path = getRenamedPath(gitPath, commit2);

				CompareUtils.openInCompare(commit1, commit2, commit1Path,
						commit2Path, repo, workbenchPage);
			} else if (input instanceof IResource) {
				CompareTreeView view;
				try {
					view = (CompareTreeView) PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage()
							.showView(CompareTreeView.ID);
					view.setInput(new IResource[] { (IResource) input },
							commit1.getId().name(), commit2.getId().name());
				} catch (PartInitException e) {
					Activator.handleError(e.getMessage(), e, true);
				}
			} else if (input == null) {
				CompareTreeView view;
				try {
					view = (CompareTreeView) PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage()
							.showView(CompareTreeView.ID);
					view.setInput(repository, commit1.getId().name(), commit2
							.getId().name());
				} catch (PartInitException e) {
					Activator.handleError(e.getMessage(), e, true);
				}
			}
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		GitHistoryPage page = getPage();
		if (page == null)
			return false;
		return getSelection(page).size() == 2;
	}
}
