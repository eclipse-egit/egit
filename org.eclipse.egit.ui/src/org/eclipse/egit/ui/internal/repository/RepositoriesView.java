/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza <dariusz@luksza.org> - add synchronization feature
 *    Daniel Megert <daniel_megert@ch.ibm.com> - Only check out on double-click
 *    Daniel Megert <daniel_megert@ch.ibm.com> - Don't reveal selection on refresh
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.io.IOException;
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
import org.eclipse.core.runtime.IAdaptable;
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
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.repository.tree.StashedCommitNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
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
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * The "Git Repositories View"
 */
public class RepositoriesView extends CommonNavigator {

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

	private final Set<Repository> repositories = new HashSet<Repository>();

	private final RefsChangedListener myRefsChangedListener;

	private final IndexChangedListener myIndexChangedListener;

	private final ConfigChangedListener myConfigChangeListener;

	private final List<ListenerHandle> myListeners = new LinkedList<ListenerHandle>();

	private Job scheduledJob;

	private final RepositoryUtil repositoryUtil;

	private final RepositoryCache repositoryCache;

	private Composite emptyArea;

	private StackLayout layout;

	private volatile long lastInputChange = 0L;

	private volatile long lastRepositoryChange = 0L;

	private volatile long lastInputUpdate = -1L;

	private boolean reactOnSelection = false;

	private final IPreferenceChangeListener configurationListener;

	private ISelectionListener selectionChangedListener;

