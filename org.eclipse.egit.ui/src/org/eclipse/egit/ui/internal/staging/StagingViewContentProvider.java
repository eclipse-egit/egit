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
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.MISSING;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.MODIFIED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.PARTIALLY_MODIFIED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.REMOVED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.UNTRACKED;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.staging.StagingView.StagingViewUpdate;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.ui.model.WorkbenchContentProvider;

/**
 * ContentProvider for staged and unstaged tree nodes
 */
public class StagingViewContentProvider extends WorkbenchContentProvider {
	private StagingEntry[] content = new StagingEntry[0];

	private StagingFolderEntry[] folders;

	private StagingFolderEntry[] rootFolders;

	private StagingEntry[] rootFiles;

	private Object[] roots;

	private Object[] compressedRoots;

	private StagingFolderEntry[] compressedFolders;

	private List<StagingFolderEntry> folderList;

	private List<StagingFolderEntry> compressedFolderList;

	private StagingView stagingView;
	private boolean unstagedSection;

	private Repository repository;

	private FolderComparator comparator = new FolderComparator();

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
		if (parentElement instanceof StagingEntry)
			return null;
		int presentation;
		if (unstagedSection)
			presentation = stagingView.getUnstagedPresentation();
		else
			presentation = stagingView.getStagedPresentation();
		if (parentElement instanceof StagingFolderEntry) {
			if (presentation == StagingView.PRESENTATION_COMPRESSED_FOLDERS)
				return getChildResources((StagingFolderEntry) parentElement);
			else
				// Tree mode
				return getFolderChildren((StagingFolderEntry) parentElement);
		} else {
			if (presentation == StagingView.PRESENTATION_FLAT)
				return content;
			else if (presentation == StagingView.PRESENTATION_COMPRESSED_FOLDERS)
				return getCompressedRoots();
			else
				// Tree mode
				return getRoots();
		}
	}

	private Object[] getRoots() {
		if (roots == null) {
			List<Object> rootList = new ArrayList<Object>();
			rootFolders = getRootFolders();
			for (StagingFolderEntry rootFolder : rootFolders) {
				rootList.add(rootFolder);
			}
			if (rootFiles != null) {
				for (StagingEntry rootFile : rootFiles) {
					rootList.add(rootFile);
				}
			}
			roots = new Object[rootList.size()];
			rootList.toArray(roots);
		}
		return roots;
	}

	private Object[] getCompressedRoots() {
		if (compressedRoots == null) {
			List<Object> compressedRootList = new ArrayList<Object>();
			compressedFolders = getCompressedFolders();
			for (StagingFolderEntry compressedFolder : compressedFolders) {
				compressedRootList.add(compressedFolder);
			}
			for (StagingEntry rootFile : rootFiles) {
				compressedRootList.add(rootFile);
			}
			compressedRoots = new Object[compressedRootList.size()];
			compressedRootList.toArray(compressedRoots);
		}
		return compressedRoots;
	}

	private StagingFolderEntry[] getRootFolders() {
		if (content == null || content.length == 0)
			return new StagingFolderEntry[0];
		if (rootFolders == null)
			getFolders();
		return rootFolders;
	}

	@SuppressWarnings("unchecked")
	private StagingFolderEntry[] getFolders() {
		if (folders == null) {
			String workTreePath = stagingView.getCurrentRepository()
					.getWorkTree().getAbsolutePath();
			List<StagingFolderEntry> rootFolderList = new ArrayList<StagingFolderEntry>();
			List<StagingEntry> rootFileList = new ArrayList<StagingEntry>();
			folderList = new ArrayList<StagingFolderEntry>();
			List<String> resourceList = new ArrayList<String>();
			for (StagingEntry stagingEntry : content) {
				File parent = stagingEntry.getSystemFile();
				if (parent == null
						|| (parent.getParentFile() != null
						&& parent.getParentFile().getAbsolutePath()
								.equals(workTreePath))) {
					rootFileList.add(stagingEntry);
					continue;
				}

				while (parent != null
						&& !parent.getAbsolutePath()
								.equals(workTreePath)) {
					if (!parent.getParentFile().getAbsolutePath()
									.equals(workTreePath)
							&& resourceList.contains(parent.getParentFile()
									.getAbsolutePath()))
						break;
					if (parent.getParentFile() == null
							|| parent.getParentFile().getAbsolutePath()
									.equals(workTreePath)) {
						rootFolderList.add(new StagingFolderEntry(parent));
						resourceList.add(parent.getAbsolutePath());
					}
					parent = parent.getParentFile();
					if (parent != null) {
						folderList.add(new StagingFolderEntry(parent));
						resourceList.add(parent.getAbsolutePath());
					}
				}
			}
			folders = new StagingFolderEntry[folderList.size()];
			folderList.toArray(folders);
			Arrays.sort(folders, comparator);
			rootFolders = new StagingFolderEntry[rootFolderList.size()];
			rootFolderList.toArray(rootFolders);
			rootFiles = new StagingEntry[rootFileList.size()];
			rootFileList.toArray(rootFiles);
			Arrays.sort(rootFolders, comparator);
		}
		return folders;
	}

	private Object[] getFolderChildren(StagingFolderEntry parent) {
		List<Object> children = new ArrayList<Object>();
		folders = getFolders();
		for (StagingFolderEntry folder : folders) {
			if (folder.getFile().getParentFile() != null
					&& folder.getFile().getParentFile()
							.equals(parent.getFile())) {
				folder.setParent(parent);
				children.add(folder);
			}
		}
		for (StagingEntry file : content) {
			if (file.getSystemFile().getParentFile() != null
					&& file.getSystemFile().getParentFile()
							.equals(parent.getFile())) {
				file.setParent(parent);
				children.add(file);
			}
		}
		Object[] childArray = new Object[children.size()];
		children.toArray(childArray);
		return childArray;
	}

	@SuppressWarnings("unchecked")
	private StagingFolderEntry[] getCompressedFolders() {
		if (compressedFolders == null) {
			String workTreePath = stagingView.getCurrentRepository()
					.getWorkTree().getAbsolutePath();
			List<File> parentList = new ArrayList<File>();
			List<StagingEntry> rootFileList = new ArrayList<StagingEntry>();
			compressedFolderList = new ArrayList<StagingFolderEntry>();
			for (StagingEntry file : content) {
				File parentFile = file.getSystemFile().getParentFile();

				if (parentFile != null
						&& parentFile.getAbsolutePath().equals(workTreePath)) {
					rootFileList.add(file);
					continue;
				}

				if (!parentList.contains(parentFile)) {
					compressedFolderList
							.add(new StagingFolderEntry(
							parentFile));
					parentList.add(parentFile);
				}
			}
			compressedFolders = new StagingFolderEntry[compressedFolderList
					.size()];
			compressedFolderList.toArray(compressedFolders);
			Arrays.sort(compressedFolders, comparator);
			rootFiles = new StagingEntry[rootFileList.size()];
			rootFileList.toArray(rootFiles);
		}
		return compressedFolders;
	}

	private StagingEntry[] getChildResources(StagingFolderEntry parent) {
		File parentFile = parent.getFile();
		List<StagingEntry> children = new ArrayList<StagingEntry>();
		for (StagingEntry file : content) {
			if (file.getSystemFile().getParentFile().equals(parentFile)) {
				file.setParent(parent);
				children.add(file);
			}
		}
		StagingEntry[] childArray = new StagingEntry[children.size()];
		children.toArray(childArray);
		return childArray;
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
			rootFolders = new StagingFolderEntry[0];
			rootFiles = new StagingEntry[0];
			roots = new Object[0];
			compressedRoots = new Object[0];
			compressedFolders = new StagingFolderEntry[0];
			return;
		}

		if (update.repository != repository) {
			folders = null;
			rootFolders = null;
			rootFiles = null;
			roots = null;
			compressedRoots = null;
			folderList = null;
			compressedFolders = null;
			compressedFolderList = null;
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
			for (String file : indexDiff.getMissing()) {
				nodes.add(new StagingEntry(repository, MISSING, file));
			}
			for (String file : indexDiff.getModified()) {
				if (indexDiff.getChanged().contains(file))
					nodes.add(new StagingEntry(repository, PARTIALLY_MODIFIED,
							file));
				else
					nodes.add(new StagingEntry(repository, MODIFIED, file));
			}
			for (String file : indexDiff.getUntracked()) {
				nodes.add(new StagingEntry(repository, UNTRACKED, file));
			}
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

		folders = null;
		rootFolders = null;
		rootFiles = null;
		roots = null;
		compressedRoots = null;
		folderList = null;
		compressedFolders = null;
		compressedFolderList = null;
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

	private class FolderComparator implements Comparator {
		public int compare(Object obj0, Object obj1) {
			StagingFolderEntry folder0 = (StagingFolderEntry) obj0;
			StagingFolderEntry folder1 = (StagingFolderEntry) obj1;
			return folder0.getPath().toOSString()
					.compareTo(folder1.getPath().toOSString());
		}
	}
}