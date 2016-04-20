/*******************************************************************************
 * Copyright (C) 2008, 2012 Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2012, Daniel Megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2012, Gunnar Wagenknecht <gunnar@wagenknecht.org>
 * Copyright (C) 2013, Laurent Goubet <laurent.goubet@obeo.fr>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.EgitUiEditorUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.blame.BlameOperation;
import org.eclipse.egit.ui.internal.revision.GitCompareFileRevisionEditorInput;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.themes.ColorUtil;

/**
 * Viewer to display {@link FileDiff} objects in a table.
 */
public class CommitFileDiffViewer extends TableViewer {
	private static final String LINESEP = System.getProperty("line.separator"); //$NON-NLS-1$

	private Repository db;

	private TreeWalk walker;

	private Clipboard clipboard;

	private IAction selectAll;

	private IAction copy;

	private IAction openThisVersion;

	private IAction openPreviousVersion;

	private IAction blame;

	private IAction openWorkingTreeVersion;

	private IAction compare;

	private IAction compareWorkingTreeVersion;

	private IAction showInHistory;

	private final IWorkbenchSite site;

	/**
	 * Shows a list of file changed by a commit.
	 *
	 * If no input is available, an error message is shown instead.
	 *
	 * @param parent
	 * @param site
	 */
	public CommitFileDiffViewer(final Composite parent,
			final IWorkbenchSite site) {
		this(parent, site, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER
				| SWT.FULL_SELECTION);
	}

