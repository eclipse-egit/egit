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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.clone.GitCloneWizard;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
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
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.statushandlers.StatusManager;
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
 * <li>Exception handling</li>
 * <li>Icons</li>
 * <li>String externalization</li>
 * <li>Clarification whether to show projects, perhaps configurable switch</li>
 *
 */
public class RepositoriesView extends ViewPart {

	private final static String PREFS_DIRECTORIES = "GitRepositoriesView.GitDirectories"; //$NON-NLS-1$

	private Job scheduledJob;

	private TreeViewer tv;

	private IAction importAction;

	private IAction addAction;

	private IAction refreshAction;

	enum RepositoryTreeNodeType {
		// TODO add support for type icons here
		REPO, REF, PROJ, BRANCHES, PROJECTS;
	}

	private static final class RepositoryTreeNode<T> {

		private final Repository myRepository;

		private final T myObject;

		private final RepositoryTreeNodeType myType;

		private final RepositoryTreeNode myParent;

		public RepositoryTreeNode(RepositoryTreeNode parent,
				RepositoryTreeNodeType type, Repository repository, T treeObject) {
			myParent = parent;
			myRepository = repository;
			myType = type;
			myObject = treeObject;
		}

		public RepositoryTreeNode getParent() {
			return myParent;
		}

		public RepositoryTreeNodeType getType() {
			return myType;
		}

		public Repository getRepository() {
			return myRepository;
		}

		public T getObject() {
			return myObject;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			switch (myType) {
			case REPO:
			case PROJECTS:
			case BRANCHES:
				result = prime
						* result
						+ ((myObject == null) ? 0 : ((Repository) myObject)
								.getDirectory().hashCode());
				break;
			case REF:
				result = prime
						* result
						+ ((myObject == null) ? 0 : ((Ref) myObject).getName()
								.hashCode());
				break;
			case PROJ:
				result = prime
						* result
						+ ((myObject == null) ? 0 : ((File) myObject).getPath()
								.hashCode());
				break;

			default:
				break;
			}

			result = prime * result
					+ ((myParent == null) ? 0 : myParent.hashCode());
			result = prime
					* result
					+ ((myRepository == null) ? 0 : myRepository.getDirectory()
							.hashCode());
			result = prime * result
					+ ((myType == null) ? 0 : myType.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;

			RepositoryTreeNode other = (RepositoryTreeNode) obj;

			if (myType == null) {
				if (other.myType != null)
					return false;
			} else if (!myType.equals(other.myType))
				return false;
			if (myParent == null) {
				if (other.myParent != null)
					return false;
			} else if (!myParent.equals(other.myParent))
				return false;
			if (myRepository == null) {
				if (other.myRepository != null)
					return false;
			} else if (!myRepository.getDirectory().equals(
					other.myRepository.getDirectory()))
				return false;
			if (myObject == null) {
				if (other.myObject != null)
					return false;
			} else if (!checkObjectsEqual(other.myObject))
				return false;

			return true;
		}

		private boolean checkObjectsEqual(Object otherObject) {
			switch (myType) {
			case REPO:
			case PROJECTS:
			case BRANCHES:
				return ((Repository) myObject).getDirectory().equals(
						((Repository) otherObject).getDirectory());
			case REF:
				return ((Ref) myObject).getName().equals(
						((Ref) otherObject).getName());
			case PROJ:
				return ((File) myObject).getPath().equals(
						((File) otherObject).getPath());
			default:
				return false;
			}
		}
	}

	private static class ContentProvider implements ITreeContentProvider {

