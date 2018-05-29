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
 *    Stephan Hackstedt <stephan.hackstedt@googlemail.com> - bug 477695
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
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.RevertCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.TeamException;

/**
 * Operation to revert a commit
 */
public class RevertCommitOperation implements IEGitOperation {

	private final Repository repo;

	private final List<RevCommit> commits;

	private RevCommit newHead;

	private List<Ref> reverted;

	private MergeResult result;

	/**
	 * Create revert commit operation
	 *
	 * @param repository
	 * @param commits
	 *            the commits to revert (in newest-first order)
	 */
	public RevertCommitOperation(Repository repository, List<RevCommit> commits) {
		this.repo = repository;
		this.commits = commits;
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

	@Override
	public void execute(IProgressMonitor m) throws CoreException {
		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor pm) throws CoreException {
				SubMonitor progress = SubMonitor.convert(pm, 2);
				progress.subTask(MessageFormat.format(
						CoreText.RevertCommitOperation_reverting,
						Integer.valueOf(commits.size())));
				try (Git git = new Git(repo)) {
					RevertCommand command = git.revert();
					MergeStrategy strategy = Activator.getDefault()
							.getPreferredMergeStrategy();
					if (strategy != null) {
						command.setStrategy(strategy);
					}
					for (RevCommit commit : commits) {
						command.include(commit);
					}
					newHead = command.call();
					reverted = command.getRevertedRefs();
					result = command.getFailingResult();
					progress.worked(1);
					ProjectUtil.refreshValidProjects(
							ProjectUtil.getValidOpenProjects(repo),
							progress.newChild(1));
				} catch (GitAPIException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				}
			}
		};
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE, m);
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRule(repo);
	}

	/**
	 * Get failing result of merge
	 *
	 * @return merge result
	 */
	public MergeResult getFailingResult() {
		return result;
	}
}
