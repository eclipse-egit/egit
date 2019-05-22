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
 * Copyright (C) 2015-2018 Thomas Wolf <thomas.wolf@paranor.ch>
 * Copyright (C) 2015-2017, Stefan Dirix <sdirix@eclipsesource.com>
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.AdapterUtils;
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
import org.eclipse.egit.ui.internal.components.RepositoryMenuUtil.RepositoryToolbarAction;
import org.eclipse.egit.ui.internal.dialogs.HyperlinkSourceViewer;
import org.eclipse.egit.ui.internal.dialogs.HyperlinkTokenScanner;
import org.eclipse.egit.ui.internal.fetch.FetchHeadChangedEvent;
import org.eclipse.egit.ui.internal.history.FindToolbar.StatusListener;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefNode;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.selection.RepositorySelectionProvider;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
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
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
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
import org.eclipse.ui.editors.text.EditorsUI;
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
				if (historyPage.store.needsSaving())
					try {
						historyPage.store.save();
					} catch (IOException e) {
						Activator.handleError(e.getMessage(), e, false);
					}
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

		IWorkbenchAction showAllBranchesAction;

		IWorkbenchAction showAdditionalRefsAction;

		BooleanPrefAction followRenamesAction;

		IWorkbenchAction reuseCompareEditorAction;

		ShowFilterAction showAllRepoVersionsAction;

		ShowFilterAction showAllProjectVersionsAction;

		ShowFilterAction showAllFolderVersionsAction;

		ShowFilterAction showAllResourceVersionsAction;

		RepositoryToolbarAction switchRepositoryAction;

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
			createShowAllBranchesAction();
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

			wrapCommentAction.setEnabled(showCommentAction.isChecked());
			fillCommentAction.setEnabled(showCommentAction.isChecked());
		}

		private void createRepositorySwitchAction() {
			switchRepositoryAction = new RepositoryToolbarAction(true,
					() -> historyPage.currentRepo,
					repo -> {
						Repository current = historyPage.currentRepo;
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
					if (historyPage.store.needsSaving()) {
						try {
							historyPage.store.save();
						} catch (IOException e) {
							Activator.handleError(e.getMessage(), e, false);
						}
					}
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

	/** Split between {@link #commentViewer} and {@link #fileViewer}. */
	private SashForm revInfoSplit;

	/** The table showing the DAG, first "paragraph", author, author date. */
	private CommitGraphTable graph;

	/** Viewer displaying the currently selected commit of {@link #graph}. */
	private CommitMessageViewer commentViewer;

	private DiffViewer diffViewer;

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

	private boolean currentShowAllBranches;

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
				setWrap(((Boolean) event.getNewValue()).booleanValue());
			}

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

	private ScrolledComposite commentAndDiffScrolledComposite;

	private Composite commentAndDiffComposite;

	private volatile boolean resizing;

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
		if (object instanceof HistoryPageInput)
			return true;

		if (object instanceof IResource)
			return typeOk((IResource) object);

		if (object instanceof RepositoryTreeNode)
			return true;

		if (object instanceof Path) {
			return true;
		}

		IResource resource = AdapterUtils.adaptToAnyResource(object);
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
		pageSchedulingRule = new HistoryPageRule();
		if (trace) {
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.HISTORYVIEW.getLocation());
		}
	}

	private interface ICommitsProvider {

		Object getSearchContext();

		SWTCommit[] getCommits();

		RevFlag getHighlight();
	}

	private static class SearchBar extends ControlContribution {

		private IActionBars bars;

		private FindToolbar toolbar;

		private Object searchContext;

		private String lastText;

		private ObjectId lastObjectId;

		private Object lastSearchContext;

		private ICommitsProvider provider;

		private boolean wasVisible = false;

		private final CommitGraphTable graph;

		private final IAction openCloseToggle;

		/**
		 * "Go to next/previous" from the {@link FindToolbar} sends
		 * {@link SWT#Selection} events with the chosen {@link RevCommit} as
		 * data.
		 */
		private final Listener selectionListener = new Listener() {

			@Override
			public void handleEvent(Event evt) {
				final RevCommit commit = (RevCommit) evt.data;
				lastObjectId = commit.getId();
				graph.selectCommit(commit);
			}
		};

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
		 * Listener to display status messages from the asynchronous find. (Is
		 * called in the UI thread.)
		 */
		private final StatusListener statusListener = new StatusListener() {

			@Override
			public void setMessage(FindToolbar originator, String text) {
				IStatusLineManager status = bars.getStatusLineManager();
				if (status != null) {
					status.setMessage(text);
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

		public SearchBar(String id, CommitGraphTable graph,
				IAction openCloseAction, IActionBars bars) {
			super(id);
			super.setVisible(false);
			this.graph = graph;
			this.openCloseToggle = openCloseAction;
			this.bars = bars;
		}

		private void beforeHide() {
			lastText = toolbar.getText();
			lastSearchContext = searchContext;
			statusListener.setMessage(toolbar, ""); //$NON-NLS-1$
			// It will be disposed by the IToolBarManager
			toolbar = null;
			openCloseToggle.setChecked(false);
			wasVisible = false;
		}

		@Override
		public void setVisible(boolean visible) {
			if (visible != isVisible()) {
				if (!visible) {
					beforeHide();
				}
				super.setVisible(visible);
				// Update the toolbar. Will dispose our FindToolbar widget on
				// hide, and will create a new one (through createControl())
				// on show. It'll also reposition the toolbar, if needed.
				// Note: just doing bars.getToolBarManager().update(true);
				// messes up big time (doesn't resize or re-position).
				bars.updateActionBars();
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
		public boolean isDynamic() {
			// We toggle our own visibility
			return true;
		}

		@Override
		protected Control createControl(Composite parent) {
			toolbar = new FindToolbar(parent);
			toolbar.setBackground(null);
			toolbar.addKeyListener(keyListener);
			toolbar.addListener(SWT.FocusIn, mouseListener);
			toolbar.addListener(SWT.FocusOut, mouseListener);
			toolbar.addListener(SWT.MouseDown, mouseListener);
			toolbar.addListener(SWT.MouseUp, mouseListener);
			toolbar.addListener(SWT.Modify,
					(e) -> lastText = toolbar.getText());
			toolbar.addStatusListener(statusListener);
			toolbar.addSelectionListener(selectionListener);
			boolean hasInput = provider != null;
			if (hasInput) {
				setInput(provider);
			}
			if (lastText != null) {
				if (lastSearchContext != null
						&& lastSearchContext.equals(searchContext)) {
					toolbar.setPreselect(lastObjectId);
				}
				toolbar.setText(lastText, hasInput);
			}
			lastSearchContext = null;
			lastObjectId = null;
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
			return toolbar;
		}

		public void setInput(ICommitsProvider provider) {
			this.provider = provider;
			if (toolbar != null) {
				searchContext = provider.getSearchContext();
				toolbar.setInput(provider.getHighlight(),
						graph.getTableView().getTable(), provider.getCommits());
			}
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

		commentAndDiffScrolledComposite = new ScrolledComposite(revInfoSplit,
				SWT.H_SCROLL | SWT.V_SCROLL);
		commentAndDiffScrolledComposite.setExpandHorizontal(true);
		commentAndDiffScrolledComposite.setExpandVertical(true);

		commentAndDiffComposite = new Composite(commentAndDiffScrolledComposite, SWT.NONE);
		commentAndDiffScrolledComposite.setContent(commentAndDiffComposite);
		commentAndDiffComposite.setLayout(GridLayoutFactory.fillDefaults()
				.create());

		commentViewer = new CommitMessageViewer(commentAndDiffComposite,
				getPartSite());
		commentViewer.getControl().setLayoutData(
				GridDataFactory.fillDefaults().grab(true, false).create());

		commentViewer.addTextListener(new ITextListener() {
			@Override
			public void textChanged(TextEvent event) {
				resizeCommentAndDiffScrolledComposite();
			}
		});

		commentAndDiffComposite.setBackground(commentViewer.getControl()
				.getBackground());


		HyperlinkSourceViewer.Configuration configuration = new HyperlinkSourceViewer.Configuration(
				EditorsUI.getPreferenceStore()) {

			@Override
			public int getHyperlinkStateMask(ISourceViewer sourceViewer) {
				return SWT.NONE;
			}

			@Override
			protected IHyperlinkDetector[] internalGetHyperlinkDetectors(
					ISourceViewer sourceViewer) {
				IHyperlinkDetector[] registered = super.internalGetHyperlinkDetectors(
						sourceViewer);
				// Always add our special detector for commit hyperlinks; we
				// want those to show always.
				if (registered == null) {
					return new IHyperlinkDetector[] {
							new CommitMessageViewer.KnownHyperlinksDetector() };
				} else {
					IHyperlinkDetector[] result = new IHyperlinkDetector[registered.length
							+ 1];
					System.arraycopy(registered, 0, result, 0,
							registered.length);
					result[registered.length] = new CommitMessageViewer.KnownHyperlinksDetector();
					return result;
				}
			}

			@Override
			public String[] getConfiguredContentTypes(
					ISourceViewer sourceViewer) {
				return new String[] { IDocument.DEFAULT_CONTENT_TYPE,
						CommitMessageViewer.HEADER_CONTENT_TYPE,
						CommitMessageViewer.FOOTER_CONTENT_TYPE };
			}

			@Override
			public IPresentationReconciler getPresentationReconciler(
					ISourceViewer viewer) {
				PresentationReconciler reconciler = new PresentationReconciler();
				reconciler.setDocumentPartitioning(
						getConfiguredDocumentPartitioning(viewer));
				DefaultDamagerRepairer hyperlinkDamagerRepairer = new DefaultDamagerRepairer(
						new HyperlinkTokenScanner(this, viewer));
				reconciler.setDamager(hyperlinkDamagerRepairer,
						IDocument.DEFAULT_CONTENT_TYPE);
				reconciler.setRepairer(hyperlinkDamagerRepairer,
						IDocument.DEFAULT_CONTENT_TYPE);
				TextAttribute headerDefault = new TextAttribute(
						PlatformUI.getWorkbench().getDisplay()
								.getSystemColor(SWT.COLOR_DARK_GRAY));
				DefaultDamagerRepairer headerDamagerRepairer = new DefaultDamagerRepairer(
						new HyperlinkTokenScanner(this, viewer, headerDefault));
				reconciler.setDamager(headerDamagerRepairer,
						CommitMessageViewer.HEADER_CONTENT_TYPE);
				reconciler.setRepairer(headerDamagerRepairer,
						CommitMessageViewer.HEADER_CONTENT_TYPE);
				DefaultDamagerRepairer footerDamagerRepairer = new DefaultDamagerRepairer(
						new FooterTokenScanner(this, viewer));
				reconciler.setDamager(footerDamagerRepairer,
						CommitMessageViewer.FOOTER_CONTENT_TYPE);
				reconciler.setRepairer(footerDamagerRepairer,
						CommitMessageViewer.FOOTER_CONTENT_TYPE);
				return reconciler;
			}

		};

		commentViewer.configure(configuration);

		diffViewer = new DiffViewer(commentAndDiffComposite, null, SWT.NONE);
		diffViewer.configure(
				new DiffViewer.Configuration(EditorsUI.getPreferenceStore()));
		diffViewer.getControl().setLayoutData(
				GridDataFactory.fillDefaults().grab(true, false).create());

		setWrap(store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_WRAP));

		commentAndDiffScrolledComposite.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				if (!resizing && commentViewer.getTextWidget()
						.getWordWrap()) {
					resizeCommentAndDiffScrolledComposite();
				}
			}
		});

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
		trackFocus(diffViewer.getControl());
		trackFocus(commentViewer.getControl());
		trackFocus(fileViewer.getControl());
		layout();

		myRefsChangedHandle = Repository.getGlobalListenerList()
				.addRefsChangedListener(this);

		IToolBarManager manager = getSite().getActionBars().getToolBarManager();
		searchBar = new SearchBar(GitHistoryPage.class.getName() + ".searchBar", //$NON-NLS-1$
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
				if (store.needsSaving())
					try {
						store.save();
					} catch (IOException e1) {
						Activator.handleError(e1.getMessage(), e1, false);
					}

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
			revInfoSplit.setMaximizedControl(commentViewer.getControl());
		} else if (!showComment && showFiles) {
			graphDetailSplit.setMaximizedControl(null);
			revInfoSplit.setMaximizedControl(fileViewer.getControl());
		} else if (!showComment && !showFiles)
			graphDetailSplit.setMaximizedControl(graph.getControl());
		historyControl.layout();
	}

	private void attachCommitSelectionChanged() {
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
				try (RevWalk walk = new RevWalk(input.getRepository())) {
					final RevCommit unfilteredCommit = walk.parseCommit(c);
					for (RevCommit parent : unfilteredCommit.getParents())
						walk.parseBody(parent);
					fileViewer.newInput(new FileDiffInput(input.getRepository(),
							fileDiffWalker, unfilteredCommit,
							fileViewerInterestingPaths,
							input.getSingleFile() != null));
				} catch (IOException e) {
					fileViewer.newInput(new FileDiffInput(input.getRepository(),
							fileDiffWalker, c, fileViewerInterestingPaths,
							input.getSingleFile() != null));
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
		mgr.add(new Separator());
		mgr.add(actions.showAllRepoVersionsAction);
		mgr.add(actions.showAllProjectVersionsAction);
		mgr.add(actions.showAllFolderVersionsAction);
		mgr.add(actions.showAllResourceVersionsAction);
		mgr.add(new Separator());
		mgr.add(actions.compareModeAction);
		mgr.add(actions.showAllBranchesAction);
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
		showInMessageManager.add(actions.showBranchSequenceAction);
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
		super.dispose();
	}

	@Override
	public void setFocus() {
		if (repoHasBeenRemoved(currentRepo)) {
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
		currentRepo = null;
		name = ""; //$NON-NLS-1$
		input = null;
		commentViewer.setInput(null);
		fileViewer.newInput(null);
		setInput(null);
	}

	private void clearViewers() {
		TableViewer viewer = graph.getTableView();
		viewer.setInput(new SWTCommit[0]);
		// Force a selection changed event
		viewer.setSelection(viewer.getSelection());
	}

	@Override
	public Control getControl() {
		return topControl;
	}

	@Override
	public void refresh() {
		if (repoHasBeenRemoved(currentRepo)) {
			clearHistoryPage();
		}
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
				RepositoryMapping mapping = RepositoryMapping.getMapping(path);
				if (mapping != null) {
					repo = mapping.getRepository();
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
				repo = AdapterUtils.adapt(o, Repository.class);
				if (repo != null) {
					File file = AdapterUtils.adapt(o, File.class);
					if (file == null) {
						input = new HistoryPageInput(repo);
					} else {
						input = new HistoryPageInput(repo, new File[] { file });
					}
				}
			}
			selection = AdapterUtils.adapt(o, RevCommit.class);

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
				currentHeadId = null;
				currentFetchHeadId = null;
				currentRepo = db;
				clearViewers();
				return;
			}
			AnyObjectId fetchHeadId = resolveFetchHead(db);

			List<FilterPath> paths = buildFilterPaths(input.getItems(), input
					.getFileList(), db);

			boolean repoChanged = false;
			if (!db.equals(currentRepo)) {
				repoChanged = true;
				currentRepo = db;
			}

			if (forceNewWalk || repoChanged
					|| shouldRedraw(headId, fetchHeadId, paths)) {
				releaseGenerateHistoryJob();

				if (repoChanged) {
					// Clear all viewers. Otherwise it may be possible that the
					// user invokes a context menu command and due to to the
					// highly asynchronous loading we end up with inconsistent
					// diff computations trying to find the diff for a commit in
					// the wrong repository.
					clearViewers();
				}

				SWTWalk walk = createNewWalk(db, headId, fetchHeadId);

				fileDiffWalker = createFileWalker(walk, db, paths);

				loadInitialHistory(walk);
			} else {
				// needed for context menu and double click
				graph.setHistoryPageInput(input);
			}
		} finally {
			if (trace)
				GitTraceLocation.getTrace().traceExit(
						GitTraceLocation.HISTORYVIEW.getLocation());

		}
	}

	private boolean shouldRedraw(AnyObjectId headId, AnyObjectId fetchHeadId,
			List<FilterPath> paths) {
		boolean pathChanged = pathChanged(pathFilters, paths);
		boolean headChanged = headId == null || !headId.equals(currentHeadId);

		boolean allBranchesChanged = currentShowAllBranches != store
			.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_ALL_BRANCHES);
		currentShowAllBranches = store
			.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_ALL_BRANCHES);

		boolean additionalRefsChange = currentShowAdditionalRefs != store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_ADDITIONAL_REFS);
		currentShowAdditionalRefs = store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_ADDITIONAL_REFS);
		boolean fetchHeadChanged = currentShowAdditionalRefs
				&& fetchHeadId != null
				&& !fetchHeadId.equals(currentFetchHeadId);

		boolean showNotesChanged = currentShowNotes != store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_NOTES);
		currentShowNotes = store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_NOTES);
		boolean followRenamesChanged = currentFollowRenames != getFollowRenames();
		currentFollowRenames = getFollowRenames();

		return pathChanged || headChanged || fetchHeadChanged
				|| allBranchesChanged || additionalRefsChange
				|| showNotesChanged || followRenamesChanged;
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
			paths = new ArrayList<>(0);
		return paths;
	}

	private boolean pathChanged(final List<FilterPath> o, final List<FilterPath> n) {
		if (o == null)
			return !n.isEmpty();
		return !o.equals(n);
	}

	private @NonNull SWTWalk createNewWalk(Repository db, AnyObjectId headId,
			AnyObjectId fetchHeadId) {
		currentHeadId = headId;
		currentFetchHeadId = fetchHeadId;
		SWTWalk walk = new GitHistoryWalk(db, headId);
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
		if (diffs.isEmpty()) {
			if (UIUtils.isUsable(diffViewer)) {
				IDocument document = new Document();
				diffViewer.setDocument(document);
				resizeCommentAndDiffScrolledComposite();
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
								|| diff.getCommit().getParentCount() > 1
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
							resizeCommentAndDiffScrolledComposite();
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

	private void setWrap(boolean wrap) {
		commentViewer.getTextWidget().setWordWrap(wrap);
		diffViewer.getTextWidget().setWordWrap(wrap);
		resizeCommentAndDiffScrolledComposite();
	}

	private void resizeCommentAndDiffScrolledComposite() {
		resizing = true;
		long start = 0;
		int lines = 0;
		if (trace) {
			IDocument document = diffViewer.getDocument();
			lines = document != null ? document.getNumberOfLines() : 0;
			System.out.println("Lines: " + lines); //$NON-NLS-1$
			if (lines > 1) {
				new Exception("resizeCommentAndDiffScrolledComposite") //$NON-NLS-1$
						.printStackTrace(System.out);
			}
			start = System.currentTimeMillis();
		}

		Point size = commentAndDiffComposite
				.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		commentAndDiffComposite.layout();
		commentAndDiffScrolledComposite.setMinSize(size);
		resizing = false;

		if (trace) {
			long stop = System.currentTimeMillis();
			long time = stop - start;
			long lps = (lines * 1000) / (time + 1);
			System.out
					.println("Resize + diff: " + time + " ms, line/s: " + lps); //$NON-NLS-1$ //$NON-NLS-2$
		}
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

		DiffConfig diffConfig = currentRepo.getConfig().get(DiffConfig.KEY);

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
	 */
	private void loadInitialHistory(@NonNull RevWalk walk) {
		job = new GenerateHistoryJob(this, walk, resources);
		job.setRule(pageSchedulingRule);
		job.setLoadHint(INITIAL_ITEM);
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
			p = AdapterUtils.adapt(site, IWorkbenchSiteProgressService.class);
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

	private static class FooterTokenScanner extends HyperlinkTokenScanner {

		private static final Pattern ITALIC_LINE = Pattern
				.compile("^[A-Z](?:[A-Za-z]+-)+by: "); //$NON-NLS-1$

		private final IToken italicToken;

		public FooterTokenScanner(SourceViewerConfiguration configuration,
				ISourceViewer viewer) {
			super(configuration, viewer);
			Object defaults = defaultToken.getData();
			TextAttribute italic;
			if (defaults instanceof TextAttribute) {
				TextAttribute defaultAttribute = (TextAttribute) defaults;
				int style = defaultAttribute.getStyle() ^ SWT.ITALIC;
				italic = new TextAttribute(defaultAttribute.getForeground(),
						defaultAttribute.getBackground(), style,
						defaultAttribute.getFont());
			} else {
				italic = new TextAttribute(null, null, SWT.ITALIC);
			}
			italicToken = new Token(italic);
		}

		@Override
		protected IToken scanToken() {
			// If we're at a "Signed-off-by" or similar footer line, make it
			// italic.
			try {
				IRegion region = document
						.getLineInformationOfOffset(currentOffset);
				if (currentOffset == region.getOffset()) {
					String line = document.get(currentOffset,
							region.getLength());
					Matcher m = ITALIC_LINE.matcher(line);
					if (m.find()) {
						currentOffset = Math.min(endOfRange,
								currentOffset + region.getLength());
						return italicToken;
					}
				}
			} catch (BadLocationException e) {
				// Ignore and return null below.
			}
			return null;
		}
	}
}
