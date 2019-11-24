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
package org.eclipse.egit.ui.internal.groups;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.util.StringUtils;
import org.osgi.service.prefs.BackingStoreException;

/**
 * This singleton manages the repository groups. The data is stored in the
 * preferences.
 */
public final class RepositoryGroups {

	private static final RepositoryGroups INSTANCE = new RepositoryGroups();

	private static final String PREFS_GROUP_NAME_PREFIX = "RepositoryGroups."; //$NON-NLS-1$

	private static final String PREFS_GROUPS = PREFS_GROUP_NAME_PREFIX
			+ "uuids"; //$NON-NLS-1$

	private static final String PREFS_GROUP_PREFIX = PREFS_GROUP_NAME_PREFIX
			+ "group."; //$NON-NLS-1$

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private static final String SEPARATOR = "\n";//$NON-NLS-1$

	private final Map<UUID, RepositoryGroup> groupMap = new HashMap<>();

	private final RepositoryUtil util = Activator.getDefault()
			.getRepositoryUtil();

	private final IEclipsePreferences preferences = util.getPreferences();

	/**
	 * @return singleton of the repository group manager
	 */
	public static RepositoryGroups getInstance() {
		return INSTANCE;
	}
	/**
	 * new repository groups initialized from preferences
	 */
	private RepositoryGroups() {
		List<String> groups = split(
				preferences.get(PREFS_GROUPS, EMPTY_STRING));
		List<RepositoryGroup> toDelete = new ArrayList<>();
		for (String groupIdString : groups) {
			UUID groupId = UUID.fromString(groupIdString);
			String name = preferences
					.get(PREFS_GROUP_NAME_PREFIX + groupIdString, EMPTY_STRING);
			// Guard against corrupted preferences
			if (isGroupNameInvalid(name)) {
				toDelete.add(new RepositoryGroup(groupId, name));
				Activator.logWarning(MessageFormat.format(
						UIText.RepositoryGroups_LoadPreferencesInvalidName,
						name), null);
				continue;
			}
			List<File> repos = split(preferences
					.get(PREFS_GROUP_PREFIX + groupIdString, EMPTY_STRING))
							.stream().map(util::getAbsoluteRepositoryPath)
							.map(File::new).filter(File::isDirectory)
							.collect(Collectors.toList());
			RepositoryGroup group = new RepositoryGroup(groupId, name, repos);
			groupMap.put(groupId, group);
		}
		if (!toDelete.isEmpty()) {
			delete(toDelete);
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
	 * Determines whether there are any repository groups.
	 *
	 * @return {@code true} if there are repository groups, {@code false}
	 *         otherwise
	 */
	public boolean hasGroups() {
		return !groupMap.isEmpty();
	}

	/**
	 * Creates a new group with the given name.
	 *
	 * @param groupName
	 *            valid name of the new group
	 * @return the new group
	 * @throws IllegalArgumentException
	 *             if the name is invalid
	 * @throws IllegalStateException
	 *             if a group with the given name already exists
	 */
	public RepositoryGroup createGroup(String groupName) {
		checkGroupName(groupName);
		if (!groupExists(groupName)) {
			UUID groupId = UUID.randomUUID();
			RepositoryGroup group = new RepositoryGroup(groupId, groupName);
			groupMap.put(groupId, group);
			savePreferences();
			return group;
		} else {
			throw new IllegalStateException(
					MessageFormat.format(
							UIText.RepositoryGroups_DuplicateGroupNameError,
							groupName));
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
		if (myGroup != null && !newName.equals(myGroup.getName())) {
			myGroup.setGroupName(newName);
			savePreferences();
		}
	}

	/**
	 * @param groupName
	 *            name of the group
	 * @return true if and only if a group of the given name already exists
	 */
	public boolean groupExists(String groupName) {
		return groupMap.values().stream()
				.anyMatch(group -> group.getName().equals(groupName));
	}

	private static boolean isGroupNameInvalid(String groupName) {
		return StringUtils.isEmptyOrNull(groupName)
				|| !groupName.equals(groupName.trim())
				|| Utils.isMultiLine(groupName);
	}

	private static void checkGroupName(String groupName) {
		if (isGroupNameInvalid(groupName)) {
			throw new IllegalArgumentException(
					UIText.RepositoryGroups_InvalidNameError);
		}
	}

	/**
	 * adds repositories to the given group and removes them from all other
	 * groups
	 *
	 * @param group
	 *            to which the repositories are added
	 * @param repoDirs
	 *            repository directories to be added to the group
	 *
	 */
	public void addRepositoriesToGroup(RepositoryGroup group,
			Collection<File> repoDirs) {
		if (!groupMap.containsKey(group.getGroupId())) {
			throw new IllegalArgumentException();
		}
		Collection<RepositoryGroup> currentGroups = groupMap.values();
		for (RepositoryGroup groups : currentGroups) {
			groups.removeRepositoryDirectories(repoDirs);
		}

		group.addRepositoryDirectories(repoDirs);
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
			preferences.remove(PREFS_GROUP_PREFIX + group.getGroupId());
			preferences.remove(PREFS_GROUP_NAME_PREFIX + group.getGroupId());
			groupMap.remove(group.getGroupId());
		}
		savePreferences();
	}

	private void savePreferences() {
		try {
			List<String> groupIds = new ArrayList<>();
			for (RepositoryGroup group : groupMap.values()) {
				String groupId = group.getGroupId().toString();
				groupIds.add(groupId);
				String name = group.getName();
				preferences.put(PREFS_GROUP_NAME_PREFIX + groupId, name);
				String repos = group.getRepositoryDirectories().stream()
						.map(File::toString)
						.map(util::relativizeToWorkspace)
						.collect(Collectors.joining(SEPARATOR));
				preferences.put(PREFS_GROUP_PREFIX + groupId, repos);
			}
			preferences.put(PREFS_GROUPS,
					StringUtils.join(groupIds, SEPARATOR));
			preferences.flush();
		} catch (BackingStoreException e) {
			Activator.logError(
					UIText.RepositoryGroups_SavePreferencesError, e);
		}
	}

	/**
	 * @return existing repository groups
	 */
	public List<RepositoryGroup> getGroups() {
		return new ArrayList<>(groupMap.values());
	}

	/**
	 * @param repositoryDirectory
	 *            directory of the Repository
	 * @return whether the repository belongs to any group
	 */
	public boolean belongsToGroup(File repositoryDirectory) {
		return groupMap.values().stream().anyMatch(group -> group
				.getRepositoryDirectories().contains(repositoryDirectory));
	}

	/**
	 * @param repositoryDirectories
	 *            repository directories to be removed from all groups
	 */
	public void removeFromGroups(List<File> repositoryDirectories) {
		for (RepositoryGroup group : groupMap.values()) {
			group.removeRepositoryDirectories(repositoryDirectories);
		}
		savePreferences();
	}
}