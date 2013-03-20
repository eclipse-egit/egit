/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.core.internal.op;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.internal.EclipseGitProgressTransformer;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleInitCommand;
import org.eclipse.jgit.api.SubmoduleUpdateCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
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

	public void execute(final IProgressMonitor monitor) throws CoreException {
		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			public void run(IProgressMonitor pm) throws CoreException {
				pm.beginTask("", 3); //$NON-NLS-1$
				Git git = Git.wrap(repository);

				Collection<String> updated = null;
				try {
					SubmoduleInitCommand init = git.submoduleInit();
					for (String path : paths)
						init.addPath(path);
					init.call();
					pm.worked(1);

					SubmoduleUpdateCommand update = git.submoduleUpdate();
					for (String path : paths)
						update.addPath(path);
					update.setProgressMonitor(new EclipseGitProgressTransformer(
							new SubProgressMonitor(pm, 2)));
					updated = update.call();
					pm.worked(1);
					SubProgressMonitor refreshMonitor = new SubProgressMonitor(
							pm, 1);
					refreshMonitor.beginTask("", updated.size()); //$NON-NLS-1$
					for (String path : updated) {
						Repository subRepo = SubmoduleWalk
								.getSubmoduleRepository(repository, path);
						if (subRepo != null)
							ProjectUtil.refreshValidProjects(
									ProjectUtil.getValidOpenProjects(subRepo),
									new SubProgressMonitor(refreshMonitor, 1));
						else
							refreshMonitor.worked(1);
					}
					refreshMonitor.done();
				} catch (GitAPIException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				} catch (IOException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				} finally {
					if (updated != null && !updated.isEmpty())
						repository.notifyIndexChanged();
					pm.done();
				}
			}
		};
		ResourcesPlugin.getWorkspace().run(action,
				monitor != null ? monitor : new NullProgressMonitor());
	}

	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
}
