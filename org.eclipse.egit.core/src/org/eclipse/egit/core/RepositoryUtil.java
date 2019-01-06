/*******************************************************************************
 * Copyright (c) 2010, 2015 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.CheckoutEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Utility class for handling Repositories in the UI.
 */
public class RepositoryUtil {

	/**
	 * The preferences to store the absolute paths of all repositories shown in
	 * the Git Repositories view
	 *
	 * @deprecated maintained to ensure compatibility for old EGit versions
	 */
	@Deprecated
	public static final String PREFS_DIRECTORIES = "GitRepositoriesView.GitDirectories"; //$NON-NLS-1$

	/**
	 * The preferences to store paths of all repositories shown in the Git
	 * Repositories view. For repositories located in the Eclipse workspace
	 * store the relative path to the workspace root to enable moving and
	 * copying the workspace. For repositories outside the Eclipse workspace
	 * store their absolute path.
	 */
	public static final String PREFS_DIRECTORIES_REL = "GitRepositoriesView.GitDirectories.relative"; //$NON-NLS-1$

	private final Map<String, Map<String, String>> commitMappingCache = new HashMap<>();

	private final Map<String, String> repositoryNameCache = new HashMap<>();

	private final IEclipsePreferences prefs = InstanceScope.INSTANCE
			.getNode(Activator.getPluginId());

	private final java.nio.file.Path workspacePath;

	/**
	 * Clients should obtain an instance from {@link Activator}
	 */
	RepositoryUtil() {
		workspacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation()
				.toFile().toPath();
	}

	/**
	 * Used by {@link Activator}
	 */
	void dispose() {
		commitMappingCache.clear();
		repositoryNameCache.clear();
	}

	/**
	 * @return The default repository directory as configured in the
	 *         preferences, with variables substituted. Returns workspace
	 *         location if there was an error during substitution.
	 */
	@NonNull
	public static String getDefaultRepositoryDir() {
		String key = GitCorePreferences.core_defaultRepositoryDir;
		String dir = migrateRepoRootPreference();
		IEclipsePreferences p = InstanceScope.INSTANCE
				.getNode(Activator.getPluginId());
		if (dir == null) {
			dir = Platform.getPreferencesService().getString(
					Activator.getPluginId(), key,
					getDefaultDefaultRepositoryDir(), null);
		} else {
			p.put(key, dir);
		}
		IStringVariableManager manager = VariablesPlugin.getDefault()
				.getStringVariableManager();
		String result;
		try {
			result = manager.performStringSubstitution(dir);
		} catch (CoreException e) {
			result = ""; //$NON-NLS-1$
		}
		if (result == null || result.isEmpty()) {
			result = ResourcesPlugin.getWorkspace().getRoot().getRawLocation()
					.toOSString();
		}
		return FileUtils.canonicalize(new File(result)).toString();
	}

	@NonNull
	static String getDefaultDefaultRepositoryDir() {
		return new File(FS.DETECTED.userHome(), "git").getPath(); //$NON-NLS-1$
	}

	/**
	 * Prior to 4.1 the preference was hosted in the UI plugin. So if this one
	 * exists, we remove it from there and return. Otherwise null is returned.
	 *
	 * @return previously existing UI preference or null
	 */
	@Nullable
	private static String migrateRepoRootPreference() {
		IEclipsePreferences p = InstanceScope.INSTANCE
				.getNode("org.eclipse.egit.ui"); //$NON-NLS-1$
		String deprecatedUiKey = "default_repository_dir"; //$NON-NLS-1$
		String value = p.get(deprecatedUiKey, null);
		if (value != null && value.isEmpty()) {
			value = null;
		}
		if (value != null) {
			p.remove(deprecatedUiKey);
		}
		return value;
	}