	/**
	 * Shows a list of file changed by a commit.
	 *
	 * If no input is available, an error message is shown instead.
	 *
	 * @param parent
	 * @param site
	 * @param style SWT style bits
	 */
	public CommitFileDiffViewer(final Composite parent,
			final IWorkbenchSite site, final int style) {
		super(parent, style);
		this.site = site;
		final Table rawTable = getTable();

		Color fg = rawTable.getForeground();
		Color bg = rawTable.getBackground();
		RGB dimmedForegroundRgb = ColorUtil.blend(fg.getRGB(), bg.getRGB(), 60);

		ColumnViewerToolTipSupport.enableFor(this);

		setLabelProvider(new FileDiffLabelProvider(dimmedForegroundRgb));
		setContentProvider(new FileDiffContentProvider());
		addOpenListener(new IOpenListener() {
			@Override
			public void open(final OpenEvent event) {
				final ISelection s = event.getSelection();
				if (s.isEmpty() || !(s instanceof IStructuredSelection))
					return;
				final IStructuredSelection iss = (IStructuredSelection) s;
				final FileDiff d = (FileDiff) iss.getFirstElement();
				if (Activator.getDefault().getPreferenceStore().getBoolean(
						UIPreferences.RESOURCEHISTORY_COMPARE_MODE)) {
					if (d.getBlobs().length <= 2)
						showTwoWayFileDiff(d);
					else
						MessageDialog
								.openInformation(
										PlatformUI.getWorkbench()
												.getActiveWorkbenchWindow()
												.getShell(),
										UIText.CommitFileDiffViewer_CanNotOpenCompareEditorTitle,
										UIText.CommitFileDiffViewer_MergeCommitMultiAncestorMessage);
				} else {
					if (d.getChange() == ChangeType.DELETE)
						openPreviousVersionInEditor(d);
					else
						openThisVersionInEditor(d);
				}
			}
		});

		addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateActionEnablement(event.getSelection());
			}
		});

		clipboard = new Clipboard(rawTable.getDisplay());
		rawTable.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				clipboard.dispose();
			}
		});

		final MenuManager mgr = new MenuManager();
		Control c = getControl();
		c.setMenu(mgr.createContextMenu(c));

		openThisVersion = new Action(UIText.CommitFileDiffViewer_OpenInEditorMenuLabel) {
			@Override
			public void run() {
				final ISelection s = getSelection();
				if (s.isEmpty() || !(s instanceof IStructuredSelection))
					return;
				final IStructuredSelection iss = (IStructuredSelection) s;
				for (Object element : iss.toList())
					openThisVersionInEditor((FileDiff) element);
			}
		};

		openPreviousVersion = new Action(
				UIText.CommitFileDiffViewer_OpenPreviousInEditorMenuLabel) {
			@Override
			public void run() {
				final ISelection s = getSelection();
				if (s.isEmpty() || !(s instanceof IStructuredSelection))
					return;
				final IStructuredSelection iss = (IStructuredSelection) s;
				for (Object element : iss.toList())
					openPreviousVersionInEditor((FileDiff) element);
			}
		};

		blame = new Action(
				UIText.CommitFileDiffViewer_ShowAnnotationsMenuLabel,
				UIIcons.ANNOTATE) {
			@Override
			public void run() {
				final ISelection s = getSelection();
				if (s.isEmpty() || !(s instanceof IStructuredSelection))
					return;
				final IStructuredSelection iss = (IStructuredSelection) s;
				for (Iterator<FileDiff> it = iss.iterator(); it.hasNext();)
					showAnnotations(it.next());
			}
		};

		openWorkingTreeVersion = new Action(
				UIText.CommitFileDiffViewer_OpenWorkingTreeVersionInEditorMenuLabel) {
			@Override
			public void run() {
				final ISelection s = getSelection();
				if (s.isEmpty() || !(s instanceof IStructuredSelection))
					return;
				final IStructuredSelection iss = (IStructuredSelection) s;
				for (Iterator<FileDiff> it = iss.iterator(); it.hasNext();) {
					String relativePath = it.next().getPath();
					String path = new Path(getRepository().getWorkTree()
							.getAbsolutePath()).append(relativePath)
							.toOSString();
					openFileInEditor(path);
				}
			}
		};

		compare = new Action(UIText.CommitFileDiffViewer_CompareMenuLabel) {
			@Override
			public void run() {
				final ISelection s = getSelection();
				if (s.isEmpty() || !(s instanceof IStructuredSelection))
					return;
				final IStructuredSelection iss = (IStructuredSelection) s;
				final FileDiff d = (FileDiff) iss.getFirstElement();
				if (d.getBlobs().length <= 2)
					showTwoWayFileDiff(d);
				else
					MessageDialog
							.openInformation(
									PlatformUI.getWorkbench()
											.getActiveWorkbenchWindow()
											.getShell(),
									UIText.CommitFileDiffViewer_CanNotOpenCompareEditorTitle,
									UIText.CommitFileDiffViewer_MergeCommitMultiAncestorMessage);
			}
		};

		compareWorkingTreeVersion = new Action(
				UIText.CommitFileDiffViewer_CompareWorkingDirectoryMenuLabel) {
			@Override
			public void run() {
				final ISelection s = getSelection();
				if (s.isEmpty() || !(s instanceof IStructuredSelection))
					return;
				final IStructuredSelection iss = (IStructuredSelection) s;
				showWorkingDirectoryFileDiff((FileDiff) iss.getFirstElement());
			}
		};

		showInHistory = new Action(
				UIText.CommitFileDiffViewer_ShowInHistoryLabel, UIIcons.HISTORY) {
			@Override
			public void run() {
				ShowInContext context = getShowInContext();
				if (context == null)
					return;

				IWorkbenchWindow window = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				IWorkbenchPart part = page.getActivePart();
				// paranoia
				if (part instanceof IHistoryView) {
					((IShowInTarget) part).show(context);
				}
			}
		};

		mgr.add(openWorkingTreeVersion);
		mgr.add(openThisVersion);
		mgr.add(openPreviousVersion);
		mgr.add(new Separator());
		mgr.add(compare);
		mgr.add(compareWorkingTreeVersion);
		mgr.add(blame);

		mgr.add(new Separator());
		mgr.add(showInHistory);
		MenuManager showInSubMenu = UIUtils.createShowInMenu(site
				.getWorkbenchWindow());
		mgr.add(showInSubMenu);

		mgr.add(new Separator());
		mgr.add(selectAll = createStandardAction(ActionFactory.SELECT_ALL));
		mgr.add(copy = createStandardAction(ActionFactory.COPY));

		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=477510
		mgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				getControl().setFocus();
			}
		});
		if (site instanceof IPageSite) {
			final IPageSite pageSite = (IPageSite) site;
			getControl().addFocusListener(new FocusListener() {
				@Override
				public void focusLost(FocusEvent e) {
					pageSite.getActionBars().setGlobalActionHandler(
							ActionFactory.SELECT_ALL.getId(), null);
					pageSite.getActionBars().setGlobalActionHandler(
							ActionFactory.COPY.getId(), null);
					pageSite.getActionBars().updateActionBars();
				}

				@Override
				public void focusGained(FocusEvent e) {
					updateActionEnablement(getSelection());
					pageSite.getActionBars().setGlobalActionHandler(
							ActionFactory.SELECT_ALL.getId(), selectAll);
					pageSite.getActionBars().setGlobalActionHandler(
							ActionFactory.COPY.getId(), copy);
					pageSite.getActionBars().updateActionBars();
				}
			});
		}
	}

	private void updateActionEnablement(ISelection selection) {
		if (!(selection instanceof IStructuredSelection))
			return;
		IStructuredSelection sel = (IStructuredSelection) selection;
		boolean allSelected = !sel.isEmpty()
				&& sel.size() == getTable().getItemCount();
		boolean submoduleSelected = false;
		boolean addSelected = false;
		boolean deleteSelected = false;
		for (Object item : sel.toList()) {
			FileDiff fileDiff = (FileDiff) item;
			if (fileDiff.isSubmodule())
				submoduleSelected = true;

			if (fileDiff.getChange() == ChangeType.ADD)
				addSelected = true;
			else if (fileDiff.getChange() == ChangeType.DELETE)
				deleteSelected = true;
		}

		selectAll.setEnabled(!allSelected);
		copy.setEnabled(!sel.isEmpty());
		showInHistory.setEnabled(!sel.isEmpty());

		if (!submoduleSelected) {
			boolean oneOrMoreSelected = !sel.isEmpty();
			openThisVersion.setEnabled(oneOrMoreSelected && !deleteSelected);
			openPreviousVersion.setEnabled(oneOrMoreSelected && !addSelected);
			compare.setEnabled(sel.size() == 1);
			blame.setEnabled(oneOrMoreSelected);
			if (sel.size() == 1 && !db.isBare()) {
				FileDiff diff = (FileDiff) sel.getFirstElement();
				String path = new Path(getRepository().getWorkTree()
						.getAbsolutePath()).append(diff.getPath())
							.toOSString();
				boolean workTreeFileExists = new File(path).exists();
				compareWorkingTreeVersion.setEnabled(workTreeFileExists);
				openWorkingTreeVersion.setEnabled(workTreeFileExists);
			} else {
				compareWorkingTreeVersion.setEnabled(false);
				openWorkingTreeVersion.setEnabled(oneOrMoreSelected);
			}
		} else {
			openThisVersion.setEnabled(false);
			openPreviousVersion.setEnabled(false);
			openWorkingTreeVersion.setEnabled(false);
			compare.setEnabled(false);
			blame.setEnabled(false);
			compareWorkingTreeVersion.setEnabled(false);
		}
	}

	private IAction createStandardAction(final ActionFactory af) {
		final String text = af.create(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow()).getText();
		IAction action = new Action() {

			@Override
			public String getActionDefinitionId() {
				return af.getCommandId();
			}

			@Override
			public String getId() {
				return af.getId();
			}

			@Override
			public String getText() {
				return text;
			}

			@Override
			public void run() {
				if (af == ActionFactory.SELECT_ALL)
					doSelectAll();
				if (af == ActionFactory.COPY)
					doCopy();
			}
		};
		action.setEnabled(true);
		return action;
	}

	@Override
	protected void inputChanged(final Object input, final Object oldInput) {
		if (oldInput == null && input == null)
			return;
		super.inputChanged(input, oldInput);
		revealFirstInterestingElement();
	}

	/**
	 * @return the show in context or null
	 * @see IShowInSource#getShowInContext()
	 */
	public ShowInContext getShowInContext() {
		if (db.isBare())
			return null;
		IPath workTreePath = new Path(db.getWorkTree().getAbsolutePath());
		IStructuredSelection selection = (IStructuredSelection) getSelection();
		List<Object> elements = new ArrayList<>();
		List<File> files = new ArrayList<>();
		for (Object selectedElement : selection.toList()) {
			FileDiff fileDiff = (FileDiff) selectedElement;
			IPath path = workTreePath.append(fileDiff.getPath());
			IFile file = ResourceUtil.getFileForLocation(path, false);
			if (file != null)
				elements.add(file);
			else
				elements.add(path);
			files.add(path.toFile());
		}
		HistoryPageInput historyPageInput = null;
		if (!files.isEmpty()) {
			historyPageInput = new HistoryPageInput(db,
					files.toArray(new File[files.size()]));
		}
		return new ShowInContext(historyPageInput, new StructuredSelection(
				elements));
	}

	private void openFileInEditor(String filePath) {
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		File file = new File(filePath);
		if (!file.exists()) {
			String message = NLS.bind(UIText.CommitFileDiffViewer_FileDoesNotExist, filePath);
			Activator.showError(message, null);
		}
		IWorkbenchPage page = window.getActivePage();
		EgitUiEditorUtils.openEditor(file, page);
	}

	private void openThisVersionInEditor(FileDiff d) {
		ObjectId[] blobs = d.getBlobs();
		ObjectId blob = blobs[blobs.length - 1];
		openInEditor(d.getNewPath(), d.getCommit(), blob);
	}

	private void openPreviousVersionInEditor(FileDiff d) {
		RevCommit commit = d.getCommit().getParent(0);
		ObjectId blob = d.getBlobs()[0];
		openInEditor(d.getOldPath(), commit, blob);
	}

	private void openInEditor(String path, RevCommit commit, ObjectId blob) {
		try {
			IFileRevision rev = CompareUtils.getFileRevision(path, commit,
					getRepository(), blob);
			if (rev != null) {
				IWorkbenchWindow window = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				EgitUiEditorUtils.openEditor(page, rev,
						new NullProgressMonitor());
			} else {
				String message = NLS.bind(
						UIText.CommitFileDiffViewer_notContainedInCommit, path,
						commit.getName());
				Activator.showError(message, null);
			}
		} catch (IOException e) {
			Activator.logError(UIText.GitHistoryPage_openFailed, e);
			Activator.showError(UIText.GitHistoryPage_openFailed, null);
		} catch (CoreException e) {
			Activator.logError(UIText.GitHistoryPage_openFailed, e);
			Activator.showError(UIText.GitHistoryPage_openFailed, null);
		}
	}

	private void showAnnotations(FileDiff d) {
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();
			RevCommit commit = d.getChange().equals(ChangeType.DELETE) ? d
					.getCommit().getParent(0) : d.getCommit();
			String path = d.getPath();
			IFileRevision rev = CompareUtils.getFileRevision(path, commit,
					getRepository(),
					d.getChange().equals(ChangeType.DELETE) ? d.getBlobs()[0]
							: d.getBlobs()[d.getBlobs().length - 1]);
			if (rev != null) {
				BlameOperation op = new BlameOperation(getRepository(),
						rev.getStorage(new NullProgressMonitor()), path,
						commit, window.getShell(), page);
				JobUtil.scheduleUserJob(op, UIText.ShowBlameHandler_JobName,
						JobFamilies.BLAME);
			} else {
				String message = NLS.bind(
						UIText.CommitFileDiffViewer_notContainedInCommit,
						path, d.getCommit().getId().getName());
				Activator.showError(message, null);
			}
		} catch (IOException e) {
			Activator.logError(UIText.GitHistoryPage_openFailed, e);
			Activator.showError(UIText.GitHistoryPage_openFailed, null);
		} catch (CoreException e) {
			Activator.logError(UIText.GitHistoryPage_openFailed, e);
			Activator.showError(UIText.GitHistoryPage_openFailed, null);
		}
	}

	void showTwoWayFileDiff(final FileDiff d) {
		final String np = d.getNewPath();
		final String op = d.getOldPath();
		final RevCommit c = d.getCommit();

		// extract commits
		final RevCommit oldCommit;
		final ObjectId oldObjectId;
		if (d.getBlobs().length == 2 && !d.getChange().equals(ChangeType.ADD)) {
			oldCommit = c.getParent(0);
			oldObjectId = d.getBlobs()[0];
		} else {
			// Initial import
			oldCommit = null;
			oldObjectId = null;
		}

		final RevCommit newCommit;
		final ObjectId newObjectId;
		if (d.getChange().equals(ChangeType.DELETE)) {
			newCommit = null;
			newObjectId = null;
		} else {
			newCommit = c;
			newObjectId = d.getBlobs()[1];
		}

		IWorkbenchPage page = site.getWorkbenchWindow().getActivePage();
		if (oldCommit != null && newCommit != null) {
			IFile file = ResourceUtil.getFileForLocation(getRepository(), np, false);
			try {
				if (file != null) {
					IResource[] resources = new IResource[] { file, };
					CompareUtils.compare(resources, getRepository(), np, op,
							newCommit.getName(), oldCommit.getName(), false,
							page);
				} else {
					IPath location = new Path(getRepository().getWorkTree()
							.getAbsolutePath()).append(np);
					CompareUtils.compare(location, getRepository(),
							newCommit.getName(), oldCommit.getName(), false,
							page);
				}
			} catch (Exception e) {
				Activator.logError(UIText.GitHistoryPage_openFailed, e);
				Activator.showError(UIText.GitHistoryPage_openFailed, null);
			}
			return;
		}

		// still happens on initial commits
		final ITypedElement oldSide = createTypedElement(op, oldCommit,
				oldObjectId);
		final ITypedElement newSide = createTypedElement(np, newCommit,
				newObjectId);
		CompareUtils.openInCompare(page, new GitCompareFileRevisionEditorInput(
				newSide, oldSide, null));
	}

	private ITypedElement createTypedElement(final String path,
			final RevCommit commit, final ObjectId objectId) {
		if (null != commit)
			return CompareUtils.getFileRevisionTypedElement(path, commit,
					getRepository(), objectId);
		else
			return new GitCompareFileRevisionEditorInput.EmptyTypedElement(""); //$NON-NLS-1$
	}

	void showWorkingDirectoryFileDiff(final FileDiff d) {
		final String p = d.getPath();
		final RevCommit commit = d.getCommit();

		if (commit == null) {
			Activator.showError(UIText.GitHistoryPage_openFailed, null);
			return;
		}

		IWorkbenchPage activePage = site.getWorkbenchWindow().getActivePage();
		IFile file = ResourceUtil.getFileForLocation(getRepository(), p, false);
		try {
			if (file != null) {
				final IResource[] resources = new IResource[] { file, };
				CompareUtils.compare(resources, getRepository(),
						Constants.HEAD, commit.getName(), true, activePage);
			} else {
				IPath path = new Path(getRepository().getWorkTree()
						.getAbsolutePath()).append(p);
				File ioFile = path.toFile();
				if (ioFile.exists())
					CompareUtils.compare(path, getRepository(), Constants.HEAD,
							commit.getName(), true, activePage);
			}
		} catch (IOException e) {
			Activator.logError(UIText.GitHistoryPage_openFailed, e);
			Activator.showError(UIText.GitHistoryPage_openFailed, null);
		}
	}

	TreeWalk getTreeWalk() {
		if (walker == null)
			throw new IllegalStateException("TreeWalk has not been set"); //$NON-NLS-1$
		return walker;
	}

	Repository getRepository() {
		if (db == null)
			throw new IllegalStateException("Repository has not been set"); //$NON-NLS-1$
		return db;
	}

	/**
	 * Set repository and tree walk
	 *
	 * @param repository
	 * @param walk
	 */
	public void setTreeWalk(Repository repository, TreeWalk walk) {
		db = repository;
		walker = walk;
	}

	private void doSelectAll() {
		final IStructuredContentProvider cp;
		final Object in = getInput();
		if (in == null)
			return;

		cp = ((IStructuredContentProvider) getContentProvider());
		final Object[] el = cp.getElements(in);
		if (el == null || el.length == 0)
			return;
		setSelection(new StructuredSelection(el));
	}

	private void doCopy() {
		final ISelection s = getSelection();
		if (s.isEmpty() || !(s instanceof IStructuredSelection))
			return;
		final IStructuredSelection iss = (IStructuredSelection) s;
		final Iterator<FileDiff> itr = iss.iterator();
		final StringBuilder r = new StringBuilder();
		while (itr.hasNext()) {
			final FileDiff d = itr.next();
			if (r.length() > 0)
				r.append(LINESEP);
			r.append(d.getNewPath());
		}

		clipboard.setContents(new Object[] { r.toString() },
				new Transfer[] { TextTransfer.getInstance() }, DND.CLIPBOARD);
	}

	/**
	 * @see FileDiffContentProvider#setInterestingPaths(Set)
	 * @param interestingPaths
	 */
	void setInterestingPaths(Set<String> interestingPaths) {
		((FileDiffContentProvider) getContentProvider()).setInterestingPaths(interestingPaths);
	}

	void selectFirstInterestingElement() {
		IStructuredContentProvider contentProvider = ((IStructuredContentProvider) getContentProvider());
		Object[] elements = contentProvider.getElements(getInput());
		for (final Object element : elements) {
			if (element instanceof FileDiff) {
				FileDiff fileDiff = (FileDiff) element;
				boolean marked = fileDiff
						.isMarked(FileDiffContentProvider.INTERESTING_MARK_TREE_FILTER_INDEX);
				if (marked) {
					setSelection(new StructuredSelection(fileDiff));
					return;
				}
			}
		}
	}

	private void revealFirstInterestingElement() {
		IStructuredContentProvider contentProvider = ((IStructuredContentProvider) getContentProvider());
		Object[] elements = contentProvider.getElements(getInput());
		if (elements.length <= 1)
			return;

		for (final Object element : elements) {
			if (element instanceof FileDiff) {
				FileDiff fileDiff = (FileDiff) element;
				boolean marked = fileDiff.isMarked(FileDiffContentProvider.INTERESTING_MARK_TREE_FILTER_INDEX);
				if (marked) {
					// Does not yet work reliably, see comment on bug 393610.
					getTable().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							reveal(element);
						}
					});
					// Only reveal first
					return;
				}
			}
		}
	}
}
