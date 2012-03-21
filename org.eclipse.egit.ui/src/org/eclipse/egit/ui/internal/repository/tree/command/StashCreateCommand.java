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

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.StashCreateOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Command to stash current changes in working directory and index
 */
public class StashCreateCommand extends
		RepositoriesViewCommandHandler<RepositoryNode> {

	/**
	 * Command id
	 */
	public static final String ID = "org.eclipse.egit.ui.team.stash.create"; //$NON-NLS-1$

	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RepositoryNode> nodes = getSelectedNodes(event);
		if (nodes.isEmpty())
			return null;
		Repository repo = nodes.get(0).getRepository();
		if (repo == null)
			return null;

		final StashCreateOperation op = new StashCreateOperation(repo);
		final Shell shell = HandlerUtil.getActiveShell(event);
		Job job = new Job(UIText.StashCreateCommand_jobTitle) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("", 1); //$NON-NLS-1$
				try {
					op.execute(monitor);
					RevCommit commit = op.getCommit();
					if (commit == null)
						showNoChangesToStash(shell);

				} catch (CoreException e) {
					Activator
							.logError(UIText.StashCreateCommand_stashFailed, e);
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

	private void showNoChangesToStash(final Shell shell) {
		shell.getDisplay().asyncExec(new Runnable() {

			public void run() {
				MessageDialog.openInformation(shell,
						UIText.StashCreateCommand_titleNoChanges,
						UIText.StashCreateCommand_messageNoChanges);
			}
		});
	}
}
