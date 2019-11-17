/*******************************************************************************
 * Copyright (C) 2019, Alexander Nittka <alex@nittka.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.ui.navigator.CommonViewer;

/**
 * Collapse the working directory ancestors of the current repositories view
 * selection.
 */
public class CollapseWorkingTreeCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode<?>> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RepositoryTreeNode<?>> nodes = getSelectedNodes();
		Set<WorkingDirNode> nodesToCollapse = nodes.stream()
				.map(this::getWorkingDirParent).filter(Objects::nonNull)
				.collect(Collectors.toSet());
		if (!nodesToCollapse.isEmpty()) {
			CommonViewer viewer = getView(event).getCommonViewer();
			for (WorkingDirNode node : nodesToCollapse) {
				viewer.collapseToLevel(node, AbstractTreeViewer.ALL_LEVELS);
			}
		}
		return null;
	}

	private WorkingDirNode getWorkingDirParent(RepositoryTreeNode<?> node) {
		RepositoryTreeNode<?> candidate = node;
		while (candidate != null
				&& candidate.getType() != RepositoryTreeNodeType.WORKINGDIR) {
			candidate = candidate.getParent();
		}
		return (WorkingDirNode) candidate;
	}
}
