/*******************************************************************************
 * Copyright (C) 2014 Ericsson and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc Khouzam (Ericsson) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.io.File;
import java.io.IOException;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.egit.ui.internal.history.HistoryPageInput;
import org.eclipse.egit.ui.internal.merge.GitCompareEditorInput;
import org.eclipse.egit.ui.internal.revision.GitCompareFileRevisionEditorInput;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Abstract class for commands that compare two commits from the history view.
 */
abstract class AbstractHistoryCompareCommandHandler extends
		AbstractHistoryCommandHandler {
	/**
	 * Compare two commits in a compare editor.
	 *
	 * @param commit1
	 *            The first commit to compare, which will be shown on the left
	 *            column of the editor.
	 * @param commit2
	 *            The second commit to compare, which will be shown on the right
	 *            column of the editor.
	 * @param event
	 *
	 * @throws ExecutionException
	 *
	 */
	protected void compare(RevCommit commit1, RevCommit commit2,
			ExecutionEvent event) throws ExecutionException {
		Object input = getPage(event).getInputInternal().getSingleItem();
		Repository repo = getRepository(event);
		IWorkbenchPage workBenchPage = HandlerUtil
				.getActiveWorkbenchWindowChecked(event).getActivePage();

		if (input instanceof IFile) {
			IResource[] resources = new IResource[] { (IFile) input, };
			try {
				CompareUtils.compare(resources, repo, commit1.getName(),
						commit2.getName(), false, workBenchPage);
			} catch (IOException e) {
				Activator
						.handleError(
								UIText.CompareWithRefAction_errorOnSynchronize,
								e, true);
			}
		} else if (input instanceof File) {
			File fileInput = (File) input;
			IPath location = new Path(fileInput.getAbsolutePath());
			try {
				CompareUtils.compare(location, repo, commit1.getName(),
						commit2.getName(), false, workBenchPage);
			} catch (IOException e) {
				Activator
						.handleError(
								UIText.CompareWithRefAction_errorOnSynchronize,
								e, true);
			}
		} else if (input instanceof IResource) {
			GitCompareEditorInput compareInput = new GitCompareEditorInput(
					commit1.name(), commit2.name(), (IResource) input);
			CompareUtils.openInCompare(workBenchPage, compareInput);
		} else if (input == null) {
			GitCompareEditorInput compareInput = new GitCompareEditorInput(
					commit1.name(), commit2.name(), repo);
			CompareUtils.openInCompare(workBenchPage, compareInput);
		}
	}

	/**
	 * Compare two commits in a compare tree view.
	 *
	 * @param commit1
	 *            The first commit to compare, which is the resulting of the
	 *            change.
	 * @param commit2
	 *            The second commit to compare, which is the base for the
	 *            change.
	 * @param event
	 *
	 * @throws ExecutionException
	 */
	protected void compareInTree(RevCommit commit1, RevCommit commit2,
			ExecutionEvent event) throws ExecutionException {

		HistoryPageInput pageInput = getPage(event).getInputInternal();
		Object input = pageInput.getSingleItem();
		Repository repo = pageInput.getRepository();
		IWorkbenchPage workBenchPage = HandlerUtil
				.getActiveWorkbenchWindowChecked(event).getActivePage();

		// IFile and File just for compatibility; the action should not be
		// available in this case in the UI
		if (input instanceof IFile) {
			IFile resource = (IFile) input;
			final RepositoryMapping map = RepositoryMapping
					.getMapping(resource);
			final String gitPath = map.getRepoRelativePath(resource);

			final ITypedElement base = CompareUtils
					.getFileRevisionTypedElement(gitPath, commit1,
							map.getRepository());
			final ITypedElement next = CompareUtils
					.getFileRevisionTypedElement(gitPath, commit2,
							map.getRepository());
			CompareEditorInput in = new GitCompareFileRevisionEditorInput(base,
					next, null);
			CompareUtils.openInCompare(workBenchPage, in);
		} else if (input instanceof File) {
			File fileInput = (File) input;
			final String gitPath = getRepoRelativePath(repo, fileInput);

			final ITypedElement base = CompareUtils
					.getFileRevisionTypedElement(gitPath, commit1, repo);
			final ITypedElement next = CompareUtils
					.getFileRevisionTypedElement(gitPath, commit2, repo);
			CompareEditorInput in = new GitCompareFileRevisionEditorInput(base,
					next, null);
			CompareUtils.openInCompare(workBenchPage, in);
		} else if (input instanceof IResource) {
			CompareTreeView view;
			try {
				view = (CompareTreeView) PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage()
						.showView(CompareTreeView.ID);
				view.setInput(new IResource[] { (IResource) input }, commit1
						.getId().name(), commit2.getId().name());
			} catch (PartInitException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
		} else if (input == null) {
			CompareTreeView view;
			try {

				view = (CompareTreeView) PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage()
						.showView(CompareTreeView.ID);
				view.setInput(repo, commit1.getId().name(), commit2.getId()
						.name());
			} catch (PartInitException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
		}
	}
}
