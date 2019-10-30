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
package org.eclipse.egit.ui.internal.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.groups.RepositoryGroup;
import org.eclipse.egit.ui.internal.groups.RepositoryGroups;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroupNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.command.CreateRepositoryGroupCommand;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

/**
 * The dynamic "Repository Groups" context menu for repositories in the
 * RepositoriesView, in particular for managing the repository to group
 * assignment.
 */
public class RepositoryGroupsMenu extends CompoundContributionItem
		implements IWorkbenchContribution {

	private IServiceLocator serviceLocator;

	@Override
	public void initialize(IServiceLocator locator) {
		this.serviceLocator = locator;
	}

	@Override
	protected IContributionItem[] getContributionItems() {
		List<IContributionItem> result = new ArrayList<>();
		ISelection selection = serviceLocator
				.getService(ISelectionService.class).getSelection();
		List<RepositoryNode> selectedRepoNodes = getRepositoryNodes(selection);
		final List<File> selectedRepoDirectories = selectedRepoNodes.stream()
				.map(node -> node.getRepository().getDirectory())
				.collect(Collectors.toList());

		if (!selectedRepoDirectories.isEmpty()) {
			final RepositoryGroups groupsUtil = RepositoryGroups.getInstance();
			addStaticItems(groupsUtil, selectedRepoDirectories, result);

			List<RepositoryGroup> groups = getMoveTargetGroups(groupsUtil,
					selectedRepoNodes);
			if (!groups.isEmpty()) {
				result.add(new Separator());
				for (RepositoryGroup group : groups) {
					result.add(getAssignGroupItem(groupsUtil, group,
							selectedRepoDirectories));
				}
			}
		}
		return result.toArray(new IContributionItem[0]);
	}

	private List<RepositoryGroup> getMoveTargetGroups(
			RepositoryGroups groupsUtil, List<RepositoryNode> selection) {
		List<RepositoryGroup> groups = groupsUtil.getGroups();
		Set<RepositoryTreeNode> repoNodeParents = selection.stream()
				.map(RepositoryNode::getParent).collect(Collectors.toSet());
		if (repoNodeParents.size() == 1) {
			RepositoryTreeNode uniqueParent = repoNodeParents.iterator().next();
			if (uniqueParent != null
					&& uniqueParent instanceof RepositoryGroupNode) {
				RepositoryGroup parentGroup = ((RepositoryGroupNode) uniqueParent)
						.getGroup();
				groups.remove(parentGroup);
			}
		}
		Collections.sort(groups,
				(g1, g2) -> CommonUtils.STRING_ASCENDING_COMPARATOR
						.compare(g1.getName(), g2.getName()));
		return groups;
	}

	private void addStaticItems(RepositoryGroups groupsUtil,
			List<File> selectedRepoDirectories, List<IContributionItem> menu) {
		CommandContributionItemParameter createCommandParameters = new CommandContributionItemParameter(
				serviceLocator, null, CreateRepositoryGroupCommand.COMMAND_ID,
				CommandContributionItem.STYLE_PUSH);
		createCommandParameters.label = UIText.RepositoriesView_RepoGroup_Create_MenuItemLabel;
		menu.add(new CommandContributionItem(createCommandParameters));
		for (File repoDir : selectedRepoDirectories) {
			if (groupsUtil.belongsToGroup(repoDir)) {
				menu.add(new ActionContributionItem(new Action(
						UIText.RepositoriesView_RepoGroup_Remove_Title,
						UIIcons.REMOVE_FROM_REPO_GROUP) {
					@Override
					public void runWithEvent(Event event) {
						groupsUtil.removeFromGroups(selectedRepoDirectories);
						refreshRepositoriesView();
					}
				}));
				break;
			}
		}
	}

	private IContributionItem getAssignGroupItem(RepositoryGroups groupsUtil,
			RepositoryGroup group, List<File> selectedRepoDirectories) {
		final UUID id = group.getGroupId();
		String actionTitle = NLS.bind(UIText.RepositoriesView_RepoGroup_Assign,
				group.getName());
		return new ActionContributionItem(new Action(actionTitle) {
			@Override
			public void runWithEvent(Event event) {
				groupsUtil.addRepositoriesToGroup(id, selectedRepoDirectories);
				refreshRepositoriesView();
			}
		});
	}

	private List<RepositoryNode> getRepositoryNodes(ISelection selection) {
		List<RepositoryNode> result = new ArrayList<>();
		if (selection instanceof StructuredSelection) {
			Object[] elements = ((StructuredSelection) selection).toArray();
			for (Object element : elements) {
				if (element instanceof RepositoryNode) {
					result.add((RepositoryNode) element);
				}
			}
		}
		return result;
	}

	private void refreshRepositoriesView() {
		RepositoriesView view = (RepositoriesView) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActivePart();
		view.refresh();
	}
}
