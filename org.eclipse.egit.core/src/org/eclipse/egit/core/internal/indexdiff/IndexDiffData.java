package org.eclipse.egit.core.internal.indexdiff;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jgit.lib.IndexDiff;

/**
 * @author d020964
 *
 */
public class IndexDiffData {

	private static final String NEW_LINE = "\n";  //$NON-NLS-1$

	private Set<String> added = new HashSet<String>();

	private Set<String> changed = new HashSet<String>();

	private Set<String> removed = new HashSet<String>();

	private Set<String> missing = new HashSet<String>();

	private Set<String> modified = new HashSet<String>();

	private Set<String> untracked = new HashSet<String>();

	private Set<String> conflicts = new HashSet<String>();

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
	}

	/**
	 * @param baseDiff
	 * @param changedFiles
	 * @param diffForChangedFiles
	 */
	public IndexDiffData(IndexDiffData baseDiff, Collection<String> changedFiles, IndexDiff diffForChangedFiles) {
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
	}

	private void mergeList(Set<String> baseList, Collection<String> changedFiles,
			Set<String> listForChangedFiles) {
		for (String file:changedFiles) {
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
		return added;
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		dumpList(builder, "added", added);  //$NON-NLS-1$
		dumpList(builder, "changed", changed);  //$NON-NLS-1$
		dumpList(builder, "removed", removed);  //$NON-NLS-1$
		dumpList(builder, "missing", missing);  //$NON-NLS-1$
		dumpList(builder, "modified", modified);  //$NON-NLS-1$
		dumpList(builder, "untracked", untracked);  //$NON-NLS-1$
		dumpList(builder, "conflicts", conflicts);  //$NON-NLS-1$
		return builder.toString();
	}

	private void dumpList(StringBuilder builder, String listName, Set<String> list) {
		builder.append(listName);
		builder.append(NEW_LINE);
		for(String entry:list) {
			builder.append(entry);
			builder.append(NEW_LINE);
		}
		builder.append(NEW_LINE);
	}

}
