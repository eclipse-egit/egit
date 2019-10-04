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
import java.util.List;
import java.util.UUID;

/**
 * This class holds the information about a group of repositories - identifier,
 * label and list of repositories belonging to the group.
 */
public class RepositoryGroup {

	private UUID uuid;

	private String name;

	private boolean hidden;

	private List<String> repositories = new ArrayList<>();

	RepositoryGroup(UUID uuid, String name) {
		this.uuid = uuid;
		this.name = name;
	}

	RepositoryGroup(UUID uuid, String name, List<String> repositories) {
		this(uuid, name);
		addRepositories(repositories);
	}

	/**
	 * @return UUID of the group
	 */
	public UUID getUuid() {
		return uuid;
	}

	/**
	 * @return Name of the group
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return repositories belonging to the group
	 */
	public List<String> getRepositories() {
		return new ArrayList<>(repositories);
	}

	/**
	 * @return true if and only if there are repositories belongign to the group
	 */
	public boolean hasRepositories() {
		return !repositories.isEmpty();
	}

	void addRepositories(List<String> repositoriesToAdd) {
		this.repositories.addAll(repositoriesToAdd);
	}

	void removeRepositories(List<String> repositoriesToRemove) {
		this.repositories.removeAll(repositoriesToRemove);
	}

	void setName(String newName) {
		this.name = newName;
	}

	void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	/**
	 * @return true if and only if the group is marked as hidden for the
	 *         Repositories View filter
	 */

	public boolean isHidden() {
		return hidden;
	}

}
