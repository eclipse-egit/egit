/*******************************************************************************
 * Copyright (C) 2011, 2013 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.CommitSelectionDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

/**
 * The "compare with commit" action. This action opens a diff editor comparing
 * the file as found in the working directory and the version in the selected
 * ref.
 */
public class CompareWithCommitActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repo = getRepository(true, event);
		if (repo == null)
			return null;
		IResource[] resources = getSelectedResources(event);

		CommitSelectionDialog dlg = new CommitSelectionDialog(getShell(event),
				repo, resources);
		if (dlg.open() != Window.OK)
			return null;

		final String commitId = dlg.getCommitId().getName();

		IWorkbenchPage workbenchPage = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		try {
			CompareUtils.compare(resources, repo, Constants.HEAD, commitId,
					true, workbenchPage);
		} catch (IOException e) {
			Activator.handleError(
					UIText.CompareWithRefAction_errorOnSynchronize, e, true);
		}

		return null;
	}

	@Override
	public boolean isEnabled() {
		return selectionMapsToSingleRepository();
	}
}
