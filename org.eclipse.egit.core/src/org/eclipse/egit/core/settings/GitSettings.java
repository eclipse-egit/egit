package org.eclipse.egit.core.settings;

import java.io.File;
import java.util.Collection;
import java.util.Objects;

import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;

/**
 * API to access some core EGit settings.
 *
 * @since 5.9
 */
public final class GitSettings {

	private GitSettings() {
		// No instantiation
	}

	/**
	 * Retrieves the configured connection timeout in seconds.
	 *
	 * @return the configured connection timeout in seconds, 60 by default.
	 */
	public static int getRemoteConnectionTimeout() {
		return Platform.getPreferencesService().getInt(Activator.getPluginId(),
				GitCorePreferences.core_remoteConnectionTimeout, 60, null);
	}

	/**
	 * Retrieves the set of absolute paths to all repositories configured in
	 * EGit.
	 * <p>
	 * Note that there is no guarantee that all the paths returned correspond to
	 * existing directories. Repositories can be deleted outside of EGit.
	 * </p>
	 *
	 * @return a collection of absolute path strings pointing to the git
	 *         directories of repositories configured in EGit.
	 */
	public static Collection<String> getConfiguredRepositoryDirectories() {
		return Activator.getDefault().getRepositoryUtil().getRepositories();
	}

	/**
	 * Adds a repository to the list of repositories configured in EGit.
	 *
	 * @param gitDir
	 *            to add; must not be {@code null}
	 * @throws IllegalArgumentException
	 *             if {@code gitDir} does not "look like" a git repository
	 *             directory
	 */
	public static void addConfiguredRepository(File gitDir)
			throws IllegalArgumentException {
		Activator.getDefault().getRepositoryUtil().addConfiguredRepository(Objects.requireNonNull(gitDir));
	}
}
