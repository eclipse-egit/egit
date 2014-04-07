/*******************************************************************************
 * Copyright (c) 2013, 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.ElementAction;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.ElementType;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.PlanElement;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commands.shared.AbortRebaseCommand;
import org.eclipse.egit.ui.internal.commands.shared.AbstractRebaseCommandHandler;
import org.eclipse.egit.ui.internal.commands.shared.ContinueRebaseCommand;
import org.eclipse.egit.ui.internal.commands.shared.ProcessStepsRebaseCommand;
import org.eclipse.egit.ui.internal.commands.shared.SkipRebaseCommand;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.util.GitDateFormatter;
import org.eclipse.jgit.util.GitDateFormatter.Format;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
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

/**
 * View visualizing git interactive rebase
 */
public class RebaseInteractiveView extends ViewPart implements
		RebaseInteractivePlan.RebaseInteractivePlanChangeListener {

	/**
	 * interactive rebase view id
	 */
	public static final String VIEW_ID = "org.eclipse.egit.ui.InteractiveRebaseView"; //$NON-NLS-1$

	TreeViewer planTreeViewer;

	private RebaseInteractivePlan currentPlan;

	Repository currentRepository;

	private RebaseInteractiveStepActionToolBarProvider actionToolBarProvider;

	private ToolItem startItem;

	private ToolItem abortItem;

	private ToolItem skipItem;

	private ToolItem continueItem;

	private ToolItem refreshItem;

	private boolean listenOnRepositoryViewSelection = true;

	private ISelectionListener selectionChangedListener;

	private boolean dndEnabled = false;

	private Form form;

	private LocalResourceManager resources = new LocalResourceManager(
			JFaceResources.getResources());

	private GitDateFormatter dateFormatter = getNewDateFormatter();

	/** these columns are dynamically resized to fit their contents */
	private TreeViewerColumn[] dynamicColumns;

	/**
	 * View for handling interactive rebase
	 */
	public RebaseInteractiveView() {
		setPartName(UIText.InteractiveRebaseView_this_partName);
	}

	/**
	 * Set the view input if the passed object can be used to determine the
	 * current repository
	 *
	 * @param o
	 */
	public void setInput(Object o) {
		if (o == null)
			return;

		if (o instanceof StructuredSelection) {
			StructuredSelection sel = (StructuredSelection) o;
			if (sel.size() != 1)
				return;
			o = sel.getFirstElement();
		}
		Repository repo = null;
		if (o instanceof RepositoryTreeNode<?>)
			repo = ((RepositoryTreeNode) o).getRepository();
		else if (o instanceof Repository)
			repo = (Repository) o;
		else if (o instanceof IAdaptable) {
			IResource resource = (IResource) ((IAdaptable) o)
					.getAdapter(IResource.class);
			if (resource != null) {
				RepositoryMapping mapping = RepositoryMapping
						.getMapping(resource);
				repo = mapping.getRepository();
			}
		}
		if (repo == null)
			repo = AdapterUtils.adapt(o, Repository.class);

		currentRepository = repo;
		showRepository(repo);
	}

	/**
	 * @return {@link RebaseInteractiveView#currentPlan}
	 */
	public RebaseInteractivePlan getCurrentPlan() {
		return currentPlan;
	}

	@Override
	public void dispose() {
		removeListeners();
		resources.dispose();
		super.dispose();
	}

	private void removeListeners() {
		ISelectionService srv = (ISelectionService) getSite().getService(
				ISelectionService.class);
		srv.removePostSelectionListener(RepositoriesView.VIEW_ID,
				selectionChangedListener);
		if (currentPlan != null)
			currentPlan.removeRebaseInteractivePlanChangeListener(this);
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);
		final FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		parent.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				toolkit.dispose();
			}
		});
		form = createForm(parent, toolkit);
		createCommandToolBar(form, toolkit);
		SashForm sashForm = createRebasePlanSashForm(form, toolkit);

		Section rebasePlanSection = toolkit.createSection(sashForm,
				ExpandableComposite.TITLE_BAR);
		planTreeViewer = createPlanTreeViewer(rebasePlanSection, toolkit);

		createColumns();
		createStepActionToolBar(rebasePlanSection, toolkit);
		createPopupMenu(planTreeViewer);

		setupListeners();
		createLocalDragandDrop();
		planTreeViewer
				.addDoubleClickListener(new RebaseInteractiveDoubleClickListener(
						this));
	}

	private void createCommandToolBar(Form theForm, FormToolkit toolkit) {
		ToolBar toolBar = new ToolBar(theForm.getHead(), SWT.FLAT);
		toolBar.setOrientation(SWT.RIGHT_TO_LEFT);
		theForm.setHeadClient(toolBar);

		toolkit.adapt(toolBar);
		toolkit.paintBordersFor(toolBar);

		startItem = new ToolItem(toolBar, SWT.NONE);
		startItem.setImage(UIIcons.getImage(resources,
				UIIcons.REBASE_PROCESS_STEPS));
		startItem.addSelectionListener(new RebaseCommandItemSelectionListener(
				new ProcessStepsRebaseCommand()));
		startItem.setEnabled(false);
		startItem.setText(UIText.InteractiveRebaseView_startItem_text);

		continueItem = new ToolItem(toolBar, SWT.NONE);
		continueItem.setImage(UIIcons.getImage(resources,
				UIIcons.REBASE_CONTINUE));
		continueItem
				.addSelectionListener(new RebaseCommandItemSelectionListener(
						new ContinueRebaseCommand()));
		continueItem.setEnabled(false);
		continueItem.setText(UIText.InteractiveRebaseView_continueItem_text);

		skipItem = new ToolItem(toolBar, SWT.NONE);
		skipItem.setImage(UIIcons.getImage(resources, UIIcons.REBASE_SKIP));
		skipItem.addSelectionListener(new RebaseCommandItemSelectionListener(
				new SkipRebaseCommand()));
		skipItem.setText(UIText.InteractiveRebaseView_skipItem_text);
		skipItem.setEnabled(false);

		abortItem = new ToolItem(toolBar, SWT.NONE);
		abortItem.setImage(UIIcons.getImage(resources, UIIcons.REBASE_ABORT));
		abortItem.addSelectionListener(new RebaseCommandItemSelectionListener(
				new AbortRebaseCommand()));
		abortItem.setText(UIText.InteractiveRebaseView_abortItem_text);
		abortItem.setEnabled(false);

		new ToolItem(toolBar, SWT.SEPARATOR);

		refreshItem = new ToolItem(toolBar, SWT.NONE);
		refreshItem.setImage(UIIcons
				.getImage(resources, UIIcons.ELCL16_REFRESH));
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

		TreeViewer viewer = new TreeViewer(planTree);
		viewer.addSelectionChangedListener(new PlanViewerSelectionChangedListener());
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(viewer.getControl());
		viewer.getTree().setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TREE_BORDER);
		viewer.setContentProvider(RebaseInteractivePlanContentProvider.INSTANCE);
		return viewer;
	}

	private SashForm createRebasePlanSashForm(final Form parent,
			final FormToolkit toolkit) {
		SashForm sashForm = new SashForm(parent.getBody(), SWT.NONE);
		toolkit.adapt(sashForm, true, true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(sashForm);
		return sashForm;
	}

	private Form createForm(Composite parent, final FormToolkit toolkit) {
		Form newForm = toolkit.createForm(parent);

		Image repoImage = UIIcons.REPOSITORY.createImage();
		UIUtils.hookDisposal(newForm, repoImage);
		newForm.setImage(repoImage);
		newForm.setText(UIText.RebaseInteractiveView_NoSelection);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(newForm);
		toolkit.decorateFormHeading(newForm);
		GridLayoutFactory.swtDefaults().applyTo(newForm.getBody());

		return newForm;
	}

	private void setupListeners() {
		setupRepositoryViewSelectionChangeListener();
		refreshUI();
	}

	private void setupRepositoryViewSelectionChangeListener() {
		selectionChangedListener = new ISelectionListener() {

			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				if (!listenOnRepositoryViewSelection
						|| part == getSite().getPart())
					return;

				// this may happen if we switch between editors
				if (part instanceof IEditorPart) {
					IEditorInput input = ((IEditorPart) part).getEditorInput();
					if (input instanceof IFileEditorInput)
						setInput(new StructuredSelection(
								((IFileEditorInput) input).getFile()));
				} else
					setInput(selection);
			}
		};

		ISelectionService srv = (ISelectionService) getSite().getService(
				ISelectionService.class);
		srv.addPostSelectionListener(RepositoriesView.VIEW_ID,
				selectionChangedListener);
	}

	private class RebaseCommandItemSelectionListener extends SelectionAdapter {

		private final AbstractRebaseCommandHandler command;

		public RebaseCommandItemSelectionListener(
				AbstractRebaseCommandHandler command) {
			super();
			this.command = command;
		}

		@Override
		public void widgetSelected(SelectionEvent sEvent) {
			try {
				command.execute(currentPlan.getRepository());
			} catch (ExecutionException e) {
				Activator.showError(e.getMessage(), e);
			}
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

	private void createLocalDragandDrop() {
		planTreeViewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY
				| DND.DROP_LINK,
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

	private static RebaseInteractivePlan.ElementType getType(Object element) {
		if (element instanceof PlanElement) {
			PlanElement planLine = (PlanElement) element;
			return planLine.getElementType();
		} else
			return null;
	}

	private static class HighlightingColumnLabelProvider extends
			ColumnLabelProvider {

		@Override
		public Font getFont(Object element) {
			ElementType t = RebaseInteractiveView.getType(element);
			if (t != null && t == ElementType.DONE_CURRENT)
				return UIUtils.getBoldFont(JFaceResources.DIALOG_FONT);
			return super.getFont(element);
		}
	}

	// TODO: How to set column width to fit the treeViewer (maximize to not
	// show empty space)
	private void createColumns() {
		String[] headings = { UIText.RebaseInteractiveView_HeadingStatus,
				UIText.RebaseInteractiveView_HeadingAction,
				UIText.RebaseInteractiveView_HeadingCommitId,
				UIText.RebaseInteractiveView_HeadingMessage,
				UIText.RebaseInteractiveView_HeadingAuthor,
				UIText.RebaseInteractiveView_HeadingAuthorDate,
				UIText.RebaseInteractiveView_HeadingCommitter,
				UIText.RebaseInteractiveView_HeadingCommitDate };

		ColumnViewerToolTipSupport.enableFor(planTreeViewer,
				ToolTip.NO_RECREATE);

		TreeViewerColumn infoColumn = createColumn(headings[0], 70);
		infoColumn.setLabelProvider(new HighlightingColumnLabelProvider() {

			@Override
			public Image getImage(Object element) {
				ElementType t = getType(element);
				if (t != null) {
					switch (t) {
					case TODO:
						return UIIcons.getImage(resources, UIIcons.TODO_STEP);
					case DONE_CURRENT:
						return UIIcons
								.getImage(resources, UIIcons.CURRENT_STEP);
					case DONE:
						return UIIcons.getImage(resources, UIIcons.DONE_STEP);
					default:
						// fall through
					}
				}
				return null;
			}

			@Override
			public String getToolTipText(Object element) {
				ElementType t = getType(element);
				if (t != null) {
					switch (t) {
					case DONE:
						return UIText.RebaseInteractiveView_StatusDone;
					case DONE_CURRENT:
						return UIText.RebaseInteractiveView_StatusCurrent;
					case TODO:
						return UIText.RebaseInteractiveView_StatusTodo;
					default:
						// fall through
					}
				}
				return ""; //$NON-NLS-1$
			}

			@Override
			public String getText(Object element) {
				return ""; //$NON-NLS-1$
			}
		});

		TreeViewerColumn actionColumn = createColumn(headings[1], 90);
		actionColumn.setLabelProvider(new HighlightingColumnLabelProvider() {

			@Override
			public Image getImage(Object element) {
				ElementAction a = getAction(element);
				if (a != null) {
					switch (a) {
					case EDIT:
						return UIIcons.getImage(resources, UIIcons.EDITCONFIG);
					case FIXUP:
						return UIIcons.getImage(resources, UIIcons.FIXUP);
					case PICK:
						return UIIcons.getImage(resources, UIIcons.CHERRY_PICK);
					case REWORD:
						return UIIcons.getImage(resources, UIIcons.REWORD);
					case SKIP:
						return UIIcons.getImage(resources, UIIcons.REBASE_SKIP);
					case SQUASH:
						return UIIcons.getImage(resources, UIIcons.SQUASH);
					default:
						// fall through
					}
				}
				return super.getImage(element);
			}

			@Override
			public String getText(Object element) {
				ElementAction a = getAction(element);
				return (a != null) ? a.name() : super.getText(element);
			}

			private ElementAction getAction(Object element) {
				if (element instanceof PlanElement) {
					PlanElement planLine = (PlanElement) element;
					return planLine.getPlanElementAction();
				} else
					return null;
			}
		});

		TreeViewerColumn commitIDColumn = createColumn(headings[2], 70);
		commitIDColumn.setLabelProvider(new HighlightingColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PlanElement) {
					PlanElement planLine = (PlanElement) element;
					return planLine.getCommit().name();
				}
				return super.getText(element);
			}
		});

		TreeViewerColumn commitMessageColumn = createColumn(headings[3], 200);
		commitMessageColumn
				.setLabelProvider(new HighlightingColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof PlanElement) {
							PlanElement planLine = (PlanElement) element;
							return planLine.getShortMessage();
						}
						return super.getText(element);
					}
				});

		TreeViewerColumn authorColumn = createColumn(headings[4], 120);
		authorColumn.setLabelProvider(new HighlightingColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PlanElement) {
					PlanElement planLine = (PlanElement) element;
					return planLine.getAuthor();
				}
				return super.getText(element);
			}
		});

		TreeViewerColumn authoredDateColumn = createColumn(headings[5], 80);
		authoredDateColumn
				.setLabelProvider(new HighlightingColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof PlanElement) {
							PlanElement planLine = (PlanElement) element;
							return planLine.getAuthoredDate(dateFormatter);
						}
						return super.getText(element);
					}
				});

		TreeViewerColumn committerColumn = createColumn(headings[6], 120);
		committerColumn.setLabelProvider(new HighlightingColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PlanElement) {
					PlanElement planLine = (PlanElement) element;
					return planLine.getCommitter();
				}
				return super.getText(element);
			}
		});

		TreeViewerColumn commitDateColumn = createColumn(headings[7], 80);
		commitDateColumn
				.setLabelProvider(new HighlightingColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof PlanElement) {
							PlanElement planLine = (PlanElement) element;
							return planLine.getCommittedDate(dateFormatter);
						}
						return super.getText(element);
					}
				});
		dynamicColumns = new TreeViewerColumn[] { commitMessageColumn,
				authorColumn, authoredDateColumn, committerColumn,
				commitDateColumn };
	}

	private TreeViewerColumn createColumn(String text, int width) {
		TreeViewerColumn column = new TreeViewerColumn(planTreeViewer, SWT.NONE);
		column.getColumn().setText(text);
		column.getColumn().setMoveable(false);
		column.getColumn().setResizable(true);
		column.getColumn().setWidth(width);
		return column;
	}

	private void asyncExec(Runnable runnable) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
	}

	private static String getRepositoryName(Repository repository) {
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		RepositoryState state = repository.getRepositoryState();
		if (state != RepositoryState.SAFE)
			return repoName + '|' + state.getDescription();
		else
			return repoName;
	}

	private void showRepository(final Repository repository) {
		if (form.isDisposed())
			return;
		if (repository == null) {
			form.setText(UIText.RebaseInteractiveView_NoSelection);
			return;
		}

		if (currentPlan != null)
			currentPlan.removeRebaseInteractivePlanChangeListener(this);
		currentPlan = RebaseInteractivePlan.getPlan(repository);
		currentPlan.addRebaseInteractivePlanChangeListener(this);
		form.setText(getRepositoryName(repository));
		refresh();
	}

	void refresh() {
		if (!isReady())
			return;
		asyncExec(new Runnable() {
			public void run() {
				planTreeViewer.setInput(currentPlan);
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
		dateFormatter = getNewDateFormatter();
		if (planTreeViewer != null) {
			planTreeViewer.refresh(true);
			// make column widths match the contents
			for (TreeViewerColumn col : dynamicColumns)
				col.getColumn().pack();
		}

		startItem.setEnabled(false);
		continueItem.setEnabled(false);
		skipItem.setEnabled(false);
		abortItem.setEnabled(false);
		dndEnabled = false;

		actionToolBarProvider.getTheToolbar().setEnabled(false);

		if (currentPlan == null || !currentPlan.isRebasingInteractive()) {
			if (currentRepository == null)
				form.setText(UIText.RebaseInteractiveView_NoSelection);
			else
				form.setText(getRepositoryName(currentRepository));
			return;
		}

		actionToolBarProvider.mapActionItemsToSelection(planTreeViewer
				.getSelection());
		if (!currentPlan.hasRebaseBeenStartedYet()) {
			actionToolBarProvider.getTheToolbar().setEnabled(true);
			startItem.setEnabled(true);
			abortItem.setEnabled(true);
			dndEnabled = true;
		} else {
			continueItem.setEnabled(true);
			skipItem.setEnabled(true);
			abortItem.setEnabled(true);
		}
	}

	private void createPopupMenu(TreeViewer planViewer) {
		// TODO create popup menu
	}

	private static GitDateFormatter getNewDateFormatter() {
		boolean useRelativeDates = Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_RELATIVE_DATE);
		if (useRelativeDates)
			return new GitDateFormatter(Format.RELATIVE);
		else
			return new GitDateFormatter(Format.LOCALE);
	}

	@Override
	public void setFocus() {
		planTreeViewer.getControl().setFocus();
	}

	boolean isDragAndDropEnabled() {
		return dndEnabled;
	}

	public void planWasUpdatedFromRepository(final RebaseInteractivePlan plan) {
		refresh();
	}

	public void planElementTypeChanged(
			RebaseInteractivePlan rebaseInteractivePlan, PlanElement element,
			ElementAction oldType, ElementAction newType) {
		planTreeViewer.refresh(element, true);
	}

	public void planElementsOrderChanged(
			RebaseInteractivePlan rebaseInteractivePlan, PlanElement element,
			int oldIndex, int newIndex) {
		planTreeViewer.refresh(true);
	}
}