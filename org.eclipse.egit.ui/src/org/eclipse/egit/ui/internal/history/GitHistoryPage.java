/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (c) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.ResourceList;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.history.command.HistoryViewCommands;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

/** Graphical commit history viewer. */
public class GitHistoryPage extends HistoryPage implements RefsChangedListener {

	private static final String POPUP_ID = "org.eclipse.egit.ui.historyPageContributions"; //$NON-NLS-1$

	/** Standard action: select all */
	private IAction selectAllAction;

	/** Standard action: copy to clip board */
	private IAction copyAction;

	/** Show Comment */
	private IAction showCommentAction;

	/** Show File */
	private IAction showFilesAction;

	/** Wrap comment */
	private IAction wrapCommentAction;

	/** Fill comment */
	private IAction fillCommentAction;

	/** Compare mode toggle */
	private IAction compareModeAction;

	private boolean compareMode = false;

	// we need to keep track of these actions so that we can
	// dispose them when the page is disposed (the history framework
	// does not do this for us)
	private final List<BooleanPrefAction> actionsToDispose = new ArrayList<BooleanPrefAction>();

	private final IPersistentPreferenceStore store = (IPersistentPreferenceStore) Activator
			.getDefault().getPreferenceStore();

	private ListenerHandle myRefsChangedHandle;

	/**
	 * Determine if the input can be shown in this viewer.
	 *
	 * @param object
	 *            an object that is hopefully of type ResourceList or IResource,
	 *            but may be anything (including null).
	 * @return true if the input is a ResourceList or an IResource of type FILE,
	 *         FOLDER or PROJECT and we can show it; false otherwise.
	 */
	public static boolean canShowHistoryFor(final Object object) {
		if (object instanceof ResourceList) {
			final IResource[] array = ((ResourceList) object).getItems();
			if (array.length == 0)
				return false;
			for (final IResource r : array) {
				if (!typeOk(r))
					return false;
			}
			return true;

		}

		if (object instanceof IAdaptable) {
			IResource resource = (IResource) ((IAdaptable) object)
					.getAdapter(IResource.class);
			return resource == null ? false : typeOk(resource);
		}

		if (object instanceof IResource) {
			return typeOk((IResource) object);
		}

		return false;
	}

	private static boolean typeOk(final IResource object) {
		switch (object.getType()) {
		case IResource.FILE:
		case IResource.FOLDER:
		case IResource.PROJECT:
			return true;
		}
		return false;
	}

	/** Overall composite hosting all of our controls. */
	private Composite ourControl;

	/** Split between {@link #graph} and {@link #revInfoSplit}. */
	private SashForm graphDetailSplit;

	/** Split between {@link #commentViewer} and {@link #fileViewer}. */
	private SashForm revInfoSplit;

	/** The table showing the DAG, first "paragraph", author, author date. */
	private CommitGraphTable graph;

	/** Viewer displaying the currently selected commit of {@link #graph}. */
	private CommitMessageViewer commentViewer;

	/** Viewer displaying file difference implied by {@link #graph}'s commit. */
	private CommitFileDiffViewer fileViewer;

	/** Toolbar to find commits in the history view. */
	private FindToolbar findToolbar;

	/** Our context menu manager for the entire page. */
	private MenuManager popupMgr;

	/** Job that is updating our history view, if we are refreshing. */
	private GenerateHistoryJob job;

	/** Revision walker that allocated our graph's commit nodes. */
	private SWTWalk currentWalk;

	/** Last HEAD */
	private AnyObjectId currentHeadId;

	/** We need to remember the current repository */
	private Repository db;

	/**
	 * Highlight flag that can be applied to commits to make them stand out.
	 * <p>
	 * Allocated at the same time as {@link #currentWalk}. If the walk rebuilds,
	 * so must this flag.
	 */
	private RevFlag highlightFlag;

	/**
	 * List of paths we used to limit {@link #currentWalk}; null if no paths.
	 * <p>
	 * Note that a change in this list requires that {@link #currentWalk} and
	 * all of its associated commits.
	 */
	private List<String> pathFilters;

	private static final String PREF_SHOWALLFILTER = "org.eclipse.egit.ui.githistorypage.showallfilter"; //$NON-NLS-1$

	enum ShowFilter {
		SHOWALLRESOURCE, SHOWALLFOLDER, SHOWALLPROJECT, SHOWALLREPO,
	}

	class ShowFilterAction extends Action {
		private final ShowFilter filter;

		ShowFilterAction(ShowFilter filter, ImageDescriptor icon,
				String toolTipText) {
			super(null, IAction.AS_CHECK_BOX);
			this.filter = filter;
			setImageDescriptor(icon);
			setText(toolTipText);
			setToolTipText(toolTipText);
		}

