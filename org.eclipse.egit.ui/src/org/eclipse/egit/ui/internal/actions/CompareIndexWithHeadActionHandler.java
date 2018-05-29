/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2013, Laurent Goubet <laurent.goubet@obeo.fr>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.resources.ResourceStateFactory;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Compares the index content of a file with the version of the file in
 * the HEAD commit.
 */
public class CompareIndexWithHeadActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {

		final Repository repository = getRepository(true, event);
		// assert all resources map to the same repository
		if (repository == null) {
			return null;
		}
		final Object fileOrPath = getSingleSelectedObject(event);
		if (fileOrPath == null) {
			return null;
		}
		try {
			runCompare(event, repository);
		} catch (Exception e) {
			Activator.handleError(
					UIText.CompareWithRefAction_errorOnSynchronize, e, true);
		}

		return null;
	}

	private void runCompare(ExecutionEvent event, @NonNull final Repository repository)
			throws Exception {
		IWorkbenchPage workBenchPage = HandlerUtil
				.getActiveWorkbenchWindowChecked(event).getActivePage();
		IResource[] resources = getSelectedResources(event);

		if (resources.length > 0) {
			CompareUtils.compare(resources, repository, GitFileRevision.INDEX,
					Constants.HEAD, false, workBenchPage);
		} else {
			IPath[] locations = getSelectedLocations(event);
			if (locations.length == 0) {
				return;
			}
			IPath location = locations[0];
			if (location == null) {
				return;
			}
			CompareUtils.compare(location, repository,
					GitFileRevision.INDEX, Constants.HEAD, false,
					workBenchPage);
		}
	}

	private Object getSingleSelectedObject(ExecutionEvent event)
			throws ExecutionException {
		IResource[] resources = getSelectedResources(event);
		if (resources.length == 1) {
			return resources[0];
		} else {
			IPath[] locations = getSelectedLocations(event);
			if (locations.length == 1) {
				return locations[0];
			}
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		IStructuredSelection selection = getSelection();
		if (selection.size() != 1) {
			return false;
		}

		Repository repository = getRepository();
		if (repository == null) {
			return false;
		}

		Object selected = selection.getFirstElement();
		IResource resource = AdapterUtils.adapt(selected, IResource.class);
		if (resource instanceof IFile) {
			// action is only working on files. Avoid calculation
			// of unnecessary expensive IndexDiff on a folder
			return ResourceStateFactory.getInstance().get(resource).isStaged();
		} else if (resource == null) {
			IPath location = AdapterUtils.adapt(selected, IPath.class);
			if (location != null) {
				return ResourceStateFactory.getInstance().get(location.toFile())
						.isStaged();
			}
		}

		return false;
	}
}
