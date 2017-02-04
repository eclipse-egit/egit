/******************************************************************************
 *  Copyright (c) 2012, 2013 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Laurent Goubet <laurent.goubet@obeo.fr - 404121
 *****************************************************************************/
package org.eclipse.egit.core.op;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.TeamException;

/**
 * Operation to add a submodule to a repository
 */
public class SubmoduleAddOperation implements IEGitOperation {

	private final Repository repo;

	private final String path;

	private final String uri;

	/**
	 * Create operation
	 *
	 * @param repo
	 * @param path
	 * @param uri
	 */
	public SubmoduleAddOperation(final Repository repo, final String path,
			final String uri) {
		this.repo = repo;
		this.path = path;
		this.uri = uri;
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor pm) throws CoreException {
				final SubmoduleAddCommand add = Git.wrap(repo).submoduleAdd();
				add.setProgressMonitor(new EclipseGitProgressTransformer(pm));
				add.setPath(path);
				add.setURI(uri);
				try {
					Repository subRepo = add.call();
					if (subRepo != null) {
						subRepo.close();
						repo.notifyIndexChanged();
					}
				} catch (GitAPIException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				}
			}
		};
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE, monitor);
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRule(repo);
	}
}
