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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.IRepositoryCommit;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.history.HistoryPageInput;
import org.eclipse.egit.ui.internal.merge.GitCompareEditorInput;
import org.eclipse.egit.ui.internal.revision.GitCompareFileRevisionEditorInput;
import org.eclipse.egit.ui.internal.synchronize.compare.LocalNonWorkspaceTypedElement;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
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
		HistoryPageInput historyInput = getPage(event).getInputInternal();
		Object input = historyInput.getSingleFile();
		IWorkbenchPage workbenchPage = HandlerUtil
				.getActiveWorkbenchWindowChecked(event).getActivePage();
		if (input instanceof IFile) {
			IFile file = (IFile) input;
			final RepositoryMapping mapping = RepositoryMapping
					.getMapping(file);
			if (mapping != null && !mapping.getRepository().isBare()) {
				final String gitPath = mapping.getRepoRelativePath(file);
				final String commitPath = getRenamedPath(gitPath, commit);
				ITypedElement right = CompareUtils.getFileRevisionTypedElement(
						commitPath, commit, mapping.getRepository());
				CompareEditorInput in = new GitCompareFileRevisionEditorInput(
						SaveableCompareEditorInput.createFileElement(file),
						right, null);
				CompareUtils.openInCompare(workbenchPage, in);
			}
		} else if (input instanceof File) {
			File file = (File) input;
			Repository repo = getRepository(event);
			if (repo != null && !repo.isBare()) {
				final String leftCommitPath = getRepoRelativePath(repo, file);
				final String rightCommitPath = getRenamedPath(leftCommitPath,
						commit);
				ITypedElement right = CompareUtils.getFileRevisionTypedElement(
						rightCommitPath, commit, repo);
				CompareEditorInput in = new GitCompareFileRevisionEditorInput(
						new LocalNonWorkspaceTypedElement(repo,
								new Path(file.getAbsolutePath())),
						right, null);
				CompareUtils.openInCompare(workbenchPage, in);
			}
		} else {
			Repository repo = getRepository(event);
			if (repo != null && !repo.isBare()) {
				Collection<IPath> paths = new HashSet<>();
				IResource[] resources = historyInput.getItems();
				if (resources != null) {
					Arrays.stream(resources).map(IResource::getLocation)
							.filter(Objects::nonNull).forEach(paths::add);
				}
				File[] files = historyInput.getFileList();
				if (files != null) {
					Arrays.stream(files).map(File::getAbsolutePath)
							.map(Path::fromOSString).forEach(paths::add);
				}
				GitCompareEditorInput comparison = new GitCompareEditorInput(
						null, commit.name(), repo, paths.toArray(new IPath[0]));
				CompareUtils.openInCompare(workbenchPage, comparison);
			}
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		GitHistoryPage page = getPage();
		if (page == null) {
			return false;
		}
		IStructuredSelection selection = getSelection(page);
		if (selection.size() != 1) {
			return false;
		}
		IRepositoryCommit commit = Adapters.adapt(selection.getFirstElement(),
				IRepositoryCommit.class);
		if (commit != null) {
			return !commit.getRepository().isBare();
		}
		return false;
	}
}
