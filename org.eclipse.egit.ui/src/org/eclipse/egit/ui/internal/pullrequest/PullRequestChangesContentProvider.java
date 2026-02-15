/*******************************************************************************
 * Copyright (C) 2026, Eclipse EGit contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.pullrequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.model.WorkbenchContentProvider;

/**
 * Content provider for pull request changed files tree
 */
public class PullRequestChangesContentProvider
		extends WorkbenchContentProvider {

	private PullRequestChangedFile[] content = new PullRequestChangedFile[0];

	private Object[] treeRoots;

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof PullRequestChangedFile) {
			return new Object[0];
		}
		if (parentElement instanceof PullRequestFolderEntry) {
			return ((PullRequestFolderEntry) parentElement).getChildren();
		}
		return getTreeRoots();
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof PullRequestFolderEntry) {
			return ((PullRequestFolderEntry) element).getParent();
		}
		if (element instanceof PullRequestChangedFile) {
			return ((PullRequestChangedFile) element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof PullRequestFolderEntry;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof List) {
			@SuppressWarnings("unchecked")
			List<PullRequestChangedFile> list = (List<PullRequestChangedFile>) inputElement;
			setContent(list.toArray(new PullRequestChangedFile[0]));
			return getTreeRoots();
		}
		return getChildren(inputElement);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		super.inputChanged(viewer, oldInput, newInput);
		treeRoots = null; // Invalidate cache
	}

	/**
	 * Sets the content for this provider
	 *
	 * @param files
	 *            the changed files
	 */
	public void setContent(PullRequestChangedFile[] files) {
		this.content = files != null ? files : new PullRequestChangedFile[0];
		this.treeRoots = null; // Invalidate cache
	}

	private Object[] getTreeRoots() {
		if (treeRoots != null) {
			return treeRoots;
		}

		treeRoots = buildTreeStructure(content);
		return treeRoots;
	}

	private Object[] buildTreeStructure(PullRequestChangedFile[] files) {
		if (files == null || files.length == 0) {
			return new Object[0];
		}

		Map<IPath, List<Object>> childrenForPath = new HashMap<>();
		Set<IPath> folderPaths = new HashSet<>();

		// Step 1: Collect all folder paths and build parent-child map
		for (PullRequestChangedFile file : files) {
			IPath filePath = new Path(file.getPath());
			IPath folderPath = filePath.removeLastSegments(1);

			// Add file to its parent folder
			addChild(childrenForPath, folderPath, file);

			// Walk up the tree to find all ancestor folders
			IPath p = folderPath;
			while (p.segmentCount() > 0) {
				folderPaths.add(p);
				if (p.segmentCount() > 1) {
					IPath parent = p.removeLastSegments(1);
					// Mark that this parent has this folder as a child
					// (will be added in step 3)
					p = parent;
				} else {
					break;
				}
			}
		}

		// Step 2: Create folder entries
		Map<IPath, PullRequestFolderEntry> folders = new HashMap<>();
		for (IPath path : folderPaths) {
			String label = path.lastSegment();
			if (label == null) {
				label = ""; //$NON-NLS-1$
			}
			PullRequestFolderEntry folder = new PullRequestFolderEntry(path,
					label);
			folders.put(path, folder);
		}

		// Step 3: Build parent-child relationships for folders
		for (IPath path : folderPaths) {
			if (path.segmentCount() > 1) {
				IPath parentPath = path.removeLastSegments(1);
				PullRequestFolderEntry folder = folders.get(path);
				addChild(childrenForPath, parentPath, folder);
			}
		}

		// Step 4: Assign children to folders and set parent references
		for (IPath path : folderPaths) {
			PullRequestFolderEntry folder = folders.get(path);
			List<Object> children = childrenForPath.get(path);

			if (children != null) {
				// Set parent for each child
				for (Object child : children) {
					if (child instanceof PullRequestChangedFile) {
						((PullRequestChangedFile) child).setParent(folder);
					} else if (child instanceof PullRequestFolderEntry) {
						((PullRequestFolderEntry) child).setParent(folder);
					}
				}

				// Sort children: folders first, then files, alphabetically
				children.sort(new Comparator<>() {
					@Override
					public int compare(Object o1, Object o2) {
						boolean o1IsFolder = o1 instanceof PullRequestFolderEntry;
						boolean o2IsFolder = o2 instanceof PullRequestFolderEntry;

						if (o1IsFolder && !o2IsFolder) {
							return -1;
						} else if (!o1IsFolder && o2IsFolder) {
							return 1;
						} else {
							String name1 = getElementName(o1);
							String name2 = getElementName(o2);
							return name1.compareToIgnoreCase(name2);
						}
					}
				});

				folder.setChildren(children.toArray());
			}
		}

		// Step 5: Find root-level entries
		List<Object> roots = new ArrayList<>();

		// Add root-level folders
		for (PullRequestFolderEntry folder : folders.values()) {
			if (folder.getParent() == null
					&& folder.getPath().segmentCount() == 1) {
				roots.add(folder);
			}
		}

		// Add root-level files (files directly in repository root)
		for (PullRequestChangedFile file : files) {
			if (new Path(file.getPath()).segmentCount() == 1) {
				roots.add(file);
			}
		}

		// Sort roots
		roots.sort(new Comparator<>() {
			@Override
			public int compare(Object o1, Object o2) {
				boolean o1IsFolder = o1 instanceof PullRequestFolderEntry;
				boolean o2IsFolder = o2 instanceof PullRequestFolderEntry;

				if (o1IsFolder && !o2IsFolder) {
					return -1;
				} else if (!o1IsFolder && o2IsFolder) {
					return 1;
				} else {
					String name1 = getElementName(o1);
					String name2 = getElementName(o2);
					return name1.compareToIgnoreCase(name2);
				}
			}
		});

		// Step 6: Compress single-child folder chains
		compressFolderChains(folders);

		return roots.toArray();
	}

	private void addChild(Map<IPath, List<Object>> map, IPath parentPath,
			Object child) {
		map.computeIfAbsent(parentPath, k -> new ArrayList<>()).add(child);
	}

	private String getElementName(Object element) {
		if (element instanceof PullRequestFolderEntry) {
			return ((PullRequestFolderEntry) element).getLabel();
		} else if (element instanceof PullRequestChangedFile) {
			return ((PullRequestChangedFile) element).getName();
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Compresses folder chains where a folder has exactly one child that is
	 * also a folder by merging them and using dot-separated labels
	 *
	 * @param folders
	 *            map of all folders
	 */
	private void compressFolderChains(Map<IPath, PullRequestFolderEntry> folders) {
		for (PullRequestFolderEntry folder : folders.values()) {
			Object[] children = folder.getChildren();
			
			// Check if folder has exactly one child and that child is a folder
			if (children != null && children.length == 1
					&& children[0] instanceof PullRequestFolderEntry) {
				PullRequestFolderEntry childFolder = (PullRequestFolderEntry) children[0];
				
				// Build compressed label by following the chain
				List<String> labels = new ArrayList<>();
				labels.add(folder.getLabel());
				
				PullRequestFolderEntry current = childFolder;
				while (current != null) {
					labels.add(current.getLabel());
					
					Object[] currentChildren = current.getChildren();
					// Continue if current has exactly one child that is a folder
					if (currentChildren != null && currentChildren.length == 1
							&& currentChildren[0] instanceof PullRequestFolderEntry) {
						current = (PullRequestFolderEntry) currentChildren[0];
					} else {
						// End of chain - adopt the children of the last folder
						folder.setChildren(currentChildren);
						
						// Update parent reference of adopted children
						if (currentChildren != null) {
							for (Object adoptedChild : currentChildren) {
								if (adoptedChild instanceof PullRequestChangedFile) {
									((PullRequestChangedFile) adoptedChild).setParent(folder);
								} else if (adoptedChild instanceof PullRequestFolderEntry) {
									((PullRequestFolderEntry) adoptedChild).setParent(folder);
								}
							}
						}
						break;
					}
				}
				
				// Set the compressed label
				folder.setLabel(String.join(".", labels)); //$NON-NLS-1$
			}
		}
	}
}