		@Override
		public void run() {
			String oldName = getName();
			if (!isChecked()) {
				if (showAllFilter == filter) {
					showAllFilter = ShowFilter.SHOWALLRESOURCE;
					showAllResourceVersionsAction.setChecked(true);
					refresh();
				}
			}
			if (isChecked() && showAllFilter != filter) {
				showAllFilter = filter;
				if (this != showAllRepoVersionsAction)
					showAllRepoVersionsAction.setChecked(false);
				if (this != showAllProjectVersionsAction)
					showAllProjectVersionsAction.setChecked(false);
				if (this != showAllFolderVersionsAction)
					showAllFolderVersionsAction.setChecked(false);
				if (this != showAllResourceVersionsAction)
					showAllResourceVersionsAction.setChecked(false);
				refresh();
			}
			GitHistoryPage.this.firePropertyChange(GitHistoryPage.this, P_NAME,
					oldName, getName());
			Activator.getDefault().getPreferenceStore().setValue(
					PREF_SHOWALLFILTER, showAllFilter.toString());
		}

		@Override
		public String toString() {
			return "ShowFilter[" + filter.toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private ShowFilter showAllFilter = ShowFilter.SHOWALLRESOURCE;

	private ShowFilterAction showAllRepoVersionsAction;

	private ShowFilterAction showAllProjectVersionsAction;

	private ShowFilterAction showAllFolderVersionsAction;

	private ShowFilterAction showAllResourceVersionsAction;

	private void createResourceFilterActions() {
		try {
			showAllFilter = ShowFilter.valueOf(Activator.getDefault()
					.getPreferenceStore().getString(PREF_SHOWALLFILTER));
		} catch (IllegalArgumentException e) {
			showAllFilter = ShowFilter.SHOWALLRESOURCE;
		}

		showAllRepoVersionsAction = new ShowFilterAction(
				ShowFilter.SHOWALLREPO, UIIcons.FILTERREPO,
				UIText.HistoryPage_ShowAllVersionsForRepo);

		showAllProjectVersionsAction = new ShowFilterAction(
				ShowFilter.SHOWALLPROJECT, UIIcons.FILTERPROJECT,
				UIText.HistoryPage_ShowAllVersionsForProject);

		showAllFolderVersionsAction = new ShowFilterAction(
				ShowFilter.SHOWALLFOLDER, UIIcons.FILTERFOLDER,
				UIText.HistoryPage_ShowAllVersionsForFolder);

		showAllResourceVersionsAction = new ShowFilterAction(
				ShowFilter.SHOWALLRESOURCE, UIIcons.FILTERRESOURCE,
				UIText.GitHistoryPage_ShowAllVersionsForResource);

		showAllRepoVersionsAction
				.setChecked(showAllFilter == showAllRepoVersionsAction.filter);
		showAllProjectVersionsAction
				.setChecked(showAllFilter == showAllProjectVersionsAction.filter);
		showAllFolderVersionsAction
				.setChecked(showAllFilter == showAllFolderVersionsAction.filter);
		showAllResourceVersionsAction
				.setChecked(showAllFilter == showAllResourceVersionsAction.filter);

		IToolBarManager mgr = getSite().getActionBars().getToolBarManager();
		mgr.add(new Separator());
		mgr.add(showAllRepoVersionsAction);
		mgr.add(showAllProjectVersionsAction);
		mgr.add(showAllFolderVersionsAction);
		mgr.add(showAllResourceVersionsAction);

		IMenuManager viewMenuMgr = getSite().getActionBars().getMenuManager();
		viewMenuMgr.add(new Separator());
		viewMenuMgr.add(showAllRepoVersionsAction);
		viewMenuMgr.add(showAllProjectVersionsAction);
		viewMenuMgr.add(showAllFolderVersionsAction);
		viewMenuMgr.add(showAllResourceVersionsAction);
	}

	private void createCompareModeAction() {
		final IToolBarManager barManager = getSite().getActionBars()
				.getToolBarManager();
		compareModeAction = new Action(UIText.GitHistoryPage_compareMode,
				IAction.AS_CHECK_BOX) {
			public void run() {
				compareMode = !compareMode;
				setChecked(compareMode);
				fileViewer.setCompareMode(compareMode);
			}
		};
		compareModeAction.setImageDescriptor(UIIcons.ELCL16_COMPARE_VIEW);
		compareModeAction.setChecked(compareMode);
		compareModeAction.setToolTipText(UIText.GitHistoryPage_compareMode);
		fileViewer.setCompareMode(compareMode);
		barManager.add(new Separator());
		barManager.add(compareModeAction);
	}

	/**
	 * @param compareMode
	 *            switch compare mode button of the view on / off
	 */
	public void setCompareMode(boolean compareMode) {
		if (compareModeAction != null) {
			this.compareMode = compareMode;
			compareModeAction.setChecked(compareMode);
		}
	}

	@Override
	public void createControl(final Composite parent) {
		GridData gd;

		ourControl = createMainPanel(parent);
		gd = new GridData();
		gd.verticalAlignment = SWT.FILL;
		gd.horizontalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		ourControl.setLayoutData(gd);

		gd = new GridData();
		gd.verticalAlignment = SWT.FILL;
		gd.horizontalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		graphDetailSplit = new SashForm(ourControl, SWT.VERTICAL);
		graphDetailSplit.setLayoutData(gd);

		graph = new CommitGraphTable(graphDetailSplit);
		graph.getTableView().addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				final Object input = getInput();
				// if multiple resources (IResourceList) or something not a file
				// is selected we do nothing
				if (!(input instanceof IFile)) {
					return;
				}

				ICommandService srv = (ICommandService) getSite().getService(
						ICommandService.class);
				IHandlerService hsrv = (IHandlerService) getSite().getService(
						IHandlerService.class);
				Command cmd = srv.getCommand(HistoryViewCommands.SHOWVERSIONS);
				Parameterization[] parms;
				if (compareMode) {
					try {
						IParameter parm = cmd
								.getParameter(HistoryViewCommands.COMPARE_MODE_PARAM);
						parms = new Parameterization[] { new Parameterization(
								parm, Boolean.TRUE.toString()) };
					} catch (NotDefinedException e) {
						Activator.handleError(e.getMessage(), e, true);
						parms = null;
					}
				} else
					parms = null;
				ParameterizedCommand pcmd = new ParameterizedCommand(cmd, parms);
				try {
					hsrv.executeCommandInContext(pcmd, null, hsrv
							.getCurrentState());
				} catch (Exception e) {
					Activator.handleError(e.getMessage(), e, true);
				}
			}
		});
		revInfoSplit = new SashForm(graphDetailSplit, SWT.HORIZONTAL);
		commentViewer = new CommitMessageViewer(revInfoSplit);
		fileViewer = new CommitFileDiffViewer(revInfoSplit);
		findToolbar = new FindToolbar(ourControl);

