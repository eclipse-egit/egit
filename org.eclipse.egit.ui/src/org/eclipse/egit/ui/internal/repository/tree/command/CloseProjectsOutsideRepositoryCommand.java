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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;

/**
 * Closes all open projects that do not belong to the selected repositories.
 */
public class CloseProjectsOutsideRepositoryCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<Repository> repositories = getRepositoriesOfNodes(
				getSelectedNodes(event));
		if (repositories.isEmpty()) {
			return null;
		}
		List<IProject> otherProjects = getOtherOpenProjects(repositories);
		if (otherProjects.isEmpty()) {
			return null;
		}
		if (!MessageDialog.openConfirm(getShell(event),
				UIText.CloseProjectsOutsideRepositoryCommand_confirmTitle,
				getConfirmMessage(repositories, otherProjects.size()))) {
			return null;
		}
		// Without a rule every close is a separate workspace operation that
		// lets conflicting jobs interleave.
		IResourceRuleFactory factory = ResourcesPlugin.getWorkspace()
				.getRuleFactory();
		ISchedulingRule rule = null;
		for (IProject project : otherProjects) {
			rule = MultiRule.combine(rule, factory.modifyRule(project));
		}
		WorkspaceJob job = new WorkspaceJob(getJobTitle(repositories)) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor)
					throws CoreException {
				SubMonitor progress = SubMonitor.convert(monitor,
						otherProjects.size());
				for (IProject project : otherProjects) {
					if (progress.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					try {
						project.close(progress.newChild(1));
					} catch (CoreException e) {
						Activator.logError(e.getMessage(), e);
					}
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(rule);
		job.setUser(true);
		job.schedule();
		return null;
	}

	@Override
	public boolean isEnabled() {
		List<Repository> repositories = getRepositoriesOfNodes(
				getSelectedNodes());
		return !repositories.isEmpty()
				&& !getOtherOpenProjects(repositories).isEmpty();
	}

	private static List<IProject> getOtherOpenProjects(
			List<Repository> repositories) {
		Set<IProject> inRepositories = new HashSet<>();
		for (Repository repository : repositories) {
			inRepositories.addAll(Arrays
					.asList(ProjectUtil.getProjectsUnderPath(new Path(
							repository.getWorkTree().getAbsolutePath()))));
		}
		List<IProject> result = new ArrayList<>();
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot()
				.getProjects()) {
			if (project.isOpen() && !inRepositories.contains(project)) {
				result.add(project);
			}
		}
		return result;
	}

	private static String getConfirmMessage(List<Repository> repositories,
			int projectCount) {
		if (repositories.size() == 1) {
			return MessageFormat.format(
					UIText.CloseProjectsOutsideRepositoryCommand_confirmMessage,
					Integer.valueOf(projectCount),
					repositories.get(0).getWorkTree().getName());
		}
		return MessageFormat.format(
				UIText.CloseProjectsOutsideRepositoryCommand_confirmMessageMultiple,
				Integer.valueOf(projectCount),
				Integer.valueOf(repositories.size()));
	}

	private static String getJobTitle(List<Repository> repositories) {
		if (repositories.size() == 1) {
			return MessageFormat.format(
					UIText.CloseProjectsOutsideRepositoryCommand_jobTitle,
					repositories.get(0).getWorkTree().getName());
		}
		return UIText.CloseProjectsOutsideRepositoryCommand_jobTitleMultiple;
	}
}
