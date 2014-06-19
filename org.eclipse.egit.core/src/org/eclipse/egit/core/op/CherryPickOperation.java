/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Maik Schreiber - modify to using interactive rebase mechanics
 *****************************************************************************/
package org.eclipse.egit.core.op;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IllegalTodoFileModification;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.team.core.TeamException;

/**
 * Cherry pick operation
 */
public class CherryPickOperation implements IEGitOperation {
	private final Repository repo;

	private List<RevCommit> commits;

	/**
	 * Create cherry pick operation
	 *
	 * @param repository
	 *            the repository to work on
	 * @param commits
	 *            the commits in newest-first order
	 */
	public CherryPickOperation(Repository repository, List<RevCommit> commits) {
		this.repo = repository;
		this.commits = commits;
	}

	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor = m != null ? m : new NullProgressMonitor();

		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			public void run(IProgressMonitor pm) throws CoreException {
				pm.beginTask("", 2); //$NON-NLS-1$

				pm.subTask(MessageFormat.format(
						CoreText.CherryPickOperation_cherryPicking,
						Integer.valueOf(commits.size())));

				InteractiveHandler handler = new InteractiveHandler() {
					public void prepareSteps(List<RebaseTodoLine> steps) {
						System.out
								.printf("CherryPickOperation.execute(...).new IWorkspaceRunnable() {...}.run(...).new InteractiveHandler() {...}.prepareSteps()") //$NON-NLS-1$
								.println();

						for (RebaseTodoLine step : steps) {
							try {
								step.setAction(RebaseTodoLine.Action.PICK);
							} catch (IllegalTodoFileModification e) {
								// shouldn't happen
							}
						}

						// apply steps in the chronological order
						List<RevCommit> stepCommits = new ArrayList<RevCommit>(
								commits);
						Collections.reverse(stepCommits);

						for (RevCommit commit : stepCommits) {
							RebaseTodoLine step = new RebaseTodoLine(
									RebaseTodoLine.Action.PICK,
									commit.abbreviate(7), ""); //$NON-NLS-1$
							steps.add(step);
						}
					}

					public String modifyCommitMessage(String oldMessage) {
						return oldMessage;
					}
				};
				try {
					Git git = new Git(repo);
					ObjectId headCommitId = repo.resolve(Constants.HEAD);
					RevCommit headCommit = new RevWalk(repo)
							.parseCommit(headCommitId);
					git.rebase().setUpstream(headCommit.getParent(0))
							.runInteractively(handler)
							.setOperation(RebaseCommand.Operation.BEGIN).call();
				} catch (GitAPIException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				} catch (IOException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				}
				pm.worked(1);

				ProjectUtil.refreshValidProjects(
						ProjectUtil.getValidOpenProjects(repo),
						new SubProgressMonitor(pm, 1));

				pm.done();
			}
		};
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE, monitor);
	}

	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRule(repo);
	}
}
