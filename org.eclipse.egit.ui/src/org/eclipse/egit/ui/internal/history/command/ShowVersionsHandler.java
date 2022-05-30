/*******************************************************************************
 * Copyright (C) 2010, 2019, 2020 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Andre Bossert <andre.bossert@siemens.com> - external merge and diff tools
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.EgitUiEditorUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.revision.GitCompareFileRevisionEditorInput;
import org.eclipse.egit.ui.internal.synchronize.compare.LocalNonWorkspaceTypedElement;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Show versions/open.
 * <p>
 * If a single version is selected, open it, otherwise open several versions of
 * the file content.
 */
public class ShowVersionsHandler extends AbstractHistoryCommandHandler {

	/**
	 * "Compare mode" parameter for the command.
	 */
	public static final String COMPARE_MODE_PARAM = "org.eclipse.egit.ui.history.CompareMode"; //$NON-NLS-1$

	/**
	 * Id of the command.
	 */
	public static final String COMMAND_ID = "org.eclipse.egit.ui.history.ShowVersions"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		boolean compareMode = Boolean.TRUE.toString()
				.equals(event.getParameter(COMPARE_MODE_PARAM));
		IStructuredSelection selection = getSelection(getPage());
		if (selection.size() < 1)
			return null;
		Object input = getPage().getInputInternal().getSingleFile();
		if (input == null)
			return null;
		IWorkbenchPage workbenchPage = HandlerUtil
				.getActiveWorkbenchWindowChecked(event).getActivePage();
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
								gitPath, commit.getId()), e);
						errorOccurred = true;
					}
					if (rev != null) {
						if (compareMode) {
							ITypedElement right = CompareUtils
									.getFileRevisionTypedElement(commitPath,
											commit, map.getRepository());
							final GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
									SaveableCompareEditorInput
											.createFileElement(resource),
									right, null);
							try {
								CompareUtils.openInCompare(workbenchPage,
										map.getRepository(), in);
							} catch (Exception e) {
								errorOccurred = true;
							}
						} else {
							try {
								EgitUiEditorUtils.openEditor(
										getPart(event).getSite().getPage(), rev,
										new NullProgressMonitor());
							} catch (CoreException e) {
								Activator.logError(
										UIText.GitHistoryPage_openFailed, e);
								errorOccurred = true;
							}
						}
					} else {
						ids.add(commit.getId());
					}
				}
			}
		}
		if (input instanceof File) {
			File fileInput = (File) input;
			Repository repo = getRepository(event);
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
				if (rev != null) {
					if (compareMode) {
						ITypedElement right = CompareUtils
								.getFileRevisionTypedElement(commitPath, commit,
										repo);
						GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
								new LocalNonWorkspaceTypedElement(repo,
										new Path(fileInput.getAbsolutePath())),
								right, null);
						CompareUtils.openInCompare(workbenchPage, repo, in);
					} else {
						try {
							EgitUiEditorUtils.openEditor(getPart(event)
									.getSite().getPage(), rev,
									new NullProgressMonitor());
						} catch (CoreException e) {
							Activator.logError(
									UIText.GitHistoryPage_openFailed, e);
							errorOccurred = true;
						}
					}
				} else {
					ids.add(commit.getId());
				}
			}
		}
		if (errorOccurred) {
			Activator.showError(UIText.GitHistoryPage_openFailed, null);
		}
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
