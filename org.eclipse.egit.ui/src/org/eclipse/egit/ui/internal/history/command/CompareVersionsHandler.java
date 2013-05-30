/*******************************************************************************
 * Copyright (C) 2010, 2013 Mathias Kinzler <mathias.kinzler@sap.com> and others.
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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
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
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(getPage());
		if (selection.size() == 2) {
			Iterator<?> it = selection.iterator();
			RevCommit commit1 = (RevCommit) it.next();
			RevCommit commit2 = (RevCommit) it.next();


			Object input = getPage().getInputInternal().getSingleItem();
			Repository repo = getRepository(event);
			IWorkbenchPage workBenchPage = HandlerUtil
					.getActiveWorkbenchWindowChecked(event).getActivePage();
			if (input instanceof IFile) {
				IResource[] resources = new IResource[] { (IFile) input, };
				try {
					CompareUtils.compare(resources, repo, commit1.getName(),
							commit2.getName(), false, workBenchPage);
				} catch (IOException e) {
					Activator.handleError(
							UIText.CompareWithRefAction_errorOnSynchronize, e,
							true);
				}
			} else if (input instanceof File) {
				File fileInput = (File) input;
				IPath location = new Path(fileInput.getAbsolutePath());
				try {
					CompareUtils.compare(location, repo, commit1.getName(),
							commit2.getName(), false, workBenchPage);
				} catch (IOException e) {
					Activator.handleError(
							UIText.CompareWithRefAction_errorOnSynchronize, e,
							true);
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
