/*******************************************************************************
 * Copyright (C) 2010, 2013 Mathias Kinzler <mathias.kinzler@sap.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.EgitUiEditorUtils;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Show versions/open.
 * <p>
 * If a single version is selected, open it, otherwise open several versions of
 * the file content.
 */
public class ShowVersionsHandler extends AbstractHistoryCommandHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(getPage());
		Object input = getPage().getInputInternal().getSingleFile();
		if (selection.size() < 1 || input == null)
			return null;

		boolean compareMode = Boolean.parseBoolean(event
				.getParameter(HistoryViewCommands.COMPARE_MODE_PARAM));
		Repository repository = getRepository(event);

		String gitPath = null;
		if (input instanceof IFile) {
			IFile resource = (IFile) input;
			RepositoryMapping map = RepositoryMapping
					.getMapping(resource);
			gitPath = map.getRepoRelativePath(resource);
		} else if (input instanceof File) {
			File fileInput = (File) input;
			gitPath = getRepoRelativePath(repository, fileInput);
		} else {
			// Should be unreachable
			return null;
		}

		boolean errorOccurred = false;
		List<ObjectId> ids = new ArrayList<ObjectId>();

		Iterator<?> it = selection.iterator();
		while (it.hasNext()) {
			RevCommit commit = (RevCommit) it.next();
			IFileRevision revision = null;
			try {
				revision = CompareUtils.getFileRevision(gitPath, commit,
						repository, null);
			} catch (IOException e) {
				Activator.logError(NLS.bind(
						UIText.GitHistoryPage_errorLookingUpPath, gitPath,
						commit.getId()), e);
				errorOccurred = true;
			}

			if (revision == null)
				ids.add(commit.getId());
			else if (compareMode) {
				try {
					IWorkbenchPage workBenchPage = HandlerUtil
							.getActiveWorkbenchWindowChecked(event)
							.getActivePage();
					if (input instanceof IFile)
						CompareUtils.compareWorkspaceWithRef(repository,
								(IFile) input, commit.getId().getName(),
								workBenchPage);
					else
						CompareUtils.compareLocalWithRef(repository,
								(File) input, commit.getId().getName(),
								workBenchPage);
				} catch (IOException e) {
					errorOccurred = true;
				}
			} else {
				try {
					EgitUiEditorUtils.openEditor(getPart(event).getSite()
							.getPage(), revision, new NullProgressMonitor());
				} catch (CoreException e) {
					Activator.logError(UIText.GitHistoryPage_openFailed, e);
					errorOccurred = true;
				}
			}
		}

		if (errorOccurred)
			Activator.showError(UIText.GitHistoryPage_openFailed, null);
		if (ids.size() > 0) {
			StringBuilder idList = new StringBuilder(""); //$NON-NLS-1$
			for (ObjectId objectId : ids) {
				idList.append(objectId.getName()).append(' ');
			}
			MessageDialog.openError(getPart(event).getSite().getShell(),
					UIText.GitHistoryPage_fileNotFound, NLS.bind(
							UIText.GitHistoryPage_notContainedInCommits,
							gitPath, idList.toString()));
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		GitHistoryPage page = getPage();
		if (page == null)
			return false;
		int size = getSelection(page).size();
		if (size == 0)
			return false;
		return page.getInputInternal().isSingleFile();
	}
}