	/**
	 * Tries to map a commit to a symbolic reference.
	 * <p>
	 * This value will be cached for the given commit ID unless refresh is
	 * specified. The return value will be the full name, e.g.
	 * "refs/remotes/someBranch", "refs/tags/v.1.0"
	 * <p>
	 * Since this mapping is not unique, the following precedence rules are
	 * used:
	 * <ul>
	 * <li>Tags take precedence over branches</li>
	 * <li>Local branches take preference over remote branches</li>
	 * <li>Newer references take precedence over older ones where time stamps
	 * are available. Use committer time stamp from commit if no stamp can be
	 * found on the tag</li>
	 * <li>If there are still ambiguities, the reference name with the highest
	 * lexicographic value will be returned</li>
	 * </ul>
	 *
	 * @param repository
	 *            the {@link Repository}
	 * @param commitId
	 *            a commit
	 * @param refresh
	 *            if true, the cache will be invalidated
	 * @return the symbolic reference, or <code>null</code> if no such reference
	 *         can be found
	 */
	public String mapCommitToRef(Repository repository, String commitId,
			boolean refresh) {
		synchronized (commitMappingCache) {

			if (!ObjectId.isId(commitId)) {
				return null;
			}

			try {
				ReflogReader reflogReader = repository.getReflogReader(Constants.HEAD);
				if (reflogReader != null) {
					List<ReflogEntry> lastEntry = reflogReader.getReverseEntries();
					for (ReflogEntry entry : lastEntry) {
						if (entry.getNewId().name().equals(commitId)) {
							CheckoutEntry checkoutEntry = entry.parseCheckout();
							if (checkoutEntry != null) {
								Ref ref = repository
										.findRef(checkoutEntry.getToBranch());
								if (ref != null) {
									ObjectId objectId = ref.getObjectId();
									if (objectId != null && objectId.getName()
											.equals(commitId)) {
										return checkoutEntry.getToBranch();
									}
									ref = repository.peel(ref);
								}
								if (ref != null) {
									ObjectId id = ref.getPeeledObjectId();
									if (id != null && id.getName().equals(commitId)) {
										return checkoutEntry.getToBranch();
									}
								}
							}
						}
					}
				}
			} catch (IOException e) {
				// ignore here
			}

			Map<String, String> cacheEntry = commitMappingCache.get(repository
					.getDirectory().toString());
			if (!refresh && cacheEntry != null
					&& cacheEntry.containsKey(commitId)) {
				// this may be null in fact
				return cacheEntry.get(commitId);
			}
			if (cacheEntry == null) {
				cacheEntry = new HashMap<>();
				commitMappingCache.put(repository.getDirectory().getPath(),
						cacheEntry);
			} else {
				cacheEntry.clear();
			}

			Map<String, Date> tagMap = new HashMap<>();
			try (RevWalk rw = new RevWalk(repository)) {
				List<Ref> tags = repository.getRefDatabase().getRefsByPrefix(
						Constants.R_TAGS);
				for (Ref tagRef : tags) {
					RevObject any = rw.parseAny(repository.resolve(tagRef.getName()));
					if (any instanceof RevTag) {
						RevTag tag = (RevTag) any;
						if (tag.getObject().name().equals(commitId)) {
							Date timestamp;
							if (tag.getTaggerIdent() != null) {
								timestamp = tag.getTaggerIdent().getWhen();
							} else {
								try {
									RevCommit commit = rw.parseCommit(tag.getObject());
									timestamp = commit.getCommitterIdent().getWhen();
								} catch (IncorrectObjectTypeException e) {
									// not referencing a commit
									timestamp = null;
								}
							}
							tagMap.put(tagRef.getName(), timestamp);
						}
					} else if (any instanceof RevCommit) {
						RevCommit commit = ((RevCommit)any);
						if (commit.name().equals(commitId))
							tagMap.put(tagRef.getName(), commit.getCommitterIdent().getWhen());
					} // else ignore here
				}
			} catch (IOException e) {
				// ignore here
			}

			String cacheValue = null;

			if (!tagMap.isEmpty()) {
				// we try to obtain the "latest" tag
				Date compareDate = new Date(0);
				for (Map.Entry<String, Date> tagEntry : tagMap.entrySet()) {
					if (tagEntry.getValue() != null
							&& tagEntry.getValue().after(compareDate)) {
						compareDate = tagEntry.getValue();
						cacheValue = tagEntry.getKey();
					}
				}
				// if we don't have time stamps, we sort
				if (cacheValue == null) {
					String compareString = ""; //$NON-NLS-1$
					for (String tagName : tagMap.keySet()) {
						if (tagName.compareTo(compareString) >= 0) {
							cacheValue = tagName;
							compareString = tagName;
						}
					}
				}
			}

			if (cacheValue == null) {
				// we didnt't find a tag, so let's look for local branches
				Set<String> branchNames = new TreeSet<>();
				// put this into a sorted set
				try {
					List<Ref> remoteBranches = repository.getRefDatabase()
							.getRefsByPrefix(Constants.R_HEADS);
					for (Ref branch : remoteBranches) {
						ObjectId objectId = branch.getObjectId();
						if (objectId != null
								&& objectId.name().equals(commitId)) {
							branchNames.add(branch.getName());
						}
					}
				} catch (IOException e) {
					// ignore here
				}
				if (!branchNames.isEmpty()) {
					// get the last (sorted) entry
					cacheValue = branchNames.toArray(new String[branchNames
							.size()])[branchNames.size() - 1];
				}
			}

			if (cacheValue == null) {
				// last try: remote branches
				Set<String> branchNames = new TreeSet<>();
				// put this into a sorted set
				try {
					List<Ref> remoteBranches = repository.getRefDatabase()
							.getRefsByPrefix(Constants.R_REMOTES);
					for (Ref branch : remoteBranches) {
						ObjectId objectId = branch.getObjectId();
						if (objectId != null
								&& objectId.name().equals(commitId)) {
							branchNames.add(branch.getName());
						}
					}
					if (!branchNames.isEmpty()) {
						// get the last (sorted) entry
						cacheValue = branchNames.toArray(new String[branchNames
								.size()])[branchNames.size() - 1];
					}
				} catch (IOException e) {
					// ignore here
				}
			}
			cacheEntry.put(commitId, cacheValue);
			return cacheValue;
		}
	}