		@SuppressWarnings("unchecked")
		public Object[] getElements(Object inputElement) {

			Set<RepositoryTreeNode> output = new HashSet<RepositoryTreeNode>();

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

				Repository rep = node.getRepository();
				for (Map.Entry<String, Ref> ref : rep.getAllRefs().entrySet()) {
					refs.add(new RepositoryTreeNode<Ref>(node,
							RepositoryTreeNodeType.REF, repo, ref.getValue()));
				}

				return refs.toArray();

			case REPO:

				List<RepositoryTreeNode<Repository>> branches = new ArrayList<RepositoryTreeNode<Repository>>();

				branches.add(new RepositoryTreeNode<Repository>(node,
						RepositoryTreeNodeType.BRANCHES, node.getRepository(),
						node.getRepository()));

				branches.add(new RepositoryTreeNode<Repository>(node,
						RepositoryTreeNodeType.PROJECTS, node.getRepository(),
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
					UIText.WizardProjectsImportPage_CheckingMessage, directory
							.getPath()));
			File[] contents = directory.listFiles();
			if (contents == null)
				return false;

			// Initialize recursion guard for recursive symbolic links
			if (directoriesVisited == null) {
				directoriesVisited = new HashSet<String>();
				try {
					directoriesVisited.add(directory.getCanonicalPath());
				} catch (IOException exception) {
					StatusManager.getManager()
							.handle(
									new Status(IStatus.ERROR, Activator
											.getPluginId(), exception
											.getLocalizedMessage(), exception));
				}
			}

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
					if (!contents[i].getName().equals(".metadata")) {
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

	private static class LabelProvider extends BaseLabelProvider implements
			ITableLabelProvider {

		private DefaultInformationControl infoControl;

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

					Point eventPoint = new Point(e.x, e.y);

					TreeItem item = viewer.getTree().getItem(eventPoint);
					if (item != null) {

						RepositoryTreeNode node = (RepositoryTreeNode) item
								.getData();
						String text = node.getRepository().getDirectory()
								.getAbsolutePath();

						final ViewerCell cell = viewer.getCell(eventPoint);

						if (infoControl != null && infoControl.isVisible()) {
							infoControl.setVisible(false);
						}

						GC testGc = new GC(cell.getControl());
						final Point textExtent = testGc.textExtent(text);
						testGc.dispose();

						if (infoControl == null || !infoControl.isVisible()) {

							IInformationPresenter ips = new IInformationPresenter() {

								public String updatePresentation(
										Display display, String hoverInfo,
										TextPresentation presentation,
										int maxWidth, int maxHeight) {
									return hoverInfo;
								}

							};

							infoControl = new DefaultInformationControl(Display
									.getCurrent().getActiveShell().getShell(),
									ips) {

								@Override
								public void setInformation(String content) {
									super.setInformation(content);
									super.setSize(textExtent.x, textExtent.y);
								}

							};
						}

						Point dispPoint = viewer.getControl().toDisplay(
								eventPoint);

						infoControl.setLocation(dispPoint);

						// the default info provider works better with \r ...
						infoControl.setInformation(text);

						final MouseMoveListener moveListener = new MouseMoveListener() {

							public void mouseMove(MouseEvent evt) {
								infoControl.setVisible(false);
								cell.getControl().removeMouseMoveListener(this);

							}
						};

						cell.getControl().addMouseMoveListener(moveListener);

						infoControl.setVisible(true);

					}

				}

			});

		}

		public Image getColumnImage(Object element, int columnIndex) {
			// TODO use the node type to obtain the icon, e.g:
			// return ((RepositoryTreeNode)element).getType().getIcon();
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {

			RepositoryTreeNode node = (RepositoryTreeNode) element;
			switch (node.getType()) {
			case REPO:
				File directory = ((Repository) node.getObject()).getDirectory()
						.getParentFile();
				return (directory.getName() + " - " + directory
						.getAbsolutePath());
			case BRANCHES:
				return "Branches";
			case PROJECTS:
				return "Existing Projects";
			case REF:
				Ref ref = (Ref) node.getObject();
				String refName = ref.getName();
				if (ref.isSymbolic()) {
					refName = refName + " - " + ref.getLeaf().getName();
				}
				try {
					String fullBranch = node.getRepository().getFullBranch();
					if (refName.equals(fullBranch)) {
						// TODO this should of course be a decorator on an icon
						return "* " + refName;
					}
				} catch (IOException e1) {
					// TODO Exception handling
					e1.printStackTrace();
				}
				return refName;
			case PROJ:

				File file = (File) node.getObject();

				for (IProject proj : ResourcesPlugin.getWorkspace().getRoot()
						.getProjects()) {
					if (proj.getLocation().equals(
							new Path(file.getAbsolutePath()))) {
						// TODO this should of course be a decorator on an icon
						return ("* " + file.getName());
					}
				}
				return file.getName();

			default:
				return null;
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
				// TODO Exception handling
			}
		}

	}

	@Override
	public Object getAdapter(Class adapter) {
		return super.getAdapter(adapter);
	}

