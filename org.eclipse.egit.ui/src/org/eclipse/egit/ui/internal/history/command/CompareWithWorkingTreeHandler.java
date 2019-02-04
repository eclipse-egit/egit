/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2012, Gunnar Wagenknecht <gunnar@wagenknecht.org>
 * Copyright (C) 2013, Laurent Goubet <laurent.goubet@obeo.fr>
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

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.revision.GitCompareFileRevisionEditorInput;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Compare the file content of a commit with the working tree
 */
public class CompareWithWorkingTreeHandler extends
		AbstractHistoryCommandHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		if (selection.isEmpty())
			return null;

		// Even if there's more than one element, only consider the first
		RevCommit commit = (RevCommit) selection.getFirstElement();
		Object input = getPage(event).getInputInternal().getSingleFile();
		IWorkbenchPage workbenchPage = HandlerUtil
				.getActiveWorkbenchWindowChecked(event).getActivePage();
		if (input instanceof IFile) {
			IFile file = (IFile) input;
			final RepositoryMapping mapping = RepositoryMapping
					.getMapping(file);
			if (mapping != null) {
				final String gitPath = mapping.getRepoRelativePath(file);
				final String commitPath = getRenamedPath(gitPath, commit);
				ITypedElement right = CompareUtils.getFileRevisionTypedElement(
						commitPath, commit, mapping.getRepository());
				final GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
						SaveableCompareEditorInput.createFileElement(file),
						right, null);
				CompareUtils.openInCompare(workbenchPage, in);
			}
		} else if (input instanceof File) {
			File file = (File) input;
			// TODO can we create a ITypedElement from the local file?
			Repository repo = getRepository(event);
			RevCommit leftCommit;
			try (RevWalk rw = new RevWalk(repo)) {
				leftCommit = rw.parseCommit(repo
						.resolve(Constants.HEAD));
			} catch (Exception e) {
				throw new ExecutionException(e.getMessage(), e);
			}
			final String leftCommitPath = getRepoRelativePath(repo, file);
			final String rightCommitPath = getRenamedPath(leftCommitPath,
					commit);
			CompareUtils.openInCompare(leftCommit, commit, leftCommitPath,
					rightCommitPath, repo, workbenchPage);
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		GitHistoryPage page = getPage();
		if (page == null)
			return false;
		int size = getSelection(page).size();
		if (size != 1)
			return false;
		return page.getInputInternal().isSingleFile();
	}
}
