/*******************************************************************************
 * Copyright (C) 2008, 2012 Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2012, Daniel Megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2012, Gunnar Wagenknecht <gunnar@wagenknecht.org>
 * Copyright (C) 2013, Laurent Goubet <laurent.goubet@obeo.fr>
 * Copyright (C) 2016, 2019 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.ActionUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.DiffViewer;
import org.eclipse.egit.ui.internal.dialogs.CommandConfirmation;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
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
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.themes.ColorUtil;

/**
 * Viewer to display {@link FileDiff} objects in a table.
 */
public class CommitFileDiffViewer extends TableViewer {

	/** Popup menu ID. */
	public static final String POPUP_ID = "org.eclipse.egit.ui.fileDiffContributions"; //$NON-NLS-1$

	/** Menu group ID. */
	public static final String GROUP_ID = "fileDiff.group"; //$NON-NLS-1$

	static final int INTERESTING_MARK_TREE_FILTER_INDEX = 0;

	private static final String LINESEP = System.lineSeparator();

	private Clipboard clipboard;

	private IAction selectAll;

	private IAction copy;

	private IAction copyAll;

	private IAction checkOutThisVersion;

	private IAction openThisVersion;

	private IAction openPreviousVersion;

	private IAction openWorkingTreeVersion;

	private IAction showInHistory;

	private FileDiffInput realInput;

	private FileDiffLoader loader;

	private MenuManager menuManager;

	/**
	 * Shows a list of file changed by a commit. The viewer is created with the
	 * default styles: a virtual table with borders and scroll bars, with
	 * multi-selection enabled.
	 *
	 * @param parent
	 * @param site
	 */
	public CommitFileDiffViewer(final Composite parent,
			final IWorkbenchSite site) {
		this(parent, site, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER
				| SWT.VIRTUAL | SWT.FULL_SELECTION);
	}