	/**
	 * Return a cached UI "name" for a Repository
	 * <p>
	 * This uses the name of the working directory. In case of a bare
	 * repository, the repository directory name is used.
	 *
	 * @param repository
	 * @return the name
	 */
	public String getRepositoryName(final Repository repository) {
		File dir;
		// Use working directory name for non-bare repositories
		if (!repository.isBare())
			dir = repository.getWorkTree();
		else
			dir = repository.getDirectory();

		if (dir == null)
			return ""; //$NON-NLS-1$

		synchronized (repositoryNameCache) {
			final String path = dir.getPath();
			String name = repositoryNameCache.get(path);
			if (name != null)
				return name;
			name = dir.getName();
			repositoryNameCache.put(path, name);
			return name;
		}
	}

	/**
	 * @return the underlying preferences
	 */
	public IEclipsePreferences getPreferences() {
		return prefs;
	}

	/**
	 * Get the set of absolute path strings of all configured repositories.
	 *
	 * @return set of absolute paths of all configured repositories' .git
	 *         directories
	 *
	 * @since 4.2
	 */
	@NonNull
	public Set<String> getRepositories() {
		String dirString;
		Set<String> dirs;
		synchronized (prefs) {
			dirString = prefs.get(PREFS_DIRECTORIES_REL, ""); //$NON-NLS-1$
			if (dirString.equals("")) { //$NON-NLS-1$
				dirs = migrateAbsolutePaths();
			} else {
				dirs = toDirSet(dirString);
			}
		}
		return dirs;
	}

	/**
	 * Migrate set of absolute paths created by an older version of EGit to the
	 * new format using relative paths for repositories located under the
	 * Eclipse workspace
	 *
	 * @return set of absolute paths of all configured git repositories
	 */
	private Set<String> migrateAbsolutePaths() {
		String dirString;
		Set<String> dirs;
		dirString = prefs.get(PREFS_DIRECTORIES, ""); //$NON-NLS-1$
		dirs = toDirSet(dirString);
		// save migrated list
		saveDirs(dirs);
		return dirs;
	}

	/**
	 * @param dirs
	 *            String with repository directories separated by path separator
	 * @return set of absolute paths of repository directories, relative paths
	 *         are resolved against the workspace root
	 */
	private Set<String> toDirSet(String dirs) {
		if (dirs == null || dirs.isEmpty()) {
			return Collections.emptySet();
		}
		Set<String> configuredStrings = new HashSet<>();
		StringTokenizer tok = new StringTokenizer(dirs, File.pathSeparator);
		while (tok.hasMoreTokens()) {
			configuredStrings
					.add(workspacePath.resolve(tok.nextToken()).toString());
		}
		return configuredStrings;
	}

	/**
	 *
	 * @return the list of configured Repository paths; will be sorted
	 */
	public List<String> getConfiguredRepositories() {
		final List<String> repos = new ArrayList<>(getRepositories());
		Collections.sort(repos);
		return repos;
	}

