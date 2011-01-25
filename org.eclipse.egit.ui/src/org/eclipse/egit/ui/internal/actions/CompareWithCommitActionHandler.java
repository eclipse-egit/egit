/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.FileRevisionTypedElement;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.egit.ui.internal.LocalFileRevision;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.egit.ui.internal.history.CommitSelectionDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * The "compare with commit" action. This action opens a diff editor comparing
 * the file as found in the working directory and the version in the selected
 * ref.
 */
public class CompareWithCommitActionHandler extends RepositoryActionHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IResource resource = getSelectedResources(event)[0];
		final RepositoryMapping mapping = RepositoryMapping.getMapping(resource
				.getProject());
		if (mapping == null || mapping.getRepository() == null)
			return null;
		final Repository repo = mapping.getRepository();

		CommitSelectionDialog dlg = new CommitSelectionDialog(getShell(event),
				repo);
		if (dlg.open() == Window.OK) {

			if (resource instanceof IFile) {
				final IFile baseFile = (IFile) resource;

				final ITypedElement base = new FileRevisionTypedElement(
						new LocalFileRevision(baseFile));

				final ITypedElement next;
				try {
					next = getElementForCommit(mapping.getRepository(), mapping
							.getRepoRelativePath(baseFile), dlg.getCommitId());
				} catch (IOException e) {
					Activator.handleError(
							UIText.CompareWithIndexAction_errorOnAddToIndex, e,
							true);
					return null;
				}

				final GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
						base, next, null);
				in.getCompareConfiguration().setRightLabel(
						dlg.getCommitId().name());
				CompareUI.openCompareEditor(in);
			}

			if (resource instanceof IContainer) {
				CompareTreeView view;
				try {
					view = (CompareTreeView) PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage()
							.showView(CompareTreeView.ID);
					view.setInput(resource, dlg.getCommitId().name());
				} catch (PartInitException e) {
					Activator.handleError(e.getMessage(), e, true);
				}
			}
		}
		return null;
	}

	private ITypedElement getElementForCommit(final Repository repository,
			final String gitPath, final ObjectId commitId) throws IOException {
		RevWalk rw = new RevWalk(repository);
		RevCommit commit = rw.parseCommit(commitId);
		rw.release();

		IFileRevision nextFile = GitFileRevision.inCommit(repository, commit,
				gitPath, null);

		FileRevisionTypedElement element = new FileRevisionTypedElement(
				nextFile);
		return element;
	}

	@Override
	public boolean isEnabled() {
		final IResource[] selectedResources = getSelectedResources();
		if (selectedResources.length != 1)
			return false;

		final IResource resource = selectedResources[0];
		final RepositoryMapping mapping = RepositoryMapping.getMapping(resource
				.getProject());
		return mapping != null;
	}

}
