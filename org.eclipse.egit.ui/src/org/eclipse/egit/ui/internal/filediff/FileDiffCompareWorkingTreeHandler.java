/******************************************************************************
 *  Copyright (c) 2024 Thomas Wolf <twolf@apache.org> and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.egit.ui.internal.filediff;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.egit.ui.internal.merge.GitCompareEditorInput;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Compare one or several {@link FileDiff}s against the working tree file
 * versions.
 */
public class FileDiffCompareWorkingTreeHandler
		extends AbstractFileDiffHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		IWorkbenchPart part = HandlerUtil.getActivePartChecked(event);
		List<FileDiff> diffs = getDiffs(selection,
				Predicate.not(FileDiff::isSubmodule));
		if (!diffs.isEmpty()) {
			if (diffs.size() == 1) {
				showWorkingDirectoryFileDiff(part, diffs.get(0));
			} else {
				FileDiff first = diffs.get(0);
				Repository repository = first.getRepository();
				IPath workingTree = Path.fromOSString(
						repository.getWorkTree().getAbsolutePath());
				List<IPath> paths = diffs.stream().map(FileDiff::getPath)
						.map(workingTree::append).collect(Collectors.toList());
				GitCompareEditorInput comparison = new GitCompareEditorInput(
						null, first.getCommit().name(), repository,
						paths.toArray(new IPath[0]));
				CompareUtils.openInCompare(part.getSite().getPage(), repository,
						comparison);
			}
		}
		return null;
	}

	private void showWorkingDirectoryFileDiff(IWorkbenchPart part, FileDiff d) {
		String p = d.getPath();
		RevCommit commit = d.getCommit();
		Repository repo = d.getRepository();

		if (commit == null || repo == null || p == null) {
			Activator.showError(UIText.GitHistoryPage_openFailed, null);
			return;
		}

		IWorkbenchPage activePage = part.getSite().getPage();
		IFile file = ResourceUtil.getFileForLocation(repo, p, false);
		try {
			if (file != null) {
				IResource[] resources = new IResource[] { file };
				CompareUtils.compare(resources, repo, Constants.HEAD,
						commit.getName(), true, activePage);
			} else {
				IPath path = new Path(repo.getWorkTree().getAbsolutePath())
						.append(p);
				File ioFile = path.toFile();
				if (ioFile.exists()) {
					CompareUtils.compare(path, repo, Constants.HEAD,
							commit.getName(), true, activePage);
				}
			}
		} catch (IOException e) {
			Activator.handleError(UIText.GitHistoryPage_openFailed, e, true);
		}
	}

}
