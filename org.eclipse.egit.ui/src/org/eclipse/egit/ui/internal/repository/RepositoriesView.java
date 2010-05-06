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
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
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
import org.eclipse.egit.ui.internal.clone.GitCreateProjectViaWizardWizard;
import org.eclipse.egit.ui.internal.repository.RepositoryTreeNode.RepositoryTreeNodeType;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.lib.RepositoryConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
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
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.PropertySheetPage;
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
public class RepositoriesView extends ViewPart implements ISelectionProvider,
		IShowInTarget {

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

	private IAction copyAction;

	private IAction pasteAction;

	/**
	 * TODO move to utility class
	 *
	 * @return the directories as configured for this view
	 */
	public static List<String> getDirs() {
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

				copyAction.setEnabled(false);

				IStructuredSelection ssel = (IStructuredSelection) event
						.getSelection();
				if (ssel.size() == 1) {
					RepositoryTreeNode node = (RepositoryTreeNode) ssel
							.getFirstElement();
					// allow copy on repository, file, or folder (copying the
					// directory)
					if (node.getType() == RepositoryTreeNodeType.REPO
							|| node.getType() == RepositoryTreeNodeType.WORKINGDIR
							|| node.getType() == RepositoryTreeNodeType.FOLDER
							|| node.getType() == RepositoryTreeNodeType.FILE) {
						copyAction.setEnabled(true);
					}
					setSelection(new StructuredSelection(ssel.getFirstElement()));
				} else {
					setSelection(new StructuredSelection());
				}

			}
		});
		tv.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				if (selection.isEmpty()) {
					// nothing selected, ignore
					return;
				}

				Object element = selection.getFirstElement();
				ITreeContentProvider contentProvider = (ITreeContentProvider) tv
						.getContentProvider();
				if (contentProvider.hasChildren(element)) {
					// this element has children, expand/collapse it
					tv.setExpandedState(element, !tv.getExpandedState(element));
				} else {
					Object[] selectionArray = selection.toArray();
					for (Object selectedElement : selectionArray) {
						RepositoryTreeNode node = (RepositoryTreeNode) selectedElement;
						// if any of the selected elements are not files, ignore
						// the open request
						if (node.getType() != RepositoryTreeNodeType.FILE
								&& node.getType() != RepositoryTreeNodeType.REF) {
							return;
						}
					}

					// open the files the user has selected
					for (Object selectedElement : selectionArray) {
						RepositoryTreeNode node = (RepositoryTreeNode) selectedElement;
						if (node.getType() == RepositoryTreeNodeType.FILE)
							openFile((File) node.getObject());
						else if (node.getType() == RepositoryTreeNodeType.REF) {
							Ref ref = (Ref) node.getObject();
							if (ref.getName().startsWith(Constants.R_HEADS)
									|| ref.getName().startsWith(
											Constants.R_REMOTES))
								checkoutBranch(node, ref.getName());
						}
					}
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

		MenuItem pasteItem = new MenuItem(men, SWT.PUSH);
		pasteItem.setText(UIText.RepositoriesView_PasteMenu);
		pasteItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				pasteAction.run();
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
										.execute(new SubProgressMonitor(
												monitor, 20));

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
						Activator.logError(e1.getMessage(), e1);
					}

				}

			});
		}

		// from here on, we only deal with single selection
		if (sel.size() > 1)
			return;

		final RepositoryTreeNode node = (RepositoryTreeNode) sel
				.getFirstElement();

		if (node.getType() == RepositoryTreeNodeType.REF) {

			final Ref ref = (Ref) node.getObject();

			// we don't check out symbolic references
			if (!ref.isSymbolic()) {

				MenuItem checkout = new MenuItem(men, SWT.PUSH);
				checkout.setText(UIText.RepositoriesView_CheckOut_MenuItem);

				try {
					if (node.getRepository().getFullBranch().equals(
							ref.getName())) {
						// no checkout on current branch
						checkout.setEnabled(false);
					}
				} catch (IOException e2) {
					// ignore
				}

				checkout.addSelectionListener(new SelectionAdapter() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						checkoutBranch(node, ref.getLeaf().getName());
					}
				});

				new MenuItem(men, SWT.SEPARATOR);

				createCreateBranchItem(men, node);
				createDeleteBranchItem(men, node);

			}
		}

		if (node.getType() == RepositoryTreeNodeType.LOCALBRANCHES
				|| node.getType() == RepositoryTreeNodeType.REMOTEBRANCHES)
			createCreateBranchItem(men, node);

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
						Activator.logError(e1.getMessage(), e1);
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

			createImportProjectItem(men, repo, repo.getWorkDir().getPath());

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

			new MenuItem(men, SWT.SEPARATOR);

			createCopyPathItem(men, repo.getDirectory().getPath());
		}

		if (node.getType() == RepositoryTreeNodeType.REMOTES) {

			MenuItem remoteConfig = new MenuItem(men, SWT.PUSH);
			remoteConfig.setText(UIText.RepositoriesView_NewRemoteMenu);
			remoteConfig.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {

					WizardDialog dlg = new WizardDialog(getSite().getShell(),
							new NewRemoteWizard(node.getRepository()));
					if (dlg.open() == Window.OK)
						scheduleRefresh();

				}

			});
		}

		if (node.getType() == RepositoryTreeNodeType.REMOTE) {

			final String configName = (String) node.getObject();

			RemoteConfig rconfig;
			try {
				rconfig = new RemoteConfig(node.getRepository().getConfig(),
						configName);
			} catch (URISyntaxException e2) {
				// TODO Exception handling
				rconfig = null;
			}

			boolean fetchExists = rconfig != null
					&& !rconfig.getURIs().isEmpty();
			boolean pushExists = rconfig != null
					&& !rconfig.getPushURIs().isEmpty();

			if (!fetchExists) {
				MenuItem configureUrlFetch = new MenuItem(men, SWT.PUSH);
				configureUrlFetch
						.setText(UIText.RepositoriesView_CreateFetch_menu);

				configureUrlFetch.addSelectionListener(new SelectionAdapter() {

					@Override
					public void widgetSelected(SelectionEvent e) {

						WizardDialog dlg = new WizardDialog(getSite()
								.getShell(), new ConfigureRemoteWizard(node
								.getRepository(), configName, false));
						if (dlg.open() == Window.OK)
							scheduleRefresh();

					}

				});
			}

			if (!pushExists) {
				MenuItem configureUrlPush = new MenuItem(men, SWT.PUSH);

				configureUrlPush
						.setText(UIText.RepositoriesView_CreatePush_menu);

				configureUrlPush.addSelectionListener(new SelectionAdapter() {

					@Override
					public void widgetSelected(SelectionEvent e) {

						WizardDialog dlg = new WizardDialog(getSite()
								.getShell(), new ConfigureRemoteWizard(node
								.getRepository(), configName, true));
						if (dlg.open() == Window.OK)
							scheduleRefresh();

					}

				});
			}

			if (!fetchExists || !pushExists)
				// add a separator dynamically
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
													configName));
					if (ok) {
						RepositoryConfig config = node.getRepository()
								.getConfig();
						config.unsetSection(REMOTE, configName);
						try {
							config.save();
							scheduleRefresh();
						} catch (IOException e1) {
							Activator.handleError(
									UIText.RepositoriesView_ErrorHeader, e1,
									true);
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

		if (node.getType() == RepositoryTreeNodeType.FETCH) {

			final String configName = (String) node.getParent().getObject();

			MenuItem configureUrlFetch = new MenuItem(men, SWT.PUSH);
			configureUrlFetch
					.setText(UIText.RepositoriesView_ConfigureFetchMenu);

			configureUrlFetch.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {

					WizardDialog dlg = new WizardDialog(getSite().getShell(),
							new ConfigureRemoteWizard(node.getRepository(),
									configName, false));
					if (dlg.open() == Window.OK)
						scheduleRefresh();

				}

			});

			MenuItem deleteFetch = new MenuItem(men, SWT.PUSH);
			deleteFetch.setText(UIText.RepositoriesView_RemoveFetch_menu);
			deleteFetch.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					RepositoryConfig config = node.getRepository().getConfig();
					config.unset("remote", configName, "url"); //$NON-NLS-1$ //$NON-NLS-2$
					config.unset("remote", configName, "fetch"); //$NON-NLS-1$//$NON-NLS-2$
					try {
						config.save();
						scheduleRefresh();
					} catch (IOException e1) {
						MessageDialog.openError(getSite().getShell(),
								UIText.RepositoriesView_ErrorHeader, e1
										.getMessage());
					}
				}

			});

		}

		if (node.getType() == RepositoryTreeNodeType.PUSH) {

			final String configName = (String) node.getParent().getObject();

			MenuItem configureUrlPush = new MenuItem(men, SWT.PUSH);

			configureUrlPush.setText(UIText.RepositoriesView_ConfigurePushMenu);

			configureUrlPush.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {

					WizardDialog dlg = new WizardDialog(getSite().getShell(),
							new ConfigureRemoteWizard(node.getRepository(),
									configName, true));
					if (dlg.open() == Window.OK)
						scheduleRefresh();
				}

			});

			MenuItem deleteFetch = new MenuItem(men, SWT.PUSH);
			deleteFetch.setText(UIText.RepositoriesView_RemovePush_menu);
			deleteFetch.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					RepositoryConfig config = node.getRepository().getConfig();
					config.unset("remote", configName, "pushurl"); //$NON-NLS-1$ //$NON-NLS-2$
					config.unset("remote", configName, "push"); //$NON-NLS-1$ //$NON-NLS-2$
					try {
						config.save();
						scheduleRefresh();
					} catch (IOException e1) {
						MessageDialog.openError(getSite().getShell(),
								UIText.RepositoriesView_ErrorHeader, e1
										.getMessage());
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
					openFile(file);
				}

			});

			new MenuItem(men, SWT.SEPARATOR);
			createCopyPathItem(men, file.getPath());
		}

		if (node.getType() == RepositoryTreeNodeType.WORKINGDIR) {
			String path = node.getRepository().getWorkDir().getAbsolutePath();
			createImportProjectItem(men, node.getRepository(), path);
			new MenuItem(men, SWT.SEPARATOR);
			createCopyPathItem(men, path);
		}

		if (node.getType() == RepositoryTreeNodeType.FOLDER) {
			String path = ((File) node.getObject()).getPath();
			createImportProjectItem(men, node.getRepository(), path);
			new MenuItem(men, SWT.SEPARATOR);
			createCopyPathItem(men, path);
		}

	}

	private void createCopyPathItem(Menu men, final String path) {

		MenuItem copyPath;
		copyPath = new MenuItem(men, SWT.PUSH);
		copyPath.setText(UIText.RepositoriesView_CopyPathToClipboardMenu);
		copyPath.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Clipboard clipboard = new Clipboard(null);
				TextTransfer textTransfer = TextTransfer.getInstance();
				Transfer[] transfers = new Transfer[] { textTransfer };
				Object[] data = new Object[] { path };
				clipboard.setContents(data, transfers);
				clipboard.dispose();
			}

		});

	}

	private void createCreateBranchItem(Menu men, final RepositoryTreeNode node) {

		final boolean remoteMode;
		final Ref ref;
		if (node.getType() == RepositoryTreeNodeType.REF) {
			remoteMode = node.getParent().getType() == RepositoryTreeNodeType.REMOTEBRANCHES;
			ref = (Ref) node.getObject();
		} else if (node.getType() == RepositoryTreeNodeType.LOCALBRANCHES) {
			remoteMode = false;
			ref = null;
		} else if (node.getType() == RepositoryTreeNodeType.REMOTEBRANCHES) {
			remoteMode = true;
			ref = null;
		} else
			return;

		MenuItem createLocal = new MenuItem(men, SWT.PUSH);
		if (remoteMode)
			createLocal.setText(UIText.RepositoriesView_NewRemoteBranchMenu);
		else
			createLocal.setText(UIText.RepositoriesView_NewLocalBranchMenu);

		createLocal.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				Wizard wiz = new Wizard() {

					@Override
					public void addPages() {
						addPage(new CreateBranchPage(node.getRepository(), ref,
								remoteMode));
						setWindowTitle(UIText.RepositoriesView_NewBranchTitle);
					}

					@Override
					public boolean performFinish() {

						try {
							getContainer().run(false, true,
									new IRunnableWithProgress() {

										public void run(IProgressMonitor monitor)
												throws InvocationTargetException,
												InterruptedException {
											CreateBranchPage cp = (CreateBranchPage) getPages()[0];
											try {
												cp.createBranch(monitor);
											} catch (CoreException ce) {
												throw new InvocationTargetException(
														ce);
											} catch (IOException ioe) {
												throw new InvocationTargetException(
														ioe);
											}

										}
									});
						} catch (InvocationTargetException ite) {
							Activator
									.handleError(
											UIText.RepositoriesView_BranchCreationFailureMessage,
											ite.getCause(), true);
							return false;
						} catch (InterruptedException ie) {
							// ignore here
						}
						return true;
					}
				};
				if (new WizardDialog(getSite().getShell(), wiz).open() == Window.OK)
					scheduleRefresh();
			}

		});

	}

	private void createDeleteBranchItem(Menu men, final RepositoryTreeNode node) {

		final Ref ref = (Ref) node.getObject();

		MenuItem deleteBranch = new MenuItem(men, SWT.PUSH);
		deleteBranch.setText(UIText.RepositoriesView_DeleteBranchMenu);

		try {
			if (node.getRepository().getFullBranch().equals(ref.getName())) {
				deleteBranch.setEnabled(false);
			}
		} catch (IOException e2) {
			// ignore
		}

		deleteBranch.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				if (!MessageDialog
						.openConfirm(
								getSite().getShell(),
								UIText.RepositoriesView_ConfirmDeleteTitle,
								NLS
										.bind(
												UIText.RepositoriesView_ConfirmBranchDeletionMessage,
												ref.getName())))
					return;

				try {
					new ProgressMonitorDialog(getSite().getShell()).run(false,
							false, new IRunnableWithProgress() {

								public void run(IProgressMonitor monitor)
										throws InvocationTargetException,
										InterruptedException {

									try {
										RefUpdate op = node.getRepository()
												.updateRef(ref.getName());
										op.setRefLogMessage("branch deleted", //$NON-NLS-1$
												false);
										// we set the force update in order
										// to avoid having this rejected
										// due to minor issues
										op.setForceUpdate(true);
										op.delete();
										scheduleRefresh();
									} catch (IOException ioe) {
										throw new InvocationTargetException(ioe);
									}

								}
							});
				} catch (InvocationTargetException e1) {
					Activator
							.handleError(
									UIText.RepositoriesView_BranchDeletionFailureMessage,
									e1.getCause(), true);
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					// ignore
				}
			}

		});

	}

	private void openFile(File file) {
		IFileStore store = EFS.getLocalFileSystem().getStore(
				new Path(file.getAbsolutePath()));
		try {
			// TODO do we need a read-only editor here?
			IDE.openEditor(getSite().getPage(),
					new FileStoreEditorInput(store),
					EditorsUI.DEFAULT_TEXT_EDITOR_ID);
		} catch (PartInitException e) {
			Activator.handleError(UIText.RepositoriesView_Error_WindowTitle, e,
					true);
		}
	}

	private void checkoutBranch(final RepositoryTreeNode node,
			final String refName) {
		// for the sake of UI responsiveness, let's start a job
		Job job = new Job(NLS.bind(UIText.RepositoriesView_CheckingOutMessage,
				refName)) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				Repository repo = node.getRepository();

				final BranchOperation op = new BranchOperation(repo, refName);
				IWorkspaceRunnable wsr = new IWorkspaceRunnable() {

					public void run(IProgressMonitor myMonitor)
							throws CoreException {
						op.execute(myMonitor);
					}
				};

				try {
					ResourcesPlugin.getWorkspace().run(wsr,
							ResourcesPlugin.getWorkspace().getRoot(),
							IWorkspace.AVOID_UPDATE, monitor);
					scheduleRefresh();
				} catch (CoreException e1) {
					return new Status(IStatus.ERROR, Activator.getPluginId(),
							e1.getMessage(), e1);
				}

				return Status.OK_STATUS;
			}
		};

		job.setUser(true);
		job.schedule();
	}

	private void createImportProjectItem(Menu men, final Repository repo,
			final String path) {

		MenuItem startWizard;
		startWizard = new MenuItem(men, SWT.PUSH);
		startWizard.setText(UIText.RepositoriesView_ImportProjectsMenu);
		startWizard.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				WizardDialog dlg = new WizardDialog(getSite().getShell(),
						new GitCreateProjectViaWizardWizard(repo, path));
				if (dlg.open() == Window.OK)
					scheduleRefresh();

			}

		});

		// we could start the ImportWizard here,
		// unfortunately, this fails within a wizard
		// startWizard = new MenuItem(men, SWT.PUSH);
		// startWizard.setText("Start the Import wizard...");
		// startWizard.addSelectionListener(new SelectionAdapter() {
		//
		// @Override
		// public void widgetSelected(SelectionEvent e) {
		//
		// IHandlerService handlerService = (IHandlerService) getSite()
		// .getWorkbenchWindow().getWorkbench().getService(
		// IHandlerService.class);
		//
		// try {
		//					handlerService.executeCommand("org.eclipse.ui.file.import", //$NON-NLS-1$
		// null);
		// } catch (ExecutionException e1) {
		// Activator.handleError(e1.getMessage(), e1, true);
		// } catch (NotDefinedException e1) {
		// Activator.handleError(e1.getMessage(), e1, true);
		// } catch (NotEnabledException e1) {
		// Activator.handleError(e1.getMessage(), e1, true);
		// } catch (NotHandledException e1) {
		// Activator.handleError(e1.getMessage(), e1, true);
		// }
		// }
		//
		// });
	}

	private void addActionsToToolbar() {
		importAction = new Action(UIText.RepositoriesView_Import_Button) {

			@Override
			public void run() {
				WizardDialog dlg = new WizardDialog(getSite().getShell(),
						new GitCloneWizard());
				if (dlg.open() == Window.OK)
					scheduleRefresh();
			}
		};
		importAction.setToolTipText(UIText.RepositoriesView_Clone_Tooltip);

		importAction.setImageDescriptor(UIIcons.CLONEGIT);

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

		IAction collapseAllAction = new Action(
				UIText.RepositoriesView_CollapseAllMenu) {

			@Override
			public void run() {
				tv.collapseAll();
			}
		};

		// copy and paste are global actions; we just implement them
		// and register them with the global action handler
		// we enable/disable them upon tree selection changes

		copyAction = new Action("") { //$NON-NLS-1$

			@Override
			public void run() {
				// for REPO, WORKINGDIR, FILE, FOLDER: copy directory
				IStructuredSelection sel = (IStructuredSelection) tv
						.getSelection();
				if (sel.size() == 1) {
					RepositoryTreeNode node = (RepositoryTreeNode) sel
							.getFirstElement();
					String dir = null;
					if (node.getType() == RepositoryTreeNodeType.REPO) {
						dir = node.getRepository().getDirectory().getPath();
					} else if (node.getType() == RepositoryTreeNodeType.FILE
							|| node.getType() == RepositoryTreeNodeType.FOLDER) {
						dir = ((File) node.getObject()).getPath();
					} else if (node.getType() == RepositoryTreeNodeType.WORKINGDIR) {
						dir = node.getRepository().getWorkDir().getPath();
					}
					if (dir != null) {
						Clipboard clip = null;
						try {
							clip = new Clipboard(getSite().getShell()
									.getDisplay());
							clip
									.setContents(new Object[] { dir },
											new Transfer[] { TextTransfer
													.getInstance() });
						} finally {
							if (clip != null)
								// we must dispose ourselves
								clip.dispose();
						}
					}
				}
			}

		};
		copyAction.setEnabled(false);

		getViewSite().getActionBars().setGlobalActionHandler(
				ActionFactory.COPY.getId(), copyAction);

		pasteAction = new Action("") { //$NON-NLS-1$

			@Override
			public void run() {
				// we check if the pasted content is a directory
				// repository location and try to add this
				String errorMessage = null;

				Clipboard clip = null;
				try {
					clip = new Clipboard(getSite().getShell().getDisplay());
					String content = (String) clip.getContents(TextTransfer
							.getInstance());
					if (content == null) {
						errorMessage = UIText.RepositoriesView_NothingToPasteMessage;
						return;
					}

					File file = new File(content);
					if (!file.exists() || !file.isDirectory()) {
						errorMessage = UIText.RepositoriesView_ClipboardContentNotDirectoryMessage;
						return;
					}

					if (!RepositoryCache.FileKey.isGitRepository(file)) {
						errorMessage = NLS
								.bind(
										UIText.RepositoriesView_ClipboardContentNoGitRepoMessage,
										content);
						return;
					}

					if (addDir(file))
						scheduleRefresh();
					else
						errorMessage = NLS.bind(
								UIText.RepositoriesView_PasteRepoAlreadyThere,
								content);
				} finally {
					if (clip != null)
						// we must dispose ourselves
						clip.dispose();
					if (errorMessage != null)
						// TODO String ext
						MessageDialog.openWarning(getSite().getShell(),
								UIText.RepositoriesView_PasteFailureTitle,
								errorMessage);
				}
			}

		};

		collapseAllAction.setImageDescriptor(UIIcons.COLLAPSEALL);

		getViewSite().getActionBars().getToolBarManager()
				.add(collapseAllAction);

		getViewSite().getActionBars().setGlobalActionHandler(
				ActionFactory.PASTE.getId(), pasteAction);

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

				final List<RepositoryTreeNode<Repository>> input;
				try {
					input = getRepositoriesFromDirs(monitor);
				} catch (InterruptedException e) {
					return new Status(IStatus.ERROR, Activator.getPluginId(), e
							.getMessage(), e);
				}

				boolean needsNewInput = tv.getInput() == null;
				List oldInput = (List) tv.getInput();
				if (!needsNewInput)
					needsNewInput = oldInput.size() != input.size();

				if (!needsNewInput) {
					for (int i = 0; i < input.size(); i++) {
						needsNewInput = !input.get(i).equals(oldInput.get(i));
						if (needsNewInput)
							break;
					}
				}

				final boolean updateInput = needsNewInput;

				Display.getDefault().syncExec(new Runnable() {

					public void run() {
						// keep expansion state and selection so that we can
						// restore the tree
						// after update
						Object[] expanded = tv.getExpandedElements();
						IStructuredSelection sel = (IStructuredSelection) tv
								.getSelection();
						if (updateInput)
							tv.setInput(input);
						else
							tv.refresh();
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

	/**
	 * Converts the directories as configured for this view into a list of
	 * {@link Repository} objects suitable for the tree content provider
	 * <p>
	 * TODO move to some utility class
	 *
	 * @param monitor
	 * @return a list of nodes
	 * @throws InterruptedException
	 */
	public static List<RepositoryTreeNode<Repository>> getRepositoriesFromDirs(
			IProgressMonitor monitor) throws InterruptedException {

		List<String> gitDirStrings = getDirs();
		List<RepositoryTreeNode<Repository>> input = new ArrayList<RepositoryTreeNode<Repository>>();

		for (String dirString : gitDirStrings) {
			if (monitor != null && monitor.isCanceled()) {
				throw new InterruptedException(
						UIText.RepositoriesView_ActionCanceled_Message);
			}
			try {
				File dir = new File(dirString);
				if (dir.exists() && dir.isDirectory()) {
					Repository repo = new Repository(dir);
					RepositoryTreeNode<Repository> node = new RepositoryTreeNode<Repository>(
							null, RepositoryTreeNodeType.REPO, repo, repo);
					input.add(node);
				}
			} catch (IOException e) {
				IStatus error = new Status(IStatus.ERROR, Activator
						.getPluginId(), e.getMessage(), e);
				Activator.getDefault().getLog().log(error);
			}
		}
		Collections.sort(input);
		return input;
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

	public boolean show(ShowInContext context) {
		ISelection selection = context.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			if (ss.size() == 1) {
				Object element = ss.getFirstElement();
				if (element instanceof IAdaptable) {
					IResource resource = (IResource) ((IAdaptable) element)
							.getAdapter(IResource.class);
					if (resource != null) {
						showResource(resource);
						return true;
					}
				}
			}
		}
		return false;
	}

}
