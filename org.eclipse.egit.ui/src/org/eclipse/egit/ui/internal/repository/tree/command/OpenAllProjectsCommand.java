/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Lars Vogel - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jgit.lib.Repository;

/**
 * Opens all closed projects belonging to the selected repositories.
 */
public class OpenAllProjectsCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<Repository> repositories = getRepositoriesOfNodes(
				getSelectedNodes(event));
		if (repositories.isEmpty()) {
			return null;
		}
		List<IProject> closedProjects = getClosedProjects(repositories);
		if (closedProjects.isEmpty()) {
			return null;
		}
		WorkspaceJob job = new WorkspaceJob(getJobTitle(repositories)) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor)
					throws CoreException {
				SubMonitor progress = SubMonitor.convert(monitor,
						closedProjects.size());
				for (IProject project : closedProjects) {
					if (progress.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					try {
						project.open(IResource.BACKGROUND_REFRESH,
								progress.newChild(1));
					} catch (CoreException e) {
						Activator.logError(e.getMessage(), e);
					}
				}
				return Status.OK_STATUS;
			}
		};
		// Without a rule every open is a separate workspace operation that
		// lets conflicting jobs interleave.
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.setUser(true);
		job.schedule();
		return null;
	}

	@Override
	public boolean isEnabled() {
		List<Repository> repositories = getRepositoriesOfNodes(
				getSelectedNodes());
		return !repositories.isEmpty()
				&& !getClosedProjects(repositories).isEmpty();
	}

	private static List<IProject> getClosedProjects(
			List<Repository> repositories) {
		// Nested repositories may report the same project more than once.
		Set<IProject> closed = new LinkedHashSet<>();
		for (Repository repository : repositories) {
			IPath repoPath = new Path(
					repository.getWorkTree().getAbsolutePath());
			for (IProject project : ProjectUtil
					.getProjectsUnderPath(repoPath)) {
				if (!project.isOpen()) {
					closed.add(project);
				}
			}
		}
		return new ArrayList<>(closed);
	}

	private static String getJobTitle(List<Repository> repositories) {
		if (repositories.size() == 1) {
			return MessageFormat.format(UIText.OpenAllProjectsCommand_jobTitle,
					repositories.get(0).getWorkTree().getName());
		}
		return MessageFormat.format(
				UIText.OpenAllProjectsCommand_jobTitleMultiple,
				Integer.valueOf(repositories.size()));
	}
}
