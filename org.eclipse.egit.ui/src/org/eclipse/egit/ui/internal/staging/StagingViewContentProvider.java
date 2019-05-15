/*******************************************************************************
 * Copyright (C) 2011, 2014 Bernard Leach <leachbj@bouncycastle.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.ADDED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.CHANGED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.CONFLICTING;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.MISSING;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.MISSING_AND_CHANGED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.MODIFIED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.MODIFIED_AND_ADDED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.MODIFIED_AND_CHANGED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.REMOVED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.UNTRACKED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.ui.internal.staging.StagingView.Presentation;
import org.eclipse.egit.ui.internal.staging.StagingView.StagingViewUpdate;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.model.WorkbenchContentProvider;

/**
 * ContentProvider for staged and unstaged tree nodes
 */
public class StagingViewContentProvider extends WorkbenchContentProvider {
	/** All files for the section (staged or unstaged). */
	private StagingEntry[] content = new StagingEntry[0];

	/** Root nodes for the "Tree" presentation. */
	private Object[] treeRoots;

	/** Root nodes for the "Compact Tree" presentation. */
	private Object[] compactTreeRoots;

	private StagingView stagingView;
	private boolean unstagedSection;

	private Repository repository;

	private final EntryComparator comparator;

	StagingViewContentProvider(StagingView stagingView, boolean unstagedSection) {
		this.stagingView = stagingView;
		this.unstagedSection = unstagedSection;
		comparator = new EntryComparator();
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof StagingFolderEntry)
			return ((StagingFolderEntry) element).getParent();
		if (element instanceof StagingEntry)
			return ((StagingEntry) element).getParent();
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return !(element instanceof StagingEntry);
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (repository == null)
			return new Object[0];
		if (parentElement instanceof StagingEntry)
			return new Object[0];
		if (parentElement instanceof StagingFolderEntry) {
			return ((StagingFolderEntry) parentElement).getChildren();
		} else {
			// Return the root nodes
			if (stagingView.getPresentation() == Presentation.LIST)
				return content;
			else
				return getTreePresentationRoots();
		}
	}

	Object[] getTreePresentationRoots() {
		Presentation presentation = stagingView.getPresentation();
		switch (presentation) {
		case COMPACT_TREE:
			return getCompactTreeRoots();
		case TREE:
			return getTreeRoots();
		default:
			return new StagingFolderEntry[0];
		}
	}

	private Object[] getCompactTreeRoots() {
		if (compactTreeRoots == null)
			compactTreeRoots = calculateTreePresentationRoots(true);
		return compactTreeRoots;
	}

	private Object[] getTreeRoots() {
		if (treeRoots == null)
			treeRoots = calculateTreePresentationRoots(false);
		return treeRoots;
	}

	private Object[] calculateTreePresentationRoots(boolean compact) {
		if (content == null || content.length == 0)
			return new Object[0];

		List<Object> roots = new ArrayList<>();
		Map<IPath, List<Object>> childrenForPath = new HashMap<>();

		Set<IPath> folderPaths = new HashSet<>();
		Map<IPath, String> childSegments = new HashMap<>();

		for (StagingEntry file : content) {
			IPath folderPath = file.getParentPath();
			if (folderPath.segmentCount() == 0) {
				// No folders need to be created, this is a root file
				roots.add(file);
				continue;
			}
			folderPaths.add(folderPath);
			addChild(childrenForPath, folderPath, file);
			for (IPath p = folderPath; p.segmentCount() != 1; p = p
					.removeLastSegments(1)) {
				IPath parent = p.removeLastSegments(1);
				if (!compact) {
					folderPaths.add(parent);
				} else {
					String childSegment = p.lastSegment();
					String knownChildSegment = childSegments.get(parent);
					if (knownChildSegment == null) {
						childSegments.put(parent, childSegment);
					} else if (!childSegment.equals(knownChildSegment)) {
						// The parent has more than 1 direct child folder -> we
						// need to make a node for it.
						folderPaths.add(parent);
					}
				}
			}
		}

		IPath workingDirectory = new Path(repository.getWorkTree()
				.getAbsolutePath());

		List<StagingFolderEntry> folderEntries = new ArrayList<>();
		for (IPath folderPath : folderPaths) {
			IPath parent = folderPath.removeLastSegments(1);
			// Find first existing parent node, but stop at root
			while (parent.segmentCount() != 0 && !folderPaths.contains(parent))
				parent = parent.removeLastSegments(1);
			if (parent.segmentCount() == 0) {
				// Parent is root
				StagingFolderEntry folderEntry = new StagingFolderEntry(
						workingDirectory, folderPath, folderPath);
				folderEntries.add(folderEntry);
				roots.add(folderEntry);
			} else {
				// Parent is existing node
				IPath nodePath = folderPath.makeRelativeTo(parent);
				StagingFolderEntry folderEntry = new StagingFolderEntry(
						workingDirectory, folderPath, nodePath);
				folderEntries.add(folderEntry);
				addChild(childrenForPath, parent, folderEntry);
			}
		}

		for (StagingFolderEntry folderEntry : folderEntries) {
			List<Object> children = childrenForPath.get(folderEntry.getPath());
			if (children != null) {
				for (Object child : children) {
					if (child instanceof StagingEntry)
						((StagingEntry) child).setParent(folderEntry);
					else if (child instanceof StagingFolderEntry)
						((StagingFolderEntry) child).setParent(folderEntry);
				}
				Collections.sort(children, comparator);
				folderEntry.setChildren(children.toArray());
			}
		}

		Collections.sort(roots, comparator);
		return roots.toArray();
	}

	private static void addChild(Map<IPath, List<Object>> childrenForPath,
			IPath path, Object child) {
		List<Object> children = childrenForPath.get(path);
		if (children == null) {
			children = new ArrayList<>();
			childrenForPath.put(path, children);
		}
		children.add(child);
	}

	int getShownCount() {
		Pattern filterPattern = getFilterPattern();
		if (filterPattern == null) {
			return getCount();
		} else {
			int shownCount = 0;
			for (StagingEntry entry : content) {
				if (matches(entry, filterPattern)) {
					shownCount++;
				}
			}
			return shownCount;
		}
	}

	List<StagingEntry> getStagingEntriesFiltered(StagingFolderEntry folder) {
		List<StagingEntry> stagingEntries = new ArrayList<>();
		addFilteredDescendants(folder, getFilterPattern(), stagingEntries);
		return stagingEntries;
	}

	private void addFilteredDescendants(StagingFolderEntry folder,
			Pattern pattern, List<StagingEntry> result) {
		for (Object child : folder.getChildren()) {
			if (child instanceof StagingFolderEntry) {
				addFilteredDescendants((StagingFolderEntry) child, pattern,
						result);
			} else if (matches((StagingEntry) child, pattern)) {
				result.add((StagingEntry) child);
			}
		}
	}

	private boolean matches(StagingEntry entry, Pattern pattern) {
		return pattern == null || pattern.matcher(entry.getPath()).find();
	}

	boolean isInFilter(StagingEntry stagingEntry) {
		return matches(stagingEntry, getFilterPattern());
	}

	private Pattern getFilterPattern() {
		return stagingView.getFilterPattern();
	}

	boolean hasVisibleChildren(StagingFolderEntry folder) {
		Pattern pattern = getFilterPattern();
		if (pattern == null) {
			return true;
		}
		return hasVisibleDescendants(folder, pattern);
	}

	private boolean hasVisibleDescendants(StagingFolderEntry folder,
			Pattern pattern) {
		for (Object child : folder.getChildren()) {
			if (child instanceof StagingFolderEntry) {
				if (hasVisibleDescendants((StagingFolderEntry) child,
						pattern)) {
					return true;
				}
			} else if (matches((StagingEntry) child, pattern)) {
				return true;
			}
		}
		return false;
	}

	StagingEntry[] getStagingEntries() {
		return content;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (!(newInput instanceof StagingViewUpdate))
			return;

		StagingViewUpdate update = (StagingViewUpdate) newInput;

		if (update.repository == null || update.indexDiff == null) {
			content = new StagingEntry[0];
			treeRoots = new Object[0];
			compactTreeRoots = new Object[0];
			return;
		}

		if (update.repository != repository) {
			treeRoots = null;
			compactTreeRoots = null;
		}

		repository = update.repository;

		Set<StagingEntry> nodes = new TreeSet<>(
				(a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getPath(),
						b.getPath()));

		if (update.changedResources != null
				&& !update.changedResources.isEmpty()) {
			nodes.addAll(Arrays.asList(content));
			for (String res : update.changedResources)
				for (StagingEntry entry : content)
					if (entry.getPath().equals(res))
						nodes.remove(entry);
		}

		final IndexDiffData indexDiff = update.indexDiff;
		if (unstagedSection) {
			for (String file : indexDiff.getMissing())
				if (indexDiff.getChanged().contains(file))
					nodes.add(new StagingEntry(repository, MISSING_AND_CHANGED,
							file));
				else
					nodes.add(new StagingEntry(repository, MISSING, file));
			for (String file : indexDiff.getModified())
				if (indexDiff.getChanged().contains(file))
					nodes.add(new StagingEntry(repository, MODIFIED_AND_CHANGED,
							file));
				else if (indexDiff.getAdded().contains(file))
					nodes.add(new StagingEntry(repository, MODIFIED_AND_ADDED,
							file));
				else
					nodes.add(new StagingEntry(repository, MODIFIED, file));
			for (String file : indexDiff.getUntracked())
				nodes.add(new StagingEntry(repository, UNTRACKED, file));
			for (String file : indexDiff.getConflicting())
				nodes.add(new StagingEntry(repository, CONFLICTING, file));
		} else {
			for (String file : indexDiff.getAdded())
				nodes.add(new StagingEntry(repository, ADDED, file));
			for (String file : indexDiff.getChanged())
				nodes.add(new StagingEntry(repository, CHANGED, file));
			for (String file : indexDiff.getRemoved())
				nodes.add(new StagingEntry(repository, REMOVED, file));
		}

		setSymlinkFileMode(indexDiff, nodes);
		setSubmoduleFileMode(indexDiff, nodes);

		content = nodes.toArray(new StagingEntry[0]);
		Arrays.sort(content, comparator);

		treeRoots = null;
		compactTreeRoots = null;
	}

	@Override
	public void dispose() {
		// nothing to dispose
	}

	/**
	 * @return StagingEntry count
	 */
	public int getCount() {
		if (content == null)
			return 0;
		else
			return content.length;
	}

	/**
	 * Set file name mode to be enabled or disabled, to keep proper sorting
	 * order. This mode displays the names of the file first followed by the
	 * path to the folder that the file is in.
	 *
	 * @param enable
	 */
	void setFileNameMode(boolean enable) {
		comparator.fileNameMode = enable;
		if (content != null) {
			Arrays.sort(content, comparator);
		}
	}

	private static class EntryComparator implements Comparator<Object> {
		boolean fileNameMode;

		@Override
		public int compare(Object o1, Object o2) {
			if (o1 instanceof StagingEntry) {
				if (o2 instanceof StagingEntry) {
					StagingEntry e1 = (StagingEntry) o1;
					StagingEntry e2 = (StagingEntry) o2;
					if (fileNameMode) {
						int result = String.CASE_INSENSITIVE_ORDER
								.compare(e1.getName(), e2.getName());
						if (result != 0)
							return result;
						else
							return String.CASE_INSENSITIVE_ORDER.compare(
									e1.getParentPath().toString(),
									e2.getParentPath().toString());
					}
					return String.CASE_INSENSITIVE_ORDER.compare(e1.getPath(),
							e2.getPath());
				} else {
					// Files should come after folders
					return 1;
				}
			} else if (o1 instanceof StagingFolderEntry) {
				if (o2 instanceof StagingFolderEntry) {
					StagingFolderEntry f1 = (StagingFolderEntry) o1;
					StagingFolderEntry f2 = (StagingFolderEntry) o2;
					return f1.getPath().toString()
							.compareTo(f2.getPath().toString());
				} else {
					// Folders should come before files
					return -1;
				}
			} else {
				return 0;
			}
		}
	}

	/**
	 * Set the symlink file mode of the given StagingEntries.
	 *
	 * @param indexDiff
	 *            the index diff
	 * @param entries
	 *            the given StagingEntries
	 */
	private void setSymlinkFileMode(IndexDiffData indexDiff,
			Collection<StagingEntry> entries) {
		final Set<String> symlinks = indexDiff.getSymlinks();
		if (symlinks.isEmpty()) {
			return;
		}
		for (StagingEntry stagingEntry : entries) {
			if (symlinks.contains(stagingEntry.getPath()))
				stagingEntry.setSymlink(true);
		}
	}

	/**
	 * Set the submodule file mode (equivalent to FileMode.GITLINK) of the given
	 * StagingEntries.
	 *
	 * @param indexDiff
	 *            the index diff
	 * @param entries
	 *            the given StagingEntries
	 */
	private void setSubmoduleFileMode(IndexDiffData indexDiff,
			Collection<StagingEntry> entries) {
		final Set<String> submodules = indexDiff.getSubmodules();
		if (submodules.isEmpty()) {
			return;
		}
		for (StagingEntry stagingEntry : entries) {
			if (submodules.contains(stagingEntry.getPath()))
				stagingEntry.setSubmodule(true);
		}
	}
}