	/**
	 * The default constructor
	 */
	public RepositoriesView() {
		repositoryUtil = Activator.getDefault().getRepositoryUtil();
		repositoryCache = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache();

		configurationListener = new IPreferenceChangeListener() {
			public void preferenceChange(PreferenceChangeEvent event) {
				lastInputChange = System.currentTimeMillis();
				scheduleRefresh(DEFAULT_REFRESH_DELAY);
			}
		};

		myRefsChangedListener = new RefsChangedListener() {
			public void onRefsChanged(RefsChangedEvent e) {
				lastRepositoryChange = System.currentTimeMillis();
				scheduleRefresh(DEFAULT_REFRESH_DELAY);
			}
		};

		myIndexChangedListener = new IndexChangedListener() {
			public void onIndexChanged(IndexChangedEvent event) {
				lastRepositoryChange = System.currentTimeMillis();
				scheduleRefresh(DEFAULT_REFRESH_DELAY);

			}
		};

		myConfigChangeListener = new ConfigChangedListener() {
			public void onConfigChanged(ConfigChangedEvent event) {
				lastRepositoryChange = System.currentTimeMillis();
				scheduleRefresh(DEFAULT_REFRESH_DELAY);
			}
		};

		selectionChangedListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				if (!reactOnSelection)
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
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER)
				.grab(true, true).applyTo(infoArea);
		GridLayoutFactory.swtDefaults().applyTo(infoArea);
		Label messageLabel = new Label(infoArea, SWT.WRAP);
		messageLabel.setText(UIText.RepositoriesView_messsageEmpty);
		messageLabel.setMenu(menu);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL)
				.grab(true, false).applyTo(messageLabel);
		Composite optionsArea = new Composite(infoArea, SWT.NONE);
		optionsArea.setMenu(menu);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(optionsArea);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER)
				.grab(true, true).applyTo(optionsArea);

		final FormToolkit toolkit = new FormToolkit(emptyArea.getDisplay());
		emptyArea.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				toolkit.dispose();
			}
		});
		final Color linkColor = JFaceColors.getHyperlinkText(emptyArea
				.getDisplay());

		Label addLabel = new Label(optionsArea, SWT.NONE);
		Image addImage = UIIcons.CREATE_REPOSITORY.createImage();
		UIUtils.hookDisposal(addLabel, addImage);
		addLabel.setImage(addImage);
		Hyperlink addLink = toolkit.createHyperlink(optionsArea,
				UIText.RepositoriesView_linkAdd, SWT.WRAP);
		addLink.setForeground(linkColor);
		addLink.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				IHandlerService service = (IHandlerService) getViewSite()
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
			public void linkActivated(HyperlinkEvent e) {
				IHandlerService service = (IHandlerService) getViewSite()
						.getService(IHandlerService.class);
				UIUtils.executeCommand(service,
						"org.eclipse.egit.ui.RepositoriesViewClone"); //$NON-NLS-1$
			}
		});
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL)
				.grab(true, false).applyTo(cloneLink);

		Label createLabel = new Label(optionsArea, SWT.NONE);
		Image createImage = UIIcons.NEW_REPOSITORY.createImage();
		UIUtils.hookDisposal(createLabel, createImage);
		createLabel.setImage(createImage);
		Hyperlink createLink = toolkit.createHyperlink(optionsArea,
				UIText.RepositoriesView_linkCreate, SWT.WRAP);
		createLink.setForeground(linkColor);
		createLink.setText(UIText.RepositoriesView_linkCreate);
		createLink.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				IHandlerService service = (IHandlerService) getViewSite()
						.getService(IHandlerService.class);
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
		ICommandService csrv = (ICommandService) w
				.getService(ICommandService.class);
		Command command = csrv
				.getCommand("org.eclipse.egit.ui.RepositoriesLinkWithSelection"); //$NON-NLS-1$
		reactOnSelection = (Boolean) command.getState(
				RegistryToggleState.STATE_ID).getValue();

		IWorkbenchSiteProgressService service = (IWorkbenchSiteProgressService) getSite()
				.getService(IWorkbenchSiteProgressService.class);
		if (service != null) {
			service.showBusyForFamily(JobFamilies.REPO_VIEW_REFRESH);
			service.showBusyForFamily(JobFamilies.CLONE);
		}
	}

	@Override
	public Object getAdapter(Class adapter) {
		// integrate with Properties view
		if (adapter == IPropertySheetPage.class) {
			PropertySheetPage page = new PropertySheetPage();
			page
					.setPropertySourceProvider(new RepositoryPropertySourceProvider(
							page));
			return page;
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
			public void doubleClick(DoubleClickEvent event) {
				TreeSelection sel = (TreeSelection) event.getSelection();
				RepositoryTreeNode element = (RepositoryTreeNode) sel
						.getFirstElement();
				if (element instanceof RefNode || element instanceof TagNode)
					executeOpenCommand();
			}
		});
		// handle open event for the working directory
		viewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				TreeSelection sel = (TreeSelection) event.getSelection();
				RepositoryTreeNode element = (RepositoryTreeNode) sel
						.getFirstElement();
				if (element instanceof FileNode
						|| element instanceof StashedCommitNode)
					executeOpenCommand();
			}
		});
		// react on selection changes
		ISelectionService srv = (ISelectionService) getSite().getService(
				ISelectionService.class);
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

	private void executeOpenCommand() {
		IHandlerService srv = (IHandlerService) getViewSite()
				.getService(IHandlerService.class);

		try {
			srv.executeCommand("org.eclipse.egit.ui.RepositoriesViewOpen", null); //$NON-NLS-1$
		} catch (Exception e) {
			Activator.handleError(e.getMessage(), e, false);
		}
	}

	private void activateContextService() {
		IContextService contextService = (IContextService) getSite()
				.getService(IContextService.class);
		if (contextService != null)
			contextService.activateContext(VIEW_ID);

	}

	private void initRepositoriesAndListeners() {
		synchronized (repositories) {
			repositories.clear();
			unregisterRepositoryListener();
			// listen for repository changes
			for (String dir : repositoryUtil.getConfiguredRepositories()) {
				File repoDir = new File(dir);
				try {
					Repository repo = repositoryCache.lookupRepository(repoDir);
					myListeners.add(repo.getListenerList()
							.addIndexChangedListener(myIndexChangedListener));
					myListeners.add(repo.getListenerList()
							.addRefsChangedListener(myRefsChangedListener));
					myListeners.add(repo.getListenerList()
							.addConfigChangedListener(myConfigChangeListener));
					repositories.add(repo);
				} catch (IOException e) {
					String message = NLS
							.bind(UIText.RepositoriesView_ExceptionLookingUpRepoMessage,
									repoDir.getPath());
					Activator.handleError(message, e, false);
					repositoryUtil.removeDir(repoDir);
				}
			}
		}
	}

	@Override
	public void dispose() {
		// make sure to cancel the refresh job
		if (this.scheduledJob != null) {
			this.scheduledJob.cancel();
			this.scheduledJob = null;
		}

		repositoryUtil.getPreferences().removePreferenceChangeListener(
				configurationListener);

		ISelectionService srv = (ISelectionService) getSite().getService(
				ISelectionService.class);
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
		final List<RepositoryTreeNode> nodesToShow = new ArrayList<RepositoryTreeNode>();

		Map<Repository, Collection<String>> pathsByRepo = ResourceUtil.splitPathsByRepository(paths);
		for (Map.Entry<Repository, Collection<String>> entry : pathsByRepo.entrySet()) {
			Repository repository = entry.getKey();
			try {
				boolean added = repositoryUtil.addConfiguredRepository(repository.getDirectory());
				if (added)
					scheduleRefresh(0);
			} catch (IllegalArgumentException iae) {
				Activator.handleError(iae.getMessage(), iae, false);
				continue;
			}

			if (this.scheduledJob != null)
				try {
					this.scheduledJob.join();
				} catch (InterruptedException e) {
					Activator.handleError(e.getMessage(), e, false);
				}

			for (String repoPath : entry.getValue()) {
				final RepositoryTreeNode node = getNodeForPath(repository, repoPath);
				if (node != null)
					nodesToShow.add(node);
			}
		}

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				selectReveal(new StructuredSelection(nodesToShow));
			}
		});
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
		scheduleRefresh(0);
	}

	private Job scheduleRefresh(long delay) {
		boolean trace = GitTraceLocation.REPOSITORIESVIEW.isActive();
		if (trace)
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.REPOSITORIESVIEW.getLocation(),
					"Entering scheduleRefresh()"); //$NON-NLS-1$

		if (scheduledJob != null
				&& (scheduledJob.getState() == Job.RUNNING
						|| scheduledJob.getState() == Job.WAITING || scheduledJob
						.getState() == Job.SLEEPING)) {
			if (trace)
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.REPOSITORIESVIEW.getLocation(),
						"Pending refresh job, returning"); //$NON-NLS-1$
			return scheduledJob;
		}

		final CommonViewer tv = getCommonViewer();
		final boolean needsNewInput = lastInputChange > lastInputUpdate;

		if (trace)
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.REPOSITORIESVIEW.getLocation(),
					"New input required: " + needsNewInput); //$NON-NLS-1$

		Job job = new Job("Refreshing Git Repositories view") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				boolean actTrace = GitTraceLocation.REPOSITORIESVIEW.isActive();
				if (actTrace)
					GitTraceLocation.getTrace().trace(
							GitTraceLocation.REPOSITORIESVIEW.getLocation(),
							"Running the update"); //$NON-NLS-1$
				lastInputUpdate = System.currentTimeMillis();
				if (needsNewInput)
					initRepositoriesAndListeners();

				if (!UIUtils.isUsable(tv))
					return Status.CANCEL_STATUS;
				PlatformUI.getWorkbench().getDisplay()
						.asyncExec(new Runnable() {
					public void run() {
						if (!UIUtils.isUsable(tv))
							return;
						long start = 0;
						boolean traceActive = GitTraceLocation.REPOSITORIESVIEW
								.isActive();
						if (traceActive) {
							start = System.currentTimeMillis();
							GitTraceLocation.getTrace().trace(
									GitTraceLocation.REPOSITORIESVIEW
											.getLocation(),
									"Starting async update job"); //$NON-NLS-1$
						}


						if (needsNewInput) {
							// keep expansion state and selection so that we can
							// restore the tree
							// after update
							Object[] expanded = tv.getExpandedElements();
							tv.setInput(ResourcesPlugin.getWorkspace()
									.getRoot());
							tv.setExpandedElements(expanded);
						} else
							tv.refresh(true);

						IViewPart part = PlatformUI.getWorkbench()
								.getActiveWorkbenchWindow().getActivePage()
								.findView(IPageLayout.ID_PROP_SHEET);
						if (part instanceof PropertySheet) {
							PropertySheet sheet = (PropertySheet) part;
							IPage page = sheet.getCurrentPage();
							if (page instanceof PropertySheetPage)
								((PropertySheetPage) page).refresh();
						}
						if (traceActive)
							GitTraceLocation
									.getTrace()
									.trace(
											GitTraceLocation.REPOSITORIESVIEW
													.getLocation(),
											"Ending async update job after " + (System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
						if (!repositories.isEmpty())
							layout.topControl = getCommonViewer().getControl();
						else
							layout.topControl = emptyArea;
						emptyArea.getParent().layout(true, true);
					}
				});

				if (lastInputChange > lastInputUpdate
						|| lastRepositoryChange > lastInputUpdate) {
					if (actTrace)
						GitTraceLocation.getTrace()
								.trace(
										GitTraceLocation.REPOSITORIESVIEW
												.getLocation(),
										"Rescheduling refresh job"); //$NON-NLS-1$
					schedule(DEFAULT_REFRESH_DELAY);
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (family.equals(JobFamilies.REPO_VIEW_REFRESH))
					return true;
				return super.belongsTo(family);
			}

		};
		job.setSystem(true);

		IWorkbenchSiteProgressService service = (IWorkbenchSiteProgressService) getSite()
				.getService(IWorkbenchSiteProgressService.class);

		if (trace)
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.REPOSITORIESVIEW.getLocation(),
					"Scheduling refresh job"); //$NON-NLS-1$
		service.schedule(job, delay);

		scheduledJob = job;
		return scheduledJob;
	}

	private void unregisterRepositoryListener() {
		for (ListenerHandle lh : myListeners)
			lh.remove();
		myListeners.clear();
	}

	public boolean show(ShowInContext context) {
		ISelection selection = context.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			List<IPath> paths = new ArrayList<IPath>();
			for (Iterator it = ss.iterator(); it.hasNext();) {
				Object element = it.next();
				IResource resource = AdapterUtils.adapt(element, IResource.class);
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
		}
		if(context.getInput() instanceof IFileEditorInput) {
			IFileEditorInput input = (IFileEditorInput) context.getInput();
			showResource(input.getFile());
		}
		return false;
	}

	private void reactOnSelection(ISelection selection) {
		if (selection instanceof StructuredSelection) {
			StructuredSelection ssel = (StructuredSelection) selection;
			if (ssel.size() != 1)
				return;
			if (ssel.getFirstElement() instanceof IResource)
				showResource((IResource) ssel.getFirstElement());
			if (ssel.getFirstElement() instanceof IAdaptable) {
				IResource adapted = (IResource) ((IAdaptable) ssel
						.getFirstElement()).getAdapter(IResource.class);
				if (adapted != null)
					showResource(adapted);
			}
		}
	}

	private RepositoryTreeNode getNodeForPath(Repository repository, String repoRelativePath) {
		RepositoryTreeNode currentNode = null;
		ITreeContentProvider cp = (ITreeContentProvider) getCommonViewer()
				.getContentProvider();
		for (Object repo : cp.getElements(getCommonViewer().getInput())) {
			RepositoryTreeNode node = (RepositoryTreeNode) repo;
			// TODO equals implementation of Repository?
			if (repository.getDirectory().equals(
					((Repository) node.getObject()).getDirectory())) {
				for (Object child : cp.getChildren(node)) {
					RepositoryTreeNode childNode = (RepositoryTreeNode) child;
					if (childNode.getType() == RepositoryTreeNodeType.WORKINGDIR) {
						currentNode = childNode;
						break;
					}
				}
				break;
			}
		}

		IPath relPath = new Path(repoRelativePath);

		for (String segment : relPath.segments())
			for (Object child : cp.getChildren(currentNode)) {
				@SuppressWarnings("unchecked")
				RepositoryTreeNode<File> childNode = (RepositoryTreeNode<File>) child;
				if (childNode.getObject().getName().equals(segment)) {
					currentNode = childNode;
					break;
				}
			}

		return currentNode;
	}
}