	/**
	 *
	 * @param repositoryDir
	 *            the Repository path
	 * @return <code>true</code> if the repository path was not yet configured
	 * @throws IllegalArgumentException
	 *             if the path does not "look" like a Repository
	 */
	public boolean addConfiguredRepository(File repositoryDir)
			throws IllegalArgumentException {
		synchronized (prefs) {

			if (!FileKey.isGitRepository(repositoryDir, FS.DETECTED))
				throw new IllegalArgumentException(MessageFormat.format(
						CoreText.RepositoryUtil_DirectoryIsNotGitDirectory,
						repositoryDir));

			String dirString = repositoryDir.getAbsolutePath();

			List<String> dirStrings = getConfiguredRepositories();
			if (dirStrings.contains(dirString)) {
				return false;
			} else {
				Set<String> dirs = new HashSet<>();
				dirs.addAll(dirStrings);
				dirs.add(dirString);
				saveDirs(dirs);
				return true;
			}
		}
	}

	/**
	 * @param file
	 * @return <code>true</code> if the configuration was changed by the remove
	 */
	public boolean removeDir(File file) {
		synchronized (prefs) {
			String dirString = file.getAbsolutePath();
			Set<String> dirStrings = new HashSet<>();
			dirStrings.addAll(getConfiguredRepositories());
			if (dirStrings.remove(dirString)) {
				saveDirs(dirStrings);
				return true;
			}
			return false;
		}
	}

