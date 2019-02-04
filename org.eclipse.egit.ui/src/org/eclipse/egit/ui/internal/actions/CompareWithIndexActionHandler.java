/*******************************************************************************
 * Copyright (C) 2009, Yann Simon <yann.simon.fr@gmail.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2013, Laurent Goubet <laurent.goubet@obeo.fr>
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
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * The "compare with index" action. This action opens a diff editor comparing
 * the file as found in the working directory and the version found in the index
 * of the repository.
 */
public class CompareWithIndexActionHandler extends RepositoryActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(true, event);
		// assert all resources map to the same repository
		if (repository == null)
			return null;

		IWorkbenchPage workbenchPage = HandlerUtil
				.getActiveWorkbenchWindowChecked(event).getActivePage();
		final IResource[] resources = getSelectedResources(event);
		try {
			if (resources.length > 0)
				CompareUtils.compare(resources, repository, Constants.HEAD,
						GitFileRevision.INDEX, true, workbenchPage);
			else {
				IPath[] locations = getSelectedLocations(event);
				if (locations.length == 0) {
					return null;
				}
				IPath location = locations[0];
				if (location == null) {
					return null;
				}
				CompareUtils.compare(location, repository, Constants.HEAD,
						GitFileRevision.INDEX, true, workbenchPage);
			}
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
