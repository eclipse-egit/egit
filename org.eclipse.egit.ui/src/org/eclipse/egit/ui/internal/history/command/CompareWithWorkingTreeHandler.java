/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.io.File;
import java.util.Iterator;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;

/**
 * Compare the file content of a commit with the working tree
 */
public class CompareWithWorkingTreeHandler extends
		AbstractHistoryCommanndHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(getPage());
		if (selection.size() == 1) {
			Iterator<?> it = selection.iterator();
			RevCommit commit = (RevCommit) it.next();
			IFile input = getFileInput(event);
			if (input != null) {
				final RepositoryMapping mapping = RepositoryMapping
						.getMapping(input.getProject());
				final String gitPath = mapping.getRepoRelativePath(input);
				ITypedElement right = CompareUtils.getFileRevisionTypedElement(
						gitPath, commit, mapping.getRepository());
				final GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
						SaveableCompareEditorInput.createFileElement(input),
						right, null);
				openInCompare(event, in);
				return null;
			}
			File localInput = getLocalFileInput(event);
			if (localInput != null) {
				// TODO can we create a ITypedElement from the local file?
				Repository repo = getRepository(event);
				RevCommit leftCommit;
				try {
					leftCommit = new RevWalk(repo).parseCommit(repo
							.resolve(Constants.HEAD));
				} catch (Exception e) {
					throw new ExecutionException(e.getMessage(), e);
				}
				final String gitPath = getRepoRelativePath(repo, localInput);
				ITypedElement left = CompareUtils.getFileRevisionTypedElement(
						gitPath, leftCommit, repo);
				ITypedElement right = CompareUtils.getFileRevisionTypedElement(
						gitPath, commit, repo);
				final GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
						left, right, null);
				openInCompare(event, in);
				return null;
			}
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
		Object pageInput = page.getInput();
		return pageInput instanceof IFile || pageInput instanceof FileNode;
	}
}
