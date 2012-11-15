/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (c) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2010-2012, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2012, Daniel megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
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
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.AdapterUtils;
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
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
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
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.OrTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

/** Graphical commit history viewer. */
public class GitHistoryPage extends HistoryPage implements RefsChangedListener,
		ISchedulingRule, TableLoader, IShowInSource {

	private static final int INITIAL_ITEM = -1;

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
				if (historyPage.store.needsSaving())
					try {
						historyPage.store.save();
					} catch (IOException e) {
						Activator.handleError(e.getMessage(), e, false);
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
				if (!isChecked())
					if (historyPage.showAllFilter == filter) {
						historyPage.showAllFilter = ShowFilter.SHOWALLRESOURCE;
						showAllResourceVersionsAction.setChecked(true);
						historyPage.initAndStartRevWalk(false);
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

		BooleanPrefAction showEmailAddressesAction;

		BooleanPrefAction showNotesAction;

		BooleanPrefAction showTagSequenceAction;

		BooleanPrefAction wrapCommentAction;

		BooleanPrefAction fillCommentAction;

		IAction findAction;

		IAction refreshAction;

		BooleanPrefAction showCommentAction;

		BooleanPrefAction showFilesAction;

		IWorkbenchAction compareModeAction;

		IWorkbenchAction showAllBranchesAction;

		IWorkbenchAction showAdditionalRefsAction;

		BooleanPrefAction followRenamesAction;

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
			createShowAdditionalRefsAction();
			createShowCommentAction();
			createShowFilesAction();
			createShowRelativeDateAction();
			createShowEmailAddressesAction();
			createShowNotesAction();
			createShowTagSequenceAction();
			createWrapCommentAction();
			createFillCommentAction();
			createFollowRenamesAction();

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
					if (historyPage.store.needsSaving())
						try {
							historyPage.store.save();
						} catch (IOException e) {
							Activator.handleError(e.getMessage(), e, false);
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

		private void createShowAdditionalRefsAction() {
			showAdditionalRefsAction = new BooleanPrefAction(
					UIPreferences.RESOURCEHISTORY_SHOW_ADDITIONAL_REFS,
					UIText.GitHistoryPage_ShowAdditionalRefsMenuLabel) {

				@Override
				void apply(boolean value) {
					historyPage.refresh();
				}
			};
			actionsToDispose.add(showAdditionalRefsAction);
		}

		private void createFollowRenamesAction() {
			followRenamesAction = new BooleanPrefAction(
					UIPreferences.RESOURCEHISTORY_FOLLOW_RENAMES,
					UIText.GitHistoryPage_FollowRenames) {
				@Override
				void apply(boolean follow) {
					historyPage.refresh();
				}
			};
			followRenamesAction.apply(followRenamesAction.isChecked());
			actionsToDispose.add(followRenamesAction);
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

		private void createShowEmailAddressesAction() {
			showEmailAddressesAction = new BooleanPrefAction(
					UIPreferences.RESOURCEHISTORY_SHOW_EMAIL_ADDRESSES,
					UIText.GitHistoryPage_toggleEmailAddresses) {
				void apply(boolean date) {
					// nothing, just set the Preference
				}
			};
			showEmailAddressesAction.apply(showEmailAddressesAction.isChecked());
			actionsToDispose.add(showEmailAddressesAction);
		}

		private void createShowNotesAction() {
			showNotesAction = new BooleanPrefAction(
					UIPreferences.RESOURCEHISTORY_SHOW_NOTES,
					UIText.ResourceHistory_toggleShowNotes) {
				void apply(boolean value) {
					historyPage.refresh();
				}
			};
			showNotesAction.apply(showNotesAction.isChecked());
			actionsToDispose.add(showNotesAction);
		}

		private void createShowTagSequenceAction() {
			showTagSequenceAction = new BooleanPrefAction(
					UIPreferences.HISTORY_SHOW_TAG_SEQUENCE,
					UIText.ResourceHistory_ShowTagSequence) {
				void apply(boolean value) {
					// nothing, just set the Preference
				}
			};
			showTagSequenceAction.apply(showTagSequenceAction.isChecked());
			actionsToDispose.add(showTagSequenceAction);
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

	/**
	 * This class defines a couple that associates two pieces of information:
	 * the file path, and whether it is a regular file (or a directory).
	 */
	private static class FilterPath {

		private String path;

		private boolean regularFile;

		public FilterPath(String path, boolean regularFile) {
			super();
			this.path = path;
			this.regularFile = regularFile;
		}

		/** @return the file path */
		public String getPath() {
			return path;
		}

		/** @return <code>true</code> if the file is a regular file,
		 * 		and <code>false</code> otherwise (directory, project) */
		public boolean isRegularFile() {
			return regularFile;
		}

		/**
		 * In {@link FilterPath} class, equality is based on {@link #getPath
		 * path} equality.
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj == null || !(obj instanceof FilterPath))
				return false;
			FilterPath other = (FilterPath) obj;
			if (path == null)
				return other.path == null;
			return path.equals(other.path);
		}

		@Override
		public int hashCode() {
			if (path != null)
				return path.hashCode();
			return super.hashCode();
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder("Path: "); //$NON-NLS-1$
			builder.append(getPath());
			builder.append("regular: "); //$NON-NLS-1$
			builder.append(isRegularFile());

			return builder.toString();
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

	/** A label field to display a warning */
	private CLabel warningLabel;

	/** Our context menu manager for the entire page. */
	private final MenuManager popupMgr = new MenuManager(null, POPUP_ID);

	/** Job that is updating our history view, if we are refreshing. */
	private GenerateHistoryJob job;

	private final ResourceManager resources = new LocalResourceManager(
			JFaceResources.getResources());

	/** Last HEAD */
	private AnyObjectId currentHeadId;

	/** Repository of the last input*/
	private Repository currentRepo;

	private boolean currentShowAllBranches;

	private boolean currentShowAdditionalRefs;

	private boolean currentShowNotes;

	private boolean currentFollowRenames;

	// react on changes to the relative date preference
	private final IPropertyChangeListener listener = new IPropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent event) {
			final String prop = event.getProperty();
			if (UIPreferences.RESOURCEHISTORY_SHOW_RELATIVE_DATE.equals(prop)) {
				Object oldValue = event.getOldValue();
				if (oldValue == null || !oldValue.equals(event.getNewValue())) {
					graph.setRelativeDate(isShowingRelativeDates());
					graph.getTableView().refresh();
				}
			}
			if (UIPreferences.RESOURCEHISTORY_SHOW_EMAIL_ADDRESSES.equals(prop)) {
				Object oldValue = event.getOldValue();
				if (oldValue == null || !oldValue.equals(event.getNewValue())) {
					graph.setShowEmailAddresses(isShowingEmailAddresses());
					graph.getTableView().refresh();
				}
			}
			if (UIPreferences.HISTORY_MAX_BRANCH_LENGTH.equals(prop)
					|| UIPreferences.HISTORY_MAX_TAG_LENGTH.equals(prop))
				graph.getTableView().refresh();
		}
	};


	/**
	 * List of paths we used to limit the revwalk; null if no paths.
	 * <p>
	 * Note that a change in this list requires that the history is redrawn
	 */
	private List<FilterPath> pathFilters;

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
		if (object instanceof HistoryPageInput)
			return true;

		if (object instanceof IResource)
			return typeOk((IResource) object);

		if (object instanceof RepositoryTreeNode)
			return true;

		IResource resource = AdapterUtils.adapt(object, IResource.class);
		if (resource != null && typeOk(resource))
			return true;

		return AdapterUtils.adapt(object, Repository.class) != null;
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
		warningComposite.setLayout(new GridLayout(2, false));
		warningComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING,
				true, false));
		warningLabel = new CLabel(warningComposite, SWT.NONE);
		warningLabel.setImage(PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJS_WARN_TSK));
		warningLabel
				.setToolTipText(UIText.GitHistoryPage_IncompleteListTooltip);

		Link preferencesLink = new Link(warningComposite, SWT.NONE);
		preferencesLink.setText(UIText.GitHistoryPage_PreferencesLink);
		preferencesLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String preferencePageId = "org.eclipse.egit.ui.internal.preferences.HistoryPreferencePage"; //$NON-NLS-1$
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
		graph = new CommitGraphTable(graphDetailSplit, getSite(), popupMgr,
				this, resources);

		graph.setRelativeDate(isShowingRelativeDates());
		graph.setShowEmailAddresses(isShowingEmailAddresses());
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

		getSite().setSelectionProvider(graph.getTableView());
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
		} else if (!showComment && !showFiles)
			graphDetailSplit.setMaximizedControl(graph.getControl());
		if (showFindToolbar)
			((GridData) findToolbar.getLayoutData()).heightHint = SWT.DEFAULT;
		else {
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
				final PlotWalk walk = new PlotWalk(input.getRepository());
				try {
					final RevCommit unfilteredCommit = walk.parseCommit(c);
					for (RevCommit parent : unfilteredCommit.getParents())
						walk.parseBody(parent);
					fileViewer.setInput(unfilteredCommit);
				} catch (IOException e) {
					fileViewer.setInput(c);
				} finally {
					walk.dispose();
				}
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
		showSubMenuMgr.add(actions.showAdditionalRefsAction);
		showSubMenuMgr.add(actions.showNotesAction);
		showSubMenuMgr.add(actions.followRenamesAction);
		showSubMenuMgr.add(new Separator());
		showSubMenuMgr.add(actions.findAction);
		showSubMenuMgr.add(actions.showCommentAction);
		showSubMenuMgr.add(actions.showFilesAction);
		showSubMenuMgr.add(new Separator());
		showSubMenuMgr.add(actions.showRelativeDateAction);
		showSubMenuMgr.add(actions.showEmailAddressesAction);

		IMenuManager showInMessageManager = new MenuManager(
				UIText.GitHistoryPage_InRevisionCommentSubMenuLabel);
		showSubMenuMgr.add(showInMessageManager);
		showInMessageManager.add(actions.showTagSequenceAction);
		showInMessageManager.add(actions.wrapCommentAction);
		showInMessageManager.add(actions.fillCommentAction);

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
	}

	public void dispose() {
		trace = GitTraceLocation.HISTORYVIEW.isActive();
		if (trace)
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.HISTORYVIEW.getLocation());

		Activator.getDefault().getPreferenceStore()
				.removePropertyChangeListener(listener);

		if (myRefsChangedHandle != null) {
			myRefsChangedHandle.remove();
			myRefsChangedHandle = null;
		}

		resources.dispose();

		// dispose of the actions (the history framework doesn't do this for us)
		for (IWorkbenchAction action : actions.actionsToDispose)
			action.dispose();
		actions.actionsToDispose.clear();
		releaseGenerateHistoryJob();
		if (popupMgr != null) {
			for (final IContributionItem i : popupMgr.getItems())
				if (i instanceof IWorkbenchAction)
					((IWorkbenchAction) i).dispose();
			for (final IContributionItem i : getSite().getActionBars()
					.getMenuManager().getItems())
				if (i instanceof IWorkbenchAction)
					((IWorkbenchAction) i).dispose();
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

			Object o = super.getInput();
			if (o == null) {
				setErrorMessage(UIText.GitHistoryPage_NoInputMessage);
				return false;
			}

			boolean showHead = false;
			boolean showRef = false;
			boolean showTag = false;
			Repository repo = null;
			RevCommit selection = null;
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
			if (repo == null) {
				repo = AdapterUtils.adapt(o, Repository.class);
				if (repo != null)
					input = new HistoryPageInput(repo);
			}
			selection = AdapterUtils.adapt(o, RevCommit.class);

			if (input == null) {
				this.name = ""; //$NON-NLS-1$
				setErrorMessage(UIText.GitHistoryPage_NoInputMessage);
				return false;
			}

			final IResource[] inResources = input.getItems();
			final File[] inFiles = input.getFileList();

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
			if (selection != null)
				graph.selectCommitStored(selection);

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
			if (any instanceof RevCommit)
				c = (RevCommit) any;
			else if (any instanceof RevTag) {
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
		if (in.getItems() == null && in.getFileList() == null)
			// plain repository, no files specified
			return NLS.bind(UIText.GitHistoryPage_RepositoryNamePattern,
					repositoryName);
		else if (in.getItems() != null && in.getItems().length == 1) {
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

	@SuppressWarnings("boxing")
	void showCommitList(final Job j, final SWTCommitList list,
			final SWTCommit[] asArray, final RevCommit toSelect, final boolean incomplete, final RevFlag highlightFlag) {
		if (trace)
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.HISTORYVIEW.getLocation(),
					new Object[] { list.size()});
		if (job != j || graph.getControl().isDisposed())
			return;

		graph.getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (!graph.getControl().isDisposed() && job == j) {
					graph.setInput(highlightFlag, list, asArray, input, true);
					if (toSelect != null)
						graph.selectCommit(toSelect);
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
			warningLabel.setText(warning);
		else
			warningLabel.setText(""); //$NON-NLS-1$
		warningComposite.getParent().layout(true);
	}

	void initAndStartRevWalk(boolean forceNewWalk) throws IllegalStateException {
		try {
			if (trace)
				GitTraceLocation.getTrace().traceEntry(
						GitTraceLocation.HISTORYVIEW.getLocation());

			if (input == null)
				return;
			Repository db = input.getRepository();
			if (repoHasBeenRemoved(db)) {
				clearHistoryPage();
				return;
			}

			AnyObjectId headId = resolveHead(db, true);
			if (headId == null) {
				graph.getTableView().setInput(new SWTCommit[0]);
				currentHeadId = null;
				return;
			}

			List<FilterPath> paths = buildFilterPaths(input.getItems(), input
					.getFileList(), db);

			if (forceNewWalk || shouldRedraw(db, headId, paths)) {
				releaseGenerateHistoryJob();

				SWTWalk walk = createNewWalk(db, headId);
				setWalkStartPoints(walk, db, headId);

				setupFileViewer(walk, db, paths);
				setupCommentViewer(db);

				loadHistory(INITIAL_ITEM, walk);
			} else
				// needed for context menu and double click
				graph.setHistoryPageInput(input);
		} finally {
			if (trace)
				GitTraceLocation.getTrace().traceExit(
						GitTraceLocation.HISTORYVIEW.getLocation());

		}
	}

	private boolean shouldRedraw(Repository db, AnyObjectId headId, List<FilterPath> paths) {
		boolean pathChanged = pathChanged(pathFilters, paths);
		boolean headChanged = headId == null || !headId.equals(currentHeadId);
		boolean repoChanged = false;

		boolean allBranchesChanged = currentShowAllBranches != store
			.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_ALL_BRANCHES);
		currentShowAllBranches = store
			.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_ALL_BRANCHES);

		boolean additionalRefsChange = currentShowAdditionalRefs != store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_ADDITIONAL_REFS);
		currentShowAdditionalRefs = store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_ADDITIONAL_REFS);

		boolean showNotesChanged = currentShowNotes != store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_NOTES);
		currentShowNotes = store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_NOTES);
		boolean followRenamesChanged = currentFollowRenames != getFollowRenames();
		currentFollowRenames = getFollowRenames();

		if (!db.equals(currentRepo)) {
			repoChanged = true;
			currentRepo = db;
		}

		return pathChanged
			|| headChanged || repoChanged || allBranchesChanged
			|| additionalRefsChange || showNotesChanged || followRenamesChanged;
	}

	/**
	 * @return whether following renames is currently enabled
	 */
	protected boolean getFollowRenames() {
		return store.getBoolean(UIPreferences.RESOURCEHISTORY_FOLLOW_RENAMES);
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

	private ArrayList<FilterPath> buildFilterPaths(final IResource[] inResources,
			final File[] inFiles, final Repository db)
			throws IllegalStateException {
		final ArrayList<FilterPath> paths;
		if (inResources != null) {
			paths = new ArrayList<FilterPath>(inResources.length);
			for (final IResource r : inResources) {
				final RepositoryMapping map = RepositoryMapping.getMapping(r);
				if (map == null)
					continue;
				if (db != map.getRepository())
					throw new IllegalStateException(
							UIText.AbstractHistoryCommanndHandler_NoUniqueRepository);

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
						paths.add(new FilterPath(path, false));
				} else if (showAllFilter == ShowFilter.SHOWALLPROJECT) {
					final String path = map.getRepoRelativePath(r.getProject());
					if (path != null && path.length() > 0)
						paths.add(new FilterPath(path, false));
				} else if (showAllFilter == ShowFilter.SHOWALLREPO) {
					// nothing
				} else /* if (showAllFilter == ShowFilter.SHOWALLRESOURCE) */{
					final String path = map.getRepoRelativePath(r);
					if (path != null && path.length() > 0)
						paths.add(new FilterPath(path, r.getType() == IResource.FILE));
				}
			}
		} else if (inFiles != null) {
			IPath workdirPath = new Path(db.getWorkTree().getPath());
			IPath gitDirPath = new Path(db.getDirectory().getPath());
			int segmentCount = workdirPath.segmentCount();
			paths = new ArrayList<FilterPath>(inFiles.length);
			for (File file : inFiles) {
				IPath filePath;
				boolean isRegularFile;
				if (showAllFilter == ShowFilter.SHOWALLFOLDER) {
					filePath = new Path(file.getParentFile().getPath());
					isRegularFile = false;
				} else if (showAllFilter == ShowFilter.SHOWALLPROJECT
						|| showAllFilter == ShowFilter.SHOWALLREPO)
					// we don't know of projects here -> treat as SHOWALLREPO
					continue;
				else /* if (showAllFilter == ShowFilter.SHOWALLRESOURCE) */{
					filePath = new Path(file.getPath());
					isRegularFile = file.isFile();
				}

				if (gitDirPath.isPrefixOf(filePath))
					throw new IllegalStateException(
							NLS
									.bind(
											UIText.GitHistoryPage_FileOrFolderPartOfGitDirMessage,
											filePath.toOSString()));

				IPath pathToAdd = filePath.removeFirstSegments(segmentCount)
						.setDevice(null);
				if (!pathToAdd.isEmpty())
					paths.add(new FilterPath(pathToAdd.toString(), isRegularFile));
			}
		} else
			paths = new ArrayList<FilterPath>(0);
		return paths;
	}

	private boolean pathChanged(final List<FilterPath> o, final List<FilterPath> n) {
		if (o == null)
			return !n.isEmpty();
		return !o.equals(n);
	}

	private SWTWalk createNewWalk(Repository db, AnyObjectId headId) {
		currentHeadId = headId;
		SWTWalk walk = new SWTWalk(db);
		try {
			if (store
					.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_ADDITIONAL_REFS))
				walk.addAdditionalRefs(db.getRefDatabase()
						.getAdditionalRefs());
			walk.addAdditionalRefs(db.getRefDatabase()
					.getRefs(Constants.R_NOTES).values());
		} catch (IOException e) {
			throw new IllegalStateException(NLS.bind(
					UIText.GitHistoryPage_errorReadingAdditionalRefs, Activator
							.getDefault().getRepositoryUtil()
							.getRepositoryName(db)), e);
		}
		walk.sort(RevSort.COMMIT_TIME_DESC, true);
		walk.sort(RevSort.BOUNDARY, true);
		walk.setRetainBody(false);
		return walk;
	}

	private void setWalkStartPoints(RevWalk walk, Repository db, AnyObjectId headId) {
		try {
			if (store
					.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_ALL_BRANCHES)) {
				markStartAllRefs(walk, Constants.R_HEADS);
				markStartAllRefs(walk, Constants.R_REMOTES);
				markStartAllRefs(walk, Constants.R_TAGS);
			}
			if (store
					.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_ADDITIONAL_REFS))
				markStartAdditionalRefs(walk);
			if (store
					.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_NOTES))
				markStartAllRefs(walk, Constants.R_NOTES);
			else
				markUninteresting(walk, Constants.R_NOTES);

			walk.markStart(walk.parseCommit(headId));
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

	private TreeWalk setupFileViewer(RevWalk walk, Repository db, List<FilterPath> paths) {
		final TreeWalk fileWalker = createFileWalker(walk, db, paths);
		fileViewer.setTreeWalk(db, fileWalker);
		fileViewer.refresh();
		fileViewer.addSelectionChangedListener(commentViewer);
		return fileWalker;
	}

	private TreeWalk createFileWalker(RevWalk walk, Repository db, List<FilterPath> paths) {
		final TreeWalk fileWalker = new TreeWalk(db);
		fileWalker.setRecursive(true);
		fileWalker.setFilter(TreeFilter.ANY_DIFF);
		if (store.getBoolean(UIPreferences.RESOURCEHISTORY_FOLLOW_RENAMES)
				&& !paths.isEmpty()
				&& allRegularFiles(paths)) {
			pathFilters = paths;

			List<String> selectedPaths = new ArrayList<String>(paths.size());
			for (FilterPath filterPath : paths)
				selectedPaths.add(filterPath.getPath());

			TreeFilter followFilter = createFollowFilterFor(selectedPaths);
			walk.setTreeFilter(followFilter);
		} else if (paths.size() > 0) {
			pathFilters = paths;
			List<String> stringPaths = new ArrayList<String>(paths.size());
			for (FilterPath p : paths)
				stringPaths.add(p.getPath());

			walk.setTreeFilter(AndTreeFilter.create(PathFilterGroup
					.createFromStrings(stringPaths), TreeFilter.ANY_DIFF));
		} else {
			pathFilters = null;
			walk.setTreeFilter(TreeFilter.ALL);
		}
		return fileWalker;
	}

	/**
	 * Creates a filter for the given files, will make sure that renames/copies
	 * of all files will be followed.
	 * @param paths the list of files to follow, must not be <code>null</code> or empty
	 * @return the ORed list of {@link FollowFilter follow filters}
	 */
	private TreeFilter createFollowFilterFor(List<String> paths) {
		if (paths == null || paths.isEmpty())
			throw new IllegalArgumentException("paths must not be null nor empty"); //$NON-NLS-1$

		List<TreeFilter> followFilters = new ArrayList<TreeFilter>(paths.size());
		for (String path : paths)
			followFilters.add(FollowFilter.create(path));

		if (followFilters.size() == 1)
			return followFilters.get(0);

		return OrTreeFilter.create(followFilters);
	}

	/**
	 * @return Returns <code>true</code> if <b>all</b> filterpaths refer to plain files,
	 * 			or if the list is empty.
	 * @param paths the paths to check
	 */
	private boolean allRegularFiles(List<FilterPath> paths) {
		for (FilterPath filterPath : paths)
			if (!filterPath.isRegularFile())
				return false;
		return true;
	}

	public void loadItem(int item) {
		if (job == null || job.loadMoreItemsThreshold() < item)
			loadHistory(item, null);
	}

	public void loadCommit(RevCommit c) {
		if (job == null)
			return;
		job.setLoadHint(c);
		if (trace)
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.HISTORYVIEW.getLocation(),
					"Scheduling GenerateHistoryJob"); //$NON-NLS-1$
		schedule(job);
	}

	/**
	 * Load history items incrementally
	 * @param itemToLoad hint for index of item that should be loaded
	 * @param walk the revwalk, used only if itemToLoad ==  INITIAL_ITEM
	 */
	private void loadHistory(final int itemToLoad, RevWalk walk) {
		if (itemToLoad == INITIAL_ITEM) {
			job = new GenerateHistoryJob(this, graph.getControl(), walk,
					resources);
			job.setRule(this);
		}
		job.setLoadHint(itemToLoad);
		if (trace)
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.HISTORYVIEW.getLocation(),
					"Scheduling GenerateHistoryJob"); //$NON-NLS-1$
		schedule(job);
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
	 * @param walk the walk to be configured
	 *
	 * @param prefix
	 *            prefix of refs to be marked
	 * @throws IOException
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 */
	private void markStartAllRefs(RevWalk walk, String prefix) throws IOException,
			MissingObjectException, IncorrectObjectTypeException {
		for (Entry<String, Ref> refEntry : input.getRepository()
				.getRefDatabase().getRefs(prefix).entrySet()) {
			Ref ref = refEntry.getValue();
			if (ref.isSymbolic())
				continue;
			markStartRef(walk, ref);
		}
	}

	private void markStartAdditionalRefs(RevWalk walk) throws IOException {
		List<Ref> additionalRefs = input.getRepository().getRefDatabase()
				.getAdditionalRefs();
		for (Ref ref : additionalRefs)
			markStartRef(walk, ref);
	}

	private void markStartRef(RevWalk walk, Ref ref) throws IOException,
			IncorrectObjectTypeException {
		try {
			Object refTarget = walk.parseAny(ref.getLeaf().getObjectId());
			if (refTarget instanceof RevCommit)
				walk.markStart((RevCommit) refTarget);
		} catch (MissingObjectException e) {
			// If there is a ref which points to Nirvana then we should simply
			// ignore this ref. We should not let a corrupt ref cause that the
			// history view is not filled at all
		}
	}

	private void markUninteresting(RevWalk walk, String prefix) throws IOException,
			MissingObjectException, IncorrectObjectTypeException {
		for (Entry<String, Ref> refEntry : input.getRepository()
				.getRefDatabase().getRefs(prefix).entrySet()) {
			Ref ref = refEntry.getValue();
			if (ref.isSymbolic())
				continue;
			Object refTarget = walk
					.parseAny(ref.getLeaf().getObjectId());
			if (refTarget instanceof RevCommit)
				walk.markUninteresting((RevCommit) refTarget);
		}
	}

	private void releaseGenerateHistoryJob() {
		if (job != null) {
			if (job.getState() != Job.NONE)
				job.cancel();
			job.release();
			job = null;
		}
	}

	private boolean isShowingRelativeDates() {
		return Activator.getDefault().getPreferenceStore().getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_RELATIVE_DATE);
	}

	private boolean isShowingEmailAddresses() {
		return Activator.getDefault().getPreferenceStore().getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_EMAIL_ADDRESSES);
	}

	public boolean contains(ISchedulingRule rule) {
		return this == rule;
	}

	public boolean isConflicting(ISchedulingRule rule) {
		return this == rule;
	}

	public ShowInContext getShowInContext() {
		if (fileViewer != null && fileViewer.getControl().isFocusControl())
			return fileViewer.getShowInContext();
		else
			return null;
	}
}
