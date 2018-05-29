/******************************************************************************
 *  Copyright (c) 2012, 2014 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.core.op;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.StashCreateCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.TeamException;

/**
 * Operation that creates a stashed commit for a repository
 */
public class StashCreateOperation implements IEGitOperation {

	private final Repository repository;

	private final String message;

	private RevCommit commit;

	private final boolean includeUntracked;

	/**
	 * Create operation for repository
	 *
	 * @param repository
	 */
	public StashCreateOperation(final Repository repository) {
		this(repository, null, false);
	}

	/**
	 * Create operation for repository
	 *
	 * @param repository
	 * @param message
	 */
	public StashCreateOperation(final Repository repository,
			final String message) {
		this(repository, message, false);
	}

	/**
	 * Create operation for repository
	 *
	 * @param repository
	 * @param message
	 * @param includeUntracked
	 */
	public StashCreateOperation(final Repository repository,
			final String message, final boolean includeUntracked) {
		this.repository = repository;
		this.message = message;
		this.includeUntracked = includeUntracked;
	}

	/**
	 * Get stashed commit
	 *
	 * @return commit
	 */
	public RevCommit getCommit() {
		return commit;
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor pm) throws CoreException {
				try {
					StashCreateCommand command = Git.wrap(repository).stashCreate();
					if (message != null)
						command.setWorkingDirectoryMessage(message);
					command.setIncludeUntracked(includeUntracked);
					commit = command.call();
				} catch (JGitInternalException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				} catch (GitAPIException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				} finally {
					if (commit != null) {
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
		return RuleUtil.getRule(repository);
	}
}
