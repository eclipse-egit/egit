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
import java.io.IOException;
import java.util.Iterator;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.egit.core.RevUtils;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;

/**
 * Compare the file content of a commit with the working tree
 */
public class CompareWithWorkingTreeHandler extends
		AbstractHistoryCommandHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(getPage());
		if (selection.size() == 1) {
			Iterator<?> it = selection.iterator();
			RevCommit commit = (RevCommit) it.next();
			Object input = getPage().getInputInternal().getSingleFile();
			RevCommit commonAncestor = null;
			try {
				Repository repo = getRepository(event);
				commonAncestor = RevUtils.getCommonAncestor(repo, repo.resolve(Constants.HEAD), commit);
			} catch (IOException e) {
				Activator.logError(NLS.bind(UIText.CompareWithWorkingTreeHandler_errorCommonAncestor,
						Constants.HEAD, commit.getName()), e);
			}
			if (input instanceof IFile) {
				IFile file = (IFile) input;
				final RepositoryMapping mapping = RepositoryMapping
						.getMapping(file.getProject());
				final String gitPath = mapping.getRepoRelativePath(file);
				ITypedElement right = CompareUtils.getFileRevisionTypedElement(
						gitPath, commit, mapping.getRepository());
				final ITypedElement ancestor = commonAncestor != null ? CompareUtils.getFileRevisionTypedElement(
						gitPath, commonAncestor, mapping.getRepository()) : null;

				final GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
						SaveableCompareEditorInput.createFileElement(file),
						right, ancestor, null);
				openInCompare(event, in);
			}
			if (input instanceof File) {
				File file = (File) input;
				// TODO can we create a ITypedElement from the local file?
				Repository repo = getRepository(event);
				RevCommit leftCommit;
				try {
					leftCommit = new RevWalk(repo).parseCommit(repo
							.resolve(Constants.HEAD));
				} catch (Exception e) {
					throw new ExecutionException(e.getMessage(), e);
				}
				final String gitPath = getRepoRelativePath(repo, file);
				ITypedElement left = CompareUtils.getFileRevisionTypedElement(
						gitPath, leftCommit, repo);
				ITypedElement right = CompareUtils.getFileRevisionTypedElement(
						gitPath, commit, repo);
				ITypedElement ancestor = commonAncestor != null ? CompareUtils.getFileRevisionTypedElement(
						gitPath, commonAncestor, repo) : null;
				final GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
						left, right, ancestor, null);
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
		return page.getInputInternal().isSingleFile();
	}
}
