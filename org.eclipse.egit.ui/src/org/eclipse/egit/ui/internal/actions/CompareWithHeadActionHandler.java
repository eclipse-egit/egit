/*******************************************************************************
 * Copyright (C) 2009, Stefan Lay <stefan.lay@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Compares the working tree content of a file with the version of the file in
 * the HEAD commit.
 */
public class CompareWithHeadActionHandler extends RepositoryActionHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(true, event);
		// assert all resources map to the same repository
		if (repository == null)
			return null;
		final IResource[] resources = getSelectedResources(event);

		if (resources.length == 1 && resources[0] instanceof IFile) {
			final IFile file = (IFile) resources[0];
			CompareUtils.compareHeadWithWorkspace(repository, file);
			return null;

		} else {
			CompareTreeView view;
			try {
				view = (CompareTreeView) PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage().showView(
								CompareTreeView.ID);
				try {
					view.setInput(resources, repository.resolve(Constants.HEAD)
							.name());
				} catch (IOException e) {
					Activator.handleError(e.getMessage(), e, true);
					return null;
				}
			} catch (PartInitException e) {
				Activator.handleError(e.getMessage(), e, true);
				return null;
			}
			return null;
		}
	}

	@Override
	public boolean isEnabled() {
		return getRepository() != null;
	}
}
