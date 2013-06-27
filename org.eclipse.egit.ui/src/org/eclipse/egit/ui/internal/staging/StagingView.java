/*******************************************************************************
 * Copyright (C) 2011, 2013 Bernard Leach <leachbj@bouncycastle.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import static org.eclipse.egit.ui.internal.CommonUtils.runCommand;
import static org.eclipse.ui.ISources.ACTIVE_MENU_SELECTION_NAME;
import static org.eclipse.ui.menus.CommandContributionItem.STYLE_PUSH;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffChangedListener;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.EgitUiEditorUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.actions.ActionCommands;
import org.eclipse.egit.ui.internal.actions.BooleanPrefAction;
import org.eclipse.egit.ui.internal.commit.CommitHelper;
import org.eclipse.egit.ui.internal.commit.CommitJob;
import org.eclipse.egit.ui.internal.commit.CommitMessageHistory;
import org.eclipse.egit.ui.internal.commit.CommitProposalProcessor;
import org.eclipse.egit.ui.internal.components.ToggleableWarningLabel;
import org.eclipse.egit.ui.internal.decorators.ProblemLabelDecorator;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageArea;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponent;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponentState;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponentStateManager;
import org.eclipse.egit.ui.internal.dialogs.ICommitMessageComponentNotifications;
import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea;
import org.eclipse.egit.ui.internal.operations.DeletePathsOperationUI;
import org.eclipse.egit.ui.internal.operations.IgnoreOperationUI;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;

/**
 * A GitX style staging view with embedded commit dialog.
 */
public class StagingView extends ViewPart implements IShowInSource {

	/**
	 * Staging view id
	 */
	public static final String VIEW_ID = "org.eclipse.egit.ui.StagingView"; //$NON-NLS-1$

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private FormToolkit toolkit;

	private Form form;

	private Section stagedSection;

	private Section unstagedSection;

	private Section commitMessageSection;

	private TreeViewer stagedViewer;

	private TreeViewer unstagedViewer;

	private ToggleableWarningLabel warningLabel;

	private Text filterText;

	private SpellcheckableMessageArea commitMessageText;

	private Text committerText;

	private Text authorText;

	private CommitMessageComponent commitMessageComponent;

	private boolean reactOnSelection = true;

	private ISelectionListener selectionChangedListener;

	private ToolBarManager unstagedToolBarManager;

	private ToolBarManager stagedToolBarManager;

	private Action listPresentationAction;

	private Action treePresentationAction;

	private Action compactTreePresentationAction;

	private Action unstagedExpandAllAction;

	private Action unstagedCollapseAllAction;

	private Action stagedExpandAllAction;

	private Action stagedCollapseAllAction;

	private Repository currentRepository;

	private Presentation presentation = Presentation.LIST;

	private Set<IPath> pathsToExpandInStaged = new HashSet<IPath>();

	private Set<IPath> pathsToExpandInUnstaged = new HashSet<IPath>();

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

	static class StagingDragListener extends DragSourceAdapter {

		private ISelectionProvider provider;

		public StagingDragListener(ISelectionProvider provider) {
			this.provider = provider;
		}

		public void dragStart(DragSourceEvent event) {
			event.doit = !provider.getSelection().isEmpty();
		}

		public void dragFinished(DragSourceEvent event) {
			if (LocalSelectionTransfer.getTransfer().isSupportedType(
					event.dataType))
				LocalSelectionTransfer.getTransfer().setSelection(null);
		}

		public void dragSetData(DragSourceEvent event) {
			IStructuredSelection selection = (IStructuredSelection) provider
					.getSelection();
			if (selection.isEmpty())
				return;

			if (LocalSelectionTransfer.getTransfer().isSupportedType(
					event.dataType)) {
				LocalSelectionTransfer.getTransfer().setSelection(selection);
				return;
			}

			if (FileTransfer.getInstance().isSupportedType(event.dataType)) {
				List<String> files = new ArrayList<String>();
				for (Object selected : selection.toList())
					if (selected instanceof StagingEntry) {
						StagingEntry entry = (StagingEntry) selected;
						File file = new File(
								entry.getRepository().getWorkTree(),
								entry.getPath());
						if (file.exists())
							files.add(file.getAbsolutePath());
					}
				if (!files.isEmpty()) {
					event.data = files.toArray(new String[files.size()]);
					return;
				}
			}
		}
	}

	class TreeDecoratingLabelProvider extends DecoratingLabelProvider {

		ILabelProvider provider;

		ILabelDecorator decorator;

		public TreeDecoratingLabelProvider(ILabelProvider provider,
				ILabelDecorator decorator) {
			super(provider, decorator);
			this.provider = provider;
			this.decorator = decorator;
		}

		public Image getColumnImage(Object element) {
			Image image = provider.getImage(element);
			if (image != null && decorator != null) {
				Image decorated = decorator.decorateImage(image, element);
				if (decorated != null)
					return decorated;
			}
			return image;
		}

		public String getText(Object element) {
			return provider.getText(element);
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

		public void run() {
			synchronized (lock) {
				if (currentThreadIx < globalThreadIndex)
					return;
				stagingView.refreshViewers();
			}
		}

	}

	private final IPreferenceChangeListener prefListener = new IPreferenceChangeListener() {

		public void preferenceChange(PreferenceChangeEvent event) {
			if (!RepositoryUtil.PREFS_DIRECTORIES.equals(event.getKey()))
				return;

			final Repository repo = currentRepository;
			if (repo == null)
				return;

			if (Activator.getDefault().getRepositoryUtil().contains(repo))
				return;

			reload(null);
		}

	};

	private Action signedOffByAction;

	private Action addChangeIdAction;

	private Action amendPreviousCommitAction;

	private Action openNewCommitsAction;

	private Action columnLayoutAction;

	private Action fileNameModeAction;

	private Action refreshAction;

	private SashForm stagingSashForm;

	private IndexDiffChangedListener myIndexDiffListener = new IndexDiffChangedListener() {
		public void indexDiffChanged(Repository repository,
				IndexDiffData indexDiffData) {
			reload(repository);
		}
	};

	private IndexDiffCacheEntry cacheEntry;

	private UndoRedoActionGroup undoRedoActionGroup;

	private Button commitButton;

	private Button commitAndPushButton;

	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);

