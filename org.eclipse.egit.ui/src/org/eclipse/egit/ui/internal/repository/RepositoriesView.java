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
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.clone.GitCloneWizard;
import org.eclipse.egit.ui.internal.repository.RepositoryTreeNode.RepositoryTreeNodeType;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryConfig;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
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
 * Implements {@link ISelectionProvider} in order to integrate with the
 * Properties view.
 * <p>
 * TODO
 * <li>Clarification whether to show projects, perhaps configurable switch</li>
 *
 */
public class RepositoriesView extends ViewPart implements ISelectionProvider {

	/** The view ID */
	public static final String VIEW_ID = "org.eclipse.egit.ui.RepositoriesView"; //$NON-NLS-1$

	// TODO central constants? RemoteConfig ones are private
	static final String REMOTE = "remote"; //$NON-NLS-1$

	static final String URL = "url"; //$NON-NLS-1$

	static final String PUSHURL = "pushurl"; //$NON-NLS-1$

	static final String FETCH = "fetch"; //$NON-NLS-1$

	static final String PUSH = "push"; //$NON-NLS-1$

	private static final String PREFS_DIRECTORIES = "GitRepositoriesView.GitDirectories"; //$NON-NLS-1$

	private static final String PREFS_SYNCED = "GitRepositoriesView.SyncWithSelection"; //$NON-NLS-1$

	private final List<ISelectionChangedListener> selectionListeners = new ArrayList<ISelectionChangedListener>();

	private ISelection currentSelection = new StructuredSelection();

	private Job scheduledJob;

	private TreeViewer tv;

	private IAction importAction;

	private IAction addAction;

	private IAction refreshAction;

	private IAction linkWithSelectionAction;