	private void saveDirs(Set<String> gitDirStrings) {
		StringBuilder sbRelative = new StringBuilder();
		StringBuilder sbAbsolute = new StringBuilder();
		for (String gitDirString : gitDirStrings) {
			sbRelative.append(relativizeToWorkspace(gitDirString));
			sbRelative.append(File.pathSeparatorChar);
			sbAbsolute.append(gitDirString);
			sbAbsolute.append(File.pathSeparatorChar);
		}

		// redundantly store absolute paths to ensure compatibility with older
		// EGit versions
		prefs.put(PREFS_DIRECTORIES, sbAbsolute.toString());
		prefs.put(PREFS_DIRECTORIES_REL, sbRelative.toString());
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			IStatus error = new Status(IStatus.ERROR, Activator.getPluginId(),
					e.getMessage(), e);
			Activator.getDefault().getLog().log(error);
		}
	}

	/**
	 * @param pathString
	 *            an absolute path String
	 * @return if the given {@code pathString} is under the workspace root the
	 *         relative path of {@code pathString} relative to the workspace
	 *         root, otherwise the absolute path {@code pathString}. This
	 *         enables moving or copying the workspace.
	 */
	private String relativizeToWorkspace(String pathString) {
		java.nio.file.Path p = java.nio.file.Paths.get(pathString);
		if (p.startsWith(workspacePath)) {
			return workspacePath.relativize(p).toString();
		} else {
			return pathString;
		}
	}

	/**
	 * Does the collection of repository returned by
	 * {@link #getConfiguredRepositories()} contain the given repository?
	 *
	 * @param repository
	 * @return true if contains repository, false otherwise
	 */
	public boolean contains(final Repository repository) {
		return contains(repository.getDirectory().getAbsolutePath());
	}

	/**
	 * Does the collection of repository returned by
	 * {@link #getConfiguredRepositories()} contain the given repository
	 * directory?
	 *
	 * @param repositoryDir
	 * @return true if contains repository directory, false otherwise
	 */
	public boolean contains(final String repositoryDir) {
		return getRepositories().contains(repositoryDir);
	}

	/**
	 * Get short branch text for given repository
	 *
	 * @param repository
	 * @return short branch text
	 * @throws IOException
	 */
	public String getShortBranch(Repository repository) throws IOException {
		Ref head = repository.exactRef(Constants.HEAD);
		if (head == null) {
			return CoreText.RepositoryUtil_noHead;
		}
		if (head.isSymbolic()) {
			return repository.getBranch();
		}
		ObjectId objectId = head.getObjectId();
		if (objectId == null) {
			return CoreText.RepositoryUtil_noHead;
		}
		String id = objectId.name();
		String ref = mapCommitToRef(repository, id, false);
		if (ref != null) {
			return Repository.shortenRefName(ref) + ' ' + id.substring(0, 7);
		} else {
			return id.substring(0, 7);
		}
	}

	/**
	 * Resolve HEAD and parse the commit. Returns null if HEAD does not exist or
	 * could not be parsed.
	 * <p>
	 * Only use this if you don't already have to work with a RevWalk.
	 *
	 * @param repository
	 * @return the commit or null if HEAD does not exist or could not be parsed.
	 * @since 2.2
	 */
	public RevCommit parseHeadCommit(Repository repository) {
		try (RevWalk walk = new RevWalk(repository)) {
			Ref head = repository.exactRef(Constants.HEAD);
			if (head == null || head.getObjectId() == null)
				return null;

			RevCommit commit = walk.parseCommit(head.getObjectId());
			return commit;
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Checks if existing resource with given path is to be ignored.
	 * <p>
	 * <b>Note:</b>The check makes sense only for files which exists in the
	 * working directory. This method returns false for paths to not existing
	 * files or directories.
	 *
	 * @param path
	 *            Path to be checked, file or directory must exist on the disk
	 * @return true if the path is either not inside git repository or exists
	 *         and matches an ignore rule
	 * @throws IOException
	 * @since 2.3
	 */
	public static boolean isIgnored(IPath path) throws IOException {
		RepositoryMapping mapping = RepositoryMapping.getMapping(path);
		if (mapping == null) {
			return true; // Linked resources may not be mapped
		}
		Repository repository = mapping.getRepository();
		WorkingTreeIterator treeIterator = IteratorService
				.createInitialIterator(repository);
		if (treeIterator == null) {
			return true;
		}
		String repoRelativePath = mapping.getRepoRelativePath(path);
		if (repoRelativePath == null || repoRelativePath.isEmpty()) {
			return true;
		}
		try (TreeWalk walk = new TreeWalk(repository)) {
			walk.addTree(treeIterator);
			walk.setFilter(PathFilterGroup.createFromStrings(repoRelativePath));
			while (walk.next()) {
				WorkingTreeIterator workingTreeIterator = walk.getTree(0,
						WorkingTreeIterator.class);
				if (walk.getPathString().equals(repoRelativePath)) {
					return workingTreeIterator.isEntryIgnored();
				}
				if (workingTreeIterator.getEntryFileMode()
						.equals(FileMode.TREE)) {
					walk.enterSubtree();
				}
			}
		}
		return false;
	}

	/**
	 * Checks if the existing resource with given path can be automatically
	 * added to the .gitignore file.
	 *
	 * @param path
	 *            Path to be checked, file or directory must exist on the disk
	 * @return true if the file or directory at given path exists, is inside
	 *         known git repository and does not match any existing ignore rule,
	 *         false otherwise
	 * @throws IOException
	 * @since 4.1.0
	 */
	public static boolean canBeAutoIgnored(IPath path) throws IOException {
		Repository repository = Activator.getDefault().getRepositoryCache()
				.getRepository(path);
		if (repository == null || repository.isBare()) {
			return false;
		}
		WorkingTreeIterator treeIterator = IteratorService
				.createInitialIterator(repository);
		if (treeIterator == null) {
			return false;
		}
		String repoRelativePath = path
				.makeRelativeTo(
						new Path(repository.getWorkTree().getAbsolutePath()))
				.toString();
		if (repoRelativePath.length() == 0
				|| repoRelativePath.equals(path.toString())) {
			return false;
		}
		try (TreeWalk walk = new TreeWalk(repository)) {
			walk.addTree(treeIterator);
			walk.setFilter(PathFilterGroup.createFromStrings(repoRelativePath));
			while (walk.next()) {
				WorkingTreeIterator workingTreeIterator = walk.getTree(0,
						WorkingTreeIterator.class);
				if (walk.getPathString().equals(repoRelativePath)) {
					return !workingTreeIterator.isEntryIgnored();
				}
				if (workingTreeIterator.getEntryFileMode()
						.equals(FileMode.TREE)) {
					walk.enterSubtree();
				}
			}
		}
		// path not found in tree, we should not automatically ignore it
		return false;
	}

	/**
	 * Checks if given repository is in the 'detached HEAD' state.
	 *
	 * @param repository
	 *            the repository to check
	 * @return <code>true</code> if the repository is in the 'detached HEAD'
	 *         state, <code>false</code> if it's not or an error occurred
	 * @since 3.2
	 */
	public static boolean isDetachedHead(Repository repository) {
		try {
			return ObjectId.isId(repository.getFullBranch());
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}
		return false;
	}

	/**
	 * Determines whether the given {@link Repository} has any changes by
	 * checking the {@link IndexDiffCacheEntry} of the repository.
	 *
	 * @param repository
	 *            to check
	 * @return {@code true} if the repository has any changes, {@code false}
	 *         otherwise
	 */
	public static boolean hasChanges(@NonNull Repository repository) {
		IndexDiffCacheEntry entry = Activator.getDefault().getIndexDiffCache()
				.getIndexDiffCacheEntry(repository);
		IndexDiffData data = entry != null ? entry.getIndexDiff() : null;
		return data != null && data.hasChanges();
	}
}
