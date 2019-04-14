/*******************************************************************************
 * Copyright (C) 2011, 2014 Jens Baumgart <jens.baumgart@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.indexdiff;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.IndexDiff;

/**
 * This immutable class is used to store the data of an {@link IndexDiff}
 * object.
 *
 */
public class IndexDiffData {

	private static final String NEW_LINE = "\n"; //$NON-NLS-1$

	private final Set<String> added;

	private final Set<String> assumeUnchanged;

	private final Set<String> changed;

	private final Set<String> removed;

	private final Set<String> missing;

	private final Set<String> modified;

	private final Set<String> untracked;

	private final Set<String> untrackedFolders;

	private final Set<String> conflicts;

	private final Set<String> ignored;

	private final Set<String> symlinks;

	private final Set<String> submodules;

	private final Collection<IResource> changedResources;

	/**
	 * Empty, immutable data
	 */
	public IndexDiffData() {
		added = Collections.emptySet();
		assumeUnchanged = Collections.emptySet();
		changed = Collections.emptySet();
		removed = Collections.emptySet();
		missing = Collections.emptySet();
		modified = Collections.emptySet();
		untracked = Collections.emptySet();
		untrackedFolders = Collections.emptySet();
		conflicts = Collections.emptySet();
		ignored = Collections.emptySet();
		symlinks = Collections.emptySet();
		submodules = Collections.emptySet();
		changedResources = Collections.emptySet();
	}

	/**
	 * @param indexDiff
	 */
	public IndexDiffData(IndexDiff indexDiff) {
		added = Collections.unmodifiableSet(new HashSet<>(indexDiff
				.getAdded()));
		assumeUnchanged = Collections.unmodifiableSet(
				new HashSet<>(indexDiff.getAssumeUnchanged()));
		changed = Collections.unmodifiableSet(new HashSet<>(indexDiff
				.getChanged()));
		removed = Collections.unmodifiableSet(new HashSet<>(indexDiff
				.getRemoved()));
		missing = Collections.unmodifiableSet(new HashSet<>(indexDiff
				.getMissing()));
		modified = Collections.unmodifiableSet(new HashSet<>(indexDiff
				.getModified()));
		untracked = Collections.unmodifiableSet(new HashSet<>(indexDiff
				.getUntracked()));
		untrackedFolders = Collections.unmodifiableSet(getUntrackedFolders(indexDiff));
		conflicts = Collections.unmodifiableSet(new HashSet<>(indexDiff
				.getConflicting()));
		ignored = Collections.unmodifiableSet(new HashSet<>(indexDiff
				.getIgnoredNotInIndex()));
		symlinks = Collections.unmodifiableSet(new HashSet<>(indexDiff
				.getPathsWithIndexMode(FileMode.SYMLINK)));
		submodules = Collections.unmodifiableSet(new HashSet<>(indexDiff
				.getPathsWithIndexMode(FileMode.GITLINK)));
		changedResources = Collections.emptySet();
	}

	private Set<String> getUntrackedFolders(IndexDiff indexDiff) {
		HashSet<String> result = new HashSet<>();
		for (String folder:indexDiff.getUntrackedFolders())
			result.add(folder + "/"); //$NON-NLS-1$
		return result;
	}

