/*******************************************************************************
 * Copyright (C) 2019, Alexander Nittka <alex@nittka.de> and others.
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
import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.groups.RepositoryGroup;
import org.eclipse.egit.ui.internal.groups.RepositoryGroups;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroupNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.swt.SWT;
import org.eclipse.ui.navigator.CommonViewer;

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

		RepositoryGroup group;
		try {
			group = groupsUtil.createGroup(newGroupName(groupsUtil));
		} catch (IllegalStateException e) {
			throw new ExecutionException(e.getLocalizedMessage(), e);
		}
		List<File> repoDirs = getSelectedRepositories(event);
		if (!repoDirs.isEmpty()) {
			groupsUtil.addRepositoriesToGroup(group, repoDirs);
		}
		CommonViewer viewer = getView(event).getCommonViewer();
		viewer.refresh();
		viewer.setSelection(
				new StructuredSelection(new RepositoryGroupNode(group)), true);
		IStructuredSelection sel = viewer.getStructuredSelection();
		if ("gtk".equals(SWT.getPlatform())) { //$NON-NLS-1$
			// If run immediately GTK sizes the editor wrongly, and sizes it
			// correctly only once the user clicks into it.
			viewer.getControl().getDisplay().asyncExec(
					() -> viewer.editElement(sel.getFirstElement(), 0));
		} else {
			// But on other platforms Display.asyncExec() leads to noticeable
			// flickering.
			viewer.editElement(sel.getFirstElement(), 0);
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

	private static @NonNull String newGroupName(RepositoryGroups groups) {
		for (int i = 1; i < 100; i++) {
			String name = MessageFormat.format(
					UIText.RepositoriesView_NewGroupFormat, Integer.valueOf(i));
			if (name != null && !groups.groupExists(name)) {
				return name;
			}
		}
		// Come on!
		return MessageFormat.format(UIText.RepositoriesView_NewGroupFormat,
				Integer.valueOf(1)) + ' ' + UUID.randomUUID().toString();
	}
}