		toolkit = new FormToolkit(parent.getDisplay());
		parent.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				toolkit.dispose();
			}
		});

		form = toolkit.createForm(parent);

		Image repoImage = UIIcons.REPOSITORY.createImage();
		UIUtils.hookDisposal(form, repoImage);
		form.setImage(repoImage);
		form.setText(UIText.StagingView_NoSelectionTitle);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);
		toolkit.decorateFormHeading(form);
		GridLayoutFactory.swtDefaults().applyTo(form.getBody());

		SashForm horizontalSashForm = new SashForm(form.getBody(), SWT.NONE);
		toolkit.adapt(horizontalSashForm, true, true);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(horizontalSashForm);

		stagingSashForm = new SashForm(horizontalSashForm,
				getStagingFormOrientation());
		toolkit.adapt(stagingSashForm, true, true);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(stagingSashForm);

		unstagedSection = toolkit.createSection(stagingSashForm,
				ExpandableComposite.TITLE_BAR);

		createUnstagedToolBarComposite();

		Composite unstagedComposite = toolkit
				.createComposite(unstagedSection);
		toolkit.paintBordersFor(unstagedComposite);
		unstagedSection.setClient(unstagedComposite);
		GridLayoutFactory.fillDefaults().extendedMargins(2, 2, 2, 2)
				.applyTo(unstagedComposite);

		unstagedViewer = createTree(unstagedComposite);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(unstagedViewer.getControl());
		unstagedViewer.getTree().setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TREE_BORDER);
		unstagedViewer.getTree().setLinesVisible(true);
		unstagedViewer.setLabelProvider(createLabelProvider(unstagedViewer));
		unstagedViewer.setContentProvider(new StagingViewContentProvider(this,
				true));
		unstagedViewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY
				| DND.DROP_LINK,
				new Transfer[] { LocalSelectionTransfer.getTransfer(),
						FileTransfer.getInstance() }, new StagingDragListener(
						unstagedViewer));
		unstagedViewer.addDropSupport(DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DropTargetAdapter() {
					public void drop(DropTargetEvent event) {
						// Bug 411466: It is very important that detail is set
						// to DND.DROP_COPY. If it was left as DND.DROP_MOVE and
						// the drag comes from the Navigator view, the code in
						// NavigatorDragAdapter would delete the resources.
						event.detail = DND.DROP_COPY;
						if (event.data instanceof IStructuredSelection) {
							final IStructuredSelection selection = (IStructuredSelection) event.data;
							unstage(selection);
						}
					}

					public void dragOver(DropTargetEvent event) {
						event.detail = DND.DROP_MOVE;
					}
				});
		unstagedViewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				compareWith(event);
			}
		});

		commitMessageSection = toolkit.createSection(
				horizontalSashForm, ExpandableComposite.TITLE_BAR);
		commitMessageSection.setText(UIText.StagingView_CommitMessage);

		Composite commitMessageToolbarComposite = toolkit
				.createComposite(commitMessageSection);
		commitMessageToolbarComposite.setBackground(null);
		commitMessageToolbarComposite.setLayout(createRowLayoutWithoutMargin());
		commitMessageSection.setTextClient(commitMessageToolbarComposite);
		ToolBarManager commitMessageToolBarManager = new ToolBarManager(
				SWT.FLAT | SWT.HORIZONTAL);

		amendPreviousCommitAction = new Action(
				UIText.StagingView_Ammend_Previous_Commit, IAction.AS_CHECK_BOX) {

			public void run() {
				commitMessageComponent.setAmendingButtonSelection(isChecked());
				updateMessage();
			}
		};
		amendPreviousCommitAction.setImageDescriptor(UIIcons.AMEND_COMMIT);
		commitMessageToolBarManager.add(amendPreviousCommitAction);

		signedOffByAction = new Action(UIText.StagingView_Add_Signed_Off_By,
				IAction.AS_CHECK_BOX) {

			public void run() {
				commitMessageComponent.setSignedOffButtonSelection(isChecked());
			}
		};
		signedOffByAction.setImageDescriptor(UIIcons.SIGNED_OFF);
		commitMessageToolBarManager.add(signedOffByAction);

		addChangeIdAction = new Action(UIText.StagingView_Add_Change_ID,
				IAction.AS_CHECK_BOX) {

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

		warningLabel = new ToggleableWarningLabel(commitMessageComposite,
				SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).exclude(true)
				.applyTo(warningLabel);

		Composite commitMessageTextComposite = toolkit
				.createComposite(commitMessageComposite);
		toolkit.paintBordersFor(commitMessageTextComposite);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(commitMessageTextComposite);
		GridLayoutFactory.fillDefaults().numColumns(1)
				.extendedMargins(2, 2, 2, 2)
				.applyTo(commitMessageTextComposite);

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
				EMPTY_STRING, toolkit.getBorderStyle()) {
			@Override
			protected CommitProposalProcessor getCommitProposalProcessor() {
				return commitProposalProcessor;
			}
			@Override
			protected IHandlerService getHandlerService() {
				return (IHandlerService) getSite().getService(IHandlerService.class);
			}
		};
		commitMessageText.setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TEXT_BORDER);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(commitMessageText);
		UIUtils.addBulbDecorator(commitMessageText.getTextWidget(),
				UIText.CommitDialog_ContentAssist);

		Composite composite = toolkit.createComposite(commitMessageComposite);
		toolkit.paintBordersFor(composite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(composite);

		toolkit.createLabel(composite, UIText.StagingView_Author)
				.setForeground(
						toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
		authorText = toolkit.createText(composite, null);
		authorText
				.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		authorText.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());

		toolkit.createLabel(composite, UIText.StagingView_Committer)
				.setForeground(
						toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
		committerText = toolkit.createText(composite, null);
		committerText.setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TEXT_BORDER);
		committerText.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());

		Composite buttonsContainer = toolkit.createComposite(composite);
		GridDataFactory.fillDefaults().grab(true, false).span(2,1).indent(0, 8)
			.applyTo(buttonsContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(buttonsContainer);

		Label filler = toolkit.createLabel(buttonsContainer, ""); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(filler);

		Composite commitButtonsContainer = toolkit.createComposite(buttonsContainer);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.applyTo(commitButtonsContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(true).applyTo(commitButtonsContainer);

		this.commitAndPushButton = toolkit.createButton(commitButtonsContainer,
				UIText.StagingView_CommitAndPush, SWT.PUSH);
		commitAndPushButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				commit(true);
			}
		});
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.applyTo(commitAndPushButton);

		this.commitButton = toolkit.createButton(commitButtonsContainer,
				UIText.StagingView_Commit, SWT.PUSH);

		commitButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				commit(false);
			}
		});
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.applyTo(commitButton);

		stagedSection = toolkit.createSection(stagingSashForm,
				ExpandableComposite.TITLE_BAR);

		createStagedToolBarComposite();

		Composite stagedComposite = toolkit.createComposite(stagedSection);
		toolkit.paintBordersFor(stagedComposite);
		stagedSection.setClient(stagedComposite);
		GridLayoutFactory.fillDefaults().extendedMargins(2, 2, 2, 2)
				.applyTo(stagedComposite);

		stagedViewer = createTree(stagedComposite);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(stagedViewer.getControl());
		stagedViewer.getTree().setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TREE_BORDER);
		stagedViewer.getTree().setLinesVisible(true);
		stagedViewer.setLabelProvider(createLabelProvider(stagedViewer));
		stagedViewer.setContentProvider(new StagingViewContentProvider(this,
				false));
		stagedViewer.addDragSupport(
				DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK,
				new Transfer[] { LocalSelectionTransfer.getTransfer(),
						FileTransfer.getInstance() }, new StagingDragListener(
						stagedViewer));
		stagedViewer.addDropSupport(DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DropTargetAdapter() {
					public void drop(DropTargetEvent event) {
						// Bug 411466: It is very important that detail is set
						// to DND.DROP_COPY. If it was left as DND.DROP_MOVE and
						// the drag comes from the Navigator view, the code in
						// NavigatorDragAdapter would delete the resources.
						event.detail = DND.DROP_COPY;
						if (event.data instanceof IStructuredSelection) {
							final IStructuredSelection selection = (IStructuredSelection) event.data;
							Object firstElement = selection.getFirstElement();
							if (firstElement instanceof StagingEntry)
								stage(selection);
							else {
								IResource resource = AdapterUtils.adapt(firstElement, IResource.class);
								if (resource != null)
									stage(selection);
							}
						}
					}

					public void dragOver(DropTargetEvent event) {
						event.detail = DND.DROP_MOVE;
					}
				});
		stagedViewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				compareWith(event);
			}
		});

		selectionChangedListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				if (!reactOnSelection || part == getSite().getPart())
					return;

				// this may happen if we switch between editors
				if (part instanceof IEditorPart) {
					IEditorInput input = ((IEditorPart) part).getEditorInput();
					if (input instanceof IFileEditorInput)
						reactOnSelection(new StructuredSelection(
								((IFileEditorInput) input).getFile()));
				} else
					reactOnSelection(selection);
			}
		};

		IPreferenceStore preferenceStore = getPreferenceStore();
		if (preferenceStore.contains(UIPreferences.STAGING_VIEW_SYNC_SELECTION))
			reactOnSelection = preferenceStore.getBoolean(
					UIPreferences.STAGING_VIEW_SYNC_SELECTION);
		else
			preferenceStore.setDefault(UIPreferences.STAGING_VIEW_SYNC_SELECTION, true);

		InstanceScope.INSTANCE.getNode(
				org.eclipse.egit.core.Activator.getPluginId())
				.addPreferenceChangeListener(prefListener);

		updateSectionText();
		updateToolbar();
		enableCommitWidgets(false);

		createPopupMenu(unstagedViewer);
		createPopupMenu(stagedViewer);

		final ICommitMessageComponentNotifications listener = new ICommitMessageComponentNotifications() {

			public void updateSignedOffToggleSelection(boolean selection) {
				signedOffByAction.setChecked(selection);
			}

			public void updateChangeIdToggleSelection(boolean selection) {
				addChangeIdAction.setChecked(selection);
			}
		};
		commitMessageComponent = new CommitMessageComponent(listener);
		commitMessageComponent.attachControls(commitMessageText, authorText,
				committerText);

		// allow to commit with ctrl-enter
		commitMessageText.getTextWidget().addVerifyKeyListener(new VerifyKeyListener() {
			public void verifyKey(VerifyEvent event) {
				if (UIUtils.isSubmitKeyEvent(event)) {
					event.doit = false;
					commit(false);
				}
			}
		});

		commitMessageText.getTextWidget().addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				// Ctrl+Enter shortcut only works when the focus is on the commit message text
				String commitButtonTooltip = MessageFormat.format(
						UIText.StagingView_CommitToolTip,
						UIUtils.SUBMIT_KEY_STROKE.format());
				commitButton.setToolTipText(commitButtonTooltip);
			}

			public void focusLost(FocusEvent e) {
				commitButton.setToolTipText(null);
			}
		});

		ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateMessage();
			}
		};
		authorText.addModifyListener(modifyListener);
		committerText.addModifyListener(modifyListener);

		// react on selection changes
		IWorkbenchPartSite site = getSite();
		ISelectionService srv = (ISelectionService) site
				.getService(ISelectionService.class);
		srv.addPostSelectionListener(selectionChangedListener);

		// Use current selection to populate staging view
		ISelection selection = srv.getSelection();
		if (selection != null && !selection.isEmpty()) {
			IWorkbenchPart part = site.getPage().getActivePart();
			if (part != null)
				selectionChangedListener.selectionChanged(part, selection);
		}

		site.setSelectionProvider(unstagedViewer);

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
	}

	private void createUnstagedToolBarComposite() {
		Composite unstagedToolbarComposite = toolkit
				.createComposite(unstagedSection);
		unstagedToolbarComposite.setBackground(null);
		unstagedToolbarComposite.setLayout(createRowLayoutWithoutMargin());
		unstagedSection.setTextClient(unstagedToolbarComposite);
		unstagedExpandAllAction = new Action(UIText.UIUtils_ExpandAll,
				IAction.AS_PUSH_BUTTON) {
			public void run() {
				unstagedViewer.expandAll();
			}
		};
		unstagedExpandAllAction.setImageDescriptor(UIIcons.EXPAND_ALL);

		unstagedCollapseAllAction = new Action(UIText.UIUtils_CollapseAll,
				IAction.AS_PUSH_BUTTON) {
			public void run() {
				unstagedViewer.collapseAll();
			}
		};
		unstagedCollapseAllAction.setImageDescriptor(UIIcons.COLLAPSEALL);

		unstagedToolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);

		unstagedToolBarManager.add(unstagedExpandAllAction);
		unstagedToolBarManager.add(unstagedCollapseAllAction);

		unstagedToolBarManager.update(true);
		unstagedToolBarManager.createControl(unstagedToolbarComposite);
	}

	private void createStagedToolBarComposite() {
		Composite stagedToolbarComposite = toolkit
				.createComposite(stagedSection);
		stagedToolbarComposite.setBackground(null);
		stagedToolbarComposite.setLayout(createRowLayoutWithoutMargin());
		stagedSection.setTextClient(stagedToolbarComposite);
		stagedExpandAllAction = new Action(UIText.UIUtils_ExpandAll,
				IAction.AS_PUSH_BUTTON) {
			public void run() {
				stagedViewer.expandAll();
			}
		};
		stagedExpandAllAction.setImageDescriptor(UIIcons.EXPAND_ALL);

		stagedCollapseAllAction = new Action(UIText.UIUtils_CollapseAll,
				IAction.AS_PUSH_BUTTON) {
			public void run() {
				stagedViewer.collapseAll();
			}
		};
		stagedCollapseAllAction.setImageDescriptor(UIIcons.COLLAPSEALL);

		stagedToolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);

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

	/**
	 * @return selected repository
	 */
	public Repository getCurrentRepository() {
		return currentRepository;
	}

	public ShowInContext getShowInContext() {
		if (stagedViewer != null && stagedViewer.getTree().isFocusControl())
			return getShowInContext(stagedViewer);
		else if (unstagedViewer != null
				&& unstagedViewer.getTree().isFocusControl())
			return getShowInContext(unstagedViewer);
		else
			return null;
	}

	private ShowInContext getShowInContext(TreeViewer treeViewer) {
		IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
		List<Object> elements = new ArrayList<Object>();
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

	private void enableCommitWidgets(boolean enabled) {
		if (!enabled) {
			commitMessageText.setText(""); //$NON-NLS-1$
			committerText.setText(""); //$NON-NLS-1$
			authorText.setText(""); //$NON-NLS-1$
		}

		commitMessageText.setEnabled(enabled);
		committerText.setEnabled(enabled);
		authorText.setEnabled(enabled);
		refreshAction.setEnabled(enabled);
		amendPreviousCommitAction.setEnabled(enabled);
		signedOffByAction.setEnabled(enabled);
		addChangeIdAction.setEnabled(enabled);
		commitButton.setEnabled(enabled);
		commitAndPushButton.setEnabled(enabled);
	}

	private void updateToolbar() {

		ControlContribution controlContribution = new ControlContribution(
				"StagingView.searchText") { //$NON-NLS-1$
			@Override
			protected Control createControl(Composite parent) {
				Composite toolbarComposite = toolkit.createComposite(parent,
						SWT.NONE);
				toolbarComposite.setBackground(null);
				GridLayout headLayout = new GridLayout();
				headLayout.numColumns = 2;
				headLayout.marginHeight = 0;
				headLayout.marginWidth = 0;
				headLayout.marginTop = 0;
				headLayout.marginBottom = 0;
				headLayout.marginLeft = 0;
				headLayout.marginRight = 0;
				toolbarComposite.setLayout(headLayout);

				filterText = new Text(toolbarComposite, SWT.SEARCH
						| SWT.ICON_CANCEL | SWT.ICON_SEARCH);
				filterText.setMessage(UIText.StagingView_Find);
				GridData data = new GridData(GridData.FILL_HORIZONTAL);
				data.widthHint = 150;
				filterText.setLayoutData(data);
				final Display display = Display.getCurrent();
				filterText.addModifyListener(new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						final StagingViewSearchThread searchThread = new StagingViewSearchThread(
								StagingView.this);
						display.timerExec(200, new Runnable() {
							public void run() {
								searchThread.start();
							}
						});
					}
				});
				return toolbarComposite;
			}
		};

		IActionBars actionBars = getViewSite().getActionBars();
		IToolBarManager toolbar = actionBars.getToolBarManager();

		toolbar.add(controlContribution);

		refreshAction = new Action(UIText.StagingView_Refresh, IAction.AS_PUSH_BUTTON) {
			public void run() {
				if(cacheEntry != null)
					cacheEntry.refreshResourcesAndIndexDiff();
			}
		};
		refreshAction.setImageDescriptor(UIIcons.ELCL16_REFRESH);
		toolbar.add(refreshAction);

		// link with selection
		Action linkSelectionAction = new BooleanPrefAction(
				(IPersistentPreferenceStore) getPreferenceStore(),
				UIPreferences.STAGING_VIEW_SYNC_SELECTION,
				UIText.StagingView_LinkSelection) {
			@Override
			public void apply(boolean value) {
				reactOnSelection = value;
			}
		};
		linkSelectionAction.setImageDescriptor(UIIcons.ELCL16_SYNCED);
		toolbar.add(linkSelectionAction);

		toolbar.add(new Separator());

		openNewCommitsAction = new Action(UIText.StagingView_OpenNewCommits,
				IAction.AS_CHECK_BOX) {

			public void run() {
				getPreferenceStore().setValue(
						UIPreferences.STAGING_VIEW_SHOW_NEW_COMMITS, isChecked());
			}
		};
		openNewCommitsAction.setChecked(getPreferenceStore().getBoolean(
				UIPreferences.STAGING_VIEW_SHOW_NEW_COMMITS));

		columnLayoutAction = new Action(UIText.StagingView_ColumnLayout,
				IAction.AS_CHECK_BOX) {

			public void run() {
				getPreferenceStore().setValue(
						UIPreferences.STAGING_VIEW_COLUMN_LAYOUT, isChecked());
				stagingSashForm.setOrientation(isChecked() ? SWT.HORIZONTAL
						: SWT.VERTICAL);
			}
		};
		columnLayoutAction.setChecked(getPreferenceStore().getBoolean(
				UIPreferences.STAGING_VIEW_COLUMN_LAYOUT));

		fileNameModeAction = new Action(UIText.StagingView_ShowFileNamesFirst,
				IAction.AS_CHECK_BOX) {

			public void run() {
				final boolean enable = isChecked();
				getLabelProvider(stagedViewer).setFileNameMode(enable);
				getLabelProvider(unstagedViewer).setFileNameMode(enable);
				refreshViewers();
				getPreferenceStore().setValue(
						UIPreferences.STAGING_VIEW_FILENAME_MODE, enable);
			}
		};
		fileNameModeAction.setChecked(getPreferenceStore().getBoolean(
				UIPreferences.STAGING_VIEW_FILENAME_MODE));

		IMenuManager dropdownMenu = actionBars.getMenuManager();
		MenuManager presentationMenu = new MenuManager(
				UIText.StagingView_Presentation);
		listPresentationAction = new Action(UIText.StagingView_List,
				IAction.AS_RADIO_BUTTON) {
			public void run() {
				presentation = Presentation.LIST;
				getPreferenceStore().setValue(
						UIPreferences.STAGING_VIEW_PRESENTATION,
						Presentation.LIST.name());
				treePresentationAction.setChecked(false);
				compactTreePresentationAction.setChecked(false);
				setExpandCollapseActionsVisible(false);
				refreshViewers();
			}
		};
		listPresentationAction.setImageDescriptor(UIIcons.FLAT);
		presentationMenu.add(listPresentationAction);

		treePresentationAction = new Action(UIText.StagingView_Tree,
				IAction.AS_RADIO_BUTTON) {
			public void run() {
				presentation = Presentation.TREE;
				getPreferenceStore().setValue(
						UIPreferences.STAGING_VIEW_PRESENTATION,
						Presentation.TREE.name());
				listPresentationAction.setChecked(false);
				compactTreePresentationAction.setChecked(false);
				setExpandCollapseActionsVisible(true);
				refreshViewers();
			}
		};
		treePresentationAction.setImageDescriptor(UIIcons.HIERARCHY);
		presentationMenu.add(treePresentationAction);

		compactTreePresentationAction = new Action(UIText.StagingView_CompactTree,
				IAction.AS_RADIO_BUTTON) {
			public void run() {
				presentation = Presentation.COMPACT_TREE;
				getPreferenceStore().setValue(
						UIPreferences.STAGING_VIEW_PRESENTATION,
						Presentation.COMPACT_TREE.name());
				listPresentationAction.setChecked(false);
				treePresentationAction.setChecked(false);
				setExpandCollapseActionsVisible(true);
				refreshViewers();
			}
		};
		compactTreePresentationAction.setImageDescriptor(UIIcons.COMPACT);
		presentationMenu.add(compactTreePresentationAction);

		String presentationString = getPreferenceStore().getString(
				UIPreferences.STAGING_VIEW_PRESENTATION);
		if (presentationString.length() > 0) {
			try {
				presentation = Presentation.valueOf(presentationString);
			} catch (IllegalArgumentException e) {
				// Use already set value of presentation
			}
		}
		switch (presentation) {
		case LIST:
			listPresentationAction.setChecked(true);
			setExpandCollapseActionsVisible(false);
			break;
		case TREE:
			treePresentationAction.setChecked(true);
			break;
		case COMPACT_TREE:
			compactTreePresentationAction.setChecked(true);
			break;
		default:
			break;
		}
		dropdownMenu.add(presentationMenu);
		dropdownMenu.add(new Separator());
		dropdownMenu.add(openNewCommitsAction);
		dropdownMenu.add(columnLayoutAction);
		dropdownMenu.add(fileNameModeAction);

		actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), new GlobalDeleteActionHandler());

		// For the normal resource undo/redo actions to be active, so that files
		// deleted via the "Delete" action in the staging view can be restored.
		IUndoContext workspaceContext = (IUndoContext) ResourcesPlugin.getWorkspace().getAdapter(IUndoContext.class);
		undoRedoActionGroup = new UndoRedoActionGroup(getViewSite(), workspaceContext, true);
		undoRedoActionGroup.fillActionBars(actionBars);

		actionBars.updateActionBars();
	}

	private void setExpandCollapseActionsVisible(boolean visible) {
		for (IContributionItem item : unstagedToolBarManager.getItems())
			item.setVisible(visible);
		for (IContributionItem item : stagedToolBarManager.getItems())
			item.setVisible(visible);
		unstagedExpandAllAction.setEnabled(visible);
		unstagedCollapseAllAction.setEnabled(visible);
		stagedExpandAllAction.setEnabled(visible);
		stagedCollapseAllAction.setEnabled(visible);
		unstagedToolBarManager.update(true);
		stagedToolBarManager.update(true);
	}

	private TreeViewer createTree(Composite composite) {
		Tree tree = toolkit.createTree(composite, SWT.FULL_SELECTION
				| SWT.MULTI);
		tree.setLinesVisible(true);
		TreeViewer treeViewer = new TreeViewer(tree);
		return treeViewer;
	}

	private IBaseLabelProvider createLabelProvider(TreeViewer treeViewer) {
		StagingViewLabelProvider baseProvider = new StagingViewLabelProvider(
				this);
		baseProvider.setFileNameMode(getPreferenceStore().getBoolean(
				UIPreferences.STAGING_VIEW_FILENAME_MODE));

		ProblemLabelDecorator decorator = new ProblemLabelDecorator(treeViewer);
		return new TreeDecoratingLabelProvider(baseProvider, decorator);
	}

	private IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	private StagingViewLabelProvider getLabelProvider(ContentViewer viewer) {
		IBaseLabelProvider base = viewer.getLabelProvider();
		ILabelProvider labelProvider = ((TreeDecoratingLabelProvider) base)
				.getLabelProvider();
		return (StagingViewLabelProvider) labelProvider;
	}

	private StagingViewContentProvider getContentProvider(ContentViewer viewer) {
		return (StagingViewContentProvider) viewer.getContentProvider();
	}

	private void updateSectionText() {
		stagedSection.setText(MessageFormat
				.format(UIText.StagingView_StagedChanges,
						getSectionCount(stagedViewer)));
		unstagedSection.setText(MessageFormat.format(
				UIText.StagingView_UnstagedChanges,
				getSectionCount(unstagedViewer)));
	}

	private String getSectionCount(TreeViewer viewer) {
		StagingViewContentProvider contentProvider = getContentProvider(viewer);
		int count = contentProvider.getCount();
		int shownCount = contentProvider.getShownCount();
		if (shownCount == count)
			return Integer.toString(count);
		else
			return shownCount + "/" + count; //$NON-NLS-1$
	}

	private void updateMessage() {
		String message = commitMessageComponent.getStatus().getMessage();
		boolean needsRedraw = false;
		if (message != null) {
			warningLabel.showMessage(message);
			needsRedraw = true;
		} else {
			needsRedraw = warningLabel.isVisible();
			warningLabel.hideMessage();
		}
		// Without this explicit redraw, the ControlDecoration of the
		// commit message area would not get updated and cause visual
		// corruption.
		if (needsRedraw)
			commitMessageSection.redraw();
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

		case MISSING:
		case MISSING_AND_CHANGED:
		case MODIFIED:
		case PARTIALLY_MODIFIED:
		case CONFLICTING:
		case UNTRACKED:
		default:
			// compare with index
			runCommand(ActionCommands.COMPARE_WITH_INDEX_ACTION, selection);
		}
	}

	private void createPopupMenu(final TreeViewer treeViewer) {
		final MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		Control control = treeViewer.getControl();
		control.setMenu(menuMgr.createContextMenu(control));
		menuMgr.addMenuListener(new IMenuListener() {

			public void menuAboutToShow(IMenuManager manager) {
				IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
				if (selection.isEmpty())
					return;

				List<StagingEntry> stagingEntryList = new ArrayList<StagingEntry>();

				boolean submoduleSelected = false;
				boolean folderSelected = false;
				for (Object element : selection.toArray()) {
					if (element instanceof StagingFolderEntry) {
						StagingFolderEntry folder = (StagingFolderEntry) element;
						folderSelected = true;
						StagingViewContentProvider contentProvider = getContentProvider(treeViewer);
						List<StagingEntry> stagingEntries = contentProvider
								.getStagingEntriesFiltered(folder);
						for (StagingEntry stagingEntry : stagingEntries) {
							if (!stagingEntryList.contains(stagingEntry))
								stagingEntryList.add(stagingEntry);
						}
					} else if (element instanceof StagingEntry) {
						StagingEntry entry = (StagingEntry) element;
						if (entry.isSubmodule())
							submoduleSelected = true;
						if (!stagingEntryList.contains(entry))
							stagingEntryList.add(entry);
					}
				}

				final IStructuredSelection fileSelection = new StructuredSelection(
						stagingEntryList);

				if (!folderSelected) {
					Action openWorkingTreeVersion = new Action(
							UIText.CommitFileDiffViewer_OpenWorkingTreeVersionInEditorMenuLabel) {
						@Override
						public void run() {
							openSelectionInEditor(fileSelection);
						}
					};
					openWorkingTreeVersion.setEnabled(!submoduleSelected);
					menuMgr.add(openWorkingTreeVersion);
				}

				Set<StagingEntry.Action> availableActions = getAvailableActions(fileSelection);

				boolean addReplaceWithFileInGitIndex = availableActions.contains(StagingEntry.Action.REPLACE_WITH_FILE_IN_GIT_INDEX);
				boolean addReplaceWithHeadRevision = availableActions.contains(StagingEntry.Action.REPLACE_WITH_HEAD_REVISION);
				boolean addStage = availableActions.contains(StagingEntry.Action.STAGE);
				boolean addUnstage = availableActions.contains(StagingEntry.Action.UNSTAGE);
				boolean addDelete = availableActions.contains(StagingEntry.Action.DELETE);
				boolean addIgnore = availableActions.contains(StagingEntry.Action.IGNORE);
				boolean addLaunchMergeTool = availableActions.contains(StagingEntry.Action.LAUNCH_MERGE_TOOL);

				if (addStage)
					menuMgr.add(new Action(UIText.StagingView_StageItemMenuLabel) {
						@Override
						public void run() {
							stage(fileSelection);
						}
					});
				if (addUnstage)
					menuMgr.add(new Action(UIText.StagingView_UnstageItemMenuLabel) {
						@Override
						public void run() {
							unstage(fileSelection);
						}
					});
				boolean selectionIncludesNonWorkspaceResources = selectionIncludesNonWorkspaceResources(fileSelection);
				if (addReplaceWithFileInGitIndex)
					if (selectionIncludesNonWorkspaceResources)
						menuMgr.add(new ReplaceAction(
								UIText.StagingView_replaceWithFileInGitIndex,
								fileSelection, false));
					else
						menuMgr.add(createItem(
								ActionCommands.DISCARD_CHANGES_ACTION,
								fileSelection)); // replace with index
				if (addReplaceWithHeadRevision)
					if (selectionIncludesNonWorkspaceResources)
						menuMgr.add(new ReplaceAction(
								UIText.StagingView_replaceWithHeadRevision,
								fileSelection, true));
					else
						menuMgr.add(createItem(
								ActionCommands.REPLACE_WITH_HEAD_ACTION,
								fileSelection));
				if (addIgnore)
					menuMgr.add(new IgnoreAction(fileSelection));
				if (addDelete)
					menuMgr.add(new DeleteAction(fileSelection));
				if (addLaunchMergeTool)
					menuMgr.add(createItem(ActionCommands.MERGE_TOOL_ACTION,
							fileSelection));
				menuMgr.add(new Separator());
				menuMgr.add(createShowInMenu());
			}
		});

	}

	/**
	 * @return selected presentation
	 */
	Presentation getPresentation() {
		return presentation;
	}

	/**
	 * @return the trimmed string which is the current filter, empty string for
	 *         no filter
	 */
	String getFilterString() {
		if (filterText != null)
			return filterText.getText().trim();
		else
			return ""; //$NON-NLS-1$
	}

	/**
	 * Refresh the unstaged and staged viewers
	 */
	public void refreshViewers() {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				Object[] unstagedExpanded = unstagedViewer
						.getExpandedElements();
				Object[] stagedExpanded = stagedViewer.getExpandedElements();
				unstagedViewer.refresh();
				stagedViewer.refresh();
				updateSectionText();
				unstagedViewer.setExpandedElements(unstagedExpanded);
				stagedViewer.setExpandedElements(stagedExpanded);
			}
		});
	}

	private IContributionItem createShowInMenu() {
		IWorkbenchWindow workbenchWindow = getSite().getWorkbenchWindow();
		return UIUtils.createShowInMenu(workbenchWindow);
	}

	private class ReplaceAction extends Action {

		IStructuredSelection selection;
		private final boolean headRevision;

		ReplaceAction(String text, IStructuredSelection selection, boolean headRevision) {
			super(text);
			this.selection = selection;
			this.headRevision = headRevision;
		}

		@Override
		public void run() {
			boolean performAction = MessageDialog.openConfirm(form.getShell(),
					UIText.DiscardChangesAction_confirmActionTitle,
					UIText.DiscardChangesAction_confirmActionMessage);
			if (!performAction)
				return ;
			String[] files = getSelectedFiles(selection);
			replaceWith(files, headRevision);
		}
	}

	private static class IgnoreAction extends Action {

		private final IStructuredSelection selection;

		IgnoreAction(IStructuredSelection selection) {
			super(UIText.StagingView_IgnoreItemMenuLabel);
			this.selection = selection;
		}

		@Override
		public void run() {
			IgnoreOperationUI operation = new IgnoreOperationUI(
					getSelectedPaths(selection));
			operation.run();
		}
	}

	private class DeleteAction extends Action {

		private final IStructuredSelection selection;

		DeleteAction(IStructuredSelection selection) {
			super(UIText.StagingView_DeleteItemMenuLabel);
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

	private void replaceWith(String[] files, boolean headRevision) {
		if (files == null || files.length == 0)
			return;
		CheckoutCommand checkoutCommand = new Git(currentRepository).checkout();
		if (headRevision)
			checkoutCommand.setStartPoint(Constants.HEAD);
		for (String path : files)
			checkoutCommand.addPath(path);
		try {
			checkoutCommand.call();
		} catch (Exception e) {
			Activator.handleError(UIText.StagingView_checkoutFailed, e, true);
		}
	}

	private String[] getSelectedFiles(IStructuredSelection selection) {
		List<String> result = new ArrayList<String>();
		Iterator iterator = selection.iterator();
		while (iterator.hasNext()) {
			Object selectedItem = iterator.next();
			if (selectedItem instanceof StagingEntry) {
				StagingEntry stagingEntry = (StagingEntry) selectedItem;
				result.add(stagingEntry.getPath());
			}
		}
		return result.toArray(new String[result.size()]);
	}

	private static List<IPath> getSelectedPaths(IStructuredSelection selection) {
		List<IPath> paths = new ArrayList<IPath>();
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
			if (file == null)
				return true;
		}
		return false;
	}

	private void openSelectionInEditor(ISelection s) {
		if (s.isEmpty() || !(s instanceof IStructuredSelection))
			return;
		final IStructuredSelection iss = (IStructuredSelection) s;
		for (Object element : iss.toList()) {
			if (element instanceof StagingEntry) {
				StagingEntry entry = (StagingEntry) element;
				String relativePath = entry.getPath();
				String path = new Path(currentRepository.getWorkTree()
						.getAbsolutePath()).append(relativePath)
						.toOSString();
				openFileInEditor(path);
			}
		}
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

	private CommandContributionItem createItem(String itemAction,
			final ISelection selection) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		CommandContributionItemParameter itemParam = new CommandContributionItemParameter(
				workbench, null, itemAction, STYLE_PUSH);

		IWorkbenchWindow activeWorkbenchWindow = workbench
				.getActiveWorkbenchWindow();
		IHandlerService hsr = (IHandlerService) activeWorkbenchWindow
				.getService(IHandlerService.class);
		IEvaluationContext ctx = hsr.getCurrentState();
		ctx.addVariable(ACTIVE_MENU_SELECTION_NAME, selection);

		return new CommandContributionItem(itemParam);
	}

	private void reactOnSelection(ISelection selection) {
		if (selection instanceof StructuredSelection) {
			StructuredSelection ssel = (StructuredSelection) selection;
			if (ssel.size() != 1)
				return;
			Object firstElement = ssel.getFirstElement();
			if (firstElement instanceof IResource)
				showResource((IResource) firstElement);
			else if (firstElement instanceof RepositoryTreeNode) {
				RepositoryTreeNode repoNode = (RepositoryTreeNode) firstElement;
				reload(repoNode.getRepository());
			} else if (firstElement instanceof IAdaptable) {
				IResource adapted = (IResource) ((IAdaptable) firstElement).getAdapter(IResource.class);
				if (adapted != null)
					showResource(adapted);
			}
		}
	}

	private void showResource(final IResource resource) {
		IProject project = resource.getProject();
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		if (mapping == null)
			return;
		if (mapping.getRepository() != currentRepository)
			reload(mapping.getRepository());
	}

	private void stage(IStructuredSelection selection) {
		StagingViewContentProvider contentProvider = getContentProvider(unstagedViewer);
		Git git = new Git(currentRepository);
		Iterator iterator = selection.iterator();
		List<String> addPaths = new ArrayList<String>();
		List<String> rmPaths = new ArrayList<String>();
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
				IResource resource = AdapterUtils.adapt(element, IResource.class);
				if (resource != null) {
					RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
					if (mapping != null && mapping.getRepository() == currentRepository) {
						String path = mapping.getRepoRelativePath(resource);
						// If resource corresponds to root of working directory
						if ("".equals(path)) //$NON-NLS-1$
							addPaths.add("."); //$NON-NLS-1$
						else
							addPaths.add(path);
					}
				}
			}
		}

		if (!addPaths.isEmpty())
			try {
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
		if (!rmPaths.isEmpty())
			try {
				RmCommand rm = git.rm().setCached(true);
				for (String rmPath : rmPaths)
					rm.addFilepattern(rmPath);
				rm.call();
			} catch (NoFilepatternException e) {
				// cannot happen
			} catch (JGitInternalException e) {
				Activator.handleError(e.getCause().getMessage(), e.getCause(),
						true);
			} catch (Exception e) {
				Activator.handleError(e.getMessage(), e, true);
			}
	}

	private void selectEntryForStaging(StagingEntry entry,
			List<String> addPaths, List<String> rmPaths) {
		switch (entry.getState()) {
		case ADDED:
		case CHANGED:
		case REMOVED:
			// already staged
			break;
		case CONFLICTING:
		case MODIFIED:
		case PARTIALLY_MODIFIED:
		case UNTRACKED:
			addPaths.add(entry.getPath());
			break;
		case MISSING:
		case MISSING_AND_CHANGED:
			rmPaths.add(entry.getPath());
			break;
		}
	}

	private void unstage(IStructuredSelection selection) {
		if (selection.isEmpty())
			return;

		RevCommit headRev = null;
		try {
			final Ref head = currentRepository.getRef(Constants.HEAD);
			// head.getObjectId() is null if the repository does not contain any
			// commit
			if (head.getObjectId() != null)
				headRev = new RevWalk(currentRepository).parseCommit(head
						.getObjectId());
		} catch (IOException e1) {
			// TODO fix text
			MessageDialog.openError(getSite().getShell(),
					UIText.CommitAction_MergeHeadErrorTitle,
					UIText.CommitAction_ErrorReadingMergeMsg);
			return;
		}

		final DirCache dirCache;
		final DirCacheEditor edit;
		try {
			dirCache = currentRepository.lockDirCache();
			edit = dirCache.editor();
		} catch (IOException e) {
			// TODO fix text
			MessageDialog.openError(getSite().getShell(),
					UIText.CommitAction_MergeHeadErrorTitle,
					UIText.CommitAction_ErrorReadingMergeMsg);
			return;
		}

		try {
			processUnstageSelection(selection, headRev, edit);

			try {
				edit.commit();
			} catch (IOException e) {
				// TODO fix text
				MessageDialog.openError(getSite().getShell(),
						UIText.CommitAction_MergeHeadErrorTitle,
						UIText.CommitAction_ErrorReadingMergeMsg);
			}
		} finally {
			dirCache.unlock();
		}
	}

	private void processUnstageSelection(IStructuredSelection selection,
			final RevCommit headRev, final DirCacheEditor edit) {
		resetPathsToExpand();
		for (Object element : selection.toList()) {
			if (element instanceof StagingEntry) {
				StagingEntry entry = (StagingEntry) element;
				updateDirCache(headRev, edit, entry);
				addPathAndParentPaths(entry.getParentPath(), pathsToExpandInUnstaged);
			} else if (element instanceof StagingFolderEntry) {
				StagingFolderEntry folder = (StagingFolderEntry) element;
				List<StagingEntry> entries = getContentProvider(stagedViewer)
						.getStagingEntriesFiltered(folder);
				for (StagingEntry entry : entries)
					updateDirCache(headRev, edit, entry);
				addExpandedPathsBelowFolder(folder, stagedViewer,
						pathsToExpandInUnstaged);
			}
		}
	}

	private void updateDirCache(final RevCommit headRev,
			final DirCacheEditor edit, final StagingEntry entry) {
		switch (entry.getState()) {
		case ADDED:
			edit.add(new DirCacheEditor.DeletePath(entry.getPath()));
			break;
		case CHANGED:
		case REMOVED:
			// set the index object id/file mode back to our head revision
			try {
				final TreeWalk tw = TreeWalk.forPath(currentRepository,
						entry.getPath(), headRev.getTree());
				if (tw != null)
					edit.add(new DirCacheEditor.PathEdit(entry.getPath()) {
						@Override
						public void apply(DirCacheEntry ent) {
							ent.setFileMode(tw.getFileMode(0));
							ent.setObjectId(tw.getObjectId(0));
							// for index & working tree compare
							ent.setLastModified(0);
						}
					});
			} catch (IOException e) {
				// TODO fix text
				MessageDialog.openError(getSite().getShell(),
						UIText.CommitAction_MergeHeadErrorTitle,
						UIText.CommitAction_ErrorReadingMergeMsg);
			}
			break;
		default:
			// unstaged
		}
	}

	private void resetPathsToExpand() {
		pathsToExpandInStaged = new HashSet<IPath>();
		pathsToExpandInUnstaged = new HashSet<IPath>();
	}

	private static void addExpandedPathsBelowFolder(StagingFolderEntry folder,
			TreeViewer treeViewer, Set<IPath> addToSet) {
		Object[] expandedElements = treeViewer.getExpandedElements();
		for (Object expandedElement : expandedElements) {
			if (expandedElement instanceof StagingFolderEntry) {
				StagingFolderEntry expandedFolder = (StagingFolderEntry) expandedElement;
				if (folder.getPath().isPrefixOf(
						expandedFolder.getPath()))
					addPathAndParentPaths(expandedFolder.getPath(), addToSet);
			}
		}
	}

	private static void addPathAndParentPaths(IPath initialPath, Set<IPath> addToSet) {
		for (IPath p = initialPath; p.segmentCount() >= 1; p = p
				.removeLastSegments(1))
			addToSet.add(p);
	}

	private boolean isValidRepo(final Repository repository) {
		return repository != null
				&& !repository.isBare()
				&& repository.getWorkTree().exists()
				&& org.eclipse.egit.core.Activator.getDefault()
						.getRepositoryUtil().contains(repository);
	}

	/**
	 * Clear the view's state.
	 * <p>
	 * This method must be called from the UI-thread
	 */
	private void clearRepository() {
		saveCommitMessageComponentState();
		currentRepository = null;
		StagingViewUpdate update = new StagingViewUpdate(null, null, null);
		unstagedViewer.setInput(update);
		stagedViewer.setInput(update);
		enableCommitWidgets(false);
		updateSectionText();
		form.setText(UIText.StagingView_NoSelectionTitle);
	}

	private void reload(final Repository repository) {
		if (form.isDisposed())
			return;
		if (repository == null) {
			asyncExec(new Runnable() {
				public void run() {
					clearRepository();
				}
			});
			return;
		}

		if (!isValidRepo(repository))
			return;

		final boolean repositoryChanged = currentRepository != repository;

		asyncExec(new Runnable() {
			public void run() {
				if (form.isDisposed())
					return;

				final IndexDiffData indexDiff = doReload(repository);

				boolean indexDiffAvailable = indexDiff !=  null;

				if (repositoryChanged)
					// Reset paths, they're from the old repository
					resetPathsToExpand();

				final StagingViewUpdate update = new StagingViewUpdate(currentRepository, indexDiff, null);
				Object[] unstagedExpanded = unstagedViewer
						.getExpandedElements();
				Object[] stagedExpanded = stagedViewer
						.getExpandedElements();
				unstagedViewer.setInput(update);
				stagedViewer.setInput(update);
				expandPreviousExpandedAndPaths(unstagedExpanded, unstagedViewer,
						pathsToExpandInUnstaged);
				expandPreviousExpandedAndPaths(stagedExpanded, stagedViewer,
						pathsToExpandInStaged);
				enableCommitWidgets(indexDiffAvailable);
				boolean commitEnabled =
						indexDiffAvailable && repository.getRepositoryState().canCommit();
				commitButton.setEnabled(commitEnabled);
				commitAndPushButton.setEnabled(commitEnabled);
				form.setText(StagingView.getRepositoryName(repository));
				updateCommitMessageComponent(repositoryChanged, indexDiffAvailable);
				updateSectionText();
			}
		});
	}

	private IndexDiffData doReload(final Repository repository) {
		currentRepository = repository;

		IndexDiffCacheEntry entry = org.eclipse.egit.core.Activator.getDefault().getIndexDiffCache().getIndexDiffCacheEntry(currentRepository);

		if(cacheEntry != null && cacheEntry != entry)
			cacheEntry.removeIndexDiffChangedListener(myIndexDiffListener);

		cacheEntry = entry;
		cacheEntry.addIndexDiffChangedListener(myIndexDiffListener);

		return cacheEntry.getIndexDiff();
	}

	private void expandPreviousExpandedAndPaths(Object[] previous,
			TreeViewer viewer, Set<IPath> additionalPaths) {
		Set<IPath> paths = new HashSet<IPath>(additionalPaths);
		// Instead of just expanding the previous elements directly, also expand
		// all parent paths. This makes it work in case of "re-folding" of
		// compact tree.
		for (Object element : previous)
			if (element instanceof StagingFolderEntry)
				addPathAndParentPaths(((StagingFolderEntry) element).getPath(), paths);
		List<Object> expand = new ArrayList<Object>();
		StagingViewContentProvider stagedContentProvider = getContentProvider(viewer);
		for (StagingFolderEntry folder : stagedContentProvider
				.getStagingFolderEntries()) {
			if (paths.contains(folder.getPath()))
				expand.add(folder);
		}
		viewer.setExpandedElements(expand.toArray());
	}

	private void clearCommitMessageToggles() {
		amendPreviousCommitAction.setChecked(false);
		addChangeIdAction.setChecked(false);
		signedOffByAction.setChecked(false);
	}

	void updateCommitMessageComponent(boolean repositoryChanged, boolean indexDiffAvailable) {
		CommitHelper helper = new CommitHelper(currentRepository);
		CommitMessageComponentState oldState = null;
		if (repositoryChanged) {
			if (userEnteredCommmitMessage())
				saveCommitMessageComponentState();
			else
				deleteCommitMessageComponentState();
			oldState = loadCommitMessageComponentState();
			commitMessageComponent.setRepository(currentRepository);
			if (oldState == null)
				loadInitialState(helper);
			else
				loadExistingState(helper, oldState);
		} else // repository did not change
			if (userEnteredCommmitMessage()) {
				if (!commitMessageComponent.getHeadCommit().equals(
						helper.getPreviousCommit()))
					addHeadChangedWarning(commitMessageComponent
							.getCommitMessage());
			} else
				loadInitialState(helper);
		amendPreviousCommitAction.setChecked(commitMessageComponent
				.isAmending());
		amendPreviousCommitAction.setEnabled(indexDiffAvailable && helper.amendAllowed());
		updateMessage();
	}

	private void loadExistingState(CommitHelper helper,
			CommitMessageComponentState oldState) {
		boolean headCommitChanged = !oldState.getHeadCommit().equals(
				getCommitId(helper.getPreviousCommit()));
		commitMessageComponent.enableListeners(false);
		commitMessageComponent.setAuthor(oldState.getAuthor());
		if (headCommitChanged)
			addHeadChangedWarning(oldState.getCommitMessage());
		else
			commitMessageComponent
					.setCommitMessage(oldState.getCommitMessage());
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
		commitMessageComponent.updateUIFromState();
		commitMessageComponent.updateSignedOffAndChangeIdButton();
		commitMessageComponent.enableListeners(true);
	}

	private void addHeadChangedWarning(String commitMessage) {
		String message = UIText.StagingView_headCommitChanged + Text.DELIMITER
				+ Text.DELIMITER + commitMessage;
		commitMessageComponent.setCommitMessage(message);
	}

	private void loadInitialState(CommitHelper helper) {
		commitMessageComponent.enableListeners(false);
		commitMessageComponent.resetState();
		commitMessageComponent.setAuthor(helper.getAuthor());
		commitMessageComponent.setCommitMessage(helper.getCommitMessage());
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

	private boolean userEnteredCommmitMessage() {
		if (commitMessageComponent.getRepository() == null)
			return false;
		String message = commitMessageComponent.getCommitMessage().replace(
				UIText.StagingView_headCommitChanged, ""); //$NON-NLS-1$
		if (message == null || message.trim().length() == 0)
			return false;

		String chIdLine = "Change-Id: I" + ObjectId.zeroId().name(); //$NON-NLS-1$

		if (currentRepository.getConfig().getBoolean(
				ConfigConstants.CONFIG_GERRIT_SECTION,
				ConfigConstants.CONFIG_KEY_CREATECHANGEID, false)
				&& commitMessageComponent.getCreateChangeId()) {
			if (message.trim().equals(chIdLine))
				return false;

			// change id was added automatically, but ther is more in the
			// message; strip the id, and check for the signed-off-by tag
			message = message.replace(chIdLine, ""); //$NON-NLS-1$
		}

		if (org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.COMMIT_DIALOG_SIGNED_OFF_BY)
				&& commitMessageComponent.isSignedOff()
				&& message.trim().equals(
						Constants.SIGNED_OFF_BY_TAG
								+ commitMessageComponent.getCommitter()))
			return false;

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

	private static String getRepositoryName(Repository repository) {
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		RepositoryState state = repository.getRepositoryState();
		if (state != RepositoryState.SAFE)
			return repoName + '|' + state.getDescription();
		else
			return repoName;
	}

	private Collection<String> getStagedFileNames() {
		StagingViewContentProvider stagedContentProvider = getContentProvider(stagedViewer);
		StagingEntry[] entries = stagedContentProvider.getStagingEntries();
		List<String> files = new ArrayList<String>();
		for (StagingEntry entry : entries)
			files.add(entry.getPath());
		return files;
	}

	private void commit(boolean pushUpstream) {
		if (!isCommitWithoutFilesAllowed()) {
			MessageDialog.openError(getSite().getShell(),
					UIText.StagingView_committingNotPossible,
					UIText.StagingView_noStagedFiles);
			return;
		}
		if (!commitMessageComponent.checkCommitInfo())
			return;

		if (!UIUtils.saveAllEditors(currentRepository))
			return;

		String commitMessage = commitMessageComponent.getCommitMessage();
		CommitOperation commitOperation = null;
		try {
			commitOperation = new CommitOperation(currentRepository,
					commitMessageComponent.getAuthor(),
					commitMessageComponent.getCommitter(),
					commitMessage);
		} catch (CoreException e) {
			Activator.handleError(UIText.StagingView_commitFailed, e, true);
			return;
		}
		if (amendPreviousCommitAction.isChecked())
			commitOperation.setAmending(true);
		commitOperation.setComputeChangeId(addChangeIdAction.isChecked());
		Job commitJob = new CommitJob(currentRepository, commitOperation)
			.setOpenCommitEditor(openNewCommitsAction.isChecked())
			.setPushUpstream(pushUpstream);
		commitJob.schedule();
		CommitMessageHistory.saveCommitHistory(commitMessage);
		clearCommitMessageToggles();
		commitMessageText.setText(EMPTY_STRING);
	}

	private boolean isCommitWithoutFilesAllowed() {
		if (stagedViewer.getTree().getItemCount() > 0)
			return true;

		if (amendPreviousCommitAction.isChecked())
			return true;

		return CommitHelper.isCommitWithoutFilesAllowed(currentRepository);
	}

	@Override
	public void setFocus() {
		unstagedViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();

		ISelectionService srv = (ISelectionService) getSite().getService(
				ISelectionService.class);
		srv.removePostSelectionListener(selectionChangedListener);

		if(cacheEntry != null)
			cacheEntry.removeIndexDiffChangedListener(myIndexDiffListener);

		if (undoRedoActionGroup != null)
			undoRedoActionGroup.dispose();

		InstanceScope.INSTANCE.getNode(
				org.eclipse.egit.core.Activator.getPluginId())
				.removePreferenceChangeListener(prefListener);
	}

	private void asyncExec(Runnable runnable) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
	}

}
