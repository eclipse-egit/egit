/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (c) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2010-2011, Matthias Sohn <matthias.sohn@sap.com>
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
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefNode;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

/** Graphical commit history viewer. */
public class GitHistoryPage extends HistoryPage implements RefsChangedListener {

	/** actions used in GitHistoryPage **/
	private static class GitHistoryPageActions {

		private abstract class BooleanPrefAction extends Action implements
				IPropertyChangeListener, IWorkbenchAction {
			private final String prefName;

			BooleanPrefAction(final String pn, final String text) {
				setText(text);
				prefName = pn;
				historyPage.store.addPropertyChangeListener(this);
				setChecked(historyPage.store.getBoolean(prefName));
			}

			public void run() {
				historyPage.store.setValue(prefName, isChecked());
				if (historyPage.store.needsSaving()) {
					try {
						historyPage.store.save();
					} catch (IOException e) {
						Activator.handleError(e.getMessage(), e, false);
					}
				}
			}

			abstract void apply(boolean value);

			public void propertyChange(final PropertyChangeEvent event) {
				if (prefName.equals(event.getProperty())) {
					setChecked(historyPage.store.getBoolean(prefName));
					apply(isChecked());
				}
			}

			public void dispose() {
				// stop listening
				historyPage.store.removePropertyChangeListener(this);
			}
		}

		private class ShowFilterAction extends Action {
			private final ShowFilter filter;

			ShowFilterAction(ShowFilter filter, ImageDescriptor icon,
					String menuLabel, String toolTipText) {
				super(null, IAction.AS_CHECK_BOX);
				this.filter = filter;
				setImageDescriptor(icon);
				setText(menuLabel);
				setToolTipText(toolTipText);
			}

			@Override
			public void run() {
				String oldName = historyPage.getName();
				String oldDescription = historyPage.getDescription();
				if (!isChecked()) {
					if (historyPage.showAllFilter == filter) {
						historyPage.showAllFilter = ShowFilter.SHOWALLRESOURCE;
						showAllResourceVersionsAction.setChecked(true);
						historyPage.initAndStartRevWalk(false);
					}
				}
				if (isChecked() && historyPage.showAllFilter != filter) {
					historyPage.showAllFilter = filter;
					if (this != showAllRepoVersionsAction)
						showAllRepoVersionsAction.setChecked(false);
					if (this != showAllProjectVersionsAction)
						showAllProjectVersionsAction.setChecked(false);
					if (this != showAllFolderVersionsAction)
						showAllFolderVersionsAction.setChecked(false);
					if (this != showAllResourceVersionsAction)
						showAllResourceVersionsAction.setChecked(false);
					historyPage.initAndStartRevWalk(false);
				}
				historyPage.firePropertyChange(historyPage, P_NAME, oldName,
						historyPage.getName());
				// even though this is currently ending nowhere (see bug
				// 324386), we
				// still create the event
				historyPage.firePropertyChange(historyPage, P_DESCRIPTION,
						oldDescription, historyPage.getDescription());
				Activator.getDefault().getPreferenceStore().setValue(
						PREF_SHOWALLFILTER,
						historyPage.showAllFilter.toString());
			}

			@Override
			public String toString() {
				return "ShowFilter[" + filter.toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		List<IWorkbenchAction> actionsToDispose;

		BooleanPrefAction showRelativeDateAction;

		BooleanPrefAction wrapCommentAction;

		BooleanPrefAction fillCommentAction;

		IAction findAction;

		IAction refreshAction;

		BooleanPrefAction showCommentAction;

		BooleanPrefAction showFilesAction;

		IWorkbenchAction compareModeAction;

		IWorkbenchAction showAllBranchesAction;

		IWorkbenchAction reuseCompareEditorAction;

		ShowFilterAction showAllRepoVersionsAction;

		ShowFilterAction showAllProjectVersionsAction;

		ShowFilterAction showAllFolderVersionsAction;

		ShowFilterAction showAllResourceVersionsAction;

		private GitHistoryPage historyPage;

		GitHistoryPageActions(GitHistoryPage historyPage) {
			actionsToDispose = new ArrayList<IWorkbenchAction>();
			this.historyPage = historyPage;
			createActions();
		}

		private void createActions() {
			createFindToolbarAction();
			createRefreshAction();
			createFilterActions();
			createCompareModeAction();
			createReuseCompareEditorAction();
			createShowAllBranchesAction();
			createShowCommentAction();
			createShowFilesAction();
			createShowRelativeDateAction();
			createWrapCommentAction();
			createFillCommentAction();

			wrapCommentAction.setEnabled(showCommentAction.isChecked());
			fillCommentAction.setEnabled(showCommentAction.isChecked());
		}

		private void createFindToolbarAction() {
			findAction = new Action(UIText.GitHistoryPage_FindMenuLabel,
					UIIcons.ELCL16_FIND) {
				public void run() {
					historyPage.store.setValue(
							UIPreferences.RESOURCEHISTORY_SHOW_FINDTOOLBAR,
							isChecked());
					if (historyPage.store.needsSaving()) {
						try {
							historyPage.store.save();
						} catch (IOException e) {
							Activator.handleError(e.getMessage(), e, false);
						}
					}
					historyPage.layout();
				}
			};
			findAction
					.setChecked(historyPage.store
							.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_FINDTOOLBAR));
			findAction.setToolTipText(UIText.GitHistoryPage_FindTooltip);
		}

		private void createRefreshAction() {
			refreshAction = new Action(UIText.GitHistoryPage_RefreshMenuLabel,
					UIIcons.ELCL16_REFRESH) {
				@Override
				public void run() {
					historyPage.refresh();
				}
			};
		}

