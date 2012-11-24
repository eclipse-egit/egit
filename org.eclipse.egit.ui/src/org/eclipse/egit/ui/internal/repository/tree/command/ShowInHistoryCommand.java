/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza (dariusz@luksza.org) - disable command when HEAD cannot be
 *    										resolved
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.history.HistoryPageInput;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Implements "Show in History"
 */
public class ShowInHistoryCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final List<File> fileList = new ArrayList<File>();
		Repository repo = null;
		final RepositoryTreeNode nodeToShow;

		List<RepositoryTreeNode> selectedNodes = getSelectedNodes(event);
		if (selectedNodes.size() == 1) {
			RepositoryTreeNode selectedNode = selectedNodes.get(0);
			if (selectedNode.getType() == RepositoryTreeNodeType.REPO
					|| selectedNode.getType() == RepositoryTreeNodeType.FILE
					|| selectedNode.getType() == RepositoryTreeNodeType.FOLDER)
				nodeToShow = selectedNode;
			else
				nodeToShow = null;
		} else
			nodeToShow = null;
		if (nodeToShow == null)
			for (RepositoryTreeNode node : getSelectedNodes(event)) {
				if (repo == null)
					repo = node.getRepository();
				if (repo != node.getRepository())
					throw new ExecutionException(
							UIText.RepositoryAction_multiRepoSelection);
				if (node.getType() == RepositoryTreeNodeType.FOLDER) {
					fileList.add(((FolderNode) node).getObject());
				}
				if (node.getType() == RepositoryTreeNodeType.FILE) {
					fileList.add(((FileNode) node).getObject());
				}
			}
		final Repository repoToShow = repo;
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				try {
					IHistoryView part = (IHistoryView) PlatformUI
							.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage().showView(IHistoryView.VIEW_ID);
					if (nodeToShow != null)
						part.showHistoryFor(nodeToShow);
					else {
						part.showHistoryFor(new HistoryPageInput(repoToShow,
								fileList.toArray(new File[fileList.size()])));
					}
				} catch (PartInitException e1) {
					Activator.handleError(e1.getMessage(), e1, true);
				}
			}
		});
		return null;
	}

	@Override
	public void setEnabled(Object evaluationContext) {
		enableWhenRepositoryHaveHead(evaluationContext);
	}

}
