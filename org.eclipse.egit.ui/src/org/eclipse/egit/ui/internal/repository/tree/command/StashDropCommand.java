/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.op.StashDropOperation;
import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.StashedCommitNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;

/**
 * Command to drop one or all stashed commits
 */
public class StashDropCommand extends
		RepositoriesViewCommandHandler<StashedCommitNode> {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<StashedCommitNode> nodes = getSelectedNodes(event);
		if (nodes.isEmpty())
			return null;

		StashedCommitNode node = nodes.get(0);
		final Repository repo = node.getRepository();
		if (repo == null)
			return null;
		final int index = node.getIndex();
		if (index < 0)
			return null;
		final RevCommit commit = node.getObject();
		if (commit == null)
			return null;

		// Confirm deletion of selected tags
		final AtomicBoolean confirmed = new AtomicBoolean();
		final Shell shell = getActiveShell(event);
		shell.getDisplay().syncExec(new Runnable() {

			public void run() {
				confirmed.set(MessageDialog.openConfirm(shell,
						UIText.StashDropCommand_confirmTitle, MessageFormat
								.format(UIText.StashDropCommand_confirmMessage,
										Integer.toString(index))));
			}
		});
		if (!confirmed.get())
			return null;

		final StashDropOperation op = new StashDropOperation(repo, index);
		Job job = new Job(MessageFormat.format(
				UIText.StashDropCommand_jobTitle, commit.name())) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					op.execute(monitor);
				} catch (CoreException e) {
					Activator.logError(MessageFormat.format(
							UIText.StashDropCommand_dropFailed, commit.name()),
							e);
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
