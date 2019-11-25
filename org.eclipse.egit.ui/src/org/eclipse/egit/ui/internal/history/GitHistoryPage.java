/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (c) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2010-2012, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2012, Daniel megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2012-2013 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2012, François Rey <eclipse.org_@_francois_._rey_._name>
 * Copyright (C) 2015, IBM Corporation (Dani Megert <daniel_megert@ch.ibm.com>)
 * Copyright (C) 2015-2019 Thomas Wolf <thomas.wolf@paranor.ch>
 * Copyright (C) 2015-2017, Stefan Dirix <sdirix@eclipsesource.com>
 * Copyright (C) 2019, Tim Neumann <Tim.Neumann@advantest.com>
 * Copyright (C) 2019, Simon Muschel <smuschel@gmx.de>
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
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.UnitOfWork;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.DiffDocument;
import org.eclipse.egit.ui.internal.commit.DiffRegionFormatter;
import org.eclipse.egit.ui.internal.commit.DiffViewer;
import org.eclipse.egit.ui.internal.commit.FocusTracker;
import org.eclipse.egit.ui.internal.components.DropDownMenuAction;
import org.eclipse.egit.ui.internal.components.RepositoryMenuUtil.RepositoryToolbarAction;
import org.eclipse.egit.ui.internal.fetch.FetchHeadChangedEvent;
import org.eclipse.egit.ui.internal.history.RefFilterHelper.RefFilter;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefNode;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.selection.RepositorySelectionProvider;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.SubContributionItem;
import org.eclipse.jface.action.SubToolBarManager;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.diff.DiffConfig;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RenameCallback;
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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.IUpdate;

