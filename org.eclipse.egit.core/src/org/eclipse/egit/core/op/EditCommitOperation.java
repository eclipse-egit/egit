/*******************************************************************************
 *  Copyright (c) 2014 Maik Schreiber
 *  Copyright (C) 2015, Stephan Hackstedt <stephan.hackstedt@googlemail.com>
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Maik Schreiber - initial implementation
 *    Stephan Hackstedt - Bug 477695
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

/** An operation that edits an existing commit. */
public class EditCommitOperation implements IEGitOperation {
	private Repository repository;
	private RevCommit commit;

	/**
	 * Constructs a new edit commit operation.
	 *
	 * @param repository
	 *            the repository to work on
	 * @param commit
	 *            the commit
	 */
	public EditCommitOperation(Repository repository, RevCommit commit) {
		if (commit.getParentCount() != 1)
			throw new UnsupportedOperationException(
					"commit is expected to have exactly one parent"); //$NON-NLS-1$

		this.repository = repository;
		this.commit = commit;
	}

	@Override
	public void execute(IProgressMonitor m) throws CoreException {

		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor pm) throws CoreException {
				SubMonitor progress = SubMonitor.convert(pm, 2);

				progress.subTask(MessageFormat.format(
						CoreText.EditCommitOperation_editing,
						commit.name()));

				InteractiveHandler handler = new InteractiveHandler() {
					@Override
					public void prepareSteps(List<RebaseTodoLine> steps) {
						for (RebaseTodoLine step : steps) {
							if (step.getCommit().prefixCompare(commit) == 0) {
								try {
									step.setAction(RebaseTodoLine.Action.EDIT);
								} catch (IllegalTodoFileModification e) {
									// shouldn't happen
								}
							}
						}
					}

					@Override
					public String modifyCommitMessage(String oldMessage) {
						return oldMessage;
					}
				};
				try (Git git = new Git(repository)) {
					git.rebase().setUpstream(commit.getParent(0))
							.runInteractively(handler)
							.setOperation(RebaseCommand.Operation.BEGIN).call();
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
