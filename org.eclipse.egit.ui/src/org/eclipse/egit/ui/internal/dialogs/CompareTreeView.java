/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.AdaptableFileTreeIterator;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.EditableRevision;
import org.eclipse.egit.ui.internal.FileRevisionTypedElement;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.egit.ui.internal.LocalFileRevision;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView.PathNode.Type;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.OrTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;

/**
 * Shows a tree when opening compare on an {@link IContainer}, or a
 * {@link Repository}
 * <p>
 * If the input is an {@link IContainer}, the tree is similar to the tree shown
 * by the PackageExplorer (based on {@link WorkbenchLabelProvider} and
 * {@link WorkbenchContentProvider}, otherwise a simple tree representing files
 * and folders is used based on {@link PathNode} instances.
 */
public class CompareTreeView extends ViewPart {
	/** The "magic" right-side version to compare with the index */
	public static final String INDEX_VERSION = "%%%INDEX%%%"; //$NON-NLS-1$

	/** The View ID */
	public static final String ID = "org.eclipse.egit.ui.CompareTreeView"; //$NON-NLS-1$

	private static final Image FILE_IMAGE = PlatformUI.getWorkbench()
			.getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);

	private static final Image FOLDER_IMAGE = PlatformUI.getWorkbench()
			.getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

	private final Image SAME_CONTENT = UIIcons.ELCL16_SYNCED.createImage();

	private Image ADDED = UIIcons.ELCL16_ADD.createImage();

	private Image DELETED = UIIcons.ELCL16_DELETE.createImage();

	private RepositoryMapping repositoryMapping;

	private TreeViewer leftTree;

	private TreeViewer rightTree;

	private IWorkbenchAction showEqualsAction;

	private Map<IPath, GitFileRevision> rightVersionMap = new HashMap<IPath, GitFileRevision>();

	private Map<IPath, GitFileRevision> leftVersionMap = new HashMap<IPath, GitFileRevision>();

	private Set<IPath> leftOnly = new HashSet<IPath>();

	private Set<IPath> equalIds = new HashSet<IPath>();

	private Set<IPath> leftPathsWithChildren = new HashSet<IPath>();

	private Set<IPath> rightPathsWithChildren = new HashSet<IPath>();

	private Set<IPath> rightOnly = new HashSet<IPath>();

	private List<IWorkbenchAction> actionsToDispose = new ArrayList<IWorkbenchAction>();

	private final IPersistentPreferenceStore store = (IPersistentPreferenceStore) Activator
			.getDefault().getPreferenceStore();

	private Object input;

	private String rightVersion;

	private String leftVersion;

	private boolean showAddedOnly;

	private boolean showDeletedOnly;

	private boolean showEquals = false;

	@Override
	public void createPartControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		SashForm mainSash = new SashForm(main, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(mainSash);

		leftTree = new TreeViewer(mainSash, SWT.BORDER);
		leftTree.setContentProvider(new LeftTreeContentProvider());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(
				leftTree.getTree());

		leftTree.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				reactOnSelection(event);
			}
		});

		leftTree.addTreeListener(new ITreeViewerListener() {
			public void treeExpanded(TreeExpansionEvent event) {
				reactOnExpand(event);
			}

			public void treeCollapsed(TreeExpansionEvent event) {
				reactOnCollapse(event);
			}
		});

		leftTree.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				reactOnOpen(event);
			}
		});
		rightTree = new TreeViewer(mainSash, SWT.BORDER);
		rightTree.setContentProvider(new RightTreeContentProvider());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(
				rightTree.getTree());

		rightTree.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				reactOnSelection(event);
			}
		});

		rightTree.addTreeListener(new ITreeViewerListener() {
			public void treeExpanded(TreeExpansionEvent event) {
				reactOnExpand(event);
			}

			public void treeCollapsed(TreeExpansionEvent event) {
				reactOnCollapse(event);
			}
		});

		rightTree.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				reactOnOpen(event);
			}
		});
		leftTree.getTree().setEnabled(false);
		rightTree.getTree().setEnabled(false);
		createActions();
	}

	private void createActions() {
		IWorkbenchAction reuseCompareEditorAction = new CompareUtils.ReuseCompareEditorAction();
		actionsToDispose.add(reuseCompareEditorAction);
		getViewSite().getActionBars().getMenuManager().add(
				reuseCompareEditorAction);

		IWorkbenchAction addedOnlyAction = new BooleanPrefAction(store,
				UIPreferences.TREE_COMPARE_ADDED_ONLY,
				UIText.CompareTreeView_AddedOnlyTooltip) {
			@Override
			void apply(boolean value) {
				buildTrees();
			}
		};
		addedOnlyAction.setImageDescriptor(UIIcons.ELCL16_ADD);
		addedOnlyAction.setEnabled(false);
		actionsToDispose.add(addedOnlyAction);
		getViewSite().getActionBars().getToolBarManager().add(addedOnlyAction);

		showEqualsAction = new BooleanPrefAction(store,
				UIPreferences.TREE_COMPARE_SHOW_EQUALS,
				UIText.CompareTreeView_EqualFilesTooltip) {
			@Override
			void apply(boolean value) {
				buildTrees();
			}
		};
		showEqualsAction.setImageDescriptor(UIIcons.ELCL16_SYNCED);
		showEqualsAction.setEnabled(false);
		actionsToDispose.add(showEqualsAction);
		getViewSite().getActionBars().getToolBarManager().add(showEqualsAction);

		IWorkbenchAction deletedOnlyAction = new BooleanPrefAction(store,
				UIPreferences.TREE_COMPARE_DELETED_ONLY,
				UIText.CompareTreeView_DeletedOnlyTooltip) {
			@Override
			void apply(boolean value) {
				buildTrees();
			}
		};
		deletedOnlyAction.setImageDescriptor(UIIcons.ELCL16_DELETE);
		deletedOnlyAction.setEnabled(false);
		actionsToDispose.add(deletedOnlyAction);
		getViewSite().getActionBars().getToolBarManager()
				.add(deletedOnlyAction);

		IAction expandAllAction = new Action(
				UIText.CompareTreeView_CollapseAllTooltip) {
			@Override
			public void run() {
				leftTree.collapseAll();
				rightTree.collapseAll();
			}
		};
		expandAllAction.setImageDescriptor(UIIcons.COLLAPSEALL);
		getViewSite().getActionBars().getToolBarManager().add(expandAllAction);
	}

	private void reactOnOpen(OpenEvent event) {
		Object selected = ((IStructuredSelection) event.getSelection())
				.getFirstElement();
		ITypedElement left;
		ITypedElement right;
		if (selected instanceof IContainer) {
			// open/close folder
			TreeViewer tv = (TreeViewer) event.getViewer();
			tv.setExpandedState(selected, !tv.getExpandedState(selected));
			return;
		} else if (selected instanceof IFile) {
			final IFile res = (IFile) selected;
			left = new EditableRevision(new LocalFileRevision(res)) {
				@Override
				public void setContent(final byte[] newContent) {
					try {
						PlatformUI.getWorkbench().getProgressService().run(
								false, false, new IRunnableWithProgress() {
									public void run(IProgressMonitor myMonitor)
											throws InvocationTargetException,
											InterruptedException {
										try {
											res.setContents(
													new ByteArrayInputStream(
															newContent), false,
													true, myMonitor);
										} catch (CoreException e) {
											throw new InvocationTargetException(
													e);
										}
									}
								});
					} catch (InvocationTargetException e) {
						Activator.handleError(e.getTargetException()
								.getMessage(), e.getTargetException(), true);
					} catch (InterruptedException e) {
						// ignore here
					}
				}
			};
			GitFileRevision rightRevision = rightVersionMap.get(new Path(
					repositoryMapping.getRepoRelativePath(res)));
			if (rightRevision == null)
				right = new GitCompareFileRevisionEditorInput.EmptyTypedElement(
						NLS
								.bind(
										UIText.CompareTreeView_ItemNotFoundInVersionMessage,
										res.getName(), getRightVersion()));
			else
				right = new FileRevisionTypedElement(rightRevision);
			GitCompareFileRevisionEditorInput compareInput = new GitCompareFileRevisionEditorInput(
					left, right, PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage());
			CompareUtils.openInCompare(PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage(), compareInput);
		} else if (selected instanceof GitFileRevision) {
			GitFileRevision rightRevision = (GitFileRevision) selected;
			left = new GitCompareFileRevisionEditorInput.EmptyTypedElement(NLS
					.bind(UIText.CompareTreeView_ItemNotFoundInVersionMessage,
							rightRevision.getName(), getLeftVersion()));
			right = new FileRevisionTypedElement(rightRevision);
		} else if (selected instanceof PathNode) {
			PathNode node = (PathNode) selected;
			switch (node.type) {
			case FILE_BOTH_SIDES_DIFFER:
				// fall through
			case FILE_BOTH_SIDES_SAME: {
				// open a compare editor with both sides filled
				GitFileRevision rightRevision = rightVersionMap.get(node.path);
				right = new FileRevisionTypedElement(rightRevision);
				GitFileRevision leftRevision = leftVersionMap.get(node.path);
				left = new FileRevisionTypedElement(leftRevision);
				break;
			}
			case FILE_DELETED: {
				// open compare editor with left side empty
				GitFileRevision rightRevision = rightVersionMap.get(node.path);
				right = new FileRevisionTypedElement(rightRevision);
				left = new GitCompareFileRevisionEditorInput.EmptyTypedElement(
						NLS
								.bind(
										UIText.CompareTreeView_ItemNotFoundInVersionMessage,
										rightRevision.getName(),
										getLeftVersion()));
				break;
			}
			case FILE_ADDED: {
				// open compare editor with right side empty
				GitFileRevision leftRevision = leftVersionMap.get(node.path);
				left = new FileRevisionTypedElement(leftRevision);
				right = new GitCompareFileRevisionEditorInput.EmptyTypedElement(
						NLS
								.bind(
										UIText.CompareTreeView_ItemNotFoundInVersionMessage,
										leftRevision.getName(),
										getRightVersion()));
				break;
			}
			case FOLDER:
				// open/close folder
				TreeViewer tv = (TreeViewer) event.getViewer();
				tv.setExpandedState(selected, !tv.getExpandedState(selected));
				return;
			default:
				return;
			}

		} else
			return;

		GitCompareFileRevisionEditorInput compareInput = new GitCompareFileRevisionEditorInput(
				left, right, PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage());
		CompareUtils.openInCompare(PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage(), compareInput);
	}

	private String getLeftVersion() {
		// null in case of Workspace compare
		if (leftVersion == null)
			return UIText.CompareTreeView_WorkspaceVersionText;
		return leftVersion;
	}

	private String getRightVersion() {
		return rightVersion;
	}

	@Override
	public void setFocus() {
		leftTree.getTree().setFocus();
	}

	/**
	 * Used to compare the working tree with another version
	 *
	 * @param input
	 *            the {@link IResource}s from which to build the tree
	 * @param rightVersion
	 *            a {@link Ref} name or {@link RevCommit} id or
	 *            {@link #INDEX_VERSION}
	 */
	public void setInput(final IResource[] input, String rightVersion) {
		setResourceInput(input);
		this.leftVersion = null;
		this.rightVersion = rightVersion;
		buildTrees();
		updateControls();
	}

	private void setResourceInput(final IResource[] input) {
		if (input.length > 0) {
			// we must make sure to only show the topmost resources as roots
			List<IResource> resources = new ArrayList<IResource>(input.length);
			List<IPath> allPaths = new ArrayList<IPath>(input.length);
			for (IResource originalInput : input) {
				allPaths.add(originalInput.getFullPath());
			}
			for (IResource originalInput : input) {
				boolean skip = false;
				for (IPath path : allPaths) {
					if (path.isPrefixOf(originalInput.getFullPath())
							&& path.segmentCount() < originalInput
									.getFullPath().segmentCount()) {
						skip = true;
						break;
					}
				}
				if (!skip)
					resources.add(originalInput);
			}
			this.input = resources.toArray(new IResource[resources.size()]);
		} else
			this.input = input;
	}

	/**
	 * Used to compare two versions with each other filtering by a workspace
	 * resource
	 *
	 * @param input
	 *            the {@link IResource}s from which to build the tree
	 * @param leftVersion
	 *            a {@link Ref} name or {@link RevCommit} id
	 * @param rightVersion
	 *            a {@link Ref} name or {@link RevCommit} id or
	 *            {@link #INDEX_VERSION}
	 */
	public void setInput(final IResource[] input, String leftVersion,
			String rightVersion) {
		setResourceInput(input);
		this.leftVersion = leftVersion;
		this.rightVersion = rightVersion;
		buildTrees();
		updateControls();
	}

	/**
	 * Used if no workspace filtering should be applied
	 *
	 * @param input
	 *            the {@link Repository} from which to build the tree
	 * @param leftVersion
	 *            a {@link Ref} name or {@link RevCommit} id
	 * @param rightVersion
	 *            a {@link Ref} name or {@link RevCommit} id
	 */
	public void setInput(final Repository input, String leftVersion,
			String rightVersion) {
		this.input = input;
		this.leftVersion = leftVersion;
		this.rightVersion = rightVersion;
		buildTrees();
		updateControls();
	}

	private void updateControls() {
		for (IWorkbenchAction action : actionsToDispose)
			action.setEnabled(input != null);
		leftTree.getTree().setEnabled(input != null);
		rightTree.getTree().setEnabled(input != null);
		showEqualsAction.setEnabled(!(showAddedOnly || showDeletedOnly));
		if (input == null)
			setContentDescription(UIText.CompareTreeView_NoInputText);
		else {
			String name;
			if (input instanceof IResource[]) {
				IResource[] resources = (IResource[]) input;
				if (resources.length == 1)
					name = (resources[0]).getFullPath().makeRelative()
							.toString();
				else
					name = UIText.CompareTreeView_MultipleResourcesHeaderText;
			} else if (input instanceof Repository)
				name = Activator.getDefault().getRepositoryUtil()
						.getRepositoryName(((Repository) input));
			else
				throw new IllegalStateException();
			if (leftVersion == null)
				setContentDescription(NLS
						.bind(
								UIText.CompareTreeView_ComparingWorkspaceVersionDescription,
								name,
								rightVersion.equals(INDEX_VERSION) ? UIText.CompareTreeView_IndexVersionText
										: rightVersion));
			else
				setContentDescription(NLS
						.bind(
								UIText.CompareTreeView_ComparingTwoVersionDescription,
								new String[] {
										leftVersion,
										name,
										rightVersion.equals(INDEX_VERSION) ? UIText.CompareTreeView_IndexVersionText
												: rightVersion }));
		}
	}

	private void buildTrees() {
		final Object[] wsExpaneded = leftTree.getExpandedElements();
		final Object[] gitExpanded = rightTree.getExpandedElements();
		final ISelection wsSel = leftTree.getSelection();
		final ISelection gitSel = rightTree.getSelection();
		rightTree.setInput(null);
		leftTree.setInput(null);

		if (leftVersion == null) {
			leftTree
					.setContentProvider(new LocalWorkbenchTreeContentProvider());
			leftTree.setLabelProvider(new AddingWorkbenchLabelProvider());

			rightTree
					.setContentProvider(new RepositoryWorkbenchTreeContentProvider());

			rightTree.setLabelProvider(new GitWorkbenchLabelProvider());
		} else {
			leftTree.setContentProvider(new LeftTreeContentProvider());
			leftTree.setLabelProvider(new RepositoryTreeLabelProvider());

			rightTree.setContentProvider(new RightTreeContentProvider());

			rightTree.setLabelProvider(new RepositoryTreeLabelProvider());
		}
		for (IWorkbenchAction action : actionsToDispose)
			action.setEnabled(false);

		showAddedOnly = Activator.getDefault().getPreferenceStore().getBoolean(
				UIPreferences.TREE_COMPARE_ADDED_ONLY);
		showDeletedOnly = Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.TREE_COMPARE_DELETED_ONLY);
		showEquals = Activator.getDefault().getPreferenceStore().getBoolean(
				UIPreferences.TREE_COMPARE_SHOW_EQUALS);
		final Repository repo;
		if (input instanceof IResource[]) {
			repositoryMapping = RepositoryMapping
					.getMapping(((IResource[]) input)[0]);
			if (repositoryMapping == null
					|| repositoryMapping.getRepository() == null)
				return;
			repo = repositoryMapping.getRepository();
		} else if (input instanceof Repository) {
			repo = (Repository) input;
		} else
			return;
		final RevCommit leftCommit;
		final RevCommit rightCommit;
		RevWalk rw = new RevWalk(repo);
		try {
			ObjectId commitId = repo.resolve(rightVersion);
			rightCommit = commitId != null ? rw.parseCommit(commitId) : null;
			if (leftVersion == null)
				leftCommit = null;
			else {
				commitId = repo.resolve(leftVersion);
				leftCommit = rw.parseCommit(commitId);
			}
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
			return;
		} finally {
			rw.release();
		}
		showBusy(true);
		try {
			// this does the hard work...
			new ProgressMonitorDialog(getViewSite().getShell()).run(true, true,
					new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							try {
								buildMaps(repo, leftCommit, rightCommit,
										monitor);
								PlatformUI.getWorkbench().getDisplay()
										.asyncExec(new Runnable() {
											public void run() {
												leftTree.setInput(input);
												rightTree.setInput(input);
												leftTree
														.setExpandedElements(wsExpaneded);
												rightTree
														.setExpandedElements(gitExpanded);
												leftTree.setSelection(wsSel);
												rightTree.setSelection(gitSel);
												updateControls();
											}
										});
							} catch (IOException e) {
								throw new InvocationTargetException(e);
							}
						}
					});
		} catch (InvocationTargetException e) {
			Activator.handleError(e.getTargetException().getMessage(), e
					.getTargetException(), true);
		} catch (InterruptedException e) {
			input = null;
		} finally {
			showBusy(false);
		}
	}

	private void buildMaps(Repository repository, RevCommit leftCommit,
			RevCommit rightCommit, IProgressMonitor monitor)
			throws InterruptedException, IOException {
		monitor.beginTask(UIText.CompareTreeView_AnalyzingRepositoryTaskText,
				IProgressMonitor.UNKNOWN);
		boolean useIndex = rightVersion.equals(INDEX_VERSION);
		rightOnly.clear();
		equalIds.clear();
		leftVersionMap.clear();
		rightVersionMap.clear();
		rightPathsWithChildren.clear();
		leftOnly.clear();
		leftPathsWithChildren.clear();
		boolean checkIgnored = false;
		TreeWalk tw = new TreeWalk(repository);
		try {
			int leftTreeIndex;
			if (leftCommit == null) {
				checkIgnored = true;
				leftTreeIndex = tw.addTree(new AdaptableFileTreeIterator(
						repository, ResourcesPlugin.getWorkspace().getRoot()));
			} else
				leftTreeIndex = tw.addTree(new CanonicalTreeParser(null,
						repository.newObjectReader(), leftCommit.getTree()));
			int rightTreeIndex;
			if (!useIndex)
				rightTreeIndex = tw.addTree(new CanonicalTreeParser(null,
						repository.newObjectReader(), rightCommit.getTree()));
			else
				rightTreeIndex = tw.addTree(new DirCacheIterator(repository
						.readDirCache()));

			if (input instanceof IResource[]) {
				IResource[] resources = (IResource[]) input;
				List<TreeFilter> orFilters = new ArrayList<TreeFilter>(
						resources.length);

				for (IResource resource : resources) {
					String relPath = repositoryMapping
							.getRepoRelativePath(resource);
					if (relPath.length() > 0)
						orFilters.add(PathFilter.create(relPath));
				}
				if (orFilters.size() > 1)
					tw.setFilter(OrTreeFilter.create(orFilters));
				else if (orFilters.size() == 1)
					tw.setFilter(orFilters.get(0));
			}

			tw.setRecursive(true);

			if (monitor.isCanceled())
				throw new InterruptedException();
			while (tw.next()) {
				if (monitor.isCanceled())
					throw new InterruptedException();
				AbstractTreeIterator rightVersionIterator = tw.getTree(
						rightTreeIndex, AbstractTreeIterator.class);
				AbstractTreeIterator leftVersionIterator = tw.getTree(
						leftTreeIndex, AbstractTreeIterator.class);
				if (checkIgnored
						&& leftVersionIterator != null
						&& ((WorkingTreeIterator) leftVersionIterator)
								.isEntryIgnored())
					continue;
				if (rightVersionIterator != null && leftVersionIterator != null) {
					monitor.setTaskName(leftVersionIterator
							.getEntryPathString());
					IPath currentPath = new Path(leftVersionIterator
							.getEntryPathString());
					if (!useIndex)
						rightVersionMap.put(currentPath, GitFileRevision
								.inCommit(repository, rightCommit,
										leftVersionIterator
												.getEntryPathString(), tw
												.getObjectId(rightTreeIndex)));
					else
						rightVersionMap.put(currentPath, GitFileRevision
								.inIndex(repository, leftVersionIterator
										.getEntryPathString()));
					if (leftCommit != null)
						leftVersionMap.put(currentPath, GitFileRevision
								.inCommit(repository, leftCommit,
										leftVersionIterator
												.getEntryPathString(), tw
												.getObjectId(leftTreeIndex)));
					boolean equalContent = rightVersionIterator
							.getEntryObjectId().equals(
									leftVersionIterator.getEntryObjectId());
					if (equalContent)
						equalIds.add(currentPath);

					if (equalContent && !showEquals)
						continue;

					while (currentPath.segmentCount() > 0) {
						currentPath = currentPath.removeLastSegments(1);
						boolean addedLeft = showAddedOnly
								|| !leftPathsWithChildren.add(currentPath);
						boolean addedRight = showDeletedOnly
								|| !rightPathsWithChildren.add(currentPath);
						if (addedLeft && addedRight)
							break;
					}

				} else if (leftVersionIterator != null
						&& rightVersionIterator == null) {
					monitor.setTaskName(leftVersionIterator
							.getEntryPathString());
					// only on left side
					IPath currentPath = new Path(leftVersionIterator
							.getEntryPathString());
					leftOnly.add(currentPath);
					if (leftCommit != null)
						leftVersionMap.put(currentPath, GitFileRevision
								.inCommit(repository, leftCommit,
										leftVersionIterator
												.getEntryPathString(), tw
												.getObjectId(leftTreeIndex)));
					while (currentPath.segmentCount() > 0) {
						currentPath = currentPath.removeLastSegments(1);
						if (!leftPathsWithChildren.add(currentPath))
							break;
					}

				} else if (rightVersionIterator != null
						&& leftVersionIterator == null) {
					monitor.setTaskName(rightVersionIterator
							.getEntryPathString());
					// only on right side
					IPath currentPath = new Path(rightVersionIterator
							.getEntryPathString());
					rightOnly.add(currentPath);

					if (!useIndex)
						rightVersionMap.put(currentPath, GitFileRevision
								.inCommit(repository, rightCommit,
										rightVersionIterator
												.getEntryPathString(), tw
												.getObjectId(rightTreeIndex)));
					else
						rightVersionMap.put(currentPath, GitFileRevision
								.inIndex(repository, rightVersionIterator
										.getEntryPathString()));

					while (currentPath.segmentCount() > 0) {
						currentPath = currentPath.removeLastSegments(1);
						if (!rightPathsWithChildren.add(currentPath))
							break;
					}
				}
			}
		} finally {
			tw.release();
			monitor.done();
		}
	}

	private void reactOnSelection(SelectionChangedEvent event) {
		if (rightTree.getTree().isFocusControl()
				&& !(leftTree.getSelection().equals(event.getSelection())))
			leftTree.setSelection(event.getSelection());
		else if (leftTree.getTree().isFocusControl()
				&& !(rightTree.getSelection().equals(event.getSelection())))
			rightTree.setSelection(event.getSelection());
	}

	private void reactOnCollapse(TreeExpansionEvent event) {
		if (event.getSource() == rightTree)
			leftTree.collapseToLevel(event.getElement(), 1);
		else if (event.getSource() == leftTree)
			rightTree.collapseToLevel(event.getElement(), 1);
	}

	private void reactOnExpand(TreeExpansionEvent event) {
		if (event.getSource() == rightTree)
			leftTree.expandToLevel(event.getElement(), 1);
		else if (event.getSource() == leftTree)
			rightTree.expandToLevel(event.getElement(), 1);
	}

	@Override
	public void dispose() {
		super.dispose();
		for (IWorkbenchAction action : actionsToDispose)
			action.dispose();
		ADDED.dispose();
		DELETED.dispose();
		SAME_CONTENT.dispose();
	}

	final static class PathNode {
		/** Type */
		public enum Type {
			/** File added (only on left side) */
			FILE_ADDED,
			/** File deleted (only on right side) */
			FILE_DELETED,
			/** File differs on both sides */
			FILE_BOTH_SIDES_DIFFER,
			/** File same on both sides */
			FILE_BOTH_SIDES_SAME,
			/** Folder */
			FOLDER
		}

		public final IPath path;

		public final Type type;

		/**
		 * @param path
		 * @param type
		 */
		public PathNode(IPath path, Type type) {
			this.path = path;
			this.type = type;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + path.hashCode();
			result = prime * result + type.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PathNode other = (PathNode) obj;
			if (!path.equals(other.path))
				return false;
			if (!type.equals(other.type))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return type.name() + ": " + path.toString(); //$NON-NLS-1$
		}
	}

	private final class AddingWorkbenchLabelProvider extends
			WorkbenchLabelProvider {
		@Override
		protected ImageDescriptor decorateImage(ImageDescriptor baseImage,
				Object element) {
			if (!(element instanceof IFile)) {
				return super.decorateImage(baseImage, element);
			}
			// decorate with + for files not found in the repository and = for
			// "same" files
			if (leftOnly.contains(new Path(repositoryMapping
					.getRepoRelativePath((IFile) element)))) {
				return UIIcons.ELCL16_ADD;
			}
			if (equalIds.contains(new Path(repositoryMapping
					.getRepoRelativePath((IFile) element)))) {
				return UIIcons.ELCL16_SYNCED;
			}
			return super.decorateImage(baseImage, element);
		}
	}

	/**
	 * Used to render the "Repository" side of a tree compare where the left
	 * side is the workspace
	 */
	private final class RepositoryWorkbenchTreeContentProvider extends
			WorkbenchContentProvider {

		@Override
		public Object[] getChildren(Object element) {
			boolean rebuildArray = false;
			Object[] children;
			if (element == input)
				children = (Object[]) input;
			else
				children = super.getChildren(element);
			List<Object> childList = new ArrayList<Object>(children.length);
			for (Object child : children) {
				IPath path = new Path(repositoryMapping
						.getRepoRelativePath((IResource) child));
				if (child instanceof IFile && (leftOnly.contains(path))) {
					rebuildArray = true;
					continue;
				}
				if (child instanceof IFile && showDeletedOnly
						&& !rightOnly.contains(path)) {
					rebuildArray = true;
					continue;
				}
				if (child instanceof IContainer
						&& !rightPathsWithChildren.contains(path)) {
					rebuildArray = true;
					continue;
				}
				if (!showEquals && equalIds.contains(path)) {
					rebuildArray = true;
					continue;
				}
				childList.add(child);
			}
			// mix in "right only" children
			if (element instanceof IContainer) {
				IPath containerPath = new Path(repositoryMapping
						.getRepoRelativePath((IContainer) element));
				for (IPath rightOnlyPath : rightOnly) {
					if (rightOnlyPath.removeLastSegments(1).equals(
							containerPath)) {
						childList.add(rightVersionMap.get(rightOnlyPath));
						rebuildArray = true;
					}
				}
			}
			if (rebuildArray)
				return childList.toArray();
			return children;
		}
	}

	/**
	 * Used to render the "local" (workspace) side of a tree compare where one
	 * side is the workspace
	 */
	private final class LocalWorkbenchTreeContentProvider extends
			WorkbenchContentProvider {
		@Override
		public Object[] getChildren(Object element) {
			boolean rebuildArray = false;
			Object[] children;
			if (element == input)
				children = (Object[]) input;
			else
				children = super.getChildren(element);
			List<Object> childList = new ArrayList<Object>(children.length);
			for (Object child : children) {
				IPath path = new Path(repositoryMapping
						.getRepoRelativePath((IResource) child));
				if (!showEquals && equalIds.contains(path)) {
					rebuildArray = true;
					continue;
				}
				if (child instanceof IContainer
						&& !leftPathsWithChildren.contains(path)) {
					rebuildArray = true;
					continue;
				}
				if (child instanceof IFile && showAddedOnly
						&& !leftOnly.contains(path)) {
					rebuildArray = true;
					continue;
				}
				if (!showEquals && equalIds.contains(path)) {
					rebuildArray = true;
					continue;
				}
				childList.add(child);
			}
			if (rebuildArray)
				return childList.toArray();
			return children;
		}
	}

	private static abstract class BooleanPrefAction extends Action implements
			IPropertyChangeListener, IWorkbenchAction {
		private final String prefName;

		private final IPersistentPreferenceStore store;

		BooleanPrefAction(final IPersistentPreferenceStore store,
				final String pn, final String text) {
			this.store = store;
			setText(text);
			prefName = pn;
			store.addPropertyChangeListener(this);
			setChecked(store.getBoolean(prefName));
		}

		public void run() {
			store.setValue(prefName, isChecked());
			if (store.needsSaving()) {
				try {
					store.save();
				} catch (IOException e) {
					Activator.handleError(e.getMessage(), e, false);
				}
			}
		}

		abstract void apply(boolean value);

		public void propertyChange(final PropertyChangeEvent event) {
			if (prefName.equals(event.getProperty())) {
				setChecked(store.getBoolean(prefName));
				apply(isChecked());
			}
		}

		public void dispose() {
			// stop listening
			store.removePropertyChangeListener(this);
		}
	}

	/**
	 * Used to render the "right" side of a workbench compare
	 */
	private final class GitWorkbenchLabelProvider extends LabelProvider
			implements IColorProvider, IFontProvider {
		WorkbenchLabelProvider myProvider = new WorkbenchLabelProvider();

		@Override
		public Image getImage(Object element) {
			Image superImage = myProvider.getImage(element);
			if (superImage == null)
				return DELETED;
			if (element instanceof IFile) {
				if (equalIds.contains(new Path(repositoryMapping
						.getRepoRelativePath((IFile) element)))) {
					return SAME_CONTENT;
				}
			}
			return superImage;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof GitFileRevision) {
				return ((GitFileRevision) element).getName();
			}
			return myProvider.getText(element);
		}

		public Color getBackground(Object element) {
			return myProvider.getBackground(element);
		}

		public Color getForeground(Object element) {
			return myProvider.getForeground(element);
		}

		public Font getFont(Object element) {
			return myProvider.getFont(element);
		}
	}

	/**
	 * Used to render the left tree in case we have no workspace
	 */
	private abstract class AbstractPathNodeContentProvider extends
			ArrayContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			IResource[] resources = (IResource[]) input;
			PathNode[] nodes = new PathNode[resources.length];
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				if (resource instanceof IFile) {
					IPath path = new Path(repositoryMapping
							.getRepoRelativePath(resource));
					Type type;
					if (leftOnly.contains(path))
						type = Type.FILE_ADDED;
					else if (equalIds.contains(path))
						type = Type.FILE_BOTH_SIDES_SAME;
					else
						type = Type.FILE_BOTH_SIDES_DIFFER;
					nodes[i] = new PathNode(path, type);
				} else
					nodes[i] = new PathNode(new Path(repositoryMapping
							.getRepoRelativePath(resource)), Type.FOLDER);
			}
			return nodes;
		}

		public Object getParent(Object element) {
			if (!(element instanceof PathNode))
				return null;
			IPath currentPath = ((PathNode) element).path;
			if (currentPath.segmentCount() > 0)
				return currentPath.removeLastSegments(1);
			return null;
		}
	}

	/**
	 * Used to render the left tree in case we have no workspace
	 */
	private final class LeftTreeContentProvider extends
			AbstractPathNodeContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			if (leftPathsWithChildren.isEmpty() && leftOnly.isEmpty())
				return new String[] { UIText.CompareTreeView_NoDifferencesFoundMessage };
			return super.getElements(inputElement);
		}

		public Object[] getChildren(Object parentElement) {
			IPath parent = ((PathNode) parentElement).path;
			List<PathNode> children = new ArrayList<PathNode>();
			for (IPath childPath : leftPathsWithChildren) {
				if (childPath.segmentCount() > 0
						&& childPath.removeLastSegments(1).equals(parent)) {
					children.add(new PathNode(childPath, Type.FOLDER));
				}
			}
			for (IPath mapPath : leftVersionMap.keySet()) {
				if (mapPath.removeLastSegments(1).equals(parent)
						&& (!showAddedOnly || leftOnly.contains(mapPath))
						&& (showEquals || !equalIds.contains(mapPath))) {
					if (leftOnly.contains(mapPath))
						children.add(new PathNode(mapPath, Type.FILE_ADDED));
					else if (equalIds.contains(mapPath))
						children.add(new PathNode(mapPath,
								Type.FILE_BOTH_SIDES_SAME));
					else
						children.add(new PathNode(mapPath,
								Type.FILE_BOTH_SIDES_DIFFER));
				}
			}
			Collections.sort(children, new Comparator<PathNode>() {
				public int compare(PathNode o1, PathNode o2) {
					int diff = o1.type.ordinal() - o2.type.ordinal();
					if (diff != 0)
						return diff;
					return o1.path.toString().compareTo(o2.path.toString());
				}
			});
			return children.toArray();
		}

		public boolean hasChildren(Object element) {
			if (!(element instanceof PathNode))
				return false;
			IPath parent = ((PathNode) element).path;
			for (IPath childPath : leftPathsWithChildren) {
				if (childPath.removeLastSegments(1).equals(parent)) {
					return true;
				}
			}
			for (IPath mapPath : leftVersionMap.keySet()) {
				if (mapPath.removeLastSegments(1).equals(parent)
						&& (!showAddedOnly || leftOnly.contains(mapPath))
						&& (showEquals || !equalIds.contains(mapPath))) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Used to render the right tree in case we have no workspace
	 */
	private final class RightTreeContentProvider extends
			AbstractPathNodeContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			if (rightPathsWithChildren.isEmpty() && rightOnly.isEmpty())
				return new String[] { UIText.CompareTreeView_NoDifferencesFoundMessage };
			return super.getElements(inputElement);
		}

		public Object[] getChildren(Object parentElement) {
			IPath parent = ((PathNode) parentElement).path;
			List<PathNode> children = new ArrayList<PathNode>();
			for (IPath childPath : rightPathsWithChildren) {
				if (childPath.segmentCount() > 0
						&& childPath.removeLastSegments(1).equals(parent)) {
					children.add(new PathNode(childPath, Type.FOLDER));
				}
			}
			for (IPath mapPath : rightVersionMap.keySet()) {
				if (mapPath.removeLastSegments(1).equals(parent)
						&& (!showDeletedOnly || rightOnly.contains(mapPath))
						&& (showEquals || !equalIds.contains(mapPath))) {
					if (rightOnly.contains(mapPath))
						children.add(new PathNode(mapPath, Type.FILE_DELETED));
					else if (equalIds.contains(mapPath))
						children.add(new PathNode(mapPath,
								Type.FILE_BOTH_SIDES_SAME));
					else
						children.add(new PathNode(mapPath,
								Type.FILE_BOTH_SIDES_DIFFER));
				}
			}
			Collections.sort(children, new Comparator<PathNode>() {
				public int compare(PathNode o1, PathNode o2) {
					int diff = o1.type.ordinal() - o2.type.ordinal();
					if (diff != 0)
						return diff;
					return o1.path.toString().compareTo(o2.path.toString());
				}
			});
			return children.toArray();
		}

		public boolean hasChildren(Object element) {
			if (!(element instanceof PathNode))
				return false;
			IPath parent = ((PathNode) element).path;
			for (IPath childPath : rightPathsWithChildren) {
				if (childPath.removeLastSegments(1).equals(parent)) {
					return true;
				}
			}
			for (IPath mapPath : rightVersionMap.keySet()) {
				if (mapPath.removeLastSegments(1).equals(parent)
						&& (!showDeletedOnly || rightOnly.contains(mapPath))
						&& (showEquals || !equalIds.contains(mapPath))) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Used to render {@link PathNode} trees
	 */
	private final class RepositoryTreeLabelProvider extends BaseLabelProvider
			implements ILabelProvider {
		public Image getImage(Object element) {
			if (element instanceof String)
				return null;
			Type type = ((PathNode) element).type;
			switch (type) {
			case FILE_BOTH_SIDES_SAME:
				return SAME_CONTENT;
			case FILE_BOTH_SIDES_DIFFER:
				return FILE_IMAGE;
			case FILE_ADDED:
				return ADDED;
			case FILE_DELETED:
				return DELETED;
			case FOLDER:
				return FOLDER_IMAGE;
			}
			return null;
		}

		public String getText(Object element) {
			if (element instanceof String)
				return (String) element;
			IPath path = ((PathNode) element).path;
			if (path.segmentCount() == 0)
				return UIText.CompareTreeView_RepositoryRootName;
			return path.lastSegment();

		}
	}
}
