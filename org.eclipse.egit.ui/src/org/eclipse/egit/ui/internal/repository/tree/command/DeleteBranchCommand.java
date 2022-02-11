/*******************************************************************************
 * Copyright (c) 2010, 2022 SAP AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.DeleteBranchOperationUI;
import org.eclipse.egit.ui.internal.repository.tree.BranchHierarchyNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * Deletes selected branches in one or several repositories.
 */
public class DeleteBranchCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		List<RepositoryTreeNode> nodes = getSelectedNodes(event);
		Map<Repository, List<Ref>> refs = getRefsToDelete(nodes);
		try {
			DeleteBranchOperationUI.deleteBranches(refs);
		} catch (InvocationTargetException e) {
			throw new ExecutionException(
					UIText.RepositoriesView_BranchDeletionFailureMessage,
					e.getCause());
		}
		return null;
	}

	private Map<Repository, List<Ref>> getRefsToDelete(
			List<RepositoryTreeNode> nodes) {
		Map<Repository, List<Ref>> refs = new HashMap<>();
		for (RepositoryTreeNode node : nodes) {
			if (node instanceof BranchHierarchyNode) {
				try {
					for (Ref ref : ((BranchHierarchyNode) node)
							.getChildRefsRecursive()) {
						refs.computeIfAbsent(node.getRepository(),
								key -> new ArrayList<>()).add(ref);
					}
				} catch (IOException e) {
					Activator.logError(MessageFormat.format(
							UIText.RepositoriesView_BranchCollectionError,
							node.getPath(),
							node.getRepository().getDirectory()), e);
				}
			} else if (node instanceof RefNode) {
				refs.computeIfAbsent(node.getRepository(),
						key -> new ArrayList<>()).add((Ref) node.getObject());
			}
		}
		return refs;
	}
}
