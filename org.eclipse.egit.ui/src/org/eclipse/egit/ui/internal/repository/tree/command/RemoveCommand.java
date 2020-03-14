/*******************************************************************************
 * Copyright (c) 2010, 2019 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Daniel Megert <daniel_megert@ch.ibm.com> - Delete empty working directory
 *    Laurent Goubet <laurent.goubet@obeo.fr> - Bug 404121
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 479964
 *    Alexander Nittka <alex@nittka.de> -  Bug 545123
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.groups.RepositoryGroups;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.services.IServiceLocator;

/**
 * "Removes" one or several nodes
 */
public class RemoveCommand extends
		RepositoriesViewCommandHandler<RepositoryNode> {
	@Override
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
		IServiceLocator serviceLocator = HandlerUtil.getActiveSite(event);
		IWorkbenchSiteProgressService service = null;
		if (serviceLocator != null) {
			service = CommonUtils.getService(serviceLocator,
					IWorkbenchSiteProgressService.class);
		}

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
							getShell(event), repository, projectsToDelete);
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
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

					@Override
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

		Job job = new WorkspaceJob(UIText.RemoveCommand_RemoveRepositoriesJob) {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {

				monitor.setTaskName(UIText.RepositoriesView_DeleteRepoDeterminProjectsMessage);

				if (removeProj) {
					// confirmed deletion
					deleteProjects(deleteWorkDir, projectsToDelete,
							monitor);
				}
				List<File> repoDirs = selectedNodes.stream()
						.map(node -> node.getRepository().getDirectory())
						.collect(Collectors.toList());
				repoDirs.stream().forEach(util::removeDir);
				RepositoryGroups.getInstance().removeFromGroups(repoDirs);

				if (delete) {
					try {
						deleteRepositoryContent(selectedNodes, deleteWorkDir);
					} catch (IOException e) {
						return Activator.createErrorStatus(e.getMessage(), e);
					}
				}
				PlatformUI.getWorkbench().getDisplay()
						.asyncExec(new Runnable() {

					@Override
					public void run() {
						for (RepositoryNode node : selectedNodes) {
							node.clear();
						}
					}
				});
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.REPOSITORY_DELETE.equals(family))
					return true;
				else
					return super.belongsTo(family);
			}
		};

		if (service == null) {
			job.schedule();
		} else {
			service.schedule(job);
		}
	}

	private void deleteProjects(
			final boolean delete,
			final List<IProject> projectsToDelete,
			IProgressMonitor monitor) {
		IWorkspaceRunnable wsr = new IWorkspaceRunnable() {

			@Override
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
			if (!repo.isBare()) {
				closeSubmoduleRepositories(repo);
			}
			File workTree = deleteWorkDir && !repo.isBare() ? repo.getWorkTree() : null;
			if (workTree != null) {
				File[] files = workTree.listFiles();
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

			if (workTree != null) {
				// Delete working directory if a submodule repository and refresh
				// parent repository
				if (node.getParent() != null
						&& node.getParent().getType() == RepositoryTreeNodeType.SUBMODULES) {
					FileUtils.delete(workTree, FileUtils.RECURSIVE
							| FileUtils.RETRY | FileUtils.SKIP_MISSING);
					node.getParent().getRepository().notifyIndexChanged(true);
				}
				// Delete if empty working directory
				String[] files = workTree.list();
				boolean isWorkingDirEmpty = files != null && files.length == 0;
				if (isWorkingDirEmpty)
					FileUtils.delete(workTree, FileUtils.RETRY | FileUtils.SKIP_MISSING);
			}
		}
	}

	private static void closeSubmoduleRepositories(Repository repo)
			throws IOException {
		try (SubmoduleWalk walk = SubmoduleWalk.forIndex(repo)) {
			while (walk.next()) {
				Repository subRepo = walk.getRepository();
				if (subRepo != null) {
					RepositoryCache cache = null;
					try {
						cache = org.eclipse.egit.core.Activator.getDefault()
								.getRepositoryCache();
					} finally {
						if (cache != null)
							cache.lookupRepository(subRepo.getDirectory())
									.close();
						subRepo.close();
					}
				}
			}
		}
	}

	private boolean isTracked(File file, Repository repo) throws IOException {
		ObjectId objectId = repo.resolve(Constants.HEAD);
		RevTree tree;
		try (RevWalk rw = new RevWalk(repo);
				TreeWalk treeWalk = new TreeWalk(repo)) {
			if (objectId != null)
				tree = rw.parseTree(objectId);
			else
				tree = null;

			treeWalk.setRecursive(true);
			if (tree != null)
				treeWalk.addTree(tree);
			else
				treeWalk.addTree(new EmptyTreeIterator());
			treeWalk.addTree(new DirCacheIterator(repo.readDirCache()));
			treeWalk.setFilter(PathFilterGroup
					.createFromStrings(Collections.singleton(Repository
							.stripWorkDir(repo.getWorkTree(), file))));
			return treeWalk.next();
		}

	}

	private List<IProject> findProjectsToDelete(final List<RepositoryNode> selectedNodes) {
		final List<IProject> projectsToDelete = new ArrayList<>();
		for (RepositoryNode node : selectedNodes) {
			Repository repository = node.getRepository();
			if (repository == null || repository.isBare()) {
				continue;
			}
			File workDir = repository.getWorkTree();
			final IPath wdPath = new Path(workDir.getAbsolutePath());
			for (IProject prj : ResourcesPlugin.getWorkspace()
					.getRoot().getProjects()) {
				IPath location = prj.getLocation();
				if (location != null && wdPath.isPrefixOf(location)) {
					projectsToDelete.add(prj);
				}
			}
		}
		return projectsToDelete;
	}

	@SuppressWarnings("boxing")
	private boolean confirmProjectDeletion(List<IProject> projectsToDelete,
			ExecutionEvent event) throws OperationCanceledException {

		String message = MessageFormat.format(
				UIText.RepositoriesView_ConfirmProjectDeletion_Question,
				projectsToDelete.size());
		MessageDialog dlg = new MessageDialog(getShell(event),
				UIText.RepositoriesView_ConfirmProjectDeletion_WindowTitle,
				null, message, MessageDialog.INFORMATION, new String[] {
						IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL,
						IDialogConstants.CANCEL_LABEL }, 0);
		int index = dlg.open();
		// Return true if 'Yes' was selected
		if (index == 0)
			return true;
		// Return false if 'No' was selected
		if (index == 1)
			return false;
		// Cancel operation in all other cases
		throw new OperationCanceledException();
	}
}