/** Graphical commit history viewer. */
public class GitHistoryPage extends HistoryPage implements RefsChangedListener,
		TableLoader, IShowInSource, IShowInTargetList {

	private static final int INITIAL_ITEM = -1;

	private static final String P_REPOSITORY = "GitHistoryPage.Repository"; //$NON-NLS-1$

	private static final EnumSet SUPPORTED_REPOSITORY_NODE_TYPES = EnumSet.of(
			RepositoryTreeNodeType.REPO, RepositoryTreeNodeType.REF,
			RepositoryTreeNodeType.ADDITIONALREF, RepositoryTreeNodeType.TAG,
			RepositoryTreeNodeType.FOLDER, RepositoryTreeNodeType.FILE,
			RepositoryTreeNodeType.WORKINGDIR);

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

			@Override
			public void run() {
				historyPage.store.setValue(prefName, isChecked());
				historyPage.saveStoreIfNeeded();
			}

			abstract void apply(boolean value);

			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				if (prefName.equals(event.getProperty())) {
					Control control = historyPage.getControl();
					if (control != null && !control.isDisposed()) {
						control.getDisplay().asyncExec(() -> {
							if (!control.isDisposed()) {
								setChecked(
										historyPage.store.getBoolean(prefName));
								apply(isChecked());
							}
						});
					}
				}
			}

			@Override
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

		private static class FilterAction extends DropDownMenuAction {

			private final @NonNull List<IAction> actions;

			private final IPropertyChangeListener listener;

			private boolean childEnablement = true;

			public FilterAction(IAction... actions) {
				super(UIText.GitHistoryPage_FilterSubMenuLabel);
				@SuppressWarnings("null")
				@NonNull
				List<IAction> a = actions == null ? Collections.emptyList()
						: Arrays.asList(actions);
				this.actions = a;
				setToolTipText(UIText.GitHistoryPage_FilterTooltip);
				listener = e -> {
					if (IAction.ENABLED.equals(e.getProperty())) {
						boolean previousEnablement = isEnabled();
						childEnablement = FilterAction.this.actions.stream()
								.anyMatch(act -> act.isEnabled());
						boolean currentEnablement = isEnabled();
						if (currentEnablement != previousEnablement) {
							IAction currentChild = currentEnablement
									? FilterAction.this.actions.stream()
											.filter(act -> act.isChecked())
											.findFirst().orElse(null)
									: null;
							if (currentChild == null) {
								setToolTipText(
										UIText.GitHistoryPage_FilterTooltip);
							} else {
								setToolTipText(MessageFormat.format(
										UIText.GitHistoryPage_FilterTooltipCurrent,
										currentChild.getToolTipText()));
							}
							firePropertyChange(IAction.ENABLED,
									Boolean.valueOf(previousEnablement),
									Boolean.valueOf(currentEnablement));
						}
					} else if (IAction.CHECKED.equals(e.getProperty())) {
						Object newValue = e.getNewValue();
						boolean isChecked = false;
						if (newValue instanceof Boolean) {
							isChecked = ((Boolean) newValue).booleanValue();
						} else if (newValue instanceof String) {
							isChecked = Boolean.parseBoolean((String) newValue);
						}
						if (isChecked) {
							Object source = e.getSource();
							if (source instanceof IAction) {
								if (isEnabled()) {
									setToolTipText(MessageFormat.format(
											UIText.GitHistoryPage_FilterTooltipCurrent,
											((IAction) source)
													.getToolTipText()));
								}
								ImageDescriptor image = ((IAction) source)
										.getImageDescriptor();
								if (image != null) {
									setImageDescriptor(image);
								}
							}
						}
					}
				};
				for (IAction action : this.actions) {
					action.addPropertyChangeListener(listener);
				}
			}

			@Override
			protected Collection<IContributionItem> getActions() {
				return actions.stream().map(ActionContributionItem::new)
						.collect(Collectors.toList());
			}

			@Override
			public boolean isEnabled() {
				return super.isEnabled() && childEnablement;
			}

			@Override
			public void dispose() {
				for (IAction action : this.actions) {
					action.removePropertyChangeListener(listener);
				}
				super.dispose();
			}

			@Override
			public void run() {
				if (!isEnabled()) {
					return;
				}
				// Cycle through the available actions
				int i = 1;
				for (IAction action : actions) {
					if (action.isChecked()) {
						IAction next = actions.get(i % actions.size());
						while (next != action) {
							if (next.isEnabled()) {
								action.setChecked(false);
								next.setChecked(true);
								next.run();
								break;
							}
							i++;
							next = actions.get(i % actions.size());
						}
						return;
					}
					i++;
				}
			}
		}

		List<IWorkbenchAction> actionsToDispose;

		BooleanPrefAction showRelativeDateAction;

		BooleanPrefAction showEmailAddressesAction;

		BooleanPrefAction showNotesAction;

		BooleanPrefAction showTagSequenceAction;

		BooleanPrefAction showBranchSequenceAction;

		BooleanPrefAction wrapCommentAction;

		BooleanPrefAction fillCommentAction;

		IAction findAction;

		IAction refreshAction;

		BooleanPrefAction showCommentAction;

		BooleanPrefAction showFilesAction;

		IWorkbenchAction compareModeAction;

		IWorkbenchAction selectShownRefsAction;

		IWorkbenchAction showFirstParentOnlyAction;

		IWorkbenchAction showAdditionalRefsAction;

		BooleanPrefAction followRenamesAction;

		IWorkbenchAction reuseCompareEditorAction;

		ShowFilterAction showAllRepoVersionsAction;

		ShowFilterAction showAllProjectVersionsAction;

		ShowFilterAction showAllFolderVersionsAction;

		ShowFilterAction showAllResourceVersionsAction;

		FilterAction filterAction;

		RepositoryToolbarAction switchRepositoryAction;

		IWorkbenchAction configureFiltersAction;

		private GitHistoryPage historyPage;

		GitHistoryPageActions(GitHistoryPage historyPage) {
			actionsToDispose = new ArrayList<>();
			this.historyPage = historyPage;
			createActions();
		}

		private static String formatAccelerator(int accelerator) {
			return SWTKeySupport.getKeyFormatterForPlatform().format(
					SWTKeySupport.convertAcceleratorToKeyStroke(accelerator));
		}

		private void createActions() {
			createRepositorySwitchAction();
			createFindToolbarAction();
			createRefreshAction();
			createFilterActions();
			createCompareModeAction();
			createReuseCompareEditorAction();
			createSelectShownRefsAction();
			createShowFirstParentOnlyAction();
			createShowAdditionalRefsAction();
			createShowCommentAction();
			createShowFilesAction();
			createShowRelativeDateAction();
			createShowEmailAddressesAction();
			createShowNotesAction();
			createShowTagSequenceAction();
			createShowBranchSequenceAction();
			createWrapCommentAction();
			createFillCommentAction();
			createFollowRenamesAction();
			createConfigureFiltersAction();

			wrapCommentAction.setEnabled(showCommentAction.isChecked());
			fillCommentAction.setEnabled(showCommentAction.isChecked());
		}

		private void createRepositorySwitchAction() {
			switchRepositoryAction = new RepositoryToolbarAction(true,
					() -> historyPage.getCurrentRepo(),
					repo -> {
						Repository current = historyPage.getCurrentRepo();
						if (current != null && repo.getDirectory()
								.equals(current.getDirectory())) {
							HistoryPageInput currentInput = historyPage
									.getInputInternal();
							if (currentInput != null
									&& currentInput.getItems() == null
									&& currentInput.getFileList() == null) {
								// Already showing this repo unfiltered
								return;
							}
						}
						if (historyPage.selectionTracker != null) {
							historyPage.selectionTracker.clearSelection();
						}
						historyPage.getHistoryView()
								.showHistoryFor(new RepositoryNode(null, repo));
					});
			actionsToDispose.add(switchRepositoryAction);
		}

		private void createFindToolbarAction() {
			findAction = new Action(UIText.GitHistoryPage_FindMenuLabel,
					UIIcons.ELCL16_FIND) {

				@Override
				public void run() {
					historyPage.store.setValue(
							UIPreferences.RESOURCEHISTORY_SHOW_FINDTOOLBAR,
							isChecked());
					historyPage.saveStoreIfNeeded();
					historyPage.searchBar.setVisible(isChecked());
				}

				@Override
				public void setChecked(boolean checked) {
					super.setChecked(checked);
					int accelerator = getAccelerator();
					if (checked) {
						setToolTipText(
								NLS.bind(UIText.GitHistoryPage_FindHideTooltip,
										formatAccelerator(accelerator)));
					} else {
						setToolTipText(
								NLS.bind(UIText.GitHistoryPage_FindShowTooltip,
										formatAccelerator(accelerator)));
					}
				}

			};
			// TODO: how not to hard-wire this?
			findAction.setAccelerator(SWT.MOD1 | 'F');
			findAction.setEnabled(false);
			// Gets enabled once we have commits
			boolean isChecked = historyPage.store
					.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_FINDTOOLBAR);
			findAction.setChecked(isChecked);
			historyPage.getSite().getActionBars().setGlobalActionHandler(
					ActionFactory.FIND.getId(), findAction);
			historyPage.getSite().getActionBars().updateActionBars();
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
					ShowFilter.SHOWALLREPO, UIIcons.FILTERNONE,
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

			filterAction = new FilterAction(showAllRepoVersionsAction,
					showAllProjectVersionsAction, showAllFolderVersionsAction,
					showAllResourceVersionsAction);

			showAllRepoVersionsAction
					.setChecked(historyPage.showAllFilter == showAllRepoVersionsAction.filter);
			showAllProjectVersionsAction
					.setChecked(historyPage.showAllFilter == showAllProjectVersionsAction.filter);
			showAllFolderVersionsAction
					.setChecked(historyPage.showAllFilter == showAllFolderVersionsAction.filter);
			showAllResourceVersionsAction
					.setChecked(historyPage.showAllFilter == showAllResourceVersionsAction.filter);
			actionsToDispose.add(filterAction);
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

		private class SelectShownRefsAction extends DropDownMenuAction
				implements IPropertyChangeListener {

			private boolean headMode;

			private RefFilterHelper helper;

			public SelectShownRefsAction() {
				super(UIText.GitHistoryPage_SelectShownRefsMenuLabel);
				historyPage.addPropertyChangeListener(this);

				Repository currentRepo = historyPage.getCurrentRepo();

				if (currentRepo != null) {
					helper = new RefFilterHelper(currentRepo);
				}

				setHeadModeFromHelperState();
				updateUiForMode();
			}

			private void setHeadModeFromHelperState() {
				if (helper == null) {
					return;
				}
				Set<RefFilter> filters = helper.getRefFilters();

				headMode = helper.isOnlyHEADSelected(filters);
			}

			private void updateUiForMode() {
				if (headMode) {
					this.setImageDescriptor(UIIcons.BRANCH);
					this.setToolTipText(
							UIText.GitHistoryPage_showingHistoryOfHead);
				} else {
					this.setImageDescriptor(UIIcons.BRANCHES);
					this.setToolTipText(
							UIText.GitHistoryPage_showingHistoryOfConfiguredFilters);
				}
			}

			@Override
			public void dispose() {
				historyPage.removePropertyChangeListener(this);
				super.dispose();
			}

			@Override
			protected Collection<IContributionItem> getActions() {
				if (historyPage.getCurrentRepo() == null) {
					return new ArrayList<>();
				}
				List<IContributionItem> actions = new ArrayList<>();
				actions.add(new ActionContributionItem(configureFiltersAction));
				actions.add(new Separator());
				Set<RefFilter> filters = helper.getRefFilters();
				List<RefFilter> sortedFilters = new ArrayList<>(
						filters);
				sortedFilters
						.sort(Comparator
								.comparing(RefFilter::isPreconfigured,
										Comparator.reverseOrder())
								.thenComparing(RefFilter::getFilterString,
										String.CASE_INSENSITIVE_ORDER));

				boolean separated = false;
				for (RefFilter filter : sortedFilters) {
					Action action = new ShownRefAction(filter, () -> {
						helper.setRefFilters(filters);
						setHeadModeFromHelperState();
						updateUiForMode();
						historyPage.refresh();
					});
					if (!separated && !filter.isPreconfigured()) {
						actions.add(new Separator());
						separated = true;
					}
					actions.add(new ActionContributionItem(action));
				}
				return actions;
			}

			@Override
			public void run() {
				if (historyPage.getCurrentRepo() == null)
					return;
				Set<RefFilter> filters = helper.getRefFilters();

				if (helper.isOnlyHEADSelected(filters)) {
					helper.restoreLastSelectionState(filters);
					headMode = false;
				} else {
					helper.saveSelectionStateAsLastSelectionState(filters);
					helper.selectOnlyHEAD(filters);
					headMode = true;
				}
				updateUiForMode();
				helper.setRefFilters(filters);
				historyPage.refresh(historyPage.selectedCommit());
			}

			private class ShownRefAction extends Action {

				private RefFilter filter;

				private Runnable postChangeAction;

				public ShownRefAction(RefFilter filter,
						Runnable postChangeAction) {
					super(filter.getFilterString(), IAction.AS_CHECK_BOX);
					if (filter.isPreconfigured()) {
						this.setText(filter.getFilterString()
								+ UIText.GitHistoryPage_filterRefDialog_preconfiguredText);
					}
					this.filter = filter;
					this.postChangeAction = postChangeAction;
				}

				@Override
				public boolean isChecked() {
					return filter.isSelected();
				}

				@Override
				public void run() {
					if (historyPage.getCurrentRepo() == null)
						return;
					filter.setSelected(!filter.isSelected());
					postChangeAction.run();
				}
			}

			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (P_REPOSITORY.equals(event.getProperty())) {
					Repository currentRepo = historyPage.getCurrentRepo();
					if (currentRepo == null) {
						this.setEnabled(false);
						helper = null;
					} else {
						this.setEnabled(true);
						helper = new RefFilterHelper(currentRepo);
						setHeadModeFromHelperState();
						updateUiForMode();
					}
				}
			}
		}

		private void createSelectShownRefsAction() {
			selectShownRefsAction = new SelectShownRefsAction();
			actionsToDispose.add(selectShownRefsAction);
		}

		private class ShowFirstParentOnlyPrefAction extends Action
				implements IPropertyChangeListener, IWorkbenchAction {

			ShowFirstParentOnlyPrefAction() {
				super(UIText.GitHistoryPage_ShowFirstParentOnlyMenuLabel);
				historyPage.addPropertyChangeListener(this);
				historyPage.store.addPropertyChangeListener(this);
				setChecked(historyPage.isShowFirstParentOnly());
				setImageDescriptor(UIIcons.FIRST_PARENT_ONLY);
				setToolTipText(UIText.GitHistoryPage_showFirstParentOnly);
			}

			@Override
			public void run() {
				final String prefKey = UIPreferences.RESOURCEHISTORY_SHOW_FIRST_PARENT_ONLY_DEFAULT;
				Repository repo = historyPage.getCurrentRepo();
				if (repo != null) {
					String repoSpecificKey = Activator.getDefault()
							.getRepositoryUtil()
							.getRepositorySpecificPreferenceKey(repo, prefKey);
					boolean newBoolean = isChecked();
					if (newBoolean == historyPage.store.getBoolean(prefKey)) {
						historyPage.store.setToDefault(repoSpecificKey);
					} else {
						String newValue = newBoolean ? IPreferenceStore.TRUE
								: IPreferenceStore.FALSE;
						historyPage.store.putValue(repoSpecificKey, newValue);
					}

					historyPage.saveStoreIfNeeded();
				}
				historyPage.refresh(historyPage.selectedCommit());
			}

			/**
			 * Applies the new boolean state to the checkbox and refreshes the
			 * historyPage if the state was changed.
			 *
			 * @param newState
			 *            the new state to apply.
			 * @param forceRefresh
			 *            whether to force a refresh of the entire history page
			 */
			private void applyNewState(boolean newState, boolean forceRefresh) {
				Control control = historyPage.getControl();
				if (control != null && !control.isDisposed()) {
					control.getDisplay().asyncExec(() -> {
						if (!control.isDisposed()) {
							setChecked(newState);
						}
					});
				}
				if (forceRefresh) {
					historyPage.refresh(historyPage.selectedCommit());
				}
			}

			@Override
			public void propertyChange(PropertyChangeEvent event) {
				Repository repo = historyPage.getCurrentRepo();

				final String prefKey = UIPreferences.RESOURCEHISTORY_SHOW_FIRST_PARENT_ONLY_DEFAULT;

				if (repo == null) {
					if (prefKey.equals(event.getProperty())) {
						// global first parent preference changed and we have no
						// current repo. Apply the new global preference
						applyNewState(historyPage.store.getBoolean(prefKey),
								true);
					}
					return;
				}

				String repoSpecificKey = Activator.getDefault()
						.getRepositoryUtil()
						.getRepositorySpecificPreferenceKey(repo, prefKey);

				if (prefKey.equals(event.getProperty())) {
					// global first parent preference changed, if this repo does
					// not have a repo specific one apply the global one
					if (!historyPage.store.contains(repoSpecificKey)) {
						applyNewState(historyPage.store.getBoolean(prefKey),
								true);
					}
				}

				if (P_REPOSITORY.equals(event.getProperty())) {
					// The repository was switched. Apply that correct state.
					// As the repository switch causes a refresh anyway don't do
					// it again here.
					applyNewState(historyPage.isShowFirstParentOnly(), false);
				}
			}

			@Override
			public void dispose() {
				historyPage.removePropertyChangeListener(this);
				historyPage.store.removePropertyChangeListener(this);
			}
		}

		private void createShowFirstParentOnlyAction() {
			showFirstParentOnlyAction = new ShowFirstParentOnlyPrefAction();
			actionsToDispose.add(showFirstParentOnlyAction);
		}

		private void createShowAdditionalRefsAction() {
			showAdditionalRefsAction = new BooleanPrefAction(
					UIPreferences.RESOURCEHISTORY_SHOW_ADDITIONAL_REFS,
					UIText.GitHistoryPage_ShowAdditionalRefsMenuLabel) {

				@Override
				void apply(boolean value) {
					historyPage.refresh(historyPage.selectedCommit());
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
					historyPage.refresh(historyPage.selectedCommit());
				}
			};
			followRenamesAction.apply(followRenamesAction.isChecked());
			actionsToDispose.add(followRenamesAction);
		}

		private void createShowCommentAction() {
			showCommentAction = new BooleanPrefAction(
					UIPreferences.RESOURCEHISTORY_SHOW_REV_COMMENT,
					UIText.ResourceHistory_toggleRevComment) {
				@Override
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
				@Override
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
				@Override
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
				@Override
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
				@Override
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
				@Override
				void apply(boolean value) {
					// nothing, just set the Preference
				}
			};
			showTagSequenceAction.apply(showTagSequenceAction.isChecked());
			actionsToDispose.add(showTagSequenceAction);
		}

		private void createShowBranchSequenceAction() {
			showBranchSequenceAction = new BooleanPrefAction(
					UIPreferences.HISTORY_SHOW_BRANCH_SEQUENCE,
					UIText.ResourceHistory_ShowBranchSequence) {
				@Override
				void apply(boolean value) {
					// nothing, just set the Preference
				}
			};
			showBranchSequenceAction
					.apply(showBranchSequenceAction.isChecked());
			actionsToDispose.add(showBranchSequenceAction);
		}

		private void createWrapCommentAction() {
			wrapCommentAction = new BooleanPrefAction(
					UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_WRAP,
					UIText.ResourceHistory_toggleCommentWrap) {
				@Override
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
				@Override
				void apply(boolean fill) {
					// nothing, just set the Preference
				}
			};
			fillCommentAction.apply(fillCommentAction.isChecked());
			actionsToDispose.add(fillCommentAction);
		}

		private class ConfigureFilterAction extends Action
				implements IWorkbenchAction, IPropertyChangeListener {

			private GitHistoryRefFilterConfigurationDialog dialog;

			private RefFilterHelper helper;

			ConfigureFilterAction() {
				super(UIText.GitHistoryPage_configureFilters);
				historyPage.addPropertyChangeListener(this);
				Repository currentRepo = historyPage.getCurrentRepo();
				if (currentRepo != null) {
					helper = new RefFilterHelper(currentRepo);
				}
			}

			@Override
			public void run() {
				if (historyPage.getCurrentRepo() == null) {
					return;
				}

				dialog = new GitHistoryRefFilterConfigurationDialog(
						historyPage.getSite().getWorkbenchWindow().getShell(),
						historyPage.getCurrentRepo(), helper);
				if (dialog.open() == Window.OK) {
					historyPage.refresh(historyPage.selectedCommit());
				}
			}

			@Override
			public void dispose() {
				historyPage.removePropertyChangeListener(this);
				if (dialog != null) {
					dialog.close();
				}
			}

			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (P_REPOSITORY.equals(event.getProperty())) {
					Repository currentRepo = historyPage.getCurrentRepo();
					if (currentRepo == null) {
						this.setEnabled(false);
						helper = null;
					} else {
						this.setEnabled(true);
						helper = new RefFilterHelper(currentRepo);
					}
				}

			}
		}

		private void createConfigureFiltersAction() {
			configureFiltersAction = new ConfigureFilterAction();
			actionsToDispose.add(configureFiltersAction);
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
			builder.append(", regular: "); //$NON-NLS-1$
			builder.append(isRegularFile());

			return builder.toString();
		}
	}

	private static class HistoryPageRule implements ISchedulingRule {
		@Override
		public boolean contains(ISchedulingRule rule) {
			return this == rule;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return this == rule;
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

	/** Split between {@link #commitAndDiff} and {@link #fileViewer}. */
	private SashForm revInfoSplit;

	/** The table showing the DAG, first "paragraph", author, author date. */
	private CommitGraphTable graph;

	private CommitAndDiffComponent commitAndDiff;

	/** Viewer displaying file difference implied by {@link #graph}'s commit. */
	private CommitFileDiffViewer fileViewer;

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

	/** Last FETCH_HEAD */
	private AnyObjectId currentFetchHeadId;

	/** Repository of the last input*/
	private Repository currentRepo;

	/** ObjectId of the ref or commit of the last input, if any. */
	private ObjectId selectedObj;

	private String currentRefFilters;

	private boolean currentShowFirstParentOnly;

	private boolean currentShowAdditionalRefs;

	private boolean currentShowNotes;

	private boolean currentFollowRenames;

	/** Tracks the file names that are to be highlighted in the diff file viewer */
	private Set<String> fileViewerInterestingPaths;

	/** Tree walker to use in the file diff viewer. */
	private TreeWalk fileDiffWalker;

	// react on changes to the relative date preference
	private final IPropertyChangeListener listener = new IPropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			final String prop = event.getProperty();
			if (UIPreferences.HISTORY_MAX_BRANCH_LENGTH.equals(prop)
					|| UIPreferences.HISTORY_MAX_TAG_LENGTH.equals(prop))
				graph.getTableView().refresh();
			if (UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_WRAP.equals(prop)) {
				commitAndDiff.setWrap(
						((Boolean) event.getNewValue()).booleanValue());
			}

		}
	};

	private final IPreferenceChangeListener prefListener = event -> {
		if (!RepositoryUtil.PREFS_DIRECTORIES_REL.equals(event.getKey())) {
			return;
		}
		if (getCurrentRepo() == null || !Activator.getDefault()
				.getRepositoryUtil().contains(getCurrentRepo())) {
			Control control = historyControl;
			if (!control.isDisposed()) {
				control.getDisplay().asyncExec(() -> {
					if (!control.isDisposed()) {
						setInput(null);
					}
				});
			}
		}

		Object oldValue = event.getOldValue();
		String pathSep = File.pathSeparator;

		if (oldValue != null) {
			String[] oldPaths = oldValue.toString().split(pathSep);
			List<String> removedPaths = new ArrayList<>(
					Arrays.asList(oldPaths));

			Object newValue = event.getNewValue();
			if (newValue != null) {
				String[] newPaths = newValue.toString().split(pathSep);
				for (String path : newPaths) {
					removedPaths.remove(path);
				}
			}

			for (String path : removedPaths) {
				unsetRepoSpecificPreference(path,
						UIPreferences.RESOURCEHISTORY_SHOW_FIRST_PARENT_ONLY_DEFAULT);
			}
			saveStoreIfNeeded();
		}
	};

	/** Tracks the selection to display the correct input when linked with editors. */
	private GitHistorySelectionTracker selectionTracker;

	/**
	 * List of paths we used to limit the revwalk; null if no paths.
	 * <p>
	 * Note that a change in this list requires that the history is redrawn
	 */
	private List<FilterPath> pathFilters;

	private Runnable refschangedRunnable;

	private final RenameTracker renameTracker = new RenameTracker();

	private final HistoryPageRule pageSchedulingRule;

	/** Toolbar to find commits in the history view. */
	private SearchBar searchBar;

	private FocusTracker focusTracker;

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

		if (object instanceof RepositoryTreeNode) {
			return SUPPORTED_REPOSITORY_NODE_TYPES
					.contains(((RepositoryTreeNode) object).getType());
		}

		if (object instanceof Path) {
			return true;
		}

		IResource resource = AdapterUtils.adaptToAnyResource(object);
		if (resource != null && typeOk(resource)) {
			return true;
		}

		return Adapters.adapt(object, Repository.class) != null;
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
		pageSchedulingRule = new HistoryPageRule();
		if (trace) {
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.HISTORYVIEW.getLocation());
		}
	}

	@Override
	public void createControl(final Composite parent) {
		trace = GitTraceLocation.HISTORYVIEW.isActive();
		if (trace)
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.HISTORYVIEW.getLocation());

		attachSelectionTracker();

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

		Activator.getDefault().getPreferenceStore()
				.addPropertyChangeListener(listener);

		revInfoSplit = new SashForm(graphDetailSplit, SWT.HORIZONTAL);

		commitAndDiff = new CommitAndDiffComponent(revInfoSplit, getPartSite());

		commitAndDiff.setWrap(store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_WRAP));

		fileViewer = new CommitFileDiffViewer(revInfoSplit, getSite());
		fileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				List<FileDiff> diffs = new ArrayList<>();
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection sel = (IStructuredSelection) selection;
					for (Object obj : sel.toList())
						if (obj instanceof FileDiff)
							diffs.add((FileDiff) obj);
				}
				formatDiffs(diffs);
			}
		});

		layoutSashForm(graphDetailSplit,
				UIPreferences.RESOURCEHISTORY_GRAPH_SPLIT);
		layoutSashForm(revInfoSplit, UIPreferences.RESOURCEHISTORY_REV_SPLIT);

		attachCommitSelectionChanged();
		initActions();

		getSite().setSelectionProvider(
				new RepositorySelectionProvider(graph.getTableView(), () -> {
					HistoryPageInput myInput = getInputInternal();
					return myInput != null ? myInput.getRepository() : null;
				}));
		getSite().registerContextMenu(POPUP_ID, popupMgr, graph.getTableView());
		// Track which of our controls has the focus, so that we can focus the
		// last focused one in setFocus().
		focusTracker = new FocusTracker();
		trackFocus(graph.getTable());
		trackFocus(commitAndDiff.getDiffViewer().getControl());
		trackFocus(commitAndDiff.getCommitViewer().getControl());
		trackFocus(fileViewer.getControl());
		layout();

		myRefsChangedHandle = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache().getGlobalListenerList()
				.addRefsChangedListener(this);

		InstanceScope.INSTANCE
				.getNode(org.eclipse.egit.core.Activator.getPluginId())
				.addPreferenceChangeListener(prefListener);

		IToolBarManager manager = getSite().getActionBars().getToolBarManager();
		searchBar = new HistorySearchBar(
				GitHistoryPage.class.getName() + ".searchBar", //$NON-NLS-1$
				graph, actions.findAction, getSite().getActionBars());
		manager.prependToGroup("org.eclipse.team.ui.historyView", searchBar); //$NON-NLS-1$
		getSite().getActionBars().updateActionBars();
		if (trace)
			GitTraceLocation.getTrace().traceExit(
					GitTraceLocation.HISTORYVIEW.getLocation());
	}

	private void trackFocus(Control control) {
		if (control != null) {
			focusTracker.addToFocusTracking(control);
		}
	}

	private void layoutSashForm(final SashForm sf, final String key) {
		sf.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				final int[] w = sf.getWeights();
				store.putValue(key, UIPreferences.intArrayToString(w));
				saveStoreIfNeeded();

			}
		});
		int[] weights = UIPreferences.stringToIntArray(store.getString(key), 2);
		if (weights == null) {
			// Corrupted preferences?
			weights = UIPreferences
					.stringToIntArray(store.getDefaultString(key), 2);
		}
		sf.setWeights(weights);
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

		if (showComment && showFiles) {
			graphDetailSplit.setMaximizedControl(null);
			revInfoSplit.setMaximizedControl(null);
		} else if (showComment && !showFiles) {
			graphDetailSplit.setMaximizedControl(null);
			revInfoSplit.setMaximizedControl(
					commitAndDiff.getCommitViewer().getControl());
		} else if (!showComment && showFiles) {
			graphDetailSplit.setMaximizedControl(null);
			revInfoSplit.setMaximizedControl(fileViewer.getControl());
		} else if (!showComment && !showFiles)
			graphDetailSplit.setMaximizedControl(graph.getControl());
		historyControl.layout();
	}

	private void attachCommitSelectionChanged() {
		CommitMessageViewer commentViewer = commitAndDiff.getCommitViewer();
		graph.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				final ISelection s = event.getSelection();
				if (s.isEmpty() || !(s instanceof IStructuredSelection)) {
					commentViewer.setInput(null);
					fileViewer.newInput(null);
					return;
				}

				final IStructuredSelection sel = ((IStructuredSelection) s);
				if (sel.size() > 1) {
					commentViewer.setInput(null);
					fileViewer.newInput(null);
					return;
				}
				if (input == null) {
					return;
				}
				final SWTCommit c = (SWTCommit) sel.getFirstElement();
				if (c == commentViewer.getInput()) {
					return;
				}
				commentViewer.setInput(c);
				boolean firstParentOnly = isShowFirstParentOnly();
				try (RevWalk walk = new RevWalk(input.getRepository())) {
					final RevCommit unfilteredCommit = walk.parseCommit(c);
					for (RevCommit parent : unfilteredCommit.getParents())
						walk.parseBody(parent);
					fileViewer.newInput(new FileDiffInput(input.getRepository(),
							fileDiffWalker, unfilteredCommit,
							fileViewerInterestingPaths,
							input.getSingleFile() != null, firstParentOnly));
				} catch (IOException e) {
					fileViewer.newInput(new FileDiffInput(input.getRepository(),
							fileDiffWalker, c, fileViewerInterestingPaths,
							input.getSingleFile() != null, firstParentOnly));
				}
			}
		});
		commentViewer
				.addCommitNavigationListener(graph::selectCommit);
	}

	/**
	 * Attaches the selection tracker to the workbench page containing this page.
	 */
	private void attachSelectionTracker() {
		if (selectionTracker == null) {
			selectionTracker = new GitHistorySelectionTracker();
			selectionTracker.attach(getSite().getPage());
		}
	}

	/**
	 * Detaches the selection tracker from the workbench page, if necessary.
	 */
	private void detachSelectionTracker() {
		if (selectionTracker != null) {
			selectionTracker.detach(getSite().getPage());
		}
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
		mgr.add(actions.switchRepositoryAction);
		mgr.add(actions.filterAction);
		mgr.add(actions.compareModeAction);
		mgr.add(actions.selectShownRefsAction);
		mgr.add(actions.showFirstParentOnlyAction);
	}

	private class ColumnAction extends Action implements IUpdate {

		private final int columnIndex;

		public ColumnAction(String text, int idx) {
			super(text, IAction.AS_CHECK_BOX);
			columnIndex = idx;
			update();
		}

		@Override
		public void run() {
			graph.setVisible(columnIndex, isChecked());
		}

		@Override
		public void update() {
			setChecked(graph.getTableView().getTable().getColumn(columnIndex)
					.getWidth() > 0);
		}
	}

	private void setupViewMenu() {
		IMenuManager viewMenuMgr = getSite().getActionBars().getMenuManager();
		viewMenuMgr.add(actions.refreshAction);

		viewMenuMgr.add(new Separator());
		IMenuManager columnsMenuMgr = new MenuManager(
				UIText.GitHistoryPage_ColumnsSubMenuLabel);
		viewMenuMgr.add(columnsMenuMgr);
		TableColumn[] columns = graph.getTableView().getTable().getColumns();
		for (int i = 0; i < columns.length; i++) {
			if (i != 1) {
				ColumnAction action = new ColumnAction(columns[i].getText(), i);
				columnsMenuMgr.add(action);
				columns[i].addListener(SWT.Resize, event -> {
					action.update();
				});
			}
		}
		IMenuManager showSubMenuMgr = new MenuManager(
				UIText.GitHistoryPage_ShowSubMenuLabel);
		viewMenuMgr.add(showSubMenuMgr);
		showSubMenuMgr.add(actions.showFirstParentOnlyAction);
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
		showInMessageManager.add(actions.showBranchSequenceAction);
		showInMessageManager.add(actions.showTagSequenceAction);
		showInMessageManager.add(actions.wrapCommentAction);
		showInMessageManager.add(actions.fillCommentAction);

		viewMenuMgr.add(actions.filterAction);

		viewMenuMgr.add(actions.configureFiltersAction);

		viewMenuMgr.add(new Separator());
		viewMenuMgr.add(actions.compareModeAction);
		viewMenuMgr.add(actions.reuseCompareEditorAction);
	}

	@Override
	public void dispose() {
		trace = GitTraceLocation.HISTORYVIEW.isActive();
		if (trace)
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.HISTORYVIEW.getLocation());

		if (focusTracker != null) {
			focusTracker.dispose();
			focusTracker = null;
		}
		detachSelectionTracker();

		Activator.getDefault().getPreferenceStore()
				.removePropertyChangeListener(listener);

		InstanceScope.INSTANCE
				.getNode(org.eclipse.egit.core.Activator.getPluginId())
				.removePreferenceChangeListener(prefListener);

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
		renameTracker.reset(null);
		Job.getJobManager().cancel(JobFamilies.HISTORY_DIFF);
		setCurrentRepo(null);
		selectedObj = null;
		super.dispose();
	}

	@Override
	public void setFocus() {
		if (repoHasBeenRemoved(getCurrentRepo())) {
			clearHistoryPage();
			graph.getControl().setFocus();
		} else {
			Control control = focusTracker.getLastFocusControl();
			if (control == null) {
				control = graph.getControl();
			}
			control.setFocus();
		}
	}

	private boolean repoHasBeenRemoved(final Repository repo) {
		return (repo != null && repo.getDirectory() != null && !repo
				.getDirectory().exists());
	}

	private void clearHistoryPage() {
		setCurrentRepo(null);
		selectedObj = null;
		name = ""; //$NON-NLS-1$
		input = null;
		commitAndDiff.getCommitViewer().setInput(null);
		fileViewer.newInput(null);
		setInput(null);
	}

	private void clearViewers() {
		TableViewer viewer = graph.getTableView();
		viewer.setSelection(StructuredSelection.EMPTY);
		viewer.setInput(new SWTCommit[0]);
	}

	@Override
	public Control getControl() {
		return topControl;
	}

	@Override
	public void refresh() {
		refresh(null);
	}

	private void refresh(RevCommit prevSelection) {
		if (repoHasBeenRemoved(getCurrentRepo())) {
			clearHistoryPage();
		}
		this.input = null;
		inputSet(prevSelection);
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

	private RevCommit selectedCommit() {
		IStructuredSelection selection = graph.getTableView()
				.getStructuredSelection();
		if (!selection.isEmpty()) {
			return Adapters.adapt(selection.getFirstElement(), RevCommit.class);
		}
		return null;
	}

	@Override
	public void onRefsChanged(final RefsChangedEvent e) {
		if (input == null || e.getRepository() != input.getRepository())
			return;

		if (getControl().isDisposed())
			return;

		synchronized (this) {
			if (refschangedRunnable == null) {
				refschangedRunnable = () -> {
					if (!getControl().isDisposed()) {
						if (GitTraceLocation.HISTORYVIEW.isActive()) {
							GitTraceLocation.getTrace().trace(
									GitTraceLocation.HISTORYVIEW.getLocation(),
									"Executing async repository changed event"); //$NON-NLS-1$
						}
						refschangedRunnable = null;
						initAndStartRevWalk(
								!(e instanceof FetchHeadChangedEvent));
					}
				};
				getControl().getDisplay().asyncExec(refschangedRunnable);
			}
		}
	}

	/**
	 * Returns the last, tracked selection. If no selection has been tracked,
	 * returns the current selection in the active part.
	 *
	 * @return selection
	 */
	private IStructuredSelection getSelection() {
		if (selectionTracker != null
				&& selectionTracker.getSelection() != null) {
			return selectionTracker.getSelection();
		}
		// fallback to current selection of the active part
		ISelection selection = getSite().getPage().getSelection();
		if (selection != null) {
			return SelectionUtils.getStructuredSelection(selection);
		}
		return null;
	}

	/**
	 * <p>
	 * Determines the
	 * {@link SelectionUtils#getMostFittingInput(IStructuredSelection, Object)
	 * most fitting} HistoryPageInput for the {@link #getSelection() last
	 * selection} and the given object. Most fitting means that the input will
	 * contain all selected resources which are contained in the same repository
	 * as the given object. If no most fitting input can be determined, the
	 * given object is returned as is.
	 * </p>
	 * <p>
	 * This is a workaround for the limitation of the GenericHistoryView that
	 * only forwards the first part of a selection and adapts it immediately to
	 * an {@link IResource}.
	 * </p>
	 *
	 * @param object
	 *            The object to which the HistoryPageInput is tailored
	 * @return the most fitting history input
	 * @see SelectionUtils#getMostFittingInput(IStructuredSelection, Object)
	 */
	private Object getMostFittingInput(Object object) {
		IStructuredSelection selection = getSelection();
		if (selection != null && !selection.isEmpty()) {
			HistoryPageInput mostFittingInput = SelectionUtils
					.getMostFittingInput(selection, object);
			if (mostFittingInput != null) {
				return mostFittingInput;
			}
		}
		return object;
	}

	@Override
	public boolean setInput(Object object) {
		try {
			Object useAsInput = getMostFittingInput(object);
			// reset tracked selection after it has been used to avoid wrong behavior
			if (selectionTracker != null) {
				selectionTracker.clearSelection();
			}
			// hide the warning text initially
			setWarningText(null);
			trace = GitTraceLocation.HISTORYVIEW.isActive();
			if (trace)
				GitTraceLocation.getTrace().traceEntry(
						GitTraceLocation.HISTORYVIEW.getLocation(), useAsInput);

			if (useAsInput == super.getInput())
				return true;
			this.input = null;
			return super.setInput(useAsInput);
		} finally {
			if (trace)
				GitTraceLocation.getTrace().traceExit(
						GitTraceLocation.HISTORYVIEW.getLocation());
		}
	}

	@Override
	public boolean inputSet() {
		return inputSet(null);
	}

	private boolean inputSet(RevCommit prevSelection) {
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
					if (ref.getObjectId() == null) {
						ref = null;
					}
					showRef = ref != null;
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
			} else if (o instanceof HistoryPageInput) {
				input = (HistoryPageInput) o;
				repo = input.getRepository();
			} else if (o instanceof Path) {
				Path path = (Path) o;
				repo = ResourceUtil.getRepository(path);
				if (repo != null) {
					input = new HistoryPageInput(repo,
							new File[] { path.toFile() });
				}
			} else {
				IResource resource = AdapterUtils.adaptToAnyResource(o);
				if (resource != null) {
					RepositoryMapping mapping = RepositoryMapping
							.getMapping(resource);
					if (mapping != null) {
						repo = mapping.getRepository();
						input = new HistoryPageInput(repo,
								new IResource[] { resource });
					}
				}
			}
			if (repo == null) {
				repo = Adapters.adapt(o, Repository.class);
				if (repo != null) {
					File file = Adapters.adapt(o, File.class);
					if (file == null) {
						input = new HistoryPageInput(repo);
					} else {
						input = new HistoryPageInput(repo, new File[] { file });
					}
				}
			}
			selection = Adapters.adapt(o, RevCommit.class);

			if (input == null || repo == null) {
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
			// the repository itself has no notion of projects
			actions.showAllProjectVersionsAction
					.setEnabled(inResources != null);
			actions.showAllFolderVersionsAction.setEnabled(filtersActive);
			actions.showAllResourceVersionsAction.setEnabled(filtersActive);

			setErrorMessage(null);
			try {
				ObjectId id = null;
				if (ref != null) {
					id = ref.getLeaf().getObjectId();
				} else if (selection != null) {
					id = selection.getId();
				}
				initAndStartRevWalk(false, id);
			} catch (IllegalStateException e) {
				Activator.handleError(e.getMessage(), e, true);
				return false;
			}

			if (prevSelection != null) {
				graph.selectCommitStored(prevSelection);
			} else {
				if (showHead) {
					showHead(repo);
				}
				if (showRef) {
					showRef(ref, repo);
				}
				if (showTag) {
					showTag(ref, repo);
				}
				if (selection != null) {
					graph.selectCommitStored(selection);
				}
			}
			return true;
		} finally {
			if (trace)
				GitTraceLocation.getTrace().traceExit(
						GitTraceLocation.HISTORYVIEW.getLocation());
		}
	}

	private void showHead(Repository repo) {
		try (RevWalk rw = new RevWalk(repo)) {
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
		try (RevWalk rw = new RevWalk(repo)) {
			RevCommit c = rw.parseCommit(ref.getLeaf().getObjectId());
			graph.selectCommit(c);
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
	}

	private void showTag(Ref ref, Repository repo) {
		try (RevWalk rw = new RevWalk(repo)) {
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
		// <type>: <path> [<repository name>]
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
			// History (the generic history view cannot deal with multiple
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
			@Override
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

	@Override
	public boolean isValidInput(final Object object) {
		return canShowHistoryFor(object);
	}

	@Override
	public <T> T getAdapter(final Class<T> adapter) {
		return null;
	}

	@Override
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

	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * @return the internal input object, or <code>null</code>
	 */
	public HistoryPageInput getInputInternal() {
		return this.input;
	}

	/**
	 * The super implementation returns the raw input (e.g. the workbench
	 * selection). The Git History Page however adapts that input before
	 * actually using it. If we don't return this adapted input, then the
	 * history drop down will show the same (adapted) history input multiple
	 * times.
	 */
	@Override
	public Object getInput() {
		return getInputInternal();
	}

	private Repository getCurrentRepo() {
		return currentRepo;
	}

	private void setCurrentRepo(Repository newRepo) {
		Repository old = getCurrentRepo();
		this.currentRepo = newRepo;

		if (!Objects.equals(old, newRepo)) {
			this.firePropertyChange(this, P_REPOSITORY, old, newRepo);
		}
	}

	void setWarningTextInUIThread(final Job j) {
		graph.getControl().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (!graph.getControl().isDisposed() && job == j) {
					setWarningText(UIText.GitHistoryPage_ListIncompleteWarningMessage);
				}
			}
		});
	}

	@SuppressWarnings("boxing")
	void showCommitList(final Job j, final SWTCommitList list,
			final SWTCommit[] asArray, final RevCommit toSelect, final boolean incomplete, final RevFlag highlightFlag) {
		if (trace)
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.HISTORYVIEW.getLocation(),
					new Object[] { asArray.length });
		if (job != j || graph.getControl().isDisposed())
			return;

		graph.getControl().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (!graph.getControl().isDisposed() && job == j) {
					graph.setInput(highlightFlag, list, asArray, input, true);
					if (toSelect != null)
						graph.selectCommit(toSelect);
					if (getFollowRenames())
						updateInterestingPathsOfFileViewer();
					if (trace)
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.HISTORYVIEW.getLocation(),
								"Setting input to table"); //$NON-NLS-1$
					final Object currentInput = GitHistoryPage.super.getInput();
					searchBar.setInput(new ICommitsProvider() {

						@Override
						public Object getSearchContext() {
							return currentInput;
						}

						@Override
						public SWTCommit[] getCommits() {
							return asArray;
						}

						@Override
						public RevFlag getHighlight() {
							return highlightFlag;
						}
					});
					actions.findAction.setEnabled(true);
					if (store.getBoolean(
							UIPreferences.RESOURCEHISTORY_SHOW_FINDTOOLBAR)) {
						searchBar.setVisible(true);
					}
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

	private void updateInterestingPathsOfFileViewer() {
		fileViewer.setInterestingPaths(fileViewerInterestingPaths);
		fileViewer.refresh();
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

	private void initAndStartRevWalk(boolean forceNewWalk) {
		initAndStartRevWalk(forceNewWalk, selectedObj);
	}

	private void initAndStartRevWalk(boolean forceNewWalk,
			ObjectId newSelectedObj) throws IllegalStateException {
		try {
			if (trace)
				GitTraceLocation.getTrace()
						.traceEntry(GitTraceLocation.HISTORYVIEW.getLocation());

			if (input == null) {
				return;
			}
			Repository db = input.getRepository();
			if (repoHasBeenRemoved(db)) {
				clearHistoryPage();
				return;
			}

			Assert.isNotNull(db);

			UnitOfWork.execute(db, () -> {
				AnyObjectId headId = resolveHead(db, true);
				if (headId == null) {
					currentHeadId = null;
					currentFetchHeadId = null;
					selectedObj = null;
					setCurrentRepo(db);
					clearViewers();
					return;
				}
				AnyObjectId fetchHeadId = resolveFetchHead(db);

				List<FilterPath> paths = buildFilterPaths(input.getItems(),
						input.getFileList(), db);

				boolean repoChanged = false;
				if (!db.equals(getCurrentRepo())) {
					repoChanged = true;
					setCurrentRepo(db);
				}

				boolean objChanged = false;
				if (newSelectedObj != null && newSelectedObj != selectedObj) {
					objChanged = !newSelectedObj.equals(selectedObj);
				}
				selectedObj = newSelectedObj;

				boolean settingsChanged = updateSettings();
				boolean pathsChanged = pathChanged(pathFilters, paths);
				// Force a new walk and showing it only if we do show it at all.
				boolean headChanged = !headId.equals(currentHeadId)
						&& showsHead(db);
				boolean fetchHeadChanged = currentShowAdditionalRefs
						&& fetchHeadId != null
						&& !fetchHeadId.equals(currentFetchHeadId);
				if (forceNewWalk || repoChanged || objChanged || headChanged
						|| fetchHeadChanged || pathsChanged
						|| settingsChanged) {
					releaseGenerateHistoryJob();

					if (repoChanged) {
						// Clear all viewers. Otherwise it may be possible that
						// the user invokes a context menu command and due to to
						// the highly asynchronous loading we end up with
						// inconsistent diff computations trying to find the
						// diff for a commit in the wrong repository.
						clearViewers();
					}

					SWTWalk walk = createNewWalk(db, headId, fetchHeadId);

					fileDiffWalker = createFileWalker(walk, db, paths);

					RevCommit toShow = null;
					if (!repoChanged) {
						if (headChanged) {
							toShow = toRevCommit(walk, headId);
						} else if (fetchHeadChanged) {
							toShow = toRevCommit(walk, fetchHeadId);
						}
					}
					loadInitialHistory(walk, toShow);
				} else {
					// needed for context menu and double click
					graph.setHistoryPageInput(input);
				}
			});
		} finally {
			if (trace)
				GitTraceLocation.getTrace().traceExit(
						GitTraceLocation.HISTORYVIEW.getLocation());

		}
	}

	private RevCommit toRevCommit(RevWalk w, AnyObjectId id) {
		try {
			return w.parseCommit(id);
		} catch (IOException e) {
			// Ignore here; HEAD or FETCH_HEAD are not a commit? Result is only
			// for display purposes.
			return null;
		}
	}

	private boolean showsHead(@NonNull Repository db) {
		return new RefFilterHelper(db).getRefFilters().stream()
				.anyMatch(f -> f.isSelected()
						&& Constants.HEAD.equals(f.getFilterString()));
	}

	/**
	 * Updates the settings from the preferences and returns whether any have
	 * changed.
	 *
	 * @return {@code true} if any setting changed, {@code false} otherwise
	 */
	private boolean updateSettings() {
		String newRefFilters = ""; //$NON-NLS-1$
		Repository repo = getCurrentRepo();
		if (repo != null) {
			newRefFilters = store.getString(Activator.getDefault()
					.getRepositoryUtil()
					.getRepositorySpecificPreferenceKey(repo,
							UIPreferences.RESOURCEHISTORY_SELECTED_REF_FILTERS));
		}
		boolean refFiltersChanged = !Objects.equals(currentRefFilters,
				newRefFilters);
		currentRefFilters = newRefFilters;

		boolean isShowFirstParentOnly = isShowFirstParentOnly();
		boolean firstParentOnlyChanged = currentShowFirstParentOnly != isShowFirstParentOnly;
		currentShowFirstParentOnly = isShowFirstParentOnly;

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

		return refFiltersChanged || firstParentOnlyChanged
				|| additionalRefsChange || showNotesChanged
				|| followRenamesChanged;
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

	private AnyObjectId resolveFetchHead(Repository db) {
		try {
			return db.resolve(Constants.FETCH_HEAD);
		} catch (IOException e) {
			return null;
		}
	}

	private ArrayList<FilterPath> buildFilterPaths(final IResource[] inResources,
			final File[] inFiles, final Repository db)
			throws IllegalStateException {
		final ArrayList<FilterPath> paths;
		if (inResources != null) {
			paths = new ArrayList<>(inResources.length);
			for (final IResource r : inResources) {
				final RepositoryMapping map = RepositoryMapping.getMapping(r);
				if (map == null)
					continue;
				if (db != map.getRepository())
					throw new IllegalStateException(
							UIText.RepositoryAction_multiRepoSelection);

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
			paths = new ArrayList<>(inFiles.length);
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

				if (gitDirPath.isPrefixOf(filePath)) {
					throw new IllegalStateException(
							NLS.bind(
									UIText.GitHistoryPage_FileOrFolderPartOfGitDirMessage,
									filePath.toOSString()));
				}
				IPath pathToAdd = filePath.removeFirstSegments(segmentCount)
						.setDevice(null);
				if (!pathToAdd.isEmpty())
					paths.add(new FilterPath(pathToAdd.toString(), isRegularFile));
			}
		} else
			paths = new ArrayList<>(0);
		return paths;
	}

	private boolean pathChanged(final List<FilterPath> o, final List<FilterPath> n) {
		if (o == null)
			return !n.isEmpty();
		return !o.equals(n);
	}

	/**
	 * Unset the repository specific preference of the given key for the
	 * repository represented by the given repository path.
	 * <p>
	 * This method does not save the preference store. Call
	 * {@link #saveStoreIfNeeded()} when appropriate.
	 * </p>
	 *
	 * @param repositoryPath
	 *            Representing the repository to remove the preference for
	 * @param key
	 *            The preference to remove for the given repository.
	 */
	private void unsetRepoSpecificPreference(String repositoryPath,
			String key) {
		String prefString = Activator.getDefault().getRepositoryUtil()
				.getRepositorySpecificPreferenceKey(repositoryPath, key);
		store.setToDefault(prefString);
	}

	private void saveStoreIfNeeded() {
		if (store.needsSaving()) {
			try {
				store.save();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, false);
			}
		}
	}

	private boolean isShowFirstParentOnly() {
		final String prefKey = UIPreferences.RESOURCEHISTORY_SHOW_FIRST_PARENT_ONLY_DEFAULT;
		boolean firstParent = store.getBoolean(prefKey);
		Repository repo = getCurrentRepo();

		if (repo != null) {
			String repoSpecificKey = Activator.getDefault().getRepositoryUtil()
					.getRepositorySpecificPreferenceKey(repo, prefKey);
			if (store.contains(repoSpecificKey)) {
				firstParent = store.getBoolean(repoSpecificKey);
			}
		}
		return firstParent;
	}

	private @NonNull SWTWalk createNewWalk(@NonNull Repository db,
			AnyObjectId headId,
			AnyObjectId fetchHeadId) {
		currentHeadId = headId;
		currentFetchHeadId = fetchHeadId;
		SWTWalk walk = new GitHistoryWalk(db, selectedObj);

		if (isShowFirstParentOnly()) {
			walk.setFirstParent(true);
		}

		try {
			if (store
					.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_ADDITIONAL_REFS))
				walk.addAdditionalRefs(db.getRefDatabase()
						.getAdditionalRefs());
			walk.addAdditionalRefs(db.getRefDatabase()
					.getRefsByPrefix(Constants.R_NOTES));
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

	private void formatDiffs(final List<FileDiff> diffs) {
		Job.getJobManager().cancel(JobFamilies.HISTORY_DIFF);
		DiffViewer diffViewer = commitAndDiff.getDiffViewer();
		if (diffs.isEmpty()) {
			if (UIUtils.isUsable(diffViewer)) {
				IDocument document = new Document();
				diffViewer.setDocument(document);
			}
			return;
		}

		Job formatJob = new Job(UIText.GitHistoryPage_FormatDiffJobName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				int maxLines = Activator.getDefault().getPreferenceStore()
						.getInt(UIPreferences.HISTORY_MAX_DIFF_LINES);
				final DiffDocument document = new DiffDocument();
				try (DiffRegionFormatter formatter = new DiffRegionFormatter(
						document, document.getLength(), maxLines)) {
					SubMonitor progress = SubMonitor.convert(monitor,
							diffs.size());
					for (FileDiff diff : diffs) {
						if (progress.isCanceled()
								|| diff.getBlobs().length > 2
								|| document.getNumberOfLines() > maxLines) {
							break;
						}
						progress.subTask(diff.getPath());
						try {
							formatter.write(diff);
						} catch (IOException ignore) {
							// Ignored
						}
						progress.worked(1);
					}
					if (progress.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					document.connect(formatter);
				}
				UIJob uiJob = new UIJob(UIText.GitHistoryPage_FormatDiffJobName) {
					@Override
					public IStatus runInUIThread(IProgressMonitor uiMonitor) {
						if (uiMonitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						if (UIUtils.isUsable(diffViewer)) {
							diffViewer.setDocument(document);
						}
						return Status.OK_STATUS;
					}

					@Override
					public boolean belongsTo(Object family) {
						return JobFamilies.HISTORY_DIFF.equals(family);
					}
				};
				uiJob.setRule(pageSchedulingRule);
				GitHistoryPage.this.schedule(uiJob);
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return JobFamilies.HISTORY_DIFF.equals(family);
			}
		};
		formatJob.setRule(pageSchedulingRule);
		schedule(formatJob);
	}

	private TreeWalk createFileWalker(RevWalk walk, Repository db, List<FilterPath> paths) {
		final TreeWalk fileWalker = new TreeWalk(db);
		fileWalker.setRecursive(true);
		fileWalker.setFilter(TreeFilter.ANY_DIFF);
		if (store.getBoolean(UIPreferences.RESOURCEHISTORY_FOLLOW_RENAMES)
				&& !paths.isEmpty()
				&& allRegularFiles(paths)) {
			pathFilters = paths;

			List<String> selectedPaths = new ArrayList<>(paths.size());
			for (FilterPath filterPath : paths)
				selectedPaths.add(filterPath.getPath());

			fileViewerInterestingPaths = new HashSet<>(selectedPaths);
			TreeFilter followFilter = createFollowFilterFor(selectedPaths);
			walk.setTreeFilter(followFilter);
			walk.setRevFilter(renameTracker.getFilter());

		} else if (paths.size() > 0) {
			pathFilters = paths;
			List<String> stringPaths = new ArrayList<>(paths.size());
			for (FilterPath p : paths)
				stringPaths.add(p.getPath());

			walk.setTreeFilter(AndTreeFilter.create(PathFilterGroup
					.createFromStrings(stringPaths), TreeFilter.ANY_DIFF));
			fileViewerInterestingPaths = new HashSet<>(stringPaths);
		} else {
			pathFilters = null;
			walk.setTreeFilter(TreeFilter.ALL);
			fileViewerInterestingPaths = null;
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

		DiffConfig diffConfig = getCurrentRepo().getConfig()
				.get(DiffConfig.KEY);

		List<TreeFilter> followFilters = new ArrayList<>(paths.size());
		for (String path : paths)
			followFilters.add(createFollowFilter(path, diffConfig));

		if (followFilters.size() == 1) {
			FollowFilter followFilter = (FollowFilter) followFilters.get(0);
			renameTracker.reset(followFilter.getPath());
			return followFilters.get(0);
		}

		// TODO: this scenario is not supported by JGit: RewriteTreeFilter
		// can not handle composite TreeFilters and expects a plain
		// FollowFilter for rename detection.
		return OrTreeFilter.create(followFilters);
	}

	private FollowFilter createFollowFilter(String path, DiffConfig diffConfig) {
		FollowFilter followFilter = FollowFilter.create(path, diffConfig);
		followFilter.setRenameCallback(new RenameCallback() {
			@Override
			public void renamed(DiffEntry entry) {
				renameTracker.getCallback().renamed(entry);
				if (fileViewerInterestingPaths != null) {
					fileViewerInterestingPaths.add(entry.getOldPath());
					fileViewerInterestingPaths.add(entry.getNewPath());
				}
			}
		});
		return followFilter;
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

	@Override
	public void loadItem(int item) {
		if (job != null && job.loadMoreItemsThreshold() < item) {
			loadHistory(item);
		}
	}

	@Override
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
	 * Load initial history items
	 *
	 * @param walk
	 *            the revwalk, non null
	 * @param toShow
	 *            commit to show, if any
	 */
	private void loadInitialHistory(@NonNull RevWalk walk, RevCommit toShow) {
		job = new GenerateHistoryJob(this, walk, resources);
		job.setRule(pageSchedulingRule);
		job.setLoadHint(INITIAL_ITEM);
		if (toShow != null) {
			job.setShowHint(toShow);
		}
		if (trace)
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.HISTORYVIEW.getLocation(),
					"Scheduling initial GenerateHistoryJob"); //$NON-NLS-1$
		schedule(job);
	}

	/**
	 * Load history items incrementally
	 *
	 * @param itemToLoad
	 *            hint for index of item that should be loaded
	 */
	private void loadHistory(final int itemToLoad) {
		if (job == null) {
			return;
		}
		job.setLoadHint(itemToLoad);
		if (trace)
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.HISTORYVIEW.getLocation(),
					"Scheduling incremental GenerateHistoryJob"); //$NON-NLS-1$
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
			p = Adapters.adapt(site, IWorkbenchSiteProgressService.class);
			if (p != null) {
				p.schedule(j, 0, true /* use half-busy cursor */);
				return;
			}
		}
		j.schedule();
	}


	private void releaseGenerateHistoryJob() {
		if (job != null) {
			if (job.getState() != Job.NONE)
				job.cancel();
			job.release();
			job = null;
		}
	}

	@Override
	public ShowInContext getShowInContext() {
		if (fileViewer != null && fileViewer.getControl().isFocusControl())
			return fileViewer.getShowInContext();
		else
			return null;
	}

	@Override
	public String[] getShowInTargetIds() {
		return new String[] { IHistoryView.VIEW_ID };
	}

	/**
	 * Get renamed path in given commit with initial starting path
	 *
	 * @param path
	 * @param commit
	 * @return actual path in commit
	 */
	public String getRenamedPath(String path, ObjectId commit) {
		return renameTracker.getPath(commit, path);
	}

	private static final class HistorySearchBar extends SearchBar {

		private IActionBars bars;

		private final IAction openCloseToggle;

		private boolean wasVisible = false;

		/**
		 * Listener to close the search bar on ESC. (Ctrl/Cmd-F is already
		 * handled via global retarget action.)
		 */
		private final KeyListener keyListener = new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				int key = SWTKeySupport.convertEventToUnmodifiedAccelerator(e);
				if (key == SWT.ESC) {
					setVisible(false);
					e.doit = false;
				}
			}
		};

		/**
		 * Listener to ensure that the history view is fully activated when the
		 * user clicks into the search bar's text widget. This makes sure our
		 * status manager gets activated and thus shows the status messages. We
		 * don't get a focus event when the user clicks in the field; and
		 * fiddling with the focus in a FocusListener could get hairy anyway.
		 */
		private final Listener mouseListener = new Listener() {

			private boolean hasFocus;

			private boolean hadFocusOnMouseDown;

			@Override
			public void handleEvent(Event e) {
				switch (e.type) {
				case SWT.FocusIn:
					toolbar.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							hasFocus = true;
						}
					});

					break;
				case SWT.FocusOut:
					hasFocus = false;
					break;
				case SWT.MouseDown:
					hadFocusOnMouseDown = hasFocus;
					break;
				case SWT.MouseUp:
					if (!hadFocusOnMouseDown) {
						graph.getControl().setFocus();
						toolbar.setFocus();
					}
					break;
				default:
					break;
				}
			}
		};

		public HistorySearchBar(String id, CommitGraphTable graph,
				IAction openCloseAction, IActionBars bars) {
			super(id, graph);
			this.bars = bars;
			this.openCloseToggle = openCloseAction;
			super.setVisible(false);
		}

		@Override
		public boolean isDynamic() {
			// We toggle our own visibility
			return true;
		}

		@Override
		protected FindToolbar createControl(Composite parent) {
			FindToolbar createdControl = super.createControl(parent);
			toolbar.addKeyListener(keyListener);
			toolbar.addListener(SWT.FocusIn, mouseListener);
			toolbar.addListener(SWT.FocusOut, mouseListener);
			toolbar.addListener(SWT.MouseDown, mouseListener);
			toolbar.addListener(SWT.MouseUp, mouseListener);

			if (wasVisible) {
				return toolbar;
			}
			wasVisible = true;
			// This fixes the wrong background when Eclipse starts up with the
			// search bar visible.
			toolbar.getDisplay().asyncExec(new Runnable() {

				@Override
				public void run() {
					if (toolbar != null && !toolbar.isDisposed()) {
						// See setVisible() above. Somehow, we need this, too.
						graph.getControl().setFocus();
						toolbar.setFocus();
					}
				}
			});
			return createdControl;
		}

		private void beforeHide() {
			lastText = toolbar.getText();
			lastSearchContext = searchContext;
			showStatus(toolbar, ""); //$NON-NLS-1$
			// It will be disposed by the IToolBarManager
			toolbar = null;
			openCloseToggle.setChecked(false);
			wasVisible = false;
		}

		private void workAroundBug551067(boolean visible) {
			// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=551067
			IContributionManager parent = getParent();
			if (parent instanceof SubToolBarManager) {
				SubToolBarManager subManager = (SubToolBarManager) parent;
				IContributionItem item = subManager.getParent().find(getId());
				if (item instanceof SubContributionItem) {
					item.setVisible(visible && subManager.isVisible());
				}
			}
		}

		@Override
		public void setVisible(boolean visible) {
			if (visible != isVisible()) {
				if (!visible) {
					beforeHide();
				}
				super.setVisible(visible);
				workAroundBug551067(visible);
				// Update the toolbar. Will dispose our FindToolbar widget on
				// hide, and will create a new one (through createControl())
				// on show. It'll also reposition the toolbar, if needed.
				// Note: just doing bars.getToolBarManager().update(true);
				// messes up big time (doesn't resize or re-position).
				if (bars != null) {
					bars.updateActionBars();
				}
				if (visible && toolbar != null) {
					openCloseToggle.setChecked(true);
					// If the toolbar was moved below the tabs, we now have
					// the wrong background. It disappears when one clicks
					// elsewhere. Looks like an inactive selection... No
					// way found to fix this but this ugly focus juggling:
					graph.getControl().setFocus();
					toolbar.setFocus();
				} else if (!visible && !graph.getControl().isDisposed()) {
					graph.getControl().setFocus();
				}
			}
		}

		@Override
		protected void showStatus(FindToolbar originator, String text) {
			bars.getStatusLineManager().setMessage(text);
		}

	}
}
