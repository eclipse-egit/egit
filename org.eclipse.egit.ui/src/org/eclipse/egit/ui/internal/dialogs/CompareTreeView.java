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
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
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
import org.eclipse.egit.ui.internal.actions.BooleanPrefAction;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView.PathNode.Type;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.model.IWorkbenchAdapter;
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
 * <p>
 * The tree nodes are shown with icons for "Added", "Deleted", and
 * "Same Contents" for files. Files with same content can be hidden using a
 * filter button.
 * <p>
 * This view can also show files and folders outside the Eclipse workspace when
 * a {@link Repository} is used as input.
 */
public class CompareTreeView extends ViewPart {
	/** The "magic" compare version to compare with the index */
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

	private TreeViewer tree;

	private IWorkbenchAction showEqualsAction;

	private Map<IPath, GitFileRevision> compareVersionMap = new HashMap<IPath, GitFileRevision>();

	private Map<IPath, GitFileRevision> baseVersionMap = new HashMap<IPath, GitFileRevision>();

	private Set<IPath> addedPaths = new HashSet<IPath>();

	private Set<IPath> equalContentPaths = new HashSet<IPath>();

	private Set<IPath> baseVersionPathsWithChildren = new HashSet<IPath>();

	private Map<IPath, List<PathNodeAdapter>> compareVersionPathsWithChildren = new HashMap<IPath, List<PathNodeAdapter>>();

	private Set<IPath> deletedPaths = new HashSet<IPath>();

	private List<IWorkbenchAction> actionsToDispose = new ArrayList<IWorkbenchAction>();

	private Object input;

	private String compareVersion;

	private String baseVersion;

	private boolean showEquals = false;

