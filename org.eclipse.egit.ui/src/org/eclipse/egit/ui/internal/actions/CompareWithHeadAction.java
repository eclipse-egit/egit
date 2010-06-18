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
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.jface.action.IAction;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.storage.file.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;

/**
 * Compares the working tree content of a file with the version of the file
 * in the HEAD commit.
 */
public class CompareWithHeadAction extends RepositoryAction {

	public void execute(IAction action) throws InvocationTargetException {
		final IResource resource = getSelectedResources()[0];
		final RepositoryMapping mapping = RepositoryMapping.getMapping(resource.getProject());
		final Repository repository = mapping.getRepository();
		final String gitPath = mapping.getRepoRelativePath(resource);


		final IFile baseFile = (IFile) resource;
		final ITypedElement base = SaveableCompareEditorInput.createFileElement(baseFile);

		ITypedElement next;
		try {
			Ref head = repository.getRef(Constants.HEAD);
			RevWalk rw = new RevWalk(repository);
			RevCommit commit = rw.parseCommit(head.getObjectId());

			next = CompareUtils.getFileRevisionTypedElement(gitPath, commit,
					repository);
		} catch (IOException e) {
			// this exception is handled by TeamAction.run
			throw new InvocationTargetException(e);
		}


		final GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
				base, next, null);
		CompareUI.openCompareEditor(in);
	}

	@Override
	public boolean isEnabled() {
		final IResource[] selectedResources = getSelectedResources();
		if (selectedResources.length != 1)
			return false;

		final IResource resource = selectedResources[0];
		if (!(resource instanceof IFile)) {
			return false;
		}
		final RepositoryMapping mapping = RepositoryMapping.getMapping(resource.getProject());
		return mapping != null;
	}


}
