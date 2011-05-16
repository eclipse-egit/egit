/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.lib.Repository;

/**
 * Implements an action to add a file to the Git index
 *
 */
public class AddToIndexCommand extends RepositoriesViewCommandHandler<FileNode> {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		AddCommand addCommand = null;
		List selectedNodes = getSelectedNodes(event);
		for (Object selectedNode : selectedNodes) {
			RepositoryTreeNode node = (RepositoryTreeNode) selectedNode;
			if (addCommand == null)
				addCommand = new Git(node.getRepository()).add();
			Repository repository = node.getRepository();
			IPath path;
			if (node.getType().equals(RepositoryTreeNodeType.FOLDER))
				path = new Path(((FolderNode) node).getObject()
						.getAbsolutePath());
			else if (node.getType().equals(RepositoryTreeNodeType.FILE))
				path = new Path(((FileNode) node).getObject().getAbsolutePath());
			else
				path = new Path(repository.getWorkTree().getAbsolutePath());
			String repoRelativepath;
			IPath workTreePath = new Path(repository.getWorkTree()
					.getAbsolutePath());
			if (path.equals(workTreePath))
				repoRelativepath = "."; //$NON-NLS-1$
			else
				repoRelativepath = path.removeFirstSegments(
								path.matchingFirstSegments(workTreePath))
						.setDevice(null).toString();
			addCommand.addFilepattern(repoRelativepath);
		}
		if (addCommand != null)
			try {
				addCommand.call();
			} catch (NoFilepatternException e) {
				Activator.logError(UIText.AddToIndexCommand_addingFilesFailed,
						e);
			}
		return null;
	}

	@Override
	public void setEnabled(Object evaluationContext) {
		enableWorkingDirCommand(evaluationContext);
	}

}