		private void createFilterActions() {
			showAllRepoVersionsAction = new ShowFilterAction(
					ShowFilter.SHOWALLREPO, UIIcons.REPOSITORY,
					UIText.GitHistoryPage_AllInRepoMenuLabel,
					UIText.GitHistoryPage_AllInRepoTooltip);

			showAllProjectVersionsAction = new ShowFilterAction(
					ShowFilter.SHOWALLPROJECT, UIIcons.FILTERPROJECT,
					UIText.GitHistoryPage_AllInProjectMenuLabel,
					UIText.GitHistoryPage_AllInProjectTooltip);

			showAllFolderVersionsAction = new ShowFilterAction(
					ShowFilter.SHOWALLFOLDER, UIIcons.FILTERFOLDER,
					UIText.GitHistoryPage_AllInParentMenuLabel,
					UIText.GitHistoryPage_AllInParentTooltip);

			showAllResourceVersionsAction = new ShowFilterAction(
					ShowFilter.SHOWALLRESOURCE, UIIcons.FILTERRESOURCE,
					UIText.GitHistoryPage_AllOfResourceMenuLabel,
					UIText.GitHistoryPage_AllOfResourceTooltip);

			showAllRepoVersionsAction
					.setChecked(historyPage.showAllFilter == showAllRepoVersionsAction.filter);
			showAllProjectVersionsAction
					.setChecked(historyPage.showAllFilter == showAllProjectVersionsAction.filter);
			showAllFolderVersionsAction
					.setChecked(historyPage.showAllFilter == showAllFolderVersionsAction.filter);
			showAllResourceVersionsAction
					.setChecked(historyPage.showAllFilter == showAllResourceVersionsAction.filter);
		}

		private void createCompareModeAction() {
			compareModeAction = new BooleanPrefAction(
					UIPreferences.RESOURCEHISTORY_COMPARE_MODE,
					UIText.GitHistoryPage_CompareModeMenuLabel) {
				@Override
				void apply(boolean value) {
					// nothing, just switch the preference
				}
			};
			compareModeAction.setImageDescriptor(UIIcons.ELCL16_COMPARE_VIEW);
			compareModeAction.setToolTipText(UIText.GitHistoryPage_compareMode);
			actionsToDispose.add(compareModeAction);
		}

		private void createReuseCompareEditorAction() {
			reuseCompareEditorAction = new CompareUtils.ReuseCompareEditorAction();
			actionsToDispose.add(reuseCompareEditorAction);
		}

		private void createShowAllBranchesAction() {
			showAllBranchesAction = new BooleanPrefAction(
					UIPreferences.RESOURCEHISTORY_SHOW_ALL_BRANCHES,
					UIText.GitHistoryPage_ShowAllBranchesMenuLabel) {

				@Override
				void apply(boolean value) {
					historyPage.refresh();
				}
			};
			showAllBranchesAction.setImageDescriptor(UIIcons.BRANCH);
			showAllBranchesAction
					.setToolTipText(UIText.GitHistoryPage_showAllBranches);
			actionsToDispose.add(showAllBranchesAction);
		}

		private void createShowCommentAction() {
			showCommentAction = new BooleanPrefAction(
					UIPreferences.RESOURCEHISTORY_SHOW_REV_COMMENT,
					UIText.ResourceHistory_toggleRevComment) {
				void apply(final boolean value) {
					historyPage.layout();
					wrapCommentAction.setEnabled(isChecked());
					fillCommentAction.setEnabled(isChecked());
				}
			};
			actionsToDispose.add(showCommentAction);
		}

		private void createShowFilesAction() {
			showFilesAction = new BooleanPrefAction(
					UIPreferences.RESOURCEHISTORY_SHOW_REV_DETAIL,
					UIText.ResourceHistory_toggleRevDetail) {
				void apply(final boolean value) {
					historyPage.layout();
				}
			};
			actionsToDispose.add(showFilesAction);
		}

		private void createShowRelativeDateAction() {
			showRelativeDateAction = new BooleanPrefAction(
					UIPreferences.RESOURCEHISTORY_SHOW_RELATIVE_DATE,
					UIText.ResourceHistory_toggleRelativeDate) {
				void apply(boolean date) {
					// nothing, just set the Preference
				}
			};
			showRelativeDateAction.apply(showRelativeDateAction.isChecked());
			actionsToDispose.add(showRelativeDateAction);
		}

		private void createWrapCommentAction() {
			wrapCommentAction = new BooleanPrefAction(
					UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_WRAP,
					UIText.ResourceHistory_toggleCommentWrap) {
				void apply(boolean wrap) {
					// nothing, just set the Preference
				}
			};
			wrapCommentAction.apply(wrapCommentAction.isChecked());
			actionsToDispose.add(wrapCommentAction);
		}

		private void createFillCommentAction() {
			fillCommentAction = new BooleanPrefAction(
					UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_FILL,
					UIText.ResourceHistory_toggleCommentFill) {
				void apply(boolean fill) {
					// nothing, just set the Preference
				}
			};
			fillCommentAction.apply(fillCommentAction.isChecked());
			actionsToDispose.add(fillCommentAction);
		}
	}

	private static final String POPUP_ID = "org.eclipse.egit.ui.historyPageContributions"; //$NON-NLS-1$

	private static final String DESCRIPTION_PATTERN = "{0} - {1}"; //$NON-NLS-1$

	private static final String NAME_PATTERN = "{0}: {1} [{2}]"; //$NON-NLS-1$

	private static final String PREF_SHOWALLFILTER = "org.eclipse.egit.ui.githistorypage.showallfilter"; //$NON-NLS-1$

	enum ShowFilter {
		SHOWALLRESOURCE, SHOWALLFOLDER, SHOWALLPROJECT, SHOWALLREPO,
	}

