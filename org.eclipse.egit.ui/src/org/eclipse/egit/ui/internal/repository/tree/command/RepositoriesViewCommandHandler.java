/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza (dariusz@luksza.org) - add initial implementation of
 *    										enableWhenRepositoryHaveHead(Object)
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

abstract class RepositoriesViewCommandHandler<T> extends AbstractHandler {

	protected final RepositoryUtil util = Activator.getDefault()
			.getRepositoryUtil();

	public RepositoriesView getView(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePartChecked(event);
		return (RepositoriesView) part;
	}

	public Shell getShell(ExecutionEvent event) {
		return HandlerUtil.getActiveShell(event);
	}

	@SuppressWarnings("unchecked")
	public List<T> getSelectedNodes(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelectionChecked(event);
		return selection.toList();
	}

	public Shell getActiveShell(ExecutionEvent event) throws ExecutionException {
		return HandlerUtil.getActiveShellChecked(event);
	}

	protected void enableWhenRepositoryHaveHead(Object evaluationContext) {
		Object selection = HandlerUtil.getVariable(evaluationContext,
				ISources.ACTIVE_CURRENT_SELECTION_NAME);
		if (selection instanceof TreeSelection) {
			TreeSelection treeSelection = (TreeSelection) selection;
			Object firstElement = treeSelection.getFirstElement();
			if (firstElement instanceof RepositoryTreeNode) {
				RepositoryTreeNode<?> treeNode = (RepositoryTreeNode<?>) firstElement;
				Repository repo = treeNode.getRepository();
				boolean enabled = false;
				try {
					Ref ref = repo.getRef(Constants.HEAD);
					enabled = ref != null && ref.getObjectId() != null;
				} catch (IOException e) {
					enabled = false;
				}
				setBaseEnabled(enabled);
				return;
			}
		}

		setBaseEnabled(false);
	}

	/**
	 * Enable the command if all of the following conditions are fulfilled: <li>
	 * All selected nodes belong to the same repository <li>All selected nodes
	 * are of type FileNode or FolderNode or WorkingTreeNode <li>Each node does
	 * not represent a file / folder in the git directory
	 *
	 * @param evaluationContext
	 */
	protected void enableWorkingDirCommand(Object evaluationContext) {
		if (!(evaluationContext instanceof EvaluationContext)) {
			setBaseEnabled(false);
			return;
		}
		EvaluationContext context = (EvaluationContext) evaluationContext;
		Object selection = context
				.getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
		if (!(selection instanceof TreeSelection)) {
			setBaseEnabled(false);
			return;
		}
		Repository repository = null;
		TreeSelection treeSelection = (TreeSelection) selection;
		for (Iterator iterator = treeSelection.iterator(); iterator.hasNext();) {
			Object object = iterator.next();
			if (!(object instanceof RepositoryTreeNode)) {
				setBaseEnabled(false);
				return;
			}
			Repository nodeRepository = ((RepositoryTreeNode) object)
					.getRepository();
			if (repository == null)
				repository = nodeRepository;
			else if (repository != nodeRepository) {
				setBaseEnabled(false);
				return;
			}
			if (!(object instanceof WorkingDirNode)) {
				String path;
				if (object instanceof FolderNode) {
					path = ((FolderNode) object).getObject().getAbsolutePath();
				} else {
					if (object instanceof FileNode) {
						path = ((FileNode) object).getObject()
								.getAbsolutePath();
					} else {
						setBaseEnabled(false);
						return;
					}
				}
				if (path.startsWith(repository.getDirectory().getAbsolutePath())) {
					setBaseEnabled(false);
					return;
				}
			}
		}

		setBaseEnabled(true);
	}

}
