/*******************************************************************************
 * Copyright (c) 2010, 2021 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza <dariusz@luksza.org> - add synchronization feature
 *    Daniel Megert <daniel_megert@ch.ibm.com> - Only check out on double-click
 *    Daniel Megert <daniel_megert@ch.ibm.com> - Don't reveal selection on refresh
 *    Robin Stocker <robin@nibor.org> - Show In support
 *    Daniel Megert <daniel_megert@ch.ibm.com> - Show Git Staging view in Show In menu
 *    Alexander Nittka <alex@nittka.de> - Bug 545123
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.IStateListener;
import org.eclipse.core.commands.State;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.EgitUiEditorUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.actions.ActionCommands;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.egit.ui.internal.commands.ToggleCommand;
import org.eclipse.egit.ui.internal.components.MessagePopupTextCellEditor;
import org.eclipse.egit.ui.internal.groups.RepositoryGroup;
import org.eclipse.egit.ui.internal.groups.RepositoryGroups;
import org.eclipse.egit.ui.internal.history.HistoryPageInput;
import org.eclipse.egit.ui.internal.properties.GitPropertySheetPage;
import org.eclipse.egit.ui.internal.reflog.ReflogView;
import org.eclipse.egit.ui.internal.repository.tree.FetchNode;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.egit.ui.internal.repository.tree.FilterableNode;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.PushNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroupNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.repository.tree.StashedCommitNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.egit.ui.internal.selection.RepositoryVirtualNode;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.events.ConfigChangedListener;
import org.eclipse.jgit.events.IndexChangedListener;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.handlers.RegistryToggleState;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.WorkbenchJob;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * The "Git Repositories View".
 */
public class RepositoriesView extends CommonNavigator implements IShowInSource, IShowInTargetList {

	/** View id; also doubles as context id. */
	public static final String VIEW_ID = "org.eclipse.egit.ui.RepositoriesView"; //$NON-NLS-1$

	/** Sub-context active when a single repository is selected. */
	private static final String SINGLE_REPO_CONTEXT_ID = VIEW_ID
			+ ".SingleRepository"; //$NON-NLS-1$

	/** The command id for toggling "link with selection". */
	public static final String LINK_WITH_SELECTION_ID = "org.eclipse.egit.ui.RepositoriesLinkWithSelection"; //$NON-NLS-1$

	/**
	 * Delay between refreshes in milliseconds. Used to avoid overwhelming the
	 * viewer with refreshes when many change events arrive from repositories.
	 */
	private static final long DEFAULT_REFRESH_DELAY = 300L;

	private final Set<Repository> repositories = new HashSet<>();

	private final RefsChangedListener myRefsChangedListener = event -> scheduleRefresh();

	private final IndexChangedListener myIndexChangedListener = event -> scheduleRefresh();

	private final ConfigChangedListener myConfigChangeListener = event -> scheduleRefresh();

	private final List<ListenerHandle> myListeners = new LinkedList<>();

	private RefreshUiJob refreshUiJob;

	private RefCache.Cache refCache = RefCache.get();

	private Composite emptyArea;

	private StackLayout layout;

	private State reactOnSelection;

	private IWorkbenchPart lastSelectionPart;

	private File lastSelectedRepository;

	private boolean filterCacheLoaded;

	private final ISelectionListener selectionChangedListener = (part,
			selection) -> {
		if (part == RepositoriesView.this) {
			if (!selection.isEmpty()
					&& selection instanceof IStructuredSelection) {
				Repository repo = SelectionUtils
						.getRepository((IStructuredSelection) selection);
				if (repo != null) {
					lastSelectedRepository = repo.getDirectory();
				} else {
					lastSelectedRepository = null;
				}
			}
			return;
		}
		IWorkbenchPart currentPart = determinePart(part);
		if (!((Boolean) reactOnSelection.getValue()).booleanValue()) {
			lastSelectionPart = currentPart;
		} else {
			lastSelectionPart = null;
			reactOnSelection(convertSelection(currentPart, selection));
		}
	};

