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
package org.eclipse.egit.ui.internal.repository.tree.filter;

import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Filter for RepositoriesView hiding nodes of a given type
 */
class NodeByTypeFilter extends ViewerFilter {

	private RepositoryTreeNodeType typeToHide;

	NodeByTypeFilter(RepositoryTreeNodeType typeToHide) {
		this.typeToHide = typeToHide;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof RepositoryTreeNode<?>) {
			RepositoryTreeNodeType type = ((RepositoryTreeNode) element)
					.getType();
			if (type == typeToHide) {
				return false;
			}
		}
		return true;
	}
}