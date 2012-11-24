/*******************************************************************************
 * Copyright (C) 2008, 2012 Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2012, Daniel Megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2012, Gunnar Wagenknecht <gunnar@wagenknecht.org>
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

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.EgitUiEditorUtils;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.egit.ui.internal.blame.BlameOperation;
import org.eclipse.egit.ui.internal.synchronize.GitModelSynchronize;
import org.eclipse.egit.ui.internal.synchronize.compare.LocalNonWorkspaceTypedElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;

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

	private IAction open;

	private IAction blame;

	private IAction openWorkingTreeVersion;

	private IAction compare;

	private IAction compareWorkingTreeVersion;

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

		rawTable.setLinesVisible(true);

		setLabelProvider(new FileDiffLabelProvider());
		setContentProvider(new FileDiffContentProvider());
		addOpenListener(new IOpenListener() {
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
				} else
					openFileInEditor(d);
			}
		});

		addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateActionEnablement(event.getSelection());
			}
		});

		clipboard = new Clipboard(rawTable.getDisplay());
		rawTable.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				clipboard.dispose();
			}
		});

		final MenuManager mgr = new MenuManager();
		Control c = getControl();
		c.setMenu(mgr.createContextMenu(c));

		open = new Action(UIText.CommitFileDiffViewer_OpenInEditorMenuLabel) {
			@Override
			public void run() {
				final ISelection s = getSelection();
				if (s.isEmpty() || !(s instanceof IStructuredSelection))
					return;
				final IStructuredSelection iss = (IStructuredSelection) s;
				for (Iterator<FileDiff> it = iss.iterator(); it.hasNext();)
					openFileInEditor(it.next());
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

		mgr.add(openWorkingTreeVersion);
		mgr.add(open);
		mgr.add(compare);
		mgr.add(compareWorkingTreeVersion);
		mgr.add(blame);

		MenuManager showInSubMenu = UIUtils.createShowInMenu(
				site.getWorkbenchWindow());

		mgr.add(new Separator());
		mgr.add(showInSubMenu);

		mgr.add(new Separator());
		mgr.add(selectAll = createStandardAction(ActionFactory.SELECT_ALL));
		mgr.add(copy = createStandardAction(ActionFactory.COPY));

		if (site instanceof IPageSite) {
			final IPageSite pageSite = (IPageSite) site;
			getControl().addFocusListener(new FocusListener() {
				public void focusLost(FocusEvent e) {
					pageSite.getActionBars().setGlobalActionHandler(
							ActionFactory.SELECT_ALL.getId(), null);
					pageSite.getActionBars().setGlobalActionHandler(
							ActionFactory.COPY.getId(), null);
					pageSite.getActionBars().updateActionBars();
				}

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
		for (Object item : sel.toArray())
			if (((FileDiff) item).isSubmodule()) {
				submoduleSelected = true;
				break;
			}

		selectAll.setEnabled(!allSelected);
		copy.setEnabled(!sel.isEmpty());

		if (!submoduleSelected) {
			boolean oneOrMoreSelected = !sel.isEmpty();
			open.setEnabled(oneOrMoreSelected);
			openWorkingTreeVersion.setEnabled(oneOrMoreSelected);
			compare.setEnabled(sel.size() == 1);
			blame.setEnabled(oneOrMoreSelected);
			if (sel.size() == 1) {
				FileDiff diff = (FileDiff) sel.getFirstElement();
				String path = new Path(getRepository().getWorkTree()
						.getAbsolutePath()).append(diff.getPath()).toOSString();
				compareWorkingTreeVersion.setEnabled(new File(path).exists()
						&& !submoduleSelected);
			} else
				compareWorkingTreeVersion.setEnabled(false);
		} else {
			open.setEnabled(false);
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
		List<Object> elements = new ArrayList<Object>();
		for (Object selectedElement : selection.toList()) {
			FileDiff fileDiff = (FileDiff) selectedElement;
			IPath path = workTreePath.append(fileDiff.getPath());
			IFile file = ResourceUtil.getFileForLocation(path);
			if (file != null)
				elements.add(file);
			else
				elements.add(path);
		}
		return new ShowInContext(null, new StructuredSelection(elements));
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

	private void openFileInEditor(FileDiff d) {
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();
			IFileRevision rev = CompareUtils.getFileRevision(d.getPath(), d
					.getChange().equals(ChangeType.DELETE) ? d.getCommit()
					.getParent(0) : d.getCommit(), getRepository(), d
					.getChange().equals(ChangeType.DELETE) ? d.getBlobs()[0]
					: d.getBlobs()[d.getBlobs().length - 1]);
			if (rev != null)
				EgitUiEditorUtils.openEditor(page, rev,
						new NullProgressMonitor());
			else {
				String message = NLS.bind(
						UIText.CommitFileDiffViewer_notContainedInCommit, d
								.getPath(), d.getCommit().getId().getName());
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
			IFileRevision rev = CompareUtils.getFileRevision(d.getPath(),
					commit, getRepository(),
					d.getChange().equals(ChangeType.DELETE) ? d.getBlobs()[0]
							: d.getBlobs()[d.getBlobs().length - 1]);
			if (rev != null) {
				BlameOperation op = new BlameOperation(getRepository(),
						rev.getStorage(new NullProgressMonitor()), d.getPath(),
						commit, window.getShell(), page);
				JobUtil.scheduleUserJob(op, UIText.ShowBlameHandler_JobName,
						JobFamilies.BLAME);
			} else {
				String message = NLS.bind(
						UIText.CommitFileDiffViewer_notContainedInCommit,
						d.getPath(), d.getCommit().getId().getName());
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
		final String p = d.getPath();
		final RevCommit c = d.getCommit();

		// extract commits
		final RevCommit leftCommit;
		final ObjectId baseObjectId;
		if (d.getBlobs().length == 2 && !d.getChange().equals(ChangeType.ADD)) {
			leftCommit = c.getParent(0);
			baseObjectId = d.getBlobs()[0];
		} else {
			// Initial import
			leftCommit = null;
			baseObjectId = null;
		}

		final RevCommit rightCommit;
		final ObjectId rightObjectId;
		if (d.getChange().equals(ChangeType.DELETE)) {
			rightCommit = null;
			rightObjectId = null;
		} else {
			rightCommit = c;
			rightObjectId = d.getBlobs()[1];
		}


		// determine (from a local available file) if a model compare is possible
		IFile file = ResourceUtil.getFileForLocation(getRepository(), p);
		if (file != null && leftCommit != null && rightCommit != null) {
			if (!CompareUtils.canDirectlyOpenInCompare(file)) {
				try {
					GitModelSynchronize.synchronizeModelBetweenRefs(file,
							getRepository(), leftCommit.getName(),
							rightCommit.getName());
				} catch (Exception e) {
					Activator.logError(UIText.GitHistoryPage_openFailed, e);
					Activator.showError(UIText.GitHistoryPage_openFailed, null);
				}
				return;
			}
		}

		final ITypedElement base = createTypedElement(p, leftCommit, baseObjectId);
		final ITypedElement next = createTypedElement(p, rightCommit, rightObjectId);
		CompareUtils.openInCompare(site.getWorkbenchWindow().getActivePage(),
				new GitCompareFileRevisionEditorInput(next, base, null));
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
		final GitCompareFileRevisionEditorInput in;

		final String p = d.getPath();
		final RevCommit commit = d.getCommit();
		final ObjectId[] blobs = d.getBlobs();
		final ITypedElement base;
		final ITypedElement next;

		IFile file = ResourceUtil.getFileForLocation(getRepository(), p);
		if (file != null && commit != null) {
			if (!CompareUtils.canDirectlyOpenInCompare(file)) {
				try {
					GitModelSynchronize.synchronizeModelWithWorkspace(file,
							getRepository(), commit.getName());
				} catch (Exception e) {
					Activator.logError(UIText.GitHistoryPage_openFailed, e);
					Activator.showError(UIText.GitHistoryPage_openFailed, null);
				}
				return;
			}
		}

		if (file != null)
			next = SaveableCompareEditorInput.createFileElement(file);
		else
			next = new LocalNonWorkspaceTypedElement(new Path(getRepository()
					.getWorkTree().getAbsolutePath()).append(p));

		if (d.getChange().equals(ChangeType.DELETE))
			base = new GitCompareFileRevisionEditorInput.EmptyTypedElement(""); //$NON-NLS-1$
		else
			base = CompareUtils.getFileRevisionTypedElement(p, commit,
					getRepository(), blobs[blobs.length - 1]);

		in = new GitCompareFileRevisionEditorInput(next, base, null);
		CompareUtils.openInCompare(site.getWorkbenchWindow().getActivePage(),
				in);
	}

	TreeWalk getTreeWalk() {
		if (walker == null)
			throw new IllegalStateException("TreeWalk has not been set"); //$NON-NLS-1$
		return walker;
	}

	private Repository getRepository() {
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
			r.append(d.getPath());
		}

		clipboard.setContents(new Object[] { r.toString() },
				new Transfer[] { TextTransfer.getInstance() }, DND.CLIPBOARD);
	}
}
