/*******************************************************************************
 * Copyright (C) 2011, 2012 Jens Baumgart <jens.baumgart@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.indexdiff;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.jgit.lib.IndexDiff;

/**
 * This immutable class is used to store the data of an {@link IndexDiff}
 * object.
 *
 */
public class IndexDiffData {

	private static final String NEW_LINE = "\n"; //$NON-NLS-1$

	private final Set<String> added;

	private final Set<String> changed;

	private final Set<String> removed;

	private final Set<String> missing;

	private final Set<String> modified;

	private final Set<String> untracked;

	private final Set<String> untrackedFolders;

	private final Set<String> conflicts;

	private final Set<String> ignored;

	private final Collection<IResource> changedResources;

	/**
	 * @param indexDiff
	 */
	public IndexDiffData(IndexDiff indexDiff) {
		added = Collections.unmodifiableSet(new HashSet<String>(indexDiff
				.getAdded()));
		changed = Collections.unmodifiableSet(new HashSet<String>(indexDiff
				.getChanged()));
		removed = Collections.unmodifiableSet(new HashSet<String>(indexDiff
				.getRemoved()));
		missing = Collections.unmodifiableSet(new HashSet<String>(indexDiff
				.getMissing()));
		modified = Collections.unmodifiableSet(new HashSet<String>(indexDiff
				.getModified()));
		untracked = Collections.unmodifiableSet(new HashSet<String>(indexDiff
				.getUntracked()));
		untrackedFolders = Collections.unmodifiableSet(getUntrackedFolders(indexDiff));
		conflicts = Collections.unmodifiableSet(new HashSet<String>(indexDiff
				.getConflicting()));
		ignored = Collections.unmodifiableSet(new HashSet<String>(indexDiff
				.getIgnoredNotInIndex()));
		changedResources = null;
	}

	private Set<String> getUntrackedFolders(IndexDiff indexDiff) {
		HashSet<String> result = new HashSet<String>();
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
		Set<String> added2 = new HashSet<String>(baseDiff.getAdded());
		Set<String> changed2 = new HashSet<String>(baseDiff.getChanged());
		Set<String> removed2 = new HashSet<String>(baseDiff.getRemoved());
		Set<String> missing2 = new HashSet<String>(baseDiff.getMissing());
		Set<String> modified2 = new HashSet<String>(baseDiff.getModified());
		Set<String> untracked2 = new HashSet<String>(baseDiff.getUntracked());
		Set<String> conflicts2 = new HashSet<String>(baseDiff.getConflicting());

		mergeList(added2, changedFiles, diffForChangedFiles.getAdded());
		mergeList(changed2, changedFiles, diffForChangedFiles.getChanged());
		mergeList(removed2, changedFiles, diffForChangedFiles.getRemoved());
		mergeList(missing2, changedFiles, diffForChangedFiles.getMissing());
		mergeList(modified2, changedFiles, diffForChangedFiles.getModified());
		mergeList(untracked2, changedFiles, diffForChangedFiles.getUntracked());
		Set<String> untrackedFolders2 = mergeUntrackedFolders(
				baseDiff.getUntrackedFolders(), changedFiles,
				getUntrackedFolders(diffForChangedFiles));
		mergeList(conflicts2, changedFiles,
				diffForChangedFiles.getConflicting());
		Set<String> ignored2 = mergeIgnored(baseDiff.getIgnoredNotInIndex(), changedFiles,
				diffForChangedFiles.getIgnoredNotInIndex());

		added = Collections.unmodifiableSet(added2);
		changed = Collections.unmodifiableSet(changed2);
		removed = Collections.unmodifiableSet(removed2);
		missing = Collections.unmodifiableSet(missing2);
		modified = Collections.unmodifiableSet(modified2);
		untracked = Collections.unmodifiableSet(untracked2);
		untrackedFolders = Collections.unmodifiableSet(untrackedFolders2);
		conflicts = Collections.unmodifiableSet(conflicts2);
		ignored = Collections.unmodifiableSet(ignored2);
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
		Set<String> merged = new HashSet<String>();
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

	private static Set<String> mergeIgnored(Set<String> oldIgnoredPaths,
			Collection<String> changedPaths, Set<String> newIgnoredPaths) {
		Set<String> merged = new HashSet<String>();
		for (String oldIgnoredPath : oldIgnoredPaths) {
			boolean changed = isAnyPrefixOf(oldIgnoredPath, changedPaths);
			if (!changed)
				merged.add(oldIgnoredPath);
		}
		merged.addAll(newIgnoredPaths);
		return merged;
	}

	private static boolean isAnyPrefixOf(String pathToCheck, Collection<String> possiblePrefixes) {
		for (String possiblePrefix : possiblePrefixes)
			if (pathToCheck.startsWith(possiblePrefix) || possiblePrefix.equals(pathToCheck + '/'))
				return true;
		return false;
	}

	/**
	 * @return list of files added to the index, not in the tree
	 */
	public Set<String> getAdded() {
		return Collections.unmodifiableSet(added);
	}

	/**
	 * @return list of files changed from tree to index
	 */
	public Set<String> getChanged() {
		return changed;
	}

	/**
	 * @return list of files removed from index, but in tree
	 */
	public Set<String> getRemoved() {
		return removed;
	}

	/**
	 * @return list of files in index, but not filesystem
	 */
	public Set<String> getMissing() {
		return missing;
	}

	/**
	 * @return list of files modified on disk relative to the index
	 */
	public Set<String> getModified() {
		return modified;
	}

	/**
	 * @return list of files that are not ignored, and not in the index.
	 */
	public Set<String> getUntracked() {
		return untracked;
	}

	/**
	 * @return list of folders containing only untracked files/folders
	 * The folder paths end with /
	 */
	public Set<String> getUntrackedFolders() {
		return untrackedFolders;
	}

	/**
	 * @return list of files that are in conflict
	 */
	public Set<String> getConflicting() {
		return conflicts;
	}

	/**
	 * @see IndexDiff#getIgnoredNotInIndex()
	 * @return list of files that are ignored
	 */
	public Set<String> getIgnoredNotInIndex() {
		return ignored;
	}

	/**
	 * @return the changed files
	 */
	public Collection<IResource> getChangedResources() {
		return changedResources;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		dumpList(builder, "added", added); //$NON-NLS-1$
		dumpList(builder, "changed", changed); //$NON-NLS-1$
		dumpList(builder, "removed", removed); //$NON-NLS-1$
		dumpList(builder, "missing", missing); //$NON-NLS-1$
		dumpList(builder, "modified", modified); //$NON-NLS-1$
		dumpList(builder, "untracked", untracked); //$NON-NLS-1$
		dumpList(builder, "untrackedFolders", untrackedFolders); //$NON-NLS-1$
		dumpList(builder, "conflicts", conflicts); //$NON-NLS-1$
		dumpList(builder, "ignored", ignored); //$NON-NLS-1$
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
