/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.Collection;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.ui.internal.operations.DeletePathsOperationUI;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;

/**
 * Delete a working dir file or folder from the repositories view.
 */
public class DeleteFileCommand extends RepositoriesViewCommandHandler<FileNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Collection<IPath> paths = getSelectedFileAndFolderPaths(event);

		RepositoriesView view = getView(event);
		DeletePathsOperationUI operation = new DeletePathsOperationUI(paths, view.getSite());
		operation.run();

		view.refresh();
		return null;
	}

	@Override
	public boolean isEnabled() {
		return isWorkingDirSelection();
	}
}
