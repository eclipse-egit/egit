/*******************************************************************************
 * Copyright (C) 2011, 2022 Bernard Leach <leachbj@bouncycastle.org> and others.
 * Copyright (C) 2015 SAP SE (Christian Georgi <christian.georgi@sap.com>)
 * Copyright (C) 2015 Denis Zygann <d.zygann@web.de>
 * Copyright (C) 2016 IBM (Daniel Megert <daniel_megert@ch.ibm.com>)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Baumann <tobbaumann@gmail.com> - Bug 373969, 473544
 *    Thomas Wolf <thomas.wolf@paranor.ch>
 *    Tobias Hein <th.mailinglists@googlemail.com> - Bug 499697
 *    Ralf M Petter <ralf.petter@gmail.com> - Bug 509945
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import static org.eclipse.egit.ui.internal.CommonUtils.runCommand;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.UnitOfWork;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.internal.gerrit.GerritUtil;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffChangedListener;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.op.AssumeUnchangedOperation;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.egit.core.op.UntrackOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.ActionUtils;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.actions.ActionCommands;
import org.eclipse.egit.ui.internal.actions.BooleanPrefAction;
import org.eclipse.egit.ui.internal.commands.shared.AbortRebaseCommand;
import org.eclipse.egit.ui.internal.commands.shared.AbstractRebaseCommandHandler;
import org.eclipse.egit.ui.internal.commands.shared.ContinueRebaseCommand;
import org.eclipse.egit.ui.internal.commands.shared.SkipRebaseCommand;
import org.eclipse.egit.ui.internal.commit.CommitHelper;
import org.eclipse.egit.ui.internal.commit.CommitJob;
import org.eclipse.egit.ui.internal.commit.CommitMessageHistory;
import org.eclipse.egit.ui.internal.commit.CommitProposalProcessor;
import org.eclipse.egit.ui.internal.commit.DiffViewer;
import org.eclipse.egit.ui.internal.commit.PushSettings;
import org.eclipse.egit.ui.internal.components.DropDownMenuAction;
import org.eclipse.egit.ui.internal.components.PartVisibilityListener;
import org.eclipse.egit.ui.internal.components.RepositoryMenuUtil.RepositoryToolbarAction;
import org.eclipse.egit.ui.internal.decorators.ProblemLabelDecorator;
import org.eclipse.egit.ui.internal.dialogs.CommandConfirmation;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageArea;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponent;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponentState;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponentStateManager;
import org.eclipse.egit.ui.internal.dialogs.ICommitMessageComponentNotifications;
import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea;
import org.eclipse.egit.ui.internal.operations.DeletePathsOperationUI;
import org.eclipse.egit.ui.internal.operations.IgnoreOperationUI;
import org.eclipse.egit.ui.internal.push.PushMode;
import org.eclipse.egit.ui.internal.push.PushWizardDialog;
import org.eclipse.egit.ui.internal.push.SimpleConfigurePushDialog;
import org.eclipse.egit.ui.internal.repository.RepositoryTreeNodeLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.selection.MultiViewerSelectionProvider;
import org.eclipse.egit.ui.internal.selection.RepositorySelectionProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.IViewerLabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.lib.CommitConfig;
import org.eclipse.jgit.lib.CommitConfig.CleanupMode;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff.StageState;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * A GitX style staging view with embedded commit dialog.
 */
