/******************************************************************************
 *  Copyright (c) 2014, Tobias Melcher <tobias.melcher@sap.com>,
 *                      Felix Otto <felix.otto@sap.com>
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.io.File;
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
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

/**
 * Imports the projects changed by (a) given commit(s).
 *
 * Loops over all changed files of a revision to find the enclosing projects and
 * imports them.
 */
@SuppressWarnings("restriction")
public class ImportChangedProjectsCommand
		extends AbstractHistoryCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(event);
		Set<File> changedFiles = new HashSet<>();

		List<RevCommit> commits = getSelectedCommits(event);
		for (RevCommit commit : commits) {
			RepositoryCommit repoCommit = new RepositoryCommit(repository,
					commit);
			changedFiles.addAll(determineChangedFilesOfCommit(repoCommit));
		}
		Set<File> dotProjectFiles = findDotProjectFiles(changedFiles,
				repository);
		importProjects(dotProjectFiles);
		return null;
	}

	private Set<File> determineChangedFilesOfCommit(
			RepositoryCommit repoCommit) {
		Set<File> changedFilesOfCommit = new HashSet<>();
		File rootOfWorkingDirectory = repoCommit.getRepository().getWorkTree();

		FileDiff[] fileDiffs = repoCommit.getDiffs();
		for (FileDiff fileDiff : fileDiffs) {
			// in case of brand new files there is no old path
			addIfNotDevNull(changedFilesOfCommit, rootOfWorkingDirectory,
					fileDiff.getNewPath());
			// in case of deletions there is no new path
			addIfNotDevNull(changedFilesOfCommit, rootOfWorkingDirectory,
					fileDiff.getOldPath());
		}
		return changedFilesOfCommit;
	}

	private void addIfNotDevNull(Set<File> changedFilesOfCommit,
			File rootOfWorkingDirectory, String relativePathOfChangedFile) {
		if (!relativePathOfChangedFile.contains("/dev/null")) { //$NON-NLS-1$
			changedFilesOfCommit.add(new File(rootOfWorkingDirectory,
					relativePathOfChangedFile));
		}
	}

	private Set<File> findDotProjectFiles(Set<File> changedFiles,
			Repository repository) {
		Set<File> result = new HashSet<>();
		String workingTreeRootPath = repository.getWorkTree().toString();
		for (File changedFile : changedFiles) {
			File projectFile = searchEnclosingProjectInWorkDir(
					changedFile.getParentFile().getAbsoluteFile(),
					workingTreeRootPath);
			if (projectFile != null)
				result.add(projectFile);
		}
		return result;
	}

	private File searchEnclosingProjectInWorkDir(File startFolder,
			String workingTreeRootPath) {
		File projectFile = null;
		File currentPath = startFolder;

		while (currentPath.toString().startsWith(workingTreeRootPath)) {
			projectFile = new File(
					currentPath.toString() + File.separator + ".project"); //$NON-NLS-1$
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
				SubMonitor progress = SubMonitor.convert(monitor,
						dotProjectFiles.size());
				for (File f : dotProjectFiles) {
					if (progress.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					String ap = f.getAbsolutePath();
					importProject(ap, progress.newChild(1));
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private void importProject(String path, IProgressMonitor monitor) {
		try {
			IProjectDescription description = IDEWorkbenchPlugin
					.getPluginWorkspace().loadProjectDescription(
							new org.eclipse.core.runtime.Path(path));
			if (description != null) {
				String projectName = description.getName();
				IProject project = ResourcesPlugin.getWorkspace().getRoot()
						.getProject(projectName);
				if (project.exists()) {
					if (!project.isOpen()) {
						project.open(IResource.BACKGROUND_REFRESH, monitor);
					}
				} else {
					SubMonitor progress = SubMonitor.convert(monitor, 2);
					project.create(description, progress.newChild(1));
					project.open(IResource.BACKGROUND_REFRESH,
							progress.newChild(1));
				}
			}
		} catch (CoreException e) {
			Activator.error(e.getMessage(), e);
		}
	}
}