	private static List<String> getDirs() {
		List<String> resultStrings = new ArrayList<String>();
		String dirs = getPrefs().get(PREFS_DIRECTORIES, ""); //$NON-NLS-1$
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

	private static void removeDir(File file) {

		String dir;
		try {
			dir = file.getCanonicalPath();
		} catch (IOException e1) {
			dir = file.getAbsolutePath();
		}

		IEclipsePreferences prefs = getPrefs();

		TreeSet<String> resultStrings = new TreeSet<String>();
		String dirs = prefs.get(PREFS_DIRECTORIES, ""); //$NON-NLS-1$
		if (dirs != null && dirs.length() > 0) {
			StringTokenizer tok = new StringTokenizer(dirs, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				String dirName = tok.nextToken();
				File testFile = new File(dirName);
				if (testFile.exists()) {
					try {
						resultStrings.add(testFile.getCanonicalPath());
					} catch (IOException e) {
						resultStrings.add(testFile.getAbsolutePath());
					}
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

	@Override
	public void createPartControl(Composite parent) {
		tv = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		tv.setContentProvider(new RepositoriesViewContentProvider());
		// the label provider registers itself
		new RepositoriesViewLabelProvider(tv);

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

		addContextMenu();

		addActionsToToolbar();

		scheduleRefresh();

		ISelectionService srv = (ISelectionService) getSite().getService(
				ISelectionService.class);
		srv.addPostSelectionListener(new ISelectionListener() {

			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {

				// if the "link with selection" toggle is off, we're done
				if (linkWithSelectionAction == null
						|| !linkWithSelectionAction.isChecked())
					return;

				// this may happen if we switch between editors
				if (part instanceof IEditorPart) {
					IEditorInput input = ((IEditorPart) part).getEditorInput();
					if (input instanceof IFileEditorInput)
						reactOnSelection(new StructuredSelection(
								((IFileEditorInput) input).getFile()));

				} else {
					reactOnSelection(selection);
				}
			}

		});
	}

	private void reactOnSelection(ISelection selection) {
		if (selection instanceof StructuredSelection) {
			StructuredSelection ssel = (StructuredSelection) selection;
			if (ssel.size() != 1)
				return;
			if (ssel.getFirstElement() instanceof IResource) {
				showResource((IResource) ssel.getFirstElement());
			}
			if (ssel.getFirstElement() instanceof IAdaptable) {
				IResource adapted = (IResource) ((IAdaptable) ssel
						.getFirstElement()).getAdapter(IResource.class);
				if (adapted != null)
					showResource(adapted);
			}
		}
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
		importItem.setText(UIText.RepositoriesView_ImportRepository_MenuItem);
		importItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				importAction.run();
			}

		});

		MenuItem addItem = new MenuItem(men, SWT.PUSH);
		addItem.setText(UIText.RepositoriesView_AddRepository_MenuItem);
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
			sync.setText(UIText.RepositoriesView_ImportProject_MenuItem);

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
			checkout.setText(UIText.RepositoriesView_CheckOut_MenuItem);
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
						MessageDialog.openError(getSite().getShell(),
								UIText.RepositoriesView_Error_WindowTitle, e1
										.getMessage());
					}

				}

			});
		}

		// for Repository: import existing projects, remove, (delete), open
		// properties
		if (node.getType() == RepositoryTreeNodeType.REPO) {

			final Repository repo = (Repository) node.getObject();

			// TODO "import existing plug-in" menu item

			MenuItem remove = new MenuItem(men, SWT.PUSH);
			remove.setText(UIText.RepositoriesView_Remove_MenuItem);
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

							removeDir(repo.getDirectory());
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

			new MenuItem(men, SWT.SEPARATOR);

			MenuItem openPropsView = new MenuItem(men, SWT.PUSH);
			openPropsView.setText(UIText.RepositoriesView_OpenPropertiesMenu);
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
			remoteConfig.setText(UIText.RepositoriesView_NewRemoteMenu);
			remoteConfig.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					WizardDialog dialog = new WizardDialog(
							getSite().getShell(), new ConfigureRemoteWizard(
									node.getRepository()));
					if (dialog.open() == Window.OK) {
						scheduleRefresh();
					}
				}

			});
		}

		if (node.getType() == RepositoryTreeNodeType.REMOTE) {

			final String name = (String) node.getObject();

			MenuItem configureUrlFetch = new MenuItem(men, SWT.PUSH);
			configureUrlFetch
					.setText(UIText.RepositoriesView_ConfigureFetchMenu);
			configureUrlFetch.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					WizardDialog dialog = new WizardDialog(
							getSite().getShell(), new ConfigureRemoteWizard(
									node.getRepository(), name, false));
					if (dialog.open() == Window.OK) {
						scheduleRefresh();
					}
				}

			});

			MenuItem configureUrlPush = new MenuItem(men, SWT.PUSH);
			configureUrlPush.setText(UIText.RepositoriesView_ConfigurePushMenu);
			configureUrlPush.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					WizardDialog dialog = new WizardDialog(
							getSite().getShell(), new ConfigureRemoteWizard(
									node.getRepository(), name, true));
					if (dialog.open() == Window.OK) {
						scheduleRefresh();
					}
				}

			});

			new MenuItem(men, SWT.SEPARATOR);

			MenuItem removeRemote = new MenuItem(men, SWT.PUSH);
			removeRemote.setText(UIText.RepositoriesView_RemoveRemoteMenu);
			removeRemote.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {

					boolean ok = MessageDialog
							.openConfirm(
									getSite().getShell(),
									UIText.RepositoriesView_ConfirmDeleteRemoteHeader,
									NLS
											.bind(
													UIText.RepositoriesView_ConfirmDeleteRemoteMessage,
													name));
					if (ok) {
						RepositoryConfig config = node.getRepository()
								.getConfig();
						config.unsetSection(REMOTE, name);
						try {
							config.save();
							scheduleRefresh();
						} catch (IOException e1) {
							MessageDialog.openError(getSite().getShell(),
									UIText.RepositoriesView_ErrorHeader, e1
											.getMessage());
						}
					}

				}

			});

			new MenuItem(men, SWT.SEPARATOR);

			MenuItem openPropsView = new MenuItem(men, SWT.PUSH);
			openPropsView.setText(UIText.RepositoriesView_OpenPropertiesMenu);
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

		if (node.getType() == RepositoryTreeNodeType.FILE) {

			final File file = (File) node.getObject();

			MenuItem openInTextEditor = new MenuItem(men, SWT.PUSH);
			openInTextEditor
					.setText(UIText.RepositoriesView_OpenInTextEditor_menu);
			openInTextEditor.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					IFileStore store = EFS.getLocalFileSystem().getStore(
							new Path(file.getAbsolutePath()));
					try {
						// TODO do we need a read-only editor here?
						IDE.openEditor(getSite().getPage(),
								new FileStoreEditorInput(store),
								EditorsUI.DEFAULT_TEXT_EDITOR_ID);

					} catch (PartInitException e1) {
						MessageDialog.openError(getSite().getShell(),
								UIText.RepositoriesView_Error_WindowTitle, e1
										.getMessage());
					}

				}

			});
		}

		if (node.getType() == RepositoryTreeNodeType.FOLDER) {
			String path = ((File) node.getObject()).getAbsolutePath();
			createImportProjectItem(men, node.getRepository(), path);
		}

		if (node.getType() == RepositoryTreeNodeType.WORKINGDIR) {
			String path = node.getRepository().getWorkDir().getAbsolutePath();
			createImportProjectItem(men, node.getRepository(), path);
		}

	}

	private void createImportProjectItem(Menu men, final Repository repo,
			final String path) {
		MenuItem importProjects;
		importProjects = new MenuItem(men, SWT.PUSH);
		importProjects
				.setText(UIText.RepositoriesView_ImportExistingProjects_MenuItem);
		importProjects.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				// TODO the ExternalProjectImportWizard
				// does not allow to set a path in 3.4
				// use the GitCloneWizard page in a new
				// GitImportWizard instead
				Wizard wiz = new ExternalProjectImportWizard() {

					@Override
					public void addPages() {
						super.addPages();
						// we could add some page with a single
					}

					@Override
					public boolean performFinish() {

						final Set<IPath> previousLocations = new HashSet<IPath>();
						// we want to share only new projects
						for (IProject project : ResourcesPlugin.getWorkspace()
								.getRoot().getProjects()) {
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
											if (previousLocations.contains(prj
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
												UIText.RepositoriesView_Error_WindowTitle,
												ce.getMessage());
							}

						}
						return success;
					}

				};

				WizardDialog dlg = new WizardDialog(getSite().getShell(), wiz);
				dlg.open();
			}

		});
	}

	private void addActionsToToolbar() {
		importAction = new Action(UIText.RepositoriesView_Import_Button) {

			@Override
			public void run() {
				GitCloneWizard wiz = new GitCloneWizard();
				wiz.init(null, null);
				new WizardDialog(getSite().getShell(), wiz).open();
			}
		};
		importAction.setToolTipText(UIText.RepositoriesView_Clone_Tooltip);

		importAction.setImageDescriptor(UIIcons.IMPORT);

		getViewSite().getActionBars().getToolBarManager().add(importAction);

		addAction = new Action(UIText.RepositoriesView_Add_Button) {

			@Override
			public void run() {
				RepositorySearchDialog sd = new RepositorySearchDialog(
						getSite().getShell(), getDirs());
				if (sd.open() == Window.OK) {
					Set<String> dirs = new HashSet<String>();
					dirs.addAll(getDirs());
					if (dirs.addAll(sd.getDirectories()))
						saveDirs(dirs);
					scheduleRefresh();
				}

			}
		};
		addAction.setToolTipText(UIText.RepositoriesView_AddRepository_Tooltip);

		addAction.setImageDescriptor(UIIcons.NEW_REPOSITORY);

		getViewSite().getActionBars().getToolBarManager().add(addAction);

		linkWithSelectionAction = new Action(
				UIText.RepositoriesView_LinkWithSelection_action,
				IAction.AS_CHECK_BOX) {

			@Override
			public void run() {
				IEclipsePreferences prefs = getPrefs();
				prefs.putBoolean(PREFS_SYNCED, isChecked());
				try {
					prefs.flush();
				} catch (BackingStoreException e) {
					// ignore here
				}
				if (isChecked()) {
					ISelectionService srv = (ISelectionService) getSite()
							.getService(ISelectionService.class);
					reactOnSelection(srv.getSelection());
				}

			}

		};

		linkWithSelectionAction
				.setToolTipText(UIText.RepositoriesView_LinkWithSelection_action);

		linkWithSelectionAction.setImageDescriptor(UIIcons.ELCL16_SYNCED);

		linkWithSelectionAction.setChecked(getPrefs().getBoolean(PREFS_SYNCED,
				false));

		getViewSite().getActionBars().getToolBarManager().add(
				linkWithSelectionAction);

		refreshAction = new Action(UIText.RepositoriesView_Refresh_Button) {

			@Override
			public void run() {
				scheduleRefresh();
			}
		};

		refreshAction.setImageDescriptor(UIIcons.ELCL16_REFRESH);

		getViewSite().getActionBars().getToolBarManager().add(refreshAction);
	}

	/**
	 * @return the preferences
	 */
	protected static IEclipsePreferences getPrefs() {
		return new InstanceScope().getNode(Activator.getPluginId());
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

	/**
	 * Schedules a refreh
	 */
	public void scheduleRefresh() {

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

		List<String> gitDirStrings = getDirs();
		List<Repository> input = new ArrayList<Repository>();
		for (String dirString : gitDirStrings) {
			if (monitor.isCanceled()) {
				throw new InterruptedException(
						UIText.RepositoriesView_ActionCanceled_Message);
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

	/**
	 * Adds a directory to the list if it is not already there
	 *
	 * @param file
	 * @return see {@link Collection#add(Object)}
	 */
	public static boolean addDir(File file) {

		String dirString;
		try {
			dirString = file.getCanonicalPath();
		} catch (IOException e) {
			dirString = file.getAbsolutePath();
		}

		List<String> dirStrings = getDirs();
		if (dirStrings.contains(dirString)) {
			return false;
		} else {
			Set<String> dirs = new HashSet<String>();
			dirs.addAll(dirStrings);
			dirs.add(dirString);
			saveDirs(dirs);
			return true;
		}
	}

	private static void saveDirs(Set<String> gitDirStrings) {
		StringBuilder sb = new StringBuilder();
		for (String gitDirString : gitDirStrings) {
			sb.append(gitDirString);
			sb.append(File.pathSeparatorChar);
		}

		IEclipsePreferences prefs = getPrefs();
		prefs.put(PREFS_DIRECTORIES, sb.toString());
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			IStatus error = new Status(IStatus.ERROR, Activator.getPluginId(),
					e.getMessage(), e);
			Activator.getDefault().getLog().log(error);
		}
	}

	@Override
	public void setFocus() {
		tv.getTree().setFocus();
	}

	@SuppressWarnings("boxing")
	private boolean confirmProjectDeletion(List<IProject> projectsToDelete) {
		boolean confirmed;
		confirmed = MessageDialog
				.openConfirm(
						getSite().getShell(),
						UIText.RepositoriesView_ConfirmProjectDeletion_WindowTitle,
						NLS
								.bind(
										UIText.RepositoriesView_ConfirmProjectDeletion_Question,
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

	/**
	 * Opens the tree and marks the folder to which a project is pointing
	 *
	 * @param resource
	 *            TODO exceptions?
	 */
	@SuppressWarnings("unchecked")
	public void showResource(final IResource resource) {
		IProject project = resource.getProject();
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		if (mapping == null)
			return;

		if (addDir(mapping.getRepository().getDirectory())) {
			scheduleRefresh();
		}

		boolean doSetSelection = false;

		if (this.scheduledJob != null) {
			int state = this.scheduledJob.getState();
			if (state == Job.WAITING || state == Job.RUNNING) {
				this.scheduledJob.addJobChangeListener(new JobChangeAdapter() {

					@Override
					public void done(IJobChangeEvent event) {
						showResource(resource);
					}
				});
			} else {
				doSetSelection = true;
			}
		}

		if (doSetSelection) {
			RepositoriesViewContentProvider cp = (RepositoriesViewContentProvider) tv
					.getContentProvider();
			RepositoryTreeNode currentNode = null;
			Object[] repos = cp.getElements(tv.getInput());
			for (Object repo : repos) {
				RepositoryTreeNode node = (RepositoryTreeNode) repo;
				// TODO equals implementation of Repository?
				if (mapping.getRepository().getDirectory().equals(
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

			IPath relPath = new Path(mapping.getRepoRelativePath(resource));

			for (String segment : relPath.segments()) {
				for (Object child : cp.getChildren(currentNode)) {
					RepositoryTreeNode<File> childNode = (RepositoryTreeNode<File>) child;
					if (childNode.getObject().getName().equals(segment)) {
						currentNode = childNode;
						break;
					}
				}
			}

			final RepositoryTreeNode selNode = currentNode;

			Display.getDefault().asyncExec(new Runnable() {

				public void run() {
					tv.setSelection(new StructuredSelection(selNode), true);
				}
			});

		}

	}

}
