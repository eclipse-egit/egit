/******************************************************************************
 *  Copyright (c) 2011, 2015 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Laurent Delaigue (Obeo) - use of preferred merge strategy
 *    Stephan Hackstedt - Bug 477695
 *****************************************************************************/
package org.eclipse.egit.core.op;

import java.text.MessageFormat;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.api.CherryPickCommand;
import org.eclipse.jgit.api.CherryPickResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.TeamException;

/**
 * Cherry pick operation
 */
public class CherryPickOperation implements IEGitOperation {

	private final Repository repo;

	private final RevCommit commit;

	private int parentIndex = -1;

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
	 * Defines the parent to diff against if the commit is a merge commit.
	 * Ignored if the commit has only one parent.
	 *
	 * @param parentIndex
	 *            defining the diff, zero-based
	 */
	public void setMainlineIndex(int parentIndex) {
		if (parentIndex >= 0 && parentIndex < commit.getParentCount()) {
			this.parentIndex = parentIndex;
		}
	}

	/**
	 * @return cherry pick result
	 */
	public CherryPickResult getResult() {
		return result;
	}

	@Override
	public void execute(IProgressMonitor m) throws CoreException {
		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor pm) throws CoreException {
				SubMonitor progress = SubMonitor.convert(pm, 2);

				progress.subTask(MessageFormat.format(
						CoreText.CherryPickOperation_cherryPicking,
						commit.name()));
				try (Git git = new Git(repo)) {
					CherryPickCommand command = git.cherryPick()
							.include(commit.getId());
					MergeStrategy strategy = Activator.getDefault()
							.getPreferredMergeStrategy();
					if (strategy != null) {
						command.setStrategy(strategy);
					}
					if (parentIndex >= 0
							&& parentIndex < commit.getParentCount()) {
						command.setMainlineParentNumber(parentIndex + 1);
					}
					result = command.call();
				} catch (GitAPIException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				}
				progress.worked(1);

				ProjectUtil.refreshValidProjects(
						ProjectUtil.getValidOpenProjects(repo),
						progress.newChild(1));
			}
		};
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE, m);
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRule(repo);
	}
}
