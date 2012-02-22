/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tomasz Zarna (IBM) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit.command;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.RevertCommitOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;

/**
 * Executes the {@link RevertCommitOperation}
 */
public class RevertHandler extends CommitCommandHandler {

	/**
	 * Command id
	 */
	public static final String ID = "org.eclipse.egit.ui.commit.Revert"; //$NON-NLS-1$

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		List<RepositoryCommit> commits = getCommits(event);
		if (commits.size() == 1) {
			RepositoryCommit repoCommit = commits.get(0);
			final RevCommit commit = repoCommit.getRevCommit();
			Repository repo = repoCommit.getRepository();
			final Shell shell = getPart(event).getSite().getShell();

			final RevertCommitOperation op = new RevertCommitOperation(repo, commit);

			Job job = new Job(MessageFormat.format(UIText.RevertHandler_JobName,
					commit.name())) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						op.execute(monitor);
						RevCommit newHead = op.getNewHead();
						List<Ref> revertedRefs = op.getRevertedRefs();
						if (newHead != null && revertedRefs.isEmpty())
							org.eclipse.egit.ui.internal.history.command.RevertHandler
									.showRevertedDialog(shell);
						if (newHead == null)
							org.eclipse.egit.ui.internal.history.command.RevertHandler
									.showFailureDialog(shell, commit,
											op.getFailingResult());
					} catch (CoreException e) {
						Activator.handleError(UIText.RevertOperation_InternalError,
								e, true);
					}
					return Status.OK_STATUS;
				}

				@Override
				public boolean belongsTo(Object family) {
					if (JobFamilies.REVERT_COMMIT.equals(family))
						return true;
					return super.belongsTo(family);
				}
			};
			job.setUser(true);
			job.setRule(op.getSchedulingRule());
			job.schedule();
			return null;
		}
		return null;
	}
}