	private ShowFilter showAllFilter = ShowFilter.SHOWALLRESOURCE;

	private GitHistoryPageActions actions;

	/** An error text to be shown instead of the control */
	private StyledText errorText;

	private final IPersistentPreferenceStore store = (IPersistentPreferenceStore) Activator
			.getDefault().getPreferenceStore();

	private ListenerHandle myRefsChangedHandle;

	private HistoryPageInput input;

	private String name;

	private boolean trace = GitTraceLocation.HISTORYVIEW.isActive();

	/** Overall composite hosting all of our controls. */
	private Composite topControl;

	/** Overall composite hosting the controls that displays the history. */
	private Composite historyControl;

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

	/** A label showing a warning icon */
	private Composite warningComposite;

	/** A text field to display a warning */
	private Text warningText;

	/** Our context menu manager for the entire page. */
	private final MenuManager popupMgr = new MenuManager(null, POPUP_ID);

	/** Job that is updating our history view, if we are refreshing. */
	private GenerateHistoryJob job;

	/** Revision walker that allocated our graph's commit nodes. */
	private SWTWalk currentWalk;

	/** Last HEAD */
	private AnyObjectId currentHeadId;

	/** Repository of the last input*/
	private Repository currentRepo;

	private boolean currentShowAllBranches;

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

	private Runnable refschangedRunnable;

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
		if (object instanceof HistoryPageInput) {
			return true;
		}

		if (object instanceof IResource) {
			return typeOk((IResource) object);
		}

		if (object instanceof RepositoryTreeNode)
			return true;

		if (object instanceof IAdaptable) {
			IResource resource = (IResource) ((IAdaptable) object)
					.getAdapter(IResource.class);
			return resource == null ? false : typeOk(resource);
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

	/**
	 * The default constructor
	 */
	public GitHistoryPage() {
		trace = GitTraceLocation.HISTORYVIEW.isActive();
		if (trace)
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.HISTORYVIEW.getLocation());
	}

	@Override
	public void createControl(final Composite parent) {
		trace = GitTraceLocation.HISTORYVIEW.isActive();
		if (trace)
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.HISTORYVIEW.getLocation());

		historyControl = createMainPanel(parent);

