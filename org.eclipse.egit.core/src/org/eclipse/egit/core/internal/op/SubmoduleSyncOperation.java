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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleSyncCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.TeamException;

/**
 * Operation that syncs a repository's submodule configurations
 */
public class SubmoduleSyncOperation implements IEGitOperation {

	private final Repository repository;

	private final Collection<String> paths;

	/**
	 * Create submodule sync operation
	 *
	 * @param repository
	 */
	public SubmoduleSyncOperation(final Repository repository) {
		this.repository = repository;
		paths = new ArrayList<String>();
	}

	/**
	 * Add path of submodule to update
	 *
	 * @param path
	 * @return this operation
	 */
	public SubmoduleSyncOperation addPath(final String path) {
		paths.add(path);
		return this;
	}

	public void execute(final IProgressMonitor monitor) throws CoreException {
		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			public void run(IProgressMonitor pm) throws CoreException {
				pm.beginTask("", 1); //$NON-NLS-1$
				Map<String, String> updates = null;
				try {
					SubmoduleSyncCommand sync = Git.wrap(repository)
							.submoduleSync();
					for (String path : paths)
						sync.addPath(path);
					updates = sync.call();
				} catch (GitAPIException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				} finally {
					if (updates != null && !updates.isEmpty())
						repository.notifyIndexChanged();
					pm.done();
				}
			}
		};
		ResourcesPlugin.getWorkspace().run(action,
				monitor != null ? monitor : new NullProgressMonitor());
	}

	public ISchedulingRule getSchedulingRule() {
		return null;
	}
}
