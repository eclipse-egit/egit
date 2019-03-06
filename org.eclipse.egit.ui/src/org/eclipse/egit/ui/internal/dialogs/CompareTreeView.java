/*******************************************************************************
 * Copyright (c) 2011, 2019 SAP AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Robin Stocker <robin@nibor.org> - ignore linked resources
 *    Robin Stocker <robin@nibor.org> - Unify workbench and PathNode tree code
 *    Marc Khouzam <marc.khouzam@ericsson.com> - Add compare mode toggle
 *    Marc Khouzam <marc.khouzam@ericsson.com> - Skip expensive computations for equal content (bug 431610)
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Prevent NPE on empty content; git attributes
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.internal.storage.WorkingTreeFileRevision;
import org.eclipse.egit.core.internal.storage.WorkspaceFileRevision;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.actions.BooleanPrefAction;
import org.eclipse.egit.ui.internal.commit.DiffViewer;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView.PathNode.Type;
import org.eclipse.egit.ui.internal.revision.FileRevisionTypedElement;
import org.eclipse.egit.ui.internal.revision.GitCompareFileRevisionEditorInput;
import org.eclipse.egit.ui.internal.synchronize.compare.LocalNonWorkspaceTypedElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.NotIgnoredFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;

/**
 * Shows a tree when opening compare on an {@link IContainer}, or a
 * {@link Repository}
 * <p>
 * In both cases the tree consists of {@link PathNode} instances representing
 * files and folders. The container nodes are decorated with
 * {@link WorkbenchLabelProvider} to use the same icons as the workbench. Linked resources
 * however are ignored and not listed as content.
 * <p>
 * The tree nodes are shown with icons for "Added", "Deleted", and
 * "Same Contents" for files. Files with same content can be hidden using a
 * filter button.
 * <p>
 * This view can also show files and folders outside the Eclipse workspace when
 * a {@link Repository} is used as input.
 * <p>
 */
public class CompareTreeView extends ViewPart implements IMenuListener, IShowInSource {
	/** The "magic" compare version to compare with the index */
	public static final String INDEX_VERSION = "%%%INDEX%%%"; //$NON-NLS-1$

	/** The View ID */
	public static final String ID = "org.eclipse.egit.ui.CompareTreeView"; //$NON-NLS-1$

