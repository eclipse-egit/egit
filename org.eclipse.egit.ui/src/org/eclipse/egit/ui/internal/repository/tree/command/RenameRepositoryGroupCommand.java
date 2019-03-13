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
import java.util.Optional;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroup;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroupNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroups;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;

/**
 * Rename an existing repository group
 */
public class RenameRepositoryGroupCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryGroups groups = new RepositoryGroups();

		RepositoryGroup group = getSelectedGroup(event);
		Optional<String> optionalGroupName =
				AddRepositoryGroupCommand.getNewGroupName(getActiveShell(event),
						UIText.RepositoriesView_RenameRepoGroup_title, groups,
						group.getName());
		if (optionalGroupName.isPresent()) {
			String name = optionalGroupName.get();
			groups.renameGroup(group, name);
			getView(event).refresh();
		}
		return null;
	}

	private RepositoryGroup getSelectedGroup(ExecutionEvent event)
			throws ExecutionException {
		List<RepositoryTreeNode> elements = getSelectedNodes(event);
		if (elements.size() == 1
				&& elements.get(0) instanceof RepositoryGroupNode) {
			return ((RepositoryGroupNode) elements.get(0)).getGroup();
		} else {
			throw new ExecutionException(
					"can rename only a single repository group"); //$NON-NLS-1$
		}
	}

}