	/**
	 * This constructor merges the existing IndexDiffData object baseDiff with a
	 * new IndexDiffData object that was calculated for a subset of files
	 * (changedFiles).
	 *
	 * @param baseDiff
	 * @param changedFiles
	 *            collection of changed files / folders. folders must end with /
	 * @param changedResources
	 * @param diffForChangedFiles
	 */
	public IndexDiffData(IndexDiffData baseDiff,
			Collection<String> changedFiles,
			Collection<IResource> changedResources,
			IndexDiff diffForChangedFiles) {
		this.changedResources = Collections
				.unmodifiableCollection(new HashSet<IResource>(changedResources));
		Set<String> added2 = new HashSet<>(baseDiff.getAdded());
		Set<String> assumeUnchanged2 = new HashSet<>(
				baseDiff.getAssumeUnchanged());
		Set<String> changed2 = new HashSet<>(baseDiff.getChanged());
		Set<String> removed2 = new HashSet<>(baseDiff.getRemoved());
		Set<String> missing2 = new HashSet<>(baseDiff.getMissing());
		Set<String> modified2 = new HashSet<>(baseDiff.getModified());
		Set<String> untracked2 = new HashSet<>(baseDiff.getUntracked());
		Set<String> conflicts2 = new HashSet<>(baseDiff.getConflicting());
		Set<String> symlinks2 = new HashSet<>(baseDiff.getSymlinks());
		Set<String> submodules2 = new HashSet<>(baseDiff.getSubmodules());

		mergeList(added2, changedFiles, diffForChangedFiles.getAdded());
		mergeList(assumeUnchanged2, changedFiles,
				diffForChangedFiles.getAssumeUnchanged());
		mergeList(changed2, changedFiles, diffForChangedFiles.getChanged());
		mergeList(removed2, changedFiles, diffForChangedFiles.getRemoved());
		mergeList(missing2, changedFiles, diffForChangedFiles.getMissing());
		mergeList(modified2, changedFiles, diffForChangedFiles.getModified());
		mergeList(untracked2, changedFiles, diffForChangedFiles.getUntracked());
		mergeList(symlinks2, changedFiles,
				diffForChangedFiles.getPathsWithIndexMode(FileMode.SYMLINK));
		mergeList(submodules2, changedFiles,
				diffForChangedFiles.getPathsWithIndexMode(FileMode.GITLINK));
		Set<String> untrackedFolders2 = mergeUntrackedFolders(
				baseDiff.getUntrackedFolders(), changedFiles,
				getUntrackedFolders(diffForChangedFiles));
		mergeList(conflicts2, changedFiles,
				diffForChangedFiles.getConflicting());
		Set<String> ignored2 = mergeIgnored(baseDiff.getIgnoredNotInIndex(), changedFiles,
				diffForChangedFiles.getIgnoredNotInIndex());

		added = Collections.unmodifiableSet(added2);
		assumeUnchanged = Collections.unmodifiableSet(assumeUnchanged2);
		changed = Collections.unmodifiableSet(changed2);
		removed = Collections.unmodifiableSet(removed2);
		missing = Collections.unmodifiableSet(missing2);
		modified = Collections.unmodifiableSet(modified2);
		untracked = Collections.unmodifiableSet(untracked2);
		untrackedFolders = Collections.unmodifiableSet(untrackedFolders2);
		conflicts = Collections.unmodifiableSet(conflicts2);
		ignored = Collections.unmodifiableSet(ignored2);
		symlinks = Collections.unmodifiableSet(symlinks2);
		submodules = Collections.unmodifiableSet(submodules2);
	}

	private void mergeList(Set<String> baseList,
			Collection<String> changedFiles, Set<String> listForChangedFiles) {
		for (String file : changedFiles) {
			if (baseList.contains(file)) {
				if (!listForChangedFiles.contains(file))
					baseList.remove(file);
			} else {
				if (listForChangedFiles.contains(file))
					baseList.add(file);
			}
		}
	}

	private static Set<String> mergeUntrackedFolders(Set<String> oldUntrackedFolders,
			Collection<String> changedFiles, Set<String> newUntrackedFolders) {
		Set<String> merged = new HashSet<>();
		for (String oldUntrackedFolder : oldUntrackedFolders) {
			boolean changeInUntrackedFolder = isAnyFileContainedInFolder(
					oldUntrackedFolder, changedFiles);
			if (!changeInUntrackedFolder)
				merged.add(oldUntrackedFolder);
		}
		merged.addAll(newUntrackedFolders);
		return merged;
	}

	private static boolean isAnyFileContainedInFolder(String folder,
			Collection<String> files) {
		for (String file : files)
			if (file.startsWith(folder))
				return true;
		return false;
	}

	/**
	 * THIS METHOD IS PROTECTED FOR TESTS ONLY
	 *
	 * @param oldIgnoredPaths
	 * @param changedPaths
	 * @param newIgnoredPaths
	 * @return never null
	 */
	protected static Set<String> mergeIgnored(Set<String> oldIgnoredPaths,
			Collection<String> changedPaths, Set<String> newIgnoredPaths) {
		Set<String> merged = new HashSet<>();
		for (String oldIgnoredPath : oldIgnoredPaths) {
			boolean changed = isAnyPrefixOf(oldIgnoredPath, changedPaths);
			if (!changed) {
				merged.add(oldIgnoredPath);
			}
		}
		merged.addAll(newIgnoredPaths);
		return merged;
	}

