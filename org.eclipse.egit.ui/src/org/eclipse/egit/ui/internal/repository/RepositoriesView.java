/*******************************************************************************
 * Copyright (c) 2010, 2017 SAP AG and others.
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

import org.eclipse.core.commands.Command;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.actions.ActionCommands;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.egit.ui.internal.history.HistoryPageInput;
import org.eclipse.egit.ui.internal.reflog.ReflogView;
import org.eclipse.egit.ui.internal.repository.tree.FetchNode;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.PushNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.repository.tree.StashedCommitNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.events.ConfigChangedEvent;
import org.eclipse.jgit.events.ConfigChangedListener;
import org.eclipse.jgit.events.IndexChangedEvent;
import org.eclipse.jgit.events.IndexChangedListener;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
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
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.WorkbenchJob;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * The "Git Repositories View"
 */
public class RepositoriesView extends CommonNavigator implements IShowInSource, IShowInTargetList {

	/** "remote" */
	public static final String REMOTE = "remote"; //$NON-NLS-1$

	/** "url" */
	public static final String URL = "url"; //$NON-NLS-1$

	/** "pushurl" */
	public static final String PUSHURL = "pushurl"; //$NON-NLS-1$

	/** "push" */
	public static final String PUSH = "push"; //$NON-NLS-1$

	/** "fetch" */
	public static final String FETCH = "fetch"; //$NON-NLS-1$

	/** view id */
	public static final String VIEW_ID = "org.eclipse.egit.ui.RepositoriesView"; //$NON-NLS-1$

	private static final long DEFAULT_REFRESH_DELAY = 1000;

	private final Set<Repository> repositories = new HashSet<>();

	private final RefsChangedListener myRefsChangedListener;

	private final IndexChangedListener myIndexChangedListener;

	private final ConfigChangedListener myConfigChangeListener;

	private final List<ListenerHandle> myListeners = new LinkedList<>();

	private Job scheduledJob;

	private RefreshUiJob refreshUiJob;

	private final RepositoryUtil repositoryUtil;

	private final RepositoryCache repositoryCache;

	private Composite emptyArea;

	private StackLayout layout;

	private volatile long lastInputChange = 0L;

	private volatile long lastInputUpdate = -1L;

	private boolean reactOnSelection;

	private final IPreferenceChangeListener configurationListener;

	private ISelectionListener selectionChangedListener;

