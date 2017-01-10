/******************************************************************************
 *  Copyright (c) 2012, 2015 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Laurent Delaigue (Obeo) - use of preferred merge strategy
 *    Stephan Hackstedt <stephan.hackstedt@googlemail.com - bug 477695
 *****************************************************************************/
package org.eclipse.egit.core.op;

import java.io.IOException;
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
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleInitCommand;
import org.eclipse.jgit.api.SubmoduleUpdateCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
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
		paths = new ArrayList<String>();
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
				SubMonitor progress = SubMonitor.convert(pm, 4);

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
						repository.notifyIndexChanged();
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
