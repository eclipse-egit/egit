/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.internal.clone.GitCloneWizard;
import org.eclipse.egit.ui.internal.clone.GitProjectsImportPage;
import org.eclipse.egit.ui.internal.repository.RepositoryTreeNode.RepositoryTreeNodeType;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.lib.RepositoryConfig;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.wizards.datatransfer.ExternalProjectImportWizard;
import org.osgi.service.prefs.BackingStoreException;

/**
 *
 * The Git Repositories view.
 * <p>
 * This keeps track of a bunch of local directory names each of which represent
 * a Git Repository. This list is stored in some Preferences object and used to
 * build the tree in the view.
 * <p>
 * TODO
 * <li>Icons</li>
 * <li>String externalization</li>
 * <li>Clarification whether to show projects, perhaps configurable switch</li>
 *
 */
public class RepositoriesView extends ViewPart implements ISelectionProvider {

	// TODO central constants? RemoteConfig ones are private
	static final String REMOTE = "remote"; //$NON-NLS-1$

	static final String URL = "url"; //$NON-NLS-1$

	static final String FETCH = "fetch"; //$NON-NLS-1$

	static final String PUSH = "push"; //$NON-NLS-1$

	private static final String PREFS_DIRECTORIES = "GitRepositoriesView.GitDirectories"; //$NON-NLS-1$

	private final List<ISelectionChangedListener> selectionListeners = new ArrayList<ISelectionChangedListener>();

	private ISelection currentSelection = new StructuredSelection();

	private Job scheduledJob;

	private TreeViewer tv;

	private IAction importAction;

	private IAction addAction;

	private IAction refreshAction;

	private static final class ContentProvider implements ITreeContentProvider {

		@SuppressWarnings("unchecked")
		public Object[] getElements(Object inputElement) {

			Comparator<RepositoryTreeNode<Repository>> sorter = new Comparator<RepositoryTreeNode<Repository>>() {

				public int compare(RepositoryTreeNode<Repository> o1,
						RepositoryTreeNode<Repository> o2) {
					return getRepositoryName(o1.getObject()).compareTo(
							getRepositoryName(o2.getObject()));
				}

			};

			Set<RepositoryTreeNode<Repository>> output = new TreeSet<RepositoryTreeNode<Repository>>(
					sorter);

			for (Repository repo : ((List<Repository>) inputElement)) {
				output.add(new RepositoryTreeNode<Repository>(null,
						RepositoryTreeNodeType.REPO, repo, repo));
			}

			return output.toArray();
		}

		public void dispose() {
			// nothing
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// nothing
		}

		public Object[] getChildren(Object parentElement) {

			RepositoryTreeNode node = (RepositoryTreeNode) parentElement;
			Repository repo = node.getRepository();

			switch (node.getType()) {

			case BRANCHES:

				List<RepositoryTreeNode<Ref>> refs = new ArrayList<RepositoryTreeNode<Ref>>();

				for (Ref ref : repo.getAllRefs().values()) {
					refs.add(new RepositoryTreeNode<Ref>(node,
							RepositoryTreeNodeType.REF, repo, ref));
				}

				return refs.toArray();

			case REMOTES:

				List<RepositoryTreeNode<String>> remotes = new ArrayList<RepositoryTreeNode<String>>();

				Repository rep = node.getRepository();

				Set<String> configNames = rep.getConfig()
						.getSubsections(REMOTE);

				for (String configName : configNames) {
					remotes.add(new RepositoryTreeNode<String>(node,
							RepositoryTreeNodeType.REMOTE, repo, configName));
				}

				return remotes.toArray();

			case REPO:

				List<RepositoryTreeNode<Repository>> branches = new ArrayList<RepositoryTreeNode<Repository>>();

				branches.add(new RepositoryTreeNode<Repository>(node,
						RepositoryTreeNodeType.BRANCHES, node.getRepository(),
						node.getRepository()));

				branches.add(new RepositoryTreeNode<Repository>(node,
						RepositoryTreeNodeType.PROJECTS, node.getRepository(),
						node.getRepository()));

				branches.add(new RepositoryTreeNode<Repository>(node,
						RepositoryTreeNodeType.REMOTES, node.getRepository(),
						node.getRepository()));

				return branches.toArray();

			case PROJECTS:

				List<RepositoryTreeNode<File>> projects = new ArrayList<RepositoryTreeNode<File>>();

				// TODO do we want to show the projects here?
				Collection<File> result = new HashSet<File>();
				Set<String> traversed = new HashSet<String>();
				collectProjectFilesFromDirectory(result, repo.getDirectory()
						.getParentFile(), traversed, new NullProgressMonitor());
				for (File file : result) {
					projects.add(new RepositoryTreeNode<File>(node,
							RepositoryTreeNodeType.PROJ, repo, file));
				}

				Comparator<RepositoryTreeNode<File>> sorter = new Comparator<RepositoryTreeNode<File>>() {

					public int compare(RepositoryTreeNode<File> o1,
							RepositoryTreeNode<File> o2) {
						return o1.getObject().getName().compareTo(
								o2.getObject().getName());
					}
				};
				Collections.sort(projects, sorter);

				return projects.toArray();

			default:
				return null;
			}

		}

