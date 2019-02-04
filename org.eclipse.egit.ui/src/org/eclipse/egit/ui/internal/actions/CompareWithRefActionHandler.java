/*******************************************************************************
 * Copyright (C) 2010, 2013 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CompareTargetSelectionDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

/**
 * The "compare with ref" action. This action opens a diff editor comparing the
 * file as found in the working directory and the version in the selected ref.
 */
public class CompareWithRefActionHandler extends RepositoryActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repo = getRepository(true, event);
		// assert all resources map to the same repository
		if (repo == null)
			return null;
		final IResource[] resources = getSelectedResources(event);

		CompareTargetSelectionDialog dlg = new CompareTargetSelectionDialog(
				getShell(event), repo, resources.length == 1 ? resources[0]
						.getFullPath().lastSegment() : null);
		if (dlg.open() != Window.OK)
			return null;

		final String refName = dlg.getRefName();

		IWorkbenchPage workbenchPage = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		try {
			CompareUtils
					.compare(resources, repo, Constants.HEAD, refName, true, workbenchPage);
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
