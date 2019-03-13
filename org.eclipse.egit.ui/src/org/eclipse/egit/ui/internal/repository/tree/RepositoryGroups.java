package org.eclipse.egit.ui.internal.repository.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jgit.util.StringUtils;
import org.osgi.service.prefs.BackingStoreException;

/***/
public class RepositoryGroups {

	private Map<String, List<String>> groupToRepositoriesMap = new HashMap<>();

	private IEclipsePreferences preferences = Activator.getDefault()
			.getRepositoryUtil().getPreferences();

	private static final String PREFS_GROUPS = "GitRepositoriesView.RepositoryGroups.names"; //$NON-NLS-1$

	private static final String PREFS_GROUP_PREFIX = "GitRepositoriesView.RepositoryGroups.group."; //$NON-NLS-1$

	/**
	 * new repository groups initialized from preferences
	 */
	public RepositoryGroups() {
		List<String> groups = split(preferences.get(PREFS_GROUPS, "")); //$NON-NLS-1$
		for (String group : groups) {
			List<String> repos = split(
					preferences.get(PREFS_GROUP_PREFIX + group, ""));//$NON-NLS-1$
			groupToRepositoriesMap.put(group, repos);
		}
	}

	private List<String> split(String input) {
		List<String> result=new ArrayList<>();
		String[] split = input.split("\n");//$NON-NLS-1$
		for (String string : split) {
			if(string != null && string.trim().length()>0) {
				result.add(string.trim());
			}
		}
		return result;
	}

	/**
	 * @param groupName
	 */
	public void addGroup(String groupName) {
		if (!StringUtils.isEmptyOrNull(groupName)
				&& !groupToRepositoriesMap.containsKey(groupName)) {
			groupToRepositoriesMap.put(groupName.trim(),
					new ArrayList<String>());
			preferences.put(PREFS_GROUP_PREFIX + groupName.trim(), "");//$NON-NLS-1$
			savePreferences();
		}
	}

	/**
	 * @param group
	 * @param repoDirs
	 */
	public void addRepositoriesToGroup(String group, List<String> repoDirs) {
		List<String> currentGroupRepos = groupToRepositoriesMap.get(group);
		if (currentGroupRepos == null) {
			throw new IllegalArgumentException();
		} else {
			currentGroupRepos.addAll(repoDirs);
		}
		preferences.put(PREFS_GROUP_PREFIX + group,
				StringUtils.join(currentGroupRepos, "\n"));//$NON-NLS-1$
		savePreferences();

	}

	/**
	 * @param groupsToDelete
	 */
	public void delete(List<String> groupsToDelete) {
		for (String group : groupsToDelete) {
			preferences.remove(PREFS_GROUP_PREFIX + group);
			groupToRepositoriesMap.remove(group);
		}
		savePreferences();
	}

	private void savePreferences() {
		try {
			preferences.put(PREFS_GROUPS,
					StringUtils.join(groupToRepositoriesMap.keySet(), "\n"));//$NON-NLS-1$
			preferences.flush();
		} catch (BackingStoreException e) {
			Activator.error("error saving repository group state", e);//$NON-NLS-1$
		}
	}

	/**
	 * @return group names
	 */
	public List<String> getGroupNames() {
		return new ArrayList<>(groupToRepositoriesMap.keySet());
	}

	/**
	 * @param groupName
	 * @return repos belonging to group
	 */
	public List<String> getRepositories(String groupName) {
		if (groupToRepositoriesMap.containsKey(groupName)) {
			return groupToRepositoriesMap.get(groupName);
		} else {
			throw new IllegalArgumentException(
					"unkown repository group " + groupName); //$NON-NLS-1$
		}
	}

	/**
	 * @param repositoryName
	 * @return whether the repository belongs to any group
	 */
	public boolean belongsToGroup(String repositoryName) {
		for (List<String> groupRepos : groupToRepositoriesMap.values()) {
			if (groupRepos.contains(repositoryName)) {
				return true;
			}
		}
		return false;
	}
}