	@Override
	public void createPartControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(main);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		tree = new TreeViewer(main, SWT.BORDER);
		tree.setContentProvider(new PathNodeContentProvider());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree.getTree());

		tree.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				reactOnOpen(event);
			}
		});
		tree.getTree().setEnabled(false);
		createActions();
	}

	private void createActions() {
		IWorkbenchAction reuseCompareEditorAction = new CompareUtils.ReuseCompareEditorAction();
		actionsToDispose.add(reuseCompareEditorAction);
		getViewSite().getActionBars().getMenuManager().add(
				reuseCompareEditorAction);

		showEqualsAction = new BooleanPrefAction(
				(IPersistentPreferenceStore) Activator.getDefault()
						.getPreferenceStore(),
				UIPreferences.TREE_COMPARE_SHOW_EQUALS,
				UIText.CompareTreeView_EqualFilesTooltip) {
			@Override
			public void apply(boolean value) {
				buildTrees(false);
			}
		};
		showEqualsAction.setImageDescriptor(UIIcons.ELCL16_SYNCED);
		showEqualsAction.setEnabled(false);
		actionsToDispose.add(showEqualsAction);
		getViewSite().getActionBars().getToolBarManager().add(showEqualsAction);

		IAction expandAllAction = new Action(
				UIText.CompareTreeView_CollapseAllTooltip) {
			@Override
			public void run() {
				tree.collapseAll();
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
			GitFileRevision rightRevision = compareVersionMap.get(new Path(
					repositoryMapping.getRepoRelativePath(res)));
			if (rightRevision == null) {
				right = new GitCompareFileRevisionEditorInput.EmptyTypedElement(
						NLS
								.bind(
										UIText.CompareTreeView_ItemNotFoundInVersionMessage,
										res.getName(), getCompareVersion()));
			} else {
				String encoding = CompareUtils.getResourceEncoding(res);
				right = new FileRevisionTypedElement(rightRevision, encoding);
			}
			GitCompareFileRevisionEditorInput compareInput = new GitCompareFileRevisionEditorInput(
					left, right, PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage());
			CompareUtils.openInCompare(PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage(), compareInput);
		} else if (selected instanceof GitFileRevision) {
			GitFileRevision rightRevision = (GitFileRevision) selected;
			left = new GitCompareFileRevisionEditorInput.EmptyTypedElement(NLS
					.bind(UIText.CompareTreeView_ItemNotFoundInVersionMessage,
							rightRevision.getName(), getBaseVersion()));
			right = new FileRevisionTypedElement(rightRevision);
		} else if (selected instanceof PathNode) {
			PathNode node = (PathNode) selected;
			switch (node.type) {
			case FILE_BOTH_SIDES_DIFFER:
				// fall through
			case FILE_BOTH_SIDES_SAME: {
				// open a compare editor with both sides filled
				GitFileRevision rightRevision = compareVersionMap
						.get(node.path);
				right = new FileRevisionTypedElement(rightRevision);
				GitFileRevision leftRevision = baseVersionMap.get(node.path);
				left = new FileRevisionTypedElement(leftRevision);
				break;
			}
			case FILE_DELETED: {
				// open compare editor with left side empty
				GitFileRevision rightRevision = compareVersionMap
						.get(node.path);
				right = new FileRevisionTypedElement(rightRevision);
				left = new GitCompareFileRevisionEditorInput.EmptyTypedElement(
						NLS
								.bind(
										UIText.CompareTreeView_ItemNotFoundInVersionMessage,
										rightRevision.getName(),
										getBaseVersion()));
				break;
			}
			case FILE_ADDED: {
				// open compare editor with right side empty
				GitFileRevision leftRevision = baseVersionMap.get(node.path);
				left = new FileRevisionTypedElement(leftRevision);
				right = new GitCompareFileRevisionEditorInput.EmptyTypedElement(
						NLS
								.bind(
										UIText.CompareTreeView_ItemNotFoundInVersionMessage,
										leftRevision.getName(),
										getCompareVersion()));
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

		} else if (selected instanceof PathNodeAdapter) {
			// deleted in workspace
			PathNodeAdapter node = (PathNodeAdapter) selected;
			GitFileRevision rightRevision = compareVersionMap
					.get(node.pathNode.path);
			right = new FileRevisionTypedElement(rightRevision);
			left = new GitCompareFileRevisionEditorInput.EmptyTypedElement(NLS
					.bind(UIText.CompareTreeView_ItemNotFoundInVersionMessage,
							node.pathNode.path.lastSegment(), getBaseVersion()));
		} else
			return;

		GitCompareFileRevisionEditorInput compareInput = new GitCompareFileRevisionEditorInput(
				left, right, PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage());
		CompareUtils.openInCompare(PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage(), compareInput);
	}

	private String getBaseVersion() {
		// null in case of Workspace compare
		if (baseVersion == null)
			return UIText.CompareTreeView_WorkspaceVersionText;
		return baseVersion;
	}

	private String getCompareVersion() {
		return compareVersion;
	}

	@Override
	public void setFocus() {
		tree.getTree().setFocus();
	}

	/**
	 * Used to compare the working tree with another version
	 *
	 * @param input
	 *            the {@link IResource}s from which to build the tree
	 * @param compareVersion
	 *            a {@link Ref} name or {@link RevCommit} id or
	 *            {@link #INDEX_VERSION}
	 */
	public void setInput(final IResource[] input, String compareVersion) {
		setResourceInput(input);
		this.baseVersion = null;
		this.compareVersion = compareVersion;
		buildTrees(true);
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
	 * @param baseVersion
	 *            a {@link Ref} name or {@link RevCommit} id
	 * @param compareVersion
	 *            a {@link Ref} name or {@link RevCommit} id or
	 *            {@link #INDEX_VERSION}
	 */
	public void setInput(final IResource[] input, String baseVersion,
			String compareVersion) {
		setResourceInput(input);
		this.baseVersion = baseVersion;
		this.compareVersion = compareVersion;
		buildTrees(true);
		updateControls();
	}

	/**
	 * Used if no workspace filtering should be applied
	 *
	 * @param input
	 *            the {@link Repository} from which to build the tree
	 * @param baseVersion
	 *            a {@link Ref} name or {@link RevCommit} id
	 * @param compareVersion
	 *            a {@link Ref} name or {@link RevCommit} id
	 */
	public void setInput(final Repository input, String baseVersion,
			String compareVersion) {
		this.input = input;
		this.baseVersion = baseVersion;
		this.compareVersion = compareVersion;
		buildTrees(true);
		updateControls();
	}

	private void updateControls() {
		for (IWorkbenchAction action : actionsToDispose)
			action.setEnabled(input != null);
		tree.getTree().setEnabled(input != null);
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
			if (baseVersion == null)
				setContentDescription(NLS
						.bind(
								UIText.CompareTreeView_ComparingWorkspaceVersionDescription,
								name,
								compareVersion.equals(INDEX_VERSION) ? UIText.CompareTreeView_IndexVersionText
										: compareVersion));
			else
				setContentDescription(NLS
						.bind(
								UIText.CompareTreeView_ComparingTwoVersionDescription,
								new String[] {
										baseVersion,
										name,
										compareVersion.equals(INDEX_VERSION) ? UIText.CompareTreeView_IndexVersionText
												: compareVersion }));
		}
	}

	private void buildTrees(final boolean buildMaps) {
		final Object[] wsExpaneded = tree.getExpandedElements();
		final ISelection wsSel = tree.getSelection();

		tree.setInput(null);

		if (baseVersion == null) {
			tree.setContentProvider(new WorkbenchTreeContentProvider());
			tree.setComparator(new WorkbenchTreeComparator());
			tree.setLabelProvider(new WorkbenchTreeLabelProvider());
		} else {
			tree.setContentProvider(new PathNodeContentProvider());
			tree.setComparator(new PathNodeTreeComparator());
			tree.setLabelProvider(new PathNodeLabelProvider());
		}
		for (IWorkbenchAction action : actionsToDispose)
			action.setEnabled(false);

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
		final RevCommit baseCommit;
		final RevCommit compareCommit;
		RevWalk rw = new RevWalk(repo);
		try {
			ObjectId commitId = repo.resolve(compareVersion);
			compareCommit = commitId != null ? rw.parseCommit(commitId) : null;
			if (baseVersion == null)
				baseCommit = null;
			else {
				commitId = repo.resolve(baseVersion);
				baseCommit = rw.parseCommit(commitId);
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
								if (buildMaps)
									buildMaps(repo, baseCommit, compareCommit,
											monitor);
								PlatformUI.getWorkbench().getDisplay()
										.asyncExec(new Runnable() {
											public void run() {
												tree.setInput(input);
												tree
														.setExpandedElements(wsExpaneded);
												tree.setSelection(wsSel);
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

	private void buildMaps(Repository repository, RevCommit baseCommit,
			RevCommit compareCommit, IProgressMonitor monitor)
			throws InterruptedException, IOException {
		monitor.beginTask(UIText.CompareTreeView_AnalyzingRepositoryTaskText,
				IProgressMonitor.UNKNOWN);
		boolean useIndex = compareVersion.equals(INDEX_VERSION);
		deletedPaths.clear();
		equalContentPaths.clear();
		baseVersionMap.clear();
		compareVersionMap.clear();
		compareVersionPathsWithChildren.clear();
		addedPaths.clear();
		baseVersionPathsWithChildren.clear();
		boolean checkIgnored = false;
		TreeWalk tw = new TreeWalk(repository);
		try {
			int baseTreeIndex;
			if (baseCommit == null) {
				checkIgnored = true;
				baseTreeIndex = tw.addTree(new AdaptableFileTreeIterator(
						repository, ResourcesPlugin.getWorkspace().getRoot()));
			} else
				baseTreeIndex = tw.addTree(new CanonicalTreeParser(null,
						repository.newObjectReader(), baseCommit.getTree()));
			int compareTreeIndex;
			if (!useIndex)
				compareTreeIndex = tw.addTree(new CanonicalTreeParser(null,
						repository.newObjectReader(), compareCommit.getTree()));
			else
				compareTreeIndex = tw.addTree(new DirCacheIterator(repository
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
				AbstractTreeIterator compareVersionIterator = tw.getTree(
						compareTreeIndex, AbstractTreeIterator.class);
				AbstractTreeIterator baseVersionIterator = tw.getTree(
						baseTreeIndex, AbstractTreeIterator.class);
				if (checkIgnored
						&& baseVersionIterator != null
						&& ((WorkingTreeIterator) baseVersionIterator)
								.isEntryIgnored())
					continue;
				if (compareVersionIterator != null
						&& baseVersionIterator != null) {
					monitor.setTaskName(baseVersionIterator
							.getEntryPathString());
					IPath currentPath = new Path(baseVersionIterator
							.getEntryPathString());
					if (!useIndex)
						compareVersionMap
								.put(currentPath, GitFileRevision.inCommit(
										repository, compareCommit,
										baseVersionIterator
												.getEntryPathString(), tw
												.getObjectId(compareTreeIndex)));
					else
						compareVersionMap.put(currentPath, GitFileRevision
								.inIndex(repository, baseVersionIterator
										.getEntryPathString()));
					if (baseCommit != null)
						baseVersionMap.put(currentPath, GitFileRevision
								.inCommit(repository, baseCommit,
										baseVersionIterator
												.getEntryPathString(), tw
												.getObjectId(baseTreeIndex)));
					boolean equalContent = compareVersionIterator
							.getEntryObjectId().equals(
									baseVersionIterator.getEntryObjectId());
					if (equalContent)
						equalContentPaths.add(currentPath);

					if (equalContent && !showEquals)
						continue;

					while (currentPath.segmentCount() > 0) {
						currentPath = currentPath.removeLastSegments(1);
						if (!baseVersionPathsWithChildren.add(currentPath))
							break;
					}

				} else if (baseVersionIterator != null
						&& compareVersionIterator == null) {
					monitor.setTaskName(baseVersionIterator
							.getEntryPathString());
					// only on base side
					IPath currentPath = new Path(baseVersionIterator
							.getEntryPathString());
					addedPaths.add(currentPath);
					if (baseCommit != null)
						baseVersionMap.put(currentPath, GitFileRevision
								.inCommit(repository, baseCommit,
										baseVersionIterator
												.getEntryPathString(), tw
												.getObjectId(baseTreeIndex)));
					while (currentPath.segmentCount() > 0) {
						currentPath = currentPath.removeLastSegments(1);
						if (!baseVersionPathsWithChildren.add(currentPath))
							break;
					}

				} else if (compareVersionIterator != null
						&& baseVersionIterator == null) {
					monitor.setTaskName(compareVersionIterator
							.getEntryPathString());
					// only on compare side
					IPath currentPath = new Path(compareVersionIterator
							.getEntryPathString());
					deletedPaths.add(currentPath);
					List<PathNodeAdapter> children = compareVersionPathsWithChildren
							.get(currentPath.removeLastSegments(1));
					if (children == null) {
						children = new ArrayList<PathNodeAdapter>(1);
						compareVersionPathsWithChildren.put(currentPath
								.removeLastSegments(1), children);
					}
					children.add(new PathNodeAdapter(new PathNode(currentPath,
							Type.FILE_DELETED)));

					if (!useIndex)
						compareVersionMap
								.put(currentPath, GitFileRevision.inCommit(
										repository, compareCommit,
										compareVersionIterator
												.getEntryPathString(), tw
												.getObjectId(compareTreeIndex)));
					else
						compareVersionMap.put(currentPath, GitFileRevision
								.inIndex(repository, compareVersionIterator
										.getEntryPathString()));
				}
			}
		} finally {
			tw.release();
			monitor.done();
		}
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
		/** Type; note that the ordinal is used to sort the tree */
		public enum Type {
			/** Folder */
			FOLDER,
			/** File added (only on base side) */
			FILE_ADDED,
			/** File deleted (only on compare side) */
			FILE_DELETED,
			/** File differs on both sides */
			FILE_BOTH_SIDES_DIFFER,
			/** File same on both sides */
			FILE_BOTH_SIDES_SAME
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

	private final class WorkbenchTreeLabelProvider extends
			WorkbenchLabelProvider {
		@Override
		protected ImageDescriptor decorateImage(ImageDescriptor baseImage,
				Object element) {
			if (!(element instanceof IFile)) {
				return super.decorateImage(baseImage, element);
			}
			IPath elementPath = new Path(repositoryMapping
					.getRepoRelativePath((IFile) element));
			// decorate with + for files not found in the repository and = for
			// "same" files
			if (addedPaths.contains(elementPath)) {
				return UIIcons.ELCL16_ADD;
			}
			if (deletedPaths.contains(elementPath)) {
				return UIIcons.ELCL16_DELETE;
			}
			if (equalContentPaths.contains(elementPath)) {
				return UIIcons.ELCL16_SYNCED;
			}
			return super.decorateImage(baseImage, element);
		}
	}

	/**
	 * Sorts the workbench tree by type and state (Folder, Added, Deleted,
	 * Changed, Unchanged Files)
	 */
	private final class WorkbenchTreeComparator extends ViewerComparator {
		private static final int FOLDERCATEGORY = 5;

		private static final int ADDEDCATEGORY = 10;

		private static final int DELETEDCATEGORY = 15;

		private static final int CHANGEDCATEGORY = 20;

		private static final int UNCHANGEDCATEGORY = 30;

		private static final int UNKNOWNCATEGORY = 50;

		@Override
		public int category(Object element) {
			IResource adapter = (IResource) getAdapter(element, IResource.class);
			if (adapter != null) {
				if (adapter instanceof IContainer) {
					return FOLDERCATEGORY;
				}
				if (adapter instanceof IFile) {
					IFile file = (IFile) adapter;
					IPath path = new Path(repositoryMapping
							.getRepoRelativePath(file));
					if (addedPaths.contains(path))
						return ADDEDCATEGORY;
					if (equalContentPaths.contains(path))
						return UNCHANGEDCATEGORY;
					return CHANGEDCATEGORY;
				}
			}
			if (element instanceof PathNodeAdapter)
				return DELETEDCATEGORY;

			return UNKNOWNCATEGORY;
		}

		private Object getAdapter(Object sourceObject, Class adapterType) {
			Assert.isNotNull(adapterType);
			if (sourceObject == null)
				return null;

			if (adapterType.isInstance(sourceObject))
				return sourceObject;

			if (sourceObject instanceof IAdaptable) {
				IAdaptable adaptable = (IAdaptable) sourceObject;

				Object result = adaptable.getAdapter(adapterType);
				if (result != null) {
					// Sanity-check
					Assert.isTrue(adapterType.isInstance(result));
					return result;
				}
			}

			if (!(sourceObject instanceof PlatformObject)) {
				Object result = Platform.getAdapterManager().getAdapter(
						sourceObject, adapterType);
				if (result != null)
					return result;
			}
			return null;
		}
	}

	/**
	 * Sorts the workbench tree by type and state (Folder, Added, Deleted,
	 * Changed, Unchanged Files)
	 */
	private final static class PathNodeTreeComparator extends ViewerComparator {
		private static final int UNKNOWNCATEGORY = 50;

		@Override
		public int category(Object element) {
			if (element instanceof PathNode) {
				return ((PathNode) element).type.ordinal();
			}
			return UNKNOWNCATEGORY;
		}
	}

	/**
	 * Used to render the "local" (workspace) side of a tree compare where one
	 * side is the workspace
	 */
	private final class WorkbenchTreeContentProvider extends
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
				if (!showEquals && equalContentPaths.contains(path)) {
					rebuildArray = true;
					continue;
				}
				if (child instanceof IContainer
						&& !baseVersionPathsWithChildren.contains(path)) {
					rebuildArray = true;
					continue;
				}
				if (!showEquals && equalContentPaths.contains(path)) {
					rebuildArray = true;
					continue;
				}
				childList.add(child);
			}
			if (element instanceof IContainer) {
				List<PathNodeAdapter> deletedChildren = compareVersionPathsWithChildren
						.get(new Path(repositoryMapping
								.getRepoRelativePath((IResource) element)));
				if (deletedChildren != null) {
					rebuildArray = true;
					for (IWorkbenchAdapter path : deletedChildren)
						childList.add(path);
				}
			}
			if (rebuildArray)
				return childList.toArray();
			return children;
		}
	}

	/**
	 * Used to render the tree in case we have no workspace
	 */
	private final class PathNodeContentProvider extends ArrayContentProvider
			implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			if (baseVersionPathsWithChildren.isEmpty() && addedPaths.isEmpty())
				return new String[] { UIText.CompareTreeView_NoDifferencesFoundMessage };
			if (input instanceof IResource[]) {
				IResource[] resources = (IResource[]) input;
				PathNode[] nodes = new PathNode[resources.length];
				for (int i = 0; i < resources.length; i++) {
					IResource resource = resources[i];
					if (resource instanceof IFile) {
						IPath path = new Path(repositoryMapping
								.getRepoRelativePath(resource));
						Type type;
						if (addedPaths.contains(path))
							type = Type.FILE_ADDED;
						else if (equalContentPaths.contains(path))
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
			return new PathNode[] { new PathNode(new Path(""), Type.FOLDER) }; //$NON-NLS-1$
		}

		public Object[] getChildren(Object parentElement) {
			PathNode parentNode = (PathNode) parentElement;
			IPath parent = parentNode.path;
			List<PathNode> children = new ArrayList<PathNode>();
			for (IPath childPath : baseVersionPathsWithChildren) {
				if (childPath.segmentCount() > 0
						&& childPath.removeLastSegments(1).equals(parent)) {
					children.add(new PathNode(childPath, Type.FOLDER));
				}
			}
			for (IPath mapPath : baseVersionMap.keySet()) {
				if (mapPath.removeLastSegments(1).equals(parent)
						&& (showEquals || !equalContentPaths.contains(mapPath))) {
					if (addedPaths.contains(mapPath))
						children.add(new PathNode(mapPath, Type.FILE_ADDED));
					else if (equalContentPaths.contains(mapPath))
						children.add(new PathNode(mapPath,
								Type.FILE_BOTH_SIDES_SAME));
					else
						children.add(new PathNode(mapPath,
								Type.FILE_BOTH_SIDES_DIFFER));
				}
			}
			if (parentNode.type == Type.FOLDER) {
				List<PathNodeAdapter> deletedChildren = compareVersionPathsWithChildren
						.get(parent);
				if (deletedChildren != null)
					for (PathNodeAdapter path : deletedChildren)
						children.add(path.pathNode);

			}
			return children.toArray();
		}

		public boolean hasChildren(Object element) {
			if (!(element instanceof PathNode))
				return false;
			IPath parent = ((PathNode) element).path;
			for (IPath childPath : baseVersionPathsWithChildren) {
				if (childPath.removeLastSegments(1).equals(parent))
					return true;
			}
			for (IPath mapPath : baseVersionMap.keySet()) {
				if (mapPath.removeLastSegments(1).equals(parent)
						&& (showEquals || !equalContentPaths.contains(mapPath))) {
					return true;
				}
			}
			return false;
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
	 * Used to render {@link PathNode} trees
	 */
	private final class PathNodeLabelProvider extends BaseLabelProvider
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

	private final static class PathNodeAdapter implements IWorkbenchAdapter {
		private final static Object[] EMPTYARRAY = new Object[0];

		PathNode pathNode;

		public PathNodeAdapter(PathNode path) {
			pathNode = path;
		}

		public Object[] getChildren(Object o) {
			return EMPTYARRAY;
		}

		public ImageDescriptor getImageDescriptor(Object object) {
			return UIIcons.ELCL16_DELETE;
		}

		public String getLabel(Object o) {
			return pathNode.path.lastSegment();
		}

		public Object getParent(Object o) {
			// doesn't seem to hurt to simply return null
			return null;
		}
	}
}
