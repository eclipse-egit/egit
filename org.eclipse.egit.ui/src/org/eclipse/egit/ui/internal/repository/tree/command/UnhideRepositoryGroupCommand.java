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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroup;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroupNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroups;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;

/**
 * mark repository groups as not hidden
 */
public class UnhideRepositoryGroupCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RepositoryGroup> groupsToUnHide = new ArrayList<>();
		List<RepositoryTreeNode> elements = getSelectedNodes();
		for (Object element : elements) {
			if (element instanceof RepositoryGroupNode) {
				groupsToUnHide.add(((RepositoryGroupNode) element).getGroup());
			}
		}
		if (!groupsToUnHide.isEmpty()) {
			RepositoryGroups groups = new RepositoryGroups();
			groups.setHidden(groupsToUnHide, false);
			getView(event).refresh();
		}
		return null;
	}
}
