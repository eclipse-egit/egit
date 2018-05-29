/*******************************************************************************
 * Copyright (c) 2014, 2015 Maik Schreiber and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Maik Schreiber - initial implementation
 *    Laurent Delaigue (Obeo) - use of preferred merge strategy
 *    Stephan Hackstedt <stephan.hackstedt@googlemail.com - Bug 477695
 *******************************************************************************/
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
import org.eclipse.egit.core.CommitUtil;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IllegalTodoFileModification;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.TeamException;

/** Squashes multiple commits into one. */
public class SquashCommitsOperation implements IEGitOperation {
	private Repository repository;

	private List<RevCommit> commits;

	private InteractiveHandler messageHandler;

	/**
	 * Constructs a new squash commits operation.
	 *
	 * @param repository
	 *            the repository to work on
	 * @param commits
	 *            the commits
	 * @param messageHandler
	 *            handler that will be used to prompt for a commit message
	 */
	public SquashCommitsOperation(Repository repository,
			List<RevCommit> commits, InteractiveHandler messageHandler) {
		this.repository = repository;
		this.commits = CommitUtil.sortCommits(commits);
		this.messageHandler = messageHandler;
	}

	@Override
	public void execute(IProgressMonitor m) throws CoreException {

		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor pm) throws CoreException {
				SubMonitor progress = SubMonitor.convert(pm, 2);

				progress.subTask(MessageFormat.format(
						CoreText.SquashCommitsOperation_squashing,
						Integer.valueOf(commits.size())));

				InteractiveHandler handler = new InteractiveHandler() {
					@Override
					public void prepareSteps(List<RebaseTodoLine> steps) {
						RevCommit firstCommit = commits.get(0);
						for (RebaseTodoLine step : steps) {
							if (isRelevant(step.getCommit())) {
								try {
									if (step.getCommit().prefixCompare(
											firstCommit) == 0)
										step.setAction(RebaseTodoLine.Action.PICK);
									else
										step.setAction(RebaseTodoLine.Action.SQUASH);
								} catch (IllegalTodoFileModification e) {
									// shouldn't happen
								}
							}
						}
					}

					private boolean isRelevant(AbbreviatedObjectId id) {
						for (RevCommit commit : commits) {
							if (id.prefixCompare(commit) == 0)
								return true;
						}
						return false;
					}

					@Override
					public String modifyCommitMessage(String oldMessage) {
						return messageHandler.modifyCommitMessage(oldMessage);
					}
				};
				try (Git git = new Git(repository)) {
					RebaseCommand command = git.rebase()
							.setUpstream(commits.get(0).getParent(0))
							.runInteractively(handler)
							.setOperation(RebaseCommand.Operation.BEGIN);
					MergeStrategy strategy = Activator.getDefault()
							.getPreferredMergeStrategy();
					if (strategy != null) {
						command.setStrategy(strategy);
					}
					command.call();
				} catch (GitAPIException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				}
				progress.worked(1);

				ProjectUtil.refreshValidProjects(
						ProjectUtil.getValidOpenProjects(repository),
						progress.newChild(1));
			}
		};
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE, m);
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRule(repository);
	}
}
