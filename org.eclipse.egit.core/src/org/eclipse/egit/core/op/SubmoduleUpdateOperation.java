/******************************************************************************
 *  Copyright (c) 2012, 2015 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Laurent Delaigue (Obeo) - use of preferred merge strategy
 *    Stephan Hackstedt <stephan.hackstedt@googlemail.com - bug 477695
 *****************************************************************************/
package org.eclipse.egit.core.op;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleInitCommand;
import org.eclipse.jgit.api.SubmoduleUpdateCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.team.core.TeamException;

/**
 * Operation that updates a repository's submodules
 */
public class SubmoduleUpdateOperation implements IEGitOperation {

	private final Repository repository;

	private final Collection<String> paths;

	/**
	 * Create submodule update operation
	 *
	 * @param repository
	 */
	public SubmoduleUpdateOperation(final Repository repository) {
		this.repository = repository;
		paths = new ArrayList<>();
	}

	/**
	 * Add path of submodule to update
	 *
	 * @param path
	 * @return this operation
	 */
	public SubmoduleUpdateOperation addPath(final String path) {
		paths.add(path);
		return this;
	}

	@Override
	public void execute(final IProgressMonitor monitor) throws CoreException {
		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor pm) throws CoreException {
				RepositoryUtil util = Activator.getDefault()
						.getRepositoryUtil();
				SubMonitor progress = SubMonitor.convert(pm, 4);
				progress.setTaskName(MessageFormat.format(
						CoreText.SubmoduleUpdateOperation_updating,
						util.getRepositoryName(repository)));

				Git git = Git.wrap(repository);

				Collection<String> updated = null;
				try {
					SubmoduleInitCommand init = git.submoduleInit();
					for (String path : paths)
						init.addPath(path);
					init.call();
					progress.worked(1);

					SubmoduleUpdateCommand update = git.submoduleUpdate();
					for (String path : paths)
						update.addPath(path);
					update.setProgressMonitor(new EclipseGitProgressTransformer(
							progress.newChild(2)));
					MergeStrategy strategy = Activator.getDefault()
							.getPreferredMergeStrategy();
					if (strategy != null) {
						update.setStrategy(strategy);
					}
					update.setCallback(new CloneCommand.Callback() {

						@Override
						public void initializedSubmodules(
								Collection<String> submodules) {
							// Nothing to do
						}

						@Override
						public void cloningSubmodule(String path) {
							progress.setTaskName(MessageFormat.format(
									CoreText.SubmoduleUpdateOperation_cloning,
									util.getRepositoryName(repository), path));
						}

						@Override
						public void checkingOut(AnyObjectId commit,
								String path) {
							// Nothing to do
						}
					});
					updated = update.call();
					SubMonitor refreshMonitor = progress.newChild(1)
							.setWorkRemaining(updated.size());
					for (String path : updated) {
						Repository subRepo = SubmoduleWalk
								.getSubmoduleRepository(repository, path);
						if (subRepo != null) {
							ProjectUtil.refreshValidProjects(
									ProjectUtil.getValidOpenProjects(subRepo),
									refreshMonitor.newChild(1));
						} else {
							refreshMonitor.worked(1);
						}
					}
				} catch (GitAPIException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				} catch (IOException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				} finally {
					if (updated != null && !updated.isEmpty()) {
						repository.notifyIndexChanged(true);
					}
				}
			}
		};
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE, monitor);
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
}
