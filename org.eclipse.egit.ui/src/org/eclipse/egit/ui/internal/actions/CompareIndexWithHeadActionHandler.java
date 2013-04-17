/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2013, laurent Goubet <laurent.goubet@obeo.fr>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.osgi.util.NLS;

/**
 * Compares the index content of a file with the version of the file in
 * the HEAD commit.
 */
public class CompareIndexWithHeadActionHandler extends RepositoryActionHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(true, event);
		// assert all resources map to the same repository
		if (repository == null)
			return null;

		IResource[] resources = getSelectedResources();
		try {
			CompareUtils.compare(resources, repository, GitFileRevision.INDEX,
					Constants.HEAD, false);
		} catch (IOException e) {
			Activator.handleError(
					UIText.CompareWithRefAction_errorOnSynchronize, e, true);
		}

		return null;
	}

	@Override
	public boolean isEnabled() {
		IStructuredSelection selection = getSelection();
		if (selection.size() != 1)
			return false;

		Repository repository = getRepository();
		if (repository == null)
			return false;

		IResource resource = AdapterUtils.adapt(selection.getFirstElement(), IResource.class);
		if (resource != null) {
			// action is only working on files. Avoid calculation
			// of unnecessary expensive IndexDiff on a folder
			if (resource instanceof IFile)
				return isStaged(repository, resource.getLocation());
		} else {
			IPath location = AdapterUtils.adapt(selection.getFirstElement(), IPath.class);
			if (location != null && location.toFile().isFile())
				return isStaged(repository, location);
		}

		return false;
	}

	private boolean isStaged(Repository repository,
			IPath location) {
		String resRelPath = RepositoryMapping.getMapping(location).getRepoRelativePath(location);

		// This action at the moment only works for files anyway
		if (resRelPath == null || resRelPath.length() == 0) {
			return false;
		}

		try {
			FileTreeIterator fileTreeIterator = new FileTreeIterator(repository);
			IndexDiff indexDiff = new IndexDiff(repository, Constants.HEAD,
					fileTreeIterator);
			indexDiff.setFilter(PathFilterGroup.createFromStrings(Collections.singletonList(resRelPath)));
			indexDiff.diff();

			return indexDiff.getAdded().contains(resRelPath) || indexDiff.getChanged().contains(resRelPath)
					|| indexDiff.getRemoved().contains(resRelPath);
		} catch (IOException e) {
			Activator.error(NLS.bind(UIText.GitHistoryPage_errorLookingUpPath,
					location.toString()), e);
			return false;
		}
	}
}
