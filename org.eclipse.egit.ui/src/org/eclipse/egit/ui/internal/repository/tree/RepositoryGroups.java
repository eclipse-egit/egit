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
import java.util.Optional;
import java.util.UUID;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.egit.ui.Activator;
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
		for (String groupUUIDString : groups) {
			UUID groupUUIDuuid = UUID.fromString(groupUUIDString);
			String name = preferences
					.get(PREFS_GROUP_NAME_PREFIX + groupUUIDString,
							EMPTY_STRING);
			List<String> repos = split(
					preferences.get(PREFS_GROUP_PREFIX + groupUUIDString,
							EMPTY_STRING));
			RepositoryGroup group = new RepositoryGroup(groupUUIDuuid, name,
					repos);
			groupMap.put(groupUUIDuuid, group);
		}
	}

	private List<String> split(String input) {
		List<String> result=new ArrayList<>();
		String[] split = input.split(SEPARATOR);
		for (String string : split) {
			if(string != null && string.trim().length()>0) {
				result.add(string.trim());
			}
		}
		return result;
	}

	/**
	 * @param groupName
	 * @return UUID of the new group
	 */
	public Optional<UUID> addGroup(String groupName) {
		if (!groupExists(groupName)) {
			String trimmedName = groupName.trim();
			UUID groupUUIDuuid = UUID.randomUUID();
			RepositoryGroup group = new RepositoryGroup(groupUUIDuuid,
					trimmedName);
			groupMap.put(groupUUIDuuid, group);
			savePreferences();
			return Optional.of(groupUUIDuuid);
		}
		return Optional.empty();
	}

	/**
	 * @param group
	 *            the group to rename
	 * @param newName
	 *            new name of the group
	 */
	public void renameGroup(RepositoryGroup group, String newName) {
		RepositoryGroup myGroup = groupMap.get(group.getUuid());
		if (myGroup != null) {
			myGroup.setName(newName);
			savePreferences();
		}
	}

	/**
	 * @param groupName Name of the group
	 * @return true if and only if a group of the given name already exists
	 */
	public boolean groupExists(String groupName) {
		if (groupName != null && !StringUtils.isEmptyOrNull(groupName)) {
			String trimmedName = groupName.trim();
			for (RepositoryGroup group : groupMap.values()) {
				if (group.getName().equals(trimmedName)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @param groupUUID
	 * @param repoDirs
	 *
	 *            Adds repositories to the given Group and removes them from all
	 *            other groups
	 */
	public void addRepositoriesToGroup(UUID groupUUID, List<String> repoDirs) {
		if (!groupMap.containsKey(groupUUID)) {
			throw new IllegalArgumentException();
		}
		Collection<RepositoryGroup> currentGroups = groupMap
				.values();
		for (RepositoryGroup groups : currentGroups) {
			groups.removeRepositories(repoDirs);
		}

		groupMap.get(groupUUID).addRepositories(repoDirs);
		// preferences.put(PREFS_GROUP_PREFIX + group,
		// StringUtils.join(currentGroupRepos, "\n"));//$NON-NLS-1$
		savePreferences();

	}

	/**
	 * @param groupsToDelete
	 */
	public void delete(Collection<RepositoryGroup> groupsToDelete) {
		for (RepositoryGroup groupUUID : groupsToDelete) {
			preferences.remove(
					PREFS_GROUP_PREFIX + groupUUID.getUuid().toString());
			groupMap.remove(groupUUID.getUuid());
		}
		savePreferences();
	}

	private void savePreferences() {
		try {
			List<String> groupUUIDS = new ArrayList<>();
			for (RepositoryGroup group : groupMap.values()) {
				UUID uuid = group.getUuid();
				String uuidString = uuid.toString();
				groupUUIDS.add(uuidString);
				String name = group.getName();
				preferences.put(PREFS_GROUP_NAME_PREFIX + uuidString, name);
				List<String> repos = group.getRepositories();
				preferences.put(PREFS_GROUP_PREFIX + uuidString,
						StringUtils.join(repos, SEPARATOR));
			}
			preferences.put(PREFS_GROUPS,
					StringUtils.join(groupUUIDS, SEPARATOR));
			preferences.flush();
		} catch (BackingStoreException e) {
			Activator.error("error saving repository group state", e);//$NON-NLS-1$
		}
	}

	/**
	 * @return groups
	 */
	public List<RepositoryGroup> getGroups() {
		return new ArrayList<>(groupMap.values());
	}

	// /**
	// * @param groupUUID
	// * @return repos belonging to group
	// */
	// public List<String> getRepositories(UUID groupUUID) {
	// if (groupToRepositoriesMap.containsKey(groupUUID)) {
	// return groupToRepositoriesMap.get(groupUUID);
	// } else {
	// throw new IllegalArgumentException("unkown repository group");
	// //$NON-NLS-1$
	// }
	// }

	/**
	 * @param repositoryName
	 * @return whether the repository belongs to any group
	 */
	public boolean belongsToGroup(String repositoryName) {
		for (RepositoryGroup group : groupMap.values()) {
			if (group.getRepositories().contains(repositoryName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param reposToRemove
	 *            repositories to be removed from all groups
	 */
	public void removeFromGroups(List<String> reposToRemove) {
		for (RepositoryGroup group : groupMap.values()) {
			group.removeRepositories(reposToRemove);
		}
		savePreferences();
	}
}