		layoutSashForm(graphDetailSplit,
				UIPreferences.RESOURCEHISTORY_GRAPH_SPLIT);
		layoutSashForm(revInfoSplit, UIPreferences.RESOURCEHISTORY_REV_SPLIT);

		popupMgr = new MenuManager(null, POPUP_ID);
		attachCommitSelectionChanged();
		createLocalToolbarActions();
		createResourceFilterActions();
		createCompareModeAction();
		createStandardActions();

		getSite().registerContextMenu(POPUP_ID, popupMgr, graph.getTableView());
		// due to the issues described in bug 322751, it makes no
		// sense to set a selection provider for the site here

		attachContextMenu(graph.getControl());
		attachContextMenu(commentViewer.getControl());
		attachContextMenu(fileViewer.getControl());
		layout();

		if (myRefsChangedHandle == null)
			myRefsChangedHandle = Repository.getGlobalListenerList()
					.addRefsChangedListener(this);
	}

	/**
	 * @return the selection provider
	 */
	public ISelectionProvider getSelectionProvider() {
		return graph.getTableView();
	}

	private Runnable refschangedRunnable;

	public void onRefsChanged(final RefsChangedEvent e) {
		if (e.getRepository() != db)
			return;

		if (getControl().isDisposed())
			return;

		synchronized (this) {
			if (refschangedRunnable == null) {
				refschangedRunnable = new Runnable() {
					public void run() {
						if (!getControl().isDisposed()) {
							// TODO is this the right location?
							if (GitTraceLocation.UI.isActive())
								GitTraceLocation
										.getTrace()
										.trace(
												GitTraceLocation.UI
														.getLocation(),
												"Executing async repository changed event"); //$NON-NLS-1$
							refschangedRunnable = null;
							inputSet();
						}
					}
				};
				getControl().getDisplay().asyncExec(refschangedRunnable);
			}
		}
	}

	private CommandContributionItem getCommandContributionItem(
			String commandId, String menuLabel) {
		CommandContributionItemParameter parameter = new CommandContributionItemParameter(
				getSite(), commandId, commandId,
				CommandContributionItem.STYLE_PUSH);
		parameter.label = menuLabel;
		return new CommandContributionItem(parameter);
	}

	private CommandContributionItem getCommandContributionItem(
			String commandId, String menuLabel, Map<String, String> parameters) {
		CommandContributionItemParameter parameter = new CommandContributionItemParameter(
				getSite(), commandId, commandId,
				CommandContributionItem.STYLE_PUSH);
		parameter.label = menuLabel;
		parameter.parameters = parameters;
		return new CommandContributionItem(parameter);
	}

	private void attachContextMenu(final Control c) {
		if (c == graph.getControl()) {
			// commit table
			c.setMenu(popupMgr.createContextMenu(c));
			c.addMenuDetectListener(new MenuDetectListener() {
				public void menuDetected(MenuDetectEvent e) {
					popupMgr.removeAll();

					int selectionSize = ((IStructuredSelection) getSelectionProvider()
							.getSelection()).size();
					int type = ((IResource) getInput()).getType();

					if (type == IResource.FILE) {
						if (selectionSize == 1)
							popupMgr
									.add(getCommandContributionItem(
											HistoryViewCommands.COMPARE_WITH_TREE,
											UIText.GitHistoryPage_CompareWithWorkingTreeMenuMenuLabel));
						else if (selectionSize == 2)
							popupMgr
									.add(getCommandContributionItem(
											HistoryViewCommands.COMPARE_VERSIONS,
											UIText.GitHistoryPage_CompareWithEachOtherMenuLabel));
						if (selectionSize > 0)
							popupMgr.add(getCommandContributionItem(
									HistoryViewCommands.OPEN,
									UIText.GitHistoryPage_OpenMenuLabel));
					}

					if (selectionSize == 1) {
						popupMgr.add(new Separator());
						popupMgr.add(getCommandContributionItem(
								HistoryViewCommands.CHECKOUT,
								UIText.GitHistoryPage_CheckoutMenuLabel));
						popupMgr.add(getCommandContributionItem(
								HistoryViewCommands.CREATE_BRANCH,
								UIText.GitHistoryPage_CreateBranchMenuLabel));
						popupMgr.add(getCommandContributionItem(
								HistoryViewCommands.CREATE_TAG,
								UIText.GitHistoryPage_CreateTagMenuLabel));
						popupMgr.add(getCommandContributionItem(
								HistoryViewCommands.CREATE_PATCH,
								UIText.GitHistoryPage_CreatePatchMenuLabel));
						popupMgr.add(new Separator());

						MenuManager resetManager = new MenuManager(
								UIText.GitHistoryPage_ResetMenuLabel,
								UIIcons.RESET, "Reset"); //$NON-NLS-1$

						popupMgr.add(resetManager);

						Map<String, String> parameters = new HashMap<String, String>();
						parameters.put(HistoryViewCommands.RESET_MODE, "Soft"); //$NON-NLS-1$
						resetManager.add(getCommandContributionItem(
								HistoryViewCommands.RESET,
								UIText.GitHistoryPage_ResetSoftMenuLabel,
								parameters));
						parameters = new HashMap<String, String>();
						parameters.put(HistoryViewCommands.RESET_MODE, "Mixed"); //$NON-NLS-1$
						resetManager.add(getCommandContributionItem(
								HistoryViewCommands.RESET,
								UIText.GitHistoryPage_ResetMixedMenuLabel,
								parameters));
						parameters = new HashMap<String, String>();
						parameters.put(HistoryViewCommands.RESET_MODE, "Hard"); //$NON-NLS-1$
						resetManager.add(getCommandContributionItem(
								HistoryViewCommands.RESET,
								UIText.GitHistoryPage_ResetHardMenuLabel,
								parameters));
					}
					popupMgr.add(new Separator());

					MenuManager quickDiffManager = new MenuManager(
							UIText.GitHistoryPage_QuickdiffMenuLabel, null,
							"Quickdiff"); //$NON-NLS-1$

					popupMgr.add(quickDiffManager);

					quickDiffManager.add(getCommandContributionItem(
							HistoryViewCommands.SET_QUICKDIFF_BASELINE,
							UIText.GitHistoryPage_SetAsBaselineMenuLabel));

					Map<String, String> parameters = new HashMap<String, String>();
					parameters.put(HistoryViewCommands.BASELINE_TARGET, "HEAD"); //$NON-NLS-1$
					quickDiffManager.add(getCommandContributionItem(
							HistoryViewCommands.RESET_QUICKDIFF_BASELINE,
							UIText.GitHistoryPage_ResetBaselineToHeadMenuLabel,
							parameters));

					parameters = new HashMap<String, String>();
					parameters.put(HistoryViewCommands.BASELINE_TARGET,
							"HEAD^1"); //$NON-NLS-1$
					quickDiffManager
							.add(getCommandContributionItem(
									HistoryViewCommands.RESET_QUICKDIFF_BASELINE,
									UIText.GitHistoryPage_ResetBaselineToParentOfHeadMenuLabel,
									parameters));

					// copy and such after additions
					popupMgr.add(new Separator(
							IWorkbenchActionConstants.MB_ADDITIONS));
					popupMgr.add(copyAction);
					popupMgr.add(new Separator());
					popupMgr.add(showCommentAction);
					popupMgr.add(showFilesAction);
				}
			});
		} else if (c == commentViewer.getControl()) {
			// comment viewer
			final MenuManager mgr = new MenuManager();
			c.setMenu(mgr.createContextMenu(c));
			c.addMenuDetectListener(new MenuDetectListener() {
				public void menuDetected(MenuDetectEvent e) {
					if (mgr.isEmpty()) {
						mgr.add(selectAllAction);
						mgr.add(copyAction);
						mgr.add(new Separator());
						mgr.add(wrapCommentAction);
						mgr.add(fillCommentAction);
					}
				}
			});
		} else {
			// file list
			final MenuManager mgr = new MenuManager();
			c.setMenu(mgr.createContextMenu(c));
			c.addMenuDetectListener(new MenuDetectListener() {
				public void menuDetected(MenuDetectEvent e) {
					if (mgr.isEmpty()) {
						mgr.add(selectAllAction);
						mgr.add(copyAction);
					}
				}
			});
		}
	}

	private void layoutSashForm(final SashForm sf, final String key) {
		sf.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				final int[] w = sf.getWeights();
				store.putValue(key, UIPreferences.intArrayToString(w));
				if (store.needsSaving())
					try {
						store.save();
					} catch (IOException e1) {
						Activator.handleError(e1.getMessage(), e1, false);
					}

			}
		});
		sf.setWeights(UIPreferences.stringToIntArray(store.getString(key), 2));
	}

	private Composite createMainPanel(final Composite parent) {
		final Composite c = new Composite(parent, SWT.NULL);
		final GridLayout parentLayout = new GridLayout();
		parentLayout.marginHeight = 0;
		parentLayout.marginWidth = 0;
		parentLayout.verticalSpacing = 0;
		c.setLayout(parentLayout);
		return c;
	}

	private void layout() {
		final boolean showComment = store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_REV_COMMENT);
		final boolean showFiles = store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_REV_DETAIL);
		final boolean showFindToolbar = store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_FINDTOOLBAR);

		if (showComment && showFiles) {
			graphDetailSplit.setMaximizedControl(null);
			revInfoSplit.setMaximizedControl(null);
		} else if (showComment && !showFiles) {
			graphDetailSplit.setMaximizedControl(null);
			revInfoSplit.setMaximizedControl(commentViewer.getControl());
		} else if (!showComment && showFiles) {
			graphDetailSplit.setMaximizedControl(null);
			revInfoSplit.setMaximizedControl(fileViewer.getControl());
		} else if (!showComment && !showFiles) {
			graphDetailSplit.setMaximizedControl(graph.getControl());
		}
		if (showFindToolbar) {
			((GridData) findToolbar.getLayoutData()).heightHint = SWT.DEFAULT;
		} else {
			((GridData) findToolbar.getLayoutData()).heightHint = 0;
			findToolbar.clear();
		}
		ourControl.layout();
	}

	private void attachCommitSelectionChanged() {
		graph.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				final ISelection s = event.getSelection();
				if (s.isEmpty() || !(s instanceof IStructuredSelection)) {
					commentViewer.setInput(null);
					fileViewer.setInput(null);
					return;
				}

				final IStructuredSelection sel;
				final PlotCommit<?> c;

				sel = ((IStructuredSelection) s);
				c = (PlotCommit<?>) sel.getFirstElement();
				commentViewer.setInput(c);
				fileViewer.setInput(c);
			}
		});
		commentViewer
				.addCommitNavigationListener(new CommitNavigationListener() {
					public void showCommit(final RevCommit c) {
						graph.selectCommit(c);
					}
				});
		findToolbar.addSelectionListener(new Listener() {
			public void handleEvent(Event event) {
				graph.selectCommit((RevCommit) event.data);
			}
		});
	}

	private void createLocalToolbarActions() {
		final IToolBarManager barManager = getSite().getActionBars()
				.getToolBarManager();
		IAction a;

		a = createFindToolbarAction();
		barManager.add(a);
	}

	private IAction createFindToolbarAction() {
		final IAction r = new Action(UIText.GitHistoryPage_find,
				UIIcons.ELCL16_FIND) {
			public void run() {
				store.setValue(UIPreferences.RESOURCEHISTORY_SHOW_FINDTOOLBAR,
						isChecked());
				if (store.needsSaving()) {
					try {
						store.save();
					} catch (IOException e) {
						Activator.handleError(e.getMessage(), e, false);
					}
				}
				layout();
			}
		};
		r.setChecked(store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_FINDTOOLBAR));
		r.setToolTipText(UIText.HistoryPage_findbar_findTooltip);
		return r;
	}

	private IAction createCommentWrap() {
		final BooleanPrefAction a = new BooleanPrefAction(
				UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_WRAP,
				UIText.ResourceHistory_toggleCommentWrap) {
			void apply(boolean wrap) {
				commentViewer.setWrap(wrap);
			}
		};
		a.apply(a.isChecked());
		actionsToDispose.add(a);
		return a;
	}

	private IAction createCommentFill() {
		final BooleanPrefAction a = new BooleanPrefAction(
				UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_FILL,
				UIText.ResourceHistory_toggleCommentFill) {
			void apply(boolean fill) {
				commentViewer.setFill(fill);
			}
		};
		a.apply(a.isChecked());
		actionsToDispose.add(a);
		return a;
	}

	private IAction createShowComment() {
		BooleanPrefAction a = new BooleanPrefAction(
				UIPreferences.RESOURCEHISTORY_SHOW_REV_COMMENT,
				UIText.ResourceHistory_toggleRevComment) {
			void apply(final boolean value) {
				layout();
			}

			@Override
			public void run() {
				super.run();
				wrapCommentAction.setEnabled(isChecked());
				fillCommentAction.setEnabled(isChecked());
			}

		};
		actionsToDispose.add(a);
		return a;
	}

	private IAction createShowFiles() {
		BooleanPrefAction a = new BooleanPrefAction(
				UIPreferences.RESOURCEHISTORY_SHOW_REV_DETAIL,
				UIText.ResourceHistory_toggleRevDetail) {
			void apply(final boolean value) {
				layout();
			}
		};
		actionsToDispose.add(a);
		return a;
	}

	private void createStandardActions() {
		final TextAction copy = new TextAction(ITextOperationTarget.COPY);
		final TextAction sAll = new TextAction(ITextOperationTarget.SELECT_ALL);

		graph.getControl().addFocusListener(copy);
		graph.getControl().addFocusListener(sAll);
		graph.addSelectionChangedListener(copy);
		graph.addSelectionChangedListener(sAll);

		commentViewer.getControl().addFocusListener(copy);
		commentViewer.getControl().addFocusListener(sAll);
		commentViewer.addSelectionChangedListener(copy);
		commentViewer.addSelectionChangedListener(sAll);

		fileViewer.getControl().addFocusListener(copy);
		fileViewer.getControl().addFocusListener(sAll);
		fileViewer.addSelectionChangedListener(copy);
		fileViewer.addSelectionChangedListener(sAll);

		final IActionBars b = getSite().getActionBars();
		b.setGlobalActionHandler(ActionFactory.COPY.getId(), copy);
		b.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), sAll);

		copyAction = createStandardAction(ActionFactory.COPY);
		selectAllAction = createStandardAction(ActionFactory.SELECT_ALL);

		showCommentAction = createShowComment();
		showFilesAction = createShowFiles();
		wrapCommentAction = createCommentWrap();
		fillCommentAction = createCommentFill();

		wrapCommentAction.setEnabled(showCommentAction.isChecked());
		fillCommentAction.setEnabled(showCommentAction.isChecked());

		final IMenuManager menuManager = getSite().getActionBars()
				.getMenuManager();

		menuManager.add(showFilesAction);
		menuManager.add(showCommentAction);

		menuManager.add(new Separator());

		menuManager.add(wrapCommentAction);
		menuManager.add(fillCommentAction);
	}

	private IAction createStandardAction(final ActionFactory af) {
		final IPageSite s = getSite();
		final IWorkbenchAction a = af.create(s.getWorkbenchWindow());
		if (af instanceof IPartListener)
			((IPartListener) a).partActivated(s.getPage().getActivePart());
		return a;
	}

	public void dispose() {
		if (myRefsChangedHandle != null) {
			myRefsChangedHandle.remove();
			myRefsChangedHandle = null;
		}

		// dispose of the actions (the history framework doesn't do this for us)
		for (BooleanPrefAction action : actionsToDispose)
			action.dispose();
		actionsToDispose.clear();
		cancelRefreshJob();
		if (popupMgr != null) {
			for (final IContributionItem i : popupMgr.getItems()) {
				if (i instanceof ActionFactory.IWorkbenchAction)
					((ActionFactory.IWorkbenchAction) i).dispose();
			}
			for (final IContributionItem i : getSite().getActionBars()
					.getMenuManager().getItems()) {
				if (i instanceof ActionFactory.IWorkbenchAction)
					((ActionFactory.IWorkbenchAction) i).dispose();
			}
		}
		super.dispose();
	}

	public void refresh() {
		inputSet();
	}

	@Override
	public void setFocus() {
		graph.getControl().setFocus();
	}

	@Override
	public Control getControl() {
		return ourControl;
	}

	public Object getInput() {
		final ResourceList r = (ResourceList) super.getInput();
		if (r == null)
			return null;
		final IResource[] in = r.getItems();
		if (in == null || in.length == 0)
			return null;
		if (in.length == 1)
			return in[0];
		return r;
	}

	public boolean setInput(final Object o) {
		final Object in;
		if (o instanceof IResource)
			in = new ResourceList(new IResource[] { (IResource) o });
		else if (o instanceof ResourceList)
			in = o;
		else if (o instanceof IAdaptable) {
			IResource resource = (IResource) ((IAdaptable) o)
					.getAdapter(IResource.class);
			in = resource == null ? null : new ResourceList(
					new IResource[] { resource });
		} else
			in = null;
		return super.setInput(in);
	}

	@Override
	public boolean inputSet() {
		cancelRefreshJob();

		if (graph == null || super.getInput() == null)
			return false;

		final IResource[] in = ((ResourceList) super.getInput()).getItems();
		if (in == null || in.length == 0)
			return false;

		db = null;
		if (currentWalk != null) {
			currentWalk.release();
			currentWalk = null;
		}

		final ArrayList<String> paths = new ArrayList<String>(in.length);
		for (final IResource r : in) {
			final RepositoryMapping map = RepositoryMapping.getMapping(r);
			if (map == null)
				continue;

			if (db == null)
				db = map.getRepository();
			else if (db != map.getRepository())
				return false;

			if (showAllFilter == ShowFilter.SHOWALLFOLDER) {
				final String name = map.getRepoRelativePath(r.getParent());
				if (name != null && name.length() > 0)
					paths.add(name);
			} else if (showAllFilter == ShowFilter.SHOWALLPROJECT) {
				final String name = map.getRepoRelativePath(r.getProject());
				if (name != null && name.length() > 0)
					paths.add(name);
			} else if (showAllFilter == ShowFilter.SHOWALLREPO) {
				// nothing
			} else /* if (showAllFilter == ShowFilter.SHOWALLRESOURCE) */{
				final String name = map.getRepoRelativePath(r);
				if (name != null && name.length() > 0)
					paths.add(name);
			}
		}

		if (db == null)
			return false;

		final AnyObjectId headId;
		try {
			headId = db.resolve(Constants.HEAD);
		} catch (IOException e) {
			Activator.logError(NLS.bind(UIText.GitHistoryPage_errorParsingHead,
					db.getDirectory().getAbsolutePath()), e);
			return false;
		}

		if (currentWalk == null || pathChange(pathFilters, paths)
				|| headId != null && !headId.equals(currentHeadId)) {
			// TODO Do not dispose SWTWalk just because HEAD changed
			// In theory we should be able to update the graph and
			// not dispose of the SWTWalk, even if HEAD was reset to
			// HEAD^1 and the old HEAD commit should not be visible.
			//
			currentWalk = new SWTWalk(db);
			currentWalk.sort(RevSort.COMMIT_TIME_DESC, true);
			currentWalk.sort(RevSort.BOUNDARY, true);
			highlightFlag = currentWalk.newFlag("highlight"); //$NON-NLS-1$
		} else {
			currentWalk.reset();
		}

		if (headId == null)
			return false;
		try {
			currentWalk.markStart(currentWalk.parseCommit(headId));
		} catch (IOException e) {
			Activator.logError(NLS.bind(
					UIText.GitHistoryPage_errorReadingHeadCommit, headId, db
							.getDirectory().getAbsolutePath()), e);
			return false;
		}

		final TreeWalk fileWalker = new TreeWalk(db);
		fileWalker.setRecursive(true);
		if (paths.size() > 0) {
			pathFilters = paths;
			currentWalk.setTreeFilter(AndTreeFilter.create(PathFilterGroup
					.createFromStrings(paths), TreeFilter.ANY_DIFF));
			fileWalker.setFilter(currentWalk.getTreeFilter().clone());

		} else {
			pathFilters = null;
			currentWalk.setTreeFilter(TreeFilter.ALL);
			fileWalker.setFilter(TreeFilter.ANY_DIFF);
		}
		fileViewer.setTreeWalk(db, fileWalker);
		fileViewer.addSelectionChangedListener(commentViewer);
		commentViewer.setTreeWalk(fileWalker);
		commentViewer.setDb(db);
		findToolbar.clear();
		graph.setInput(highlightFlag, null, null);

		final SWTCommitList list;
		list = new SWTCommitList(graph.getControl().getDisplay());
		list.source(currentWalk);

		final GenerateHistoryJob rj = new GenerateHistoryJob(this, list);
		rj.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(final IJobChangeEvent event) {
				final Control graphctl = graph.getControl();
				if (job != rj || graphctl.isDisposed())
					return;
				graphctl.getDisplay().asyncExec(new Runnable() {
					public void run() {
						if (job == rj)
							job = null;
					}
				});
			}
		});
		job = rj;
		schedule(rj);
		return true;
	}

	private void cancelRefreshJob() {
		if (job != null && job.getState() != Job.NONE) {
			job.cancel();

			// As the job had to be canceled but was working on
			// the data connected with the currentWalk we cannot
			// be sure it really finished. Since the walk is not
			// thread safe we must throw it away and build a new
			// one to start another walk. Clearing our field will
			// ensure that happens.
			//
			job = null;
			currentWalk = null;
			highlightFlag = null;
			pathFilters = null;
		}
	}

	private boolean pathChange(final List<String> o, final List<String> n) {
		if (o == null)
			return !n.isEmpty();
		return !o.equals(n);
	}

	private void schedule(final Job j) {
		final IWorkbenchPartSite site = getWorkbenchSite();
		if (site != null) {
			final IWorkbenchSiteProgressService p;
			p = (IWorkbenchSiteProgressService) site
					.getAdapter(IWorkbenchSiteProgressService.class);
			if (p != null) {
				p.schedule(j, 0, true /* use half-busy cursor */);
				return;
			}
		}
		j.schedule();
	}

	void showCommitList(final Job j, final SWTCommitList list,
			final SWTCommit[] asArray) {
		if (job != j || graph.getControl().isDisposed())
			return;

		graph.getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (!graph.getControl().isDisposed() && job == j) {
					graph.setInput(highlightFlag, list, asArray);
					findToolbar.setInput(highlightFlag, graph.getTableView()
							.getTable(), asArray);
				}
			}
		});
	}

	private IWorkbenchPartSite getWorkbenchSite() {
		final IWorkbenchPart part = getHistoryPageSite().getPart();
		return part != null ? part.getSite() : null;
	}

	public boolean isValidInput(final Object object) {
		return canShowHistoryFor(object);
	}

	public Object getAdapter(final Class adapter) {
		return null;
	}

	public String getName() {
		final ResourceList in = (ResourceList) super.getInput();
		if (currentWalk == null || in == null)
			return ""; //$NON-NLS-1$
		final IResource[] items = in.getItems();
		if (items.length == 0)
			return ""; //$NON-NLS-1$

		final StringBuilder b = new StringBuilder();
		b.append(db.getDirectory().getParentFile().getName());
		if (currentWalk.getRevFilter() != RevFilter.ALL) {
			b.append(": "); //$NON-NLS-1$
			b.append(currentWalk.getRevFilter());
		}
		if (currentWalk.getTreeFilter() != TreeFilter.ALL) {
			b.append(":"); //$NON-NLS-1$
			for (final String p : pathFilters) {
				b.append(' ');
				b.append(p);
			}
		}
		return b.toString();
	}

	public String getDescription() {
		return getName();
	}

	private abstract class BooleanPrefAction extends Action implements
			IPropertyChangeListener, ActionFactory.IWorkbenchAction {
		private final String prefName;

		BooleanPrefAction(final String pn, final String text) {
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
			apply(isChecked());
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

	private class TextAction extends Action implements FocusListener,
			ISelectionChangedListener {
		private final int op;

		TextAction(final int operationCode) {
			op = operationCode;
			setEnabled(false);
		}

		public void run() {
			if (commentViewer.getTextWidget().isFocusControl()) {
				if (commentViewer.canDoOperation(op))
					commentViewer.doOperation(op);
			} else if (fileViewer.getTable().isFocusControl()) {
				switch (op) {
				case ITextOperationTarget.COPY:
					fileViewer.doCopy();
					break;
				case ITextOperationTarget.SELECT_ALL:
					fileViewer.doSelectAll();
					break;
				}
			} else if (graph.getControl().isFocusControl()) {
				switch (op) {
				case ITextOperationTarget.COPY:
					graph.doCopy();
					break;
				}
			}
		}

		private void update() {
			if (commentViewer.getTextWidget().isFocusControl()) {
				setEnabled(commentViewer.canDoOperation(op));
			} else if (fileViewer.getTable().isFocusControl()) {
				switch (op) {
				case ITextOperationTarget.COPY:
					setEnabled(!fileViewer.getSelection().isEmpty());
					break;
				case ITextOperationTarget.SELECT_ALL:
					setEnabled(fileViewer.getTable().getItemCount() > 0);
					break;
				}
			} else if (graph.getControl().isFocusControl()) {
				switch (op) {
				case ITextOperationTarget.COPY:
					setEnabled(graph.canDoCopy());
					break;
				case ITextOperationTarget.SELECT_ALL:
					setEnabled(false);
					break;
				}
			}
		}

		public void focusGained(final FocusEvent e) {
			update();
		}

		public void selectionChanged(final SelectionChangedEvent event) {
			update();
		}

		public void focusLost(final FocusEvent e) {
			// Ignore lost events. If focus leaves our page then the
			// workbench will update the global action away from us.
			// If focus stays in our page someone else should have
			// gained it from us.
		}
	}
}
