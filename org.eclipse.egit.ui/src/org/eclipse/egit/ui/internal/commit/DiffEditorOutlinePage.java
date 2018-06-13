/*******************************************************************************
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.DiffRegionFormatter.FileDiffRegion;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * A {@link NestedContentOutlinePage} for the {DiffEditorPage}, displaying an
 * outline for {@link DiffDocument}s.
 */
public class DiffEditorOutlinePage extends NestedContentOutlinePage {

	private IDocument input;

	private CopyOnWriteArrayList<IOpenListener> openListeners = new CopyOnWriteArrayList<>();

	private ISelection selection;

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		TreeViewer viewer = getTreeViewer();
		viewer.setAutoExpandLevel(2);
		viewer.setUseHashlookup(true);
		viewer.setContentProvider(new DiffContentProvider());
		viewer.setLabelProvider(new DiffLabelProvider());
		viewer.addDoubleClickListener(
				event -> openFolder(event.getSelection()));
		viewer.addOpenListener(event -> fireOpenEvent(event));
		if (input != null) {
			viewer.setInput(input);
		}
		createContextMenu(viewer);
		if (selection != null) {
			viewer.setSelection(selection);
		}
	}

	/**
	 * Sets the input of the page to the given {@link IDocument}.
	 *
	 * @param input
	 *            to set for the page
	 */
	public void setInput(IDocument input) {
		this.input = input;
		TreeViewer viewer = getTreeViewerChecked();
		if (viewer != null) {
			viewer.setInput(input);
		}
	}

	@Override
	public void setSelection(ISelection selection) {
		this.selection = selection;
		TreeViewer viewer = getTreeViewerChecked();
		if (viewer != null) {
			super.setSelection(selection);
		}
	}

	private TreeViewer getTreeViewerChecked() {
		TreeViewer viewer = getTreeViewer();
		if (viewer == null || viewer.getControl() == null
				|| viewer.getControl().isDisposed()) {
			return null;
		}
		return viewer;
	}

	/**
	 * Adds a listener for selection-open in this page's viewer. Has no effect
	 * if an identical listener is already registered.
	 *
	 * @param listener
	 *            to add to the page'sviewer
	 */
	public void addOpenListener(IOpenListener listener) {
		openListeners.addIfAbsent(listener);
	}

	/**
	 * Removes the given open listener from this page's viewer. Has no effect if
	 * the listener is not registered.
	 *
	 * @param listener
	 *            to remove from this page's viewer.
	 */
	public void removeOpenListener(IOpenListener listener) {
		openListeners.remove(listener);
	}

	private void openFolder(ISelection currentSelection) {
		if (currentSelection instanceof IStructuredSelection) {
			Object currentNode = ((IStructuredSelection) currentSelection)
					.getFirstElement();
			if (currentNode instanceof DiffContentProvider.Folder) {
				TreeViewer viewer = getTreeViewerChecked();
				if (viewer != null) {
					viewer.setExpandedState(currentNode,
							!viewer.getExpandedState(currentNode));
				}
			}
		}
	}

	private void fireOpenEvent(OpenEvent event) {
		for (IOpenListener listener : openListeners) {
			SafeRunnable.run(new SafeRunnable() {

				@Override
				public void run() {
					listener.open(event);
				}
			});
		}
	}

	private void createContextMenu(TreeViewer viewer) {
		MenuManager contextMenu = new MenuManager();
		contextMenu.setRemoveAllWhenShown(true);
		contextMenu.addMenuListener(menuManager -> {
			setFocus();
			Collection<FileDiffRegion> selected = getSelectedFileDiffs();
			if (selected.isEmpty()) {
				return;
			}
			Collection<FileDiffRegion> haveNew = selected.stream()
					.filter(diff -> !diff.getDiff().getChange()
							.equals(DiffEntry.ChangeType.DELETE))
					.collect(Collectors.toList());
			Collection<FileDiffRegion> haveOld = selected.stream()
					.filter(diff -> !diff.getDiff().getChange()
							.equals(DiffEntry.ChangeType.ADD))
					.collect(Collectors.toList());
			Collection<FileDiffRegion> existing = haveNew.stream()
					.filter(diff -> new Path(diff.getRepository().getWorkTree()
							.getAbsolutePath())
									.append(diff.getDiff().getNewPath())
									.toFile().exists())
					.collect(Collectors.toList());
			if (!existing.isEmpty()) {
				menuManager.add(new Action(
						UIText.CommitFileDiffViewer_OpenWorkingTreeVersionInEditorMenuLabel) {

					@Override
					public void run() {
						for (FileDiffRegion fileDiff : existing) {
							File file = new Path(fileDiff.getRepository()
									.getWorkTree().getAbsolutePath()).append(
											fileDiff.getDiff().getNewPath())
											.toFile();
							DiffViewer.openFileInEditor(file, -1);
						}
					}
				});
			}
			if (!haveNew.isEmpty()) {
				menuManager.add(new Action(
						UIText.CommitFileDiffViewer_OpenInEditorMenuLabel) {

					@Override
					public void run() {
						for (FileDiffRegion fileDiff : haveNew) {
							DiffViewer.openInEditor(fileDiff.getRepository(),
									fileDiff.getDiff(), DiffEntry.Side.NEW, -1);
						}
					}
				});
			}
			if (!haveOld.isEmpty()) {
				menuManager.add(new Action(
						UIText.CommitFileDiffViewer_OpenPreviousInEditorMenuLabel) {

					@Override
					public void run() {
						for (FileDiffRegion fileDiff : haveOld) {
							DiffViewer.openInEditor(fileDiff.getRepository(),
									fileDiff.getDiff(), DiffEntry.Side.OLD, -1);
						}
					}
				});
			}
			if (selected.size() == 1) {
				menuManager.add(new Separator());
				menuManager.add(new Action(
						UIText.CommitFileDiffViewer_CompareMenuLabel) {

					@Override
					public void run() {
						FileDiffRegion fileDiff = selected.iterator().next();
						DiffViewer.showTwoWayFileDiff(fileDiff.getRepository(),
								fileDiff.getDiff());
					}
				});
			}
		});
		Menu menu = contextMenu.createContextMenu(viewer.getTree());
		viewer.getTree().setMenu(menu);
	}

	private Collection<FileDiffRegion> getSelectedFileDiffs() {
		ISelection currentSelection = getSelection();
		List<FileDiffRegion> result = new ArrayList<>();
		if (!currentSelection.isEmpty() && currentSelection instanceof StructuredSelection) {
			for (Object selected : ((StructuredSelection) currentSelection).toList()) {
				if (selected instanceof FileDiffRegion
						&& !((FileDiffRegion) selected).getDiff()
								.isSubmodule()) {
					result.add((FileDiffRegion) selected);
				}
			}
		}
		return result;
	}

	private static class DiffContentProvider implements ITreeContentProvider {

		private static final Object[] NOTHING = new Object[0];

		public static class Folder {
			public String name;

			public List<FileDiffRegion> files;
		}

		private HashMap<String, Folder> folders = new LinkedHashMap<>();

		private Map<FileDiffRegion, Folder> parents = new HashMap<>();

		@Override
		public void inputChanged(Viewer viewer, Object oldInput,
				Object newInput) {
			folders.clear();
			parents.clear();
			if (newInput instanceof DiffDocument) {
				computeFolders(((DiffDocument) newInput).getFileRegions());
			}
		}

		@Override
		public void dispose() {
			folders.clear();
			parents.clear();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof DiffDocument) {
				return folders.values().toArray();
			}
			return NOTHING;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof Folder) {
				return ((Folder) parentElement).files.toArray();
			}
			return NOTHING;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof FileDiffRegion) {
				return parents.get(element);
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return (element instanceof Folder);
		}

		private void computeFolders(FileDiffRegion[] ranges) {
			for (FileDiffRegion range : ranges) {
				String path = range.getDiff().getPath();
				int i = path.lastIndexOf('/');
				if (i > 0) {
					path = path.substring(0, i);
				} else {
					path = "/"; //$NON-NLS-1$
				}
				Folder folder = folders.get(path);
				if (folder == null) {
					folder = new Folder();
					folder.name = path;
					folder.files = new ArrayList<>();
					folders.put(path, folder);
				}
				folder.files.add(range);
				parents.put(range, folder);
			}
		}
	}

	private static class DiffLabelProvider extends LabelProvider {

		private final Image FOLDER = PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJ_FOLDER);

		private final ResourceManager resourceManager = new LocalResourceManager(
				JFaceResources.getResources());

		public DiffLabelProvider() {
			super();
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof DiffContentProvider.Folder) {
				return FOLDER;
			}
			if (element instanceof FileDiffRegion) {
				FileDiff diff = ((FileDiffRegion) element).getDiff();
				return (Image) resourceManager
						.get(diff.getImageDescriptor(diff));
			}
			return super.getImage(element);
		}

		@Override
		public String getText(Object element) {
			if (element instanceof DiffContentProvider.Folder) {
				return ((DiffContentProvider.Folder) element).name;
			}
			if (element instanceof FileDiffRegion) {
				FileDiff diff = ((FileDiffRegion) element).getDiff();
				String path = diff.getPath();
				int i = path.lastIndexOf('/');
				return path.substring(i + 1);
			}
			return super.getText(element);
		}

		@Override
		public void dispose() {
			resourceManager.dispose();
			super.dispose();
		}
	}

}

