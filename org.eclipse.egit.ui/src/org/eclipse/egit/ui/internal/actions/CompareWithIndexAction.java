/*******************************************************************************
 * Copyright (C) 2009, Yann Simon <yann.simon.fr@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.actions;

import java.io.File;
import java.io.IOException;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IContentChangeListener;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.EditableRevision;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.Repository;

/**
 * The "compare with index" action. This action opens a diff editor comparing
 * the file as found in the working directory and the version found in the index
 * of the repository.
 */
@SuppressWarnings("restriction")
public class CompareWithIndexAction extends RepositoryAction {

	@Override
	public void execute(IAction action) {
		final IResource resource = getSelectedResources()[0];
		final RepositoryMapping mapping = RepositoryMapping.getMapping(resource.getProject());
		final Repository repository = mapping.getRepository();
		final String gitPath = mapping.getRepoRelativePath(resource);

		final IFileRevision nextFile = GitFileRevision.inIndex(repository, gitPath);

		final IFile baseFile = (IFile) resource;
		final ITypedElement base = SaveableCompareEditorInput.createFileElement(baseFile);

		final EditableRevision next = new EditableRevision(nextFile);

		IContentChangeListener listener = new IContentChangeListener() {
			public void contentChanged(IContentChangeNotifier source) {
				final byte[] newContent = next.getModifiedContent();
				try {
					final GitIndex index = repository.getIndex();
					final File file = new File(baseFile.getLocation().toString());
					index.add(mapping.getWorkDir(), file, newContent);
					index.write();
				} catch (IOException e) {
					Utils.handleError(getTargetPart().getSite().getShell(), e,
							UIText.CompareWithIndexAction_errorOnAddToIndex,
							UIText.CompareWithIndexAction_errorOnAddToIndex);
					return;
				}
			}
		};

		next.addContentChangeListener(listener);

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