public class StagingView extends ViewPart
		implements IShowInSource, IShowInTarget {

	/**
	 * Staging view id
	 */
	public static final String VIEW_ID = "org.eclipse.egit.ui.StagingView"; //$NON-NLS-1$

	private static final String CSS_CLASS = "org-eclipse-egit-ui-StagingView"; //$NON-NLS-1$

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private static final String SORT_ITEM_TOOLBAR_ID = "sortItem"; //$NON-NLS-1$

	private static final String HIDE_UNTRACKED_TOOLBAR_ID = "hideUntracked"; //$NON-NLS-1$

	private static final String EXPAND_ALL_ITEM_TOOLBAR_ID = "expandAllItem"; //$NON-NLS-1$

	private static final String COLLAPSE_ALL_ITEM_TOOLBAR_ID = "collapseAllItem"; //$NON-NLS-1$

	private static final String STORE_SORT_STATE = SORT_ITEM_TOOLBAR_ID
			+ "State"; //$NON-NLS-1$

	private static final String MAIN_SASH_FORM_ORIENTATION_VERTICAL = "MAIN_SASH_FORM_ORIENTATION_VERTICAL"; //$NON-NLS-1$

	private static final String HORIZONTAL_SASH_FORM_WEIGHT = "HORIZONTAL_SASH_FORM_WEIGHT"; //$NON-NLS-1$

	private static final String STAGING_SASH_FORM_WEIGHT = "STAGING_SASH_FORM_WEIGHT"; //$NON-NLS-1$

	private FormToolkit toolkit;

	private Form form;

	private RepositoryNode titleNode;

	private final Map<File, ViewerLabel> titleLabels = new HashMap<>();

	private DecoratingLabelProvider titleLabelProvider;

	private SashForm mainSashForm;

	private Section stagedSection;

	private Section unstagedSection;

	private Section commitMessageSection;

	private TreeViewer stagedViewer;

	private TreeViewer unstagedViewer;

	private ToggleableLabel warningLabel;

	private Text filterText;

	/** Remember compiled pattern of the current filter string for performance. */
	private Pattern filterPattern;

	private SpellcheckableMessageArea commitMessageText;

	private Composite commitMessagePreview;

	private StackLayout previewLayout;

	private CommitMessagePreviewer previewer;

	private Text committerText;

	private Text authorText;

	private CommitMessageComponent commitMessageComponent;

	private boolean reactOnSelection = true;

	/** Tracks the last selection while the view is not active. */
	private StructuredSelection lastSelection;

	private ISelectionListener selectionChangedListener;

	private PartVisibilityListener partListener;

	private ToolBarManager unstagedToolBarManager;

	private ToolBarManager stagedToolBarManager;

	private IAction listPresentationAction;

	private IAction treePresentationAction;

	private IAction compactTreePresentationAction;

	private IAction unstagedExpandAllAction;

	private IAction unstagedCollapseAllAction;

	private IAction stagedExpandAllAction;

	private IAction stagedCollapseAllAction;

	private IAction unstageAction;

	private IAction stageAction;

	private IAction unstageAllAction;

	private IAction stageAllAction;

	private IAction compareModeAction;

	private IWorkbenchAction switchRepositoriesAction;

	/** The currently set repository, even if it is bare. */
	private Repository realRepository;

	/** The currently set repository, if it's not a bare repository. */
	@Nullable
	private Repository currentRepository;

	/** The push mode; key is whether the Change-ID button is checked. */
	private final Map<Boolean, PushMode> currentPushMode = new HashMap<>();

	private boolean pushesHeadOnly;

	private Presentation presentation = Presentation.LIST;

	private PresentationAction presentationAction;

	private Set<IPath> pathsToExpandInStaged = new HashSet<>();

	private Set<IPath> pathsToExpandInUnstaged = new HashSet<>();

	private boolean isUnbornHead;

	private String currentBranch;

	/**
	 * Presentation mode of the staged/unstaged files.
	 */
	public enum Presentation {
		/** Show files in flat list */
		LIST,
		/** Show folder structure in full tree */
		TREE,
		/**
		 * Show folder structure in compact tree (folders with only one child
		 * are folded into parent)
		 */
		COMPACT_TREE;
	}

	static class StagingViewUpdate {
		Repository repository;
		IndexDiffData indexDiff;
		Collection<String> changedResources;

		StagingViewUpdate(Repository theRepository,
				IndexDiffData theIndexDiff, Collection<String> theChanges) {
			this.repository = theRepository;
			this.indexDiff = theIndexDiff;
			this.changedResources = theChanges;
		}
	}

	private static class StagingDragSelection implements IStructuredSelection {

		private final IStructuredSelection delegate;

		private final boolean fromUnstaged;

		public StagingDragSelection(IStructuredSelection original,
				boolean fromUnstaged) {
			this.delegate = original;
			this.fromUnstaged = fromUnstaged;
		}

		@Override
		public boolean isEmpty() {
			return delegate.isEmpty();
		}

		@Override
		public Object getFirstElement() {
			return delegate.getFirstElement();
		}

		@Override
		public Iterator iterator() {
			return delegate.iterator();
		}

		@Override
		public int size() {
			return delegate.size();
		}

		@Override
		public Object[] toArray() {
			return delegate.toArray();
		}

		@Override
		public List toList() {
			return delegate.toList();
		}

		public boolean isFromUnstaged() {
			return fromUnstaged;
		}
	}

	private static class StagingDragListener extends DragSourceAdapter {

		private final ISelectionProvider provider;

		private final StagingViewContentProvider contentProvider;

		private final boolean unstaged;

		public StagingDragListener(ISelectionProvider provider,
				StagingViewContentProvider contentProvider, boolean unstaged) {
			this.provider = provider;
			this.contentProvider = contentProvider;
			this.unstaged = unstaged;
		}

		@Override
		public void dragStart(DragSourceEvent event) {
			event.doit = !provider.getSelection().isEmpty();
		}

		@Override
		public void dragFinished(DragSourceEvent event) {
			if (LocalSelectionTransfer.getTransfer().isSupportedType(
					event.dataType)) {
				LocalSelectionTransfer.getTransfer().setSelection(null);
			}
		}

		@Override
		public void dragSetData(DragSourceEvent event) {
			IStructuredSelection selection = (IStructuredSelection) provider
					.getSelection();
			if (selection.isEmpty()) {
				// Should never happen as per dragStart()
				return;
			}
			if (LocalSelectionTransfer.getTransfer().isSupportedType(
					event.dataType)) {
				LocalSelectionTransfer.getTransfer().setSelection(
						new StagingDragSelection(selection, unstaged));
				return;
			}

			if (FileTransfer.getInstance().isSupportedType(event.dataType)) {
				Set<String> files = new HashSet<>();
				for (Object selected : selection.toList())
					if (selected instanceof StagingEntry) {
						add((StagingEntry) selected, files);
					} else if (selected instanceof StagingFolderEntry) {
						// Only add the files, otherwise much more than intended
						// might be copied or moved. The user selected a staged
						// or unstaged folder, so only the staged or unstaged
						// files inside that folder should be included, not
						// everything.
						StagingFolderEntry folder = (StagingFolderEntry) selected;
						for (StagingEntry entry : contentProvider
								.getStagingEntriesFiltered(folder)) {
							add(entry, files);
						}
					}
				if (!files.isEmpty()) {
					event.data = files.toArray(new String[0]);
					return;
				}
				// We may still end up with an empty list here if the selection
				// contained only deleted files. In that case, the drag&drop
				// will log an SWTException: Data does not have correct format
				// for type. Note that GTK sometimes creates the FileTransfer
				// up front even though a drag between our own viewers would
				// need only the LocalSelectionTransfer. Drag&drop between our
				// viewers still works (also on GTK) even if the creation of
				// the FileTransfer fails.
			}
		}

		private void add(StagingEntry entry, Collection<String> files) {
			File file = entry.getLocation().toFile();
			if (file.exists()) {
				files.add(file.getAbsolutePath());
			}
		}
	}

	private final class PartListener extends PartVisibilityListener {

		public PartListener() {
			super(StagingView.this);
		}

		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			if (isMe(partRef)) {
				if (lastSelection != null) {
					// view activated: synchronize with last active part
					// selection
					reactOnSelection(lastSelection);
					lastSelection = null;
				}
				return;
			}
			IWorkbenchPart part = partRef.getPart(false);
			StructuredSelection sel = getSelectionOfPart(part);
			if (!isVisible()) {
				// remember last selection in the part so that we can
				// synchronize on it as soon as we will be visible
				lastSelection = sel;
			} else {
				lastSelection = null;
				if (sel != null) {
					reactOnSelection(sel);
				}
			}

		}
	}

	/**
	 * A wrapped {@link DecoratingLabelProvider} to be used in the tree viewers
	 * of the staging view. We wrap it instead of deriving directly because a
	 * {@link DecoratingLabelProvider} is a
	 * {@link org.eclipse.jface.viewers.ITreePathLabelProvider
	 * ITreePathLabelProvider}, which makes the tree viewer compute a
	 * {@link org.eclipse.jface.viewers.TreePath TreePath} for each element,
	 * which is then ultimately unused because the
	 * {@link StagingViewLabelProvider} is <em>not</em> a
	 * {@link org.eclipse.jface.viewers.ITreePathLabelProvider
	 * ITreePathLabelProvider}. Computing the
	 * {@link org.eclipse.jface.viewers.TreePath TreePath} is a fairly expensive
	 * operation on GTK, and avoiding to compute it speeds up label updates
	 * significantly.
	 */
	private static class TreeDecoratingLabelProvider extends ColumnLabelProvider
			implements IViewerLabelProvider {

		private final DecoratingLabelProvider provider;

		private final StagingViewLabelProvider base;

		public TreeDecoratingLabelProvider(StagingViewLabelProvider base,
				ILabelDecorator decorator) {
			this.base = base;
			this.provider = new DecoratingLabelProvider(base, decorator);
		}

		public StagingViewLabelProvider getBaseLabelProvider() {
			return base;
		}

		@Override
		public Image getImage(Object element) {
			return provider.getImage(element);
		}

		@Override
		public String getText(Object element) {
			return provider.getText(element);
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
			provider.addListener(listener);
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
			provider.removeListener(listener);
		}

		@Override
		public void dispose() {
			provider.dispose();
		}

		@Override
		public void updateLabel(ViewerLabel label, Object element) {
			provider.updateLabel(label, element);
		}

	}

	static class StagingViewSearchThread extends Thread {
		private StagingView stagingView;

		private static final Object lock = new Object();

		private volatile static int globalThreadIndex = 0;

		private int currentThreadIx;

		public StagingViewSearchThread(StagingView stagingView) {
			super("staging_view_filter_thread" + ++globalThreadIndex); //$NON-NLS-1$
			this.stagingView = stagingView;
			currentThreadIx = globalThreadIndex;
		}

		@Override
		public void run() {
			synchronized (lock) {
				if (currentThreadIx < globalThreadIndex)
					return;
				stagingView.refreshViewersPreservingExpandedElements();
			}
		}

	}

	private final IPreferenceChangeListener prefListener = new IPreferenceChangeListener() {

		@Override
		public void preferenceChange(PreferenceChangeEvent event) {
			if (!RepositoryUtil.PREFS_DIRECTORIES_REL.equals(event.getKey())) {
				return;
			}
			Repository repo = currentRepository;
			if (repo == null || RepositoryUtil.INSTANCE.contains(repo)) {
				return;
			}
			reload(null);
		}

	};

	private final IPropertyChangeListener uiPrefsListener = new IPropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (UIPreferences.COMMIT_DIALOG_WARN_ABOUT_MESSAGE_SECOND_LINE
					.equals(event.getProperty())) {
				asyncExec(() -> {
					if (!commitMessageSection.isDisposed()) {
						updateMessage();
					}
				});
			}
		}
	};

	private Action signedOffByAction;

	private Action addChangeIdAction;

	private Action amendPreviousCommitAction;

	private Action previewAction;

	private Action signCommitAction;

	private Action openNewCommitsAction;

	private Action columnLayoutAction;

	private Action fileNameModeAction;

	private Action refreshAction;

	private Action sortAction;

	private Action hideUntrackedAction;

	private SashForm stagingSashForm;

	private IndexDiffChangedListener myIndexDiffListener = new IndexDiffChangedListener() {
		@Override
		public void indexDiffChanged(Repository repository,
				IndexDiffData indexDiffData) {
			reload(repository);
		}
	};

	private IndexDiffCacheEntry cacheEntry;

	private UndoRedoActionGroup undoRedoActionGroup;

	private Button commitButton;

	private Button commitAndPushButton;

	private PushSettings pushSettings;

	private Section rebaseSection;

	private Button rebaseContinueButton;

	private Button rebaseSkipButton;

	private Button rebaseAbortButton;

	private Button ignoreErrors;

	private ListenerHandle refsChangedListener;

	private ListenerHandle configChangedListener;

	private LocalResourceManager resources = new LocalResourceManager(
			JFaceResources.getResources());

	private boolean disposed;

	private Image getImage(ImageDescriptor descriptor) {
		return (Image) this.resources.get(descriptor);
	}

	private void createPersonLabel(Composite parent, ImageDescriptor image,
			String text) {
		Label imageLabel = new Label(parent, SWT.NONE);
		imageLabel.setImage(UIIcons.getImage(resources, image));

		Label textLabel = toolkit.createLabel(parent, text);
		textLabel.setForeground(
				toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
	}

	@Override
	public void createPartControl(final Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);

		titleNode = null;
		titleLabelProvider = new DecoratingLabelProvider(
				new RepositoryTreeNodeLabelProvider(),
				PlatformUI.getWorkbench().getDecoratorManager());
		titleLabelProvider.addListener(e -> {
			if (titleNode != null && form != null && !form.isDisposed()) {
				updateTitle(false);
			}
		});
		toolkit = new FormToolkit(parent.getDisplay());
		parent.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (commitMessageComponent.isAmending()
						|| userEnteredCommitMessage())
					saveCommitMessageComponentState();
				else
					deleteCommitMessageComponentState();
				resources.dispose();
				toolkit.dispose();
			}
		});

		form = toolkit.createForm(parent);
		parent.addControlListener(new ControlListener() {

			private int[] defaultWeights = { 1, 1 };

			@Override
			public void controlResized(ControlEvent e) {
				org.eclipse.swt.graphics.Rectangle b = parent.getBounds();
				int oldOrientation = mainSashForm.getOrientation();
				if ((oldOrientation == SWT.HORIZONTAL)
						&& (b.height > b.width)) {
					mainSashForm.setOrientation(SWT.VERTICAL);
					mainSashForm.setWeights(defaultWeights);
				} else if ((oldOrientation == SWT.VERTICAL)
						&& (b.height <= b.width)) {
					mainSashForm.setOrientation(SWT.HORIZONTAL);
					mainSashForm.setWeights(defaultWeights);
				}
			}

			@Override
			public void controlMoved(ControlEvent e) {
				// ignore
			}
		});
		form.setImage(getImage(UIIcons.REPOSITORY));
		form.setText(UIText.StagingView_NoSelectionTitle);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);
		toolkit.decorateFormHeading(form);
		GridLayoutFactory.swtDefaults().applyTo(form.getBody());

		mainSashForm = new SashForm(form.getBody(), getMainSashFormOrientation());
		saveSashFormOrientationOnDisposal(mainSashForm, MAIN_SASH_FORM_ORIENTATION_VERTICAL);
		saveSashFormWeightsOnDisposal(mainSashForm,
				HORIZONTAL_SASH_FORM_WEIGHT);
		toolkit.adapt(mainSashForm, true, true);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(mainSashForm);

		stagingSashForm = new SashForm(mainSashForm,
				getStagingFormOrientation());
		saveSashFormWeightsOnDisposal(stagingSashForm,
				STAGING_SASH_FORM_WEIGHT);
		toolkit.adapt(stagingSashForm, true, true);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(stagingSashForm);

		unstageAction = new Action(UIText.StagingView_UnstageItemMenuLabel,
				UIIcons.UNSTAGE) {
			@Override
			public void run() {
				unstage(stagedViewer.getStructuredSelection().toList());
			}
		};
		unstageAction.setToolTipText(UIText.StagingView_UnstageItemTooltip);
		stageAction = new Action(UIText.StagingView_StageItemMenuLabel,
				UIIcons.ELCL16_ADD) {
			@Override
			public void run() {
				stage(unstagedViewer.getStructuredSelection().toList());
			}
		};
		stageAction.setToolTipText(UIText.StagingView_StageItemTooltip);

		unstageAction.setEnabled(false);
		stageAction.setEnabled(false);

		unstageAllAction = new Action(
				UIText.StagingView_UnstageAllItemMenuLabel,
				UIIcons.UNSTAGE_ALL) {
			@Override
			public void run() {
				unstage(getContentProvider(stagedViewer)
						.getStagingEntriesFiltered());
			}
		};
		unstageAllAction
				.setToolTipText(UIText.StagingView_UnstageAllItemTooltip);
		stageAllAction = new Action(UIText.StagingView_StageAllItemMenuLabel,
				UIIcons.ELCL16_ADD_ALL) {
			@Override
			public void run() {
				stage(getContentProvider(unstagedViewer)
						.getStagingEntriesFiltered());
			}
		};
		stageAllAction.setToolTipText(UIText.StagingView_StageAllItemTooltip);

		unstageAllAction.setEnabled(false);
		stageAllAction.setEnabled(false);

		createPresentationActions();

		unstagedSection = toolkit.createSection(stagingSashForm,
				ExpandableComposite.SHORT_TITLE_BAR);
		unstagedSection.clientVerticalSpacing = 0;

		unstagedSection.setLayoutData(
				GridDataFactory.fillDefaults().grab(true, true).create());

		createUnstagedToolBarComposite();

		Composite unstagedComposite = toolkit.createComposite(unstagedSection);
		toolkit.paintBordersFor(unstagedComposite);
		unstagedSection.setClient(unstagedComposite);
		GridLayoutFactory.fillDefaults().applyTo(unstagedComposite);

		unstagedViewer = createViewer(unstagedComposite, true,
				selection -> unstage(selection.toList()), stageAction);

		unstagedViewer.addSelectionChangedListener(event -> {
			boolean hasSelection = !event.getSelection().isEmpty();
			if (hasSelection != stageAction.isEnabled()) {
				stageAction.setEnabled(hasSelection);
				unstagedToolBarManager.update(true);
			}
			workaroundMissingSwtRefresh(unstagedViewer);
		});

		Composite rebaseAndCommitComposite = toolkit.createComposite(mainSashForm);
		rebaseAndCommitComposite.setLayout(GridLayoutFactory.fillDefaults().create());

		rebaseSection = toolkit.createSection(rebaseAndCommitComposite,
				ExpandableComposite.SHORT_TITLE_BAR);
		rebaseSection.clientVerticalSpacing = 0;
		rebaseSection.setText(UIText.StagingView_RebaseLabel);

		Composite rebaseComposite = toolkit.createComposite(rebaseSection);
		toolkit.paintBordersFor(rebaseComposite);
		rebaseSection.setClient(rebaseComposite);

		rebaseSection.setLayoutData(GridDataFactory.fillDefaults().create());
		rebaseComposite.setLayout(GridLayoutFactory.fillDefaults()
				.numColumns(3).equalWidth(true).create());
		GridDataFactory buttonGridData = GridDataFactory.fillDefaults().align(
				SWT.FILL, SWT.CENTER);

		this.rebaseAbortButton = toolkit.createButton(rebaseComposite,
				UIText.StagingView_RebaseAbort, SWT.PUSH);
		rebaseAbortButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				rebaseAbort();
			}
		});
		rebaseAbortButton.setImage(getImage(UIIcons.REBASE_ABORT));
		buttonGridData.applyTo(rebaseAbortButton);

		this.rebaseSkipButton = toolkit.createButton(rebaseComposite,
				UIText.StagingView_RebaseSkip, SWT.PUSH);
		rebaseSkipButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				rebaseSkip();
			}
		});
		rebaseSkipButton.setImage(getImage(UIIcons.REBASE_SKIP));
		buttonGridData.applyTo(rebaseSkipButton);

		this.rebaseContinueButton = toolkit.createButton(rebaseComposite,
				UIText.StagingView_RebaseContinue, SWT.PUSH);
		rebaseContinueButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				rebaseContinue();
			}
		});
		rebaseContinueButton.setImage(getImage(UIIcons.REBASE_CONTINUE));
		buttonGridData.applyTo(rebaseContinueButton);

		showControl(rebaseSection, false);

		commitMessageSection = toolkit.createSection(rebaseAndCommitComposite,
				ExpandableComposite.SHORT_TITLE_BAR);
		commitMessageSection.clientVerticalSpacing = 0;
		commitMessageSection.setText(UIText.StagingView_CommitMessage);
		commitMessageSection.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, true).create());

		Composite commitMessageToolbarComposite = toolkit
				.createComposite(commitMessageSection);
		commitMessageToolbarComposite.setBackground(null);
		commitMessageToolbarComposite.setLayout(createRowLayoutWithoutMargin());
		commitMessageSection.setTextClient(commitMessageToolbarComposite);
		ToolBarManager commitMessageToolBarManager = new ToolBarManager(
				SWT.FLAT | SWT.HORIZONTAL);

		previewAction = new Action(UIText.StagingView_Preview_Commit_Message,
				IAction.AS_CHECK_BOX) {

			@Override
			public void run() {
				if (isChecked()) {
					previewLayout.topControl = commitMessagePreview;
					commitMessageSection.setText(
							UIText.StagingView_CommitMessagePreview);
					setCommentCharTooltip();
					previewer
							.setText(commitMessageComponent.getRepository(),
									commitMessageComponent.getCommitMessage());
				} else {
					previewLayout.topControl = commitMessageText;
					commitMessageSection
							.setText(UIText.StagingView_CommitMessage);
					setCommentCharTooltip();
				}
				previewLayout.topControl.getParent().layout(true, true);
				commitMessageSection.redraw();
				if (!isChecked()) {
					commitMessageText.setFocus();
				}
			}
		};
		previewAction.setImageDescriptor(UIIcons.ELCL16_PREVIEW);
		commitMessageToolBarManager.add(previewAction);
		commitMessageToolBarManager.add(new Separator());

		amendPreviousCommitAction = new Action(
				UIText.StagingView_Ammend_Previous_Commit, IAction.AS_CHECK_BOX) {

			@Override
			public void run() {
				commitMessageComponent.setAmendingButtonSelection(isChecked());
				updateMessage();
				updateCommitButtons();
			}
		};
		amendPreviousCommitAction.setImageDescriptor(UIIcons.AMEND_COMMIT);
		commitMessageToolBarManager.add(amendPreviousCommitAction);

		signedOffByAction = new Action(UIText.StagingView_Add_Signed_Off_By,
				IAction.AS_CHECK_BOX) {

			@Override
			public void run() {
				commitMessageComponent.setSignedOffButtonSelection(isChecked());
			}
		};
		signedOffByAction.setImageDescriptor(UIIcons.SIGNED_OFF);
		commitMessageToolBarManager.add(signedOffByAction);

		signCommitAction = new Action(UIText.StagingView_Sign_Commit,
				IAction.AS_CHECK_BOX) {

			@Override
			public void run() {
				commitMessageComponent
						.setSignCommitButtonSelection(isChecked());
			}
		};
		signCommitAction.setImageDescriptor(UIIcons.SIGN_COMMIT);
		commitMessageToolBarManager.add(signCommitAction);
		signCommitAction.setEnabled(true);
		// TODO: UIText.StagingView_Sign_Not_Available

		addChangeIdAction = new Action(UIText.StagingView_Add_Change_ID,
				IAction.AS_CHECK_BOX) {

			@Override
			public void run() {
				commitMessageComponent.setChangeIdButtonSelection(isChecked());
			}
		};
		addChangeIdAction.setImageDescriptor(UIIcons.GERRIT);
		commitMessageToolBarManager.add(addChangeIdAction);

		commitMessageToolBarManager
				.createControl(commitMessageToolbarComposite);

		Composite commitMessageComposite = toolkit
				.createComposite(commitMessageSection);
		commitMessageSection.setClient(commitMessageComposite);
		GridLayoutFactory.fillDefaults().numColumns(1)
				.applyTo(commitMessageComposite);

		warningLabel = new ToggleableLabel(commitMessageComposite,
				SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).exclude(true)
				.applyTo(warningLabel);

		Composite commitMessageTextComposite = toolkit
				.createComposite(commitMessageComposite);
		toolkit.paintBordersFor(commitMessageTextComposite);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(commitMessageTextComposite);
		previewLayout = new StackLayout();
		previewLayout.marginHeight = 2;
		previewLayout.marginWidth = 2;
		commitMessageTextComposite.setLayout(previewLayout);

		final CommitProposalProcessor commitProposalProcessor = new CommitProposalProcessor() {
			@Override
			protected Collection<String> computeFileNameProposals() {
				return getStagedFileNames();
			}

			@Override
			protected Collection<String> computeMessageProposals() {
				return CommitMessageHistory.getCommitHistory();
			}
		};
		commitMessageText = new CommitMessageArea(commitMessageTextComposite,
				EMPTY_STRING, SWT.NONE) {

			@Override
			protected CommitProposalProcessor getCommitProposalProcessor() {
				return commitProposalProcessor;
			}

			@Override
			protected IHandlerService getHandlerService() {
				return getSite().getService(IHandlerService.class);
			}
		};
		commitMessageText.setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TEXT_BORDER);
		// Disable CSS styling. The StyledText behaves like a text editor and
		// uses the text editor color and font preferences. CSS overrides those;
		// see bug 559321.
		//
		// The widget still shows correctly because the theme/CSS engine does
		// update the text editor preferences.
		UIUtils.setCssStyling(commitMessageText.getTextWidget(), false);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(commitMessageText);
		UIUtils.addBulbDecorator(
				commitMessageText.getTextWidget(),
				UIText.CommitDialog_ContentAssist);

		commitMessagePreview = new Composite(commitMessageTextComposite,
				SWT.NONE);
		commitMessagePreview.setLayout(new FillLayout());
		commitMessagePreview.setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TEXT_BORDER);
		previewer = new CommitMessagePreviewer();
		previewer.createControl(commitMessagePreview);

		previewLayout.topControl = commitMessageText;

		Composite composite = toolkit.createComposite(commitMessageComposite);
		toolkit.paintBordersFor(composite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);
		GridLayoutFactory.swtDefaults().margins(1, 2).numColumns(3)
				.spacing(1, LayoutConstants.getSpacing().y).applyTo(composite);

		createPersonLabel(composite, UIIcons.ELCL16_AUTHOR,
				UIText.StagingView_Author);
		authorText = toolkit.createText(composite, null);
		authorText.setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TEXT_BORDER);
		authorText.setLayoutData(GridDataFactory.fillDefaults().indent(5, 0)
				.grab(true, false).align(SWT.FILL, SWT.CENTER).create());

		createPersonLabel(composite, UIIcons.ELCL16_COMMITTER,
				UIText.StagingView_Committer);
		committerText = toolkit.createText(composite, null);
		committerText.setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TEXT_BORDER);
		committerText.setLayoutData(GridDataFactory.fillDefaults().indent(5, 0)
				.grab(true, false).align(SWT.FILL, SWT.CENTER).create());

		Composite buttonsContainer = toolkit.createComposite(composite);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
				.indent(0, 3).applyTo(buttonsContainer);
		GridLayoutFactory.fillDefaults().numColumns(2)
				.applyTo(buttonsContainer);

		ignoreErrors = toolkit.createButton(buttonsContainer,
					UIText.StagingView_IgnoreErrors, SWT.CHECK);
		ignoreErrors.setSelection(false);
		ignoreErrors.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateMessage();
				updateCommitButtons();
			}
		});
		getPreferenceStore()
				.addPropertyChangeListener(new IPropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent event) {
						if (isDisposed()) {
							getPreferenceStore()
									.removePropertyChangeListener(this);
							return;
						}
						asyncExec(() -> {
							updateIgnoreErrorsButtonVisibility();
							updateMessage();
							updateCommitButtons();
						});
					}
				});

		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING)
				.grab(true, true).applyTo(ignoreErrors);
		updateIgnoreErrorsButtonVisibility();

		Label filler = toolkit.createLabel(buttonsContainer, ""); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL)
				.grab(true, true).applyTo(filler);

		Composite commitButtonsContainer = toolkit
				.createComposite(buttonsContainer);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.applyTo(commitButtonsContainer);
		GridLayoutFactory.fillDefaults().numColumns(3)
				.applyTo(commitButtonsContainer);

		pushSettings = new PushSettings();
		Control pushSettingsUi = pushSettings
				.createControl(commitButtonsContainer);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.applyTo(pushSettingsUi);

		commitAndPushButton = toolkit.createButton(commitButtonsContainer,
				UIText.StagingView_CommitAndPushWithEllipsis, SWT.PUSH);
		commitAndPushButton.setImage(getImage(UIIcons.PUSH));
		commitAndPushButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (canPushHeadOnly(new LazyRepositoryState(currentRepository))) {
					pushHead(currentRepository);
				} else {
					commit(true);
				}
			}

			private void pushHead(Repository repository) {
				if (repository == null) {
					return;
				}
				PushMode mode = getPushMode(repository);
				if (mode == null) {
					return;
				}
				try {
					Wizard wizard = mode.getWizard(repository, null,
							pushSettings.isForce());
					if (wizard != null) {
						PushWizardDialog dialog = new PushWizardDialog(
								commitAndPushButton.getShell(), wizard);
						dialog.open();
					}
				} catch (IOException e) {
					Activator.handleError(e.getMessage(), e, true);
				}
			}
		});
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.applyTo(commitAndPushButton);

		commitButton = toolkit.createButton(commitButtonsContainer,
				UIText.StagingView_Commit, SWT.PUSH);
		commitButton.setImage(getImage(UIIcons.COMMIT));
		commitButton.setText(UIText.StagingView_Commit);
		commitButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				commit(false);
			}
		});
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.applyTo(commitButton);

		stagedSection = toolkit.createSection(stagingSashForm,
				ExpandableComposite.SHORT_TITLE_BAR);
		stagedSection.clientVerticalSpacing = 0;

		createStagedToolBarComposite();

		Composite stagedComposite = toolkit.createComposite(stagedSection);
		toolkit.paintBordersFor(stagedComposite);
		stagedSection.setClient(stagedComposite);
		GridLayoutFactory.fillDefaults().applyTo(stagedComposite);

		stagedViewer = createViewer(stagedComposite, false,
				selection -> stage(selection.toList()), unstageAction);
		stagedViewer.getLabelProvider().addListener(event -> {
			if (event.getSource() instanceof ProblemLabelDecorator
					&& !isDisposed()) {
				// Update UI when problem markers on staged items change
				updateMessage();
				updateCommitButtons();
			}
		});
		stagedViewer.addSelectionChangedListener(event -> {
			boolean hasSelection = !event.getSelection().isEmpty();
			if (hasSelection != unstageAction.isEnabled()) {
				unstageAction.setEnabled(hasSelection);
				stagedToolBarManager.update(true);
			}
			workaroundMissingSwtRefresh(stagedViewer);
		});

		selectionChangedListener = new ISelectionListener() {
			@Override
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				if (part == StagingView.this) {
					return;
				}
				if (part instanceof IEditorPart) {
					IEditorInput input = ((IEditorPart) part).getEditorInput();
					Repository repository = Adapters.adapt(input,
							Repository.class);
					if (repository != null) {
						reactOnSelection(new StructuredSelection(repository));
					}
				} else {
					reactOnSelection(selection);
				}
			}
		};

		initPresentation();
		partListener = new PartListener();

		IPreferenceStore preferenceStore = getPreferenceStore();
		if (preferenceStore.contains(UIPreferences.STAGING_VIEW_SYNC_SELECTION))
			reactOnSelection = preferenceStore.getBoolean(
					UIPreferences.STAGING_VIEW_SYNC_SELECTION);
		else
			preferenceStore.setDefault(UIPreferences.STAGING_VIEW_SYNC_SELECTION, true);

		preferenceStore.addPropertyChangeListener(uiPrefsListener);

		InstanceScope.INSTANCE
				.getNode(org.eclipse.egit.core.Activator.PLUGIN_ID)
				.addPreferenceChangeListener(prefListener);

		updateSectionText();
		stagedSection.setToolTipText(UIText.StagingView_StagedChangesTooltip);
		unstagedSection
				.setToolTipText(UIText.StagingView_UnstagedChangesTooltip);
		setCommentCharTooltip();
		commitMessageText.addCleanupChangeListener(this::setCommentCharTooltip);
		updateToolbar();
		enableCommitWidgets(false);
		refreshAction.setEnabled(false);

		createPopupMenu(unstagedViewer);
		createPopupMenu(stagedViewer);

		final ICommitMessageComponentNotifications listener = new ICommitMessageComponentNotifications() {

			private boolean addChangeState = addChangeIdAction.isChecked();

			@Override
			public void updateSignedOffToggleSelection(boolean selection) {
				signedOffByAction.setChecked(selection);
			}

			@Override
			public void updateChangeIdToggleSelection(boolean selection) {
				if (selection != addChangeState) {
					addChangeIdAction.setChecked(selection);
					addChangeState = selection;
					updateCommitAndPush(currentRepository);
				}
			}

			@Override
			public void updateSignCommitToggleSelection(boolean selection) {
				signCommitAction.setChecked(selection);
			}

			@Override
			public void statusUpdated() {
				updateMessage();
			}
		};
		commitMessageComponent = new CommitMessageComponent(listener);
		commitMessageComponent.attachControls(commitMessageText, authorText,
				committerText);

		// allow to commit with ctrl-enter
		commitMessageText.getTextWidget().addVerifyKeyListener(new VerifyKeyListener() {
			@Override
			public void verifyKey(VerifyEvent event) {
				if (UIUtils.isSubmitKeyEvent(event)) {
					event.doit = false;
					commit(false);
				}
			}
		});

		commitMessageText.getTextWidget().addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				// Ctrl+Enter shortcut only works when the focus is on the commit message text
				String commitButtonTooltip = MessageFormat.format(
						UIText.StagingView_CommitToolTip,
						UIUtils.SUBMIT_KEY_STROKE.format());
				commitButton.setToolTipText(commitButtonTooltip);
			}

			@Override
			public void focusLost(FocusEvent e) {
				commitButton.setToolTipText(null);
			}
		});

		IWorkbenchPartSite site = getSite();
		// Use current selection to populate staging view
		UIUtils.notifySelectionChangedWithCurrentSelection(
				selectionChangedListener, site);

		// react on selection changes
		ISelectionService srv = site.getService(ISelectionService.class);
		srv.addPostSelectionListener(selectionChangedListener);
		site.getService(IPartService.class).addPartListener(partListener);

		site.setSelectionProvider(new RepositorySelectionProvider(
				new MultiViewerSelectionProvider(unstagedViewer, stagedViewer),
				() -> realRepository));

		ViewerFilter filter = new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				StagingViewContentProvider contentProvider = getContentProvider((TreeViewer) viewer);
				if (element instanceof StagingEntry)
					return contentProvider.isInFilter((StagingEntry) element);
				else if (element instanceof StagingFolderEntry)
					return contentProvider
							.hasVisibleChildren((StagingFolderEntry) element);
				return true;
			}
		};
		unstagedViewer.addFilter(filter);
		stagedViewer.addFilter(filter);

		restoreSashFormWeights();

		IWorkbenchSiteProgressService service = getSite()
				.getService(IWorkbenchSiteProgressService.class);
		if (service != null && reactOnSelection)
			// If we are linked, each time IndexDiffUpdateJob starts, indicate
			// that the view is busy (e.g. reload() will trigger this job in
			// background!).
			service.showBusyForFamily(org.eclipse.egit.core.JobFamilies.INDEX_DIFF_CACHE_UPDATE);
	}

	private void updateTitle(boolean force) {
		ViewerLabel label = titleLabels.computeIfAbsent(
				titleNode.getRepository().getDirectory(),
				r -> new ViewerLabel(null, null));
		if (force) {
			if (label.getImage() != null) {
				form.setImage(label.getImage());
			}
			if (!StringUtils.isEmptyOrNull(label.getText())) {
				form.setText(label.getText());
			}
		}
		titleLabelProvider.updateLabel(label, titleNode);
		if (label.hasNewImage()) {
			form.setImage(label.getImage());
		}
		if (label.hasNewText()) {
			form.setText(label.getText());
		}
	}

	private void setCommentCharTooltip() {
		if (previewLayout.topControl != commitMessageText) {
			commitMessageSection.setToolTipText(null);
			return;
		}
		switch (commitMessageText.getCleanupMode()) {
		case STRIP:
		case SCISSORS:
			commitMessageSection.setToolTipText(MessageFormat.format(
					UIText.StagingView_CommentChar,
					String.valueOf(commitMessageText.getCommentChar())));
			break;
		default:
			commitMessageSection.setToolTipText(null);
			break;
		}
	}

	/**
	 * On Windows some SWT bug avoids repainting the non selected elements
	 * correctly, see bugzilla 533555.
	 *
	 * @param viewer
	 *            tree viewer
	 */
	private void workaroundMissingSwtRefresh(TreeViewer viewer) {
		if (Util.isWindows()) {
			viewer.getControl().redraw();
		}
	}

	private void updateIgnoreErrorsButtonVisibility() {
		boolean visible = getPreferenceStore()
				.getBoolean(UIPreferences.WARN_BEFORE_COMMITTING)
				&& getPreferenceStore().getBoolean(UIPreferences.BLOCK_COMMIT);
		showControl(ignoreErrors, visible);
		mainSashForm.layout();
	}

	private boolean hasProblemSeverity(int severity) {
		StagingViewContentProvider stagedContentProvider = getContentProvider(
				stagedViewer);
		StagingEntry[] entries = stagedContentProvider.getStagingEntries();
		for (StagingEntry entry : entries) {
			if (entry.getProblemSeverity() >= severity) {
				return true;
			}
		}
		return false;
	}

	private void updateCommitButtons() {
		IndexDiffData indexDiff;
		if (cacheEntry != null) {
			indexDiff = cacheEntry.getIndexDiff();
		} else {
			Repository repo = currentRepository;
			if (repo == null) {
				indexDiff = null;
			} else {
				indexDiff = doReload(repo);
			}
		}
		boolean indexDiffAvailable = indexDiffAvailable(indexDiff);
		boolean noConflicts = noConflicts(indexDiff);
		UnitOfWork.execute(currentRepository, () -> {
			Repository repo = currentRepository;
			LazyRepositoryState stateCache = new LazyRepositoryState(repo);
			boolean commitEnabled = noConflicts && indexDiffAvailable
					&& isCommitPossible(stateCache) && !isCommitBlocked();
			commitButton.setEnabled(commitEnabled);

			pushesHeadOnly = canPushHeadOnly(stateCache);
			commitAndPushButton
					.setEnabled(
							repo != null && (commitEnabled || pushesHeadOnly)
							&& !stateCache.get().isRebasing());
			updateCommitAndPush(repo);
		});
	}

	/**
	 * Updates the text and image of the "Commit&Push" button depending on
	 * whether the Change-Id button is selected (and the commit text has a
	 * Change-Id line).
	 *
	 * @param repository
	 *            to work with
	 */
	private void updateCommitAndPush(Repository repository) {
		PushMode pushMode = getPushMode(repository);
		if (pushMode == PushMode.GERRIT) {
			pushSettings.setVisible(false);
			commitAndPushButton.setImage(getImage(UIIcons.GERRIT));
		} else {
			pushSettings.setVisible(true);
			pushSettings.setEnabled(commitAndPushButton.isEnabled());
			commitAndPushButton.setImage(getImage(UIIcons.PUSH));
		}
		commitAndPushButton.setText(pushesHeadOnly ? UIText.StagingView_PushHEAD
				: canPushWithoutConfirmation(pushMode)
						? UIText.StagingView_CommitAndPush
						: UIText.StagingView_CommitAndPushWithEllipsis);
		commitAndPushButton.getParent().requestLayout();
	}

	private void saveSashFormWeightsOnDisposal(final SashForm sashForm,
			final String settingsKey) {
		sashForm.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				getDialogSettings().put(settingsKey,
						intArrayToString(sashForm.getWeights()));
			}
		});
	}

	private void saveSashFormOrientationOnDisposal(final SashForm sashForm,
			final String verticalOrientationSettingsKey) {
		sashForm.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				getDialogSettings().put(verticalOrientationSettingsKey,
						sashForm.getOrientation() == SWT.VERTICAL);
			}
		});
	}

	private IDialogSettings getDialogSettings() {
		return DialogSettings.getOrCreateSection(
				Activator.getDefault().getDialogSettings(),
				StagingView.class.getName());
	}

	private static String intArrayToString(int[] ints) {
		StringBuilder res = new StringBuilder();
		if (ints != null && ints.length > 0) {
			res.append(String.valueOf(ints[0]));
			for (int i = 1; i < ints.length; i++) {
				res.append(',');
				res.append(String.valueOf(ints[i]));
			}
		}
		return res.toString();
	}

	private void restoreSashFormWeights() {
		restoreSashFormWeights(mainSashForm,
				HORIZONTAL_SASH_FORM_WEIGHT);
		restoreSashFormWeights(stagingSashForm,
				STAGING_SASH_FORM_WEIGHT);
	}

	private void restoreSashFormWeights(SashForm sashForm, String settingsKey) {
		IDialogSettings settings = getDialogSettings();
		String weights = settings.get(settingsKey);
		if (weights != null && !weights.isEmpty()) {
			sashForm.setWeights(stringToIntArray(weights));
		}
	}

	private static int[] stringToIntArray(String s) {
		String[] parts = s.split(","); //$NON-NLS-1$
		int[] ints = new int[parts.length];
		for (int i = 0; i < parts.length; i++) {
			ints[i] = Integer.parseInt(parts[i]);
		}
		return ints;
	}

	private StructuredSelection getSelectionOfActiveEditor() {
		final IWorkbenchPartSite site = getSite();
		if (site == null) {
			return null;
		}
		IEditorPart activeEditor = site.getPage().getActiveEditor();
		if (activeEditor == null) {
			return null;
		}
		return getSelectionOfPart(activeEditor);
	}

	private static StructuredSelection getSelectionOfPart(IWorkbenchPart part) {
		if (part == null) {
			return null;
		}
		StructuredSelection sel = null;
		if (part instanceof IEditorPart) {
			IResource resource = getResource((IEditorPart) part);
			if (resource != null) {
				sel = new StructuredSelection(resource);
			} else {
				Repository repository = Adapters.adapt(
						((IEditorPart) part).getEditorInput(),
						Repository.class);
				if (repository != null) {
					sel = new StructuredSelection(repository);
				}
			}
		} else {
			ISelection selection = part.getSite().getPage().getSelection();
			if (selection instanceof StructuredSelection) {
				sel = (StructuredSelection) selection;
			}
		}
		return sel;
	}

	private static IResource getResource(IEditorPart part) {
		IEditorInput input = part.getEditorInput();
		if (input instanceof IFileEditorInput) {
			return ((IFileEditorInput) input).getFile();
		} else {
			return AdapterUtils.adaptToAnyResource(input);
		}
	}

	private boolean getSortCheckState() {
		return getDialogSettings().getBoolean(STORE_SORT_STATE);
	}

	private void executeRebaseOperation(AbstractRebaseCommandHandler command) {
		try {
			command.execute(currentRepository);
		} catch (ExecutionException e) {
			Activator.showError(e.getMessage(), e);
		}
	}

	/**
	 * Abort rebase command in progress
	 */
	protected void rebaseAbort() {
		AbortRebaseCommand abortCommand = new AbortRebaseCommand();
		executeRebaseOperation(abortCommand);
	}

	/**
	 * Rebase next commit and continue rebase in progress
	 */
	protected void rebaseSkip() {
		SkipRebaseCommand skipCommand = new SkipRebaseCommand();
		executeRebaseOperation(skipCommand);
	}

	/**
	 * Continue rebase command in progress
	 */
	protected void rebaseContinue() {
		ContinueRebaseCommand continueCommand = new ContinueRebaseCommand();
		executeRebaseOperation(continueCommand);
	}

	private void createUnstagedToolBarComposite() {
		Composite unstagedToolbarComposite = toolkit
				.createComposite(unstagedSection);
		unstagedToolbarComposite.setBackground(null);
		unstagedToolbarComposite.setLayout(createRowLayoutWithoutMargin());
		unstagedSection.setTextClient(unstagedToolbarComposite);
		unstagedExpandAllAction = new Action(UIText.UIUtils_ExpandAll,
				IAction.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				UIUtils.expandAll(unstagedViewer);
				enableAutoExpand(unstagedViewer);
			}
		};
		unstagedExpandAllAction.setImageDescriptor(UIIcons.EXPAND_ALL);
		unstagedExpandAllAction.setId(EXPAND_ALL_ITEM_TOOLBAR_ID);

		unstagedCollapseAllAction = new Action(UIText.UIUtils_CollapseAll,
				IAction.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				UIUtils.collapseAll(unstagedViewer);
				disableAutoExpand(unstagedViewer);
			}
		};
		unstagedCollapseAllAction.setImageDescriptor(UIIcons.COLLAPSEALL);
		unstagedCollapseAllAction.setId(COLLAPSE_ALL_ITEM_TOOLBAR_ID);

		hideUntrackedAction = new Action(UIText.StagingView_HideUntrackedFiles,
				IAction.AS_CHECK_BOX) {

			@Override
			public void run() {
				updateUnstagedViewer();
				refreshViewersPreservingExpandedElements();
			}

		};
		hideUntrackedAction.setImageDescriptor(UIIcons.ELCL16_HIDE_UNTRACKED);
		hideUntrackedAction.setId(HIDE_UNTRACKED_TOOLBAR_ID);
		hideUntrackedAction.setChecked(false);

		sortAction = new Action(UIText.StagingView_UnstagedSort,
				IAction.AS_CHECK_BOX) {

			@Override
			public void run() {
				StagingEntryComparator comparator = (StagingEntryComparator) unstagedViewer
						.getComparator();
				comparator.setAlphabeticSort(!isChecked());
				comparator = (StagingEntryComparator) stagedViewer.getComparator();
				comparator.setAlphabeticSort(!isChecked());
				unstagedViewer.refresh();
				stagedViewer.refresh();
			}
		};

		sortAction.setImageDescriptor(UIIcons.STATE_SORT);
		sortAction.setId(SORT_ITEM_TOOLBAR_ID);
		sortAction.setChecked(getSortCheckState());

		unstagedToolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);

		unstagedToolBarManager.add(stageAction);
		unstagedToolBarManager.add(stageAllAction);
		unstagedToolBarManager.add(hideUntrackedAction);
		unstagedToolBarManager.add(presentationAction);
		unstagedToolBarManager.add(sortAction);
		unstagedToolBarManager.add(unstagedExpandAllAction);
		unstagedToolBarManager.add(unstagedCollapseAllAction);

		unstagedToolBarManager.update(true);
		unstagedToolBarManager.createControl(unstagedToolbarComposite);
	}

	private void updateUnstagedViewer() {
		StagingViewContentProvider stagingViewContentProvider = (StagingViewContentProvider) unstagedViewer
				.getContentProvider();
		stagingViewContentProvider
				.setShowUntracked(!hideUntrackedAction.isChecked());
		updateSectionText();
		setRedraw(false);
		try {
			unstagedViewer.refresh();
			Object[] expandFolders = stagingViewContentProvider
					.getUntrackedFileFolders()
					.toArray(new StagingFolderEntry[0]);
			if (!hideUntrackedAction.isChecked() && expandFolders.length > 0) {
				unstagedViewer.setExpandedElements(expandFolders);
			}
		} finally {
			setRedraw(true);
		}
	}

	private void createStagedToolBarComposite() {
		Composite stagedToolbarComposite = toolkit
				.createComposite(stagedSection);
		stagedToolbarComposite.setBackground(null);
		stagedToolbarComposite.setLayout(createRowLayoutWithoutMargin());
		stagedSection.setTextClient(stagedToolbarComposite);
		stagedExpandAllAction = new Action(UIText.UIUtils_ExpandAll,
				IAction.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				UIUtils.expandAll(stagedViewer);
				enableAutoExpand(stagedViewer);
			}
		};
		stagedExpandAllAction.setImageDescriptor(UIIcons.EXPAND_ALL);
		stagedExpandAllAction.setId(EXPAND_ALL_ITEM_TOOLBAR_ID);

		stagedCollapseAllAction = new Action(UIText.UIUtils_CollapseAll,
				IAction.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				UIUtils.collapseAll(stagedViewer);
				disableAutoExpand(stagedViewer);
			}
		};
		stagedCollapseAllAction.setImageDescriptor(UIIcons.COLLAPSEALL);
		stagedCollapseAllAction.setId(COLLAPSE_ALL_ITEM_TOOLBAR_ID);

		stagedToolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);

		stagedToolBarManager.add(unstageAction);
		stagedToolBarManager.add(unstageAllAction);
		stagedToolBarManager.add(stagedExpandAllAction);
		stagedToolBarManager.add(stagedCollapseAllAction);
		stagedToolBarManager.update(true);
		stagedToolBarManager.createControl(stagedToolbarComposite);
	}

	private static RowLayout createRowLayoutWithoutMargin() {
		RowLayout layout = new RowLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.marginTop = 0;
		layout.marginBottom = 0;
		layout.marginLeft = 0;
		layout.marginRight = 0;
		return layout;
	}

	private static void addListenerToDisableAutoExpandOnCollapse(
			TreeViewer treeViewer) {
		treeViewer.addTreeListener(new ITreeViewerListener() {
			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				disableAutoExpand(event.getTreeViewer());
			}

			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				// Nothing to do
			}
		});
	}

	private static void enableAutoExpand(AbstractTreeViewer treeViewer) {
		treeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
	}

	private static void disableAutoExpand(AbstractTreeViewer treeViewer) {
		treeViewer.setAutoExpandLevel(0);
	}

	/**
	 * @return selected repository
	 */
	public Repository getCurrentRepository() {
		return currentRepository;
	}

	@Override
	public ShowInContext getShowInContext() {
		if (stagedViewer != null && stagedViewer.getTree().isFocusControl())
			return getShowInContext(stagedViewer);
		else if (unstagedViewer != null
				&& unstagedViewer.getTree().isFocusControl())
			return getShowInContext(unstagedViewer);
		else
			return null;
	}

	@Override
	public boolean show(ShowInContext context) {
		ISelection selection = context.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			for (Object element : structuredSelection.toList()) {
				if (element instanceof RepositoryTreeNode) {
					RepositoryTreeNode node = (RepositoryTreeNode) element;
					reload(node.getRepository());
					return true;
				}
			}
		}
		return false;
	}

	private ShowInContext getShowInContext(TreeViewer treeViewer) {
		IStructuredSelection selection = treeViewer.getStructuredSelection();
		List<Object> elements = new ArrayList<>();
		for (Object selectedElement : selection.toList()) {
			if (selectedElement instanceof StagingEntry) {
				StagingEntry entry = (StagingEntry) selectedElement;
				IFile file = entry.getFile();
				if (file != null)
					elements.add(file);
				else
					elements.add(entry.getLocation());
			} else if (selectedElement instanceof StagingFolderEntry) {
				StagingFolderEntry entry = (StagingFolderEntry) selectedElement;
				IContainer container = entry.getContainer();
				if (container != null)
					elements.add(container);
				else
					elements.add(entry.getLocation());
			}
		}
		return new ShowInContext(null, new StructuredSelection(elements));
	}

	private int getStagingFormOrientation() {
		boolean columnLayout = Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.STAGING_VIEW_COLUMN_LAYOUT);
		if (columnLayout)
			return SWT.HORIZONTAL;
		else
			return SWT.VERTICAL;
	}


	private int getMainSashFormOrientation() {
		IDialogSettings settings = getDialogSettings();
		return settings.getBoolean(MAIN_SASH_FORM_ORIENTATION_VERTICAL)
				? SWT.VERTICAL
				: SWT.HORIZONTAL;
	}

	private void enableAllWidgets(boolean enabled) {
		if (isDisposed())
			return;
		enableCommitWidgets(enabled);
		commitMessageText.setEnabled(enabled);
		enableStagingWidgets(enabled);
	}

	private void enableStagingWidgets(boolean enabled) {
		if (isDisposed())
			return;
		unstagedViewer.getControl().setEnabled(enabled);
		stagedViewer.getControl().setEnabled(enabled);
	}

	private void enableCommitWidgets(boolean enabled) {
		if (isDisposed()) {
			return;
		}
		committerText.setEnabled(enabled);
		enableAuthorText(enabled);
		amendPreviousCommitAction.setEnabled(enabled);
		signedOffByAction.setEnabled(enabled);
		signCommitAction.setEnabled(enabled);
		addChangeIdAction.setEnabled(enabled);
		if (enabled) {
			updateCommitButtons();
		} else {
			commitButton.setEnabled(enabled);
			commitAndPushButton.setEnabled(enabled);
		}
	}

	private void enableAuthorText(boolean enabled) {
		Repository repo = currentRepository;
		if (repo != null && repo.getRepositoryState()
				.equals(RepositoryState.CHERRY_PICKING_RESOLVED)) {
			authorText.setEnabled(false);
		} else {
			authorText.setEnabled(enabled);
		}
	}

	private void createPresentationActions() {
		listPresentationAction = new Action(UIText.StagingView_List,
				IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				if (!isChecked()) {
					return;
				}
				switchToListMode();
				refreshViewers();
			}
		};
		listPresentationAction.setImageDescriptor(UIIcons.FLAT);

		treePresentationAction = new Action(UIText.StagingView_Tree,
				IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				if (!isChecked()) {
					return;
				}
				presentation = Presentation.TREE;
				setPresentation(presentation, false);
				listPresentationAction.setChecked(false);
				compactTreePresentationAction.setChecked(false);
				setExpandCollapseActionsVisible(false, isExpandAllowed(false),
						true);
				setExpandCollapseActionsVisible(true, isExpandAllowed(true),
						true);
				refreshViewers();
			}
		};
		treePresentationAction.setImageDescriptor(UIIcons.HIERARCHY);

		compactTreePresentationAction = new Action(
				UIText.StagingView_CompactTree, IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				if (!isChecked()) {
					return;
				}
				switchToCompactModeInternal(false);
				refreshViewers();
			}

		};
		compactTreePresentationAction.setImageDescriptor(UIIcons.COMPACT);

		presentationAction = new PresentationAction(getPreferenceStore(),
				listPresentationAction, treePresentationAction,
				compactTreePresentationAction);
		presentationAction.setImageDescriptor(UIIcons.FLAT);
	}

	private void initPresentation() {
		presentation = readPresentation(UIPreferences.STAGING_VIEW_PRESENTATION,
				Presentation.LIST);
		switch (presentation) {
		case LIST:
			presentationAction.setImageDescriptor(UIIcons.FLAT);
			listPresentationAction.setChecked(true);
			setExpandCollapseActionsVisible(false, false, false);
			setExpandCollapseActionsVisible(true, false, false);
			break;
		case TREE:
			presentationAction.setImageDescriptor(UIIcons.HIERARCHY);
			treePresentationAction.setChecked(true);
			break;
		case COMPACT_TREE:
			presentationAction.setImageDescriptor(UIIcons.COMPACT);
			compactTreePresentationAction.setChecked(true);
			break;
		default:
			break;
		}
	}

	private void updateToolbar() {

		ControlContribution controlContribution = new ControlContribution(
				"StagingView.searchText") { //$NON-NLS-1$

			@Override
			protected Control createControl(Composite parent) {
				Composite toolbarComposite = new Composite(parent,
						SWT.NONE);
				toolbarComposite.setBackground(null);
				GridLayout headLayout = new GridLayout();
				headLayout.marginHeight = 0;
				headLayout.marginBottom = 1;
				headLayout.marginWidth = 0;
				toolbarComposite.setLayout(headLayout);

				filterText = new Text(toolbarComposite, SWT.SEARCH
						| SWT.ICON_CANCEL | SWT.ICON_SEARCH);
				filterText.setMessage(UIText.StagingView_Find);
				GridData data = new GridData(SWT.LEFT, SWT.TOP, true, false);
				data.minimumWidth = 150;
				filterText.setLayoutData(data);
				filterText.addModifyListener(e -> {
					filterPattern = wildcardToRegex(filterText.getText());
					StagingViewSearchThread searchThread = new StagingViewSearchThread(
							StagingView.this);
					filterText.getDisplay().timerExec(200,
							searchThread::start);
				});
				return toolbarComposite;
			}
		};

		IActionBars actionBars = getViewSite().getActionBars();
		IToolBarManager toolbar = actionBars.getToolBarManager();

		toolbar.add(controlContribution);

		refreshAction = new Action(UIText.StagingView_Refresh, IAction.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				if (cacheEntry != null) {
					schedule(
							cacheEntry.createRefreshResourcesAndIndexDiffJob(),
							false);
				}
				// Git config might have changed commit.cleanup?
				Repository repo = currentRepository;
				if (repo != null) {
					setCleanup(repo);
				}
			}
		};
		refreshAction.setImageDescriptor(UIIcons.ELCL16_REFRESH);
		refreshAction
				.setActionDefinitionId(IWorkbenchCommandConstants.FILE_REFRESH);
		toolbar.add(refreshAction);
		actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(),
				refreshAction);

		// link with selection
		Action linkSelectionAction = new BooleanPrefAction(
				(IPersistentPreferenceStore) getPreferenceStore(),
				UIPreferences.STAGING_VIEW_SYNC_SELECTION,
				UIText.StagingView_LinkSelection) {
			@Override
			public void apply(boolean value) {
				reactOnSelection = value;
				if (value && currentRepository == null) {
					if (lastSelection != null) {
						reactOnSelection(lastSelection);
					} else {
						ISelection selection = getSelectionOfActiveEditor();
						if (selection != null) {
							reactOnSelection(selection);
						}
					}
				}
			}
		};
		linkSelectionAction.setImageDescriptor(UIIcons.ELCL16_SYNCED);
		toolbar.add(linkSelectionAction);

		toolbar.add(new Separator());

		switchRepositoriesAction = new RepositoryToolbarAction(false,
				() -> realRepository,
				repo -> {
					if (realRepository != repo) {
						reload(repo);
					}
				});
		toolbar.add(switchRepositoriesAction);

		compareModeAction = new Action(UIText.StagingView_CompareMode,
				IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				getPreferenceStore().setValue(
						UIPreferences.STAGING_VIEW_COMPARE_MODE, isChecked());
			}
		};
		compareModeAction.setImageDescriptor(UIIcons.ELCL16_COMPARE_VIEW);
		compareModeAction.setChecked(getPreferenceStore()
				.getBoolean(UIPreferences.STAGING_VIEW_COMPARE_MODE));

		toolbar.add(compareModeAction);
		toolbar.add(new Separator());

		openNewCommitsAction = new Action(UIText.StagingView_OpenNewCommits,
				IAction.AS_CHECK_BOX) {

			@Override
			public void run() {
				getPreferenceStore().setValue(
						UIPreferences.STAGING_VIEW_SHOW_NEW_COMMITS, isChecked());
			}
		};
		openNewCommitsAction.setChecked(getPreferenceStore().getBoolean(
				UIPreferences.STAGING_VIEW_SHOW_NEW_COMMITS));

		columnLayoutAction = new Action(UIText.StagingView_ColumnLayout,
				IAction.AS_CHECK_BOX) {

			@Override
			public void run() {
				getPreferenceStore().setValue(
						UIPreferences.STAGING_VIEW_COLUMN_LAYOUT, isChecked());
				stagingSashForm.setOrientation(isChecked() ? SWT.HORIZONTAL
						: SWT.VERTICAL);
			}
		};
		columnLayoutAction.setImageDescriptor(UIIcons.ELCL16_COLUMN_LAYOUT);
		columnLayoutAction.setChecked(getPreferenceStore().getBoolean(
				UIPreferences.STAGING_VIEW_COLUMN_LAYOUT));

		fileNameModeAction = new Action(UIText.StagingView_ShowFileNamesFirst,
				IAction.AS_CHECK_BOX) {

			@Override
			public void run() {
				final boolean enable = isChecked();
				getLabelProvider(stagedViewer).setFileNameMode(enable);
				getLabelProvider(unstagedViewer).setFileNameMode(enable);
				getContentProvider(stagedViewer).setFileNameMode(enable);
				getContentProvider(unstagedViewer).setFileNameMode(enable);
				StagingEntryComparator comparator = (StagingEntryComparator) unstagedViewer
						.getComparator();
				comparator.setFileNamesFirst(enable);
				comparator = (StagingEntryComparator) stagedViewer.getComparator();
				comparator.setFileNamesFirst(enable);
				getPreferenceStore().setValue(
						UIPreferences.STAGING_VIEW_FILENAME_MODE, enable);
				refreshViewersPreservingExpandedElements();
			}
		};
		fileNameModeAction.setChecked(getPreferenceStore().getBoolean(
				UIPreferences.STAGING_VIEW_FILENAME_MODE));

		IMenuManager dropdownMenu = actionBars.getMenuManager();
		dropdownMenu.add(presentationAction);
		dropdownMenu.add(new Separator());
		dropdownMenu.add(openNewCommitsAction);
		dropdownMenu.add(columnLayoutAction);
		dropdownMenu.add(fileNameModeAction);
		dropdownMenu.add(compareModeAction);

		actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), new GlobalDeleteActionHandler());

		// For the normal resource undo/redo actions to be active, so that files
		// deleted via the "Delete" action in the staging view can be restored.
		IUndoContext workspaceContext = Adapters
				.adapt(ResourcesPlugin.getWorkspace(), IUndoContext.class);
		undoRedoActionGroup = new UndoRedoActionGroup(getViewSite(), workspaceContext, true);
		undoRedoActionGroup.fillActionBars(actionBars);

		actionBars.updateActionBars();
	}

	private Presentation readPresentation(String key, Presentation def) {
		return getPresentation(getPreferenceStore().getString(key), def);
	}

	private static Presentation getPresentation(String value,
			Presentation def) {
		if (!value.isEmpty()) {
			try {
				return Presentation.valueOf(value);
			} catch (IllegalArgumentException e) {
				// Use given default
			}
		}
		return def;
	}

	private void setPresentation(Presentation newOne, boolean auto) {
		Presentation old = presentation;
		presentation = newOne;
		IPreferenceStore store = getPreferenceStore();
		store.setValue(UIPreferences.STAGING_VIEW_PRESENTATION, newOne.name());
		if (auto && old != newOne) {
			// remember user choice if we switch mode automatically
			store.setValue(UIPreferences.STAGING_VIEW_PRESENTATION_CHANGED,
					true);
		} else {
			store.setToDefault(UIPreferences.STAGING_VIEW_PRESENTATION_CHANGED);
		}
	}

	private void setExpandCollapseActionsVisible(boolean staged,
			boolean visibleExpandAll,
			boolean visibleCollapseAll) {
		ToolBarManager toolBarManager = staged ? stagedToolBarManager
						: unstagedToolBarManager;
		for (IContributionItem item : toolBarManager.getItems()) {
			String id = item.getId();
			if (EXPAND_ALL_ITEM_TOOLBAR_ID.equals(id)) {
				item.setVisible(visibleExpandAll);
			} else if (COLLAPSE_ALL_ITEM_TOOLBAR_ID.equals(id)) {
				item.setVisible(visibleCollapseAll);
			}
		}
		(staged ? stagedExpandAllAction : unstagedExpandAllAction)
				.setEnabled(visibleExpandAll);
		(staged ? stagedCollapseAllAction : unstagedCollapseAllAction)
				.setEnabled(visibleCollapseAll);
		toolBarManager.update(true);
	}

	private boolean isExpandAllowed(boolean staged) {
		StagingViewContentProvider contentProvider = getContentProvider(
				staged ? stagedViewer : unstagedViewer);
		return contentProvider.getCount() <= getMaxLimitForListMode();
	}

	private TreeViewer createTree(Composite composite) {
		Tree tree = toolkit.createTree(composite, SWT.FULL_SELECTION
				| SWT.MULTI);
		TreeViewer treeViewer = new TreeViewer(tree);
		treeViewer.setUseHashlookup(true);
		return treeViewer;
	}

	private ColumnLabelProvider createLabelProvider(TreeViewer treeViewer) {
		StagingViewLabelProvider baseProvider = new StagingViewLabelProvider(
				this);
		baseProvider.setFileNameMode(getPreferenceStore().getBoolean(
				UIPreferences.STAGING_VIEW_FILENAME_MODE));

		ProblemLabelDecorator decorator = new ProblemLabelDecorator(treeViewer);
		return new TreeDecoratingLabelProvider(baseProvider, decorator);
	}

	private StagingViewContentProvider createStagingContentProvider(
			boolean unstaged) {
		StagingViewContentProvider provider = new StagingViewContentProvider(
				this, unstaged) {

			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				super.inputChanged(viewer, oldInput, newInput);
				if (unstaged) {
					stageAllAction.setEnabled(hasVisibleItems());
					unstagedToolBarManager.update(true);
				} else {
					unstageAllAction.setEnabled(hasVisibleItems());
					stagedToolBarManager.update(true);
				}
			}
		};
		provider.setFileNameMode(getPreferenceStore().getBoolean(
				UIPreferences.STAGING_VIEW_FILENAME_MODE));
		return provider;
	}

	private static String getConflictText(StagingEntry entry) {
		if (entry.hasConflicts()) {
			StageState conflictType = entry.getConflictType();
			switch (conflictType) {
			case DELETED_BY_THEM:
				return UIText.StagingView_Conflict_MD_short;
			case DELETED_BY_US:
				return UIText.StagingView_Conflict_DM_short;
			case BOTH_MODIFIED:
				return UIText.StagingView_Conflict_M_short;
			case BOTH_ADDED:
				return UIText.StagingView_Conflict_A_short;
			default:
				break;
			}
		}
		return ""; //$NON-NLS-1$
	}

	private TreeViewer createViewer(Composite parent, boolean unstaged,
			final Consumer<IStructuredSelection> dropAction,
			IAction... tooltipActions) {
		final TreeViewer viewer = createTree(parent);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(viewer.getControl());
		viewer.getTree().setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TREE_BORDER);
		UIUtils.setCssClass(viewer.getTree(), CSS_CLASS);
		StagingViewContentProvider contentProvider = createStagingContentProvider(
				unstaged);
		viewer.setContentProvider(contentProvider);
		if (tooltipActions != null && tooltipActions.length > 0) {
			StagingViewTooltips tooltips = new StagingViewTooltips(viewer,
					tooltipActions);
			tooltips.setShift(new Point(1, 1));
		}
		viewer.setLabelProvider(createLabelProvider(viewer));
		if (unstaged) {
			Tree tree = viewer.getTree();
			// Paint an indicator at the end of the column.
			tree.addListener(SWT.MeasureItem, event -> {
				Object obj = ((TreeItem) event.item).getData();
				if (obj instanceof StagingEntry) {
					StagingEntry entry = (StagingEntry) obj;
					String text = getConflictText(entry);
					if (text.isEmpty()) {
						entry.setExtraWidth(0);
						return;
					}
					int width = event.gc.textExtent(text).x
							+ 2 * IDialogConstants.HORIZONTAL_SPACING;
					event.width += width;
					entry.setExtraWidth(width);
				}
			});
			tree.addListener(SWT.PaintItem, event -> {
				if (!event.gc.isClipped()) {
					// The GC is clipped when we're drawing really inside the
					// tree. When we're drawing in a native(?) hover shown when
					// the label is longer than the available width in the cell,
					// the GC is not clipped. In the latter case we don't want
					// our indicator to appear -- it would be drawn right in the
					// middle of the label text in the hover.
					return;
				}
				Object obj = ((TreeItem) event.item).getData();
				if (obj instanceof StagingEntry) {
					StagingEntry entry = (StagingEntry) obj;
					String text = getConflictText(entry);
					if (text.isEmpty()) {
						entry.setExtraWidth(0);
						return;
					}

					Rectangle bounds = tree.getClientArea();
					Rectangle clip = event.gc.getClipping();
					if (clip.width < bounds.width) {
						clip.width = bounds.width;
						event.gc.setClipping(clip);
					}
					Rectangle cellBounds = ((TreeItem) event.item).getBounds();
					Point extent = event.gc.textExtent(text);
					int width = extent.x
							+ 2 * IDialogConstants.HORIZONTAL_SPACING;
					entry.setExtraWidth(width);
					// Draw a background-colored rectangle to erase the
					// foreground if the default painting has written the
					// label and it extends into the area we want to draw
					// our indicator in.
					int rightEdge = bounds.x + bounds.width;
					event.gc.fillRectangle(rightEdge - width, cellBounds.y,
							width, cellBounds.height);
					event.gc.drawString(text,
							rightEdge - extent.x
									- IDialogConstants.HORIZONTAL_SPACING,
							cellBounds.y + (cellBounds.height - extent.y) / 2);
				}
			});
			// The following scrollbar handling is a work-around for Eclipse
			// 2021-06 not repainting items on OS X when the view is scrolled
			// horizontally. Instead it somehow manages to scroll the item
			// without repainting, including our custom-drawn indicator, which
			// then appears somewhere in the middle of the label instead of at
			// the right edge of the tree's client area.
			ScrollBar bar = tree.getHorizontalBar();
			if (bar != null) {
				bar.addSelectionListener(new SelectionAdapter() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						super.widgetSelected(e);
						tree.getDisplay().asyncExec(() -> {
							if (!tree.isDisposed()) {
								tree.redraw();
							}
						});
					}
				});
			}
			ConflictStateHoverManager hovers = new ConflictStateHoverManager(
					viewer);
			hovers.install(viewer.getControl());
		}
		viewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK,
				new Transfer[] { LocalSelectionTransfer.getTransfer(),
						FileTransfer.getInstance() },
				new StagingDragListener(viewer, contentProvider, unstaged));
		viewer.addDropSupport(DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DropTargetAdapter() {

					@Override
					public void drop(DropTargetEvent event) {
						// Bug 411466: It is very important that detail is set
						// to DND.DROP_COPY. If it was left as DND.DROP_MOVE and
						// the drag comes from the Navigator view, the code in
						// NavigatorDragAdapter would delete the resources.
						event.detail = DND.DROP_COPY;
						if (event.data instanceof IStructuredSelection) {
							final IStructuredSelection selection = (IStructuredSelection) event.data;
							if ((selection instanceof StagingDragSelection)
									&& ((StagingDragSelection) selection)
											.isFromUnstaged() == unstaged) {
								// Dropped a selection made in this viewer
								// back on this viewer: don't do anything,
								// otherwise if there are folders in the
								// selection, we might unstage or stage files
								// not selected!
								return;
							}
							dropAction.accept(selection);
						}
					}
				});
		viewer.addOpenListener(this::compareWith);
		viewer.setComparator(new StagingEntryComparator(!getSortCheckState(),
				getPreferenceStore()
						.getBoolean(UIPreferences.STAGING_VIEW_FILENAME_MODE)));
		viewer.addDoubleClickListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) event
					.getSelection();
			Object selectedNode = selection.getFirstElement();
			if (selectedNode instanceof StagingFolderEntry) {
				viewer.setExpandedState(selectedNode,
						!viewer.getExpandedState(selectedNode));
			}
		});
		addCopyAction(viewer);
		enableAutoExpand(viewer);
		addListenerToDisableAutoExpandOnCollapse(viewer);
		return viewer;
	}

	private void addCopyAction(final TreeViewer viewer) {
		IAction copyAction = createSelectionPathCopyAction(viewer);

		ActionUtils.setGlobalActions(viewer.getControl(),
				getSite().getService(IHandlerService.class), copyAction);
	}

	private IAction createSelectionPathCopyAction(final TreeViewer viewer) {
		IStructuredSelection selection = viewer.getStructuredSelection();
		String copyPathActionText = MessageFormat.format(
				UIText.StagingView_CopyPaths,
				Integer.valueOf(selection.size()));
		IAction copyAction = ActionUtils.createGlobalAction(ActionFactory.COPY,
				() -> copyPathOfSelectionToClipboard(viewer));
		copyAction.setText(copyPathActionText);
		return copyAction;
	}

	private void copyPathOfSelectionToClipboard(final TreeViewer viewer) {
		Clipboard cb = new Clipboard(viewer.getControl().getDisplay());
		try {
			TextTransfer t = TextTransfer.getInstance();
			String text = getTextFrom(
				(IStructuredSelection) viewer.getSelection());
			if (text != null) {
				cb.setContents(new Object[] { text }, new Transfer[] { t });
			}
		} finally {
			cb.dispose();
		}
	}

	@Nullable
	private String getTextFrom(IStructuredSelection selection) {
		Object[] selectionEntries = selection.toArray();
		if (selectionEntries.length <= 0) {
			return null;
		} else if (selectionEntries.length == 1) {
			return getPathFrom(selectionEntries[0]);
		} else {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < selectionEntries.length; i++) {
				String text = getPathFrom(selectionEntries[i]);
				if (text != null) {
					if (i < selectionEntries.length - 1) {
						sb.append(text).append(System.lineSeparator());
					} else {
						sb.append(text);
					}
				}
			}
			return sb.toString();
		}
	}

	@Nullable
	private String getPathFrom(Object obj) {
		if (obj instanceof StagingEntry) {
			return ((StagingEntry) obj).getPath();
		} else if (obj instanceof StagingFolderEntry) {
			return ((StagingFolderEntry) obj).getPath().toString();
		}
		return null;
	}

	private void setStagingViewerInput(TreeViewer stagingViewer,
			StagingViewUpdate newInput, Object[] previous,
			Set<IPath> additionalPaths) {
		// Disable painting and show a busy cursor for the tree during the
		// entire update process.
		final Tree tree = stagingViewer.getTree();
		tree.setRedraw(false);
		Cursor oldCursor = tree.getCursor();
		tree.setCursor(tree.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));

		try {
			// Remember the elements at or before the current top element of the
			// viewer.
			TreeItem topItem = tree.getTopItem();
			final Set<Object> precedingObjects = new LinkedHashSet<>();
			if (topItem != null) {
				new TreeItemVisitor(tree.getItems()) {
					@Override
					public boolean visit(TreeItem treeItem) {
						precedingObjects.add(treeItem.getData());
						return true;
					}
				}.traverse(topItem);
				precedingObjects.remove(null);
			}

			// Controls whether we'll try to preserve the top element in the
			// view, i.e., the scroll position. We generally want that unless
			// the update has added new objects to the view, in which case those
			// are selected and revealed.
			boolean preserveTop = true;
			boolean keepSelectionVisible = false;
			StagingViewUpdate oldInput = (StagingViewUpdate) stagingViewer
					.getInput();
			if (oldInput != null && oldInput.repository == newInput.repository
					&& oldInput.indexDiff != null) {
				// If the input has changed and wasn't empty before or wasn't
				// for a different repository before, record the contents of the
				// viewer before the input is changed.
				StagingViewContentProvider contentProvider = getContentProvider(
						stagingViewer);
				ViewerComparator comparator = stagingViewer.getComparator();
				Map<String, Object> oldPaths = buildElementMap(stagingViewer,
						contentProvider, comparator);

				// Update the input.
				stagingViewer.setInput(newInput);
				// Restore the previous expansion state, if there is one.
				if (previous != null) {
					expandPreviousExpandedAndPaths(previous, stagingViewer,
							additionalPaths);
				}

				Map<String, Object> newPaths = buildElementMap(stagingViewer,
						contentProvider, comparator);
				if (newPaths.isEmpty()) {
					preserveTop = false;
				} else {
					// Update the selection.
					StagingViewerUpdate stagingViewerUpdate = updateSelection(
							stagingViewer, contentProvider, oldPaths, newPaths);

					// If something has been removed, the element before the
					// removed item has been selected, in which case we want to
					// preserve the scroll state as much as possible, keeping
					// the selection in view. If something has been added, those
					// added things have been selected and revealed, so we don't
					// want to preserve the top but rather leave the revealed
					// selection alone. If nothing has changed, we want to
					// preserve the top, regardless of where the current
					// unmodified selection might be, which is what's done by
					// default anyway.
					if (stagingViewerUpdate == StagingViewerUpdate.REMOVED) {
						keepSelectionVisible = true;
					} else if (stagingViewerUpdate == StagingViewerUpdate.ADDED) {
						preserveTop = false;
					}
				}
			} else {
				// The update is completely different so don't do any of the
				// above analysis to see what's different.
				stagingViewer.setInput(newInput);
				// Restore the previous expansion state, if there is one.
				if (previous != null) {
					expandPreviousExpandedAndPaths(previous, stagingViewer,
							additionalPaths);
				}
			}

			if (preserveTop) {
				// It's likely that the tree has scrolled to change the top
				// item. So try to restore the current top item to be the one
				// with the same data as was at the top before, either starting
				// with the selection so that it generally stays in view, or at
				// the bottom, if we're not trying to keep the selection
				// visible.
				TreeItem[] selection = tree.getSelection();
				TreeItem initialItem = keepSelectionVisible
						&& selection.length > 0 ? selection[0] : null;
				new TreeItemVisitor(tree.getItems()) {
					@Override
					public boolean visit(TreeItem treeItem) {
						if (precedingObjects.contains(treeItem.getData())) {
							// If we reach an item that was at or before the
							// original top item, make it the top item
							// again, and stop the visitor.
							tree.setTopItem(treeItem);
							return false;
						}
						return true;
					}
				}.traverse(initialItem);
			}
		} finally {
			// The viewer is fully updated now, so we can paint it.
			tree.setRedraw(true);
			tree.setCursor(oldCursor);
		}
	}

	private static Map<String, Object> buildElementMap(TreeViewer stagingViewer,
			StagingViewContentProvider contentProvider,
			ViewerComparator comparator) {
		// Builds a map from paths, represented as strings, to elements visible
		// in the staging viewer.
		Map<String, Object> result = new LinkedHashMap<>();
		// Start visiting the root elements in the order in which they appear in
		// the UI.
		Object[] elements = contentProvider.getElements(null);
		comparator.sort(stagingViewer, elements);
		for (Object element : elements) {
			visitElement(stagingViewer, contentProvider, comparator, element,
					result);
		}
		return result;
	}

	private static boolean visitElement(TreeViewer stagingViewer,
			StagingViewContentProvider contentProvider,
			ViewerComparator comparator,
			Object element, Map<String, Object> paths) {
		if (element instanceof StagingEntry) {
			StagingEntry stagingEntry = (StagingEntry) element;
			if (contentProvider.isInFilter(stagingEntry)) {
				// If the element is a staging entry, and it's included by the
				// filter, add a mapping for it.
				String path = stagingEntry.getPath();
				paths.put(path, stagingEntry);
				return true;
			}

			return false;
		}

		// If the element is a staging folder entry, visit all the children,
		// checking that at least one visited descendant has been added to the
		// map before adding a mapping for this staging folder entry.
		if (element instanceof StagingFolderEntry) {
			StagingFolderEntry stagingFolderEntry = (StagingFolderEntry) element;
			// Visit the children in the order in which they appear in the UI.
			Object[] children = contentProvider.getChildren(stagingFolderEntry);
			comparator.sort(stagingViewer, children);

			IPath path = stagingFolderEntry.getPath();
			String pathString = path.toString();
			paths.put(pathString, stagingFolderEntry);

			boolean hasVisibleChildren = false;
			for (Object child : children) {
				if (visitElement(stagingViewer, contentProvider, comparator,
						child, paths)) {
					hasVisibleChildren = true;
				}
			}

			if (hasVisibleChildren) {
				return true;
			}

			// If there were no visible children, remove the path from the map.
			paths.remove(pathString);
			return false;
		}

		return false;
	}

	private enum StagingViewerUpdate {
		ADDED, REMOVED, UNCHANGED
	}

	/**
	 * Updates the selection depending on the type of change in the staging
	 * viewer's state. If something has been removed, it returns
	 * {@link StagingViewerUpdate#REMOVED} and the item before the removed
	 * element is selected. If something has been added, it returns
	 * {@link StagingViewerUpdate#ADDED} and those added elements are selected
	 * and revealed. If nothing has changed, it returns
	 * {@link StagingViewerUpdate#UNCHANGED} and the selection state is
	 * unchanged.
	 *
	 * @param stagingViewer
	 *            the staging viewer for which to update the selection.
	 * @param contentProvider
	 *            the content provider used by that staging viewer.
	 * @param oldPaths
	 *            the old content state of the staging viewer.
	 * @param newPaths
	 *            the new content state of the staging viewer.
	 * @return the type of change to the selecting of the staging viewer
	 */
	private static StagingViewerUpdate updateSelection(TreeViewer stagingViewer,
			StagingViewContentProvider contentProvider,
			Map<String, Object> oldPaths, Map<String, Object> newPaths) {
		// Update the staging viewer's selection by analyzing the change
		// to the contents of the viewer.
		Map<String, Object> addedPaths = new LinkedHashMap<>(newPaths);
		addedPaths.keySet().removeAll(oldPaths.keySet());
		if (!addedPaths.isEmpty()) {
			// If anything has been added to the viewer, select those added
			// things. But, to minimize the selection, select a parent node when
			// all its children have been added. The general idea is that if you
			// drag and drop between staged and unstaged, the new selection in
			// the target view, when dragged back again to the source view, will
			// undo the original drag-and-drop operation operation.
			List<Object> newSelection = new ArrayList<>();
			Set<Object> elements = new LinkedHashSet<>(addedPaths.values());
			Set<Object> excludeChildren = new LinkedHashSet<>();
			for (Object element : elements) {
				if (element instanceof StagingEntry) {
					StagingEntry stagingEntry = (StagingEntry) element;
					if (!excludeChildren.contains(stagingEntry.getParent())) {
						// If it's a leaf entry and its parent has not been
						// excluded from the selection, include it in the
						// selection.
						newSelection.add(stagingEntry);
					}
				} else if (element instanceof StagingFolderEntry) {
					StagingFolderEntry stagingFolderEntry = (StagingFolderEntry) element;
					StagingFolderEntry parent = stagingFolderEntry.getParent();
					if (excludeChildren.contains(parent)) {
						// If its parent has been excluded from the selection,
						// exclude this folder entry also.
						excludeChildren.add(stagingFolderEntry);
					} else if (elements.containsAll(contentProvider
							.getStagingEntriesFiltered(stagingFolderEntry))) {
						// If all of this folder's visible children are added,
						// i.e., it had no existing children before, then
						// include it in the selection, and exclude its
						// children from the selection.
						newSelection.add(stagingFolderEntry);
						excludeChildren.add(stagingFolderEntry);
					}
				}
			}

			// Select and reveal the selection of the newly added elements.
			stagingViewer.setSelection(new StructuredSelection(newSelection),
					true);
			return StagingViewerUpdate.ADDED;
		} else {
			Map<String, Object> removedPaths = new LinkedHashMap<>(oldPaths);
			removedPaths.keySet().removeAll(newPaths.keySet());
			if (!removedPaths.isEmpty()) {
				// If anything has been removed from the viewer, try to select
				// the closest following unremoved sibling of the first removed
				// element, a parent if there isn't such a sibling, or the first
				// element in the viewer failing those. The general idea is that
				// it's really annoying to have the viewer scroll to the top
				// element whenever you drag something out of a staging viewer.
				Collection<Object> removedElements = new LinkedHashSet<>(
						removedPaths.values());
				Object firstRemovedElement = removedElements.iterator()
						.next();
				Object parent = contentProvider.getParent(firstRemovedElement);
				Object candidate = null;
				boolean visitSubsequentSiblings = false;
				for (Object oldElement : oldPaths.values()) {
					if (oldElement == firstRemovedElement) {
						// Once we reach the first removed element, siblings
						// that follow are ideal candidates.
						visitSubsequentSiblings = true;
					}

					if (visitSubsequentSiblings) {
						if (!removedElements.contains(oldElement)) {
							if (contentProvider
									.getParent(oldElement) == parent) {
								// If this is a subsequent sibling that's not
								// itself removed, it's the best candidate.
								candidate = oldElement;
								break;
							} else if (candidate != null) {
								// If we already have a candidate, and we're
								// looking for a subsequent sibling, but now
								// we've hit an element with a different parent
								// of the removed element, then we're never
								// going to find a subsequent unremoved sibling,
								// so just return the candidate.
								break;
							}
						}
					} else if (candidate == null || oldElement == parent
							|| contentProvider
									.getParent(oldElement) == parent) {
						// If there is no candidate, or there is a better
						// candidate, i.e., the parent or an element with the
						// same parent, record the current entry.
						candidate = oldElement;
					}
				}

				if (candidate == null && !newPaths.isEmpty()) {
					// If there is no selected object yet, just choose the first
					// element in the viewer, if there is such an element.
					candidate = newPaths.values().iterator().next();
				}

				if (candidate != null) {
					// If we have a selection, which will always be the case
					// unless the viewer is empty, set it. This selection is
					// preserved during update of the viewer. Unfortunately the
					// scroll position is generally quite poor. Fixing the
					// scroll position is done after the viewer is updated.
					stagingViewer.setSelection(
							new StructuredSelection(candidate), true);
					return StagingViewerUpdate.REMOVED;
				}
			}

			return StagingViewerUpdate.UNCHANGED;
		}
	}

	/**
	 * This visitor is used to traverse all visible tree items of a tree viewer
	 * starting at some specific item, visiting the items in the reverse order
	 * in which they appear in the UI.
	 */
	private static abstract class TreeItemVisitor {
		private final TreeItem[] roots;

		public TreeItemVisitor(TreeItem[] roots) {
			this.roots = roots;
		}

		public abstract boolean visit(TreeItem treeItem);

		/**
		 * The public entry point for invoking this visitor.
		 *
		 * @param treeItem
		 *            the item at which to start, are null, to start at the
		 *            bottom.
		 */
		public void traverse(TreeItem treeItem) {
			if (treeItem == null) {
				treeItem = getLastItem(roots);
				if (treeItem == null) {
					return;
				}
			}
			if (treeItem.isDisposed()) {
				return;
			}
			if (treeItem.getData() != null && visit(treeItem)) {
				traversePrecedingSiblings(treeItem);
			}
		}

		private TreeItem getLastItem(TreeItem[] treeItems) {
			if (treeItems.length == 0) {
				return null;
			}
			TreeItem lastItem = treeItems[treeItems.length - 1];
			if (lastItem.getExpanded()) {
				TreeItem result = getLastItem(lastItem.getItems());
				if (result != null) {
					return result;
				}
			}
			return lastItem;
		}

		private boolean traversePrecedingSiblings(TreeItem treeItem) {
			TreeItem parent = treeItem.getParentItem();
			if (parent == null) {
				// If there is no parent, traverse based on the root items.
				return traversePrecedingSiblings(roots, treeItem);
			}
			// Traverse based on the parent items, i.e., the siblings of the
			// tree item.
			if (!traversePrecedingSiblings(parent.getItems(), treeItem)) {
				return false;
			}
			// Recursively traverse the parent.
			return traversePrecedingSiblings(parent);
		}

		private boolean traversePrecedingSiblings(TreeItem[] siblings,
				TreeItem treeItem) {
			// Traverse the siblings in reverse order, skipping the ones that
			// are at or before the tree item.
			boolean start = false;
			for (int i = siblings.length - 1; i >= 0; --i) {
				TreeItem sibling = siblings[i];
				if (start) {
					// Traverse all the visible children of this preceding
					// sibling.
					if (!traverseChildren(sibling)) {
						return false;
					}
				} else if (sibling == treeItem) {
					start = true;
				}
			}

			return true;
		}

		private boolean traverseChildren(TreeItem treeItem) {
			if (treeItem.getExpanded()) {
				// If the tree item is expanded, traverse all the children in
				// reverse order.
				TreeItem[] children = treeItem.getItems();
				for (int i = children.length - 1; i >= 0; --i) {
					// Recursively traverse the children of the children.
					if (!traverseChildren(children[i])) {
						return false;
					}
				}
			}
			// Call the visitor callback after the children have been visited.
			return visit(treeItem);
		}
	}

	private IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	private StagingViewLabelProvider getLabelProvider(TreeViewer viewer) {
		IBaseLabelProvider base = viewer.getLabelProvider(0);
		if (base == null) {
			base = viewer.getLabelProvider();
		}
		return ((TreeDecoratingLabelProvider) base).getBaseLabelProvider();
	}

	private StagingViewContentProvider getContentProvider(ContentViewer viewer) {
		return (StagingViewContentProvider) viewer.getContentProvider();
	}

	private void updateSectionText() {
		stagedSection.setText(MessageFormat
				.format(UIText.StagingView_StagedChanges,
						getSectionCount(stagedViewer, false)));
		unstagedSection.setText(MessageFormat.format(
				UIText.StagingView_UnstagedChanges,
				getSectionCount(unstagedViewer, true)));
	}

	private String getSectionCount(TreeViewer viewer, boolean unstagedCount) {
		StagingViewContentProvider contentProvider = getContentProvider(viewer);
		int count = contentProvider.getCount();
		int shownCount = contentProvider.getShownCount();
		if ((getFilterPattern() != null
				|| (unstagedCount && hideUntrackedAction.isChecked()
						&& shownCount < count))
				&& count > 0) {
			return shownCount + "/" + count; //$NON-NLS-1$
		} else {
			return Integer.toString(count);
		}
	}

	private void updateMessage() {
		boolean needsRefresh = false;
		if (hasErrorsOrWarnings()) {
			needsRefresh = warningLabel
					.showMessage(UIText.StagingView_MessageErrors);
		} else {
			String message = commitMessageComponent.getStatus().getMessage();
			if (message != null) {
				needsRefresh = warningLabel.showMessage(message);
			} else if (isUnbornHead) {
				needsRefresh = warningLabel.showInfo(MessageFormat.format(
						UIText.StagingView_InitialCommitText, currentBranch));
			} else {
				needsRefresh = warningLabel.hideMessage();
			}
		}
		// Without this explicit redraw, the ControlDecoration of the
		// commit message area would not get updated and cause visual
		// corruption. A simple requestLayout() is not good enough.
		if (needsRefresh) {
			commitMessageSection.requestLayout();
			commitMessageSection.redraw();
		}
	}

	private void compareWith(OpenEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event
				.getSelection();
		if (selection.isEmpty()
				|| !(selection.getFirstElement() instanceof StagingEntry))
			return;
		StagingEntry stagingEntry = (StagingEntry) selection.getFirstElement();
		if (stagingEntry.isSubmodule())
			return;
		switch (stagingEntry.getState()) {
		case ADDED:
		case CHANGED:
		case REMOVED:
			runCommand(ActionCommands.COMPARE_INDEX_WITH_HEAD_ACTION, selection);
			break;

		case CONFLICTING:
			runCommand(ActionCommands.MERGE_TOOL_ACTION, selection);
			break;

		case MISSING:
		case MISSING_AND_CHANGED:
		case MODIFIED:
		case MODIFIED_AND_CHANGED:
		case MODIFIED_AND_ADDED:
		case UNTRACKED:
		default:
			if (Activator.getDefault().getPreferenceStore().getBoolean(
					UIPreferences.STAGING_VIEW_COMPARE_MODE)) {
				// compare with index
				runCommand(ActionCommands.COMPARE_WITH_INDEX_ACTION, selection);
			} else {
				openSelectionInEditor(selection);
			}

		}
	}

	private void createPopupMenu(final TreeViewer treeViewer) {
		final MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		Control control = treeViewer.getControl();
		control.setMenu(menuMgr.createContextMenu(control));
		menuMgr.addMenuListener(new IMenuListener() {

			@Override
			public void menuAboutToShow(IMenuManager manager) {
				control.setFocus();
				final IStructuredSelection selection = treeViewer
						.getStructuredSelection();
				if (selection.isEmpty())
					return;

				Set<StagingEntry> stagingEntrySet = new LinkedHashSet<>();
				Set<StagingFolderEntry> stagingFolderSet = new LinkedHashSet<>();

				boolean submoduleSelected = false;
				boolean folderSelected = false;
				boolean onlyFoldersSelected = true;
				for (Object element : selection.toArray()) {
					if (element instanceof StagingFolderEntry) {
						StagingFolderEntry folder = (StagingFolderEntry) element;
						folderSelected = true;
						if (onlyFoldersSelected) {
							stagingFolderSet.add(folder);
						}
						StagingViewContentProvider contentProvider = getContentProvider(treeViewer);
						stagingEntrySet.addAll(contentProvider
								.getStagingEntriesFiltered(folder));
					} else if (element instanceof StagingEntry) {
						if (onlyFoldersSelected) {
							stagingFolderSet.clear();
						}
						onlyFoldersSelected = false;
						StagingEntry entry = (StagingEntry) element;
						if (entry.isSubmodule()) {
							submoduleSelected = true;
						}
						stagingEntrySet.add(entry);
					}
				}

				List<StagingEntry> stagingEntryList = new ArrayList<>(
						stagingEntrySet);
				final IStructuredSelection fileSelection = new StructuredSelection(
						stagingEntryList);
				stagingEntrySet = null;

				if (!folderSelected) {
					Action openWorkingTreeVersion = new Action(
							UIText.CommitFileDiffViewer_OpenWorkingTreeVersionInEditorMenuLabel,
							UIIcons.GOTO_INPUT) {
						@Override
						public void run() {
							openSelectionInEditor(fileSelection);
						}
					};
					openWorkingTreeVersion.setEnabled(!submoduleSelected
							&& anyElementIsExistingFile(fileSelection));
					menuMgr.add(openWorkingTreeVersion);
					String label = stagingEntryList.get(0).isStaged()
									? UIText.CommitFileDiffViewer_CompareWorkingDirectoryMenuLabel
									: UIText.StagingView_CompareWithIndexMenuLabel;
					Action openCompareWithIndex = new Action(label,
							UIIcons.ELCL16_COMPARE_VIEW) {
						@Override
						public void run() {
							runCommand(ActionCommands.COMPARE_WITH_INDEX_ACTION,
									fileSelection);
						}
					};
					menuMgr.add(openCompareWithIndex);
					if (stagingEntryList.size() == 1
							&& stagingEntryList.get(0).isStaged()) {
						Action compareWithHead = new Action(
								UIText.StagingView_CompareWithHeadMenuLabel,
								UIIcons.ELCL16_COMPARE_VIEW) {

							@Override
							public void run() {
								runCommand(
										ActionCommands.COMPARE_INDEX_WITH_HEAD_ACTION,
										fileSelection);
							}
						};
						menuMgr.add(compareWithHead);
					}
					if (stagingEntryList.size() == 2) {
						menuMgr.add(
								compareWithEachOther(stagingEntryList.get(0),
										stagingEntryList.get(1)));
					}
				}

				Set<StagingEntry.Action> availableActions = getAvailableActions(fileSelection);

				boolean addReplaceWithFileInGitIndex = availableActions.contains(StagingEntry.Action.REPLACE_WITH_FILE_IN_GIT_INDEX);
				boolean addReplaceWithHeadRevision = availableActions.contains(StagingEntry.Action.REPLACE_WITH_HEAD_REVISION);
				boolean addStage = availableActions.contains(StagingEntry.Action.STAGE);
				boolean addUnstage = availableActions.contains(StagingEntry.Action.UNSTAGE);
				boolean addDelete = availableActions.contains(StagingEntry.Action.DELETE);
				boolean addIgnore = availableActions.contains(StagingEntry.Action.IGNORE);
				boolean addLaunchMergeTool = availableActions.contains(StagingEntry.Action.LAUNCH_MERGE_TOOL);
				boolean addReplaceWithOursTheirsMenu = availableActions
						.contains(StagingEntry.Action.REPLACE_WITH_OURS_THEIRS_MENU);
				boolean addAssumeUnchanged = availableActions
						.contains(StagingEntry.Action.ASSUME_UNCHANGED);
				boolean addUntrack = availableActions
						.contains(StagingEntry.Action.UNTRACK);

				if (addStage) {
					menuMgr.add(
							new Action(UIText.StagingView_StageItemMenuLabel,
									UIIcons.ELCL16_ADD) {
						@Override
						public void run() {
									stage(selection.toList());
						}
					});
				}
				if (addUnstage) {
					menuMgr.add(
							new Action(UIText.StagingView_UnstageItemMenuLabel,
									UIIcons.UNSTAGE) {
						@Override
						public void run() {
									unstage(selection.toList());
						}
					});
				}
				boolean selectionIncludesNonWorkspaceResources = selectionIncludesNonWorkspaceResources(fileSelection);
				if (addReplaceWithFileInGitIndex) {
					if (selectionIncludesNonWorkspaceResources) {
						menuMgr.add(new ReplaceAction(
								UIText.StagingView_replaceWithFileInGitIndex,
								fileSelection, false));
					} else {
						menuMgr.add(createItem(
								UIText.StagingView_replaceWithFileInGitIndex,
								ActionCommands.DISCARD_CHANGES_ACTION,
								fileSelection)); // replace with index
					}
				}
				if (addReplaceWithHeadRevision) {
					if (selectionIncludesNonWorkspaceResources) {
						menuMgr.add(new ReplaceAction(
								UIText.StagingView_replaceWithHeadRevision,
								fileSelection, true));
					} else {
						menuMgr.add(createItem(
								UIText.StagingView_replaceWithHeadRevision,
								ActionCommands.REPLACE_WITH_HEAD_ACTION,
								fileSelection));
					}
				}
				if (addIgnore) {
					if (!stagingFolderSet.isEmpty()) {
						menuMgr.add(new IgnoreFoldersAction(stagingFolderSet));
					}
					menuMgr.add(new IgnoreAction(fileSelection));
				}
				if (addDelete) {
					menuMgr.add(new DeleteAction(fileSelection));
				}
				if (addLaunchMergeTool) {
					menuMgr.add(new Action(UIText.StagingView_MergeTool,
							UIIcons.MERGE_TOOL) {
						@Override
						public void run() {
							CommonUtils.runCommand(ActionCommands.MERGE_TOOL_ACTION, fileSelection);
						}
					});
				}
				if (addReplaceWithOursTheirsMenu) {
					MenuManager replaceWithMenu = new MenuManager(
							UIText.StagingView_ReplaceWith);
					ReplaceConflictMenu oursTheirsMenu = new ReplaceConflictMenu(
							currentRepository, stagingEntryList);
					replaceWithMenu.add(oursTheirsMenu);
					menuMgr.add(replaceWithMenu);
				}
				if (addAssumeUnchanged) {
					menuMgr.add(
							new Action(UIText.StagingView_Assume_Unchanged,
									UIIcons.ASSUME_UNCHANGED) {
								@Override
								public void run() {
									assumeUnchanged(selection);
								}
							});
				}
				if (addUntrack) {
					menuMgr.add(new Action(UIText.StagingView_Untrack,
							UIIcons.UNTRACK) {
						@Override
						public void run() {
							untrack(selection);
						}
					});
				}
				menuMgr.add(new Separator());
				menuMgr.add(createShowInMenu());
				menuMgr.add(createSelectionPathCopyAction(treeViewer));
			}
		});

	}

	private IAction compareWithEachOther(StagingEntry left,
			StagingEntry right) {
		if (left.isStaged()) {
			return new Action(UIText.StagingView_CompareWithEachOtherLabel,
					UIIcons.ELCL16_COMPARE_VIEW) {

				@Override
				public void run() {
					CompareUtils.compareBetween(currentRepository,
							left.getPath(), right.getPath(),
							GitFileRevision.INDEX, GitFileRevision.INDEX,
							StagingView.this.getViewSite().getPage());
				}
			};
		}
		// Unstaged.
		return new Action(UIText.StagingView_CompareWithEachOtherLabel,
				UIIcons.ELCL16_COMPARE_VIEW) {

			@Override
			public void run() {
				CompareUtils.compareFiles(left.getFile(), right.getFile(),
						left.getLocation().toFile(),
						right.getLocation().toFile(), StagingView.this);
			}
		};
	}

	private boolean anyElementIsExistingFile(IStructuredSelection s) {
		for (Object element : s.toList()) {
			if (element instanceof StagingEntry) {
				StagingEntry entry = (StagingEntry) element;
				if (entry.getType() != IResource.FILE) {
					continue;
				}
				if (entry.getLocation().toFile().exists()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @return selected presentation
	 */
	Presentation getPresentation() {
		return presentation;
	}

	/**
	 * @return the trimmed string which is the current filter, {@code null} for
	 *         no filter
	 */
	Pattern getFilterPattern() {
		return filterPattern;
	}

	/**
	 * Convert a filter string to a regex pattern. Wildcard characters "*" will
	 * match anything, other characters must match literally (case insensitive).
	 * The filter string will be trimmed.
	 *
	 * @param filter
	 * @return compiled pattern, or {@code null} if trimmed filter input is
	 *         empty
	 */
	private Pattern wildcardToRegex(String filter) {
		String trimmed = filter.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		String regex = (trimmed.contains("*") ? "^" : "") + "\\Q"//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				+ trimmed.replaceAll("\\*", //$NON-NLS-1$
						Matcher.quoteReplacement("\\E.*?\\Q")) //$NON-NLS-1$
				+ "\\E";//$NON-NLS-1$
		// remove potentially empty quotes at begin or end
		regex = regex.replaceAll(Pattern.quote("\\Q\\E"), ""); //$NON-NLS-1$ //$NON-NLS-2$
		return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
	}

	/**
	 * Refresh the unstaged and staged viewers without preserving expanded
	 * elements
	 */
	public void refreshViewers() {
		syncExec(() -> {
			setRedraw(false);
			try {
				refreshViewersInternal();
			} finally {
				setRedraw(true);
			}
		});
	}

	private void setRedraw(boolean redraw) {
		unstagedViewer.getControl().setRedraw(redraw);
		stagedViewer.getControl().setRedraw(redraw);
	}

	/**
	 * Refresh the unstaged and staged viewers, preserving expanded elements
	 */
	public void refreshViewersPreservingExpandedElements() {
		syncExec(() -> {
			Object[] unstagedExpanded = unstagedViewer.getVisibleExpandedElements();
			Object[] stagedExpanded = stagedViewer.getVisibleExpandedElements();
			setRedraw(false);
			try {
				refreshViewersInternal();
				unstagedViewer.setExpandedElements(unstagedExpanded);
				stagedViewer.setExpandedElements(stagedExpanded);
			} finally {
				setRedraw(true);
			}
			stageAllAction
					.setEnabled(unstagedViewer.getTree().getItemCount() > 0);
			unstagedToolBarManager.update(true);
			unstageAllAction
					.setEnabled(stagedViewer.getTree().getItemCount() > 0);
			stagedToolBarManager.update(true);
		});
	}

	private void refreshViewersInternal() {
		// Refreshing without selected elements is much faster.
		IStructuredSelection selection = unstagedViewer
				.getStructuredSelection();
		unstagedViewer.setSelection(StructuredSelection.EMPTY);
		unstagedViewer.refresh();
		// Create a *new* selection to avoid the viewer uses the original tree
		// paths, which may no longer exist if the presentation has changed.
		unstagedViewer
				.setSelection(new StructuredSelection(selection.toList()));
		selection = stagedViewer.getStructuredSelection();
		stagedViewer.setSelection(StructuredSelection.EMPTY);
		stagedViewer.refresh();
		stagedViewer.setSelection(new StructuredSelection(selection.toList()));
		updateSectionText();
	}

	private IContributionItem createShowInMenu() {
		IWorkbenchWindow workbenchWindow = getSite().getWorkbenchWindow();
		return UIUtils.createShowInMenu(workbenchWindow);
	}

	private class ReplaceAction extends Action {

		private final IStructuredSelection selection;
		private final boolean headRevision;

		ReplaceAction(String text, @NonNull IStructuredSelection selection,
				boolean headRevision) {
			super(text);
			this.selection = selection;
			this.headRevision = headRevision;
		}

		private void getSelectedFiles(@NonNull List<String> files,
				@NonNull List<String> inaccessibleFiles) {
			Iterator iterator = selection.iterator();
			while (iterator.hasNext()) {
				Object selectedItem = iterator.next();
				if (selectedItem instanceof StagingEntry) {
					StagingEntry stagingEntry = (StagingEntry) selectedItem;
					String path = stagingEntry.getPath();
					files.add(path);
					IFile resource = stagingEntry.getFile();
					if (resource == null || !resource.isAccessible()) {
						inaccessibleFiles.add(path);
					}
				}
			}
		}

		@Override
		public void run() {
			Repository repo = getCurrentRepository();
			if (repo == null) {
				return;
			}
			List<String> files = new ArrayList<>();
			List<String> inaccessibleFiles = new ArrayList<>();
			getSelectedFiles(files, inaccessibleFiles);
			if (files.isEmpty()) {
				return;
			}
			if (!CommandConfirmation.confirmCheckout(form.getShell(), repo)) {
				return;
			}
			DiscardChangesOperation operation = new DiscardChangesOperation(
					repo, files);
			if (headRevision) {
				operation.setRevision(Constants.HEAD);
			}
			JobChangeAdapter refresher = null;
			if (!inaccessibleFiles.isEmpty()) {
				refresher = new JobChangeAdapter() {

					@Override
					public void done(IJobChangeEvent event) {
						IndexDiffCacheEntry indexDiffCacheForRepository = IndexDiffCache.INSTANCE
								.getIndexDiffCacheEntry(repo);
						if (indexDiffCacheForRepository != null) {
							indexDiffCacheForRepository
									.refreshFiles(inaccessibleFiles);
						}
					}
				};
			}
			JobUtil.scheduleUserWorkspaceJob(operation,
					UIText.DiscardChangesAction_discardChanges,
					JobFamilies.DISCARD_CHANGES, refresher);
		}
	}

	private static class IgnoreAction extends Action {

		private final IStructuredSelection selection;

		IgnoreAction(IStructuredSelection selection) {
			super(UIText.StagingView_IgnoreItemMenuLabel, UIIcons.IGNORE);
			this.selection = selection;
		}

		@Override
		public void run() {
			IgnoreOperationUI operation = new IgnoreOperationUI(
					getSelectedPaths(selection));
			operation.run();
		}
	}

	private static class IgnoreFoldersAction extends Action {
		private final Set<StagingFolderEntry> selection;

		IgnoreFoldersAction(Set<StagingFolderEntry> selection) {
			super(UIText.StagingView_IgnoreFolderMenuLabel);
			this.selection = selection;
		}

		@Override
		public void run() {
			List<IPath> paths = new ArrayList<>();
			for (StagingFolderEntry folder : selection) {
				paths.add(folder.getLocation());
			}
			IgnoreOperationUI operation = new IgnoreOperationUI(paths);
			operation.run();
		}

	}

	private class DeleteAction extends Action {

		private final IStructuredSelection selection;

		DeleteAction(IStructuredSelection selection) {
			super(UIText.StagingView_DeleteItemMenuLabel,
					UIIcons.ELCL16_DELETE);
			this.selection = selection;
		}

		@Override
		public void run() {
			DeletePathsOperationUI operation = new DeletePathsOperationUI(
					getSelectedPaths(selection), getSite());
			operation.run();
		}
	}

	private class GlobalDeleteActionHandler extends Action {

		@Override
		public void run() {
			DeletePathsOperationUI operation = new DeletePathsOperationUI(
					getSelectedPaths(getSelection()), getSite());
			operation.run();
		}

		@Override
		public boolean isEnabled() {
			if (!unstagedViewer.getTree().isFocusControl())
				return false;

			IStructuredSelection selection = getSelection();
			if (selection.isEmpty())
				return false;

			for (Object element : selection.toList()) {
				if (!(element instanceof StagingEntry))
					return false;
				StagingEntry entry = (StagingEntry) element;
				if (!entry.getAvailableActions().contains(StagingEntry.Action.DELETE))
					return false;
			}

			return true;
		}

		private IStructuredSelection getSelection() {
			return (IStructuredSelection) unstagedViewer.getSelection();
		}
	}

	private static List<IPath> getSelectedPaths(IStructuredSelection selection) {
		List<IPath> paths = new ArrayList<>();
		Iterator iterator = selection.iterator();
		while (iterator.hasNext()) {
			StagingEntry stagingEntry = (StagingEntry) iterator.next();
			paths.add(stagingEntry.getLocation());
		}
		return paths;
	}

	/**
	 * @param selection
	 * @return true if the selection includes a non-workspace resource, false otherwise
	 */
	private boolean selectionIncludesNonWorkspaceResources(ISelection selection) {
		if (!(selection instanceof IStructuredSelection))
			return false;
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		Iterator iterator = structuredSelection.iterator();
		while (iterator.hasNext()) {
			Object selectedObject = iterator.next();
			if (!(selectedObject instanceof StagingEntry))
				return false;
			StagingEntry stagingEntry = (StagingEntry) selectedObject;
			IFile file = stagingEntry.getFile();
			if (file == null || !file.isAccessible()) {
				return true;
			}
		}
		return false;
	}

	private void openSelectionInEditor(ISelection s) {
		Repository repo = currentRepository;
		if (repo == null || s.isEmpty() || !(s instanceof IStructuredSelection)) {
			return;
		}
		final IStructuredSelection iss = (IStructuredSelection) s;
		for (Object element : iss.toList()) {
			if (element instanceof StagingEntry) {
				StagingEntry entry = (StagingEntry) element;
				String relativePath = entry.getPath();
				File file = new Path(repo.getWorkTree().getAbsolutePath())
						.append(relativePath).toFile();
				DiffViewer.openFileInEditor(file, -1);
			}
		}
	}

	private static Set<StagingEntry.Action> getAvailableActions(IStructuredSelection selection) {
		Set<StagingEntry.Action> availableActions = EnumSet.noneOf(StagingEntry.Action.class);
		for (Iterator it = selection.iterator(); it.hasNext(); ) {
			StagingEntry stagingEntry = (StagingEntry) it.next();
			if (availableActions.isEmpty())
				availableActions.addAll(stagingEntry.getAvailableActions());
			else
				availableActions.retainAll(stagingEntry.getAvailableActions());
		}
		return availableActions;
	}

	private IAction createItem(String text, final String commandId,
			final IStructuredSelection selection) {
		return new Action(text) {
			@Override
			public void run() {
				CommonUtils.runCommand(commandId, selection);
			}
		};
	}

	private boolean shouldUpdateSelection() {
		return !isDisposed() && partListener.isVisible() && reactOnSelection;
	}

	private void reactOnSelection(ISelection sel) {
		if (!(sel instanceof StructuredSelection)) {
			return;
		}
		StructuredSelection selection = (StructuredSelection) sel;
		if (selection.size() != 1 || isDisposed()) {
			return;
		}
		if (!shouldUpdateSelection()) {
			// Remember it all the same to be able to update the view when it
			// becomes active again
			lastSelection = reactOnSelection ? selection : null;
			return;
		}
		lastSelection = null;
		Object firstElement = selection.getFirstElement();
		if (firstElement instanceof RepositoryTreeNode) {
			RepositoryTreeNode repoNode = (RepositoryTreeNode) firstElement;
			if (currentRepository != repoNode.getRepository()) {
				reload(repoNode.getRepository());
			}
		} else if (firstElement instanceof Repository) {
			Repository repo = (Repository) firstElement;
			if (currentRepository != repo) {
				reload(repo);
			}
		} else {
			Repository repo = Adapters.adapt(firstElement, Repository.class);
			if (repo != null) {
				if (currentRepository != repo) {
					reload(repo);
				}
			} else {
				IResource resource = AdapterUtils
						.adaptToAnyResource(firstElement);
				if (resource != null) {
					showResource(resource);
				}
			}
		}
	}

	private void showResource(final IResource resource) {
		if (resource == null || !resource.isAccessible()) {
			return;
		}
		Job.getJobManager().cancel(JobFamilies.UPDATE_SELECTION);
		Job job = new Job(UIText.StagingView_GetRepo) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				RepositoryMapping mapping = RepositoryMapping
						.getMapping(resource);
				if (mapping != null) {
					Repository newRep = mapping.getRepository();
					if (newRep != null && newRep != currentRepository) {
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						reload(newRep);
					}
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return JobFamilies.UPDATE_SELECTION == family;
			}

			@Override
			public boolean shouldRun() {
				return shouldUpdateSelection();
			}
		};
		job.setSystem(true);
		schedule(job, false);
	}

	private void stage(Collection<?> selectedEntries) {
		StagingViewContentProvider contentProvider = getContentProvider(unstagedViewer);
		final Repository repository = currentRepository;
		Iterator iterator = selectedEntries.iterator();
		final Set<String> addPaths = new HashSet<>();
		final Set<String> rmPaths = new HashSet<>();
		resetPathsToExpand();
		while (iterator.hasNext()) {
			Object element = iterator.next();
			if (element instanceof StagingEntry) {
				StagingEntry entry = (StagingEntry) element;
				selectEntryForStaging(entry, addPaths, rmPaths);
				addPathAndParentPaths(entry.getParentPath(), pathsToExpandInStaged);
			} else if (element instanceof StagingFolderEntry) {
				StagingFolderEntry folder = (StagingFolderEntry) element;
				List<StagingEntry> entries = contentProvider
						.getStagingEntriesFiltered(folder);
				for (StagingEntry entry : entries)
					selectEntryForStaging(entry, addPaths, rmPaths);
				addExpandedPathsBelowFolder(folder, unstagedViewer,
						pathsToExpandInStaged);
			} else {
				IResource resource = AdapterUtils.adaptToAnyResource(element);
				if (resource != null) {
					RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
					// doesn't do anything if the current repository is a
					// submodule of the mapped repo
					if (mapping != null && mapping.getRepository() == currentRepository) {
						String path = mapping.getRepoRelativePath(resource);
						// If resource corresponds to root of working directory
						if (path == null || path.isEmpty())
							addPaths.add("."); //$NON-NLS-1$
						else
							addPaths.add(path);
					}
				}
			}
		}

		// start long running operations
		if (!addPaths.isEmpty()) {
			Job addJob = new Job(UIText.StagingView_AddJob) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try (Git git = new Git(repository)) {
						AddCommand add = git.add();
						for (String addPath : addPaths)
							add.addFilepattern(addPath);
						add.call();
					} catch (NoFilepatternException e1) {
						// cannot happen
					} catch (JGitInternalException e1) {
						Activator.handleError(e1.getCause().getMessage(),
								e1.getCause(), true);
					} catch (Exception e1) {
						Activator.handleError(e1.getMessage(), e1, true);
					}
					return Status.OK_STATUS;
				}

				@Override
				public boolean belongsTo(Object family) {
					return family == JobFamilies.ADD_TO_INDEX;
				}
			};

			schedule(addJob, true);
		}

		if (!rmPaths.isEmpty()) {
			Job removeJob = new Job(UIText.StagingView_RemoveJob) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try (Git git = new Git(repository)) {
						RmCommand rm = git.rm().setCached(true);
						for (String rmPath : rmPaths)
							rm.addFilepattern(rmPath);
						rm.call();
					} catch (NoFilepatternException e) {
						// cannot happen
					} catch (JGitInternalException e) {
						Activator.handleError(e.getCause().getMessage(),
								e.getCause(), true);
					} catch (Exception e) {
						Activator.handleError(e.getMessage(), e, true);
					}
					return Status.OK_STATUS;
				}

				@Override
				public boolean belongsTo(Object family) {
					return family == JobFamilies.REMOVE_FROM_INDEX;
				}
			};

			schedule(removeJob, true);
		}
	}

	private void selectEntryForStaging(StagingEntry entry,
			Collection<String> addPaths, Collection<String> rmPaths) {
		switch (entry.getState()) {
		case ADDED:
		case CHANGED:
		case REMOVED:
			// already staged
			break;
		case CONFLICTING: {
			// In a delete-modify conflict, we do have the file in the working
			// tree. If it has been deleted, the user resolved the conflict in
			// favour of the deletion. In other conflicts, if the user removes
			// the file, we also should remove it from the index.
			if (!Files.exists(entry.getLocation().toFile().toPath(),
					LinkOption.NOFOLLOW_LINKS)) {
				rmPaths.add(entry.getPath());
			} else {
				addPaths.add(entry.getPath());
			}
			break;
		}
		case MODIFIED:
		case MODIFIED_AND_CHANGED:
		case MODIFIED_AND_ADDED:
		case UNTRACKED:
			addPaths.add(entry.getPath());
			break;
		case MISSING:
		case MISSING_AND_CHANGED:
			rmPaths.add(entry.getPath());
			break;
		}
	}

	private void unstage(Collection<?> selectedEntries) {
		if (selectedEntries.isEmpty())
			return;

		Collection<String> paths = processUnstageSelection(selectedEntries);
		if (paths.isEmpty())
			return;

		final Repository repository = currentRepository;

		Job resetJob = new Job(UIText.StagingView_ResetJob) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try (Git git = new Git(repository)) {
					ResetCommand reset = git.reset();
					for (String path : paths)
						reset.addPath(path);
					reset.call();
				} catch (GitAPIException e) {
					Activator.handleError(e.getMessage(), e, true);
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return family == JobFamilies.RESET;
			}
		};
		schedule(resetJob, true);
	}

	private Collection<String> processUnstageSelection(
			Collection<?> selectedEntries) {
		Set<String> paths = new HashSet<>();
		resetPathsToExpand();
		for (Object element : selectedEntries) {
			if (element instanceof StagingEntry) {
				StagingEntry entry = (StagingEntry) element;
				addUnstagePath(entry, paths);
				addPathAndParentPaths(entry.getParentPath(), pathsToExpandInUnstaged);
			} else if (element instanceof StagingFolderEntry) {
				StagingFolderEntry folder = (StagingFolderEntry) element;
				List<StagingEntry> entries = getContentProvider(stagedViewer)
						.getStagingEntriesFiltered(folder);
				for (StagingEntry entry : entries)
					addUnstagePath(entry, paths);
				addExpandedPathsBelowFolder(folder, stagedViewer,
						pathsToExpandInUnstaged);
			}
		}
		return paths;
	}

	private void addUnstagePath(StagingEntry entry, Collection<String> paths) {
		switch (entry.getState()) {
		case ADDED:
		case CHANGED:
		case REMOVED:
			paths.add(entry.getPath());
			return;
		default:
			// unstaged
		}
	}

	private void assumeUnchanged(@NonNull IStructuredSelection selection) {
		List<IPath> locations = new ArrayList<>();
		collectPaths(selection.toList(), locations);

		if (locations.isEmpty()) {
			return;
		}

		JobUtil.scheduleUserJob(
				new AssumeUnchangedOperation(currentRepository, locations, true),
				UIText.AssumeUnchanged_assumeUnchanged,
				JobFamilies.ASSUME_NOASSUME_UNCHANGED);
	}

	private void untrack(@NonNull IStructuredSelection selection) {
		List<IPath> locations = new ArrayList<>();
		collectPaths(selection.toList(), locations);

		if (locations.isEmpty()) {
			return;
		}

		JobUtil.scheduleUserJob(
				new UntrackOperation(currentRepository, locations),
				UIText.Untrack_untrack, JobFamilies.UNTRACK);
	}

	private void collectPaths(Object o, List<IPath> result) {
		if (o instanceof Iterable<?>) {
			((Iterable<?>) o).forEach(child -> collectPaths(child, result));
		} else if (o instanceof StagingFolderEntry) {
			StagingViewContentProvider contentProvider = getContentProvider(unstagedViewer);
			List<StagingEntry> entries = contentProvider
					.getStagingEntriesFiltered((StagingFolderEntry) o);
			collectPaths(entries, result);
		} else if (o instanceof StagingEntry) {
			result.add(Adapters.adapt(o, IPath.class));
		}
	}

	private void resetPathsToExpand() {
		pathsToExpandInStaged = new HashSet<>();
		pathsToExpandInUnstaged = new HashSet<>();
	}

	private static void addExpandedPathsBelowFolder(StagingFolderEntry folder,
			TreeViewer treeViewer, Set<IPath> addToSet) {
		if (treeViewer.getExpandedState(folder)) {
			addPathAndParentPaths(folder.getPath(), addToSet);
		}
		addExpandedSubfolders(folder, treeViewer, addToSet);
	}

	private static void addExpandedSubfolders(StagingFolderEntry folder,
			TreeViewer treeViewer, Set<IPath> addToSet) {
		for (Object child : folder.getChildren()) {
			if (child instanceof StagingFolderEntry
					&& treeViewer.getExpandedState(child)) {
				addToSet.add(((StagingFolderEntry) child).getPath());
				addExpandedSubfolders((StagingFolderEntry) child, treeViewer,
						addToSet);
			}
		}
	}

	private static void addPathAndParentPaths(IPath initialPath, Set<IPath> addToSet) {
		IPath p = initialPath;
		// If already added, then all further ancestors also already have been
		// added.
		while (p.segmentCount() > 0 && addToSet.add(p)) {
			p = p.removeLastSegments(1);
		}
	}

	private boolean isValidRepo(final Repository repository) {
		return repository != null
				&& !repository.isBare()
				&& repository.getWorkTree().exists();
	}

	/**
	 * Clear the view's state.
	 * <p>
	 * This method must be called from the UI-thread
	 *
	 * @param repository
	 */
	private void clearRepository(@Nullable Repository repository) {
		saveCommitMessageComponentState();
		removeRepositoryListeners();
		realRepository = repository;
		currentRepository = null;
		currentPushMode.clear();
		if (isDisposed()) {
			return;
		}
		StagingViewUpdate update = new StagingViewUpdate(null, null, null);
		setStagingViewerInput(unstagedViewer, update, null, null);
		setStagingViewerInput(stagedViewer, update, null, null);
		pushSettings.load(null);
		enableCommitWidgets(false);
		refreshAction.setEnabled(false);
		updateSectionText();
		titleNode = null;
		form.setImage(getImage(UIIcons.REPOSITORY));
		if (repository != null && repository.isBare()) {
			form.setText(UIText.StagingView_BareRepoSelection);
		} else {
			form.setText(UIText.StagingView_NoSelectionTitle);
		}
		updateIgnoreErrorsButtonVisibility();
		updateRebaseButtonVisibility(false);
		// Force a selection changed event
		unstagedViewer.setSelection(unstagedViewer.getSelection());
	}

	/**
	 * Show rebase buttons only if a rebase operation is in progress
	 *
	 * @param isRebasing
	 *            {@code}true if rebase is in progress
	 */
	protected void updateRebaseButtonVisibility(final boolean isRebasing) {
		asyncExec(() -> {
			showControl(rebaseSection, isRebasing);
			rebaseSection.getParent().layout(true);
		});
	}

	private static void showControl(Control c, final boolean show) {
		c.setVisible(show);
		GridData g = (GridData) c.getLayoutData();
		g.exclude = !show;
	}

	/**
	 * @param isAmending
	 *            if the current commit should be amended
	 */
	public void setAmending(boolean isAmending) {
		if (isDisposed())
			return;
		if (amendPreviousCommitAction.isChecked() != isAmending) {
			amendPreviousCommitAction.setChecked(isAmending);
			amendPreviousCommitAction.run();
		}
	}

	/**
	 * Sets the raw commit message text.
	 *
	 * @param message
	 *            raw commit message to set for current repository
	 */
	public void setCommitText(String message) {
		commitMessageText.setText(message);
	}

	/**
	 * Reload the staging view asynchronously
	 *
	 * @param repository
	 */
	public void reload(final Repository repository) {
		if (isDisposed()) {
			return;
		}
		if (repository == null) {
			asyncUpdate(() -> clearRepository(null));
			return;
		}

		if (!isValidRepo(repository)) {
			asyncUpdate(() -> clearRepository(repository));
			return;
		}

		final boolean repositoryChanged = currentRepository != repository;
		realRepository = repository;
		currentRepository = repository;

		asyncUpdate(() -> {
			if (isDisposed()) {
				return;
			}

			final IndexDiffData indexDiff = doReload(repository);
			boolean indexDiffAvailable = indexDiffAvailable(indexDiff);
			boolean noConflicts = noConflicts(indexDiff);

			if (repositoryChanged) {
				titleNode = new RepositoryNode(null, repository);
				updateTitle(true);
				pushSettings.load(repository);
				currentPushMode.clear();
				// Reset paths, they're from the old repository
				resetPathsToExpand();
				removeRepositoryListeners();
				refsChangedListener = repository.getListenerList()
						.addRefsChangedListener(
								event -> updateRebaseButtonVisibility(repository
										.getRepositoryState().isRebasing()));
				configChangedListener = repository.getListenerList()
						.addConfigChangedListener(
								event -> {
									updateCommitAuthorAndCommitter(repository);
									// Configs related to the push mode or to
									// commit message cleaning might have
									// changed
									asyncExec(() -> {
										currentPushMode.clear();
										updateCommitAndPush(repository);
										setCleanup(repository);
									});
								});
			} else if (titleNode != null) {
				// The label decoration may need an update.
				updateTitle(false);
			}
			final StagingViewUpdate update = new StagingViewUpdate(repository,
					indexDiff, null);
			Object[] unstagedExpanded = unstagedViewer
					.getVisibleExpandedElements();
			Object[] stagedExpanded = stagedViewer.getVisibleExpandedElements();

			int unstagedElementsCount = updateAutoExpand(unstagedViewer,
					getUnstaged(indexDiff));
			int stagedElementsCount = updateAutoExpand(stagedViewer,
					getStaged(indexDiff));
			int elementsCount = unstagedElementsCount + stagedElementsCount;

			if (elementsCount > getMaxLimitForListMode()) {
				listPresentationAction.setEnabled(false);
				if (presentation == Presentation.LIST) {
					compactTreePresentationAction.setChecked(true);
					switchToCompactModeInternal(true);
				} else {
					setExpandCollapseActionsVisible(false,
							unstagedElementsCount <= getMaxLimitForListMode(),
							true);
					setExpandCollapseActionsVisible(true,
							stagedElementsCount <= getMaxLimitForListMode(),
							true);
				}
			} else {
				listPresentationAction.setEnabled(true);
				boolean changed = getPreferenceStore().getBoolean(
						UIPreferences.STAGING_VIEW_PRESENTATION_CHANGED);
				if (changed) {
					listPresentationAction.setChecked(true);
					switchToListMode();
				} else if (presentation != Presentation.LIST) {
					setExpandCollapseActionsVisible(false, true, true);
					setExpandCollapseActionsVisible(true, true, true);
				}
			}

			setStagingViewerInput(unstagedViewer, update, unstagedExpanded,
					pathsToExpandInUnstaged);
			setStagingViewerInput(stagedViewer, update, stagedExpanded,
					pathsToExpandInStaged);
			resetPathsToExpand();
			// Force a selection changed event
			unstagedViewer.setSelection(unstagedViewer.getSelection());
			refreshAction.setEnabled(true);

			RepositoryState repositoryState = repository.getRepositoryState();
			updateRebaseButtonVisibility(repositoryState.isRebasing());

			updateIgnoreErrorsButtonVisibility();

			boolean rebaseContinueEnabled = indexDiffAvailable
					&& repositoryState.isRebasing()
					&& noConflicts;
			rebaseContinueButton.setEnabled(rebaseContinueEnabled);

			isUnbornHead = false;
			if (repositoryState == RepositoryState.SAFE) {
				try {
					Ref head = repository.exactRef(Constants.HEAD);
					if (head != null && head.isSymbolic()
							&& head.getObjectId() == null) {
						isUnbornHead = true;
					}
					currentBranch = repository.getBranch();
				} catch (IOException e) {
					Activator.logError(e.getLocalizedMessage(), e);
				}
			}
			updateCommitMessageComponent(repositoryChanged, indexDiffAvailable);
			enableCommitWidgets(indexDiffAvailable && noConflicts);

			updateCommitButtons();
			updateSectionText();

			if (repositoryChanged && hideUntrackedAction.isChecked()) {
				// Always show untracked files when the repository changed
				hideUntrackedAction.setChecked(false);
				updateUnstagedViewer();
			}
		});
	}

	private void removeRepositoryListeners() {
		if (refsChangedListener != null) {
			refsChangedListener.remove();
			refsChangedListener = null;
		}
		if (configChangedListener != null) {
			configChangedListener.remove();
			configChangedListener = null;
		}
	}

	/**
	 * The max number of changed files we can handle in the "list" presentation
	 * without freezing Eclipse UI for a too long time.
	 *
	 * @return default is 10000
	 */
	private int getMaxLimitForListMode() {
		return Activator.getDefault().getPreferenceStore()
				.getInt(UIPreferences.STAGING_VIEW_MAX_LIMIT_LIST_MODE);
	}

	private static int getUnstaged(@Nullable IndexDiffData indexDiff) {
		if (indexDiff == null) {
			return 0;
		}
		int size = indexDiff.getUntracked().size();
		size += indexDiff.getMissing().size();
		size += indexDiff.getModified().size();
		size += indexDiff.getConflicting().size();
		return size;
	}

	private static int getStaged(@Nullable IndexDiffData indexDiff) {
		if (indexDiff == null) {
			return 0;
		}
		int size = indexDiff.getAdded().size();
		size += indexDiff.getChanged().size();
		size += indexDiff.getRemoved().size();
		return size;
	}

	private int updateAutoExpand(TreeViewer viewer, int newSize) {
		if (newSize > getMaxLimitForListMode()) {
			// auto expand with too many nodes freezes eclipse
			disableAutoExpand(viewer);
		}
		return newSize;
	}

	private void switchToCompactModeInternal(boolean auto) {
		setPresentation(Presentation.COMPACT_TREE, auto);
		listPresentationAction.setChecked(false);
		treePresentationAction.setChecked(false);
		if (auto) {
			setExpandCollapseActionsVisible(false, false, true);
			setExpandCollapseActionsVisible(true, false, true);
		} else {
			setExpandCollapseActionsVisible(false, isExpandAllowed(false),
					true);
			setExpandCollapseActionsVisible(true, isExpandAllowed(true), true);
		}
	}

	private void switchToListMode() {
		setPresentation(Presentation.LIST, false);
		treePresentationAction.setChecked(false);
		compactTreePresentationAction.setChecked(false);
		setExpandCollapseActionsVisible(false, false, false);
		setExpandCollapseActionsVisible(true, false, false);
	}

	private static boolean noConflicts(IndexDiffData indexDiff) {
		return indexDiff == null ? true : indexDiff.getConflicting().isEmpty();
	}

	private static boolean indexDiffAvailable(IndexDiffData indexDiff) {
		return indexDiff == null ? false : true;
	}

	private boolean hasErrorsOrWarnings() {
		return getPreferenceStore()
				.getBoolean(UIPreferences.WARN_BEFORE_COMMITTING)
				&& !ignoreErrors.getSelection()
				&& hasProblemSeverity(
						Integer.parseInt(getPreferenceStore().getString(
										UIPreferences.WARN_BEFORE_COMMITTING_LEVEL)));
	}

	private boolean isCommitBlocked() {
		return getPreferenceStore().getBoolean(UIPreferences.BLOCK_COMMIT)
				&& !ignoreErrors.getSelection() && hasProblemSeverity(Integer
								.parseInt(getPreferenceStore().getString(
								UIPreferences.BLOCK_COMMIT_LEVEL)));
	}

	private IndexDiffData doReload(@NonNull	final Repository repository) {
		IndexDiffCacheEntry entry = IndexDiffCache.INSTANCE
				.getIndexDiffCacheEntry(repository);

		if(cacheEntry != null && cacheEntry != entry)
			cacheEntry.removeIndexDiffChangedListener(myIndexDiffListener);

		cacheEntry = entry;
		cacheEntry.addIndexDiffChangedListener(myIndexDiffListener);

		return cacheEntry.getIndexDiff();
	}

	private void expandPreviousExpandedAndPaths(Object[] previous,
			TreeViewer viewer, Set<IPath> additionalPaths) {

		StagingViewContentProvider stagedContentProvider = getContentProvider(
				viewer);
		int count = stagedContentProvider.getCount();
		updateAutoExpand(viewer, count);

		// Auto-expand is on, so don't change expanded items
		if (viewer.getAutoExpandLevel() == AbstractTreeViewer.ALL_LEVELS) {
			return;
		}

		// No need to expand anything
		if (getPresentation() == Presentation.LIST)
			return;

		Set<IPath> paths = new HashSet<>(additionalPaths);
		// Instead of just expanding the previous elements directly, also expand
		// all parent paths. This makes it work in case of "re-folding" of
		// compact tree.
		for (Object element : previous) {
			if (element instanceof StagingFolderEntry) {
				addPathAndParentPaths(((StagingFolderEntry) element).getPath(), paths);
			}
		}
		// Also consider the currently expanded elements because auto selection
		// could have expanded some elements.
		for (Object element : viewer.getVisibleExpandedElements()) {
			if (element instanceof StagingFolderEntry) {
				addPathAndParentPaths(((StagingFolderEntry) element).getPath(),
						paths);
			}
		}

		List<StagingFolderEntry> expand = new ArrayList<>();

		calculateNodesToExpand(paths, stagedContentProvider.getElements(null),
				expand);
		viewer.setExpandedElements(expand.toArray());
	}

	private void calculateNodesToExpand(Set<IPath> paths, Object[] elements,
			List<StagingFolderEntry> result) {
		if (elements == null)
			return;

		for (Object element : elements) {
			if (element instanceof StagingFolderEntry) {
				StagingFolderEntry folder = (StagingFolderEntry) element;
				if (paths.contains(folder.getPath())) {
					result.add(folder);
					// Only recurs if folder matched (i.e. don't try to expand
					// children of unexpanded parents)
					calculateNodesToExpand(paths, folder.getChildren(), result);
				}
			}
		}
	}

	private void clearCommitMessageToggles() {
		amendPreviousCommitAction.setChecked(false);
		addChangeIdAction.setChecked(false);
		signedOffByAction.setChecked(false);
		signCommitAction.setChecked(false);
	}

	void updateCommitMessageComponent(boolean repositoryChanged, boolean indexDiffAvailable) {
		if (repositoryChanged)
			if (commitMessageComponent.isAmending()
					|| userEnteredCommitMessage())
				saveCommitMessageComponentState();
			else
				deleteCommitMessageComponentState();
		if (!indexDiffAvailable)
			return; // only try to restore the stored repo commit message if
					// indexDiff is ready

		CommitHelper helper = new CommitHelper(currentRepository);
		CommitMessageComponentState oldState = null;
		boolean changed = false;
		if (repositoryChanged
				|| commitMessageComponent.getRepository() != currentRepository) {
			oldState = loadCommitMessageComponentState();
			commitMessageComponent.setRepository(currentRepository);
			if (oldState == null) {
				loadInitialState(helper);
			} else {
				loadExistingState(helper, oldState);
			}
			changed = true;
		} else { // repository did not change
			if (!commitMessageComponent.getHeadCommit().equals(
					helper.getPreviousCommit())
					|| !commitMessageComponent.isAmending()) {
				if (!commitMessageComponent.isAmending()
						&& userEnteredCommitMessage())
					addHeadChangedWarning(commitMessageText.getText(),
							commitMessageComponent.getCommentChar());
				else {
					loadInitialState(helper);
				}
				changed = true;
			}
		}
		if (changed && previewAction.isChecked()) {
			previewAction.setChecked(false);
			previewAction.run();
		}
		amendPreviousCommitAction.setChecked(commitMessageComponent
				.isAmending());
		amendPreviousCommitAction.setEnabled(helper.amendAllowed());
		updateMessage();
	}

	private void updateCommitAuthorAndCommitter(Repository repository) {
		CommitHelper helper = new CommitHelper(repository);
		asyncExec(() -> {
			boolean authorEqualsCommitter = commitMessageComponent.getAuthor()
					.equals(commitMessageComponent.getCommitter());
			if (authorEqualsCommitter) {
				commitMessageComponent.setAuthor(helper.getAuthor());
			}
			commitMessageComponent.setCommitter(helper.getCommitter());
			commitMessageComponent.updateUIFromState(false);
		});
	}

	/**
	 * Resets the commit message component state and saves the overwritten
	 * commit message into message history
	 */
	public void resetCommitMessageComponent() {
		if (currentRepository != null) {
			String commitMessage = commitMessageComponent.getCommitMessage();
			if (commitMessage.trim().length() > 0) {
				CommitMessageHistory.saveCommitHistory(commitMessage);
			}
			loadInitialState(new CommitHelper(currentRepository));
		}
	}

	private void loadExistingState(CommitHelper helper,
			CommitMessageComponentState oldState) {
		boolean headCommitChanged = !oldState.getHeadCommit().equals(
				getCommitId(helper.getPreviousCommit()));
		commitMessageComponent.enableListeners(false);
		commitMessageComponent.setAuthor(oldState.getAuthor());
		String oldMessage = oldState.getCommitMessage(); // Raw, not cleaned
		CommitConfig config = commitMessageComponent.getRepository().getConfig()
				.get(CommitConfig.KEY);
		// If the old state stored a comment char, use that one
		char commentChar = oldState.getAutoCommentChar();
		if (config.isAutoCommentChar() || commentChar != '\0') {
			if (commentChar == '\0') {
				commentChar = config
						.getCommentChar(Utils.normalizeLineEndings(oldMessage));
			}
			commitMessageComponent.setAutoCommentChar(commentChar);
		} else {
			commentChar = config.getCommentChar();
			commitMessageComponent.setAutoCommentChar('\0');
		}
		commitMessageComponent.setCommentChar(commentChar);
		CleanupMode mode = config.resolve(CleanupMode.DEFAULT, true);
		commitMessageText.setCleanupMode(mode, commentChar);
		if (headCommitChanged) {
			addHeadChangedWarning(oldMessage, commentChar);
		} else {
			commitMessageComponent
					.setCommitMessage(oldMessage);
			commitMessageComponent
					.setCaretPosition(oldState.getCaretPosition());
		}
		commitMessageComponent.setCommitter(oldState.getCommitter());
		commitMessageComponent.setHeadCommit(getCommitId(helper
				.getPreviousCommit()));
		commitMessageComponent.setCommitAllowed(helper.canCommit());
		commitMessageComponent.setCannotCommitMessage(helper.getCannotCommitMessage());
		boolean amendAllowed = helper.amendAllowed();
		commitMessageComponent.setAmendAllowed(amendAllowed);
		if (!amendAllowed)
			commitMessageComponent.setAmending(false);
		else if (!headCommitChanged && oldState.getAmend())
			commitMessageComponent.setAmending(true);
		else
			commitMessageComponent.setAmending(false);
		commitMessageComponent.setSignCommit(oldState.getSign());
		commitMessageComponent.updateUIFromState();
		commitMessageComponent.updateSignedOffAndChangeIdButton();
		commitMessageComponent.enableListeners(true);
	}

	private void addHeadChangedWarning(String commitMessage,
			char commentStart) {
		String warning = new StringBuilder().append(commentStart).append(' ')
				.append(UIText.StagingView_headCommitChanged).toString();
		if (!commitMessage.startsWith(warning)) {
			String message = warning + Text.DELIMITER + Text.DELIMITER
					+ commitMessage;
			commitMessageComponent.setCommitMessage(message);
		}
	}

	private void loadInitialState(CommitHelper helper) {
		commitMessageComponent.enableListeners(false);
		commitMessageComponent.resetState();
		commitMessageComponent.setAuthor(helper.getAuthor());
		CommitConfig config = commitMessageComponent.getRepository().getConfig()
				.get(CommitConfig.KEY);
		String initialMessage;
		char commentChar;
		if (helper.shouldUseCommitTemplate()) {
			initialMessage = helper.getCommitTemplate();
			commentChar = '\0';
		} else {
			initialMessage = helper.getCommitMessage();
			commentChar = helper.getCommentChar();
		}
		if (config.isAutoCommentChar() || commentChar != '\0') {
			if (commentChar == '\0') {
				commentChar = config.getCommentChar(
						Utils.normalizeLineEndings(initialMessage));
			}
			commitMessageComponent.setAutoCommentChar(commentChar);
		} else {
			commentChar = config.getCommentChar();
			commitMessageComponent.setAutoCommentChar('\0');
		}
		commitMessageComponent.setCommentChar(commentChar);
		CleanupMode mode = config.resolve(CleanupMode.DEFAULT, true);
		commitMessageText.setCleanupMode(mode, commentChar);
		commitMessageComponent.setCommitMessage(initialMessage);
		commitMessageComponent.setCommitter(helper.getCommitter());
		commitMessageComponent.setHeadCommit(getCommitId(helper
				.getPreviousCommit()));
		commitMessageComponent.setCommitAllowed(helper.canCommit());
		commitMessageComponent.setCannotCommitMessage(helper.getCannotCommitMessage());
		commitMessageComponent.setAmendAllowed(helper.amendAllowed());
		commitMessageComponent.setAmending(false);
		// set the defaults for change id and signed off buttons.
		commitMessageComponent.setDefaults();
		commitMessageComponent.updateUI();
		commitMessageComponent.enableListeners(true);
	}

	private void setCleanup(Repository repo) {
		if (repo != null) {
			CommitConfig config = repo.getConfig().get(CommitConfig.KEY);
			CleanupMode mode = config.resolve(CleanupMode.DEFAULT, true);
			char commentChar;
			if (config.isAutoCommentChar()) {
				commentChar = commitMessageComponent.getAutoCommentChar();
				if (commentChar == '\0') {
					String currentMessage = Utils
							.normalizeLineEndings(commitMessageText.getText());
					commentChar = config.getCommentChar(currentMessage);
					commitMessageComponent.setCommentChar(commentChar);
					commitMessageComponent.setAutoCommentChar(commentChar);
				}
			} else {
				commentChar = config.getCommentChar();
				commitMessageComponent.setCommentChar(commentChar);
				commitMessageComponent.setAutoCommentChar('\0');
			}
			commitMessageText.setCleanupMode(mode, commentChar);
		} else {
			commitMessageText.setCleanupMode(CleanupMode.STRIP, '#');
		}
		commitMessageText.invalidatePresentation();
	}

	private boolean userEnteredCommitMessage() {
		if (commitMessageComponent.getRepository() == null) {
			return false;
		}
		String warning = new StringBuilder()
				.append(commitMessageComponent.getCommentChar()).append(' ')
				.append(UIText.StagingView_headCommitChanged).toString();
		String message = commitMessageComponent.getCommitMessage()
				.replace(warning, ""); //$NON-NLS-1$
		if (message.trim().isEmpty()) {
			return false;
		}
		String chIdLine = "Change-Id: I" + ObjectId.zeroId().name(); //$NON-NLS-1$
		Repository repo = currentRepository;
		if (repo != null && GerritUtil.getCreateChangeId(repo.getConfig())
				&& commitMessageComponent.getCreateChangeId()) {
			if (message.trim().equals(chIdLine)) {
				return false;
			}
			// change id was added automatically, but there is more in the
			// message; strip the id, and check for the signed-off-by tag
			message = message.replace(chIdLine, ""); //$NON-NLS-1$
		}

		if (org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.COMMIT_DIALOG_SIGNED_OFF_BY)
				&& commitMessageComponent.isSignedOff()
				&& message.trim().equals(
						Constants.SIGNED_OFF_BY_TAG
								+ commitMessageComponent.getCommitter())) {
			return false;
		}
		return true;
	}

	private ObjectId getCommitId(RevCommit commit) {
		if (commit == null)
			return ObjectId.zeroId();
		return commit.getId();
	}

	private void saveCommitMessageComponentState() {
		final Repository repo = commitMessageComponent.getRepository();
		if (repo != null)
			CommitMessageComponentStateManager.persistState(repo,
					commitMessageComponent.getState());
	}

	private void deleteCommitMessageComponentState() {
		if (commitMessageComponent.getRepository() != null)
			CommitMessageComponentStateManager
					.deleteState(commitMessageComponent.getRepository());
	}

	private CommitMessageComponentState loadCommitMessageComponentState() {
		return CommitMessageComponentStateManager.loadState(currentRepository);
	}

	private Collection<String> getStagedFileNames() {
		StagingViewContentProvider stagedContentProvider = getContentProvider(stagedViewer);
		StagingEntry[] entries = stagedContentProvider.getStagingEntries();
		List<String> files = new ArrayList<>();
		for (StagingEntry entry : entries)
			files.add(entry.getPath());
		return files;
	}

	private void commit(boolean pushUpstream) {
		boolean reEnable = false;
		// Don't allow to do anything as long as commit is in progress
		enableAllWidgets(false);
		try {
			boolean jobScheduled = internalCommit(pushUpstream,
					() -> enableAllWidgets(true));
			// If a job was scheduled, our Runnable will re-enable the widgets
			// once the commit and push is done; even if it fails. Otherwise, we
			// must re-enable the widgets here and now ourselves.
			reEnable = !jobScheduled;
		} catch (RuntimeException e) { // See bugs 550336, 550513
			// If internalCommit() should fail after having scheduled the job,
			// we may re-enable a little too early. But better that than never.
			reEnable = true;
			Activator.handleError(e.getLocalizedMessage(), e, true);
		} finally {
			if (reEnable) {
				enableAllWidgets(true);
			}
		}
	}

	/**
	 * Performs some checks and if successful schedules a background job to
	 * execute the commit (and possibly push).
	 *
	 * @param pushUpstream
	 *            whether to also push the commit to upstream
	 * @param afterJob
	 *            code to run after the job if a job is scheduled
	 * @return whether a job was scheduled
	 */
	private boolean internalCommit(boolean pushUpstream, Runnable afterJob) {
		if (!isCommitPossible(new LazyRepositoryState(currentRepository))) {
			MessageDialog md = new MessageDialog(getSite().getShell(),
					UIText.StagingView_committingNotPossible, null,
					UIText.StagingView_noStagedFiles, MessageDialog.ERROR,
					new String[] { IDialogConstants.CLOSE_LABEL }, 0);
			md.open();
			return false;
		}
		if (!commitMessageComponent.checkCommitInfo()) {
			return false;
		}

		if (!UIUtils.saveAllEditors(currentRepository,
				UIText.StagingView_cancelCommitAfterSaving)) {
			return false;
		}

		String commitMessage = commitMessageComponent.getCommitMessage();
		CommitOperation commitOperation = null;
		try {
			commitOperation = new CommitOperation(currentRepository,
					commitMessageComponent.getAuthor(),
					commitMessageComponent.getCommitter(),
					commitMessage);
		} catch (CoreException e) {
			Activator.handleError(UIText.StagingView_commitFailed, e, true);
			return false;
		}
		if (amendPreviousCommitAction.isChecked())
			commitOperation.setAmending(true);
		final boolean withChangeId = addChangeIdAction.isChecked();
		commitOperation.setComputeChangeId(withChangeId);
		commitOperation.setSign(signCommitAction.isChecked());

		PushMode pushMode = null;
		if (pushUpstream) {
			pushMode = getPushMode(currentRepository);
		}
		Job commitJob = new CommitJob(currentRepository, commitOperation)
				.setOpenCommitEditor(openNewCommitsAction.isChecked())
				.setPushUpstream(pushMode)
				.setForce(pushSettings.isForce())
				.setDialog(pushSettings.alwaysShowDialog());

		commitJob.addJobChangeListener(new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				asyncExec(() -> {
					afterJob.run();
					if (event.getResult().isOK()) {
						commitMessageText.setText(EMPTY_STRING);
					}
				});
			}
		});

		schedule(commitJob, true);

		CommitMessageHistory.saveCommitHistory(commitMessage);
		clearCommitMessageToggles();
		return true;
	}

	private PushMode getPushMode(Repository repository) {
		PushMode pushMode = null;
		if (repository != null) {
			pushMode = PushMode.UPSTREAM; // default mode
			boolean withChangeId = addChangeIdAction.isChecked();
			if (repository == currentRepository) {
				PushMode cached = currentPushMode
						.get(Boolean.valueOf(withChangeId));
				if (cached != null) {
					return cached;
				}
			}
			try {
				if (withChangeId && RemoteConfig
						.getAllRemoteConfigs(repository.getConfig()).stream()
						.anyMatch(GerritUtil::isGerritPush)) {
					pushMode = PushMode.GERRIT;
				}
			} catch (URISyntaxException ex) {
				// ignore, stick to default
			}
			if (repository == currentRepository) {
				currentPushMode.put(Boolean.valueOf(withChangeId), pushMode);
			}
		}
		return pushMode;
	}

	/**
	 * Schedule given job in context of the current view. The view will indicate
	 * progress as long as job is running.
	 *
	 * @param job
	 *            non null
	 * @param useRepositoryRule
	 *            true to use current repository rule for the given job, false
	 *            to not enforce any rule on the job
	 */
	private void schedule(Job job, boolean useRepositoryRule) {
		if (useRepositoryRule)
			job.setRule(RuleUtil.getRule(currentRepository));
		IWorkbenchSiteProgressService service = getSite()
				.getService(IWorkbenchSiteProgressService.class);
		if (service != null)
			service.schedule(job, 0, true);
		else
			job.schedule();
	}

	private boolean isCommitPossible(LazyRepositoryState stateSupplier) {
		return stateSupplier.getRepository() != null
				&& stagedViewer.getTree().getItemCount() > 0
				|| amendPreviousCommitAction.isChecked()
				|| CommitHelper.isCommitWithoutFilesAllowed(stateSupplier.get());
	}

	@Override
	public void setFocus() {
		Tree tree = unstagedViewer.getTree();
		if (tree.getItemCount() > 0 && !isAutoStageOnCommitEnabled()) {
			unstagedViewer.getControl().setFocus();
			return;
		}
		commitMessageText.setFocus();
	}

	private boolean isAutoStageOnCommitEnabled() {
		IPreferenceStore uiPreferences = Activator.getDefault()
				.getPreferenceStore();
		return uiPreferences.getBoolean(UIPreferences.AUTO_STAGE_ON_COMMIT);
	}

	@Override
	public void dispose() {
		super.dispose();

		ISelectionService srv = getSite().getService(ISelectionService.class);
		srv.removePostSelectionListener(selectionChangedListener);
		getSite().getService(IPartService.class)
				.removePartListener(partListener);

		if (cacheEntry != null) {
			cacheEntry.removeIndexDiffChangedListener(myIndexDiffListener);
		}

		if (pushSettings != null) {
			pushSettings.dispose();
		}

		if (undoRedoActionGroup != null) {
			undoRedoActionGroup.dispose();
		}

		InstanceScope.INSTANCE
				.getNode(org.eclipse.egit.core.Activator.PLUGIN_ID)
				.removePreferenceChangeListener(prefListener);
		removeRepositoryListeners();

		if (switchRepositoriesAction != null) {
			switchRepositoriesAction.dispose();
			switchRepositoriesAction = null;
		}
		if (presentationAction != null) {
			presentationAction.dispose();
			presentationAction = null;
		}

		getPreferenceStore().removePropertyChangeListener(uiPrefsListener);

		getDialogSettings().put(STORE_SORT_STATE, sortAction.isChecked());

		if (titleLabelProvider != null) {
			titleLabelProvider.dispose();
			titleLabelProvider = null;
		}
		titleNode = null;
		titleLabels.clear();
		currentRepository = null;
		lastSelection = null;
		disposed = true;
	}

	private boolean isDisposed() {
		return disposed;
	}

	private static void syncExec(Runnable runnable) {
		PlatformUI.getWorkbench().getDisplay().syncExec(runnable);
	}

	private void asyncExec(Runnable runnable) {
		if (!isDisposed()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
				if (!isDisposed()) {
					runnable.run();
				}
			});
		}
	}

	private void asyncUpdate(Runnable runnable) {
		if (isDisposed()) {
			return;
		}
		Job update = new WorkbenchJob(UIText.StagingView_LoadJob) {

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				try {
					runnable.run();
					return Status.OK_STATUS;
				} catch (Exception e) {
					return Activator.createErrorStatus(e.getLocalizedMessage(),
							e);
				}
			}

			@Override
			public boolean shouldSchedule() {
				return super.shouldSchedule() && !isDisposed();
			}

			@Override
			public boolean shouldRun() {
				return super.shouldRun() && !isDisposed();
			}

			@Override
			public boolean belongsTo(Object family) {
				return family == JobFamilies.STAGING_VIEW_RELOAD
						|| super.belongsTo(family);
			}
		};
		update.setSystem(true);
		update.schedule();
	}

	/**
	 * This comparator sorts the {@link StagingEntry}s alphabetically or groups
	 * them by state. If grouped by state the entries in the same group are also
	 * ordered alphabetically.
	 */
	private static class StagingEntryComparator extends ViewerComparator {

		private boolean alphabeticSort;

		private Comparator<String> comparator;

		private boolean fileNamesFirst;

		private StagingEntryComparator(boolean alphabeticSort,
				boolean fileNamesFirst) {
			this.alphabeticSort = alphabeticSort;
			this.setFileNamesFirst(fileNamesFirst);
			comparator = CommonUtils.STRING_ASCENDING_COMPARATOR;
		}

		public boolean isFileNamesFirst() {
			return fileNamesFirst;
		}

		public void setFileNamesFirst(boolean fileNamesFirst) {
			this.fileNamesFirst = fileNamesFirst;
		}

		private void setAlphabeticSort(boolean sort) {
			this.alphabeticSort = sort;
		}

		private boolean isAlphabeticSort() {
			return alphabeticSort;
		}

		@Override
		public int category(Object element) {
			if (!isAlphabeticSort()) {
				StagingEntry stagingEntry = getStagingEntry(element);
				if (stagingEntry != null) {
					return getState(stagingEntry);
				}
			}
			return super.category(element);
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int cat1 = category(e1);
			int cat2 = category(e2);

			if (cat1 != cat2) {
				return cat1 - cat2;
			}

			String name1 = getStagingEntryText(e1);
			String name2 = getStagingEntryText(e2);

			return comparator.compare(name1, name2);
		}

		private String getStagingEntryText(Object element) {
			String text = ""; //$NON-NLS-1$
			StagingEntry stagingEntry = getStagingEntry(element);
			// Replace slashes by ASCII \001 to make e.g.
			// "org.eclipse.egit.gitflow/..." sort before
			// "org.eclipse.egit.gitflow.ui/...".
			if (stagingEntry != null) {
				text = stagingEntry.getPath().replace('/', '\001');
				if (isFileNamesFirst()) {
					text = stagingEntry.getName() + '\001' + text;
				}
			} else if (element instanceof StagingFolderEntry) {
				text = ((StagingFolderEntry) element).getLabel()
						.replace('/', '\001');
			}
			return text;
		}

		private int getState(StagingEntry entry) {
			switch (entry.getState()) {
			case CONFLICTING:
				return 1;
			case MODIFIED:
				return 2;
			case MODIFIED_AND_ADDED:
				return 3;
			case MODIFIED_AND_CHANGED:
				return 4;
			case ADDED:
				return 5;
			case CHANGED:
				return 6;
			case MISSING:
				return 7;
			case MISSING_AND_CHANGED:
				return 8;
			case REMOVED:
				return 9;
			case UNTRACKED:
				return 10;
			default:
				return super.category(entry);
			}
		}

	}

	private static class PresentationAction extends DropDownMenuAction
			implements IPropertyChangeListener {

		private final IPreferenceStore store;

		private final @NonNull List<IAction> actions;

		public PresentationAction(IPreferenceStore store, IAction... actions) {
			super(UIText.StagingView_Presentation);
			@SuppressWarnings("null")
			@NonNull
			List<IAction> a = actions != null ? Arrays.asList(actions)
					: Collections.emptyList();
			this.actions = a;
			this.store = store;
			store.addPropertyChangeListener(this);
		}

		@Override
		protected Collection<IContributionItem> getActions() {
			return actions.stream().map(ActionContributionItem::new)
					.collect(Collectors.toList());
		}

		@Override
		public void dispose() {
			store.removePropertyChangeListener(this);
			super.dispose();
		}

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (UIPreferences.STAGING_VIEW_PRESENTATION
					.equals(event.getProperty())) {
				Presentation current = getPresentation(
						event.getNewValue().toString(), Presentation.LIST);
				switch (current) {
				case LIST:
					setImageDescriptor(UIIcons.FLAT);
					break;
				case TREE:
					setImageDescriptor(UIIcons.HIERARCHY);
					break;
				case COMPACT_TREE:
					setImageDescriptor(UIIcons.COMPACT);
					break;
				default:
					return;
				}
			}
		}
	}

	private boolean canPushHeadOnly(LazyRepositoryState stateSupplier) {
		Repository repo = stateSupplier.getRepository();
		try {
			return repo != null && repo.resolve(Constants.HEAD) != null
					&& !isCommitPossible(stateSupplier);
		} catch (IOException e) {
			return false;
		}
	}

	private boolean canPushWithoutConfirmation(PushMode pushMode) {
		Repository repo = currentRepository;
		if (repo != null && pushMode != PushMode.GERRIT) {
			final RemoteConfig config = SimpleConfigurePushDialog
					.getConfiguredRemote(repo);
			boolean alwaysShowPushWizard = pushSettings.alwaysShowDialog();
			return config != null && !alwaysShowPushWizard;
		}
		return false;
	}

	@Nullable
	static StagingEntry getStagingEntry(Object element) {
		StagingEntry entry = null;
		if (element instanceof StagingEntry) {
			entry = (StagingEntry) element;
		}
		if (element instanceof TreeItem) {
			TreeItem item = (TreeItem) element;
			if (item.getData() instanceof StagingEntry) {
				entry = (StagingEntry) item.getData();
			}
		}
		return entry;
	}
}
