/*******************************************************************************
 * Copyright (C) 2011, 2012 Mathias Kinzler <mathias.kinzler@sap.com> and others.
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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.egit.ui.internal.history.CommitSelectionDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * The "compare with commit" action. This action opens a diff editor comparing
 * the file as found in the working directory and the version in the selected
 * ref.
 */
public class CompareWithCommitActionHandler extends RepositoryActionHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repo = getRepository(true, event);
		if (repo == null)
			return null;
		IResource[] resources = getSelectedResources(event);

		CommitSelectionDialog dlg = new CommitSelectionDialog(getShell(event),
				repo, resources);
		if (dlg.open() != Window.OK)
			return null;

		if (resources.length == 1 && resources[0] instanceof IFile) {
			final IFile baseFile = (IFile) resources[0];

			final ITypedElement base = SaveableCompareEditorInput
					.createFileElement(baseFile);

			final ITypedElement next;
			final ITypedElement ancestor;
			try {
				RepositoryMapping mapping = RepositoryMapping
						.getMapping(resources[0]);
				next = getElementForCommit(mapping.getRepository(), mapping
						.getRepoRelativePath(baseFile), dlg.getCommitId());

				ancestor =  CompareUtils.getFileRevisionTypedElementForCommonAncestor(mapping
						.getRepoRelativePath(baseFile), repo.resolve(Constants.HEAD),
						dlg.getCommitId(), repo);
			} catch (IOException e) {
				Activator.handleError(
						UIText.CompareWithIndexAction_errorOnAddToIndex, e,
						true);
				return null;
			}

			final GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
					base, next, ancestor, null);
			in.getCompareConfiguration()
					.setRightLabel(dlg.getCommitId().name());
			CompareUI.openCompareEditor(in);
		} else {
			CompareTreeView view;
			try {
				view = (CompareTreeView) PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage().showView(
								CompareTreeView.ID);
				view.setInput(resources, dlg.getCommitId().name());
			} catch (PartInitException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
		}
		return null;
	}

	private ITypedElement getElementForCommit(final Repository repository,
			final String gitPath, final ObjectId commitId) throws IOException {
		RevWalk rw = new RevWalk(repository);
		RevCommit commit = rw.parseCommit(commitId);
		rw.release();

		return CompareUtils.getFileRevisionTypedElement(gitPath, commit, repository);
	}

	@Override
	public boolean isEnabled() {
		return selectionMapsToSingleRepository();
	}
}
