/*******************************************************************************
 * Copyright (C) 2014, Andreas Hermann <a.v.hermann@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit.command;

import java.text.MessageFormat;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.op.StashApplyOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.handler.SelectionHandler;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Handler to apply the changes in a stashed commit to a repository
 */
public class StashApplyHandler extends SelectionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final RevCommit commit = getSelectedItem(RevCommit.class, event);
		if (commit == null)
			return null;
		Repository repo = getSelectedItem(Repository.class, event);
		if (repo == null)
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
									Utils.getShortObjectId(commit),
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
