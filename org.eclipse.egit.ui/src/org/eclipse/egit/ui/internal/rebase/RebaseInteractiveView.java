/*******************************************************************************
 * Copyright (c) 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import java.util.HashMap;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commands.shared.AbortRebaseCommand;
import org.eclipse.egit.ui.internal.commands.shared.AbstractRebaseCommandHandler;
import org.eclipse.egit.ui.internal.commands.shared.AbstractRebaseCommandHandler.RebaseCommandFinishedListener;
import org.eclipse.egit.ui.internal.commands.shared.ContinueRebaseCommand;
import org.eclipse.egit.ui.internal.commands.shared.ProcessStepsRebaseCommand;
import org.eclipse.egit.ui.internal.commands.shared.RebaseInteractiveCommand;
import org.eclipse.egit.ui.internal.commands.shared.SkipRebaseCommand;
import org.eclipse.egit.ui.internal.rebase.RebaseInteractivePlan.PlanEntry;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.ViewPart;

//TODO: Preferences page
//TODO: Use the toolbar
//TODO: Update RebaseResultDialog to not show "confilics" when rebase interactive stopped with "edit"
//TODO: Tests
//TODO: Show steps that has been processed (done)
//TODO: Link Selection to History View?

/**
 *
 */
public class RebaseInteractiveView extends ViewPart implements
		RefsChangedListener {

	/**
	 * interactive rebase view id
	 */
	public static final String VIEW_ID = "org.eclipse.egit.ui.InteractiveRebaseView"; //$NON-NLS-1$

	private final String MESSAGE_DIALOG_TITLE = "Rebase interactive"; //$NON-NLS-1$

	private final/* static */HashMap<Repository, RebaseInteractiveInput> inputRegistry = new HashMap<Repository, RebaseInteractiveInput>();

	TreeViewer planTreeViewer;

	RebaseInteractiveInput input;

	private RebaseInteractiveStepActionToolBarProvider actionToolBarProvider;

	private ToolItem startItem;

	private ToolItem abortItem;

	private ToolItem skipItem;

	private ToolItem continueItem;

	private ToolItem refreshItem;

	private boolean listenOnRepositoryViewSelection = true;

	private ISelectionListener selectionChangedListener;

	private ListenerHandle myRefsChangedHandle;

	private RebaseCommandFinishedListener myRebaseCommandFinishedListener;

	private boolean dndEnabled = false;

	private Image abortImage;

	private Image skipImage;

	private Image continueImage;

	private Image startImage;

	private Image refreshImage;

	/**
	 *
	 */
	public RebaseInteractiveView() {
		setPartName(UIText.InteractiveRebaseView_this_partName);
	}

	/**
	 * @param o
	 */
	public void setInput(Object o) {
		Repository repo = null;
		if (o == null)
			return;
		if (o instanceof TreeSelection) {
			TreeSelection sel = (TreeSelection) o;
			if (sel.size() != 1)
				return;
			o = sel.getFirstElement();
		}
		if (o instanceof RepositoryTreeNode<?>) {
			repo = ((RepositoryTreeNode) o).getRepository();
		} else if (o instanceof Repository) {
			repo = (Repository) o;
		} else if (o instanceof IAdaptable) {
			IResource resource = (IResource) ((IAdaptable) o)
					.getAdapter(IResource.class);
			if (resource != null) {
				RepositoryMapping mapping = RepositoryMapping
						.getMapping(resource);
				repo = mapping.getRepository();
			}
		}

		if (repo == null) {
			repo = AdapterUtils.adapt(o, Repository.class);
		}

		if (repo != null
				&& repo.getRepositoryState() == RepositoryState.REBASING_INTERACTIVE) {
			showRepository(repo);
		}
	}

	@Override
	public void dispose() {
		removeListeners();
		disposeImages();
		super.dispose();
	}

	private void disposeImages() {
		startImage.dispose();
		abortImage.dispose();
		skipImage.dispose();
		continueImage.dispose();
		refreshImage.dispose();
	}

	private void removeListeners() {
		ISelectionService srv = (ISelectionService) getSite().getService(
				ISelectionService.class);
		srv.removePostSelectionListener(RepositoriesView.VIEW_ID,
				selectionChangedListener);
		myRefsChangedHandle.remove();
		AbstractRebaseCommandHandler
				.removeRebaseCommandFinishListener(myRebaseCommandFinishedListener);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);

		final FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		parent.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				toolkit.dispose();
			}
		});
		Form form = createForm(parent, toolkit);
		createCommandToolBar(form, toolkit);
		SashForm sashForm = createRebasePlanSashForm(form, toolkit);

		Section rebasePlanSection = toolkit.createSection(sashForm,
				ExpandableComposite.TITLE_BAR);
		planTreeViewer = createPlanTreeViewer(rebasePlanSection, toolkit);

		createColumns();
		createStepActionToolBar(rebasePlanSection, toolkit);
		createPopupMenu(planTreeViewer);

		setupListeners();
		createLocalDrackandDrop();
	}

	private void createCommandToolBar(Form form, FormToolkit toolkit) {
		ToolBar toolBar = new ToolBar(form.getHead(), SWT.FLAT);
		toolBar.setOrientation(SWT.RIGHT_TO_LEFT);
		form.setHeadClient(toolBar);

		toolkit.adapt(toolBar);
		toolkit.paintBordersFor(toolBar);

		abortItem = new ToolItem(toolBar, SWT.NONE);
		abortImage = UIIcons.REBASE_ABORT.createImage();
		abortItem.setImage(abortImage);
		abortItem.addSelectionListener(new RebaseCommandItemSelectionListener(
				new AbortRebaseCommand()));
		abortItem.setText(UIText.InteractiveRebaseView_abortItem_text);
		abortItem.setEnabled(false);

		skipItem = new ToolItem(toolBar, SWT.NONE);
		skipImage = UIIcons.REBASE_SKIP.createImage();
		skipItem.setImage(skipImage);
		skipItem.addSelectionListener(new RebaseCommandItemSelectionListener(
				new SkipRebaseCommand()));
		skipItem.setText(UIText.InteractiveRebaseView_skipItem_text);
		skipItem.setEnabled(false);

		continueItem = new ToolItem(toolBar, SWT.NONE);
		continueItem
				.addSelectionListener(new RebaseCommandItemSelectionListener(
						new ContinueRebaseCommand()));
		continueItem.setEnabled(false);
		continueImage = UIIcons.REBASE_CONTINUE.createImage();
		continueItem.setImage(continueImage);
		continueItem.setText(UIText.InteractiveRebaseView_continueItem_text);

		startItem = new ToolItem(toolBar, SWT.NONE);
		startItem.addSelectionListener(new RebaseCommandItemSelectionListener(
				new ProcessStepsRebaseCommand()) {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!input.hasRebaseBeenInitializedButNotStartedYet()) {
					MessageDialog
							.openError(
									getSite().getShell(),
									MESSAGE_DIALOG_TITLE,
									"Cannot Start\n It seems that rebase has already started processing steps"); //$NON-NLS-1$
				}
				super.widgetSelected(e);
				input.setRebaseHasBeenInitializedButNotStartedYet(false);
			}
		});
		startItem.setEnabled(false);
		startImage = UIIcons.REBASE_PROCESS_STEPS.createImage();
		startItem.setImage(startImage);
		startItem.setText(UIText.InteractiveRebaseView_startItem_text);

		new ToolItem(toolBar, SWT.SEPARATOR);

		refreshItem = new ToolItem(toolBar, SWT.NONE);
		refreshImage = UIIcons.ELCL16_REFRESH.createImage();
		refreshItem.setImage(refreshImage);
		refreshItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refresh();
			}
		});
		refreshItem.setText(UIText.InteractiveRebaseView_refreshItem_text);
	}

	private TreeViewer createPlanTreeViewer(Section rebasePlanSection,
			FormToolkit toolkit) {

		Composite rebasePlanTableComposite = toolkit
				.createComposite(rebasePlanSection);
		toolkit.paintBordersFor(rebasePlanTableComposite);
		rebasePlanSection.setClient(rebasePlanTableComposite);
		GridLayoutFactory.fillDefaults().extendedMargins(2, 2, 2, 2)
				.applyTo(rebasePlanTableComposite);

		Composite toolbarComposite = toolkit.createComposite(rebasePlanSection);
		toolbarComposite.setBackground(null);
		RowLayout toolbarRowLayout = new RowLayout();
		toolbarRowLayout.marginHeight = 0;
		toolbarRowLayout.marginWidth = 0;
		toolbarRowLayout.marginTop = 0;
		toolbarRowLayout.marginBottom = 0;
		toolbarRowLayout.marginLeft = 0;
		toolbarRowLayout.marginRight = 0;
		toolbarComposite.setLayout(toolbarRowLayout);

		GridLayoutFactory.fillDefaults().extendedMargins(2, 2, 2, 2)
				.applyTo(rebasePlanTableComposite);

		final Tree planTree = toolkit.createTree(rebasePlanTableComposite,
				SWT.FULL_SELECTION | SWT.MULTI);
		planTree.setHeaderVisible(true);
		planTree.setLinesVisible(false);

		TreeViewer tmpPlanViewer = new TreeViewer(planTree);
		tmpPlanViewer
				.addSelectionChangedListener(new PlanViewerSelectionChangedListener());
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(tmpPlanViewer.getControl());
		tmpPlanViewer.getTree().setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TREE_BORDER);
		tmpPlanViewer
				.setContentProvider(RebaseInteractivePlanContentProvider.INSTANCE);
		return tmpPlanViewer;
	}

	private SashForm createRebasePlanSashForm(final Form parent,
			final FormToolkit toolkit) {
		SashForm sashForm = new SashForm(parent.getBody(), SWT.NONE);
		toolkit.adapt(sashForm, true, true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(sashForm);
		return sashForm;
	}

	private Form createForm(Composite parent, final FormToolkit toolkit) {
		Form form = toolkit.createForm(parent);

		Image repoImage = UIIcons.REPOSITORY.createImage();
		UIUtils.hookDisposal(form, repoImage);
		form.setImage(repoImage);
		form.setText(UIText.InteractiveRebaseView_frmEgit_text);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);
		toolkit.decorateFormHeading(form);
		GridLayoutFactory.swtDefaults().applyTo(form.getBody());

		return form;
	}

	private void setupListeners() {
		setupRepositoryViewSelectionChangeListener();
		setupRepositoryRefChangedListener();
		setupRebaseCommandFinishedListener();
		refreshUI();
	}

	private void setupRepositoryViewSelectionChangeListener() {
		selectionChangedListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				if (!listenOnRepositoryViewSelection)
					return;
				if (!(part instanceof RepositoriesView))
					return;
				setInput(selection);
			}
		};
		ISelectionService srv = (ISelectionService) getSite().getService(
				ISelectionService.class);
		srv.addPostSelectionListener(RepositoriesView.VIEW_ID,
				selectionChangedListener);
	}

	private void setupRebaseCommandFinishedListener() {
		myRebaseCommandFinishedListener = new RebaseCommandFinishedListener() {

			public void operationFinished(IStatus result, Repository repository,
					Ref ref, Operation operation) {
				if (repository == null)
					return;
				if (input == null)
					return;
				if (repository.getDirectory() != input.getRepo().getDirectory())
					return;
				refresh();
			}
		};
		AbstractRebaseCommandHandler
				.addRebaseCommandFinishListener(myRebaseCommandFinishedListener);
	}

	private void setupRepositoryRefChangedListener() {
		myRefsChangedHandle = Repository.getGlobalListenerList()
				.addRefsChangedListener(this);
	}

	private class RebaseCommandItemSelectionListener extends
			SelectionAdapter {

		private final AbstractRebaseCommandHandler command;

		public RebaseCommandItemSelectionListener(
				AbstractRebaseCommandHandler command) {
			super();
			this.command = command;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			input.persist();
			callOperation(command, null, true);
		}
	}

	private class PlanViewerSelectionChangedListener implements
			ISelectionChangedListener {

		public void selectionChanged(SelectionChangedEvent event) {
			if (event == null)
				return;
			ISelection selection = event.getSelection();
			actionToolBarProvider.mapActionItemsToSelection(selection);
		}
	}

	private void createLocalDrackandDrop() {
		planTreeViewer.addDragSupport(
				DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new RebaseInteractiveDragSourceListener(this));

		planTreeViewer.addDropSupport(DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new RebaseInteractiveDropTargetListener(this, planTreeViewer));
	}

	private void createStepActionToolBar(Section rebasePlanSection,
			final FormToolkit toolkit) {

		actionToolBarProvider = new RebaseInteractiveStepActionToolBarProvider(
				rebasePlanSection, SWT.FLAT | SWT.WRAP, this);
		toolkit.adapt(actionToolBarProvider.getTheToolbar());
		toolkit.paintBordersFor(actionToolBarProvider.getTheToolbar());
		rebasePlanSection.setTextClient(actionToolBarProvider.getTheToolbar());
	}

	/**
	 * Undo previous change in plan
	 */
	protected void undo() {
		// TODO: undo
	}

	/**
	 * Redo previous change in plan reverted by undo
	 */
	protected void redo() {
		// TODO: redo
	}

	// TODO: How to set column width to fit the treeViewer (maximize to not
	// show empty space)
	private void createColumns() {
		String[] headings = { "Action", "CommitID", "Message" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		TreeViewerColumn actionColumn = new TreeViewerColumn(planTreeViewer,
				SWT.NONE);
		actionColumn.getColumn().setText(headings[0]);
		actionColumn.getColumn().setMoveable(false);
		actionColumn.getColumn().setResizable(true);
		actionColumn.getColumn().setWidth(100);

		actionColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PlanEntry) {
					PlanEntry pe = (PlanEntry) element;
					if (pe.getAction() == null) {
						return PlanEntry.DELETE_TOKEN;
					}
					return pe.getAction().toToken();
				}
				return null;
			}
		});

		TreeViewerColumn commitIDColumn = new TreeViewerColumn(planTreeViewer,
				SWT.NONE);
		commitIDColumn.getColumn().setText(headings[1]);
		commitIDColumn.getColumn().setMoveable(false);
		commitIDColumn.getColumn().setResizable(true);
		commitIDColumn.getColumn().setWidth(100);

		commitIDColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PlanEntry) {
					PlanEntry pe = (PlanEntry) element;
					return pe.getCommitID().name();
				}
				return super.getText(element);
			}
		});

		TreeViewerColumn commitMessageColumn = new TreeViewerColumn(
				planTreeViewer,
				SWT.NONE);
		commitMessageColumn.getColumn().setText(headings[2]);
		commitMessageColumn.getColumn().setMoveable(false);
		commitMessageColumn.getColumn().setResizable(true);
		commitMessageColumn.getColumn().setWidth(100);

		commitMessageColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PlanEntry) {
					PlanEntry pe = (PlanEntry) element;
					return pe.getShortMessage();
				}
				return super.getText(element);
			}
		});
	}

	private void asyncExec(Runnable runnable) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
	}

	private void showRepository(final Repository repo) {
		if (repo == null)
			return;
		getInput(repo);
		refresh();
	}

	private RebaseInteractiveInput getInput(Repository repo) {
		input = inputRegistry.get(repo);
		if (input == null) {
			input = new RebaseInteractiveInput(repo);
			inputRegistry.put(repo, input);
			input.reload();
		}
		return input;
	}

	void refresh() {
		if (!isReady())
			return;
		asyncExec(new Runnable() {
			public void run() {
				if (input != null) {
					input.reload();
					planTreeViewer.setInput(input.getPlan());
					planTreeViewer.refresh(true);
				} else {
					planTreeViewer.setInput(null);
				}
				refreshUI();
			}
		});

	}

	private boolean isReady() {
		IWorkbenchPartSite site = this.getSite();
		if (site == null)
			return false;
		return !site.getShell().isDisposed();
	}

	private void refreshUI() {
		if (planTreeViewer != null)
			planTreeViewer.refresh(true);

		startItem.setEnabled(false);
		continueItem.setEnabled(false);
		skipItem.setEnabled(false);
		abortItem.setEnabled(false);
		dndEnabled = false;

		actionToolBarProvider.getTheToolbar().setEnabled(false);

		if (input == null || !input.checkState()) {
			return;
		}

		actionToolBarProvider.mapActionItemsToSelection(planTreeViewer.getSelection());
		if (input.hasRebaseBeenInitializedButNotStartedYet()) {
			actionToolBarProvider.getTheToolbar().setEnabled(true);
			startItem.setEnabled(true);
			abortItem.setEnabled(true);
			dndEnabled  = true;
		} else {
			continueItem.setEnabled(true);
			skipItem.setEnabled(true);
			abortItem.setEnabled(true);
		}
	}

	/**
	 * Start a rebase interactive and show it in this View
	 *
	 * @param repo
	 * @param ref
	 */
	public void startRebaseInteractiveAndOpen(final Repository repo, Ref ref) {
		switch (repo.getRepositoryState()) {
		case REBASING_INTERACTIVE:
			MessageDialog
					.openError(
							getSite().getShell(),
							MESSAGE_DIALOG_TITLE,
							"Cannot reinitialize\n Rebase has already been started \n Instead the current rebase will be shown"); //$NON-NLS-1$
			showRepository(repo);
			break;
		case SAFE:
			getInput(repo);
			callOperation(new RebaseInteractiveCommand(), ref, false);
			input.setRebaseHasBeenInitializedButNotStartedYet(true);
			showRepository(repo);
			break;
		default:
			break;
		}
	}

	private void createPopupMenu(TreeViewer planViewer) {
		// TODO Popup menu
	}

	boolean checkState() {
		if (input == null)
			return false;
		if (!input.checkState()) {
			MessageDialog
					.openError(getSite().getShell(),
							MESSAGE_DIALOG_TITLE,
							"Not in rebase state.\nRebase interactive may have been finished by another Git instance"); //$NON-NLS-1$
			return false;
		}
		return true;
	}

	private void callOperation(AbstractRebaseCommandHandler commandHandler,
			Ref ref, boolean check) {
		if (check && !this.checkState())
			return;
		try {
			commandHandler.execute(input.getRepo(), ref);
		} catch (ExecutionException e) {
			Activator.error(e.getMessage(), e);
		}
	}

	public void onRefsChanged(RefsChangedEvent event) {
		if (input == null)
			return;
		if (event.getRepository() != input.getRepo())
			return;
		refresh();
	}

	@Override
	public void setFocus() {
		// do nothing here?
	}

	boolean isDragAndDropEnabled() {
		return dndEnabled;
	}
}