	private static final Image FILE_IMAGE = PlatformUI.getWorkbench()
			.getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);

	private static final Image FOLDER_IMAGE = PlatformUI.getWorkbench()
			.getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

	private final Image SAME_CONTENT = new Image(FILE_IMAGE.getDevice(),
			FILE_IMAGE, SWT.IMAGE_DISABLE);

	private Image ADDED = UIIcons.ELCL16_ADD.createImage();

	private Image DELETED = UIIcons.ELCL16_DELETE.createImage();

	private RepositoryMapping repositoryMapping;

	private TreeViewer tree;

	private IWorkbenchAction showEqualsAction;

	private IWorkbenchAction compareModeAction;

	private Map<IPath, FileNode> fileNodes = new HashMap<>();

	private Map<IPath, ContainerNode> containerNodes = new HashMap<>();

	private List<IWorkbenchAction> actionsToDispose = new ArrayList<>();

	private Object input;

	private String compareVersion;

	private String baseVersion;

	private boolean showEquals = false;

	@Override
	public void createPartControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(main);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		tree = new TreeViewer(main, SWT.MULTI);
		tree.setContentProvider(new PathNodeContentProvider());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree.getTree());

		tree.addOpenListener(new IOpenListener() {
			@Override
			public void open(OpenEvent event) {
				reactOnOpen(event);
			}
		});
		tree.getTree().setEnabled(false);
		createActions();

		getViewSite().setSelectionProvider(tree);
		createContextMenu();
	}

	private void createActions() {
		IWorkbenchAction reuseCompareEditorAction = new CompareUtils.ReuseCompareEditorAction();
		actionsToDispose.add(reuseCompareEditorAction);
		getViewSite().getActionBars().getMenuManager().add(
				reuseCompareEditorAction);

		compareModeAction = new BooleanPrefAction(
				(IPersistentPreferenceStore) Activator.getDefault()
						.getPreferenceStore(),
				UIPreferences.TREE_COMPARE_COMPARE_MODE,
				UIText.CompareTreeView_CompareModeTooltip) {
			@Override
			public void apply(boolean value) {
				// nothing, just switch the preference
			}
		};
		compareModeAction.setImageDescriptor(UIIcons.ELCL16_COMPARE_VIEW);
		compareModeAction.setEnabled(true);
		actionsToDispose.add(compareModeAction);
		getViewSite().getActionBars().getToolBarManager()
				.add(compareModeAction);

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
		showEqualsAction.setImageDescriptor(UIIcons.ELCL16_FILTER);
		showEqualsAction.setEnabled(false);
		actionsToDispose.add(showEqualsAction);
		getViewSite().getActionBars().getToolBarManager().add(showEqualsAction);

		IAction expandAllAction = new Action(
				UIText.CompareTreeView_ExpandAllTooltip) {
			@Override
			public void run() {
				UIUtils.expandAll(tree);
			}
		};
		expandAllAction.setImageDescriptor(UIIcons.EXPAND_ALL);
		getViewSite().getActionBars().getToolBarManager().add(expandAllAction);

		IAction collapseAllAction = new Action(
				UIText.CompareTreeView_CollapseAllTooltip) {
			@Override
			public void run() {
				UIUtils.collapseAll(tree);
			}
		};
		collapseAllAction.setImageDescriptor(UIIcons.COLLAPSEALL);
		getViewSite().getActionBars().getToolBarManager().add(collapseAllAction);
	}

	private void reactOnOpen(OpenEvent event) {
		Object selected = ((IStructuredSelection) event.getSelection())
				.getFirstElement();
		ITypedElement left;
		ITypedElement right;
		if (selected instanceof ContainerNode) {
			// open/close folder
			TreeViewer tv = (TreeViewer) event.getViewer();
			tv.setExpandedState(selected, !tv.getExpandedState(selected));
		} else if (selected instanceof FileNode) {
			FileNode fileNode = (FileNode) selected;

			boolean compareMode = Activator.getDefault().getPreferenceStore()
					.getBoolean(UIPreferences.TREE_COMPARE_COMPARE_MODE);

			if (compareMode) {
				left = getTypedElement(fileNode, fileNode.leftRevision,
						getBaseVersionText());
				right = getTypedElement(fileNode, fileNode.rightRevision,
						getCompareVersionText());

				GitCompareFileRevisionEditorInput compareInput = new GitCompareFileRevisionEditorInput(
						left, right, PlatformUI.getWorkbench()
								.getActiveWorkbenchWindow().getActivePage());
				CompareUtils.openInCompare(PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage(),
						compareInput);
			} else {
				IFile file = fileNode.getFile();
				if (file != null) {
					DiffViewer.openFileInEditor(file.getLocation().toFile(),
							-1);
				}
			}
		}
	}

	private ITypedElement getTypedElement(FileNode node, IFileRevision fileRevision, String versionName) {
		if (fileRevision instanceof WorkspaceFileRevision) {
			return SaveableCompareEditorInput.createFileElement(node.getFile());
		} else if (fileRevision instanceof WorkingTreeFileRevision) {
			IPath path = Path
					.fromPortableString(((WorkingTreeFileRevision) fileRevision)
							.getURI().getPath());
			return new LocalNonWorkspaceTypedElement(getRepository(), path);
		} else if (fileRevision == null) {
			return new GitCompareFileRevisionEditorInput.EmptyTypedElement(
					NLS.bind(
							UIText.CompareTreeView_ItemNotFoundInVersionMessage,
							node.getPath(), versionName));
		} else {
			Repository repository = getRepository();
			String encoding = repository == null ? null
					: CompareCoreUtils.getResourceEncoding(repository,
							node.getRepoRelativePath());
			return new FileRevisionTypedElement(fileRevision, encoding);
		}
	}

	private String getBaseVersionText() {
		// null in case of Workspace compare
		if (baseVersion == null)
			return UIText.CompareTreeView_WorkspaceVersionText;
		return CompareUtils.truncatedRevision(baseVersion);
	}

	private String getCompareVersionText() {
		if (compareVersion.equals(INDEX_VERSION))
			return UIText.CompareTreeView_IndexVersionText;
		else
			return CompareUtils.truncatedRevision(compareVersion);
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
			List<IResource> resources = new ArrayList<>(input.length);
			List<IPath> allPaths = new ArrayList<>(input.length);
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
			this.input = resources.toArray(new IResource[0]);
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
						.bind(UIText.CompareTreeView_ComparingWorkspaceVersionDescription,
								name, getCompareVersionText()));
			else
				setContentDescription(NLS.bind(
						UIText.CompareTreeView_ComparingTwoVersionDescription,
						new String[] { name,
								CompareUtils.truncatedRevision(baseVersion),
								getCompareVersionText() }));
		}
	}

	private void buildTrees(final boolean buildMaps) {
		final Object[] wsExpaneded = tree.getExpandedElements();
		final ISelection wsSel = tree.getSelection();

		tree.setInput(null);

		tree.setContentProvider(new PathNodeContentProvider());
		tree.setComparator(new PathNodeTreeComparator());
		tree.setLabelProvider(new PathNodeLabelProvider());
		tree.setFilters(new PathNodeFilter[] { new PathNodeFilter() });

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
		try (RevWalk rw = new RevWalk(repo)) {
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
		}
		showBusy(true);
		try {
			// this does the hard work...
			new ProgressMonitorDialog(getViewSite().getShell()).run(true, true,
					new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							try {
								if (buildMaps)
									buildMaps(repo, baseCommit, compareCommit,
											monitor);
								PlatformUI.getWorkbench().getDisplay()
										.asyncExec(new Runnable() {
											@Override
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
		long previousTimeMilliseconds = System.currentTimeMillis();
		boolean useIndex = compareVersion.equals(INDEX_VERSION);
		fileNodes.clear();
		containerNodes.clear();
		boolean checkIgnored = false;
		try (TreeWalk tw = new TreeWalk(repository)) {
			int baseTreeIndex;
			if (baseCommit == null) {
				checkIgnored = true;
				baseTreeIndex = tw.addTree(new FileTreeIterator(repository));
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
				TreeFilter pathFilter = filterPaths(Arrays.stream(resources)
						.map(r -> repositoryMapping.getRepoRelativePath(r))
						.filter(p -> p != null && !p.isEmpty())
						.collect(Collectors.toList()));
				if (checkIgnored) {
					if (pathFilter != null) {
						tw.setFilter(AndTreeFilter.create(pathFilter,
								new NotIgnoredFilter(baseTreeIndex)));
					} else {
						tw.setFilter(new NotIgnoredFilter(baseTreeIndex));
					}
				} else if (pathFilter != null) {
					tw.setFilter(pathFilter);
				}
			}

			tw.setRecursive(true);
			while (tw.next()) {
				if (monitor.isCanceled())
					throw new InterruptedException();
				AbstractTreeIterator compareVersionIterator = tw.getTree(
						compareTreeIndex, AbstractTreeIterator.class);
				AbstractTreeIterator baseVersionIterator = tw.getTree(
						baseTreeIndex, AbstractTreeIterator.class);

				IFileRevision left = null;
				IFileRevision right = null;
				String repoRelativePath = baseVersionIterator != null
						? baseVersionIterator.getEntryPathString()
						: compareVersionIterator.getEntryPathString();
				IPath currentPath = new Path(repoRelativePath);

				// Updating the progress bar is slow, so just sample it. To
				// make sure slow compares are reflected in the progress
				// monitor also update before comparing large files.
				long currentTimeMilliseconds = System.currentTimeMillis();
				long size1 = -1;
				long size2 = -1;
				if (compareVersionIterator != null
						&& baseVersionIterator != null) {
					size1 = getEntrySize(tw, compareVersionIterator);
					size2 = getEntrySize(tw, baseVersionIterator);
				}
				final long REPORTSIZE = 100000;
				if (size1 > REPORTSIZE
						|| size2 > REPORTSIZE
						|| currentTimeMilliseconds - previousTimeMilliseconds > 500) {
					monitor.setTaskName(currentPath.toString());
					previousTimeMilliseconds = currentTimeMilliseconds;
				}

				Type type = null;
				if (compareVersionIterator != null
						&& baseVersionIterator != null) {
					boolean equalContent = compareVersionIterator
							.getEntryObjectId().equals(
									baseVersionIterator.getEntryObjectId());
					type = equalContent ? Type.FILE_BOTH_SIDES_SAME
							: Type.FILE_BOTH_SIDES_DIFFER;
				} else if (compareVersionIterator != null
						&& baseVersionIterator == null) {
					type = Type.FILE_DELETED;
				} else if (compareVersionIterator == null
						&& baseVersionIterator != null) {
					type = Type.FILE_ADDED;
				}

				IFile file = null;
				if (type != Type.FILE_BOTH_SIDES_SAME) {

					file = ResourceUtil.getFileForLocation(repository,
						repoRelativePath, false);
				}

				CheckoutMetadata metadata = null;
				if (baseVersionIterator != null) {
					if (baseCommit == null) {
						if (file != null)
							left = new WorkspaceFileRevision(file);
						else {
							IPath path = getRepositoryPath().append(
									repoRelativePath);
							left = new WorkingTreeFileRevision(
									path.toFile());
						}
					} else {
						metadata = new CheckoutMetadata(
								tw.getEolStreamType(
										TreeWalk.OperationType.CHECKOUT_OP),
								tw.getFilterCommand(
										Constants.ATTR_FILTER_TYPE_SMUDGE));
						left = GitFileRevision.inCommit(repository, baseCommit,
								repoRelativePath, tw.getObjectId(baseTreeIndex),
								metadata);
					}
				}

				if (compareVersionIterator != null) {
					if (!useIndex) {
						if (metadata == null) {
							metadata = new CheckoutMetadata(
									tw.getEolStreamType(
											TreeWalk.OperationType.CHECKOUT_OP),
									tw.getFilterCommand(
											Constants.ATTR_FILTER_TYPE_SMUDGE));
						}
						right = GitFileRevision.inCommit(repository,
								compareCommit, repoRelativePath,
								tw.getObjectId(compareTreeIndex), metadata);
					} else {
						right = GitFileRevision.inIndex(repository,
								repoRelativePath);
					}
				}

				IPath containerPath = currentPath.removeLastSegments(1);
				ContainerNode containerNode = getOrCreateContainerNode(
						containerPath, type);

				FileNode fileNode = new FileNode(currentPath, file, type, left,
						right);
				containerNode.addChild(fileNode);
				fileNodes.put(currentPath, fileNode);

				// If a file is not "equal content", the container nodes up to
				// the root must be shown in any case, so propagate the
				// change of the "only equal content" flag.
				if (type != Type.FILE_BOTH_SIDES_SAME) {
					IPath path = currentPath;
					while (path.segmentCount() > 0) {
						path = path.removeLastSegments(1);
						ContainerNode node = containerNodes.get(path);
						node.setOnlyEqualContent(false);
					}
				}
			}
		} finally {
			monitor.done();
		}
	}

	private TreeFilter filterPaths(Collection<String> paths) {
		if (paths.isEmpty()) {
			return null;
		}
		return PathFilterGroup.createFromStrings(paths);
	}

	private long getEntrySize(TreeWalk tw, AbstractTreeIterator iterator)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		if (iterator instanceof FileTreeIterator) {
			return ((FileTreeIterator) iterator).getEntryContentLength();
		}
		try {
			return tw.getObjectReader().getObjectSize(
					iterator.getEntryObjectId(), Constants.OBJ_BLOB);
		} catch (MissingObjectException e) {
			// in case the object is not stored as a blob and not
			// one of the known iterator types above, say zero.
			return 0;
		}
	}

	private ContainerNode getOrCreateContainerNode(IPath containerPath, Type fileType) {
		ContainerNode containerNode = containerNodes.get(containerPath);
		if (containerNode != null) {
			return containerNode;
		} else {
			Repository repository = getRepository();
			IContainer resource = repository == null ? null : ResourceUtil
					.getContainerForLocation(repository,
							containerPath.toString());
			ContainerNode node = new ContainerNode(containerPath, resource);
			node.setOnlyEqualContent(fileType == Type.FILE_BOTH_SIDES_SAME);
			containerNodes.put(containerPath, node);
			if (containerPath.segmentCount() > 0) {
				IPath parentPath = containerPath.removeLastSegments(1);
				ContainerNode parentNode = getOrCreateContainerNode(parentPath,
						fileType);
				parentNode.addChild(node);
			}
			return node;
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

	/**
	 * Base class of tree nodes.
	 */
	static abstract class PathNode {
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

		private final IPath path;

		/**
		 * @param path
		 */
		public PathNode(IPath path) {
			this.path = path;
		}

		public abstract Type getType();

		public IPath getPath() {
			return path;
		}

		public String getRepoRelativePath() {
			return path.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + path.hashCode();
			result = prime * result + getType().hashCode();
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
			if (!getType().equals(other.getType()))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return getType().name() + ": " + path.toString(); //$NON-NLS-1$
		}
	}

	/**
	 * Subclass for container (folder) tree nodes.
	 */
	static class ContainerNode extends PathNode {

		private final List<PathNode> children = new ArrayList<>();
		private final IContainer resource;
		private boolean onlyEqualContent = false;

		/**
		 * @param path
		 * @param resource container resource, may be null
		 */
		public ContainerNode(IPath path, IContainer resource) {
			super(path);
			this.resource = resource;
		}

		@Override
		public Type getType() {
			return Type.FOLDER;
		}

		public List<PathNode> getChildren() {
			return children;
		}

		public void addChild(PathNode child) {
			children.add(child);
		}

		public IContainer getResource() {
			return resource;
		}

		public void setOnlyEqualContent(boolean onlyEqualContent) {
			this.onlyEqualContent = onlyEqualContent;
		}

		public boolean isOnlyEqualContent() {
			return onlyEqualContent;
		}
	}

	/**
	 * Subclass for file tree nodes.
	 */
	static class FileNode extends PathNode {

		private final IFile file;
		private final Type type;
		private final IFileRevision leftRevision;
		private final IFileRevision rightRevision;

		/**
		 * @param path
		 * @param file
		 * @param type
		 * @param leftRevision
		 *            left revision, may be null
		 * @param rightRevision
		 *            right revision, may be null
		 */
		public FileNode(IPath path, IFile file, Type type,
				IFileRevision leftRevision, IFileRevision rightRevision) {
			super(path);
			this.file = file;
			this.type = type;
			this.leftRevision = leftRevision;
			this.rightRevision = rightRevision;
		}

		@Override
		public Type getType() {
			return type;
		}

		public IFile getFile() {
			return file;
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
				return ((PathNode) element).getType().ordinal();
			}
			return UNKNOWNCATEGORY;
		}
	}

	/**
	 * Content provider working with {@link PathNode} elements.
	 */
	private final class PathNodeContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			ContainerNode rootContainer = containerNodes.get(new Path("")); //$NON-NLS-1$
			if (rootContainer == null) {
				return new PathNode[0];
			}
			if (rootContainer.isOnlyEqualContent() && !showEquals)
				return new String[] { UIText.CompareTreeView_NoDifferencesFoundMessage };

			if (input instanceof IResource[]) {
				IResource[] resources = (IResource[]) input;
				List<PathNode> nodes = new ArrayList<>(resources.length);
				for (IResource resource : resources) {
					if (resource == null) {
						continue;
					}
					String repoRelative = repositoryMapping
							.getRepoRelativePath(resource);
					if (repoRelative == null) {
						continue;
					}
					IPath path = new Path(repoRelative);
					PathNode node;
					if (resource instanceof IFile) {
						node = fileNodes.get(path);
					} else {
						node = containerNodes.get(path);
					}
					if (node != null) {
						nodes.add(node);
					}
				}
				return nodes.toArray(new PathNode[0]);
			} else {
				return new PathNode[] { rootContainer };
			}
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof ContainerNode) {
				ContainerNode containerNode = (ContainerNode) parentElement;
				return containerNode.getChildren().toArray();
			} else
				return new Object[] {};
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof ContainerNode) {
				ContainerNode containerNode = (ContainerNode) element;
				return !containerNode.getChildren().isEmpty();
			} else
				return false;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof PathNode) {
				PathNode pathNode = (PathNode) element;
				IPath path = pathNode.getPath();
				if (path.segmentCount() == 0)
					return null;
				else
					return containerNodes.get(path.removeLastSegments(1));
			} else
				return null;
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// Do nothing
		}

		@Override
		public void dispose() {
			// Do nothing
		}
	}

	/**
	 * Used to render {@link PathNode} trees
	 */
	private final class PathNodeLabelProvider extends BaseLabelProvider
			implements ILabelProvider {

		private final WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();

		@Override
		public Image getImage(Object element) {
			if (element instanceof String)
				return null;
			if (element instanceof ContainerNode) {
				ContainerNode containerNode = (ContainerNode) element;
				IContainer resource = containerNode.getResource();
				if (resource != null && resource.isAccessible())
					return workbenchLabelProvider.getImage(resource);
				else
					return FOLDER_IMAGE;
			}
			FileNode fileNode = (FileNode) element;
			Type type = fileNode.getType();
			switch (type) {
			case FILE_BOTH_SIDES_SAME:
				return SAME_CONTENT;
			case FILE_BOTH_SIDES_DIFFER:
				if (fileNode.getFile() != null)
					return workbenchLabelProvider.getImage(fileNode.getFile());
				else
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

		@Override
		public String getText(Object element) {
			if (element instanceof String)
				return (String) element;
			IPath path = ((PathNode) element).getPath();
			if (path.segmentCount() == 0)
				return UIText.CompareTreeView_RepositoryRootName;
			return path.lastSegment();
		}

		@Override
		public void dispose() {
			super.dispose();
			workbenchLabelProvider.dispose();
		}
	}

	/**
	 * Filter out equal path nodes if "show equals" is disabled.
	 */
	private final class PathNodeFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			if (showEquals)
				return true;
			else {
				if (element instanceof FileNode) {
					FileNode fileNode = (FileNode) element;
					return fileNode.getType() != Type.FILE_BOTH_SIDES_SAME;
				} else if (element instanceof ContainerNode) {
					ContainerNode containerNode = (ContainerNode) element;
					return !containerNode.isOnlyEqualContent();
				} else if (element instanceof String)
					return true;
			}
			return true;
		}
	}

	/*
	 * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 * @since 2.1
	 */
	@Override
	public void menuAboutToShow(IMenuManager manager) {
		ITreeSelection selection = (ITreeSelection) tree.getSelection();
		if (selection.isEmpty())
			return;

		manager.add(new Separator(ICommonMenuConstants.GROUP_OPEN));
		manager.add(new Separator(ICommonMenuConstants.GROUP_ADDITIONS));

		IAction openAction = createOpenAction(selection);
		if (openAction != null)
			manager.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openAction);

		MenuManager showInSubMenu = UIUtils.createShowInMenu(
				getSite().getWorkbenchWindow());
		manager.appendToGroup(ICommonMenuConstants.GROUP_OPEN, showInSubMenu);
	}

	/*
	 * @see org.eclipse.ui.part.IShowInSource#getShowInContext()
	 * @since 2.1
	 */
	@Override
	public ShowInContext getShowInContext() {
		IPath repoPath = getRepositoryPath();
		ITreeSelection selection = (ITreeSelection) tree.getSelection();
		List<IResource> resources = new ArrayList<>();
		for (Iterator it = selection.iterator(); it.hasNext();) {
			Object obj = it.next();
			if (obj instanceof IResource)
				resources.add((IResource) obj);
			else if (obj instanceof PathNode && repoPath != null) {
				PathNode pathNode = (PathNode) obj;
				IResource resource = ResourceUtil
						.getResourceForLocation(repoPath.append(pathNode
								.getRepoRelativePath()), false);
				if (resource != null)
					resources.add(resource);
			}
		}
		return new ShowInContext(null, new StructuredSelection(resources));
	}

	private void createContextMenu() {
		MenuManager manager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(this);

		Menu contextMenu = manager.createContextMenu(tree.getControl());
		tree.getControl().setMenu(contextMenu);
		getSite().registerContextMenu(manager, tree);
	}

	private IAction createOpenAction(ITreeSelection selection) {
		final List<String> pathsToOpen = getSelectedPaths(selection);
		if (pathsToOpen == null || pathsToOpen.isEmpty())
			return null;

		return new Action(
				UIText.CommitFileDiffViewer_OpenWorkingTreeVersionInEditorMenuLabel) {

			@Override
			public void run() {
				for (String filePath : pathsToOpen) {
					DiffViewer.openFileInEditor(new File(filePath), -1);
				}
			}
		};
	}

	private List<String> getSelectedPaths(ITreeSelection selection) {
		IPath repoPath = getRepositoryPath();
		List<String> pathsToOpen = new ArrayList<>();
		for (Iterator it = selection.iterator(); it.hasNext();) {
			Object obj = it.next();
			if (obj instanceof IFile) {
				pathsToOpen.add(((IFile) obj).getLocation().toOSString());
			} else if (obj instanceof PathNode && repoPath != null) {
				PathNode pathNode = (PathNode) obj;
				if (pathNode.getType() == PathNode.Type.FOLDER
						|| pathNode.getType() == PathNode.Type.FILE_DELETED)
					return null;
				pathsToOpen.add(repoPath.append(pathNode.getPath()).toOSString());
			} else {
				return null; // fail if one of the selected elements does not fit
			}
		}
		return pathsToOpen;
	}

	private IPath getRepositoryPath() {
		Repository repo = getRepository();
		if (repo != null)
			return new Path(repo.getWorkTree().getAbsolutePath());

		return null;
	}

	private Repository getRepository() {
		if (repositoryMapping != null)
			return repositoryMapping.getRepository();
		else if (input instanceof Repository)
			return (Repository) input;

		return null;
	}

}