	/**
	 * Shows a list of file changed by a commit. The viewer is created with the
	 * given SWT styles and has by default an {@link ArrayContentProvider}. A
	 * label provider and {@link ViewerComparator} are set, and a context menu
	 * is set up.
	 *
	 * @param parent
	 * @param site
	 * @param style
	 *            SWT style bits
	 */
	public CommitFileDiffViewer(final Composite parent,
			final IWorkbenchSite site, final int style) {
		super(parent, style);
		setUseHashlookup(true);
		final Table rawTable = getTable();

		Color fg = rawTable.getForeground();
		Color bg = rawTable.getBackground();
		RGB dimmedForegroundRgb = ColorUtil.blend(fg.getRGB(), bg.getRGB(), 60);

		ColumnViewerToolTipSupport.enableFor(this);

		setLabelProvider(new FileDiffLabelProvider(dimmedForegroundRgb));
		setContentProvider(ArrayContentProvider.getInstance());
		setComparator(new ViewerComparator() {

			@Override
			public int compare(Viewer viewer, Object left, Object right) {
				if (left instanceof FileDiff && right instanceof FileDiff) {
					return FileDiff.PATH_COMPARATOR.compare((FileDiff) left,
							(FileDiff) right);
				}
				return super.compare(viewer, left, right);
			}
		});
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

		clipboard = new Clipboard(rawTable.getDisplay());
		rawTable.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				clipboard.dispose();
			}
		});

		final MenuManager mgr = new MenuManager();
		rawTable.setMenu(mgr.createContextMenu(rawTable));

		checkOutThisVersion = new CheckoutAction(this::getStructuredSelection);

		openThisVersion = new Action(
				UIText.CommitFileDiffViewer_OpenInEditorMenuLabel) {
			@Override
			public void run() {
				withSelection(
						CommitFileDiffViewer.this::openThisVersionInEditor);
			}
		};

		openPreviousVersion = new Action(
				UIText.CommitFileDiffViewer_OpenPreviousInEditorMenuLabel) {
			@Override
			public void run() {
				withSelection(
						CommitFileDiffViewer.this::openPreviousVersionInEditor);
			}
		};

		openWorkingTreeVersion = new Action(
				UIText.CommitFileDiffViewer_OpenWorkingTreeVersionInEditorMenuLabel) {
			@Override
			public void run() {
				withSelection(d -> {
					String relativePath = d.getPath();
					File file = new Path(
							d.getRepository().getWorkTree()
									.getAbsolutePath())
									.append(relativePath).toFile();
					DiffViewer.openFileInEditor(file, -1);
				});
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

		copy = ActionUtils.createGlobalAction(ActionFactory.COPY,
				() -> {
					IStructuredSelection selection = getStructuredSelection();
					if (selection != null && !selection.isEmpty()) {
						doCopy(selection.iterator());
					}
				});
		copy.setText(UIText.CommitFileDiffViewer_CopyFilePathMenuLabel);
		copy.setEnabled(true);
		if ((rawTable.getStyle() & SWT.MULTI) != 0) {
			selectAll = ActionUtils.createGlobalAction(ActionFactory.SELECT_ALL,
					this::doSelectAll);
			selectAll.setEnabled(true);
			ActionUtils.setGlobalActions(rawTable, copy, selectAll);
		} else {
			ActionUtils.setGlobalActions(rawTable, copy);
		}
		copyAll = new Action(
				UIText.CommitFileDiffViewer_CopyAllFilePathsMenuLabel) {

			@Override
			public void run() {
				doCopy(Arrays
						.asList(((IStructuredContentProvider) getContentProvider())
								.getElements(getInput()))
						.iterator());
			}
		};

		mgr.setRemoveAllWhenShown(true);
		mgr.addMenuListener(manager -> {
			getControl().setFocus();
			updateActionEnablement(getStructuredSelection());
			mgr.add(openWorkingTreeVersion);
			mgr.add(openThisVersion);
			mgr.add(openPreviousVersion);
			mgr.add(new Separator());
			mgr.add(checkOutThisVersion);
			mgr.add(new Separator(GROUP_ID));

			mgr.add(new Separator());
			mgr.add(showInHistory);
			MenuManager showInSubMenu = UIUtils
					.createShowInMenu(site.getWorkbenchWindow());
			mgr.add(showInSubMenu);
			mgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

			mgr.add(new Separator());
			if (selectAll != null) {
				mgr.add(selectAll);
			}
			mgr.add(copy);
			mgr.add(copyAll);
		});
		menuManager = mgr;
	}

	/**
	 * Retrieves the {@link MenuManager} of this viewer.
	 *
	 * @return the {@link MenuManager}
	 */
	public MenuManager getMenuManager() {
		return menuManager;
	}

	private void withSelection(Consumer<FileDiff> consumer) {
		IStructuredSelection selection = getStructuredSelection();
		if (selection == null || selection.isEmpty()) {
			return;
		}
		Iterator<?> items = selection.iterator();
		items.forEachRemaining(o -> {
			if (o instanceof FileDiff) {
				consumer.accept((FileDiff) o);
			}
		});
	}

	@Override
	protected void inputChanged(Object input, Object oldInput) {
		super.inputChanged(input, oldInput);
		copyAll.setEnabled(
				getContentProvider() instanceof IStructuredContentProvider
						&& doGetItemCount() > 0);
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
		Repository repository = null;
		for (Object item : sel.toList()) {
			FileDiff fileDiff = (FileDiff) item;
			if (repository == null) {
				repository = fileDiff.getRepository();
			}
			if (fileDiff.isSubmodule()) {
				submoduleSelected = true;
			}
			if (fileDiff.getChange() == ChangeType.ADD) {
				addSelected = true;
			} else if (fileDiff.getChange() == ChangeType.DELETE) {
				deleteSelected = true;
			}
		}

		boolean isBare = repository == null || repository.isBare();
		if (selectAll != null) {
			selectAll.setEnabled(!allSelected);
		}
		copy.setEnabled(!sel.isEmpty());
		showInHistory.setEnabled(!sel.isEmpty());

		if (!submoduleSelected) {
			boolean oneOrMoreSelected = !sel.isEmpty();
			checkOutThisVersion.setEnabled(
					oneOrMoreSelected && repository != null && repository
							.getRepositoryState().equals(RepositoryState.SAFE));
			openThisVersion.setEnabled(oneOrMoreSelected && !deleteSelected);
			openPreviousVersion.setEnabled(oneOrMoreSelected && !addSelected);
			if (sel.size() == 1) {
				FileDiff diff = (FileDiff) sel.getFirstElement();
				boolean workTreeFileExists = false;
				if (!isBare && repository != null) {
					String path = new Path(
							repository.getWorkTree().getAbsolutePath())
							.append(diff.getPath()).toOSString();
					workTreeFileExists = new File(path).exists();
				}
				openWorkingTreeVersion.setEnabled(workTreeFileExists);
			} else {
				openWorkingTreeVersion.setEnabled(oneOrMoreSelected && !isBare);
			}
		} else {
			checkOutThisVersion.setEnabled(false);
			openThisVersion.setEnabled(false);
			openPreviousVersion.setEnabled(false);
			openWorkingTreeVersion.setEnabled(false);
		}
	}

	@Override
	protected void handleDispose(DisposeEvent event) {
		cancelJob();
		realInput = null;
		if (menuManager != null) {
			menuManager.dispose();
		}
		super.handleDispose(event);
	}

	/**
	 * A variant of
	 * {@link org.eclipse.jface.viewers.StructuredViewer#setInput(Object)
	 * setInput(Object)} that clears the selection before setting the new input
	 * if it is known that it cannot be reset after the input has been changed.
	 *
	 * @param input
	 *            to set
	 */
	public void newInput(Object input) {
		cancelJob();
		if (input == null) {
			setSelection(StructuredSelection.EMPTY);
			setInput(new Object());
		} else {
			if (realInput != null) {
				if (input instanceof FileDiffInput) {
					FileDiffInput newInput = (FileDiffInput) input;
					if (!Objects.equals(realInput.getRepository(),
							newInput.getRepository())
							|| realInput.isFirstParentOnly() != newInput
									.isFirstParentOnly()
							|| !realInput.getCommit()
									.equals(newInput.getCommit())) {
						setSelection(StructuredSelection.EMPTY);
						setInput(new Object());
					}
				}
			}
		}
		if (input instanceof FileDiffInput) {
			realInput = (FileDiffInput) input;
			startJob((FileDiffInput) input);
		} else {
			realInput = null;
			setInput(input);
		}
	}

	@Override
	protected void setSelectionToWidget(List list, boolean reveal) {
		// setSelection(StructuredSelection.EMPTY) is not the same
		// as setSelection(null). However, the latter is undocumented.
		// Ensure here that we do take all possible shortcuts and just
		// clear the selection (normally via doDeselectAll()) if the
		// list is non-null but empty.
		if (list != null && list.isEmpty()) {
			list = null;
		}
		super.setSelectionToWidget(list, reveal);
	}

	/**
	 * @return the show in context or null
	 * @see IShowInSource#getShowInContext()
	 */
	public ShowInContext getShowInContext() {
		IStructuredSelection selection = getStructuredSelection();
		List<Object> elements = new ArrayList<>();
		List<File> files = new ArrayList<>();
		Repository repo = null;
		IPath workTreePath = null;
		boolean isBare = false;
		for (Object selectedElement : selection.toList()) {
			FileDiff fileDiff = (FileDiff) selectedElement;
			if (repo == null) {
				repo = fileDiff.getRepository();
				if (repo == null) {
					return null;
				}
				isBare = repo.isBare();
				if (!isBare) {
					workTreePath = new Path(
							repo.getWorkTree().getAbsolutePath());
				}
			}
			IFile file = null;
			IPath path;
			if (!isBare && workTreePath != null) {
				path = workTreePath.append(fileDiff.getPath());
				file = ResourceUtil.getFileForLocation(path, false);
				if (file != null) {
					elements.add(file);
				} else {
					elements.add(path);
				}
				files.add(path.toFile());
			} else {
				path = Path.fromPortableString(fileDiff.getGitPath());
				files.add(path.toFile());
				elements.add(fileDiff);
			}
		}
		HistoryPageInput historyPageInput = null;
		if (!files.isEmpty()) {
			historyPageInput = new HistoryPageInput(repo,
					files.toArray(new File[0]));
		}
		return new ShowInContext(historyPageInput,
				new StructuredSelection(elements));
	}

	private void openThisVersionInEditor(FileDiff d) {
		DiffViewer.openInEditor(d, DiffEntry.Side.NEW, -1);
	}

	private void openPreviousVersionInEditor(FileDiff d) {
		DiffViewer.openInEditor(d, DiffEntry.Side.OLD, -1);
	}

	void showTwoWayFileDiff(final FileDiff d) {
		DiffViewer.showTwoWayFileDiff(d);
	}

	private void doSelectAll() {
		if (getInput() != null) {
			Table table = getTable();
			if (table != null) {
				table.selectAll();
				setSelection(getSelection());
			}
		}
	}

	private void doCopy(Iterator<?> items) {
		final StringBuilder r = new StringBuilder();
		while (items.hasNext()) {
			Object obj = items.next();
			if (obj instanceof FileDiff) {
				if (r.length() > 0)
					r.append(LINESEP);
				r.append(((FileDiff) obj).getPath());
			}
		}

		clipboard.setContents(new Object[] { r.toString() },
				new Transfer[] { TextTransfer.getInstance() }, DND.CLIPBOARD);
	}

	/**
	 * Set the interesting paths to be marked and re-compute and update the UI.
	 *
	 * @param interestingPaths
	 *            to be marked
	 */
	void setInterestingPaths(Collection<String> interestingPaths) {
		if (realInput != null) {
			cancelJob();
			realInput.setInterestingPaths(interestingPaths);
			startJob(realInput);
		}
	}

	private TreeFilter toFilter(Collection<String> paths) {
		if (paths != null && !paths.isEmpty()) {
			return PathFilterGroup.createFromStrings(paths);
		} else {
			return TreeFilter.ALL;
		}
	}

	private void startJob(FileDiffInput input) {
		FileDiffLoader job = new FileDiffLoader(input,
				toFilter(input.getInterestingPaths()));
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (!event.getResult().isOK()) {
					return;
				}
				UIJob updater = new UpdateJob(MessageFormat.format(
						UIText.CommitFileDiffViewer_updatingFileDiffs,
						input.getCommit().getName()), job);
				updater.schedule();
			}
		});
		job.setUser(false);
		job.setSystem(true);
		loader = job;
		loader.schedule();
	}

	private void cancelJob() {
		if (loader != null) {
			loader.cancel();
			loader = null;
		}
		Job.getJobManager().cancel(JobFamilies.HISTORY_FILE_DIFF);
	}

	private static class FileDiffLoader extends Job {

		private FileDiff[] diffs;

		private final FileDiffInput input;

		private final TreeFilter filter;

		public FileDiffLoader(FileDiffInput input, TreeFilter filter) {
			super(MessageFormat.format(
					UIText.CommitFileDiffViewer_computingFileDiffs,
					input.getCommit().getName()));
			this.input = input;
			this.filter = filter;
			setRule(new TreeWalkSchedulingRule(input.getTreeWalk()));
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				if (input.isFirstParentOnly()
						&& input.getCommit().getParentCount() > 1) {
					RevCommit[] parents = { input.getCommit().getParent(0) };
					diffs = FileDiff.compute(input.getRepository(),
							input.getTreeWalk(), input.getCommit(), parents,
							monitor, filter);
				} else {
					diffs = FileDiff.compute(input.getRepository(),
							input.getTreeWalk(), input.getCommit(), monitor,
							filter);
				}
			} catch (IOException err) {
				Activator.handleError(MessageFormat.format(
						UIText.CommitFileDiffViewer_errorGettingDifference,
						input.getCommit().getId()), err, false);
			}
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			return Status.OK_STATUS;
		}

		public FileDiff[] getDiffs() {
			return diffs;
		}

		public FileDiffInput getInput() {
			return input;
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == JobFamilies.HISTORY_FILE_DIFF
					|| super.belongsTo(family);
		}
	}

	private class UpdateJob extends UIJob {

		private final FileDiffLoader loadJob;

		public UpdateJob(String name, FileDiffLoader loadJob) {
			super(name);
			this.loadJob = loadJob;
			setUser(false); // always triggered by FileDiffLoader
			setSystem(true);
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			Control control = getControl();
			if (control == null || control.isDisposed() || loader != loadJob
					|| monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			FileDiff[] diffs = loadJob.getDiffs();
			try {
				control.setRedraw(false);
				setInput(diffs);
				FileDiff interesting = getFirstInterestingElement(diffs);
				if (interesting != null) {
					if (loadJob.getInput().isSelectMarked()) {
						setSelection(new StructuredSelection(interesting),
								true);
					}
					reveal(interesting);
				}
			} finally {
				control.setRedraw(true);
			}
			return Status.OK_STATUS;
		}

		private FileDiff getFirstInterestingElement(FileDiff[] diffs) {
			if (diffs != null) {
				for (FileDiff d : diffs) {
					if (d.isMarked(INTERESTING_MARK_TREE_FILTER_INDEX)) {
						return d;
					}
				}
			}
			return null;
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == JobFamilies.HISTORY_FILE_DIFF
					|| super.belongsTo(family);
		}
	}

	/**
	 * Serializes all load jobs using the same tree walk. Tree walks are not
	 * thread safe.
	 */
	private static class TreeWalkSchedulingRule implements ISchedulingRule {

		private final TreeWalk treeWalk;

		public TreeWalkSchedulingRule(TreeWalk treeWalk) {
			this.treeWalk = treeWalk;
		}

		@Override
		public boolean contains(ISchedulingRule rule) {
			if (rule instanceof TreeWalkSchedulingRule) {
				return Objects.equals(treeWalk,
						((TreeWalkSchedulingRule) rule).treeWalk);
			}
			return false;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return contains(rule);
		}

	}

	/**
	 * An action to check out selected {@link FileDiff}s from the commit.
	 */
	public static class CheckoutAction extends Action {

		private final Supplier<IStructuredSelection> selectionProvider;

		/**
		 * Creates a new {@link CheckoutAction}.
		 *
		 * @param selectionProvider
		 *            to get the selection from
		 */
		public CheckoutAction(
				Supplier<IStructuredSelection> selectionProvider) {
			super(UIText.CommitFileDiffViewer_CheckoutThisVersionMenuLabel);
			this.selectionProvider = selectionProvider;
		}

		@Override
		public void run() {
			DiscardChangesOperation operation = createOperation();
			if (operation == null) {
				return;
			}
			Map<Repository, Collection<String>> paths = operation
					.getPathsPerRepository();
			if (!CommandConfirmation.confirmCheckout(null, paths, true)) {
				return;
			}
			JobUtil.scheduleUserWorkspaceJob(operation,
					UIText.DiscardChangesAction_discardChanges,
					JobFamilies.DISCARD_CHANGES);
		}

		private DiscardChangesOperation createOperation() {
			Collection<FileDiff> diffs = getFileDiffs(selectionProvider.get());
			if (diffs.isEmpty()) {
				return null;
			}
			FileDiff first = diffs.iterator().next();
			Repository repository = first.getRepository();
			String revision = first.getCommit().getName();
			List<String> paths = diffs.stream().map(FileDiff::getNewPath)
					.collect(Collectors.toList());
			DiscardChangesOperation operation = new DiscardChangesOperation(
					repository, paths);
			operation.setRevision(revision);
			return operation;
		}

		private Collection<FileDiff> getFileDiffs(
				IStructuredSelection selection) {
			List<FileDiff> result = new ArrayList<>();
			for (Object obj : selection.toList()) {
				FileDiff diff = Adapters.adapt(obj, FileDiff.class);
				if (diff != null && diff.getChange() != ChangeType.DELETE
						&& !diff.isSubmodule()) {
					result.add(diff);
				}
			}
			return result;
		}
	}
}
