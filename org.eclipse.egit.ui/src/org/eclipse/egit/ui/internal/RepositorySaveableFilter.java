/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.util.Collection;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISaveableFilter;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.Saveable;

/**
 * Filter which checks if the path/resources behind the saveable is part of the
 * work dir of the repository.
 */
public class RepositorySaveableFilter implements ISaveableFilter {

	private final IPath workDir;

	/**
	 * @param repository
	 *            to check
	 */
	public RepositorySaveableFilter(Repository repository) {
		this.workDir = new Path(repository.getWorkTree().getAbsolutePath());
	}

	public boolean select(Saveable saveable, IWorkbenchPart[] containingParts) {
		Collection<IResource> resources = ResourceUtil.getMappedResources(saveable);
		for (IResource resource : resources)
			if (isInWorkDir(resource))
				return true;

		// For editors of files outside of workspace
		// (e.g. opened from the repositories view)
		if (isTextFileBufferInWorkDir(saveable))
			return true;

		// For backwards compatibility, we need to check the parts
		for (IWorkbenchPart part : containingParts) {
			if (part instanceof IEditorPart) {
				IEditorPart editorPart = (IEditorPart) part;
				IFile file = org.eclipse.ui.ide.ResourceUtil.getFile(editorPart.getEditorInput());
				if (isInWorkDir(file))
					return true;
			}
		}

		return false;
	}

	private boolean isInWorkDir(IResource resource) {
		return resource != null && isInWorkDir(resource.getLocation());
	}

	private boolean isTextFileBufferInWorkDir(Saveable saveable) {
		IDocument document = (IDocument) saveable.getAdapter(IDocument.class);
		ITextFileBuffer textFileBuffer = FileBuffers.getTextFileBufferManager()
				.getTextFileBuffer(document);
		if (textFileBuffer != null)
			return isInWorkDir(textFileBuffer.getLocation());
		return false;
	}

	private boolean isInWorkDir(IPath location) {
		return location != null && workDir.isPrefixOf(location);
	}
}