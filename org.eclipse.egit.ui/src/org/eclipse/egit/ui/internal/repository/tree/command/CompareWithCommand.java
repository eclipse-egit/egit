/*******************************************************************************
 * Copyright (C) 2021, Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.info.GitItemState;
import org.eclipse.egit.core.internal.info.GitItemStateFactory;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.merge.GitCompareEditorInput;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.selection.SelectionRepositoryStateCache;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.services.IEvaluationService;

/**
 * Compares the working tree with a commit or ref.
 */
public abstract class CompareWithCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RepositoryTreeNode> nodes = getSelectedNodes();
		if (nodes.isEmpty()) {
			return null;
		}
		List<IPath> paths = new ArrayList<>();
		Repository repository = null;
		boolean allPaths = false;
		for (RepositoryTreeNode<?> node : nodes) {
			Repository nodeRepository = node.getRepository();
			if (repository == null) {
				repository = nodeRepository;
			} else if (repository != nodeRepository) {
				return null;
			}
			switch (node.getType()) {
			case WORKINGDIR:
				allPaths = true;
				paths.clear();
				break;
			case FOLDER:
			case FILE:
				if (!allPaths) {
					paths.add(node.getPath());
				}
				break;
			default:
				break;
			}
		}
		if (repository != null && (allPaths || !paths.isEmpty())) {
			String ref = getRef(event, repository, paths);
			if (ref == null) {
				return null;
			}
			IWorkbenchPage workbenchPage = HandlerUtil
					.getActiveWorkbenchWindowChecked(event).getActivePage();
			GitCompareEditorInput compareInput;
			if (allPaths) {
				compareInput = new GitCompareEditorInput(null, ref, repository);
			} else {
				compareInput = new GitCompareEditorInput(null, ref, repository,
						paths.toArray(new IPath[0]));
			}
			CompareUtils.openInCompare(workbenchPage, compareInput);
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		if (!super.isEnabled()) {
			return false;
		}
		IStructuredSelection selection = SelectionUtils.getSelection(
				PlatformUI.getWorkbench().getService(IEvaluationService.class)
						.getCurrentState());
		Repository repository = SelectionUtils.getRepository(selection);
		if (repository == null || repository.isBare()) {
			return false;
		}
		return isEnabled(selection, repository);
	}

	/**
	 * Determines the ref or OID to compare against.
	 *
	 * @param event
	 *            {@link ExecutionEvent} that triggered the command execution
	 * @param repository
	 *            {@link Repository} to get a ref or commit from
	 * @param paths
	 *            Absolute file system paths of selected files or directories
	 *            within the repository
	 * @return A ref name or OID to use for the comparison, or {@code null} to
	 *         abort the comparison
	 */
	protected abstract String getRef(ExecutionEvent event,
			@NonNull Repository repository, Collection<IPath> paths);

	/**
	 * Determines whether the command is enabled for the given selection in the
	 * given repository. Subclasses can rely on all selected elements being in
	 * the given repository.
	 *
	 * @param selection
	 *            {@link IStructuredSelection} currently active
	 * @param repository
	 *            {@link Repository} the selected items are in
	 * @return {@code true} if the command is enabled, {@code false} otherwise
	 */
	protected boolean isEnabled(IStructuredSelection selection,
			@NonNull Repository repository) {
		if (SelectionRepositoryStateCache.INSTANCE
				.getHead(repository) == null) {
			return false;
		}
		for (Object o : selection.toList()) {
			if (o instanceof RepositoryTreeNode<?>) {
				RepositoryTreeNode<?> node = (RepositoryTreeNode<?>) o;
				switch (node.getType()) {
				case WORKINGDIR:
					return true;
				case FOLDER:
				case FILE:
					IPath path = node.getPath();
					GitItemState state = GitItemStateFactory.getInstance()
							.get(path.toFile());
					if (!state.isIgnored()) {
						return true;
					}
					break;
				default:
					return false;
				}
			}
		}
		return false;
	}
}
