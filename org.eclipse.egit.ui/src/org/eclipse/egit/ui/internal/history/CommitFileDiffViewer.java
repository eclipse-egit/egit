/*******************************************************************************
 * Copyright (C) 2008, 2012 Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2012, Daniel Megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2012, Gunnar Wagenknecht <gunnar@wagenknecht.org>
 * Copyright (C) 2013, Laurent Goubet <laurent.goubet@obeo.fr>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.storage.CommitFileRevision;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.ActionUtils;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.blame.BlameOperation;
import org.eclipse.egit.ui.internal.commit.DiffViewer;
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
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Constants;
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
	 * @param style
	 *            SWT style bits
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
				ISelection s = event.getSelection();
				if (s.isEmpty() || !(s instanceof IStructuredSelection)) {
					return;
				}
				IStructuredSelection iss = (IStructuredSelection) s;
				FileDiff d = (FileDiff) iss.getFirstElement();
				if (Activator.getDefault().getPreferenceStore().getBoolean(
						UIPreferences.RESOURCEHISTORY_COMPARE_MODE)) {
					showTwoWayFileDiff(d);
				} else {
					if (d.getChange() == ChangeType.DELETE) {
						openPreviousVersionInEditor(d);
					} else {
						openThisVersionInEditor(d);
					}
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

		openThisVersion = new Action(
				UIText.CommitFileDiffViewer_OpenInEditorMenuLabel) {
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

		blame = new Action(UIText.CommitFileDiffViewer_ShowAnnotationsMenuLabel,
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
					File file = new Path(
							getRepository().getWorkTree().getAbsolutePath())
									.append(relativePath).toFile();
					DiffViewer.openFileInEditor(file, -1);
				}
			}
		};

		compare = new Action(UIText.CommitFileDiffViewer_CompareMenuLabel) {
			@Override
			public void run() {
				ISelection s = getSelection();
				if (s.isEmpty() || !(s instanceof IStructuredSelection)) {
					return;
				}
				IStructuredSelection iss = (IStructuredSelection) s;
				FileDiff d = (FileDiff) iss.getFirstElement();
				showTwoWayFileDiff(d);
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
				UIText.CommitFileDiffViewer_ShowInHistoryLabel,
				UIIcons.HISTORY) {
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
		MenuManager showInSubMenu = UIUtils
				.createShowInMenu(site.getWorkbenchWindow());
		mgr.add(showInSubMenu);

		mgr.add(new Separator());
		selectAll = ActionUtils.createGlobalAction(ActionFactory.SELECT_ALL,
				() -> doSelectAll());
		selectAll.setEnabled(true);
		copy = ActionUtils.createGlobalAction(ActionFactory.COPY,
				() -> doCopy());
		copy.setEnabled(true);
		ActionUtils.setGlobalActions(getControl(), copy, selectAll);
		mgr.add(selectAll);
		mgr.add(copy);

		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=477510
		mgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				getControl().setFocus();
			}
		});
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
				String path = new Path(
						getRepository().getWorkTree().getAbsolutePath())
								.append(diff.getPath()).toOSString();
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
		return new ShowInContext(historyPageInput,
				new StructuredSelection(elements));
	}

	private void openThisVersionInEditor(FileDiff d) {
		DiffViewer.openInEditor(getRepository(), d, DiffEntry.Side.NEW, -1);
	}

	private void openPreviousVersionInEditor(FileDiff d) {
		DiffViewer.openInEditor(getRepository(), d, DiffEntry.Side.OLD, -1);
	}

	private void showAnnotations(FileDiff d) {
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();
			RevCommit commit = d.getChange().equals(ChangeType.DELETE)
					? d.getCommit().getParent(0) : d.getCommit();
			String path = d.getPath();
			IFileRevision rev = CompareUtils.getFileRevision(path, commit,
					getRepository(),
					d.getChange().equals(ChangeType.DELETE) ? d.getBlobs()[0]
							: d.getBlobs()[d.getBlobs().length - 1]);
			if (rev instanceof CommitFileRevision) {
				BlameOperation op = new BlameOperation((CommitFileRevision) rev,
						window.getShell(), page);
				JobUtil.scheduleUserJob(op, UIText.ShowBlameHandler_JobName,
						JobFamilies.BLAME);
			} else {
				String message = NLS.bind(
						UIText.DiffViewer_notContainedInCommit, path,
						d.getCommit().getId().getName());
				Activator.showError(message, null);
			}
		} catch (IOException e) {
			Activator.logError(UIText.GitHistoryPage_openFailed, e);
			Activator.showError(UIText.GitHistoryPage_openFailed, null);
		}
	}

	void showTwoWayFileDiff(final FileDiff d) {
		if (d.getBlobs().length <= 2) {
			DiffViewer.showTwoWayFileDiff(getRepository(), d);
		} else {
			MessageDialog.openInformation(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getShell(),
					UIText.CommitFileDiffViewer_CanNotOpenCompareEditorTitle,
					UIText.CommitFileDiffViewer_MergeCommitMultiAncestorMessage);
		}
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
				CompareUtils.compare(resources, getRepository(), Constants.HEAD,
						commit.getName(), true, activePage);
			} else {
				IPath path = new Path(
						getRepository().getWorkTree().getAbsolutePath())
								.append(p);
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

	@NonNull
	Repository getRepository() {
		Repository repo = db;
		if (repo == null) {
			throw new IllegalStateException("Repository has not been set"); //$NON-NLS-1$
		}
		return repo;
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
			r.append(d.getPath());
		}

		clipboard.setContents(new Object[] { r.toString() },
				new Transfer[] { TextTransfer.getInstance() }, DND.CLIPBOARD);
	}

	/**
	 * @see FileDiffContentProvider#setInterestingPaths(Set)
	 * @param interestingPaths
	 */
	void setInterestingPaths(Set<String> interestingPaths) {
		((FileDiffContentProvider) getContentProvider())
				.setInterestingPaths(interestingPaths);
	}

	void selectFirstInterestingElement() {
		IStructuredContentProvider contentProvider = ((IStructuredContentProvider) getContentProvider());
		Object[] elements = contentProvider.getElements(getInput());
		for (final Object element : elements) {
			if (element instanceof FileDiff) {
				FileDiff fileDiff = (FileDiff) element;
				boolean marked = fileDiff.isMarked(
						FileDiffContentProvider.INTERESTING_MARK_TREE_FILTER_INDEX);
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
				boolean marked = fileDiff.isMarked(
						FileDiffContentProvider.INTERESTING_MARK_TREE_FILTER_INDEX);
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
