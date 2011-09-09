/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
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

import org.eclipse.core.resources.IFile;
import org.eclipse.jgit.lib.IndexDiff;

/**
 * This immutable class is used to store the data of an {@link IndexDiff}
 * object.
 *
 */
public class IndexDiffData {

	private static final String NEW_LINE = "\n"; //$NON-NLS-1$

	private Set<String> added = new HashSet<String>();

	private Set<String> changed = new HashSet<String>();

	private Set<String> removed = new HashSet<String>();

	private Set<String> missing = new HashSet<String>();

	private Set<String> modified = new HashSet<String>();

	private Set<String> untracked = new HashSet<String>();

	private Set<String> conflicts = new HashSet<String>();

	private final Collection<IFile> changedFileResources;

	/**
	 * @param indexDiff
	 */
	public IndexDiffData(IndexDiff indexDiff) {
		added.addAll(indexDiff.getAdded());
		changed.addAll(indexDiff.getChanged());
		removed.addAll(indexDiff.getRemoved());
		missing.addAll(indexDiff.getMissing());
		modified.addAll(indexDiff.getModified());
		untracked.addAll(indexDiff.getUntracked());
		conflicts.addAll(indexDiff.getConflicting());
		changedFileResources = null;
		makeSetsUnmodifiable();
	}

	private void makeSetsUnmodifiable() {
		added = Collections.unmodifiableSet(added);
		changed = Collections.unmodifiableSet(changed);
		removed = Collections.unmodifiableSet(removed);
		missing = Collections.unmodifiableSet(missing);
		modified = Collections.unmodifiableSet(modified);
		untracked = Collections.unmodifiableSet(untracked);
		conflicts = Collections.unmodifiableSet(conflicts);
	}

	/**
	 * The method merges the existing IndexDiffData object baseDiff with a new
	 * IndexDiffData object that was calculated for a subset of files
	 * (changedFiles).
	 *
	 * @param baseDiff
	 * @param changedFiles
	 * @param changedFileResources
	 * @param diffForChangedFiles
	 */
	public IndexDiffData(IndexDiffData baseDiff,
			Collection<String> changedFiles,
			Collection<IFile> changedFileResources,
			IndexDiff diffForChangedFiles) {
		this.changedFileResources = Collections
				.unmodifiableCollection(new HashSet<IFile>(changedFileResources));
		added.addAll(baseDiff.getAdded());
		changed.addAll(baseDiff.getChanged());
		removed.addAll(baseDiff.getRemoved());
		missing.addAll(baseDiff.getMissing());
		modified.addAll(baseDiff.getModified());
		untracked.addAll(baseDiff.getUntracked());
		conflicts.addAll(baseDiff.getConflicting());

		mergeList(added, changedFiles, diffForChangedFiles.getAdded());
		mergeList(changed, changedFiles, diffForChangedFiles.getChanged());
		mergeList(removed, changedFiles, diffForChangedFiles.getRemoved());
		mergeList(missing, changedFiles, diffForChangedFiles.getMissing());
		mergeList(modified, changedFiles, diffForChangedFiles.getModified());
		mergeList(untracked, changedFiles, diffForChangedFiles.getUntracked());
		mergeList(conflicts, changedFiles, diffForChangedFiles.getConflicting());

		makeSetsUnmodifiable();
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
	 * @return list of files that are in conflict
	 */
	public Set<String> getConflicting() {
		return conflicts;
	}

	/**
	 * @return the changed files
	 */
	public Collection<IFile> getChangedFileResources() {
		return changedFileResources;
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
		dumpList(builder, "conflicts", conflicts); //$NON-NLS-1$
		dumpFileResourceList(builder,
				"changedFileResources", changedFileResources); //$NON-NLS-1$
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

	private void dumpFileResourceList(StringBuilder builder, String listName,
			Collection<IFile> list) {
		if (list == null)
			return;
		builder.append(listName);
		builder.append(NEW_LINE);
		for (IFile file : list) {
			builder.append(file.getFullPath().toOSString());
			builder.append(NEW_LINE);
		}
		builder.append(NEW_LINE);
	}

}