	/**
	 * THIS METHOD IS PROTECTED FOR TESTS ONLY
	 *
	 * @param pathToCheck
	 * @param possiblePrefixes
	 * @return true if given path starts with any of given prefixes (possibly
	 *         followed by slash)
	 */
	protected static boolean isAnyPrefixOf(String pathToCheck,
			Collection<String> possiblePrefixes) {
		for (String possiblePrefix : possiblePrefixes) {
			if (pathToCheck.startsWith(possiblePrefix)) {
				return true;
			}
			if (possiblePrefix.length() == pathToCheck.length() + 1
					&& possiblePrefix.charAt(possiblePrefix.length() - 1) == '/'
					&& possiblePrefix.startsWith(pathToCheck)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return list of files added to the index, not in the tree
	 */
	@NonNull
	public Set<String> getAdded() {
		return Collections.unmodifiableSet(added);
	}

	/**
	 * @return list of files with git's "assume unchanged" bit set to true
	 */
	@NonNull
	public Set<String> getAssumeUnchanged() {
		return Collections.unmodifiableSet(assumeUnchanged);
	}

	/**
	 * @return list of files changed from tree to index
	 */
	@NonNull
	public Set<String> getChanged() {
		return changed;
	}

	/**
	 * @return list of files removed from index, but in tree
	 */
	@NonNull
	public Set<String> getRemoved() {
		return removed;
	}

	/**
	 * @return list of files in index, but not filesystem
	 */
	@NonNull
	public Set<String> getMissing() {
		return missing;
	}

	/**
	 * @return list of files modified on disk relative to the index
	 */
	@NonNull
	public Set<String> getModified() {
		return modified;
	}

	/**
	 * @return list of files that are not ignored, and not in the index.
	 */
	@NonNull
	public Set<String> getUntracked() {
		return untracked;
	}

	/**
	 * @return list of folders containing only untracked files/folders
	 * The folder paths end with /
	 */
	@NonNull
	public Set<String> getUntrackedFolders() {
		return untrackedFolders;
	}

	/**
	 * @return list of files that are in conflict
	 */
	@NonNull
	public Set<String> getConflicting() {
		return conflicts;
	}

	/**
	 * @see IndexDiff#getIgnoredNotInIndex()
	 * @return list of files that are ignored
	 */
	@NonNull
	public Set<String> getIgnoredNotInIndex() {
		return ignored;
	}

	/**
	 * @return list of files that are symlinks
	 */
	@NonNull
	public Set<String> getSymlinks() {
		return symlinks;
	}

	/**
	 * @return list of files that are submodules
	 */
	@NonNull
	public Set<String> getSubmodules() {
		return submodules;
	}

	/**
	 * Determines whether this {@link IndexDiffData} does contain any changes.
	 *
	 * @return {@code true} if there are changes; {@code false} otherwise
	 */
	public boolean hasChanges() {
		return !(getAdded().isEmpty() //
				&& getChanged().isEmpty() //
				&& getRemoved().isEmpty() //
				&& getUntracked().isEmpty() //
				&& getModified().isEmpty() //
				&& getMissing().isEmpty());
	}

	/**
	 * @return the changed files
	 */
	@NonNull
	public Collection<IResource> getChangedResources() {
		return changedResources;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		dumpList(builder, "added", added); //$NON-NLS-1$
		dumpList(builder, "assumeUnchanged", assumeUnchanged); //$NON-NLS-1$
		dumpList(builder, "changed", changed); //$NON-NLS-1$
		dumpList(builder, "removed", removed); //$NON-NLS-1$
		dumpList(builder, "missing", missing); //$NON-NLS-1$
		dumpList(builder, "modified", modified); //$NON-NLS-1$
		dumpList(builder, "untracked", untracked); //$NON-NLS-1$
		dumpList(builder, "untrackedFolders", untrackedFolders); //$NON-NLS-1$
		dumpList(builder, "conflicts", conflicts); //$NON-NLS-1$
		dumpList(builder, "ignored", ignored); //$NON-NLS-1$
		dumpList(builder, "symlinks", symlinks); //$NON-NLS-1$
		dumpList(builder, "submodules", submodules); //$NON-NLS-1$
		dumpResourceList(builder,
				"changedResources", changedResources); //$NON-NLS-1$
		return builder.toString();
	}

	private void dumpList(StringBuilder builder, String listName,
			Set<String> list) {
		builder.append(listName);
		builder.append(NEW_LINE);
		for (String entry : list) {
			builder.append(entry);
			builder.append(NEW_LINE);
		}
		builder.append(NEW_LINE);
	}

	private void dumpResourceList(StringBuilder builder, String listName,
			Collection<IResource> list) {
		if (list == null)
			return;
		builder.append(listName);
		builder.append(NEW_LINE);
		for (IResource file : list) {
			builder.append(file.getFullPath().toOSString());
			builder.append(NEW_LINE);
		}
		builder.append(NEW_LINE);
	}

}