		public Object getParent(Object element) {

			return ((RepositoryTreeNode) element).getParent();
		}

		public boolean hasChildren(Object element) {
			Object[] children = getChildren(element);
			return children != null && children.length > 0;
		}

		private boolean collectProjectFilesFromDirectory(
				Collection<File> files, File directory,
				Set<String> directoriesVisited, IProgressMonitor monitor) {

			// stolen from the GitCloneWizard; perhaps we should completely drop
			// the projects from this view, though
			if (monitor.isCanceled()) {
				return false;
			}
			monitor.subTask(NLS.bind(
					RepositoryViewUITexts.RepositoriesView_Checking_Message,
					directory.getPath()));
			File[] contents = directory.listFiles();
			if (contents == null)
				return false;

			// first look for project description files
			final String dotProject = IProjectDescription.DESCRIPTION_FILE_NAME;
			for (int i = 0; i < contents.length; i++) {
				File file = contents[i];
				if (file.isFile() && file.getName().equals(dotProject)) {
					files.add(file.getParentFile());
					// don't search sub-directories since we can't have nested
					// projects
					return true;
				}
			}
			// no project description found, so recurse into sub-directories
			for (int i = 0; i < contents.length; i++) {
				if (contents[i].isDirectory()) {
					if (!contents[i].getName().equals(
							GitProjectsImportPage.METADATA_FOLDER)) {
						try {
							String canonicalPath = contents[i]
									.getCanonicalPath();
							if (!directoriesVisited.add(canonicalPath)) {
								// already been here --> do not recurse
								continue;
							}
						} catch (IOException exception) {
							StatusManager.getManager().handle(
									new Status(IStatus.ERROR, Activator
											.getPluginId(), exception
											.getLocalizedMessage(), exception));

						}
						collectProjectFilesFromDirectory(files, contents[i],
								directoriesVisited, monitor);
					}
				}
			}
			return true;
		}

	}

