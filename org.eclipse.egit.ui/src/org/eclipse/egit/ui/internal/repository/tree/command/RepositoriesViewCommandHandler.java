/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza (dariusz@luksza.org) - add initial implementation of
 *    										enableWhenRepositoryHaveHead(Object)
 *    Daniel Megert <daniel_megert@ch.ibm.com> - remove unnecessary @SuppressWarnings
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.internal.RepositoryUtil;
import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.jface.viewers.ISelection;
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

	public List<T> getSelectedNodes(ExecutionEvent event)
			throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
		if (selection instanceof IStructuredSelection)
			return ((IStructuredSelection) selection).toList();
		else
			return Collections.emptyList();
	}

	public Shell getActiveShell(ExecutionEvent event) throws ExecutionException {
		return HandlerUtil.getActiveShellChecked(event);
	}

	private boolean checkRepositoryHasHead(Object element) {
		if (element instanceof RepositoryTreeNode) {
			RepositoryTreeNode<?> treeNode = (RepositoryTreeNode<?>) element;
			Repository repo = treeNode.getRepository();
			try {
				Ref ref = repo.getRef(Constants.HEAD);
				return ref != null && ref.getObjectId() != null;
			} catch (IOException e) {
				// ignore and report false
				return false;
			}
		}
		return false;
	}

	protected boolean checkSelectionHasHead(Object evaluationContext,
			boolean all) {
		// get the current selection
		Object selection = HandlerUtil.getVariable(evaluationContext,
				ISources.ACTIVE_CURRENT_SELECTION_NAME);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			// check that it's a non-empty selection
			if (structuredSelection.size() > 0) {
				if (all) {
					// check that all the repositories have a valid head
					for (Object element : structuredSelection.toArray())
						if (!checkRepositoryHasHead(element))
							return false;
					return true;
				}

				// just check the first one
				return checkRepositoryHasHead(structuredSelection
						.getFirstElement());
			}
		}
		return false;
	}

	/**
	 * Enables this handler if all the repositories that have been selected in
	 * the current evaluation context is a repository that has a valid head
	 * reference.
	 *
	 * @param evaluationContext
	 *            the current application context
	 */
	protected void enableWhenAllRepositoriesHaveHead(Object evaluationContext) {
		setBaseEnabled(checkSelectionHasHead(evaluationContext, true));
	}

	/**
	 * Enables this handler if the first repository that has been selected in
	 * the current evaluation context is a repository that has a valid head
	 * reference.
	 *
	 * @param evaluationContext
	 *            the current application context
	 */
	protected void enableWhenRepositoryHaveHead(Object evaluationContext) {
		setBaseEnabled(checkSelectionHasHead(evaluationContext, false));
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
		if (!(evaluationContext instanceof IEvaluationContext)) {
			setBaseEnabled(false);
			return;
		}
		IEvaluationContext context = (IEvaluationContext) evaluationContext;
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
				if (object instanceof FolderNode)
					path = ((FolderNode) object).getObject().getAbsolutePath();
				else if (object instanceof FileNode)
					path = ((FileNode) object).getObject()
							.getAbsolutePath();
				else {
					setBaseEnabled(false);
					return;
				}
				if (path.startsWith(repository.getDirectory().getAbsolutePath())) {
					setBaseEnabled(false);
					return;
				}
			}
		}

		setBaseEnabled(true);
	}

	/**
	 * Retrieve the current selection. The global selection is used if the menu
	 * selection is not available.
	 *
	 * @param ctx
	 * @return the selection
	 */
	protected Object getSelection(IEvaluationContext ctx) {
		Object selection = ctx.getVariable(ISources.ACTIVE_MENU_SELECTION_NAME);
		if (selection == null || !(selection instanceof ISelection))
			selection = ctx.getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
		return selection;
	}

	protected Collection<IPath> getSelectedFileAndFolderPaths(ExecutionEvent event) throws ExecutionException {
		Collection<IPath> paths = new ArrayList<IPath>();
		for (Object selectedNode : getSelectedNodes(event)) {
			RepositoryTreeNode treeNode = (RepositoryTreeNode) selectedNode;
			IPath path = treeNode.getPath();
			paths.add(path);
		}
		return paths;
	}
}
