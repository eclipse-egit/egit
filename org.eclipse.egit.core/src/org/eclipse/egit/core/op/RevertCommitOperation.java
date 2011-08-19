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
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RevertCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.TeamException;

/**
 * Operation to revert a commit
 */
public class RevertCommitOperation implements IEGitOperation {

	private final Repository repo;

	private final RevCommit commit;

	private RevCommit newHead;

	private List<Ref> reverted;

	/**
	 * Create revert commit operation
	 *
	 * @param repository
	 * @param commit
	 */
	public RevertCommitOperation(Repository repository, RevCommit commit) {
		this.repo = repository;
		this.commit = commit;
	}

	/**
	 * @return new head commit
	 */
	public RevCommit getNewHead() {
		return newHead;
	}

	/**
	 * @return reverted refs
	 */
	public List<Ref> getRevertedRefs() {
		return reverted;
	}

	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor = m != null ? m : new NullProgressMonitor();
		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			public void run(IProgressMonitor pm) throws CoreException {
				pm.beginTask("", 2); //$NON-NLS-1$

				pm.subTask(MessageFormat.format(
						CoreText.RevertCommitOperation_reverting, commit.name()));
				RevertCommand command = new Git(repo).revert().include(commit);
				try {
					newHead = command.call();
					reverted = command.getRevertedRefs();
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
