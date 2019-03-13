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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * This class holds the information about a group of repositories - identifier,
 * group name and list of repository directories belonging to the group.
 */
public class RepositoryGroup {

	private UUID groupId;

	private String groupName;

	private List<File> repositoryDirectories = new ArrayList<>();

	RepositoryGroup(UUID groupId, String groupName) {
		this.groupId = groupId;
		this.groupName = groupName;
	}

	RepositoryGroup(UUID uuid, String name,
			Collection<File> repositoryDirectories) {
		this(uuid, name);
		addRepositoryDirectories(repositoryDirectories);
	}

	/**
	 * @return id of the group (UUID)
	 */
	public UUID getGroupId() {
		return groupId;
	}

	/**
	 * @return name of the group
	 */
	public String getName() {
		return groupName;
	}

	/**
	 * @return repository directories belonging to the group
	 */
	public List<File> getRepositoryDirectories() {
		return new ArrayList<>(repositoryDirectories);
	}

	/**
	 * @return true if and only if there are repositories belonging to the group
	 */
	public boolean hasRepositories() {
		return !repositoryDirectories.isEmpty();
	}

	void addRepositoryDirectories(Collection<File> directoriesToAdd) {
		this.repositoryDirectories.addAll(directoriesToAdd);
	}

	void removeRepositoryDirectories(Collection<File> directoriesToRemove) {
		this.repositoryDirectories.removeAll(directoriesToRemove);
	}

	void setGroupName(String newName) {
		this.groupName = newName;
	}
}
