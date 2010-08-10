/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.util.Iterator;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;

/**
 * Compare the file content of a commit with the working tree
 */
public class CompareWithWorkingTreeHandler extends
		AbstractHistoryCommanndHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		if (selection.size() == 1) {
			Iterator<?> it = selection.iterator();
			RevCommit commit = (RevCommit) it.next();
			Object input = getInput(event);
			if (input instanceof IFile) {
				IFile file = (IFile) input;
				final RepositoryMapping mapping = RepositoryMapping
						.getMapping(file.getProject());
				final String gitPath = mapping.getRepoRelativePath(file);
				ITypedElement right = CompareUtils.getFileRevisionTypedElement(
						gitPath, commit, mapping.getRepository());
				final GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
						SaveableCompareEditorInput.createFileElement(file),
						right, null);
				openInCompare(event, in);
			}
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		try {
			int size = getSelection(null).size();
			return IFile.class.isAssignableFrom(getInput(null).getClass())
					&& size == 1;
		} catch (ExecutionException e) {
			Activator.handleError(e.getMessage(), e, false);
			return false;
		}
	}
}
