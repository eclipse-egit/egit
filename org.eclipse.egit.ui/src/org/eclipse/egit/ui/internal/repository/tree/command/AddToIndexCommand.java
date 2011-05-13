/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
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
		RepositoryTreeNode node = getSelectedNodes(event).get(0);
		Repository repository = node.getRepository();
		String path;
		if (node.getType().equals(RepositoryTreeNodeType.FOLDER))
			path = ((FolderNode) node).getObject().getAbsolutePath();
		else
			path = ((FileNode) node).getObject().getAbsolutePath();
		String repoRelativepath = path.substring(repository.getWorkTree()
				.getPath().length() + 1);
		AddCommand addCommand = new Git(node.getRepository()).add();
		addCommand.addFilepattern(repoRelativepath);
		try {
			addCommand.call();
		} catch (NoFilepatternException e) {
			Activator.logError(UIText.AddToIndexCommand_addingFilesFailed, e);
		}
		return null;
	}
}
