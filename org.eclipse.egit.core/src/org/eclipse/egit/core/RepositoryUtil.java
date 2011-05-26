/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.File;
import java.io.IOException;
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.FS;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Utility class for handling Repositories in the UI.
 */
public class RepositoryUtil {

	/** The preferences to store the directories known to the Git Repositories view */
	public static final String PREFS_DIRECTORIES = "GitRepositoriesView.GitDirectories"; //$NON-NLS-1$

	private final Map<String, Map<String, String>> commitMappingCache = new HashMap<String, Map<String, String>>();

	private final Map<String, String> repositoryNameCache = new HashMap<String, String>();

	private final IEclipsePreferences prefs = new InstanceScope()
			.getNode(Activator.getPluginId());

	/**
	 * Clients should obtain an instance from {@link Activator}
	 */
	RepositoryUtil() {
		// nothing
	}

	/**
	 * Used by {@link Activator}
	 */
	void dispose() {
		commitMappingCache.clear();
		repositoryNameCache.clear();
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
	 * are available. Use commiter time stamp from commit if no stamp can be
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

			Map<String, String> cacheEntry = commitMappingCache.get(repository
					.getDirectory().toString());
			if (!refresh && cacheEntry != null
					&& cacheEntry.containsKey(commitId)) {
				// this may be null in fact
				return cacheEntry.get(commitId);
			}
			if (cacheEntry == null) {
				cacheEntry = new HashMap<String, String>();
				commitMappingCache.put(repository.getDirectory().getPath(),
						cacheEntry);
			} else {
				cacheEntry.clear();
			}

			Map<String, Date> tagMap = new HashMap<String, Date>();
			try {
				RevWalk rw = new RevWalk(repository);
				Map<String, Ref> tags = repository.getRefDatabase().getRefs(
						Constants.R_TAGS);
				for (Ref tagRef : tags.values()) {
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
									// not referencing a comit.
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
				Set<String> branchNames = new TreeSet<String>();
				// put this into a sorted set
				try {
					Map<String, Ref> remoteBranches = repository
							.getRefDatabase().getRefs(Constants.R_HEADS);
					for (Ref branch : remoteBranches.values()) {
						if (branch.getObjectId().name().equals(commitId)) {
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
				Set<String> branchNames = new TreeSet<String>();
				// put this into a sorted set
				try {
					Map<String, Ref> remoteBranches = repository
							.getRefDatabase().getRefs(Constants.R_REMOTES);
					for (Ref branch : remoteBranches.values()) {
						if (branch.getObjectId().name().equals(commitId)) {
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
	 * This uses the name of the parent of the repository's directory.
	 *
	 * @param repository
	 * @return the name
	 */
	public String getRepositoryName(Repository repository) {
		synchronized (repositoryNameCache) {
			File gitDir = repository.getDirectory();
			if (gitDir != null) {
				String name = repositoryNameCache.get(gitDir.getPath()
						.toString());
				if (name != null)
					return name;
				name = gitDir.getParentFile().getName();
				repositoryNameCache.put(gitDir.getPath().toString(), name);
				return name;
			}
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * @return the underlying preferences
	 */
	public IEclipsePreferences getPreferences() {
		return prefs;
	}

	/**
	 *
	 * @return the list of configured Repository paths; will be sorted
	 */
	public List<String> getConfiguredRepositories() {
		synchronized (prefs) {
			Set<String> configuredStrings = new HashSet<String>();

			String dirs = prefs.get(PREFS_DIRECTORIES, ""); //$NON-NLS-1$
			if (dirs != null && dirs.length() > 0) {
				StringTokenizer tok = new StringTokenizer(dirs,
						File.pathSeparator);
				while (tok.hasMoreTokens()) {
					String dirName = tok.nextToken();
					configuredStrings.add(dirName);
				}
			}
			List<String> result = new ArrayList<String>();
			result.addAll(configuredStrings);
			Collections.sort(result);
			return result;
		}
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
				throw new IllegalArgumentException();

			String dirString;
			try {
				dirString = repositoryDir.getCanonicalPath();
			} catch (IOException e) {
				dirString = repositoryDir.getAbsolutePath();
			}

			List<String> dirStrings = getConfiguredRepositories();
			if (dirStrings.contains(dirString)) {
				return false;
			} else {
				Set<String> dirs = new HashSet<String>();
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

			String dir;
			try {
				dir = file.getCanonicalPath();
			} catch (IOException e1) {
				dir = file.getAbsolutePath();
			}

			Set<String> dirStrings = new HashSet<String>();
			dirStrings.addAll(getConfiguredRepositories());
			if (dirStrings.remove(dir)) {
				saveDirs(dirStrings);
				return true;
			}
			return false;
		}
	}

	private void saveDirs(Set<String> gitDirStrings) {
		StringBuilder sb = new StringBuilder();
		for (String gitDirString : gitDirStrings) {
			sb.append(gitDirString);
			sb.append(File.pathSeparatorChar);
		}

		prefs.put(PREFS_DIRECTORIES, sb.toString());
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			IStatus error = new Status(IStatus.ERROR, Activator.getPluginId(),
					e.getMessage(), e);
			Activator.getDefault().getLog().log(error);
		}
	}

}
