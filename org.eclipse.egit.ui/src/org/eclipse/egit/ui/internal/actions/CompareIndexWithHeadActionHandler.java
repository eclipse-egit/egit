/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

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
		final IPath[] locations = getSelectedLocations(event);
		final IPath baseLocation = locations[0];
		final String gitPath = RepositoryMapping.getMapping(baseLocation)
				.getRepoRelativePath(baseLocation);
		ITypedElement base;

		ITypedElement next;
		try {
			IFile file = ResourceUtil.getFileForLocation(baseLocation);
			if (file != null)
				base = CompareUtils.getHeadTypedElement(file);
			else
				base = CompareUtils.getHeadTypedElement(baseLocation, null);
			Ref head = repository.getRef(Constants.HEAD);
			RevWalk rw = new RevWalk(repository);
			RevCommit commit = rw.parseCommit(head.getObjectId());

			next = CompareUtils.getFileRevisionTypedElement(gitPath,
					commit, repository);
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
			return null;
		}

		final GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
				base, next, null);

		IWorkbenchPage workBenchPage = HandlerUtil.getActiveWorkbenchWindowChecked(event).getActivePage();
		CompareUtils.openInCompare(workBenchPage, in);

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
