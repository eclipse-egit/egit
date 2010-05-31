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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
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
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.RepositoryUtil;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jgit.lib.IndexChangedEvent;
import org.eclipse.jgit.lib.RefsChangedEvent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
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

	/** "fetch" */
	public static final String VIEW_ID = "org.eclipse.egit.ui.RepositoriesView"; //$NON-NLS-1$

	private Job scheduledJob;

	private Job autoRefreshJob;

	private final static long AUTO_REFRESH_INTERVAL_MILLISECONDS = 10000l;

	private final RepositoryUtil repositoryUtil;

	private long lastInputChange = 0l;

	private long lastInputUpdate = -1l;

	private boolean reactOnSelection = false;

	private final IPreferenceChangeListener configurationListener = new IPreferenceChangeListener() {

		public void preferenceChange(PreferenceChangeEvent event) {
			lastInputChange = System.currentTimeMillis();
		}
	};

	/**
	 * The default constructor
	 */
	public RepositoriesView() {
		repositoryUtil = Activator.getDefault().getRepositoryUtil();
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

	/**
	 * @param reactOnSelection
	 */
	public void setReactOnSelection(boolean reactOnSelection) {
		// TODO persist the state of the button somewhere
		this.reactOnSelection = reactOnSelection;
		if (this.reactOnSelection) {
			ISelectionService srv = (ISelectionService) getSite().getService(
					ISelectionService.class);
			reactOnSelection(srv.getSelection());
		}
	}

	@Override
	protected CommonViewer createCommonViewer(Composite aParent) {

		CommonViewer viewer = super.createCommonViewer(aParent);

		viewer.addOpenListener(new IOpenListener() {

			public void open(OpenEvent event) {
				TreeSelection sel = (TreeSelection) event.getSelection();
				RepositoryTreeNode element = (RepositoryTreeNode) sel
						.getFirstElement();

				if (element instanceof RefNode || element instanceof FileNode
						|| element instanceof TagNode) {
					IHandlerService srv = (IHandlerService) getViewSite()
							.getService(IHandlerService.class);
					ICommandService csrv = (ICommandService) getViewSite()
							.getService(ICommandService.class);
					Command openCommand = csrv
							.getCommand("org.eclipse.egit.ui.RepositoriesViewOpen"); //$NON-NLS-1$
					ExecutionEvent evt = srv.createExecutionEvent(openCommand,
							null);

					try {
						openCommand.executeWithChecks(evt);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});

		ISelectionService srv = (ISelectionService) getSite().getService(
				ISelectionService.class);
		srv.addPostSelectionListener(new ISelectionListener() {

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

				} else {
					reactOnSelection(selection);
				}
			}

		});

		// schedule the auto-refresh job
		autoRefreshJob = new Job("Git Repositories View Auto-Refresh") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				scheduleRefresh();
				schedule(AUTO_REFRESH_INTERVAL_MILLISECONDS);
				return Status.OK_STATUS;
			}
		};

		autoRefreshJob.setSystem(true);
		autoRefreshJob.schedule(AUTO_REFRESH_INTERVAL_MILLISECONDS);

		repositoryUtil.getPreferences().addPreferenceChangeListener(
				configurationListener);

		return viewer;
	}

	@Override
	public void dispose() {
		// make sure to cancel the refresh job
		if (this.scheduledJob != null) {
			this.scheduledJob.cancel();
			this.scheduledJob = null;
		}
		// and the auto refresh job, too
		if (this.autoRefreshJob != null) {
			this.autoRefreshJob.cancel();
			this.autoRefreshJob = null;
		}
		repositoryUtil.getPreferences().removePreferenceChangeListener(
				configurationListener);
		// TODO remove react on selection listener
		super.dispose();
	}

	/**
	 * Opens the tree and marks the folder to which a project is pointing
	 *
	 * @param resource
	 *            TODO exceptions?
	 */
	@SuppressWarnings("unchecked")
	private void showResource(final IResource resource) {
		// TODO fix this when coming from reactOnSelection
		IProject project = resource.getProject();
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		if (mapping == null)
			return;

		boolean added = repositoryUtil.addConfiguredRepository(mapping
				.getRepository().getDirectory());
		if (added) {
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

			RepositoryTreeNode currentNode = null;
			ITreeContentProvider cp = (ITreeContentProvider) getCommonViewer()
					.getContentProvider();
			for (Object repo : cp.getElements(getCommonViewer().getInput())) {
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
					selectReveal(new StructuredSelection(selNode));
				}
			});

		}

	}

	/**
	 * Executes an immediate refresh
	 */
	public void refresh() {
		scheduleRefresh();
	}

	private void scheduleRefresh() {

		if (scheduledJob != null && scheduledJob.getState() == Job.RUNNING)
			return;

		final CommonViewer tv = getCommonViewer();
		final boolean needsNewInput = lastInputChange > lastInputUpdate;

		Job job = new Job("Refreshing Git Repositories view") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				final List<RepositoryNode> oldInput = new ArrayList<RepositoryNode>();
				if (!needsNewInput) {
					Display.getDefault().syncExec(new Runnable() {

						public void run() {
							for (TreeItem item : getCommonViewer().getTree()
									.getItems()) {
								oldInput.add((RepositoryNode) item.getData());
							}
						}
					});
				}

				// we only check for Repository changes if we don't
				// have a new input
				if (needsNewInput || checkForRepositoryChanges(oldInput)) {
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							// keep expansion state and selection so that we can
							// restore the tree
							// after update
							Object[] expanded = tv.getExpandedElements();
							IStructuredSelection sel = (IStructuredSelection) tv
									.getSelection();

							if (needsNewInput) {
								lastInputUpdate = System.currentTimeMillis();
								tv.setInput(ResourcesPlugin.getWorkspace()
										.getRoot());
							} else
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
				}
				return new Status(IStatus.OK, Activator.getPluginId(), ""); //$NON-NLS-1$
			}

		};
		job.setSystem(true);

		IWorkbenchSiteProgressService service = (IWorkbenchSiteProgressService) getSite()
				.getService(IWorkbenchSiteProgressService.class);

		service.schedule(job);

		scheduledJob = job;
	}

	private boolean checkForRepositoryChanges(List<RepositoryNode> input) {
		CommonViewer tv = getCommonViewer();
		if (tv.getInput() == null)
			return false;

		if (input.isEmpty())
			return false;

		final Set<Repository> reposToRefresh = new HashSet<Repository>();

		RepositoryListener listener = new RepositoryListener() {

			public void refsChanged(RefsChangedEvent e) {
				reposToRefresh.add(e.getRepository());
			}

			public void indexChanged(IndexChangedEvent e) {
				reposToRefresh.add(e.getRepository());
			}
		};

		for (final RepositoryTreeNode<Repository> node : input) {

			Repository repository = node.getRepository();
			repository.addRepositoryChangedListener(listener);
			try {
				repository.scanForRepoChanges();
			} catch (IOException e1) {
				// ignore
			} finally {
				repository.removeRepositoryChangedListener(listener);
			}
		}

		return !reposToRefresh.isEmpty();
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
}
