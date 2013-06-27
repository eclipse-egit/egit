/*******************************************************************************
 * Copyright (C) 2011, 2013 Bernard Leach <leachbj@bouncycastle.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.ADDED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.CHANGED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.CONFLICTING;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.MISSING;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.MISSING_AND_CHANGED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.MODIFIED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.PARTIALLY_MODIFIED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.REMOVED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.UNTRACKED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.staging.StagingView.Presentation;
import org.eclipse.egit.ui.internal.staging.StagingView.StagingViewUpdate;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.ui.model.WorkbenchContentProvider;

/**
 * ContentProvider for staged and unstaged tree nodes
 */
public class StagingViewContentProvider extends WorkbenchContentProvider {
	/** All files for the section (staged or unstaged). */
	private StagingEntry[] content = new StagingEntry[0];

	/** Folders for the "Tree" presentation. */
	private StagingFolderEntry[] treeFolders;

	/** Folders for the "Compact Tree" presentation. */
	private StagingFolderEntry[] compactTreeFolders;

	private StagingView stagingView;
	private boolean unstagedSection;

	private Repository repository;

	StagingViewContentProvider(StagingView stagingView, boolean unstagedSection) {
		this.stagingView = stagingView;
		this.unstagedSection = unstagedSection;
	}

	public Object getParent(Object element) {
		if (element instanceof StagingFolderEntry)
			return ((StagingFolderEntry) element).getParent();
		if (element instanceof StagingEntry)
			return ((StagingEntry) element).getParent();
		return null;
	}

	public boolean hasChildren(Object element) {
		return !(element instanceof StagingEntry);
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public Object[] getChildren(Object parentElement) {
		if (repository == null)
			return new Object[0];
		if (parentElement instanceof StagingEntry)
			return new Object[0];
		if (parentElement instanceof StagingFolderEntry) {
			return getFolderChildren((StagingFolderEntry) parentElement);
		} else {
			if (stagingView.getPresentation() == Presentation.LIST)
				return content;
			else {
				StagingFolderEntry[] allFolders = getStagingFolderEntries();
				List<Object> roots = new ArrayList<Object>();
				for (StagingFolderEntry folder : allFolders)
					if (folder.getParentPath().segmentCount() == 0)
						roots.add(folder);
				for (StagingEntry entry : content)
					if (!entry.getPath().contains("/")) //$NON-NLS-1$
						roots.add(entry);
				return roots.toArray(new Object[roots.size()]);
			}
		}
	}

	private Object[] getFolderChildren(StagingFolderEntry parent) {
		IPath parentPath = parent.getPath();
		List<Object> children = new ArrayList<Object>();
		for (StagingFolderEntry folder : getStagingFolderEntries()) {
			if (folder.getParentPath().equals(parentPath)) {
				folder.setParent(parent);
				children.add(folder);
			}
		}
		for (StagingEntry file : content) {
			if (file.getParentPath().equals(parentPath)) {
				file.setParent(parent);
				children.add(file);
			}
		}
		return children.toArray(new Object[children.size()]);
	}

	StagingFolderEntry[] getStagingFolderEntries() {
		Presentation presentation = stagingView.getPresentation();
		switch (presentation) {
		case COMPACT_TREE:
			return getCompactTreeFolders();
		case TREE:
			return getTreeFolders();
		default:
			return new StagingFolderEntry[0];
		}
	}

	private StagingFolderEntry[] getCompactTreeFolders() {
		if (compactTreeFolders == null)
			compactTreeFolders = calculateFolders(true);
		return compactTreeFolders;
	}

	private StagingFolderEntry[] getTreeFolders() {
		if (treeFolders == null)
			treeFolders = calculateFolders(false);
		return treeFolders;
	}

	private StagingFolderEntry[] calculateFolders(boolean compact) {
		if (content == null || content.length == 0)
			return new StagingFolderEntry[0];

		Set<IPath> folderPaths = new HashSet<IPath>();
		Map<IPath, String> childSegments = new HashMap<IPath, String>();

		for (StagingEntry file : content) {
			IPath folderPath = file.getParentPath();
			if (folderPath.segmentCount() == 0)
				// No folders need to be created
				continue;
			folderPaths.add(folderPath);
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

		List<StagingFolderEntry> folderEntries = new ArrayList<StagingFolderEntry>();
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
			} else {
				// Parent is existing node
				IPath nodePath = folderPath.makeRelativeTo(parent);
				StagingFolderEntry folderEntry = new StagingFolderEntry(
						workingDirectory, folderPath, nodePath);
				folderEntries.add(folderEntry);
			}
		}

		Collections.sort(folderEntries, FolderComparator.INSTANCE);
		return folderEntries.toArray(new StagingFolderEntry[folderEntries
				.size()]);
	}

	int getShownCount() {
		String filterString = getFilterString();
		if (filterString.length() == 0) {
			return getCount();
		} else {
			int shownCount = 0;
			for (StagingEntry entry : content) {
				if (isInFilter(entry))
					shownCount++;
			}
			return shownCount;
		}
	}

	List<StagingEntry> getStagingEntriesFiltered(StagingFolderEntry folder) {
		List<StagingEntry> stagingEntries = new ArrayList<StagingEntry>();
		for (StagingEntry stagingEntry : content) {
			if (folder.getLocation().isPrefixOf(stagingEntry.getLocation())) {
				if (isInFilter(stagingEntry))
					stagingEntries.add(stagingEntry);
			}
		}
		return stagingEntries;
	}

	boolean isInFilter(StagingEntry stagingEntry) {
		String filterString = getFilterString();
		return filterString.length() == 0
				|| stagingEntry.getPath().toUpperCase()
						.contains(filterString.toUpperCase());
	}

	private String getFilterString() {
		return stagingView.getFilterString();
	}

	boolean hasVisibleChildren(StagingFolderEntry folder) {
		if (getFilterString().length() == 0)
			return true;
		else
			return !getStagingEntriesFiltered(folder).isEmpty();
	}

	StagingEntry[] getStagingEntries() {
		return content;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (!(newInput instanceof StagingViewUpdate))
			return;

		StagingViewUpdate update = (StagingViewUpdate) newInput;

		if (update.repository == null || update.indexDiff == null) {
			content = new StagingEntry[0];
			treeFolders = new StagingFolderEntry[0];
			compactTreeFolders = new StagingFolderEntry[0];
			return;
		}

		if (update.repository != repository) {
			treeFolders = null;
			compactTreeFolders = null;
		}

		repository = update.repository;

		Set<StagingEntry> nodes = new TreeSet<StagingEntry>(
				new Comparator<StagingEntry>() {
					public int compare(StagingEntry o1, StagingEntry o2) {
						return o1.getPath().compareTo(o2.getPath());
					}
				});

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
					nodes.add(new StagingEntry(repository, PARTIALLY_MODIFIED,
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

		try {
		SubmoduleWalk walk = SubmoduleWalk.forIndex(repository);
		while(walk.next())
			for (StagingEntry entry : nodes)
				entry.setSubmodule(entry.getPath().equals(walk.getPath()));
		} catch(IOException e) {
			Activator.error(UIText.StagingViewContentProvider_SubmoduleError, e);
		}

		content = nodes.toArray(new StagingEntry[nodes.size()]);

		treeFolders = null;
		compactTreeFolders = null;
	}

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

	private static class FolderComparator implements
			Comparator<StagingFolderEntry> {
		public static FolderComparator INSTANCE = new FolderComparator();
		public int compare(StagingFolderEntry o1, StagingFolderEntry o2) {
			return o1.getPath().toString().compareTo(o2.getPath().toString());
		}
	}

}