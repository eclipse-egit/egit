/******************************************************************************
 *  Copyright (c) 2011, 2015 GitHub Inc and others.
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
 *    Stephan Hackstedt - Bug 477695
 *****************************************************************************/
package org.eclipse.egit.core.op;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.MergeStrategies;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.annotations.NonNull;
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

	private final RevCommit[] commits;

	private int parentIndex = -1;

	private CherryPickResult result;

	/**
	 * Creates a cherry-pick operation.
	 *
	 * @param repository
	 *            the repository
	 * @param commit
	 *            the commit to cherry-pick
	 */
	public CherryPickOperation(@NonNull
	Repository repository, @NonNull
	RevCommit commit) {
		this(repository, List.of(commit));
	}

	/**
	 * Creates a cherry-pick operation.
	 *
	 * @param repository
	 *            the repository
	 * @param commits
	 *            the commits to cherry-pick
	 */
	public CherryPickOperation(@NonNull
	Repository repository, List<RevCommit> commits) {
		this.repo = repository;
		this.commits = commits.toArray(new RevCommit[commits.size()]);
	}

	/**
	 * Defines the parent to diff against if the commits contain merge commits.
	 * Ignored if all commits have one or less parents.
	 *
	 * @param parentIndex
	 *            defining the diff, zero-based
	 */
	public void setMainlineIndex(int parentIndex) {
		if (parentIndex >= 0) {
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
				if (commits.length == 1) {
					progress.subTask(MessageFormat.format(
							CoreText.CherryPickOperation_cherryPicking,
							commits[0].name()));
				} else {
					progress.subTask(
							CoreText.CherryPickOperation_cherryPickingMultipleCommits);
				}

				try (Git git = new Git(repo)) {
					CherryPickCommand command = git.cherryPick();
					for (RevCommit commit : commits) {
						command.include(commit.getId());
					}
					MergeStrategy strategy = MergeStrategies
							.getPreferredMergeStrategy();
					if (strategy != null) {
						command.setStrategy(strategy);
					}
					if (parentIndex >= 0) {
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
