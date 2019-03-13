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

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroups;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.swt.widgets.Shell;

/**
 * Creates a repository group, if repositories are selected, they are added to
 * the group
 */
public class CreateRepositoryGroupCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode> {

	/**
	 * id of the CreateRepositoryGroupCommand as used in the plugin.xml
	 */
	public static final String COMMAND_ID = "org.eclipse.egit.ui.RepositoriesCreateGroup"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryGroups groupsUtil = RepositoryGroups.getInstance();

		String groupName = getNewGroupName(getActiveShell(event),
				UIText.RepositoriesView_RepoGroup_Create_Title, groupsUtil, ""); //$NON-NLS-1$
		if (groupName != null) {
			UUID groupId = groupsUtil.createGroup(groupName);
			List<File> repoDirs = getSelectedRepositories(event);
			if (!repoDirs.isEmpty()) {
				groupsUtil.addRepositoriesToGroup(groupId, repoDirs);
			}
			getView(event).refresh();
		}
		return null;
	}

	private List<File> getSelectedRepositories(ExecutionEvent event)
			throws ExecutionException {
		return getSelectedNodes(event).stream()
				.filter(node -> node instanceof RepositoryNode)
				.filter(node -> node.getParent() == null || node.getParent()
						.getType() == RepositoryTreeNodeType.REPOGROUP)
				.map(e -> e.getRepository().getDirectory())
				.collect(Collectors.toList());
	}

	static String getNewGroupName(Shell shell, String title,
			RepositoryGroups groupsUtil, String initialName) {
		InputDialog inputDialog = new InputDialog(shell, title,
				UIText.RepositoriesView_RepoGroup_EnterName, initialName,
				name -> {
					if (name == null
							|| StringUtils.isEmptyOrNull(name.trim())) {
						return UIText.RepositoriesView_RepoGroup_EmptyNameError;
					}
					if (groupsUtil.groupExists(name.trim())) {
						return UIText.RepositoriesView_RepoGroup_GroupExists;
					}
					return null;
				});

		if (inputDialog.open() == Window.OK) {
			return inputDialog.getValue().trim();
		} else {
			return null;
		}
	}
}
