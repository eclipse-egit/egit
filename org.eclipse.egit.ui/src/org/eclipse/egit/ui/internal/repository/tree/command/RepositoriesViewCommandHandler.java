/*******************************************************************************
 * Copyright (c) 2010, 2015 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza (dariusz@luksza.org) - add initial implementation of
 *    										enableWhenRepositoryHaveHead(Object)
 *    Daniel Megert <daniel_megert@ch.ibm.com> - remove unnecessary @SuppressWarnings
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 482231
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.egit.ui.internal.selection.SelectionRepositoryStateCache;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

abstract class RepositoriesViewCommandHandler<T extends RepositoryTreeNode<?>>
		extends AbstractHandler {

	private IEvaluationContext evaluationContext;

	protected final RepositoryUtil util = Activator.getDefault()
			.getRepositoryUtil();

	public RepositoriesView getView(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePartChecked(event);
		return (RepositoriesView) part;
	}

	public Shell getShell(ExecutionEvent event) {
		return HandlerUtil.getActiveShell(event);
	}

	@Override
	public void setEnabled(Object evaluationContext) {
		if (evaluationContext instanceof IEvaluationContext)
			this.evaluationContext = (IEvaluationContext) evaluationContext;
		else
			this.evaluationContext = null;
	}

	@SuppressWarnings("unchecked")
	public List<T> getSelectedNodes(ExecutionEvent event)
			throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
		if (selection instanceof IStructuredSelection)
			return ((IStructuredSelection) selection).toList();
		else
			return Collections.emptyList();
	}

	/**
	 * Retrieve the current selection. The global selection is used if the menu
	 * selection is not available.
	 *
	 * @return the selection
	 */
	@SuppressWarnings("unchecked")
	protected List<T> getSelectedNodes() {
		if (evaluationContext != null) {
			Object selection = evaluationContext
					.getVariable(ISources.ACTIVE_MENU_SELECTION_NAME);
			if (selection == null
					|| !(selection instanceof IStructuredSelection))
				selection = evaluationContext
						.getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
			if (selection instanceof IStructuredSelection)
				return ((IStructuredSelection) selection).toList();
		}
		return Collections.emptyList();
	}

	public Shell getActiveShell(ExecutionEvent event) throws ExecutionException {
		return HandlerUtil.getActiveShellChecked(event);
	}

	private boolean repositoryHasHead(T treeNode) {
		return SelectionRepositoryStateCache.INSTANCE
				.getHead(treeNode.getRepository()) != null;
	}

	private boolean selectionHasHead(boolean all) {
		List<T> selectedNodes = getSelectedNodes();
		if (selectedNodes.size() > 0) {
			if (all) {
				// check that all the repositories have a valid head
				for (T element : selectedNodes)
					if (!repositoryHasHead(element))
						return false;
				return true;
			}

			// just check the first one
			return repositoryHasHead(selectedNodes.get(0));
		}
		return false;
	}

	/**
	 * @return true if the first repository that has been selected in the
	 *         current evaluation context is a repository that has a valid head
	 *         reference
	 */
	protected boolean selectedRepositoryHasHead() {
		return selectionHasHead(false);
	}

	/**
	 * @return true if all repositories that have been selected in the current
	 *         evaluation context have a valid head reference
	 */
	protected boolean selectedRepositoriesHaveHead() {
		return selectionHasHead(true);
	}

	/**
	 * Enable the command if all of the following conditions are fulfilled: <li>
	 * All selected nodes belong to the same repository <li>All selected nodes
	 * are of type FileNode or FolderNode or WorkingTreeNode <li>Each node does
	 * not represent a file / folder in the git directory
	 *
	 * @return true if selection is a working directory selection
	 */
	protected boolean isWorkingDirSelection() {
		List<T> selectedNodes = getSelectedNodes();
		if (selectedNodes.isEmpty())
			return false;
		Repository repository = null;

		for (T selectedNode : selectedNodes) {
			Repository nodeRepository = selectedNode.getRepository();
			if (repository == null)
				repository = nodeRepository;
			else if (repository != nodeRepository) {
				return false;
			}
			if (!(selectedNode instanceof WorkingDirNode)) {
				File file;
				if (selectedNode instanceof FolderNode)
					file = ((FolderNode) selectedNode).getObject()
							.getAbsoluteFile();
				else if (selectedNode instanceof FileNode)
					file = ((FileNode) selectedNode).getObject()
							.getAbsoluteFile();
				else {
					return false;
				}
				File gitDir = repository.getDirectory().getAbsoluteFile();
				if (file.toPath().startsWith(gitDir.toPath())) {
					return false;
				}
			}
		}

		return true;
	}

	protected Collection<IPath> getSelectedFileAndFolderPaths(ExecutionEvent event) throws ExecutionException {
		Collection<IPath> paths = new ArrayList<>();
		for (Object selectedNode : getSelectedNodes(event)) {
			RepositoryTreeNode treeNode = (RepositoryTreeNode) selectedNode;
			IPath path = treeNode.getPath();
			paths.add(path);
		}
		return paths;
	}

	static <T> T getFirstOrNull(List<T> list) {
		if (list.isEmpty())
			return null;
		return list.get(0);
	}
}