	/**
	 * The default constructor
	 */
	public RepositoriesView() {
		refreshUiJob = new RefreshUiJob();
		repositoryUtil = Activator.getDefault().getRepositoryUtil();
		repositoryCache = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache();

		configurationListener = new IPreferenceChangeListener() {
			@Override
			public void preferenceChange(PreferenceChangeEvent event) {
				if (RepositoryUtil.PREFS_DIRECTORIES_REL
						.equals(event.getKey())) {
					lastInputChange = System.currentTimeMillis();
					scheduleRefresh(DEFAULT_REFRESH_DELAY, null);
				}
			}
		};

		myRefsChangedListener = new RefsChangedListener() {
			@Override
			public void onRefsChanged(RefsChangedEvent e) {
				scheduleRefresh(DEFAULT_REFRESH_DELAY, null);
			}
		};

		myIndexChangedListener = new IndexChangedListener() {
			@Override
			public void onIndexChanged(IndexChangedEvent event) {
				scheduleRefresh(DEFAULT_REFRESH_DELAY, null);

			}
		};

		myConfigChangeListener = new ConfigChangedListener() {
			@Override
			public void onConfigChanged(ConfigChangedEvent event) {
				scheduleRefresh(DEFAULT_REFRESH_DELAY, null);
			}
		};

		selectionChangedListener = new ISelectionListener() {
			@Override
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				if (!reactOnSelection || part == RepositoriesView.this) {
					return;
				}

				// this may happen if we switch between editors
				if (part instanceof IEditorPart) {
					IEditorInput input = ((IEditorPart) part).getEditorInput();
					if (input instanceof IFileEditorInput) {
						reactOnSelection(new StructuredSelection(
								((IFileEditorInput) input).getFile()));
					} else if (input instanceof IURIEditorInput) {
						reactOnSelection(new StructuredSelection(input));
					}

				} else {
					reactOnSelection(selection);
				}
			}
		};
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
				IHandlerService service = CommonUtils.getService(getViewSite(), IHandlerService.class);
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
				IHandlerService service = CommonUtils.getService(getViewSite(), IHandlerService.class);
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
				IHandlerService service = CommonUtils.getService(getViewSite(), IHandlerService.class);
				UIUtils.executeCommand(service,
						"org.eclipse.egit.ui.RepositoriesViewCreateRepository"); //$NON-NLS-1$
			}
		});
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL)
				.grab(true, false).applyTo(createLink);
	}

	@SuppressWarnings("boxing")
	@Override
	public void createPartControl(Composite aParent) {
		Composite displayArea = new Composite(aParent, SWT.NONE);
		layout = new StackLayout();
		displayArea.setLayout(layout);
		createEmptyArea(displayArea);

		super.createPartControl(displayArea);

		IWorkbenchWindow w = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		ICommandService csrv = CommonUtils.getService(w, ICommandService.class);
		Command command = csrv
				.getCommand("org.eclipse.egit.ui.RepositoriesLinkWithSelection"); //$NON-NLS-1$
		reactOnSelection = (Boolean) command.getState(
				RegistryToggleState.STATE_ID).getValue();

		IWorkbenchSiteProgressService service = CommonUtils.getService(getSite(), IWorkbenchSiteProgressService.class);
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

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		// integrate with Properties view
		if (adapter == IPropertySheetPage.class) {
			PropertySheetPage page = new PropertySheetPage();
			page.setPropertySourceProvider(
					new RepositoryPropertySourceProvider(page));
			return adapter.cast(page);
		}
		return super.getAdapter(adapter);
	}

	/**
	 * Used by the "link with selection" action
	 *
	 * @param reactOnSelection
	 */
	public void setReactOnSelection(boolean reactOnSelection) {
		this.reactOnSelection = reactOnSelection;
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
		// react on selection changes
		ISelectionService srv = CommonUtils.getService(getSite(), ISelectionService.class);
		srv.addPostSelectionListener(selectionChangedListener);
		// react on changes in the configured repositories
		repositoryUtil.getPreferences().addPreferenceChangeListener(
				configurationListener);
		initRepositoriesAndListeners();
		activateContextService();

		emptyArea.setBackground(viewer.getControl().getBackground());
		if (!repositories.isEmpty())
			layout.topControl = viewer.getControl();
		else
			layout.topControl = emptyArea;

		return viewer;
	}

	private void executeOpenCommandWithConfirmation(RepositoryTreeNode element,
			String refName) {
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

	private void executeOpenCommand(RepositoryTreeNode element) {
		CommonUtils.runCommand("org.eclipse.egit.ui.RepositoriesViewOpen", //$NON-NLS-1$
				new StructuredSelection(element));
	}

	private void executeFetchCommand(FetchNode node) {
		CommonUtils.runCommand(ActionCommands.SIMPLE_FETCH_ACTION,
				new StructuredSelection(node));
	}

	private void activateContextService() {
		IContextService contextService = CommonUtils.getService(getSite(), IContextService.class);
		if (contextService != null) {
			contextService.activateContext(VIEW_ID);
		}
	}

	private void initRepositoriesAndListeners() {
		synchronized (repositories) {
			repositories.clear();
			unregisterRepositoryListener();
			Set<File> dirs = new HashSet<>();
			// listen for repository changes
			for (String dir : repositoryUtil.getConfiguredRepositories()) {
				File repoDir = new File(dir);
				try {
					Repository repo = repositoryCache.lookupRepository(repoDir);
					listenToRepository(repo);
					dirs.add(repo.getDirectory());
					repositories.add(repo);
				} catch (IOException e) {
					String message = NLS
							.bind(UIText.RepositoriesView_ExceptionLookingUpRepoMessage,
									repoDir.getPath());
					Activator.handleError(message, e, false);
					repositoryUtil.removeDir(repoDir);
				}
			}
			// Also listen to submodules and nested git repositories.
			for (Repository repo : org.eclipse.egit.core.Activator.getDefault()
					.getRepositoryCache().getAllRepositories()) {
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
		// make sure to cancel the refresh job
		if (this.scheduledJob != null) {
			this.scheduledJob.cancel();
			this.scheduledJob = null;
		}
		refreshUiJob.cancel();

		repositoryUtil.getPreferences().removePreferenceChangeListener(
				configurationListener);

		ISelectionService srv = CommonUtils.getService(getSite(), ISelectionService.class);
		srv.removePostSelectionListener(selectionChangedListener);

		// remove RepositoryChangedListener
		unregisterRepositoryListener();
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
		for (Map.Entry<Repository, Collection<String>> entry : pathsByRepo.entrySet()) {
			Repository repository = entry.getKey();
			try {
				boolean newOne = repositoryUtil
						.addConfiguredRepository(repository.getDirectory());
				if (newOne) {
					added = true;
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
		for (Map.Entry<Repository, Collection<String>> entry : pathsByRepo
				.entrySet()) {
			Repository repository = entry.getKey();
			for (String repoPath : entry.getValue()) {
				final RepositoryTreeNode node = getNodeForPath(repository,
						repoPath);
				if (node != null) {
					nodesToShow.add(node);
				}
			}
		}
		selectReveal(new StructuredSelection(nodesToShow));
	}

	/**
	 * Reveals and shows the given repository in the view.
	 *
	 * @param repositoryToShow
	 */
	public void showRepository(Repository repositoryToShow) {
		ITreeContentProvider cp = (ITreeContentProvider) getCommonViewer()
				.getContentProvider();
		for (Object repo : cp.getElements(getCommonViewer().getInput())) {
			RepositoryTreeNode node = (RepositoryTreeNode) repo;
			if (repositoryToShow.getDirectory().equals(node.getRepository().getDirectory()))
				selectReveal(new StructuredSelection(node));
		}
	}
	/**
	 * Refresh Repositories View
	 */
	public void refresh() {
		lastInputUpdate = -1L;
		scheduleRefresh(0, null);
	}

	private void trace(String message) {
		GitTraceLocation.getTrace().trace(
				GitTraceLocation.REPOSITORIESVIEW.getLocation(), message);
	}

	private synchronized void scheduleRefresh(long delay, Runnable uiTask) {
		if (GitTraceLocation.REPOSITORIESVIEW.isActive()) {
			trace("Entering scheduleRefresh()"); //$NON-NLS-1$
		}

		refreshUiJob.cancel();
		refreshUiJob.uiTask = uiTask;

		if (scheduledJob != null) {
			schedule(scheduledJob, delay);
			return;
		}

		Job job = new Job("Refreshing Git Repositories data") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final CommonViewer tv = getCommonViewer();
				if (!UIUtils.isUsable(tv)) {
					return Status.CANCEL_STATUS;
				}
				final boolean trace = GitTraceLocation.REPOSITORIESVIEW
						.isActive();
				final boolean needsNewInput = lastInputChange > lastInputUpdate;
				if (trace) {
					trace("Running the update, new input required: " //$NON-NLS-1$
									+ (lastInputChange > lastInputUpdate));
				}
				lastInputUpdate = System.currentTimeMillis();
				if (needsNewInput) {
					initRepositoriesAndListeners();
				}

				refreshUiJob.needsNewInput = needsNewInput;
				refreshUiJob.schedule();
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return JobFamilies.REPO_VIEW_REFRESH.equals(family);
			}

		};
		job.setSystem(true);
		job.setUser(false);
		schedule(job, delay);
		scheduledJob = job;
	}

	class RefreshUiJob extends WorkbenchJob {
		volatile boolean needsNewInput;
		volatile Runnable uiTask;

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

			if (needsNewInput) {
				// keep expansion state and selection so that we can
				// restore the tree after update
				Object[] expanded = tv.getExpandedElements();
				tv.setInput(ResourcesPlugin.getWorkspace().getRoot());
				tv.setExpandedElements(expanded);
			} else {
				tv.refresh(true);
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

			if (!repositories.isEmpty()) {
				layout.topControl = getCommonViewer().getControl();
			} else {
				layout.topControl = emptyArea;
			}
			emptyArea.getParent().layout(true, true);
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			Runnable task = uiTask;
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

	private void schedule(Job job, long delay) {
		IWorkbenchSiteProgressService service = CommonUtils.getService(getSite(), IWorkbenchSiteProgressService.class);

		if (GitTraceLocation.REPOSITORIESVIEW.isActive()) {
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.REPOSITORIESVIEW.getLocation(),
					"Scheduling refresh job"); //$NON-NLS-1$
		}
		service.schedule(job, delay);
	}

	private void unregisterRepositoryListener() {
		for (ListenerHandle lh : myListeners)
			lh.remove();
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

	private void reactOnSelection(ISelection selection) {
		if (selection instanceof StructuredSelection) {
			StructuredSelection ssel = (StructuredSelection) selection;
			if (ssel.size() != 1) {
				return;
			}
			IResource adapted = AdapterUtils
					.adaptToAnyResource(ssel.getFirstElement());
			if (adapted != null) {
				showResource(adapted);
				return;
			}
			File file = Adapters.adapt(ssel.getFirstElement(), File.class);
			if (file != null) {
				IPath path = new Path(file.getAbsolutePath());
				showPaths(Arrays.asList(path));
				return;
			}
			Repository repository = Adapters.adapt(ssel.getFirstElement(),
					Repository.class);
			if (repository != null) {
				showRepository(repository);
				return;
			}
		}
	}

	private RepositoryTreeNode getRepositoryChildNode(Repository repository,
			RepositoryTreeNodeType type) {
		ITreeContentProvider cp = (ITreeContentProvider) getCommonViewer()
				.getContentProvider();
		for (Object repo : cp.getElements(getCommonViewer().getInput())) {
			RepositoryTreeNode node = (RepositoryTreeNode) repo;
			// TODO equals implementation of Repository?
			if (repository.getDirectory().equals(
					((Repository) node.getObject()).getDirectory())) {
				for (Object child : cp.getChildren(node)) {
					RepositoryTreeNode childNode = (RepositoryTreeNode) child;
					if (childNode.getType() == type) {
						return childNode;
					}
				}
			}
		}
		return null;
	}

	private RepositoryTreeNode getNodeForPath(Repository repository,
			String repoRelativePath) {
		RepositoryTreeNode currentNode = getRepositoryChildNode(repository,
				RepositoryTreeNodeType.WORKINGDIR);

		ITreeContentProvider cp = (ITreeContentProvider) getCommonViewer()
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
	 * Customized {@link CommonViewer} that doesn't create a decorating label
	 * provider -- our label provider already does so, and we don't want double
	 * decorations.
	 */
	private static class RepositoriesCommonViewer extends CommonViewer {

		public RepositoriesCommonViewer(String viewId, Composite parent,
				int style) {
			super(viewId, parent, style);
		}

		@Override
		protected void init() {
			super.init();
			IBaseLabelProvider labelProvider = getLabelProvider();
			// Our label provider already decorates. Avoid double decorating.
			if (labelProvider instanceof DecoratingStyledCellLabelProvider) {
				((DecoratingStyledCellLabelProvider) labelProvider)
						.setLabelDecorator(null);
			}
		}
	}
}
