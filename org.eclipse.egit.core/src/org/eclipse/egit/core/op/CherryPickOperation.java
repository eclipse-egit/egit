/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.core.op;

import java.text.MessageFormat;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.api.CherryPickCommand;
import org.eclipse.jgit.api.CherryPickResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.TeamException;

/**
 * Cherry pick operation
 */
public class CherryPickOperation implements IEGitOperation {

	private final Repository repo;

	private final RevCommit commit;

	private CherryPickResult result;

	/**
	 * Create cherry pick operation
	 *
	 * @param repository
	 * @param commit
	 */
	public CherryPickOperation(Repository repository, RevCommit commit) {
		this.repo = repository;
		this.commit = commit;
	}

	/**
	 * @return cherry pick result
	 */
	public CherryPickResult getResult() {
		return result;
	}

	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor = m != null ? m : new NullProgressMonitor();
		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			public void run(IProgressMonitor pm) throws CoreException {
				pm.beginTask("", 2); //$NON-NLS-1$

				pm.subTask(MessageFormat.format(
						CoreText.CherryPickOperation_cherryPicking,
						commit.name()));
				CherryPickCommand command = new Git(repo).cherryPick().include(
						commit.getId());
				try {
					result = command.call();
				} catch (GitAPIException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				}
				pm.worked(1);

				ProjectUtil.refreshValidProjects(
						ProjectUtil.getValidProjects(repo),
						new SubProgressMonitor(pm, 1));

				pm.done();
			}
		};
		ResourcesPlugin.getWorkspace().run(action, monitor);
	}

	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
}
