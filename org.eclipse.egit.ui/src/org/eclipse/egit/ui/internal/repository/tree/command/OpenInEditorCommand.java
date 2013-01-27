/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

/**
 * Implements "Open in Editor"
 */
public class OpenInEditorCommand extends
		RepositoriesViewCommandHandler<FileNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		FileNode node = getSelectedNodes(event).get(0);
		IPath path = new Path(node.getObject().getAbsolutePath());

		IFile file = ResourcesPlugin.getWorkspace().getRoot()
				.getFileForLocation(path);
		if (file == null) {
			IFileStore store = EFS.getLocalFileSystem().getStore(path);
			try {
				IDE.openEditorOnFileStore(getView(event).getSite().getPage(),
						store);
			} catch (PartInitException e) {
				Activator.handleError(
						UIText.RepositoriesView_Error_WindowTitle, e, true);
			}
		} else
			try {
				IDE.openEditor(getView(event).getSite().getPage(), file);
			} catch (PartInitException e) {
				Activator.handleError(
						UIText.RepositoriesView_Error_WindowTitle, e, true);
			}
		return null;
	}
}
