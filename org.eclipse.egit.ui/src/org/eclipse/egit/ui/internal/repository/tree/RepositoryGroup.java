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

}