		warningComposite = new Composite(historyControl, SWT.NONE);
		warningComposite.setLayout(new GridLayout(3, false));
		Label warningLabel = new Label(warningComposite, SWT.NONE);
		warningLabel.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK));
		warningText = new Text(warningComposite, SWT.READ_ONLY);
		warningText.setToolTipText(UIText.GitHistoryPage_IncompleteListTooltip);

		Link preferencesLink = new Link(warningComposite, SWT.NONE);
		preferencesLink.setText(UIText.CommitDialog_ConfigureLink);
		preferencesLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String preferencePageId = "org.eclipse.egit.ui.GitPreferences"; //$NON-NLS-1$
				PreferenceDialog dialog = PreferencesUtil
						.createPreferenceDialogOn(getSite().getShell(), preferencePageId,
								new String[] { preferencePageId }, null);
				dialog.open();
			}
		});

		GridDataFactory.fillDefaults().grab(true, true).applyTo(historyControl);
		graphDetailSplit = new SashForm(historyControl, SWT.VERTICAL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(
				graphDetailSplit);
		graph = new CommitGraphTable(graphDetailSplit, getSite(), popupMgr);

		// react on changes in the date preferences
		IPropertyChangeListener listener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(
						UIPreferences.RESOURCEHISTORY_SHOW_RELATIVE_DATE)) {
					if (graph.setRelativeDate(((Boolean) event.getNewValue())
							.booleanValue()))
						graph.getTableView().refresh();
					return;
				}
			}
		};
		graph.setRelativeDate(Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_RELATIVE_DATE));
		Activator.getDefault().getPreferenceStore()
				.addPropertyChangeListener(listener);

		revInfoSplit = new SashForm(graphDetailSplit, SWT.HORIZONTAL);
		commentViewer = new CommitMessageViewer(revInfoSplit, getSite(), getPartSite());
		fileViewer = new CommitFileDiffViewer(revInfoSplit, getSite());
		findToolbar = new FindToolbar(historyControl);

		layoutSashForm(graphDetailSplit,
				UIPreferences.RESOURCEHISTORY_GRAPH_SPLIT);
		layoutSashForm(revInfoSplit, UIPreferences.RESOURCEHISTORY_REV_SPLIT);

		attachCommitSelectionChanged();
		initActions();

		getSite().registerContextMenu(POPUP_ID, popupMgr, graph.getTableView());
		// due to the issues described in bug 322751, it makes no
		// sense to set a selection provider for the site here
		layout();

		myRefsChangedHandle = Repository.getGlobalListenerList()
				.addRefsChangedListener(this);

		if (trace)
			GitTraceLocation.getTrace().traceExit(
					GitTraceLocation.HISTORYVIEW.getLocation());
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
		topControl = new Composite(parent, SWT.NONE);
		StackLayout layout = new StackLayout();
		topControl.setLayout(layout);

		final Composite c = new Composite(topControl, SWT.NULL);
		layout.topControl = c;
		// shown instead of the splitter if an error message was set
		errorText = new StyledText(topControl, SWT.NONE);
		// use the same font as in message viewer
		errorText.setFont(UIUtils
				.getFont(UIPreferences.THEME_CommitMessageFont));
		errorText.setText(UIText.CommitFileDiffViewer_SelectOneCommitMessage);

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
			// the parent of the control!
			revInfoSplit.setMaximizedControl(fileViewer.getControl()
					.getParent());
		} else if (!showComment && !showFiles) {
			graphDetailSplit.setMaximizedControl(graph.getControl());
		}
		if (showFindToolbar) {
			((GridData) findToolbar.getLayoutData()).heightHint = SWT.DEFAULT;
		} else {
			((GridData) findToolbar.getLayoutData()).heightHint = 0;
			findToolbar.clear();
		}
		historyControl.layout();
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

				final IStructuredSelection sel = ((IStructuredSelection) s);
				if (sel.size() > 1) {
					commentViewer.setInput(null);
					fileViewer.setInput(null);
					return;
				}
				final PlotCommit<?> c = (PlotCommit<?>) sel.getFirstElement();
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

	private void initActions() {
		try {
			showAllFilter = ShowFilter.valueOf(Activator.getDefault()
					.getPreferenceStore().getString(PREF_SHOWALLFILTER));
		} catch (IllegalArgumentException e) {
			showAllFilter = ShowFilter.SHOWALLRESOURCE;
		}

		actions = new GitHistoryPageActions(this);
		setupToolBar();
		setupViewMenu();

		graph.getControl().addMenuDetectListener(new MenuDetectListener() {
			public void menuDetected(MenuDetectEvent e) {
				popupMgr.add(actions.showFilesAction);
				popupMgr.add(actions.showCommentAction);
			}
		});
	}

	private void setupToolBar() {
		IToolBarManager mgr = getSite().getActionBars().getToolBarManager();
		mgr.add(actions.findAction);
		mgr.add(new Separator());
		mgr.add(actions.showAllRepoVersionsAction);
		mgr.add(actions.showAllProjectVersionsAction);
		mgr.add(actions.showAllFolderVersionsAction);
		mgr.add(actions.showAllResourceVersionsAction);
		mgr.add(new Separator());
		mgr.add(actions.compareModeAction);
		mgr.add(actions.showAllBranchesAction);
	}

	private void setupViewMenu() {
		IMenuManager viewMenuMgr = getSite().getActionBars().getMenuManager();
		viewMenuMgr.add(actions.refreshAction);

		viewMenuMgr.add(new Separator());
		IMenuManager showSubMenuMgr = new MenuManager(
				UIText.GitHistoryPage_ShowSubMenuLabel);
		viewMenuMgr.add(showSubMenuMgr);
		showSubMenuMgr.add(actions.showAllBranchesAction);
		showSubMenuMgr.add(actions.findAction);
		showSubMenuMgr.add(actions.showFilesAction);
		showSubMenuMgr.add(actions.showCommentAction);

		IMenuManager filterSubMenuMgr = new MenuManager(
				UIText.GitHistoryPage_FilterSubMenuLabel);
		viewMenuMgr.add(filterSubMenuMgr);
		filterSubMenuMgr.add(actions.showAllRepoVersionsAction);
		filterSubMenuMgr.add(actions.showAllProjectVersionsAction);
		filterSubMenuMgr.add(actions.showAllFolderVersionsAction);
		filterSubMenuMgr.add(actions.showAllResourceVersionsAction);

		viewMenuMgr.add(new Separator());
		viewMenuMgr.add(actions.compareModeAction);
		viewMenuMgr.add(actions.reuseCompareEditorAction);

		viewMenuMgr.add(new Separator());
		viewMenuMgr.add(actions.wrapCommentAction);
		viewMenuMgr.add(actions.fillCommentAction);

		viewMenuMgr.add(new Separator());
		viewMenuMgr.add(actions.showRelativeDateAction);
	}

	public void dispose() {
		trace = GitTraceLocation.HISTORYVIEW.isActive();
		if (trace)
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.HISTORYVIEW.getLocation());

		if (myRefsChangedHandle != null) {
			myRefsChangedHandle.remove();
			myRefsChangedHandle = null;
		}

		// dispose of the actions (the history framework doesn't do this for us)
		for (IWorkbenchAction action : actions.actionsToDispose)
			action.dispose();
		actions.actionsToDispose.clear();
		cancelRefreshJob();
		if (popupMgr != null) {
			for (final IContributionItem i : popupMgr.getItems()) {
				if (i instanceof IWorkbenchAction)
					((IWorkbenchAction) i).dispose();
			}
			for (final IContributionItem i : getSite().getActionBars()
					.getMenuManager().getItems()) {
				if (i instanceof IWorkbenchAction)
					((IWorkbenchAction) i).dispose();
			}
		}
		super.dispose();
	}

	@Override
	public void setFocus() {
		if (repoHasBeenRemoved(currentRepo))
			clearHistoryPage();

		graph.getControl().setFocus();
	}

	private boolean repoHasBeenRemoved(final Repository repo) {
		return (repo != null && repo.getDirectory() != null && !repo
				.getDirectory().exists());
	}

	private void clearHistoryPage() {
		currentRepo = null;
		name = ""; //$NON-NLS-1$
		input = null;
		clearCommentViewer();
		clearFileViewer();
		setInput(null);
	}

	private void clearCommentViewer() {
		commentViewer.setRepository(null);
		commentViewer.setInput(null);
	}

	private void clearFileViewer() {
		fileViewer.setTreeWalk(null, null);
		fileViewer.setInput(null);
	}

	@Override
	public Control getControl() {
		return topControl;
	}

	public void refresh() {
		if (repoHasBeenRemoved(currentRepo))
			clearHistoryPage();

		this.input = null;
		inputSet();
	}

	/**
	 * @param compareMode
	 *            switch compare mode button of the view on / off
	 */
	public void setCompareMode(boolean compareMode) {
		store.setValue(UIPreferences.RESOURCEHISTORY_COMPARE_MODE, compareMode);
	}

	/**
	 * @return the selection provider
	 */
	public ISelectionProvider getSelectionProvider() {
		return graph.getTableView();
	}

	public void onRefsChanged(final RefsChangedEvent e) {
		if (input == null || e.getRepository() != input.getRepository())
			return;

		if (getControl().isDisposed())
			return;

		synchronized (this) {
			if (refschangedRunnable == null) {
				refschangedRunnable = new Runnable() {
					public void run() {
						if (!getControl().isDisposed()) {
							if (GitTraceLocation.HISTORYVIEW.isActive())
								GitTraceLocation
										.getTrace()
										.trace(
												GitTraceLocation.HISTORYVIEW
														.getLocation(),
												"Executing async repository changed event"); //$NON-NLS-1$
							refschangedRunnable = null;
							initAndStartRevWalk(true);
						}
					}
				};
				getControl().getDisplay().asyncExec(refschangedRunnable);
			}
		}
	}

	@Override
	public boolean setInput(Object object) {
		try {
			// hide the warning text initially
			setWarningText(null);
			trace = GitTraceLocation.HISTORYVIEW.isActive();
			if (trace)
				GitTraceLocation.getTrace().traceEntry(
						GitTraceLocation.HISTORYVIEW.getLocation(), object);

			if (object == getInput())
				return true;
			this.input = null;
			return super.setInput(object);
		} finally {
			if (trace)
				GitTraceLocation.getTrace().traceExit(
						GitTraceLocation.HISTORYVIEW.getLocation());
		}
	}

	@Override
	public boolean inputSet() {
		try {
			if (trace)
				GitTraceLocation.getTrace().traceEntry(
						GitTraceLocation.HISTORYVIEW.getLocation());
			if (this.input != null)
				return true;

			cancelRefreshJob();
			Object o = super.getInput();
			if (o == null) {
				setErrorMessage(UIText.GitHistoryPage_NoInputMessage);
				return false;
			}

			boolean showHead = false;
			boolean showRef = false;
			boolean showTag = false;
			Repository repo = null;
			Ref ref = null;
			if (o instanceof IResource) {
				RepositoryMapping mapping = RepositoryMapping
						.getMapping((IResource) o);
				if (mapping != null) {
					repo = mapping.getRepository();
					input = new HistoryPageInput(repo,
							new IResource[] { (IResource) o });
					showHead = true;
				}
			} else if (o instanceof RepositoryTreeNode) {
				RepositoryTreeNode repoNode = (RepositoryTreeNode) o;
				repo = repoNode.getRepository();
				switch (repoNode.getType()) {
				case FILE:
					File file = ((FileNode) repoNode).getObject();
					input = new HistoryPageInput(repo, new File[] { file });
					showHead = true;
					break;
				case FOLDER:
					File folder = ((FolderNode) repoNode).getObject();
					input = new HistoryPageInput(repo, new File[] { folder });
					showHead = true;
					break;
				case REF:
					input = new HistoryPageInput(repo);
					ref = ((RefNode) repoNode).getObject();
					showRef = true;
					break;
				case ADDITIONALREF:
					input = new HistoryPageInput(repo);
					ref = ((AdditionalRefNode) repoNode).getObject();
					showRef = true;
					break;
				case TAG:
					input = new HistoryPageInput(repo);
					ref = ((TagNode) repoNode).getObject();
					showTag = true;
					break;
				default:
					input = new HistoryPageInput(repo);
					showHead = true;
					break;
				}
			} else if (o instanceof HistoryPageInput)
				input = (HistoryPageInput) o;
			else if (o instanceof IAdaptable) {
				IResource resource = (IResource) ((IAdaptable) o)
						.getAdapter(IResource.class);
				if (resource != null) {
					RepositoryMapping mapping = RepositoryMapping
							.getMapping(resource);
					repo = mapping.getRepository();
					input = new HistoryPageInput(repo,
							new IResource[] { resource });
				}
			}
			if (input == null) {
				this.name = ""; //$NON-NLS-1$
				setErrorMessage(UIText.GitHistoryPage_NoInputMessage);
				return false;
			}
			Repository db = input.getRepository();
			if (resolveHead(db, true) == null) {
				this.name = ""; //$NON-NLS-1$
				setErrorMessage(UIText.GitHistoryPage_NoInputMessage);
				return false;
			}

			final IResource[] inResources = input.getItems();
			final File[] inFiles = input.getFileList();
			if (inResources != null && inResources.length == 0) {
				this.name = ""; //$NON-NLS-1$
				setErrorMessage(UIText.GitHistoryPage_NoInputMessage);
				return false;
			}

			this.name = calculateName(input);

			// disable the filters if we have a Repository as input
			boolean filtersActive = inResources != null || inFiles != null;
			actions.showAllRepoVersionsAction.setEnabled(filtersActive);
			actions.showAllProjectVersionsAction.setEnabled(filtersActive);
			// the repository itself has no notion of projects
			actions.showAllFolderVersionsAction.setEnabled(inResources != null);
			actions.showAllResourceVersionsAction.setEnabled(filtersActive);

			setErrorMessage(null);
			try {
				initAndStartRevWalk(false);
			} catch (IllegalStateException e) {
				Activator.handleError(e.getMessage(), e, true);
				return false;
			}

			if (showHead)
				showHead(repo);
			if (showRef)
				showRef(ref, repo);
			if (showTag)
				showTag(ref, repo);

			return true;
		} finally {
			if (trace)
				GitTraceLocation.getTrace().traceExit(
						GitTraceLocation.HISTORYVIEW.getLocation());
		}
	}

	private void showHead(Repository repo) {
		RevWalk rw = new RevWalk(repo);
		try {
			ObjectId head = repo.resolve(Constants.HEAD);
			if (head == null)
				return;
			RevCommit c = rw.parseCommit(head);
			graph.selectCommitStored(c);
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
	}

	private void showRef(Ref ref, Repository repo) {
		RevWalk rw = new RevWalk(repo);
		try {
			RevCommit c = rw.parseCommit(ref.getLeaf().getObjectId());
			graph.selectCommit(c);
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
	}

	private void showTag(Ref ref, Repository repo) {
		RevWalk rw = new RevWalk(repo);
		try {
			RevCommit c = null;
			RevObject any = rw.parseAny(ref.getLeaf().getObjectId());
			if (any instanceof RevCommit) {
				c = (RevCommit) any;
			} else if (any instanceof RevTag) {
				RevTag t = rw.parseTag(any);
				Object anyCommit = rw.parseAny(t.getObject());
				if (anyCommit instanceof RevCommit)
					c = (RevCommit) anyCommit;
			}
			if (c != null)
				graph.selectCommit(c);
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
	}

	private static String calculateName(HistoryPageInput in) {
		// we always visualize the current input in the form
		// <type>: <path> [<respository name>]
		// in order to give the user an understanding which context
		// menus they can expect with the current input
		// we show the filter hint only upon getDescription()
		// as it wrongly pollutes the navigation history
		final String repositoryName = Activator.getDefault()
				.getRepositoryUtil().getRepositoryName(in.getRepository());
		if (in.getItems() == null && in.getFileList() == null) {
			// plain repository, no files specified
			return NLS.bind(UIText.GitHistoryPage_RepositoryNamePattern,
					repositoryName);
		} else if (in.getItems() != null && in.getItems().length == 1) {
			// single resource
			IResource resource = in.getItems()[0];
			final String type;
			switch (resource.getType()) {
			case IResource.FILE:
				type = UIText.GitHistoryPage_FileType;
				break;
			case IResource.PROJECT:
				type = UIText.GitHistoryPage_ProjectType;
				break;
			default:
				type = UIText.GitHistoryPage_FolderType;
				break;
			}
			String path = resource.getFullPath().makeRelative().toString();
			if (resource.getType() == IResource.FOLDER)
				path = path + '/';
			return NLS.bind(NAME_PATTERN, new Object[] { type, path,
					repositoryName });
		} else if (in.getFileList() != null && in.getFileList().length == 1) {
			// single file from Repository
			File resource = in.getFileList()[0];
			String path;
			final String type;
			if (resource.isDirectory()) {
				type = UIText.GitHistoryPage_FolderType;
				path = resource.getPath() + IPath.SEPARATOR;
			} else {
				type = UIText.GitHistoryPage_FileType;
				path = resource.getPath();
			}
			return NLS.bind(NAME_PATTERN, new Object[] { type, path,
					repositoryName });
		} else {
			// user has selected multiple resources and then hits Team->Show in
			// History (the generic history view can not deal with multiple
			// selection)
			int count = 0;
			StringBuilder b = new StringBuilder();
			if (in.getItems() != null) {
				count = in.getItems().length;
				for (IResource res : in.getItems()) {
					b.append(res.getFullPath());
					if (res.getType() == IResource.FOLDER)
						b.append('/');
					// limit the total length
					if (b.length() > 100) {
						b.append("...  "); //$NON-NLS-1$
						break;
					}
					b.append(", "); //$NON-NLS-1$
				}
			}
			if (in.getFileList() != null) {
				count = in.getFileList().length;
				for (File file : in.getFileList()) {
					b.append(getRepoRelativePath(in.getRepository(), file));
					if (file.isDirectory())
						b.append('/');
					// limit the total length
					if (b.length() > 100) {
						b.append("...  "); //$NON-NLS-1$
						break;
					}
					b.append(", "); //$NON-NLS-1$
				}
			}
			// trim off the last ", " (or "  " if total length exceeded)
			if (b.length() > 2)
				b.setLength(b.length() - 2);
			String multiResourcePrefix = NLS.bind(
					UIText.GitHistoryPage_MultiResourcesType, Integer
							.valueOf(count));
			return NLS.bind(NAME_PATTERN, new Object[] { multiResourcePrefix,
					b.toString(), repositoryName });
		}
	}

	private static String getRepoRelativePath(Repository repo, File file) {
		IPath workdirPath = new Path(repo.getWorkTree().getPath());
		IPath filePath = new Path(file.getPath()).setDevice(null);
		return filePath.removeFirstSegments(workdirPath.segmentCount())
				.toString();
	}

	/**
	 * @param message
	 *            the message to display instead of the control
	 */
	public void setErrorMessage(final String message) {
		if (trace)
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.HISTORYVIEW.getLocation(), message);
		getHistoryPageSite().getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (topControl.isDisposed())
					return;
				StackLayout layout = (StackLayout) topControl.getLayout();
				if (message != null) {
					errorText.setText(message);
					layout.topControl = errorText;
				} else {
					errorText.setText(""); //$NON-NLS-1$
					layout.topControl = historyControl;
				}
				topControl.layout();
			}
		});
		if (trace)
			GitTraceLocation.getTrace().traceExit(
					GitTraceLocation.HISTORYVIEW.getLocation());
	}

	public boolean isValidInput(final Object object) {
		return canShowHistoryFor(object);
	}

	public Object getAdapter(final Class adapter) {
		return null;
	}

	public String getDescription() {
		// this doesn't seem to be rendered anywhere, but still...
		String filterHint = null;
		switch (showAllFilter) {
		case SHOWALLREPO:
			filterHint = UIText.GitHistoryPage_AllChangesInRepoHint;
			break;
		case SHOWALLPROJECT:
			filterHint = UIText.GitHistoryPage_AllChangesInProjectHint;
			break;
		case SHOWALLFOLDER:
			filterHint = UIText.GitHistoryPage_AllChangesInFolderHint;
			break;
		case SHOWALLRESOURCE:
			filterHint = UIText.GitHistoryPage_AllChangesOfResourceHint;
			break;
		}
		return NLS.bind(DESCRIPTION_PATTERN, getName(), filterHint);
	}

	public String getName() {
		return this.name;
	}

	/**
	 * @return the internal input object, or <code>null</code>
	 */
	public HistoryPageInput getInputInternal() {
		return this.input;
	}

	void showCommitList(final Job j, final SWTCommitList list,
			final SWTCommit[] asArray, final boolean incomplete) {
		if (trace)
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.HISTORYVIEW.getLocation(),
					new Object[] { list, asArray });
		if (job != j || graph.getControl().isDisposed())
			return;

		graph.getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (!graph.getControl().isDisposed() && job == j) {
					graph.setInput(highlightFlag, list, asArray, input);
					if (trace)
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.HISTORYVIEW.getLocation(),
								"Setting input to table"); //$NON-NLS-1$
					findToolbar.setInput(highlightFlag, graph.getTableView()
							.getTable(), asArray);
					if (incomplete)
						setWarningText(UIText.GitHistoryPage_ListIncompleteWarningMessage);
					else
						setWarningText(null);
					setErrorMessage(null);
				}
			}
		});
		if (trace)
			GitTraceLocation.getTrace().traceExit(
					GitTraceLocation.HISTORYVIEW.getLocation());
	}

	private void setWarningText(String warning) {
		if (warningComposite == null || warningComposite.isDisposed())
			return;
		GridData gd = (GridData) warningComposite.getLayoutData();
		gd.exclude = warning == null;
		warningComposite.setVisible(!gd.exclude);
		if (warning != null)
			warningText.setText(warning);
		else
			warningText.setText(""); //$NON-NLS-1$
		warningComposite.getParent().layout(true);
	}

	void initAndStartRevWalk(boolean forceNewWalk) throws IllegalStateException {
		try {
			if (trace)
				GitTraceLocation.getTrace().traceEntry(
						GitTraceLocation.HISTORYVIEW.getLocation());

			cancelRefreshJob();

			if (input == null)
				return;
			Repository db = input.getRepository();
			if (repoHasBeenRemoved(db)) {
				clearHistoryPage();
				return;
			}

			AnyObjectId headId = resolveHead(db, true);
			if (headId == null)
				return;

			List<String> paths = buildFilterPaths(input.getItems(), input
					.getFileList(), db);

			if (forceNewWalk || shouldRedraw(db, headId, paths)) {
				// TODO Do not dispose SWTWalk just because HEAD changed
				// In theory we should be able to update the graph and
				// not dispose of the SWTWalk, even if HEAD was reset to
				// HEAD^1 and the old HEAD commit should not be visible.
				//
				createNewWalk(db, headId);
				setWalkStartPoints(db, headId);

				setupFileViewer(db, paths);
				setupCommentViewer(db);

				scheduleNewGenerateHistoryJob();
			} else
				// needed for context menu and double click
				graph.setHistoryPageInput(input);
		} finally {
			if (trace)
				GitTraceLocation.getTrace().traceExit(
						GitTraceLocation.HISTORYVIEW.getLocation());

		}
	}

	private boolean shouldRedraw(Repository db, AnyObjectId headId, List<String> paths) {
		boolean pathChanged = pathChanged(pathFilters, paths);
		boolean headChanged = !headId.equals(currentHeadId);
		boolean repoChanged = false;

		boolean allBranchesChanged = currentShowAllBranches != store
			.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_ALL_BRANCHES);
		currentShowAllBranches = store
			.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_ALL_BRANCHES);

		if (!db.equals(currentRepo)) {
			repoChanged = true;
			currentRepo = db;
		}

		return pathChanged
			|| currentWalk == null || headChanged || repoChanged || allBranchesChanged;
	}

	private AnyObjectId resolveHead(Repository db, boolean acceptNull) {
		AnyObjectId headId;
		try {
			headId = db.resolve(Constants.HEAD);
		} catch (IOException e) {
			throw new IllegalStateException(NLS.bind(
					UIText.GitHistoryPage_errorParsingHead, Activator
							.getDefault().getRepositoryUtil()
							.getRepositoryName(db)), e);
		}
		if (headId == null && !acceptNull)
			throw new IllegalStateException(NLS.bind(
					UIText.GitHistoryPage_errorParsingHead, Activator
							.getDefault().getRepositoryUtil()
							.getRepositoryName(db)));
		return headId;
	}

	private ArrayList<String> buildFilterPaths(final IResource[] inResources,
			final File[] inFiles, final Repository db)
			throws IllegalStateException {
		final ArrayList<String> paths;
		if (inResources != null) {
			paths = new ArrayList<String>(inResources.length);
			for (final IResource r : inResources) {
				final RepositoryMapping map = RepositoryMapping.getMapping(r);
				if (map == null)
					continue;
				if (db != map.getRepository()) {
					throw new IllegalStateException(
							UIText.AbstractHistoryCommanndHandler_NoUniqueRepository);
				}

				if (showAllFilter == ShowFilter.SHOWALLFOLDER) {
					final String path;
					// if the resource's parent is the workspace root, we will
					// get nonsense from map.getRepoRelativePath(), so we
					// check here and use the project instead
					if (r.getParent() instanceof IWorkspaceRoot)
						path = map.getRepoRelativePath(r.getProject());
					else
						path = map.getRepoRelativePath(r.getParent());
					if (path != null && path.length() > 0)
						paths.add(path);
				} else if (showAllFilter == ShowFilter.SHOWALLPROJECT) {
					final String path = map.getRepoRelativePath(r.getProject());
					if (path != null && path.length() > 0)
						paths.add(path);
				} else if (showAllFilter == ShowFilter.SHOWALLREPO) {
					// nothing
				} else /* if (showAllFilter == ShowFilter.SHOWALLRESOURCE) */{
					final String path = map.getRepoRelativePath(r);
					if (path != null && path.length() > 0)
						paths.add(path);
				}
			}
		} else if (inFiles != null) {
			IPath workdirPath = new Path(db.getWorkTree().getPath());
			IPath gitDirPath = new Path(db.getDirectory().getPath());
			int segmentCount = workdirPath.segmentCount();
			paths = new ArrayList<String>(inFiles.length);
			for (File file : inFiles) {
				IPath filePath;
				if (showAllFilter == ShowFilter.SHOWALLFOLDER) {
					filePath = new Path(file.getParentFile().getPath());
				} else if (showAllFilter == ShowFilter.SHOWALLPROJECT
						|| showAllFilter == ShowFilter.SHOWALLREPO) {
					// we don't know of projects here -> treat as SHOWALLREPO
					continue;
				} else /* if (showAllFilter == ShowFilter.SHOWALLRESOURCE) */{
					filePath = new Path(file.getPath());
				}

				if (gitDirPath.isPrefixOf(filePath)) {
					throw new IllegalStateException(
							NLS
									.bind(
											UIText.GitHistoryPage_FileOrFolderPartOfGitDirMessage,
											filePath.toOSString()));
				}

				IPath pathToAdd = filePath.removeFirstSegments(segmentCount)
						.setDevice(null);
				if (!pathToAdd.isEmpty()) {
					paths.add(pathToAdd.toString());
				}
			}
		} else {
			paths = new ArrayList<String>(0);
		}
		return paths;
	}

	private boolean pathChanged(final List<String> o, final List<String> n) {
		if (o == null)
			return !n.isEmpty();
		return !o.equals(n);
	}

	private void createNewWalk(Repository db, AnyObjectId headId) {
		currentHeadId = headId;
		if (currentWalk != null)
			currentWalk.release();
		currentWalk = new SWTWalk(db);
		try {
			currentWalk.addAdditionalRefs(db.getRefDatabase().getAdditionalRefs());
		} catch (IOException e) {
			throw new IllegalStateException(NLS.bind(
					UIText.GitHistoryPage_errorReadingAdditionalRefs , Activator
							.getDefault().getRepositoryUtil()
							.getRepositoryName(db)), e);
		}
		currentWalk.sort(RevSort.COMMIT_TIME_DESC, true);
		currentWalk.sort(RevSort.BOUNDARY, true);
		highlightFlag = currentWalk.newFlag("highlight"); //$NON-NLS-1$
	}

	private void setWalkStartPoints(Repository db, AnyObjectId headId) {
		try {
			if (store
					.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_ALL_BRANCHES)) {
				markStartAllRefs(Constants.R_HEADS);
				markStartAllRefs(Constants.R_REMOTES);
				markStartAllRefs(Constants.R_TAGS);
				markStartAdditionalRefs();
			}
			currentWalk.markStart(currentWalk.parseCommit(headId));
		} catch (IOException e) {
			throw new IllegalStateException(NLS.bind(
					UIText.GitHistoryPage_errorSettingStartPoints, Activator
							.getDefault().getRepositoryUtil()
							.getRepositoryName(db)), e);
		}
	}

	private void setupCommentViewer(Repository db) {
		commentViewer.setRepository(db);
		commentViewer.refresh();
	}

	private TreeWalk setupFileViewer(Repository db, List<String> paths) {
		final TreeWalk fileWalker = createFileWalker(db, paths);
		fileViewer.setTreeWalk(db, fileWalker);
		fileViewer.refresh();
		fileViewer.addSelectionChangedListener(commentViewer);
		return fileWalker;
	}

	private TreeWalk createFileWalker(Repository db, List<String> paths) {
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
		return fileWalker;
	}

	private void scheduleNewGenerateHistoryJob() {
		final SWTCommitList list = new SWTCommitList(graph.getControl()
				.getDisplay());
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
		if (trace)
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.HISTORYVIEW.getLocation(),
					"Scheduling GenerateHistoryJob"); //$NON-NLS-1$
		schedule(rj);
	}

	private IWorkbenchPartSite getPartSite() {
		final IWorkbenchPart part = getHistoryPageSite().getPart();
		IWorkbenchPartSite site = null;
		if (part != null)
			site = part.getSite();
		return site;
	}

	private void schedule(final Job j) {
		IWorkbenchPartSite site = getPartSite();
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

	/**
	 * {@link RevWalk#markStart(RevCommit)} all refs with given prefix to mark
	 * start of graph traversal using currentWalker
	 *
	 * @param prefix
	 *            prefix of refs to be marked
	 * @throws IOException
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 */
	private void markStartAllRefs(String prefix) throws IOException,
			MissingObjectException, IncorrectObjectTypeException {
		for (Entry<String, Ref> refEntry : input.getRepository()
				.getRefDatabase().getRefs(prefix).entrySet()) {
			Ref ref = refEntry.getValue();
			if (ref.isSymbolic())
				continue;
			markStartRef(ref);
		}
	}

	private void markStartAdditionalRefs() throws IOException {
		List<Ref> additionalRefs = input.getRepository().getRefDatabase()
				.getAdditionalRefs();
		for (Ref ref : additionalRefs)
			markStartRef(ref);
	}

	private void markStartRef(Ref ref) throws MissingObjectException,
			IOException, IncorrectObjectTypeException {
		Object refTarget = currentWalk.parseAny(ref.getLeaf().getObjectId());
		if (refTarget instanceof RevCommit)
			currentWalk.markStart((RevCommit) refTarget);
	}

	private void cancelRefreshJob() {
		if (job != null && job.getState() != Job.NONE) {
			job.cancel();
			try {
				job.join();
			} catch (InterruptedException e) {
				cancelRefreshJob();
				return;
			}
			job = null;
		}
	}
}
