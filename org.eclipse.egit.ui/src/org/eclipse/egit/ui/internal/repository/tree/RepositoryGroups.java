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
package org.eclipse.egit.ui.internal.repository.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.util.StringUtils;
import org.osgi.service.prefs.BackingStoreException;

/**
 * This class manages the repository groups. The data is stored in the
 * preferences.
 */
public class RepositoryGroups {

	private Map<UUID, RepositoryGroup> groupMap = new HashMap<>();

	private IEclipsePreferences preferences = Activator.getDefault()
			.getRepositoryUtil().getPreferences();

	private static final String PREFS_GROUPS = "GitRepositoriesView.RepositoryGroups.uuids"; //$NON-NLS-1$

	private static final String PREFS_HIDEABLE_GROUPS = "GitRepositoriesView.RepositoryGroups.uuidsHideable"; //$NON-NLS-1$

	private static final String PREFS_GROUP_NAME_PREFIX = "GitRepositoriesView.RepositoryGroups."; //$NON-NLS-1$

	private static final String PREFS_GROUP_PREFIX = "GitRepositoriesView.RepositoryGroups.group."; //$NON-NLS-1$

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private static final String SEPARATOR = "\n";//$NON-NLS-1$

	/**
	 * new repository groups initialized from preferences
	 */
	public RepositoryGroups() {
		List<String> groups = split(
				preferences.get(PREFS_GROUPS, EMPTY_STRING));
		List<String> hideableGroups = split(
				preferences.get(PREFS_HIDEABLE_GROUPS, EMPTY_STRING));
		for (String groupIdString : groups) {
			UUID groupId = UUID.fromString(groupIdString);
			String name = preferences
					.get(PREFS_GROUP_NAME_PREFIX + groupIdString, EMPTY_STRING);
			List<String> repos = split(preferences
					.get(PREFS_GROUP_PREFIX + groupIdString, EMPTY_STRING));
			RepositoryGroup group = new RepositoryGroup(groupId, name, repos);
			if (hideableGroups.contains(groupIdString)) {
				group.setHideable(true);
			}
			groupMap.put(groupId, group);
		}
	}

	private List<String> split(String input) {
		List<String> result = new ArrayList<>();
		String[] split = input.split(SEPARATOR);
		for (String string : split) {
			if (string != null) {
				String trimmed = string.trim();
				if (trimmed.length() > 0) {
					result.add(trimmed);
				}
			}
		}
		return result;
	}

	/**
	 * @param groupName
	 *            valid name of the new group
	 * @return id of the new group
	 */
	public UUID addGroup(String groupName) {
		if (!groupExists(groupName)) {
			String trimmedName = groupName.trim();
			UUID groupId = UUID.randomUUID();
			RepositoryGroup group = new RepositoryGroup(groupId, trimmedName);
			groupMap.put(groupId, group);
			savePreferences();
			return groupId;
		} else {
			throw new IllegalStateException(
					UIText.RepositoriesView_RepoGroup_GroupExists);
		}
	}

	/**
	 * @param group
	 *            the group to rename
	 * @param newName
	 *            new name of the group
	 */
	public void renameGroup(RepositoryGroup group, String newName) {
		checkGroupName(newName);
		RepositoryGroup myGroup = groupMap.get(group.getGroupId());
		if (myGroup != null) {
			myGroup.setGroupName(newName);
			savePreferences();
		}
	}

	/**
	 * @param groupName
	 *            name of the group
	 * @return true if and only if a group of the given name already exists
	 * @throws IllegalArgumentException
	 *             if the group name is not valid
	 *
	 */
	public boolean groupExists(String groupName)
			throws IllegalArgumentException {
		checkGroupName(groupName);
		for (RepositoryGroup group : groupMap.values()) {
			if (group.getName().equals(groupName)) {
				return true;
			}
		}
		return false;
	}

	private void checkGroupName(String groupName) {
		if (StringUtils.isEmptyOrNull(groupName)
				|| !groupName.equals(groupName.trim())) {
			throw new IllegalArgumentException(
					UIText.RepositoriesView_RepoGroup_InvalidName);
		}
	}

	/**
	 * adds repositories to the given group and removes them from all other
	 * groups
	 *
	 * @param groupId
	 *            id of group to which the repositories are added
	 * @param repoDirs
	 *            repository directories to be added to the group
	 *
	 */
	public void addRepositoriesToGroup(UUID groupId, List<String> repoDirs) {
		if (!groupMap.containsKey(groupId)) {
			throw new IllegalArgumentException();
		}
		Collection<RepositoryGroup> currentGroups = groupMap.values();
		for (RepositoryGroup groups : currentGroups) {
			groups.removeRepositoryDirectories(repoDirs);
		}

		groupMap.get(groupId).addRepositoryDirectories(repoDirs);
		savePreferences();

	}

	/**
	 * deletes repository groups, the repositories belonging to these groups are
	 * not affected
	 *
	 * @param groupsToDelete
	 *            groups to be deleted
	 */
	public void delete(Collection<RepositoryGroup> groupsToDelete) {
		for (RepositoryGroup group : groupsToDelete) {
			preferences
					.remove(PREFS_GROUP_PREFIX + group.getGroupId().toString());
			groupMap.remove(group.getGroupId());
		}
		savePreferences();
	}

	private void savePreferences() {
		try {
			List<String> groupIds = new ArrayList<>();
			List<String> hideableGroupIds = new ArrayList<>();
			for (RepositoryGroup group : groupMap.values()) {
				String groupId = group.getGroupId().toString();
				groupIds.add(groupId);
				String name = group.getName();
				preferences.put(PREFS_GROUP_NAME_PREFIX + groupId, name);
				List<String> repos = group.getRepositoryDirectories();
				preferences.put(PREFS_GROUP_PREFIX + groupId,
						StringUtils.join(repos, SEPARATOR));
				if (group.isHideable()) {
					hideableGroupIds.add(groupId);
				}
			}
			preferences.put(PREFS_GROUPS,
					StringUtils.join(groupIds, SEPARATOR));
			preferences.put(PREFS_HIDEABLE_GROUPS,
					StringUtils.join(hideableGroupIds, SEPARATOR));
			preferences.flush();
		} catch (BackingStoreException e) {
			Activator.logError(
					UIText.RepositoriesView_RepoGroup_ErrorSavePreferences, e);
		}
	}

	/**
	 * @return existing repository groups
	 */
	public List<RepositoryGroup> getGroups() {
		return new ArrayList<>(groupMap.values());
	}

	/**
	 * @param repositoryDirecptory
	 *            directory of the Repository
	 * @return whether the repository belongs to any group
	 */
	public boolean belongsToGroup(String repositoryDirecptory) {
		for (RepositoryGroup group : groupMap.values()) {
			if (group.getRepositoryDirectories()
					.contains(repositoryDirecptory)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param repositoryDirecptories
	 *            repository directories to be removed from all groups
	 */
	public void removeFromGroups(List<String> repositoryDirecptories) {
		for (RepositoryGroup group : groupMap.values()) {
			group.removeRepositoryDirectories(repositoryDirecptories);
		}
		savePreferences();
	}

	/**
	 * @param groups
	 *            the repository groups to be marked as (not) hideable
	 * @param hideable
	 *            new hideable state of the groups
	 */
	public void setHideable(Collection<RepositoryGroup> groups,
			boolean hideable) {
		for (RepositoryGroup group : groups) {
			groupMap.get(group.getGroupId()).setHideable(hideable);
		}
		savePreferences();
	}

}