	private static final class LabelProvider extends BaseLabelProvider
			implements ITableLabelProvider {

		// private DefaultInformationControl infoControl;

		/**
		 *
		 * @param viewer
		 */
		LabelProvider(final TreeViewer viewer) {

			viewer.setLabelProvider(this);
			Tree tree = viewer.getTree();
			TreeColumn col = new TreeColumn(tree, SWT.NONE);
			col.setWidth(400);
			viewer.getTree().addMouseTrackListener(new MouseTrackAdapter() {

				@Override
				public void mouseHover(MouseEvent e) {

					// Point eventPoint = new Point(e.x, e.y);
					//
					// TreeItem item = viewer.getTree().getItem(eventPoint);
					// if (item != null) {
					//
					// RepositoryTreeNode node = (RepositoryTreeNode) item
					// .getData();
					// String text = node.getRepository().getDirectory()
					// .getAbsolutePath();
					//
					// final ViewerCell cell = viewer.getCell(eventPoint);
					//
					// if (infoControl != null && infoControl.isVisible()) {
					// infoControl.setVisible(false);
					// }
					//
					// GC testGc = new GC(cell.getControl());
					// final Point textExtent = testGc.textExtent(text);
					// testGc.dispose();
					//
					// if (infoControl == null || !infoControl.isVisible()) {
					//
					// IInformationPresenter ips = new IInformationPresenter() {
					//
					// public String updatePresentation(
					// Display display, String hoverInfo,
					// TextPresentation presentation,
					// int maxWidth, int maxHeight) {
					// return hoverInfo;
					// }
					//
					// };
					//
					// infoControl = new DefaultInformationControl(Display
					// .getCurrent().getActiveShell().getShell(),
					// ips) {
					//
					// @Override
					// public void setInformation(String content) {
					// super.setInformation(content);
					// super.setSize(textExtent.x, textExtent.y);
					// }
					//
					// };
					// }
					//
					// Point dispPoint = viewer.getControl().toDisplay(
					// eventPoint);
					//
					// infoControl.setLocation(dispPoint);
					//
					// // the default info provider works better with \r ...
					// infoControl.setInformation(text);
					//
					// final MouseMoveListener moveListener = new
					// MouseMoveListener() {
					//
					// public void mouseMove(MouseEvent evt) {
					// infoControl.setVisible(false);
					// cell.getControl().removeMouseMoveListener(this);
					//
					// }
					// };
					//
					// cell.getControl().addMouseMoveListener(moveListener);
					//
					// infoControl.setVisible(true);
					//
					// }

				}

			});

		}

		public Image getColumnImage(Object element, int columnIndex) {
			return decorateImage(((RepositoryTreeNode) element).getType()
					.getIcon(), element);
		}

		public String getColumnText(Object element, int columnIndex) {

			RepositoryTreeNode node = (RepositoryTreeNode) element;
			switch (node.getType()) {
			case REPO:
				File directory = ((Repository) node.getObject()).getDirectory()
						.getParentFile();
				return (directory.getName() + " - " + directory //$NON-NLS-1$
						.getAbsolutePath());
			case BRANCHES:
				return RepositoryViewUITexts.RepositoriesView_Branches_Nodetext;
			case REMOTES:
				return RepositoryViewUITexts.RepositoriesView_RemotesNodeText;
			case REMOTE:
				String name = (String) node.getObject();
				String url = node.getRepository().getConfig().getString(REMOTE,
						name, URL);
				if (url != null && !url.equals("")) //$NON-NLS-1$
					name = name + " - " + url; //$NON-NLS-1$
				return name;
			case PROJECTS:
				return RepositoryViewUITexts.RepositoriesView_ExistingProjects_Nodetext;
			case REF:
				Ref ref = (Ref) node.getObject();
				// shorten the name
				String refName = node.getRepository().shortenRefName(
						ref.getName());
				if (ref.isSymbolic()) {
					refName = refName
							+ " - " //$NON-NLS-1$
							+ node.getRepository().shortenRefName(
									ref.getLeaf().getName());
				}
				return refName;
			case PROJ:

				File file = (File) node.getObject();
				return file.getName();

			default:
				return null;
			}
		}

		public Image decorateImage(final Image image, Object element) {

			RepositoryTreeNode node = (RepositoryTreeNode) element;
			switch (node.getType()) {

			case REF:
				Ref ref = (Ref) node.getObject();
				// shorten the name
				String refName = node.getRepository().shortenRefName(
						ref.getName());
				try {
					String branch = node.getBranch();
					if (refName.equals(branch)) {
						CompositeImageDescriptor cd = new CompositeImageDescriptor() {

							@Override
							protected Point getSize() {
								return new Point(image.getBounds().width, image
										.getBounds().width);
							}

							@Override
							protected void drawCompositeImage(int width,
									int height) {
								drawImage(image.getImageData(), 0, 0);
								drawImage(
										UIIcons.OVR_CHECKEDOUT.getImageData(),
										0, 0);

							}
						};
						return cd.createImage();
					}
				} catch (IOException e1) {
					// simply ignore here
				}
				return image;

			case PROJ:

				File file = (File) node.getObject();

				for (IProject proj : ResourcesPlugin.getWorkspace().getRoot()
						.getProjects()) {
					if (proj.getLocation().equals(
							new Path(file.getAbsolutePath()))) {
						CompositeImageDescriptor cd = new CompositeImageDescriptor() {

							@Override
							protected Point getSize() {
								return new Point(image.getBounds().width, image
										.getBounds().width);
							}

							@Override
							protected void drawCompositeImage(int width,
									int height) {
								drawImage(image.getImageData(), 0, 0);
								drawImage(
										UIIcons.OVR_CHECKEDOUT.getImageData(),
										0, 0);

							}
						};
						return cd.createImage();
					}
				}
				return image;

			default:
				return image;
			}
		}

	}

	private List<String> getGitDirs() {
		List<String> resultStrings = new ArrayList<String>();
		String dirs = new InstanceScope().getNode(Activator.getPluginId()).get(
				PREFS_DIRECTORIES, ""); //$NON-NLS-1$
		if (dirs != null && dirs.length() > 0) {
			StringTokenizer tok = new StringTokenizer(dirs, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				String dirName = tok.nextToken();
				File testFile = new File(dirName);
				if (testFile.exists()) {
					resultStrings.add(dirName);
				}
			}
		}
		Collections.sort(resultStrings);
		return resultStrings;
	}

	private void removeDir(String dir) {

		IEclipsePreferences prefs = new InstanceScope().getNode(Activator
				.getPluginId());

		TreeSet<String> resultStrings = new TreeSet<String>();
		String dirs = prefs.get(PREFS_DIRECTORIES, ""); //$NON-NLS-1$
		if (dirs != null && dirs.length() > 0) {
			StringTokenizer tok = new StringTokenizer(dirs, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				String dirName = tok.nextToken();
				File testFile = new File(dirName);
				if (testFile.exists()) {
					resultStrings.add(dirName);
				}
			}
		}

		if (resultStrings.remove(dir)) {
			StringBuilder sb = new StringBuilder();
			for (String gitDirString : resultStrings) {
				sb.append(gitDirString);
				sb.append(File.pathSeparatorChar);
			}

			prefs.put(PREFS_DIRECTORIES, sb.toString());
			try {
				prefs.flush();
			} catch (BackingStoreException e) {
				IStatus error = new Status(IStatus.ERROR, Activator
						.getPluginId(), e.getMessage(), e);
				Activator.getDefault().getLog().log(error);
			}
		}

	}

