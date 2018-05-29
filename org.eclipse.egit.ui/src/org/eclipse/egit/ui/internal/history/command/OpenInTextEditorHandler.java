/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.EgitUiEditorUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.history.IFileRevision;

/**
 * Open a file or files in a text editor
 */
public class OpenInTextEditorHandler extends AbstractHistoryCommandHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		if (selection.size() < 1)
			return null;
		Object input = getPage(event).getInputInternal().getSingleFile();
		if (input == null)
			return null;
		boolean errorOccurred = false;
		List<ObjectId> ids = new ArrayList<>();
		String gitPath = null;
		if (input instanceof IFile) {
			IFile resource = (IFile) input;
			final RepositoryMapping map = RepositoryMapping
					.getMapping(resource);
			if (map != null) {
				gitPath = map.getRepoRelativePath(resource);
				Iterator<?> it = selection.iterator();
				while (it.hasNext()) {
					RevCommit commit = (RevCommit) it.next();
					String commitPath = getRenamedPath(gitPath, commit);
					IFileRevision rev = null;
					try {
						rev = CompareUtils.getFileRevision(commitPath, commit,
								map.getRepository(), null);
					} catch (IOException e) {
						Activator.logError(NLS.bind(
								UIText.GitHistoryPage_errorLookingUpPath,
								commitPath, commit.getId()), e);
						errorOccurred = true;
					}
					if (rev != null) {
						try {
							EgitUiEditorUtils.openTextEditor(
									getPart(event).getSite().getPage(), rev,
									null);
						} catch (CoreException e) {
							Activator.logError(e.getMessage(), e);
							errorOccurred = true;
						}
					} else {
						ids.add(commit.getId());
					}
				}
			}
		}
		if (input instanceof File) {
			Repository repo = getRepository(event);
			File fileInput = (File) input;
			gitPath = getRepoRelativePath(repo, fileInput);
			Iterator<?> it = selection.iterator();
			while (it.hasNext()) {
				RevCommit commit = (RevCommit) it.next();
				String commitPath = getRenamedPath(gitPath, commit);
				IFileRevision rev = null;
				try {
					rev = CompareUtils.getFileRevision(commitPath, commit,
							repo, null);
				} catch (IOException e) {
					Activator.logError(NLS.bind(
							UIText.GitHistoryPage_errorLookingUpPath,
							commitPath, commit.getId()), e);
					errorOccurred = true;
				}
				if (rev != null)
					try {
						EgitUiEditorUtils.openTextEditor(getPart(event)
								.getSite().getPage(), rev, null);
					} catch (CoreException e) {
						Activator.logError(e.getMessage(), e);
						errorOccurred = true;
					}
				else
					ids.add(commit.getId());
			}
		}
		if (errorOccurred)
			Activator.showError(UIText.GitHistoryPage_openFailed, null);
		if (ids.size() > 0) {
			StringBuilder idList = new StringBuilder(""); //$NON-NLS-1$
			for (ObjectId objectId : ids)
				idList.append(objectId.getName()).append(' ');
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
