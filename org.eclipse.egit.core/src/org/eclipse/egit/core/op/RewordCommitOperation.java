/*******************************************************************************
 *  Copyright (c) 2014 Maik Schreiber
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Maik Schreiber - initial implementation
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.text.MessageFormat;
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
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.TeamException;

/** An operation that rewords a commit's message. */
public class RewordCommitOperation implements IEGitOperation {
	private Repository repository;
	private RevCommit commit;
	private String newMessage;

	/**
	 * Constructs a new reword commit operation.
	 *
	 * @param repository
	 *            the repository to work on
	 * @param commit
	 *            the commit
	 * @param newMessage
	 *            the new message to set for the commit
	 */
	public RewordCommitOperation(Repository repository, RevCommit commit,
			String newMessage) {
		if (commit.getParentCount() != 1) {
			throw new UnsupportedOperationException(
					"commit is expected to have exactly one parent"); //$NON-NLS-1$
		}

		this.repository = repository;
		this.commit = commit;
		this.newMessage = newMessage;
	}

	@Override
	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor = m != null ? m : new NullProgressMonitor();

		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor pm) throws CoreException {
				pm.beginTask("", 2); //$NON-NLS-1$

				pm.subTask(MessageFormat.format(CoreText.RewordCommitOperation_rewording,
						commit.name()));

				InteractiveHandler handler = new InteractiveHandler() {
					@Override
					public void prepareSteps(List<RebaseTodoLine> steps) {
						for (RebaseTodoLine step : steps) {
							if (step.getCommit().prefixCompare(commit) == 0) {
								try {
									step.setAction(RebaseTodoLine.Action.REWORD);
								} catch (IllegalTodoFileModification e) {
									// shouldn't happen
								}
							}
						}
					}

					@Override
					public String modifyCommitMessage(String oldMessage) {
						return newMessage;
					}
				};
				try {
					Git git = new Git(repository);
					git.rebase().setUpstream(commit.getParent(0))
							.runInteractively(handler)
							.setOperation(RebaseCommand.Operation.BEGIN).call();
				} catch (GitAPIException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				}
				pm.worked(1);

				ProjectUtil.refreshValidProjects(
						ProjectUtil.getValidOpenProjects(repository),
						new SubProgressMonitor(pm, 1));

				pm.done();
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