	@Override
	public void createPartControl(Composite parent) {

		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		main.setLayout(new GridLayout(1, false));

		tv = new TreeViewer(main);
		tv.setContentProvider(new ContentProvider());
		new LabelProvider(tv);

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
					if (men.getItemCount() > 0)
						new MenuItem(men, SWT.SEPARATOR);
					addMenuItemsForPanel(men);
				}

				tv.getTree().setMenu(men);
			}
		});
	}

	private void addMenuItemsForPanel(Menu men) {

		MenuItem importItem = new MenuItem(men, SWT.PUSH);
		importItem.setText("Import Git Repository...");
		importItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				importAction.run();
			}

		});

		MenuItem addItem = new MenuItem(men, SWT.PUSH);
		addItem.setText("Add Git Repository...");
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
		final List<RepositoryTreeNode<Ref>> refs = new ArrayList<RepositoryTreeNode<Ref>>();
		final List<RepositoryTreeNode<File>> projects = new ArrayList<RepositoryTreeNode<File>>();
		final List<RepositoryTreeNode<Repository>> repos = new ArrayList<RepositoryTreeNode<Repository>>();

		TreeItem[] selectedItems = tv.getTree().getSelection();
		for (TreeItem item : selectedItems) {
			RepositoryTreeNode node = (RepositoryTreeNode) item.getData();
			switch (node.getType()) {
			case PROJ:
				projects.add(node);
				break;
			case REF:
				refs.add(node);
				break;
			case REPO:
				repos.add(node);
				break;
			default:
				break;
			}
		}

		boolean importableProjectsOnly = !projects.isEmpty() && repos.isEmpty()
				&& refs.isEmpty();

		for (RepositoryTreeNode<File> prj : projects) {
			if (!importableProjectsOnly)
				break;

			for (IProject proj : ResourcesPlugin.getWorkspace().getRoot()
					.getProjects()) {
				if (proj.getLocation().equals(
						new Path(prj.getObject().getAbsolutePath())))
					importableProjectsOnly = false;

			}

		}

		boolean singleRef = refs.size() == 1 && projects.isEmpty()
				&& repos.isEmpty();
		boolean singleRepo = repos.size() == 1 && projects.isEmpty()
				&& refs.isEmpty();

		try {
			// TODO constant
			singleRef = singleRef
					&& !refs.get(0).getObject().getName().equals("HEAD")
					&& (refs.get(0).getRepository().mapCommit(
							refs.get(0).getObject().getLeaf().getObjectId()) != null);
		} catch (IOException e2) {
			singleRef = false;
		}

		if (importableProjectsOnly) {
			MenuItem sync = new MenuItem(men, SWT.PUSH);
			sync.setText("Import");

			sync.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {

					IWorkspaceRunnable wsr = new IWorkspaceRunnable() {

						public void run(IProgressMonitor monitor)
								throws CoreException {

							for (RepositoryTreeNode<File> projectNode : projects) {
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
						// TODO Exception handling
						e1.printStackTrace();
					}

				}

			});
		}

		if (singleRef) {

			MenuItem checkout = new MenuItem(men, SWT.PUSH);
			checkout.setText("Check out");
			checkout.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					Repository repo = refs.get(0).getRepository();
					String refName = refs.get(0).myObject.getLeaf().getName();
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
						MessageDialog.openError(getSite().getShell(), "Error",
								e1.getMessage());
					}

				}

			});
		}

		if (singleRepo) {

			MenuItem importProjects = new MenuItem(men, SWT.PUSH);
			importProjects.setText("Import Existing projects...");
			importProjects.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					Wizard wiz = new ExternalProjectImportWizard(repos.get(0)
							.getRepository().getWorkDir().getAbsolutePath()) {

						@Override
						public void addPages() {
							super.addPages();
							// we could add some page with a single
							// checkbox to indicate if we wan
							// addPage(new WizardPage("Share") {
							//
							// public void createControl(
							// Composite parent) {
							// // TODO Auto-generated method
							// // stub
							// Composite main = new Composite(
							// parent, SWT.NONE);
							// main.setLayout(new GridLayout(1,
							// false));
							// GridDataFactory.fillDefaults()
							// .grab(true, true).applyTo(
							// main);
							// Button but = new Button(main,
							// SWT.PUSH);
							// but.setText("Push me");
							// setControl(main);
							//
							// }
							// });
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
										File gitDir = repos.get(0)
												.getRepository().getDirectory();
										File gitWorkDir = repos.get(0)
												.getRepository().getWorkDir();
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
								} catch (CoreException e) {
									MessageDialog.openError(getShell(),
											"Error", e.getMessage());
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

			// TODO import existing plug-in menu item
			// TODO configure menu item

			MenuItem remove = new MenuItem(men, SWT.PUSH);
			remove.setText("Remove");
			remove.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {

					List<IProject> projectsToDelete = new ArrayList<IProject>();
					File workDir = repos.get(0).getRepository().getWorkDir();
					IPath wdPath = new Path(workDir.getAbsolutePath());
					for (IProject prj : ResourcesPlugin.getWorkspace()
							.getRoot().getProjects()) {
						if (wdPath.isPrefixOf(prj.getLocation())) {
							projectsToDelete.add(prj);
						}
					}

					if (!projectsToDelete.isEmpty()) {
						boolean confirmed = MessageDialog
								.openConfirm(
										getSite().getShell(),
										"Confirm project deletion",
										NLS
												.bind(
														"{0} projects must be deleted, continue?",
														projectsToDelete.size()));
						if (!confirmed) {
							return;
						}
					}

					IWorkspaceRunnable wsr = new IWorkspaceRunnable() {

						public void run(IProgressMonitor monitor)
								throws CoreException {

							File workDir = repos.get(0).getRepository()
									.getWorkDir();
							IPath wdPath = new Path(workDir.getAbsolutePath());
							for (IProject prj : ResourcesPlugin.getWorkspace()
									.getRoot().getProjects()) {
								if (wdPath.isPrefixOf(prj.getLocation())) {
									prj.delete(false, false, monitor);
								}
							}

							Repository repo = repos.get(0).getRepository();
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
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

				}

			});

			// TODO delete does not work because of file locks on .pack-files
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
			// // TODO Auto-generated catch block
			// e1.printStackTrace();
			// }
			//
			// }
			//
			// });
		}
	}

	private void addActionsToToolbar() {
		importAction = new Action("Import...") {

			@Override
			public void run() {
				GitCloneWizard wiz = new GitCloneWizard();
				wiz.init(null, null);
				new WizardDialog(getSite().getShell(), wiz).open();
				updateDirStrings(new NullProgressMonitor());
			}
		};
		importAction.setToolTipText("Import (clone) a Git Repository");

		getViewSite().getActionBars().getToolBarManager().add(importAction);

		addAction = new Action("Add...") {

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
		addAction.setToolTipText("Add an existing Git Repository");

		getViewSite().getActionBars().getToolBarManager().add(addAction);

		// TODO if we don't show projects, then we probably don't need refresh

		refreshAction = new Action("Refresh") {

			@Override
			public void run() {
				scheduleRefresh();
			}
		};

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

				final List<Repository> input = getRepositoriesFromDirs(monitor);

				Display.getDefault().syncExec(new Runnable() {

					public void run() {
						// keep expansion state and selection so that we can
						// restore the tree
						// after update
						Object[] expanded = tv.getExpandedElements();
						IStructuredSelection sel = (IStructuredSelection) tv
								.getSelection();
						Object selected = sel.getFirstElement();
						tv.setInput(input);
						tv.setExpandedElements(expanded);
						tv.reveal(selected);
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

	private List<Repository> getRepositoriesFromDirs(IProgressMonitor monitor) {

		List<String> gitDirStrings = getGitDirs();
		List<Repository> input = new ArrayList<Repository>();
		for (String dirString : gitDirStrings) {
			try {
				File dir = new File(dirString);
				if (dir.exists() && dir.isDirectory()) {
					input.add(new Repository(dir));
				}
			} catch (IOException e1) {
				// TODO Exception handling
				e1.printStackTrace();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
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
					&& child.getName().equals(".git")) { // TODO constant?
				strings.add(child.getAbsolutePath());
				return;
			}
			if (child.isDirectory()) {
				monitor.setTaskName(child.getPath());
				recurseDir(child, strings, monitor);
			}
		}

	}

	@Override
	public void setFocus() {
		// nothing special
	}

}