	private final IStateListener reactOnSelectionListener = (state,
			oldValue) -> {
		if (((Boolean) state.getValue()).booleanValue()
				&& lastSelectionPart != null) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
				if (lastSelectionPart == null) {
					return;
				}
				IWorkbenchPartSite site = lastSelectionPart.getSite();
				if (site == null) {
					return;
				}
				ISelectionProvider provider = site.getSelectionProvider();
				if (provider == null) {
					return;
				}
				reactOnSelection(convertSelection(lastSelectionPart,
						provider.getSelection()));
			});
		}
	};

	private State branchHierarchy;

	private final IStateListener stateChangeListener = (state,
			oldValue) -> refresh();

	private final IPreferenceChangeListener configurationListener = event -> {
		if (RepositoryUtil.PREFS_DIRECTORIES_REL.equals(event.getKey())) {
			refresh();
		}
	};

	private IContextActivation renameContext;

	private IContextService ctxSrv;

	private TextCellEditor textCellEditor;

	private Text filterField;

	/**
	 * The default constructor
	 */
	public RepositoriesView() {
		refreshUiJob = new RefreshUiJob();
	}

	/**
	 * Create area shown when no repositories are present
	 *
	 * @param parent
	 */
	protected void createEmptyArea(Composite parent) {
		emptyArea = new Composite(parent, SWT.NONE);
		emptyArea.setBackgroundMode(SWT.INHERIT_FORCE);
		MenuManager manager = new MenuManager();
		manager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager m) {
				getNavigatorActionService().fillContextMenu(m);
			}
		});
		getSite().registerContextMenu(manager, getCommonViewer());
		Menu menu = manager.createContextMenu(emptyArea);
		emptyArea.setMenu(menu);
		GridLayoutFactory.fillDefaults().applyTo(emptyArea);
		Composite infoArea = new Composite(emptyArea, SWT.NONE);
		infoArea.setMenu(menu);
		GridDataFactory.swtDefaults().align(SWT.LEFT, SWT.TOP)
				.grab(true, true).applyTo(infoArea);
		GridLayoutFactory.swtDefaults().applyTo(infoArea);
		Label messageLabel = new Label(infoArea, SWT.WRAP);
		messageLabel.setText(UIText.RepositoriesView_messageEmpty);
		messageLabel.setMenu(menu);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL)
				.grab(true, false).applyTo(messageLabel);
		Composite optionsArea = new Composite(infoArea, SWT.NONE);
		optionsArea.setMenu(menu);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(optionsArea);
		GridDataFactory.swtDefaults().indent(5, 0).grab(true, true)
				.applyTo(optionsArea);

		final FormToolkit toolkit = new FormToolkit(emptyArea.getDisplay());
		emptyArea.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				toolkit.dispose();
			}
		});
		final Color linkColor = JFaceColors.getHyperlinkText(emptyArea
				.getDisplay());

		Label addLabel = new Label(optionsArea, SWT.NONE);
		Image addImage = UIIcons.NEW_REPOSITORY.createImage();
		UIUtils.hookDisposal(addLabel, addImage);
		addLabel.setImage(addImage);
		Hyperlink addLink = toolkit.createHyperlink(optionsArea,
				UIText.RepositoriesView_linkAdd, SWT.WRAP);
		addLink.setForeground(linkColor);
		addLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				IHandlerService service = getViewSite()
						.getService(IHandlerService.class);
				UIUtils.executeCommand(service,
						"org.eclipse.egit.ui.RepositoriesViewAddRepository"); //$NON-NLS-1$
			}
		});
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL)
				.grab(true, false).applyTo(addLink);

		Label cloneLabel = new Label(optionsArea, SWT.NONE);
		Image cloneImage = UIIcons.CLONEGIT.createImage();
		UIUtils.hookDisposal(cloneLabel, cloneImage);
		cloneLabel.setImage(cloneImage);
		Hyperlink cloneLink = toolkit.createHyperlink(optionsArea,
				UIText.RepositoriesView_linkClone, SWT.WRAP);
		cloneLink.setForeground(linkColor);
		cloneLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				IHandlerService service = getViewSite()
						.getService(IHandlerService.class);
				UIUtils.executeCommand(service,
						"org.eclipse.egit.ui.RepositoriesViewClone"); //$NON-NLS-1$
			}
		});
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL)
				.grab(true, false).applyTo(cloneLink);

		Label createLabel = new Label(optionsArea, SWT.NONE);
		Image createImage = UIIcons.CREATE_REPOSITORY.createImage();
		UIUtils.hookDisposal(createLabel, createImage);
		createLabel.setImage(createImage);
		Hyperlink createLink = toolkit.createHyperlink(optionsArea,
				UIText.RepositoriesView_linkCreate, SWT.WRAP);
		createLink.setForeground(linkColor);
		createLink.setText(UIText.RepositoriesView_linkCreate);
		createLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				IHandlerService service = getViewSite()
						.getService(IHandlerService.class);
				UIUtils.executeCommand(service,
						"org.eclipse.egit.ui.RepositoriesViewCreateRepository"); //$NON-NLS-1$
			}
		});
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL)
				.grab(true, false).applyTo(createLink);
	}

	@Override
	public void createPartControl(Composite aParent) {
		FilterCache.INSTANCE.load();
		filterCacheLoaded = true;
		Composite displayArea = new Composite(aParent, SWT.NONE);
		layout = new StackLayout();
		displayArea.setLayout(layout);
		createEmptyArea(displayArea);

		super.createPartControl(displayArea);

		IWorkbenchWindow w = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		ICommandService csrv = w.getService(ICommandService.class);
		reactOnSelection = csrv.getCommand(LINK_WITH_SELECTION_ID)
				.getState(RegistryToggleState.STATE_ID);
		reactOnSelection.addListener(reactOnSelectionListener);
		branchHierarchy = csrv.getCommand(ToggleCommand.BRANCH_HIERARCHY_ID)
				.getState(RegistryToggleState.STATE_ID);
		branchHierarchy.addListener(stateChangeListener);
		IWorkbenchSiteProgressService service = getSite()
				.getService(IWorkbenchSiteProgressService.class);
		if (service != null) {
			service.showBusyForFamily(JobFamilies.REPO_VIEW_REFRESH);
			service.showBusyForFamily(JobFamilies.CLONE);
		}
	}

	@Override
	protected CommonViewer createCommonViewerObject(Composite aParent) {
		return new RepositoriesCommonViewer(getViewSite().getId(), aParent,
				SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
	}

	private void setTopControl(CommonViewer viewer) {
		if (!repositories.isEmpty()
				|| RepositoryGroups.INSTANCE.hasGroups()) {
			layout.topControl = viewer.getControl();
		} else {
			layout.topControl = emptyArea;
		}
	}

	// After a refresh of the CommonViewer decide what to display
	private void afterRefresh(CommonViewer viewer) {
		Control currentTop = layout.topControl;
		setTopControl(viewer);
		if (currentTop != layout.topControl) {
			emptyArea.getParent().layout(true, true);
		}
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		// integrate with Properties view
		if (adapter == IPropertySheetPage.class) {
			PropertySheetPage page = new GitPropertySheetPage();
			page.setPropertySourceProvider(
					new RepositoryPropertySourceProvider(page));
			return adapter.cast(page);
		}
		return super.getAdapter(adapter);
	}

	@Override
	protected CommonViewer createCommonViewer(Composite aParent) {
		CommonViewer viewer = super.createCommonViewer(aParent);
		// handle the double-click event for tags and branches
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				TreeSelection sel = (TreeSelection) event.getSelection();
				RepositoryTreeNode element = (RepositoryTreeNode) sel
						.getFirstElement();
				// after deletion the selection can be empty
				if (element == null) {
					return;
				}
				if (element instanceof RepositoryGroupNode) {
					return;
				}
				// Disable checkout for bare repositories
				if (element.getRepository().isBare()) {
					return;
				}
				if (element instanceof RefNode) {
					executeOpenCommandWithConfirmation(element,
							((RefNode) element).getObject().getName());
				} else if (element instanceof TagNode) {
					executeOpenCommandWithConfirmation(element,
							((TagNode) element).getObject().getName());
				} else if (element instanceof FetchNode) {
					executeFetchCommand(((FetchNode) element));
				}
			}
		});
		// handle open event for the working directory
		viewer.addOpenListener(new IOpenListener() {
			@Override
			public void open(OpenEvent event) {
				TreeSelection sel = (TreeSelection) event.getSelection();
				RepositoryTreeNode element = (RepositoryTreeNode) sel
						.getFirstElement();
				if (element instanceof FileNode
						|| element instanceof StashedCommitNode)
					executeOpenCommand(element);
			}
		});
		ctxSrv = getSite().getService(IContextService.class);
		viewer.addSelectionChangedListener(event -> {
			handleSingleRepositoryContext(event.getSelection(), viewer);
		});
		viewer.getTree().addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				handleSingleRepositoryContext(null, viewer);
			}

			@Override
			public void focusGained(FocusEvent e) {
				handleSingleRepositoryContext(viewer.getSelection(), viewer);
			}
		});
		setupInlineEditing(viewer);
		// react on selection changes
		ISelectionService srv = getSite().getService(ISelectionService.class);
		srv.addPostSelectionListener(selectionChangedListener);
		// react on changes in the configured repositories
		RepositoryUtil.INSTANCE.getPreferences().addPreferenceChangeListener(
				configurationListener);
		initRepositoriesAndListeners();
		ctxSrv.activateContext(VIEW_ID);
		// link with editor
		viewer.addPostSelectionChangedListener(event -> {
			if (!((Boolean) reactOnSelection.getValue()).booleanValue()) {
				return;
			}
			ISelection selection = event.getSelection();
			if (selection.isEmpty()
					|| !(selection instanceof IStructuredSelection)) {
				return;
			}
			IStructuredSelection sel = (IStructuredSelection) selection;
			if (sel.size() > 1) {
				return;
			}
			Object selected = sel.getFirstElement();
			if (selected instanceof FileNode) {
				showEditor((FileNode) selected);
			}
		});

		emptyArea.setBackground(viewer.getControl().getBackground());
		setTopControl(viewer);
		return viewer;
	}

	private void handleSingleRepositoryContext(ISelection selection,
			CommonViewer viewer) {
		boolean activate = false;
		if (selection != null && !selection.isEmpty()
				&& (selection instanceof StructuredSelection)) {
			StructuredSelection sel = (StructuredSelection) selection;
			Object item = sel.getFirstElement();
			activate = sel.size() == 1 && (item instanceof RepositoryNode);
		}
		if (!activate) {
			if (renameContext != null) {
				ctxSrv.deactivateContext(renameContext);
				renameContext = null;
			}
		} else if (viewer.getTree().isFocusControl() && renameContext == null) {
			renameContext = ctxSrv.activateContext(SINGLE_REPO_CONTEXT_ID);
		}
	}

	private void setupInlineEditing(CommonViewer viewer) {
		ColumnViewerEditorActivationStrategy editorActivation = new ColumnViewerEditorActivationStrategy(
				viewer) {

			@Override
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
				// Editing is started only through the
				// RenameRepositoryGroupCommand
				return event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};

		// TABBING_HORIZONTAL with only one column effectively switches off
		// tabbing. It may jump otherwise into an open editor (closing the cell
		// editor), and when the user is typing he may then inadvertently modify
		// that file.
		TreeViewerEditor.create(viewer, editorActivation,
				ColumnViewerEditor.TABBING_HORIZONTAL);

		// Record the initial value so that the validator can avoid producing an
		// error if the text is the same again during editing.
		String initialValue[] = { null };

		textCellEditor = new MessagePopupTextCellEditor(viewer.getTree(), true) {

			@Override
			protected boolean withBorder() {
				return true;
			}
		};
		textCellEditor.setValidator(value -> {
			String currentText = value.toString().trim();
			if (currentText.isEmpty()) {
				return UIText.RepositoriesView_RepoGroup_EmptyNameError;
			}
			if (!currentText.equals(initialValue[0])
					&& RepositoryGroups.INSTANCE.groupExists(currentText)) {
				return MessageFormat.format(
						UIText.RepositoryGroups_DuplicateGroupNameError,
						currentText);
			}
			return null;
		});

		// We don't have a ViewerColumn at hand... use the legacy mechanism:

		viewer.setColumnProperties(new String[] { "Name" }); //$NON-NLS-1$
		viewer.setCellEditors(new CellEditor[] { textCellEditor });
		viewer.setCellModifier(new ICellModifier() {

			@Override
			public boolean canModify(Object element, String property) {
				return element instanceof RepositoryGroupNode;
			}

			private String doGetValue(Object element) {
				if (element instanceof RepositoryGroupNode) {
					return ((RepositoryGroupNode) element).getObject()
							.getName();
				}
				return null;
			}

			@Override
			public Object getValue(Object element, String property) {
				String result = doGetValue(element);
				initialValue[0] = result;
				return result;
			}

			@Override
			public void modify(Object element, String property, Object value) {
				if (element instanceof Item) {
					element = ((Item) element).getData();
				}
				if (element instanceof RepositoryGroupNode
						&& value instanceof CharSequence) {
					RepositoryGroup group = ((RepositoryGroupNode) element)
							.getObject();
					String newName = value.toString().trim();
					if (!newName.equals(group.getName())) {
						RepositoryGroups.INSTANCE.renameGroup(group, newName);
						// Refresh all to get re-sorting
						viewer.refresh();
						// Re-set the selection to get a status bar update
						viewer.setSelection(new StructuredSelection(element),
								true);
					}
				}
			}
		});
	}

	/**
	 * If a text editor is open and has the focus, paste into it.
	 *
	 * @return {@code true} if there was a text editor that did paste,
	 *         {@code false} otherwise
	 */
	public boolean pasteInEditor() {
		if (filterField != null && !filterField.isDisposed()) {
			// We're editing
			filterField.paste();
			return true;
		}
		if (textCellEditor != null && textCellEditor.isActivated()) {
			// We're editing
			textCellEditor.performPaste();
			return true;
		}
		return false;
	}

	/**
	 * Opens a text input to filter the node's children.
	 *
	 * @param node
	 *            to filter
	 */
	public void filter(@NonNull FilterableNode node) {
		if (filterField != null) {
			// Allow only one tree editor
			return;
		}
		CommonViewer rawViewer = getCommonViewer();
		if (!(rawViewer instanceof RepositoriesCommonViewer)) {
			return; // Cannot happen
		}
		RepositoriesCommonViewer viewer = (RepositoriesCommonViewer) rawViewer;
		IContentProvider rawProvider = viewer.getContentProvider();
		if (!(rawProvider instanceof ITreeContentProvider)) {
			return; // Doesn't occur
		}
		ITreeContentProvider provider = (ITreeContentProvider) rawProvider;
		if (!provider.hasChildren(node)) {
			return;
		}
		TreeItem treeItem = viewer.getItem(node);
		if (treeItem == null) {
			return;
		}
		if (!viewer.getExpandedState(node)) {
			try {
				viewer.getTree().setRedraw(false);
				viewer.setExpandedState(node, true);
			} finally {
				viewer.getTree().setRedraw(true);
			}
		}
		// Pop up search field with initial pattern set; update pattern and
		// request a refresh of the node on each change with a small delay.
		AtomicReference<String> currentPattern = new AtomicReference<>();
		WorkbenchJob refresher = new WorkbenchJob(
				UIText.RepositoriesView_FilterJob) {

			@Override
			public boolean belongsTo(Object family) {
				return JobFamilies.REPO_VIEW_REFRESH.equals(family)
						|| super.belongsTo(family);
			}

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (!monitor.isCanceled()) {
					filter(viewer, node, currentPattern.get());
					return Status.OK_STATUS;
				}
				return Status.CANCEL_STATUS;
			}

		};
		refresher.setUser(false);
		Composite container = new Composite(viewer.getTree(), SWT.NONE);
		container.setVisible(false);
		GridLayoutFactory.fillDefaults().applyTo(container);
		// Use a text field without icons. With icons, we'd get a focusLost
		// event when the icons are selected at least on Cocoa.
		Text field = new Text(container, SWT.SEARCH);
		GridData textData = GridDataFactory.fillDefaults().grab(true, false)
				.create();
		textData.minimumWidth = 150;
		field.setLayoutData(textData);
		field.setMessage(UIText.RepositoriesView_FilterMessage);
		String pattern = node.getFilter();
		if (pattern != null) {
			field.setText(pattern);
			field.selectAll();
		}
		TreeFilterEditor filterEditor = new TreeFilterEditor(viewer.getTree(),
				treeItem, 0, container);
		field.addVerifyListener(e -> e.text = Utils.firstLine(e.text));
		field.addModifyListener(e -> {
			refresher.cancel();
			currentPattern.set(field.getText());
			refresher.schedule(200L);
		});
		FocusAdapter closeOnFocusLost = new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {
				filterField = null;
				filterEditor.dispose();
				if (!container.isDisposed()) {
					container.setVisible(false);
					container.dispose();
				}
			}
		};
		field.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				int key = SWTKeySupport.convertEventToUnmodifiedAccelerator(e);
				if (key == SWT.ESC) {
					// Reset
					refresher.cancel();
					currentPattern.set(pattern);
					refresher.schedule();
				}
				if (key == SWT.ESC || key == SWT.CR || key == SWT.LF
						|| e.character == '\r' || e.character == '\n') {
					// Character tests catch NUMPAD-ENTER
					filterField = null;
					filterEditor.dispose();
					field.removeFocusListener(closeOnFocusLost);
					container.setVisible(false);
					container.dispose();
					viewer.getTree().setFocus();
				}
			}
		});
		container.getDisplay().asyncExec(() -> {
			if (!container.isDisposed()) {
				container.setVisible(true);
				filterField = field;
				field.setFocus();
				field.addFocusListener(closeOnFocusLost);
			}
		});
	}

	private void filter(RepositoriesCommonViewer viewer,
			FilterableNode filterNode, String filter) {
		FilterCache.INSTANCE.set(filterNode, filter);
		Tree tree = viewer.getTree();
		if (tree == null || tree.isDisposed()) {
			return;
		}
		try {
			tree.setRedraw(false);
			TreeItem item = viewer.getItem(filterNode);
			Object currentNode = item.getData();
			if (filterNode != currentNode && filterNode.equals(currentNode)) {
				((FilterableNode) currentNode)
						.setFilter(filterNode.getFilter());
				viewer.update(currentNode, null);
			}
			viewer.refresh(filterNode);
		} finally {
			tree.setRedraw(true);
		}
		// Force an update of the status bar
		viewer.setSelection(viewer.getSelection());
	}

	private void executeOpenCommandWithConfirmation(RepositoryTreeNode element,
			String refName) {
		if (targetIsCurrentBranch(element, refName)) {
			return;
		}
		if (!BranchOperationUI.checkoutWillShowQuestionDialog(refName)) {
			IPreferenceStore store = Activator.getDefault()
					.getPreferenceStore();

			if (store.getBoolean(UIPreferences.SHOW_CHECKOUT_CONFIRMATION)) {
				MessageDialogWithToggle dialog = new MessageDialogWithToggle(
						getViewSite().getShell(),
						UIText.RepositoriesView_CheckoutConfirmationTitle, null,
						MessageFormat.format(
								UIText.RepositoriesView_CheckoutConfirmationMessage,
								Repository.shortenRefName(refName)),
						MessageDialog.QUESTION,
						new String[] {
								UIText.RepositoriesView_CheckoutConfirmationDefaultButtonLabel,
								IDialogConstants.CANCEL_LABEL },
						0,
						UIText.RepositoriesView_CheckoutConfirmationToggleMessage,
						false);
				// Since we use a custom button here, we may get back the first
				// internal ID instead of Window.OK.
				int result = dialog.open();
				if (result != Window.OK
						&& result != IDialogConstants.INTERNAL_ID) {
					return;
				}
				// And with custom buttons and internal IDs, the framework
				// doesn't save the preference (even if we set the preference
				// store and key).
				if (dialog.getToggleState()) {
					store.setValue(UIPreferences.SHOW_CHECKOUT_CONFIRMATION,
							false);
				}
			}
		}
		executeOpenCommand(element);
	}

	private boolean targetIsCurrentBranch(RepositoryTreeNode element,
			String refName) {
		try {
			Repository repository = element.getRepository();
			return refName.equals(repository.getFullBranch());
		} catch (IOException e) {
			// ignore and just execute the checkout operation
			return false;
		}
	}

	private void executeOpenCommand(RepositoryTreeNode element) {
		CommonUtils.runCommand("org.eclipse.egit.ui.RepositoriesViewOpen", //$NON-NLS-1$
				new StructuredSelection(element));
	}

	private void executeFetchCommand(FetchNode node) {
		CommonUtils.runCommand(ActionCommands.SIMPLE_FETCH_ACTION,
				new StructuredSelection(node));
	}

	private void initRepositoriesAndListeners() {
		synchronized (repositories) {
			refCache.remove(repositories);
			repositories.clear();
			unregisterRepositoryListeners();
			Set<File> dirs = new HashSet<>();
			// listen for repository changes
			for (String dir : RepositoryUtil.INSTANCE
					.getConfiguredRepositories()) {
				File repoDir = new File(dir);
				try {
					Repository repo = RepositoryCache.INSTANCE
							.lookupRepository(repoDir);
					listenToRepository(repo);
					dirs.add(repo.getDirectory());
					repositories.add(repo);
				} catch (IOException e) {
					String message = NLS
							.bind(UIText.RepositoriesView_ExceptionLookingUpRepoMessage,
									repoDir.getPath());
					Activator.handleError(message, e, false);
					RepositoryUtil.INSTANCE.removeDir(repoDir);
				}
			}
			// Also listen to submodules and nested git repositories.
			for (Repository repo : RepositoryCache.INSTANCE
					.getAllRepositories()) {
				if (!dirs.contains(repo.getDirectory())) {
					listenToRepository(repo);
					dirs.add(repo.getDirectory());
				}
			}
		}
	}

	private void listenToRepository(Repository repo) {
		myListeners.add(repo.getListenerList()
				.addIndexChangedListener(myIndexChangedListener));
		myListeners.add(repo.getListenerList()
				.addRefsChangedListener(myRefsChangedListener));
		myListeners.add(repo.getListenerList()
				.addConfigChangedListener(myConfigChangeListener));
	}

	@Override
	public void dispose() {
		if (filterCacheLoaded) {
			// Don't save if didn't load for whatever reason. Otherwise we might
			// overwrite existing persisted data with an empty cache.
			filterCacheLoaded = false;
			FilterCache.INSTANCE.save();
		}
		if (textCellEditor != null) {
			textCellEditor.dispose();
			textCellEditor = null;
		}
		if (reactOnSelection != null) {
			reactOnSelection.removeListener(reactOnSelectionListener);
		}
		if (branchHierarchy != null) {
			branchHierarchy.removeListener(stateChangeListener);
		}
		refreshUiJob.cancel();

		RepositoryUtil.INSTANCE.getPreferences().removePreferenceChangeListener(
				configurationListener);

		ISelectionService srv = getSite().getService(ISelectionService.class);
		srv.removePostSelectionListener(selectionChangedListener);

		unregisterRepositoryListeners();
		refCache.remove(repositories);
		refCache.dispose();
		repositories.clear();

		super.dispose();
	}

	/**
	 * @see #showPaths(List)
	 * @param resource
	 */
	private void showResource(final IResource resource) {
		IPath location = resource.getLocation();
		if (location != null)
			showPaths(Arrays.asList(location));
	}

	/**
	 * Opens the tree and marks the working directory files or folders that
	 * represent the passed paths if possible.
	 *
	 * @param paths
	 *            the paths to show
	 */
	private void showPaths(final List<IPath> paths) {
		Map<Repository, Collection<String>> pathsByRepo = ResourceUtil
				.splitPathsByRepository(paths);
		boolean added = checkNotConfiguredRepositories(pathsByRepo);
		if (added) {
			scheduleRefresh(0, () -> {
				if (UIUtils.isUsable(getCommonViewer())) {
					selectAndReveal(pathsByRepo);
				}
			});
		} else {
			selectAndReveal(pathsByRepo);
		}
	}

	private boolean checkNotConfiguredRepositories(
			Map<Repository, Collection<String>> pathsByRepo) {
		boolean added = false;
		for (Repository repository : pathsByRepo.keySet()) {
			try {
				RepositoryTreeNode node = getRepositoryChildNode(repository,
						RepositoryTreeNodeType.WORKINGDIR);
				if (node == null) {
					added |= RepositoryUtil.INSTANCE
							.addConfiguredRepository(repository.getDirectory());
				}
			} catch (IllegalArgumentException iae) {
				Activator.handleError(iae.getMessage(), iae, false);
				continue;
			}
		}
		return added;
	}

	private void selectAndReveal(
			Map<Repository, Collection<String>> pathsByRepo) {
		final List<RepositoryTreeNode> nodesToShow = new ArrayList<>();
		Repository repository = null;
		for (Map.Entry<Repository, Collection<String>> entry : pathsByRepo
				.entrySet()) {
			repository = entry.getKey();
			for (String repoPath : entry.getValue()) {
				final RepositoryTreeNode node = getNodeForPath(repository,
						repoPath);
				if (node != null) {
					nodesToShow.add(node);
				}
			}
		}

		if (repository != null && !nodesToShow.isEmpty()
				&& pathsByRepo.size() == 1) {
			lastSelectedRepository = repository.getDirectory();
		} else {
			lastSelectedRepository = null;
		}
		List<?> current = getCommonViewer().getStructuredSelection().toList();
		Set<?> currentlySelected = new HashSet<>(current);
		if (currentlySelected.containsAll(nodesToShow)) {
			getCommonViewer().getTree().showSelection();
		} else {
			selectReveal(new StructuredSelection(nodesToShow));
		}
	}

	/**
	 * Expands the tree element for the given group
	 *
	 * @param group
	 */
	public void expandNodeForGroup(RepositoryGroup group) {
		if (group != null) {
			getCommonViewer().expandToLevel(new RepositoryGroupNode(group), 1);
		}
	}

	/**
	 * Reveals and shows the given repository in the view.
	 *
	 * @param repositoryToShow
	 */
	public void showRepository(Repository repositoryToShow) {
		ITreeContentProvider cp = (ITreeContentProvider) getCommonViewer()
				.getContentProvider();
		RepositoryTreeNode node = findRepositoryNode(cp,
				cp.getElements(getCommonViewer().getInput()), repositoryToShow);
		if (node != null) {
			lastSelectedRepository = repositoryToShow.getDirectory();
			selectReveal(new StructuredSelection(node));
		}
	}

	/**
	 * Refresh Repositories View
	 */
	public void refresh() {
		initRepositoriesAndListeners();
		scheduleRefresh(0, null);
	}

	private void trace(String message) {
		GitTraceLocation.getTrace().trace(
				GitTraceLocation.REPOSITORIESVIEW.getLocation(), message);
	}

	private void scheduleRefresh() {
		scheduleRefresh(DEFAULT_REFRESH_DELAY, null);
	}

	private synchronized void scheduleRefresh(long delay, Runnable uiTask) {
		refreshUiJob.uiTask.compareAndSet(null, uiTask);
		refreshUiJob.schedule(delay);
	}

	class RefreshUiJob extends WorkbenchJob {
		final AtomicReference<Runnable> uiTask = new AtomicReference<>();

		RefreshUiJob() {
			super(PlatformUI.getWorkbench().getDisplay(),
					"Refreshing Git Repositories View"); //$NON-NLS-1$
			setSystem(true);
			setUser(false);
		}

		@Override
		public boolean belongsTo(Object family) {
			return JobFamilies.REPO_VIEW_REFRESH.equals(family);
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			final boolean trace = GitTraceLocation.REPOSITORIESVIEW.isActive();
			long start = 0;
			if (trace) {
				start = System.currentTimeMillis();
				trace("Starting async update job"); //$NON-NLS-1$
			}
			CommonViewer tv = getCommonViewer();
			if (monitor.isCanceled() || !UIUtils.isUsable(tv)) {
				return Status.CANCEL_STATUS;
			}

			tv.getControl().setRedraw(false);
			try {
				tv.refresh(true);
			} finally {
				tv.getControl().setRedraw(true);
			}
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			IWorkbenchWindow ww = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();
			IViewPart part = ww == null ? null
					: ww.getActivePage().findView(IPageLayout.ID_PROP_SHEET);
			if (part instanceof PropertySheet) {
				PropertySheet sheet = (PropertySheet) part;
				IPage page = sheet.getCurrentPage();
				if (page instanceof PropertySheetPage) {
					((PropertySheetPage) page).refresh();
				}
			}

			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			Runnable task = uiTask.getAndSet(null);
			if (task != null) {
				task.run();
			}
			if (trace) {
				trace("Ending async update job after " //$NON-NLS-1$
						+ (System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$
			}
			return monitor.isCanceled() ? Status.CANCEL_STATUS
					: Status.OK_STATUS;
		}
	}

	private void unregisterRepositoryListeners() {
		myListeners.forEach(ListenerHandle::remove);
		myListeners.clear();
	}

	@Override
	public boolean show(ShowInContext context) {
		ISelection selection = context.getSelection();
		if ((selection instanceof IStructuredSelection)
				&& !selection.isEmpty()) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			List<IPath> paths = new ArrayList<>();
			for (Iterator it = ss.iterator(); it.hasNext();) {
				Object element = it.next();
				IResource resource = AdapterUtils.adaptToAnyResource(element);
				if (resource != null) {
					IPath location = resource.getLocation();
					if (location != null)
						paths.add(location);
				} else if (element instanceof IPath)
					paths.add((IPath) element);
			}
			if (!paths.isEmpty()) {
				showPaths(paths);
				return true;
			}

			Repository repository = SelectionUtils.getRepository(ss);
			if (repository != null) {
				showRepository(repository);
				return true;
			}
		}
		if(context.getInput() instanceof IFileEditorInput) {
			IFileEditorInput input = (IFileEditorInput) context.getInput();
			showResource(input.getFile());
			return true;
		}
		Repository repository = Adapters.adapt(context.getInput(),
				Repository.class);
		if (repository != null) {
			showRepository(repository);
			return true;
		}
		return false;
	}

	@Override
	public ShowInContext getShowInContext() {
		IStructuredSelection selection = (IStructuredSelection) getCommonViewer()
				.getSelection();
		List<Object> elements = getShowInElements(selection);
		// GenericHistoryView only shows a selection of a single resource (see
		// bug 392949), so prepare our own history page input which can contain
		// multiple files to support showing more than one file in history.
		// It's also necessary for a single file that is outside of the
		// workspace (and as such is not an IResource).
		HistoryPageInput historyPageInput = getHistoryPageInput(selection);
		return new ShowInContext(historyPageInput, new StructuredSelection(elements));
	}

	@Override
	public String[] getShowInTargetIds() {
		IStructuredSelection selection = (IStructuredSelection) getCommonViewer()
				.getSelection();
		for (Object element : selection.toList())
			if (element instanceof RepositoryNode) {
				return new String[] { IHistoryView.VIEW_ID, ReflogView.VIEW_ID,
						StagingView.VIEW_ID };
			} else if (element instanceof RefNode) {
				return new String[] { IHistoryView.VIEW_ID,
						ReflogView.VIEW_ID };
			}

		// Make sure History view is always listed, regardless of perspective
		return new String[] { IHistoryView.VIEW_ID };
	}

	private static List<Object> getShowInElements(IStructuredSelection selection) {
		List<Object> elements = new ArrayList<>();
		for (Object element : selection.toList()) {
			if (element instanceof FileNode || element instanceof FolderNode
					|| element instanceof WorkingDirNode) {
				RepositoryTreeNode treeNode = (RepositoryTreeNode) element;
				IPath path = treeNode.getPath();
				IResource resource = ResourceUtil.getResourceForLocation(path,
						false);
				if (resource != null)
					elements.add(resource);
			} else if (element instanceof RepositoryNode) {
				// Can be shown in History, Reflog and Properties views
				elements.add(element);
			} else if (element instanceof RepositoryNode
					|| element instanceof RemoteNode
					|| element instanceof FetchNode
					|| element instanceof PushNode
					|| element instanceof TagNode
					|| element instanceof RefNode) {
				// These can be shown in Properties view directly
				elements.add(element);
			}
		}
		return elements;
	}

	/**
	 * @param selection
	 * @return the HistoryPageInput corresponding to the selection, or null
	 */
	private static HistoryPageInput getHistoryPageInput(IStructuredSelection selection) {
		List<File> files = new ArrayList<>();
		Repository repo = null;
		for (Object element : selection.toList()) {
			Repository nodeRepository;
			if (element instanceof FileNode) {
				FileNode fileNode = (FileNode) element;
				files.add(fileNode.getObject());
				nodeRepository = fileNode.getRepository();
			} else if (element instanceof FolderNode) {
				FolderNode folderNode = (FolderNode) element;
				files.add(folderNode.getObject());
				nodeRepository = folderNode.getRepository();
			} else {
				// Don't return input if selection is not file/folder
				return null;
			}
			if (repo == null)
				repo = nodeRepository;
			// Don't return input if nodes from different repositories are selected
			if (repo != nodeRepository)
				return null;
		}
		if (repo != null)
			return new HistoryPageInput(repo, files.toArray(new File[0]));
		else
			return null;
	}

	private IWorkbenchPart determinePart(IWorkbenchPart part) {
		IWorkbenchPart currentPart = part;
		// this may happen if we switch between editors
		if (currentPart instanceof IEditorPart) {
			if (currentPart instanceof MultiPageEditorPart) {
				Object nestedEditor = ((MultiPageEditorPart) part)
						.getSelectedPage();
				if (nestedEditor instanceof IEditorPart) {
					currentPart = ((IEditorPart) nestedEditor);
				}
			}
		}
		return currentPart;
	}

	private ISelection convertSelection(IWorkbenchPart part,
			ISelection selection) {
		if (part instanceof IEditorPart) {
			IEditorInput input = ((IEditorPart) part).getEditorInput();
			if (input instanceof IFileEditorInput) {
				return new StructuredSelection(
						((IFileEditorInput) input).getFile());
			} else if (input instanceof IURIEditorInput) {
				return new StructuredSelection(input);
			}
		}
		return selection;
	}

	private void reactOnSelection(ISelection selection) {
		if (layout.topControl != emptyArea
				&& selection instanceof StructuredSelection) {
			StructuredSelection ssel = (StructuredSelection) selection;
			if (ssel.size() != 1) {
				return;
			}
			Object selected = ssel.getFirstElement();
			IResource adapted = AdapterUtils.adaptToAnyResource(selected);
			if (adapted != null) {
				showResource(adapted);
				return;
			}
			if ((!(selected instanceof RepositoryNode)
					&& !(selected instanceof RepositoryVirtualNode))) {
				File file = Adapters.adapt(selected, File.class);
				if (file != null) {
					IPath path = new Path(file.getAbsolutePath());
					showPaths(Arrays.asList(path));
					return;
				}
			}
			Repository repository = Adapters.adapt(selected,
					Repository.class);
			if (repository != null && !repository.getDirectory()
					.equals(lastSelectedRepository)) {
				showRepository(repository);
				return;
			}
		}
	}

	private void showEditor(FileNode node) {
		File file = node.getObject();
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			IEditorPart editor = EgitUiEditorUtils.findEditor(file, page);
			IEditorPart active = page.getActiveEditor();
			if (editor != null && editor != active) {
				window.getWorkbench().getDisplay()
						.asyncExec(() -> page.bringToTop(editor));
			}
		}
	}

	private RepositoryTreeNode getRepositoryChildNode(Repository repository,
			RepositoryTreeNodeType type) {
		ITreeContentProvider cp = (ITreeContentProvider) getCommonViewer()
				.getContentProvider();
		RepositoryTreeNode repoNode = findRepositoryNode(cp,
				cp.getElements(getCommonViewer().getInput()), repository);
		return repoNode == null ? null : findChild(cp, repoNode, type);
	}

	private RepositoryTreeNode findChild(ITreeContentProvider cp,
			RepositoryTreeNode root, RepositoryTreeNodeType type) {
		for (Object child : cp.getChildren(root)) {
			RepositoryTreeNode childNode = (RepositoryTreeNode) child;
			if (childNode.getType() == type) {
				return childNode;
			}
		}
		return null;
	}

	private RepositoryTreeNode findRepositoryNode(
			ITreeContentProvider cp, Object[] roots,
			Repository repository) {
		for (Object repo : roots) {
			RepositoryTreeNode node = (RepositoryTreeNode) repo;
			if (node instanceof RepositoryGroupNode) {
				RepositoryTreeNode candidate = findRepositoryNode(cp,
						cp.getChildren(node), repository);
				if (candidate != null) {
					return candidate;
				}
			} else {
				// TODO equals implementation of Repository?
				if (repository.getDirectory()
						.equals(((Repository) node.getObject())
								.getDirectory())) {
					return node;
				}
			}
		}
		// Might be a submodule
		for (Object repo : roots) {
			RepositoryTreeNode node = (RepositoryTreeNode) repo;
			RepositoryTreeNode submodules = findChild(cp, node,
						RepositoryTreeNodeType.SUBMODULES);
			if (submodules != null) {
				RepositoryTreeNode submoduleNode = findRepositoryNode(cp,
						cp.getChildren(submodules), repository);
				if (submoduleNode != null) {
					return submoduleNode;
				}
			}
		}
		return null;
	}

	private RepositoryTreeNode getNodeForPath(Repository repository,
			String repoRelativePath) {
		RepositoryTreeNode currentNode = getRepositoryChildNode(repository,
				RepositoryTreeNodeType.WORKINGDIR);
		CommonViewer viewer = getCommonViewer();
		if (currentNode == null) {
			return null;
		} else {
			// reveal repository in case working dir filter is applied
			final RepositoryTreeNode workingDir = currentNode;
			if (Arrays.stream(viewer.getFilters()).anyMatch(filter -> !filter
					.select(viewer, workingDir.getParent(), workingDir))) {
				return currentNode.getParent();
			}
		}
		ITreeContentProvider cp = (ITreeContentProvider) viewer
				.getContentProvider();
		IPath relPath = new Path(repoRelativePath);

		for (String segment : relPath.segments()) {
			for (Object child : cp.getChildren(currentNode)) {
				@SuppressWarnings("unchecked")
				RepositoryTreeNode<File> childNode = (RepositoryTreeNode<File>) child;
				if (childNode.getObject().getName().equals(segment)) {
					currentNode = childNode;
					break;
				}
			}
		}
		return currentNode;
	}

	/**
	 * Customized {@link CommonViewer} that
	 * <ul>
	 * <li>switches back to the empty area if the tree view becomes empty,
	 * and</li>
	 * <li>adds additional information at the end of labels, and</li>
	 * <li>provides access to the {@link TreeItem} of an element.</li>
	 * </ul>
	 */
	private class RepositoriesCommonViewer extends CommonViewer {

		/**
		 * Creates a new {@link RepositoriesCommonViewer}.
		 *
		 * @param viewId
		 *            of the view containing the viewer
		 * @param parent
		 *            for the new viewer
		 * @param style
		 *            of the new viewer
		 */
		public RepositoriesCommonViewer(String viewId, Composite parent,
				int style) {
			super(viewId, parent, style);
		}

		@Override
		protected void init() {
			super.init();
			setLabelProvider(new PathAddingLabelProvider(
					getNavigatorContentService().createCommonLabelProvider()));
		}

		@Override
		public void refresh() {
			super.refresh();
			afterRefresh(this);
		}

		@Override
		public void refresh(boolean updateLabels) {
			super.refresh(updateLabels);
			afterRefresh(this);
		}

		public TreeItem getItem(Object element) {
			Widget item = findItem(element);
			if (item instanceof TreeItem) {
				return (TreeItem) item;
			}
			return null;
		}
	}

	@SuppressWarnings("restriction")
	private static class PathAddingLabelProvider extends
			org.eclipse.ui.internal.navigator.NavigatorDecoratingLabelProvider {

		public PathAddingLabelProvider(ILabelProvider commonLabelProvider) {
			super(commonLabelProvider);
		}

		@Override
		public void update(ViewerCell cell) {
			RepositoryTreeNodeLabelProvider.update(cell, super::update);
		}
	}

	/**
	 * A customized {@link ControlEditor} that keeps the editor control just
	 * behind the label of the node.
	 * <p>
	 * Note that the super class handles scolling and invokes {@link #layout()}
	 * when the parent is scrolled.
	 * </p>
	 */
	private static class TreeFilterEditor extends ControlEditor {

		private Composite parent;

		private TreeItem item;

		private int columnIndex;

		private Point editorSize;

		public TreeFilterEditor(Composite parent, TreeItem item,
				int columnIndex, Control editor) {
			super(parent);
			this.parent = parent;
			this.item = item;
			this.columnIndex = columnIndex;
			editorSize = editor.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			setEditor(editor);
		}

		@Override
		public void layout() {
			Point textSize = null;
			GC gc = null;
			try {
				gc = new GC(parent.getDisplay());
				gc.setFont(item.getFont(columnIndex));
				textSize = gc.stringExtent(item.getText(columnIndex));
			} finally {
				if (gc != null) {
					gc.dispose();
				}
			}
			Rectangle cell = item.getBounds(columnIndex);
			Rectangle text = item.getTextBounds(columnIndex);
			Rectangle area = parent.getClientArea();
			area.y = cell.y;
			area.x = Math.max(area.x, Math.min(text.x + textSize.x + 5,
					area.x + area.width - editorSize.x));
			area.width = editorSize.x;
			area.height = editorSize.y;
			getEditor().setBounds(area);
		}

		@Override
		public void dispose() {
			super.dispose();
			parent = null;
			item = null;
			editorSize = null;
		}
	}
}
