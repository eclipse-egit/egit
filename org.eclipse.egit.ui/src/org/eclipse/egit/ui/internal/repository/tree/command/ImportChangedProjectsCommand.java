/******************************************************************************
 *  Copyright (c) 2014, Tobias Melcher <tobias.melcher@sap.com>
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *****************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.egit.ui.internal.history.HistoryPageInput;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

/**
 * Imports the projects changed by a given commit.
 *
 * Loops over all changed files of a revision to find the enclosing projects and
 * imports them.
 */
public class ImportChangedProjectsCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RepositoryTreeNode> selectedNodes = getSelectedNodes(event);
		if (selectedNodes == null || selectedNodes.isEmpty()) {
			MessageDialog.openError(Display.getDefault().getActiveShell(),
					UIText.ImportProjectsWrongSelection,
					UIText.ImportProjectsSelectionInRepositoryRequired);
			return null;
		}

		for (Object node : selectedNodes) {
			List<File> files = null;
			Repository repo = getRepository(event);
			if (repo == null || !(node instanceof PlotCommit<?>))
				return null;

			files = getChangedFiles((RevCommit) node, repo);
			Set<File> dotProjectFiles = findDotProjectFiles(files, repo);
			importProjects(dotProjectFiles);
		}

		return null;
	}

	private List<File> getChangedFiles(RevCommit commit, Repository repo) {
		try {
			List<File> files = new ArrayList<File>();
			try (TreeWalk tw = new TreeWalk(repo);
					final RevWalk walk = new RevWalk(repo)) {
				tw.setRecursive(true);
				FileDiff[] diffs = FileDiff.compute(repo, tw, commit,
						TreeFilter.ALL);
				if (diffs != null && diffs.length > 0) {
					String workDir = repo.getWorkTree().getAbsolutePath();
					for (FileDiff d : diffs) {
						String path = d.getPath();
						File f = new File(workDir + File.separator + path);
						files.add(f);
					}
				}
			}
			return files;
		} catch (IOException e) {
			Activator.error(e.getMessage(), e);
		}
		return null;
	}

	private Repository getRepository(Object input) throws ExecutionException {
		if (input == null)
			return null;
		if (input instanceof RefNode) {
			Repository repo = ((RefNode) input).getRepository();
			return repo;
		} else if (input instanceof ExecutionEvent) {
			ExecutionEvent event = (ExecutionEvent) input;
			IWorkbenchPart ap = HandlerUtil.getActivePartChecked(event);
			if (ap instanceof IHistoryView) {
				input = ((IHistoryView) ap).getHistoryPage().getInput();
				return getRepository(input);
			}
		} else if (input instanceof HistoryPageInput) {
			return ((HistoryPageInput) input).getRepository();
		} else if (input instanceof RepositoryTreeNode) {
			RepositoryTreeNode rptn = (RepositoryTreeNode) input;
			Repository repo = rptn.getRepository();
			return repo;
		} else if (input instanceof IResource) {
			RepositoryMapping mapping = RepositoryMapping
					.getMapping((IResource) input);
			if (mapping != null) {
				Repository repo = mapping.getRepository();
				return repo;
			}
		}
		return null;
	}

	private Set<File> findDotProjectFiles(List<File> files, Repository repo) {
		Set<File> result = new HashSet<File>();
		String workingTreeRootPath = repo.getWorkTree().toString();
		for (File changedFile : files) {
			File projectFile = searchEnclosingProjectInWorkDir(
					changedFile.getParentFile(), workingTreeRootPath);
			if (projectFile != null)
				result.add(projectFile);
		}
		return result;
	}

	private File searchEnclosingProjectInWorkDir(File subFolder, String rootPath) {
		File projectFile = null;
		File currentPath = subFolder;

		while (currentPath.toString().startsWith(rootPath)) {
			projectFile = new File(currentPath.toString() + File.separator
					+ ".project"); //$NON-NLS-1$
			if (projectFile.isFile())
				break;
			projectFile = null;
			currentPath = currentPath.getParentFile();
		}
		return projectFile;
	}

	private void importProjects(final Set<File> dotProjectFiles) {
		WorkspaceJob job = new WorkspaceJob(
				UIText.ImportChangedProjectsCommand_ImportingChangedProjects) {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor)
					throws CoreException {
				for (File f : dotProjectFiles) {
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;
					String ap = f.getAbsolutePath();
					importProject(ap);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private void importProject(String path) {
		try {
			@SuppressWarnings("restriction")
			IProjectDescription description = IDEWorkbenchPlugin
					.getPluginWorkspace().loadProjectDescription(
							new org.eclipse.core.runtime.Path(path));
			if (description != null) {
				String projectName = description.getName();
				IProject project = ResourcesPlugin.getWorkspace().getRoot()
						.getProject(projectName);
				if (project.exists() == true) {
					if (project.isOpen() == false)
						project.open(IResource.BACKGROUND_REFRESH,
								new NullProgressMonitor());
				} else {
					project.create(description, new NullProgressMonitor());
					project.open(IResource.BACKGROUND_REFRESH,
							new NullProgressMonitor());
				}
			}
		} catch (CoreException e) {
			Activator.error(e.getMessage(), e);
		}
	}
}
