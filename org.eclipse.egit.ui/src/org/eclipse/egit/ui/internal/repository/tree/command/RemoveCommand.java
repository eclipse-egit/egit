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
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

/**
 * "Removes" one or several nodes
 */
public class RemoveCommand extends
		RepositoriesViewCommandHandler<RepositoryNode> implements IHandler {
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		removeRepository(event, false);
		return null;
	}

	/**
	 * Remove or delete the repository
	 *
	 * @param event
	 * @param delete
	 *            if <code>true</code>, the repository will be deleted from disk
	 */
	protected void removeRepository(final ExecutionEvent event,
			final boolean delete) {
		IWorkbenchSite activeSite = HandlerUtil.getActiveSite(event);
		IWorkbenchSiteProgressService service = (IWorkbenchSiteProgressService) activeSite
				.getService(IWorkbenchSiteProgressService.class);

		// get selected nodes
		final List<RepositoryNode> selectedNodes;
		try {
			selectedNodes = getSelectedNodes(event);
		} catch (ExecutionException e) {
			Activator.handleError(e.getMessage(), e, true);
			return;
		}
		boolean deleteWorkingDir = false;
		boolean removeProjects = false;
		final List<IProject> projectsToDelete = findProjectsToDelete(selectedNodes);
		if (delete) {
			if (selectedNodes.size() > 1) {
				return;
			} else if (selectedNodes.size() == 1) {
				Repository repository = selectedNodes.get(0).getObject();
				if (repository.isBare()) {
					// simple confirm dialog
					String title = UIText.RemoveCommand_ConfirmDeleteBareRepositoryTitle;
					String message = NLS
							.bind(
									UIText.RemoveCommand_ConfirmDeleteBareRepositoryMessage,
									repository.getDirectory().getPath());
					if (!MessageDialog.openConfirm(getShell(event), title,
							message))
						return;
				} else {
					// confirm dialog with check box
					// "delete also working directory"
					DeleteRepositoryConfirmDialog dlg = new DeleteRepositoryConfirmDialog(
							getShell(event), repository, projectsToDelete.size());
					if (dlg.open() != Window.OK)
						return;
					deleteWorkingDir = dlg.shouldDeleteWorkingDir();
					removeProjects = dlg.shouldRemoveProjects();
				}
			}
		}
		else {
			if (!projectsToDelete.isEmpty()) {
				final boolean[] confirmedCanceled = new boolean[] { false,
						false };
				Display.getDefault().syncExec(new Runnable() {

					public void run() {
						try {
							confirmedCanceled[0] = confirmProjectDeletion(
									projectsToDelete, event);
						} catch (OperationCanceledException e) {
							confirmedCanceled[1] = true;
						}
					}
				});
				if (confirmedCanceled[1])
					return;
				removeProjects = confirmedCanceled[0];
			}
		}

		final boolean deleteWorkDir = deleteWorkingDir;
		final boolean removeProj = removeProjects;

		Job job = new Job("Remove Repositories Job") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				monitor
						.setTaskName(UIText.RepositoriesView_DeleteRepoDeterminProjectsMessage);

				if (removeProj) {
					// confirmed deletion
					deleteProjects(delete, projectsToDelete,
							monitor);
				}
				for (RepositoryNode node : selectedNodes) {
					util.removeDir(node.getRepository().getDirectory());
				}

				if (delete) {
					try {
						deleteRepositoryContent(selectedNodes, deleteWorkDir);
					} catch (IOException e) {
						return Activator.createErrorStatus(e.getMessage(), e);
					}
				}
				return Status.OK_STATUS;
			}
		};

		service.schedule(job);
	}

	private void deleteProjects(
			final boolean delete,
			final List<IProject> projectsToDelete,
			IProgressMonitor monitor) {
		IWorkspaceRunnable wsr = new IWorkspaceRunnable() {

			public void run(IProgressMonitor actMonitor)
			throws CoreException {

				for (IProject prj : projectsToDelete)
					prj.delete(delete, false, actMonitor);
			}
		};

		try {
			ResourcesPlugin.getWorkspace().run(wsr,
					ResourcesPlugin.getWorkspace().getRoot(),
					IWorkspace.AVOID_UPDATE, monitor);
		} catch (CoreException e1) {
			Activator.logError(e1.getMessage(), e1);
		}
	}

	private void deleteRepositoryContent(
			final List<RepositoryNode> selectedNodes,
			final boolean deleteWorkDir) throws IOException {
		for (RepositoryNode node : selectedNodes) {
			Repository repo = node.getRepository();
			if (!repo.isBare() && deleteWorkDir) {
				File[] files = repo.getWorkTree().listFiles();
				if (files != null)
					for (File file : files) {
						if (isTracked(file, repo))
							FileUtils.delete(file,
									FileUtils.RECURSIVE | FileUtils.RETRY);
					}
			}
			repo.close();
			FileUtils.delete(repo.getDirectory(),
					FileUtils.RECURSIVE | FileUtils.RETRY
							| FileUtils.SKIP_MISSING);
		}
	}

	private boolean isTracked(File file, Repository repo) throws IOException {
		ObjectId objectId = repo.resolve(Constants.HEAD);
		RevTree tree;
		if (objectId != null)
			tree = new RevWalk(repo).parseTree(objectId);
		else
			tree = null;

		TreeWalk treeWalk = new TreeWalk(repo);
		treeWalk.setRecursive(true);
		if (tree != null)
			treeWalk.addTree(tree);
		else
			treeWalk.addTree(new EmptyTreeIterator());
		treeWalk.addTree(new DirCacheIterator(repo.readDirCache()));
		treeWalk.setFilter(PathFilterGroup.createFromStrings(Collections.singleton(
				Repository.stripWorkDir(repo.getWorkTree(), file)), treeWalk.getPathEncoding()));
		return treeWalk.next();

	}

	private List<IProject> findProjectsToDelete(final List<RepositoryNode> selectedNodes) {
		final List<IProject> projectsToDelete = new ArrayList<IProject>();
		for (RepositoryNode node : selectedNodes) {
			if (node.getRepository().isBare())
				continue;
			File workDir = node.getRepository().getWorkTree();
			final IPath wdPath = new Path(workDir.getAbsolutePath());
			for (IProject prj : ResourcesPlugin.getWorkspace()
					.getRoot().getProjects()) {
				if (wdPath.isPrefixOf(prj.getLocation())) {
					projectsToDelete.add(prj);
				}
			}
		}
		return projectsToDelete;
	}

	@SuppressWarnings("boxing")
	private boolean confirmProjectDeletion(List<IProject> projectsToDelete,
			ExecutionEvent event) throws OperationCanceledException {

		String message = NLS.bind(
				UIText.RepositoriesView_ConfirmProjectDeletion_Question,
				projectsToDelete.size());
		MessageDialog dlg = new MessageDialog(getShell(event),
				UIText.RepositoriesView_ConfirmProjectDeletion_WindowTitle,
				null, message, MessageDialog.INFORMATION, new String[] {
						IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL,
						IDialogConstants.CANCEL_LABEL }, 0);
		int index = dlg.open();
		if (index == 2)
			throw new OperationCanceledException();

		return index == 0;
	}
}
