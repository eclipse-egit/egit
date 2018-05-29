/*******************************************************************************
 * Copyright (C) 2010, 2013 Mathias Kinzler <mathias.kinzler@sap.com> and others.
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
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.merge.GitCompareEditorInput;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Compare the file contents of two commits.
 */
public class CompareVersionsHandler extends AbstractHistoryCommandHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		if (selection.size() == 2) {
			Iterator<?> it = selection.iterator();
			RevCommit commit1 = (RevCommit) it.next();
			RevCommit commit2 = (RevCommit) it.next();


			Object input = getPage(event).getInputInternal().getSingleItem();
			Repository repo = getRepository(event);
			IWorkbenchPage workBenchPage = HandlerUtil
					.getActiveWorkbenchWindowChecked(event).getActivePage();
			if (input instanceof IFile) {
				IFile resource = (IFile) input;
				final RepositoryMapping map = RepositoryMapping
						.getMapping(resource);
				if (map != null) {
					final String gitPath = map.getRepoRelativePath(resource);
					final String commit1Path = getRenamedPath(gitPath, commit1);
					final String commit2Path = getRenamedPath(gitPath, commit2);
					CompareUtils.openInCompare(commit1, commit2, commit1Path,
							commit2Path, map.getRepository(), workBenchPage);
				}
			} else if (input instanceof File) {
				File fileInput = (File) input;
				final String gitPath = getRepoRelativePath(repo, fileInput);
				final String commit1Path = getRenamedPath(gitPath, commit1);
				final String commit2Path = getRenamedPath(gitPath, commit2);
				CompareUtils.openInCompare(commit1, commit2, commit1Path,
						commit2Path, repo, workBenchPage);
			} else if (input instanceof IResource) {
				GitCompareEditorInput compareInput = new GitCompareEditorInput(
						commit1.name(), commit2.name(), repo,
						(IResource) input);
				CompareUtils.openInCompare(workBenchPage, compareInput);
			} else if (input == null) {
				GitCompareEditorInput compareInput = new GitCompareEditorInput(
						commit1.name(), commit2.name(), repo);
				CompareUtils.openInCompare(workBenchPage, compareInput);
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