	@Override
	public Object getAdapter(Class adapter) {

		if (adapter == IPropertySheetPage.class) {
			PropertySheetPage page = new PropertySheetPage();
			page
					.setPropertySourceProvider(new RepositoryPropertySourceProvider(
							page));
			return page;
		}

		return super.getAdapter(adapter);
	}

	@Override
	public void createPartControl(Composite parent) {

		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		main.setLayout(new GridLayout(1, false));

		tv = new TreeViewer(main);
		tv.setContentProvider(new ContentProvider());
		new LabelProvider(tv);

		getSite().setSelectionProvider(this);

		tv.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {

				IStructuredSelection ssel = (IStructuredSelection) event
						.getSelection();
				if (ssel.size() == 1) {
					setSelection(new StructuredSelection(ssel.getFirstElement()));
				} else {
					setSelection(new StructuredSelection());
				}

			}
		});

		GridDataFactory.fillDefaults().grab(true, true).applyTo(tv.getTree());

		addContextMenu();

		addActionsToToolbar();

		scheduleRefresh();
	}

	private void addContextMenu() {
		tv.getTree().addMenuDetectListener(new MenuDetectListener() {

			public void menuDetected(MenuDetectEvent e) {

				tv.getTree().setMenu(null);
				Menu men = new Menu(tv.getTree());

				TreeItem testItem = tv.getTree().getItem(
						tv.getTree().toControl(new Point(e.x, e.y)));
				if (testItem == null) {
					addMenuItemsForPanel(men);
				} else {
					addMenuItemsForTreeSelection(men);
				}

				tv.getTree().setMenu(men);
			}
		});
	}

	private void addMenuItemsForPanel(Menu men) {

		MenuItem importItem = new MenuItem(men, SWT.PUSH);
		importItem
				.setText(RepositoryViewUITexts.RepositoriesView_ImportRepository_MenuItem);
		importItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				importAction.run();
			}

		});

		MenuItem addItem = new MenuItem(men, SWT.PUSH);
		addItem
				.setText(RepositoryViewUITexts.RepositoriesView_AddRepository_MenuItem);
		addItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				addAction.run();
			}

		});

		MenuItem refreshItem = new MenuItem(men, SWT.PUSH);
		refreshItem.setText(refreshAction.getText());
		refreshItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshAction.run();
			}

		});

	}

	@SuppressWarnings("unchecked")
	private void addMenuItemsForTreeSelection(Menu men) {

		final IStructuredSelection sel = (IStructuredSelection) tv
				.getSelection();

		boolean importableProjectsOnly = true;

		for (Object node : sel.toArray()) {
			RepositoryTreeNode tnode = (RepositoryTreeNode) node;
			importableProjectsOnly = tnode.getType() == RepositoryTreeNodeType.PROJ;
			if (!importableProjectsOnly)
				break;
		}

		if (importableProjectsOnly) {
			MenuItem sync = new MenuItem(men, SWT.PUSH);
			sync
					.setText(RepositoryViewUITexts.RepositoriesView_ImportProject_MenuItem);

			sync.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {

					IWorkspaceRunnable wsr = new IWorkspaceRunnable() {

						public void run(IProgressMonitor monitor)
								throws CoreException {

							for (Object selected : sel.toArray()) {
								RepositoryTreeNode<File> projectNode = (RepositoryTreeNode<File>) selected;
								File file = projectNode.getObject();

								IProjectDescription pd = ResourcesPlugin
										.getWorkspace().newProjectDescription(
												file.getName());
								IPath locationPath = new Path(file
										.getAbsolutePath());

								pd.setLocation(locationPath);

								ResourcesPlugin.getWorkspace().getRoot()
										.getProject(pd.getName()).create(pd,
												monitor);
								IProject project = ResourcesPlugin
										.getWorkspace().getRoot().getProject(
												pd.getName());
								project.open(monitor);

								File gitDir = projectNode.getRepository()
										.getDirectory();

								ConnectProviderOperation connectProviderOperation = new ConnectProviderOperation(
										project, gitDir);
								connectProviderOperation
										.run(new SubProgressMonitor(monitor, 20));

							}

						}
					};

					try {

						ResourcesPlugin.getWorkspace().run(wsr,
								ResourcesPlugin.getWorkspace().getRoot(),
								IWorkspace.AVOID_UPDATE,
								new NullProgressMonitor());

						scheduleRefresh();
					} catch (CoreException e1) {
						Activator.getDefault().getLog().log(e1.getStatus());
					}

				}

			});
		}

		// from here on, we only deal with single selection
		if (sel.size() > 1)
			return;

		final RepositoryTreeNode node = (RepositoryTreeNode) sel
				.getFirstElement();

		// for Refs (branches): checkout
		if (node.getType() == RepositoryTreeNodeType.REF) {

			final Ref ref = (Ref) node.getObject();

			MenuItem checkout = new MenuItem(men, SWT.PUSH);
			checkout
					.setText(RepositoryViewUITexts.RepositoriesView_CheckOut_MenuItem);
			checkout.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					Repository repo = node.getRepository();
					String refName = ref.getLeaf().getName();
					final BranchOperation op = new BranchOperation(repo,
							refName);
					IWorkspaceRunnable wsr = new IWorkspaceRunnable() {

						public void run(IProgressMonitor monitor)
								throws CoreException {
							op.run(monitor);
						}
					};
					try {
						ResourcesPlugin.getWorkspace().run(wsr,
								ResourcesPlugin.getWorkspace().getRoot(),
								IWorkspace.AVOID_UPDATE,
								new NullProgressMonitor());
						scheduleRefresh();
					} catch (CoreException e1) {
						MessageDialog
								.openError(
										getSite().getShell(),
										RepositoryViewUITexts.RepositoriesView_Error_WindowTitle,
										e1.getMessage());
					}

				}

			});
		}

		// for Repository: import existing projects, remove, (delete), open
		// properties
		if (node.getType() == RepositoryTreeNodeType.REPO) {

			final Repository repo = (Repository) node.getObject();

			MenuItem importProjects = new MenuItem(men, SWT.PUSH);
			importProjects
					.setText(RepositoryViewUITexts.RepositoriesView_ImportExistingProjects_MenuItem);
			importProjects.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					Wizard wiz = new ExternalProjectImportWizard(repo
							.getWorkDir().getAbsolutePath()) {

						@Override
						public void addPages() {
							super.addPages();
							// we could add some page with a single
						}

						@Override
						public boolean performFinish() {

							final Set<IPath> previousLocations = new HashSet<IPath>();
							// we want to share only new projects
							for (IProject project : ResourcesPlugin
									.getWorkspace().getRoot().getProjects()) {
								previousLocations.add(project.getLocation());
							}

							boolean success = super.performFinish();
							if (success) {
								// IWizardPage page = getPage("Share");
								// TODO evaluate checkbox or such, but
								// if we do share
								// always, we don't even need another
								// page

								IWorkspaceRunnable wsr = new IWorkspaceRunnable() {

									public void run(IProgressMonitor monitor)
											throws CoreException {
										File gitDir = repo.getDirectory();
										File gitWorkDir = repo.getWorkDir();
										Path workPath = new Path(gitWorkDir
												.getAbsolutePath());

										// we check which projects are
										// in the workspace
										// pointing to a location in the
										// repo's
										// working directory
										// and share them
										for (IProject prj : ResourcesPlugin
												.getWorkspace().getRoot()
												.getProjects()) {

											if (workPath.isPrefixOf(prj
													.getLocation())) {
												if (previousLocations
														.contains(prj
																.getLocation())) {
													continue;
												}
												ConnectProviderOperation connectProviderOperation = new ConnectProviderOperation(
														prj, gitDir);
												connectProviderOperation
														.run(new SubProgressMonitor(
																monitor, 20));

											}
										}

									}
								};

								try {
									ResourcesPlugin.getWorkspace().run(
											wsr,
											ResourcesPlugin.getWorkspace()
													.getRoot(),
											IWorkspace.AVOID_UPDATE,
											new NullProgressMonitor());
									scheduleRefresh();
								} catch (CoreException ce) {
									MessageDialog
											.openError(
													getShell(),
													RepositoryViewUITexts.RepositoriesView_Error_WindowTitle,
													ce.getMessage());
								}

							}
							return success;
						}

					};

					WizardDialog dlg = new WizardDialog(getSite().getShell(),
							wiz);
					dlg.open();
				}

			});

			// TODO "import existing plug-in" menu item
			// TODO "configure" menu item

			MenuItem remove = new MenuItem(men, SWT.PUSH);
			remove
					.setText(RepositoryViewUITexts.RepositoriesView_Remove_MenuItem);
			remove.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {

					List<IProject> projectsToDelete = new ArrayList<IProject>();
					File workDir = repo.getWorkDir();
					final IPath wdPath = new Path(workDir.getAbsolutePath());
					for (IProject prj : ResourcesPlugin.getWorkspace()
							.getRoot().getProjects()) {
						if (wdPath.isPrefixOf(prj.getLocation())) {
							projectsToDelete.add(prj);
						}
					}

					if (!projectsToDelete.isEmpty()) {
						boolean confirmed;
						confirmed = confirmProjectDeletion(projectsToDelete);
						if (!confirmed) {
							return;
						}
					}

					IWorkspaceRunnable wsr = new IWorkspaceRunnable() {

						public void run(IProgressMonitor monitor)
								throws CoreException {

							for (IProject prj : ResourcesPlugin.getWorkspace()
									.getRoot().getProjects()) {
								if (wdPath.isPrefixOf(prj.getLocation())) {
									prj.delete(false, false, monitor);
								}
							}

							removeDir(repo.getDirectory().getAbsolutePath());
							scheduleRefresh();
						}
					};

					try {
						ResourcesPlugin.getWorkspace().run(wsr,
								ResourcesPlugin.getWorkspace().getRoot(),
								IWorkspace.AVOID_UPDATE,
								new NullProgressMonitor());
					} catch (CoreException e1) {
						Activator.getDefault().getLog().log(e1.getStatus());
					}

				}

			});

			// TODO delete does not work because of file locks on .pack-files
			// Shawn Pearce has added the following thoughts:

			// Hmm. We probably can't active detect file locks on pack files on
			// Windows, can we?
			// It would be nice if we could support a delete, but only if the
			// repository is
			// reasonably believed to be not-in-use right now.
			//
			// Within EGit you might be able to check GitProjectData and its
			// repositoryCache to
			// see if the repository is open by this workspace. If it is, then
			// we know we shouldn't
			// try to delete it.
			//
			// Some coding might look like this:
			//
			// MenuItem deleteRepo = new MenuItem(men, SWT.PUSH);
			// deleteRepo.setText("Delete");
			// deleteRepo.addSelectionListener(new SelectionAdapter() {
			//
			// @Override
			// public void widgetSelected(SelectionEvent e) {
			//
			// boolean confirmed = MessageDialog.openConfirm(getSite()
			// .getShell(), "Confirm",
			// "This will delete the repository, continue?");
			//
			// if (!confirmed)
			// return;
			//
			// IWorkspaceRunnable wsr = new IWorkspaceRunnable() {
			//
			// public void run(IProgressMonitor monitor)
			// throws CoreException {
			// File workDir = repos.get(0).getRepository()
			// .getWorkDir();
			//
			// File gitDir = repos.get(0).getRepository()
			// .getDirectory();
			//
			// IPath wdPath = new Path(workDir.getAbsolutePath());
			// for (IProject prj : ResourcesPlugin.getWorkspace()
			// .getRoot().getProjects()) {
			// if (wdPath.isPrefixOf(prj.getLocation())) {
			// prj.delete(false, false, monitor);
			// }
			// }
			//
			// repos.get(0).getRepository().close();
			//
			// boolean deleted = deleteRecursively(gitDir, monitor);
			// if (!deleted) {
			// MessageDialog.openError(getSite().getShell(),
			// "Error",
			// "Could not delete Git Repository");
			// }
			//
			// deleted = deleteRecursively(workDir, monitor);
			// if (!deleted) {
			// MessageDialog
			// .openError(getSite().getShell(),
			// "Error",
			// "Could not delete Git Working Directory");
			// }
			//
			// scheduleRefresh();
			// }
			//
			// private boolean deleteRecursively(File fileToDelete,
			// IProgressMonitor monitor) {
			// if (fileToDelete.isDirectory()) {
			// for (File file : fileToDelete.listFiles()) {
			// if (!deleteRecursively(file, monitor)) {
			// return false;
			// }
			// }
			// }
			// monitor.setTaskName(fileToDelete.getAbsolutePath());
			// boolean deleted = fileToDelete.delete();
			// if (!deleted) {
			// System.err.println("Could not delete "
			// + fileToDelete.getAbsolutePath());
			// }
			// return deleted;
			// }
			// };
			//
			// try {
			// ResourcesPlugin.getWorkspace().run(wsr,
			// ResourcesPlugin.getWorkspace().getRoot(),
			// IWorkspace.AVOID_UPDATE,
			// new NullProgressMonitor());
			// } catch (CoreException e1) {
			// // TODO Exception handling
			// e1.printStackTrace();
			// }
			//
			// }
			//
			// });

			MenuItem openPropsView = new MenuItem(men, SWT.PUSH);
			openPropsView
					.setText(RepositoryViewUITexts.RepositoriesView_OpenPropertiesMenu);
			openPropsView.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					try {
						PlatformUI.getWorkbench().getActiveWorkbenchWindow()
								.getActivePage().showView(
										IPageLayout.ID_PROP_SHEET);
					} catch (PartInitException e1) {
						// just ignore
					}
				}

			});
		}

		if (node.getType() == RepositoryTreeNodeType.REMOTES) {

			MenuItem remoteConfig = new MenuItem(men, SWT.PUSH);
			remoteConfig
					.setText(RepositoryViewUITexts.RepositoriesView_NewRemoteMenu);
			remoteConfig.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					new WizardDialog(getSite().getShell(),
							new ConfigureRemoteWizard(node.getRepository()))
							.open();
					scheduleRefresh();

				}

			});
		}

		if (node.getType() == RepositoryTreeNodeType.REMOTE) {

			final String name = (String) node.getObject();

			MenuItem configureUrlFetch = new MenuItem(men, SWT.PUSH);
			configureUrlFetch
					.setText(RepositoryViewUITexts.RepositoriesView_ConfigureFetchMenu);
			configureUrlFetch.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {

					new WizardDialog(getSite().getShell(),
							new ConfigureRemoteWizard(node.getRepository(),
									name, false)).open();
					scheduleRefresh();

				}

			});

			MenuItem configureUrlPush = new MenuItem(men, SWT.PUSH);
			configureUrlPush
					.setText(RepositoryViewUITexts.RepositoriesView_ConfigurePushMenu);
			configureUrlPush.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {

					new WizardDialog(getSite().getShell(),
							new ConfigureRemoteWizard(node.getRepository(),
									name, true)).open();
					scheduleRefresh();

				}

			});

			new MenuItem(men, SWT.SEPARATOR);

			MenuItem removeRemote = new MenuItem(men, SWT.PUSH);
			removeRemote
					.setText(RepositoryViewUITexts.RepositoriesView_RemoveRemoteMenu);
			removeRemote.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {

					boolean ok = MessageDialog
							.openConfirm(
									getSite().getShell(),
									RepositoryViewUITexts.RepositoriesView_ConfirmDeleteRemoteHeader,
									NLS
											.bind(
													RepositoryViewUITexts.RepositoriesView_ConfirmDeleteRemoteMessage,
													name));
					if (ok) {
						RepositoryConfig config = node.getRepository()
								.getConfig();
						config.unsetSection(REMOTE, name);
						try {
							config.save();
							scheduleRefresh();
						} catch (IOException e1) {
							MessageDialog
									.openError(
											getSite().getShell(),
											RepositoryViewUITexts.RepositoriesView_ErrorHeader,
											e1.getMessage());
						}
					}

				}

			});

			new MenuItem(men, SWT.SEPARATOR);

			MenuItem openPropsView = new MenuItem(men, SWT.PUSH);
			openPropsView
					.setText(RepositoryViewUITexts.RepositoriesView_OpenPropertiesMenu);
			openPropsView.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					try {
						PlatformUI.getWorkbench().getActiveWorkbenchWindow()
								.getActivePage().showView(
										IPageLayout.ID_PROP_SHEET);
					} catch (PartInitException e1) {
						// just ignore
					}
				}

			});
		}
	}

	private void addActionsToToolbar() {
		importAction = new Action(
				RepositoryViewUITexts.RepositoriesView_Import_Button) {

			@Override
			public void run() {
				GitCloneWizard wiz = new GitCloneWizard();
				wiz.init(null, null);
				new WizardDialog(getSite().getShell(), wiz).open();
				updateDirStrings(new NullProgressMonitor());
			}
		};
		importAction
				.setToolTipText(RepositoryViewUITexts.RepositoriesView_Clone_Tooltip);

		importAction.setImageDescriptor(UIIcons.IMPORT);

		getViewSite().getActionBars().getToolBarManager().add(importAction);

		addAction = new Action(
				RepositoryViewUITexts.RepositoriesView_Add_Button) {

			@Override
			public void run() {
				RepositorySearchDialog sd = new RepositorySearchDialog(
						getSite().getShell(), ResourcesPlugin.getWorkspace()
								.getRoot().getLocation().toOSString(),
						getGitDirs());
				if (sd.open() == Window.OK) {
					Set<String> dirs = new HashSet<String>();
					dirs.addAll(getGitDirs());
					if (dirs.addAll(sd.getDirectories()))
						saveDirStrings(dirs);
					scheduleRefresh();
				}

			}
		};
		addAction
				.setToolTipText(RepositoryViewUITexts.RepositoriesView_AddRepository_Tooltip);

		addAction.setImageDescriptor(UIIcons.NEW_REPOSITORY);

		getViewSite().getActionBars().getToolBarManager().add(addAction);

		// TODO if we don't show projects, then we probably don't need refresh

		refreshAction = new Action(
				RepositoryViewUITexts.RepositoriesView_Refresh_Button) {

			@Override
			public void run() {
				scheduleRefresh();
			}
		};

		refreshAction.setImageDescriptor(UIIcons.ELCL16_REFRESH);

		getViewSite().getActionBars().getToolBarManager().add(refreshAction);
	}

	@Override
	public void dispose() {
		// make sure to cancel the refresh job
		if (this.scheduledJob != null) {
			this.scheduledJob.cancel();
			this.scheduledJob = null;
		}
		super.dispose();
	}

	private void scheduleRefresh() {

		Job job = new Job("Refreshing Git Repositories view") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				final List<Repository> input;
				try {
					input = getRepositoriesFromDirs(monitor);
				} catch (InterruptedException e) {
					return new Status(IStatus.ERROR, Activator.getPluginId(), e
							.getMessage(), e);
				}

				Display.getDefault().syncExec(new Runnable() {

					public void run() {
						// keep expansion state and selection so that we can
						// restore the tree
						// after update
						Object[] expanded = tv.getExpandedElements();
						IStructuredSelection sel = (IStructuredSelection) tv
								.getSelection();
						tv.setInput(input);
						tv.setExpandedElements(expanded);

						Object selected = sel.getFirstElement();
						if (selected != null)
							tv.reveal(selected);

						IViewPart part = PlatformUI.getWorkbench()
								.getActiveWorkbenchWindow().getActivePage()
								.findView(IPageLayout.ID_PROP_SHEET);
						if (part != null) {
							PropertySheet sheet = (PropertySheet) part;
							PropertySheetPage page = (PropertySheetPage) sheet
									.getCurrentPage();
							page.refresh();
						}
					}
				});

				return new Status(IStatus.OK, Activator.getPluginId(), ""); //$NON-NLS-1$

			}

		};
		job.setSystem(true);

		IWorkbenchSiteProgressService service = (IWorkbenchSiteProgressService) getSite()
				.getService(IWorkbenchSiteProgressService.class);

		service.schedule(job);

		scheduledJob = job;

	}

	private List<Repository> getRepositoriesFromDirs(IProgressMonitor monitor)
			throws InterruptedException {

		List<String> gitDirStrings = getGitDirs();
		List<Repository> input = new ArrayList<Repository>();
		for (String dirString : gitDirStrings) {
			if (monitor.isCanceled()) {
				throw new InterruptedException(
						RepositoryViewUITexts.RepositoriesView_ActionCanceled_Message);
			}
			try {
				File dir = new File(dirString);
				if (dir.exists() && dir.isDirectory()) {
					input.add(new Repository(dir));
				}
			} catch (IOException e) {
				IStatus error = new Status(IStatus.ERROR, Activator
						.getPluginId(), e.getMessage(), e);
				Activator.getDefault().getLog().log(error);
			}
		}
		return input;
	}

	private void updateDirStrings(IProgressMonitor monitor) {

		IPath path = ResourcesPlugin.getWorkspace().getRoot().getLocation();
		File root = path.toFile();
		TreeSet<String> dirStrings = new TreeSet<String>();
		recurseDir(root, dirStrings, monitor);
		saveDirStrings(dirStrings);
		scheduleRefresh();

	}

	private void saveDirStrings(Set<String> gitDirStrings) {
		StringBuilder sb = new StringBuilder();
		for (String gitDirString : gitDirStrings) {
			sb.append(gitDirString);
			sb.append(File.pathSeparatorChar);
		}

		IEclipsePreferences prefs = new InstanceScope().getNode(Activator
				.getPluginId());
		prefs.put(PREFS_DIRECTORIES, sb.toString());
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			IStatus error = new Status(IStatus.ERROR, Activator.getPluginId(),
					e.getMessage(), e);
			Activator.getDefault().getLog().log(error);
		}
	}

	/**
	 *
	 * @param root
	 * @param strings
	 * @param monitor
	 */
	public static void recurseDir(File root, TreeSet<String> strings,
			IProgressMonitor monitor) {

		if (!root.exists() || !root.isDirectory()) {
			return;
		}
		File[] children = root.listFiles();
		for (File child : children) {
			if (monitor.isCanceled()) {
				return;
			}

			if (child.exists() && child.isDirectory()
					&& RepositoryCache.FileKey.isGitRepository(child)) {
				strings.add(child.getAbsolutePath());
				return;
			}
			if (child.isDirectory()) {
				monitor.setTaskName(child.getPath());
				recurseDir(child, strings, monitor);
			}
		}

	}

	private static String getRepositoryName(Repository repository) {
		return repository.getDirectory().getParentFile().getName();
	}

	@Override
	public void setFocus() {
		// nothing special
	}

	@SuppressWarnings("boxing")
	private boolean confirmProjectDeletion(List<IProject> projectsToDelete) {
		boolean confirmed;
		confirmed = MessageDialog
				.openConfirm(
						getSite().getShell(),
						RepositoryViewUITexts.RepositoriesView_ConfirmProjectDeletion_WindowTitle,
						NLS
								.bind(
										RepositoryViewUITexts.RepositoriesView_ConfirmProjectDeletion_Question,
										projectsToDelete.size()));
		return confirmed;
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionListeners.add(listener);
	}

	public ISelection getSelection() {
		return currentSelection;
	}

	public void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		selectionListeners.remove(listener);

	}

	public void setSelection(ISelection selection) {
		currentSelection = selection;
		for (ISelectionChangedListener listener : selectionListeners) {
			listener.selectionChanged(new SelectionChangedEvent(
					RepositoriesView.this, selection));
		}
	}

	// private void showExceptionMessage(Exception e) {
	// MessageDialog.openError(getSite().getShell(), "Error", e.getMessage());
	// }
}
