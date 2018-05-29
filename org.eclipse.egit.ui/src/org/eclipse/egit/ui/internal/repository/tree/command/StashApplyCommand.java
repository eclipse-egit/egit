/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.StashApplyOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.StashedCommitNode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Command to apply the changes in a stashed commit to a repository
 */
public class StashApplyCommand extends
		RepositoriesViewCommandHandler<StashedCommitNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<StashedCommitNode> nodes = getSelectedNodes(event);
		if (nodes.isEmpty())
			return null;
		Repository repo = nodes.get(0).getRepository();
		if (repo == null)
			return null;
		final RevCommit commit = nodes.get(0).getObject();
		if (commit == null)
			return null;

		final StashApplyOperation op = new StashApplyOperation(repo, commit);
		Job job = new WorkspaceJob(MessageFormat.format(
				UIText.StashApplyCommand_jobTitle, commit.name())) {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				try {
					op.execute(monitor);
				} catch (CoreException e) {
					return new Status(IStatus.ERROR, Activator.getPluginId(),
							MessageFormat.format(
									UIText.StashApplyCommand_applyFailed,
									commit.abbreviate(7).name(),
									e.getLocalizedMessage()), e);
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.STASH.equals(family))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.setRule(op.getSchedulingRule());
		job.schedule();
		return null;
	}
}
