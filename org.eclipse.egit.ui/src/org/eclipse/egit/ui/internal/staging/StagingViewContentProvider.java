/*******************************************************************************
 * Copyright (C) 2011, 2020 Bernard Leach <leachbj@bouncycastle.org> and others.
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
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.staging.StagingView.Presentation;
import org.eclipse.egit.ui.internal.staging.StagingView.StagingViewUpdate;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.model.WorkbenchContentProvider;

/**
 * ContentProvider for staged and unstaged tree nodes
 */
public class StagingViewContentProvider extends WorkbenchContentProvider
		implements ILazyTreeContentProvider {

	/** All files for the section (staged or unstaged). */
	private StagingEntry[] content = new StagingEntry[0];

	/** Root nodes for the "Tree" presentation. */
	private Object[] treeRoots;

	/** Root nodes for the "Compact Tree" presentation. */
	private Object[] compactTreeRoots;

	// The virtual tree maintains expansion for been visible folders only.
	private Set<StagingFolderEntry> expandedFolders = new HashSet<>();

	private final StagingView stagingView;

	private final TreeViewer treeViewer;

	private final boolean unstagedSection;

	private Repository repository;

	private boolean rootDetermined;

	private IContainer rootContainer;

	private final EntryComparator comparator;

	StagingViewContentProvider(StagingView stagingView, TreeViewer treeViewer,
			boolean unstagedSection) {
		this.stagingView = stagingView;
		this.treeViewer = treeViewer;
		this.unstagedSection = unstagedSection;
		comparator = new EntryComparator();

		Tree tree = treeViewer.getTree();
		tree.addListener(SWT.SetData, new Listener() {
			@Override
			public void handleEvent(Event event) {
				TreeItem item = (TreeItem) event.item;
				TreeItem parentItem = item.getParentItem();
				Object parent = parentItem == null ? null
						: parentItem.getData();
				Object[] children = getChildren(parent);
				if (event.index < children.length) {
					ILabelProvider labelProvider = (ILabelProvider) treeViewer
						.getLabelProvider();
					Object entry = children[event.index];
					item.setImage(labelProvider.getImage(entry));
					item.setText(labelProvider.getText(entry));
					item.setData(entry);
					if (entry instanceof StagingFolderEntry) {
						StagingFolderEntry folder = (StagingFolderEntry) entry;
						item.setItemCount(folder.getChildren().length);
						if (needsExpansion(folder)) {
							expandedFolders.add(folder);
							item.setExpanded(true);
						}
					}
				}
			}
		});

		treeViewer.addTreeListener(new ITreeViewerListener() {
			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				expandedFolders.remove(event.getElement());
			}

			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				Object element = event.getElement();
				if (element instanceof StagingFolderEntry) {
					expandedFolders.add((StagingFolderEntry) element);
				}
			}
		});
	}

	private boolean needsExpansion(StagingFolderEntry folder) {
		int autoExpandLevel = treeViewer.getAutoExpandLevel();
		if (autoExpandLevel == AbstractTreeViewer.ALL_LEVELS) {
			return true;
		}
		if (expandedFolders.contains(folder)) {
			return true;
		}
		StagingFolderEntry parent = folder.getParent();
		while (autoExpandLevel > 1) {
			if (parent == null) {
				// auto expand level is higher than our tree depth
				return true;
			}
			parent = parent.getParent();
			autoExpandLevel--;
		}
		return false;
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
			if (stagingView.getPresentation() == Presentation.LIST) {
				getTreeRoots();
				return content;
			}
			return getTreePresentationRoots();
		}
	}

	IFile getFile(StagingEntry entry) {
		StagingFolderEntry parent = entry.getParent();
		IContainer container = null;
		if (parent == null) {
			if (rootDetermined) {
				container = rootContainer;
			} else {
				rootDetermined = true;
				Repository repo = entry.getRepository();
				if (repo != null) {
					IPath path = new Path(
							repository.getWorkTree().getAbsolutePath());
					rootContainer = ResourcesPlugin.getWorkspace().getRoot()
							.getContainerForLocation(path);
				}
				container = rootContainer;
			}
		} else {
			container = parent.getContainer();
		}
		if (container == null) {
			return null;
		}
		IFile file = container.getFile(new Path(entry.getName()));
		return file.exists() ? file : null;
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
		List<Object> children = childrenForPath.computeIfAbsent(path,
				key -> new ArrayList<>());
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

	void setInput(StagingViewUpdate input) {
		if ((treeViewer.getControl().getStyle() & SWT.VIRTUAL) == 0) {
			treeViewer.setInput(input);
			return;
		}
		final Tree tree = treeViewer.getTree();
		inputChanged(treeViewer, tree.getData(), input);
	}

	StagingViewUpdate getInput() {
		if ((treeViewer.getControl().getStyle() & SWT.VIRTUAL) == 0) {
			return (StagingViewUpdate) treeViewer.getInput();
		}
		return (StagingViewUpdate) treeViewer.getTree().getData();
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (!(newInput instanceof StagingViewUpdate))
			return;

		if (viewer != treeViewer) {
			return;
		}

		StagingViewUpdate update = (StagingViewUpdate) newInput;

		if (update.repository == null || update.indexDiff == null) {
			content = new StagingEntry[0];
			treeRoots = new Object[0];
			compactTreeRoots = new Object[0];
			rootDetermined = false;
			rootContainer = null;
			return;
		}

		if (update.repository != repository) {
			treeRoots = null;
			compactTreeRoots = null;
			rootDetermined = false;
			rootContainer = null;
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
							file, s -> null));
				else
					nodes.add(new StagingEntry(repository, MISSING, file,
							s -> null));
			for (String file : indexDiff.getModified())
				if (indexDiff.getChanged().contains(file))
					nodes.add(new StagingEntry(repository, MODIFIED_AND_CHANGED,
							file, this::getFile));
				else if (indexDiff.getAdded().contains(file))
					nodes.add(new StagingEntry(repository, MODIFIED_AND_ADDED,
							file, this::getFile));
				else
					nodes.add(new StagingEntry(repository, MODIFIED, file,
							this::getFile));
			for (String file : indexDiff.getUntracked())
				nodes.add(new StagingEntry(repository, UNTRACKED, file,
						this::getFile));
			for (String file : indexDiff.getConflicting())
				nodes.add(new StagingEntry(repository, CONFLICTING, file,
						this::getFile));
		} else {
			for (String file : indexDiff.getAdded())
				nodes.add(new StagingEntry(repository, ADDED, file,
						this::getFile));
			for (String file : indexDiff.getChanged())
				nodes.add(new StagingEntry(repository, CHANGED, file,
						this::getFile));
			for (String file : indexDiff.getRemoved())
				nodes.add(new StagingEntry(repository, REMOVED, file,
						this::getFile));
		}

		setSymlinkFileMode(indexDiff, nodes);
		setSubmoduleFileMode(indexDiff, nodes);

		content = nodes.toArray(new StagingEntry[0]);
		Arrays.sort(content, comparator);

		treeRoots = null;
		compactTreeRoots = null;

		if ((treeViewer.getControl().getStyle() & SWT.VIRTUAL) == 0) {
			return;
		}

		treeViewer.getTree().setData(newInput);
		internalRedraw();
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

	/**
	 * Clear the widget tree item data, to obtain information freshly from
	 * content provider as becoming visible.
	 */
	public void refreshView() {
		if ((treeViewer.getControl().getStyle() & SWT.VIRTUAL) == 0) {
			treeViewer.refresh();
			return;
		}

		internalRedraw();
	}

	@Override
	public void updateElement(Object parent, int index) {
		Object[] children = getChildren(parent);
		if (index < children.length) {
			treeViewer.replace(parent, index, children[index]);
		}
		treeViewer.setHasChildren(parent, children.length > 0);
	}

	@Override
	public void updateChildCount(Object element, int currentChildCount) {
		Object[] children = getChildren(element);
		int newChildCount = children.length;
		if (newChildCount != currentChildCount) {
			treeViewer.setChildCount(element, newChildCount);
		}
	}

	void setExpandedElements(Set<StagingFolderEntry> expanded) {
		if ((treeViewer.getControl().getStyle() & SWT.VIRTUAL) == 0) {
			treeViewer.setExpandedElements(expanded.toArray());
			return;
		}

		expandedFolders = expanded;
		internalRedraw();
	}

	void collapseAll() {
		if ((treeViewer.getControl().getStyle() & SWT.VIRTUAL) == 0) {
			UIUtils.collapseAll(treeViewer);
			return;
		}

		expandedFolders = new HashSet<>();
		internalRedraw();
	}

	void expandAll() {
		if ((treeViewer.getControl().getStyle() & SWT.VIRTUAL) == 0) {
			UIUtils.expandAll(treeViewer);
			return;
		}

		expandedFolders = new HashSet<>();
		for (Object root : getTreePresentationRoots()) {
			if (root instanceof StagingFolderEntry) {
				walkRecursiveFolders(root,
						(folder) -> expandedFolders.add(folder));
			}
		}
		internalRedraw();
	}

	private void walkRecursiveFolders(Object start,
			Consumer<StagingFolderEntry> consumer) {
		if (start instanceof StagingFolderEntry) {
			StagingFolderEntry folder = (StagingFolderEntry) start;
			consumer.accept(folder);
			for (Object child : folder.getChildren()) {
				walkRecursiveFolders(child, consumer);
			}
		}
	}

	private void internalRedraw() {
		if ((treeViewer.getControl().getStyle() & SWT.VIRTUAL) == 0) {
			return;
		}
		Tree tree = treeViewer.getTree();
		// tree.clearAll(true) does neither clear item data, child count
		// nor expanded state: need to remove all elements instead.
		tree.setItemCount(0);
		tree.setItemCount(getChildren(null).length);
	}

}
