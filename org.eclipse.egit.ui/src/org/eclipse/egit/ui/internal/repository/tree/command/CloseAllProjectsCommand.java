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
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jgit.lib.Repository;

/**
 * Closes all open projects belonging to the selected repositories.
 */
public class CloseAllProjectsCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<Repository> repositories = getRepositoriesOfNodes(
				getSelectedNodes(event));
		if (repositories.isEmpty()) {
			return null;
		}
		List<IProject> openProjects = getOpenProjects(repositories);
		if (openProjects.isEmpty()) {
			return null;
		}
		// Without a rule every close is a separate workspace operation that
		// lets conflicting jobs interleave.
		IResourceRuleFactory factory = ResourcesPlugin.getWorkspace()
				.getRuleFactory();
		ISchedulingRule rule = null;
		for (IProject project : openProjects) {
			rule = MultiRule.combine(rule, factory.modifyRule(project));
		}
		WorkspaceJob job = new WorkspaceJob(getJobTitle(repositories)) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor)
					throws CoreException {
				SubMonitor progress = SubMonitor.convert(monitor,
						openProjects.size());
				for (IProject project : openProjects) {
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
				&& !getOpenProjects(repositories).isEmpty();
	}

	private static List<IProject> getOpenProjects(
			List<Repository> repositories) {
		// Nested repositories may report the same project more than once.
		Set<IProject> openProjects = new LinkedHashSet<>();
		for (Repository repository : repositories) {
			for (IProject project : ProjectUtil.getProjects(repository)) {
				if (project.isOpen()) {
					openProjects.add(project);
				}
			}
		}
		return new ArrayList<>(openProjects);
	}

	private static String getJobTitle(List<Repository> repositories) {
		if (repositories.size() == 1) {
			return MessageFormat.format(
					UIText.CloseAllProjectsCommand_jobTitle,
					repositories.get(0).getWorkTree().getName());
		}
		return MessageFormat.format(
				UIText.CloseAllProjectsCommand_jobTitleMultiple,
				Integer.valueOf(repositories.size()));
	}
}
