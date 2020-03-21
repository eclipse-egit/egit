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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.DiffRegionFormatter.FileDiffRegion;
import org.eclipse.egit.ui.internal.history.CommitFileDiffViewer.CheckoutAction;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SearchPattern;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

/**
 * A {@link ContentOutlinePage} for the {DiffEditorPage}, displaying an outline
 * for {@link DiffDocument}s.
 */
public class DiffEditorOutlinePage extends ContentOutlinePage {

	private IDocument input;

	private CopyOnWriteArrayList<IOpenListener> openListeners = new CopyOnWriteArrayList<>();

	private ISelection selection;

	private ActionHandler collapseHandler;

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		TreeViewer viewer = getTreeViewer();
		viewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		viewer.setUseHashlookup(true);
		viewer.setContentProvider(new DiffContentProvider());
		viewer.setLabelProvider(new DiffLabelProvider());
		viewer.setComparator(
				new ViewerComparator(CommonUtils.STRING_ASCENDING_COMPARATOR) {
					@Override
					public int category(Object element) {
						if (element instanceof DiffContentProvider.Folder) {
							return 0;
						} else {
							return 1;
						}
					}
				});
		viewer.addDoubleClickListener(
				event -> openFolder(event.getSelection()));
		viewer.addOpenListener(this::fireOpenEvent);
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
			List<FileDiffRegion> haveNew = selected.stream()
					.filter(diff -> !diff.getDiff().getChange()
							.equals(DiffEntry.ChangeType.DELETE))
					.collect(Collectors.toList());
			List<FileDiffRegion> haveOld = selected.stream()
					.filter(diff -> !diff.getDiff().getChange()
							.equals(DiffEntry.ChangeType.ADD))
					.collect(Collectors.toList());
			List<FileDiffRegion> existing = haveNew.stream()
					.filter(diff -> new Path(diff.getDiff().getRepository()
							.getWorkTree().getAbsolutePath())
									.append(diff.getDiff().getNewPath())
									.toFile().exists())
					.collect(Collectors.toList());
			if (!existing.isEmpty()) {
				menuManager.add(new Action(
						UIText.CommitFileDiffViewer_OpenWorkingTreeVersionInEditorMenuLabel) {

					@Override
					public void run() {
						for (FileDiffRegion fileDiff : existing) {
							File file = new Path(
									fileDiff.getDiff().getRepository()
											.getWorkTree().getAbsolutePath())
													.append(fileDiff.getDiff()
															.getNewPath())
													.toFile();
							DiffViewer.openFileInEditor(file, -1);
						}
					}
				});
			}
			if (!haveNew.isEmpty()) {
				RevCommit commit = haveNew.get(0).getDiff().getCommit();
				String title = MessageFormat.format(
						UIText.CommitFileDiffViewer_OpenInEditorMenuWithCommitLabel,
						Utils.getShortObjectId(commit));
				String tooltip = MessageFormat.format(
						UIText.CommitFileDiffViewer_OpenInEditorMenuTooltip,
						UIUtils.menuText(commit.getShortMessage(), 80));
				IAction action = new Action(title) {

					@Override
					public void run() {
						for (FileDiffRegion fileDiff : haveNew) {
							DiffViewer.openInEditor(fileDiff.getDiff(),
									DiffEntry.Side.NEW, -1);
						}
					}
				};
				if (tooltip != null) {
					action.setToolTipText(tooltip);
				}
				menuManager.add(action);
			}
			if (!haveOld.isEmpty()) {
				FileDiff diff = haveOld.get(0).getDiff();
				RevCommit commit = diff.getCommit();
				RevCommit base = diff.getBase();
				String msg;
				if (base == null || base.equals(commit.getParent(0))) {
					msg = UIText.CommitFileDiffViewer_OpenPreviousInEditorMenuWithCommitLabel;
					if (base == null) {
						base = commit.getParent(0);
					}
				} else {
					msg = UIText.CommitFileDiffViewer_OpenBaseInEditorMenuWithCommitLabel;
				}

				String title = MessageFormat.format(msg,
						Utils.getShortObjectId(base));
				String tooltip = MessageFormat.format(
						UIText.CommitFileDiffViewer_OpenInEditorMenuTooltip,
						UIUtils.menuText(base.getShortMessage(), 80));
				IAction action = new Action(title) {

					@Override
					public void run() {
						for (FileDiffRegion fileDiff : haveOld) {
							DiffViewer.openInEditor(fileDiff.getDiff(),
									DiffEntry.Side.OLD, -1);
						}
					}
				};
				if (tooltip != null) {
					action.setToolTipText(tooltip);
				}
				menuManager.add(action);
			}
			if (!haveNew.isEmpty()) {
				boolean hasFiles = haveNew.stream()
						.anyMatch(d -> !d.getDiff().isSubmodule());
				if (hasFiles) {
					menuManager.add(new Separator());
					CheckoutAction action = new CheckoutAction(
							this::getStructuredSelection);
					menuManager.add(action);
					action.setEnabled(haveNew.iterator().next().getDiff()
							.getRepository().getRepositoryState()
							.equals(RepositoryState.SAFE));
				}
			}
			if (selected.size() == 1 && !haveNew.isEmpty()
					&& !haveOld.isEmpty()) {
				// "Compare with previous" makes only sense if there are
				// both a new and a previous version.
				FileDiff diff = haveNew.get(0).getDiff();
				RevCommit base = diff.getBase();
				String title;
				if (base == null
						|| base.equals(diff.getCommit().getParent(0))) {
					title = UIText.CommitFileDiffViewer_CompareMenuLabel;
				} else {
					title = UIText.CommitFileDiffViewer_CompareSideBySideMenuLabel;
				}
				menuManager.add(new Separator());
				menuManager.add(new Action(title) {

					@Override
					public void run() {
						FileDiffRegion fileDiff = selected.iterator().next();
						DiffViewer.showTwoWayFileDiff(fileDiff.getDiff());
					}
				});
			}
		});
		Menu menu = contextMenu.createContextMenu(viewer.getTree());
		viewer.getTree().setMenu(menu);
	}

	private IStructuredSelection getStructuredSelection() {
		ISelection currentSelection = getSelection();
		if (currentSelection instanceof IStructuredSelection) {
			return (IStructuredSelection) currentSelection;
		}
		return StructuredSelection.EMPTY;
	}

	private Collection<FileDiffRegion> getSelectedFileDiffs() {
		IStructuredSelection currentSelection = getStructuredSelection();
		List<FileDiffRegion> result = new ArrayList<>();
		if (!currentSelection.isEmpty()) {
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

	@Override
	public void setActionBars(IActionBars actionBars) {
		super.setActionBars(actionBars);
		addToolbarActions(actionBars.getToolBarManager());
	}

	private void addToolbarActions(IToolBarManager toolbarManager) {
		Action collapseAction = new Action(UIText.UIUtils_CollapseAll,
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
						ISharedImages.IMG_ELCL_COLLAPSEALL)) {
			@Override
			public void run() {
				UIUtils.collapseAll(getTreeViewer());
			}
		};
		collapseAction.setActionDefinitionId(
				IWorkbenchCommandConstants.NAVIGATE_COLLAPSE_ALL);
		collapseHandler = new ActionHandler(collapseAction);
		IHandlerService handlerService = getSite()
				.getService(IHandlerService.class);
		handlerService.activateHandler(collapseAction.getActionDefinitionId(),
				collapseHandler);

		Action toggleTreeModeAction = new Action(
				UIText.DiffEditor_OutlineTreeToggle) {
			@Override
			public void run() {
				updateOutlineTreeMode(true, this);
			}
		};
		updateOutlineTreeMode(false, toggleTreeModeAction);
		toolbarManager.add(collapseAction);
		toolbarManager.add(toggleTreeModeAction);
	}

	private void updateOutlineTreeMode(boolean doToggle, Action toggleAction) {
		IPreferenceStore preference = Activator.getDefault()
				.getPreferenceStore();
		String prefID = "DiffEditorOutline.compactTree"; //$NON-NLS-1$
		boolean compact = preference.getBoolean(prefID);
		if (doToggle) {
			compact = !compact;
		}
		((DiffContentProvider) getTreeViewer().getContentProvider())
				.setCompactTree(compact);
		toggleAction
				.setImageDescriptor(compact ? UIIcons.COMPACT : UIIcons.FLAT);
		preference.setValue(prefID, compact);
		getTreeViewer().setInput(getTreeViewer().getInput());
	}

	@Override
	public void dispose() {
		if (collapseHandler != null) {
			collapseHandler.dispose();
		}
		super.dispose();
	}

	private static class DiffContentProvider implements ITreeContentProvider {

		private static final Object[] NOTHING = new Object[0];

		private static String SLASH = "/"; //$NON-NLS-1$

		private boolean compactTree = false;

		public static class Folder {

			public Folder(String name) {
				this.name = name;
				this.folders = new ArrayList<>();
				this.files = new ArrayList<>();
			}

			public String name;

			public List<Folder> folders;

			public List<FileDiffRegion> files;
		}

		private HashMap<String, Folder> rootFolders = new LinkedHashMap<>();

		private Map<Object, Folder> parents = new HashMap<>();

		@Override
		public void inputChanged(Viewer viewer, Object oldInput,
				Object newInput) {
			rootFolders.clear();
			parents.clear();
			if (newInput instanceof DiffDocument) {
				computeFolders(((DiffDocument) newInput).getFileRegions());
			}
		}

		public void setCompactTree(boolean compactTree) {
			this.compactTree = compactTree;
		}

		@Override
		public void dispose() {
			rootFolders.clear();
			parents.clear();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof DiffDocument) {
				return rootFolders.values().toArray();
			}
			return NOTHING;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof Folder) {
				List<Object> children = new ArrayList<>();
				Folder parentFolder = (Folder) parentElement;
				children.addAll(parentFolder.folders);
				children.addAll(parentFolder.files);
				return children.toArray();
			}
			return NOTHING;
		}

		@Override
		public Object getParent(Object element) {
			return parents.get(element);
		}

		@Override
		public boolean hasChildren(Object element) {
			return (element instanceof Folder);
		}

		private void computeFolders(FileDiffRegion[] ranges) {
			if (compactTree) {
				for (FileDiffRegion range : ranges) {
					String path = range.getDiff().getPath();
					Folder folder = computeRootFolder(SLASH);
					String[] segments = path.split(SLASH);
					for (int i = 0; i < segments.length - 1; i++) {
						String segment = segments[i];
						Folder newFolder = null;
						for (Folder childFolder : folder.folders) {
							if (childFolder.name.equals(segment)) {
								newFolder = childFolder;
								break;
							}
						}
						if (newFolder == null) {
							newFolder = new Folder(segment);
							folder.folders.add(newFolder);
						}
						parents.put(newFolder, folder);
						folder = newFolder;
					}
					folder.files.add(range);
					parents.put(range, folder);
				}
				compactify();
			} else {
				for (FileDiffRegion range : ranges) {
					String path = range.getDiff().getPath();
					int i = path.lastIndexOf('/');
					if (i > 0) {
						path = path.substring(0, i);
					} else {
						path = SLASH;
					}
					Folder folder = computeRootFolder(path);
					folder.files.add(range);
					parents.put(range, folder);
				}
			}
		}

		private void compactify() {
			Folder root = rootFolders.get(SLASH);
			compactify(root);
			if (root.files.isEmpty()) {
				rootFolders.clear();
				root.folders.forEach(f -> {
					parents.remove(f);
					rootFolders.put(f.name, f);
				});
			}
		}

		private void compactify(Folder folder) {
			if (folder.files.isEmpty() && folder.folders.size() == 1) {
				Folder parent = parents.get(folder);
				Folder child = folder.folders.get(0);
				if (parent != null) {
					child.name = folder.name + SLASH + child.name;
					parent.folders.remove(folder);
					parent.folders.add(child);
					parents.remove(folder);
					parents.put(child, parent);
				}
			}
			new ArrayList<>(folder.folders).forEach(f -> compactify(f));
		}

		private Folder computeRootFolder(String path) {
			return rootFolders.computeIfAbsent(path, Folder::new);
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
				ImageDescriptor desc = diff.getBaseImageDescriptor();
				if (desc == null) {
					return null;
				}
				Image image = UIIcons.getImage(resourceManager, desc);
				desc = diff.getImageDcoration();
				if (desc != null) {
					image = UIIcons.getImage(resourceManager,
							new DecorationOverlayIcon(image, desc,
									IDecoration.BOTTOM_RIGHT));
				}
				return image;
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

	/**
	 * Opens a quick outline analgous to the outline page with tree filter
	 * capability.
	 *
	 * @param document
	 *            the diff document
	 * @param selectionProvider
	 *            the editor's selection provider for revealing the selected
	 *            region
	 */
	static void openQuickOutline(IDocument document,
			ISelectionProvider selectionProvider) {
		new QuickOutlinePopup(document, selectionProvider).open();
	}

	// The outline popup is adapted from Xtext's
	// org.eclipse.xtext.ui.editor.outline.quickoutline.QuickOutlinePopup
	// with a simplified filtering
	private static class QuickOutlinePopup extends PopupDialog {
		private DiffEditorOutlinePage delegate;

		private ISelectionProvider selectionProvider;

		private Text filterText;


		public QuickOutlinePopup(IDocument document,
				ISelectionProvider selectionProvider) {
			this(null, document, selectionProvider);
		}

		public QuickOutlinePopup(Shell parent, IDocument document,
				ISelectionProvider selectionProvider) {
			super(parent, SWT.RESIZE, true, false, true, true, true,
					UIText.DiffEditor_QuickOutlineAction,
					UIText.DiffEditor_QuickOutlineFilterDescription);
			delegate = new DiffEditorOutlinePage();
			delegate.setInput(document);
			this.selectionProvider = selectionProvider;
		}

		@Override
		protected Control createTitleControl(Composite parent) {
			filterText = createFilterText(parent);
			return filterText;
		}

		@Override
		protected Control getFocusControl() {
			return filterText;
		}

		private Text createFilterText(Composite parent) {
			filterText = new Text(parent, SWT.NONE);
			Dialog.applyDialogFont(filterText);

			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
					.grab(true, false).applyTo(filterText);

			filterText.addKeyListener(new KeyAdapter() {

				@Override
				public void keyPressed(KeyEvent e) {
					if (e.keyCode == SWT.CR || e.character == '\r'
							|| e.character == '\n') {// return
						gotoSelectedElement();
						close();
					} else if (e.keyCode == SWT.ARROW_DOWN) {
						delegate.getTreeViewer().getTree().setFocus();
						selectFirst();
					} else if (e.character == SWT.ESC) {
						close();
					}
				}
			});
			filterText.setMessage(UIText.DiffEditor_QuickOutlineFilterHint);
			filterText.addModifyListener(e -> {
				TreeViewer viewer = delegate.getTreeViewer();
				try {
					viewer.getControl().setRedraw(false);
					String text = filterText.getText();
					if (StringUtils.isEmptyOrNull(text)) {
						viewer.setFilters();
					} else {
						final SearchPattern pattern = new SearchPattern();
						pattern.setPattern(text);
						viewer.setFilters(new ViewerFilter() {

							@Override
							public boolean select(Viewer v,
									Object parentElement, Object element) {
								return isMatch(pattern, element);
							}
						});
					}
					viewer.expandAll();
					selectFirst();
				} finally {
					viewer.getControl().setRedraw(true);
				}
			});
			return filterText;
		}

		private void selectFirst() {
			TreeViewer viewer = delegate.getTreeViewer();
			Tree tree = viewer.getTree();
			if (tree.getItemCount() > 0) {
				TreeItem folder = tree.getItem(0);
				if (folder.getItemCount() > 0) {
					TreeItem file = folder.getItem(0);
					viewer.setSelection(
							new StructuredSelection(file.getData()));
					gotoSelectedElement();
				}
			}
		}

		private boolean isMatch(SearchPattern pattern, Object treeElement) {
			if (treeElement instanceof FileDiffRegion) {
				String path = ((FileDiffRegion) treeElement).getDiff()
						.getPath();
				String fileName = path;
				int lastSegmentIndex = path.lastIndexOf('/');
				if (lastSegmentIndex >= 0) {
					fileName = path.substring(lastSegmentIndex + 1);
					if (pattern.matches(fileName)) {
						return true;
					}
				}
				return pattern.matches(path);
			} else if (treeElement instanceof DiffContentProvider.Folder) {
				DiffContentProvider.Folder folder = (DiffContentProvider.Folder) treeElement;
				return folder.files.stream()
						.anyMatch(r -> isMatch(pattern, r))
						|| folder.folders.stream()
								.anyMatch(f -> isMatch(pattern, f));
			}
			return false;
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			delegate.createControl(parent);

			final Tree tree = delegate.getTreeViewer().getTree();
			tree.addKeyListener(new KeyAdapter() {

				@Override
				public void keyPressed(KeyEvent e) {
					if (e.character == SWT.ESC) {
						close();
					}
				}
			});

			tree.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					selectionProvider.setSelection(
							delegate.getTreeViewer().getSelection());
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
					ITreeSelection selection = delegate.getTreeViewer()
							.getStructuredSelection();
					if (selection.getFirstElement() instanceof FileDiffRegion) {
						close();
					}
				}
			});
			tree.setMenu(null);
			return delegate.getTreeViewer().getControl();
		}

		private void gotoSelectedElement() {
			IStructuredSelection sel = delegate.getTreeViewer()
					.getStructuredSelection();
			if (!sel.isEmpty()) {
				selectionProvider.setSelection(sel);
			}
		}

		@Override
		public boolean close() {
			delegate.dispose();
			return super.close();
		}

		@Override
		protected IDialogSettings getDialogSettings() {
			String sectionName = "diffEditor.quickoutline"; //$NON-NLS-1$

			IDialogSettings settings = Activator.getDefault()
					.getDialogSettings().getSection(sectionName);
			if (settings == null) {
				settings = Activator.getDefault().getDialogSettings()
						.addNewSection(sectionName);
			}

			return settings;
		}

		@Override
		protected Point getDefaultLocation(Point initialSize) {
			IEditorPart editor = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage()
					.getActiveEditor();
			Control widget = editor.getAdapter(Control.class);
			Point size = widget.getSize();

			Point popupLocation = new Point((size.x / 2) - (initialSize.x / 2),
					(size.y / 2) - (initialSize.y / 2));
			return widget.toDisplay(popupLocation);
		}
	}
}