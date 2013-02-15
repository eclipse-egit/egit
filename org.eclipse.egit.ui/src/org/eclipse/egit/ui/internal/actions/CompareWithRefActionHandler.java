/*******************************************************************************
 * Copyright (C) 2010, 2013 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler <mathias.kinzler@sap.com>
 *    Laurent Goubet <laurent.goubet@obeo.fr>
 *    Gunnar Wagenknecht <gunnar@wagenknecht.org>
 *******************************************************************************/

package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.dialogs.CompareTargetSelectionDialog;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.egit.ui.internal.synchronize.GitModelSynchronize;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * The "compare with ref" action. This action opens a diff editor comparing the
 * file as found in the working directory and the version in the selected ref.
 */
public class CompareWithRefActionHandler extends RepositoryActionHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repo = getRepository(true, event);
		// assert all resources map to the same repository
		if (repo == null)
			return null;
		final IResource[] resources = getSelectedResources(event);

		CompareTargetSelectionDialog dlg = new CompareTargetSelectionDialog(
				getShell(event), repo, resources.length == 1 ? resources[0]
						.getFullPath().lastSegment() : null);
		if (dlg.open() == Window.OK) {

			if (resources.length == 1 && resources[0] instanceof IFile) {
				final IFile baseFile = (IFile) resources[0];

				if (CompareUtils.canDirectlyOpenInCompare(baseFile)) {
					showSingleFileComparison(baseFile, dlg.getRefName());
				} else {
					synchronizeModel(baseFile, repo, dlg.getRefName());
				}
			} else {
				CompareTreeView view;
				try {
					view = (CompareTreeView) PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage()
							.showView(CompareTreeView.ID);
					view.setInput(resources, dlg.getRefName());
				} catch (PartInitException e) {
					Activator.handleError(e.getMessage(), e, true);
				}
			}
		}
		return null;
	}

	private void showSingleFileComparison(IFile file, String refName) {
		try {
			CompareUtils.compareWorkspaceWithRef(getRepository(), file,
					refName, null);
		} catch (IOException e) {
			Activator.handleError(
					UIText.CompareWithRefAction_errorOnSynchronize, e, true);
		}
	}

	private void synchronizeModel(final IFile file, Repository repo,
			String refName) {
		try {
			GitModelSynchronize.synchronizeModelWithWorkspace(file, repo,
					refName);
		} catch (IOException e) {
			Activator.handleError(
					UIText.CompareWithRefAction_errorOnSynchronize, e, true);
			return;
		}
	}

	@Override
	public boolean isEnabled() {
		return selectionMapsToSingleRepository();
	}
}
