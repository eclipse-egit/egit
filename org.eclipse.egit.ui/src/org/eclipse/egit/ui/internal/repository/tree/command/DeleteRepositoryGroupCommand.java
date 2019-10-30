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
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.groups.RepositoryGroup;
import org.eclipse.egit.ui.internal.groups.RepositoryGroups;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroupNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;

/**
 * Deletes a repository group, the repositories themselves are not affected.
 */
public class DeleteRepositoryGroupCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RepositoryGroup> groupsToDelete = new ArrayList<>();
		List<RepositoryGroupNode> groupsNodes = new ArrayList<>();
		List<RepositoryTreeNode> elements = getSelectedNodes();
		int numberOfRepos = 0;
		for (Object element : elements) {
			if (element instanceof RepositoryGroupNode) {
				RepositoryGroupNode groupNode = (RepositoryGroupNode) element;
				groupsNodes.add(groupNode);
				RepositoryGroup group = groupNode.getObject();
				groupsToDelete.add(group);
				numberOfRepos += group.getRepositoryDirectories().size();
			}
		}
		if (!groupsToDelete.isEmpty()) {
			IPreferenceStore store = Activator.getDefault()
					.getPreferenceStore();

			boolean delete = true;
			if (numberOfRepos > 0 && store
					.getBoolean(UIPreferences.SHOW_DELETE_REPO_GROUP_WARNING)) {
				DeleteRepositoryGroupConfirmDialog confirmDelete = new DeleteRepositoryGroupConfirmDialog(
						getShell(event), groupsNodes);
				delete = confirmDelete.open() == Window.OK;
				if (!confirmDelete.showAgain()) {
					store.setValue(UIPreferences.SHOW_DELETE_REPO_GROUP_WARNING,
							false);
				}
			}
			if (delete) {
				RepositoryGroups.getInstance().delete(groupsToDelete);
				getView(event).refresh();
			}
		}
		return null;
	}
}
