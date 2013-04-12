/******************************************************************************
 *  Copyright (c) 2012, 2013 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Stefan Lay (SAP AG)
 *****************************************************************************/
package org.eclipse.egit.ui.internal.stash;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.StashCreateOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
* The UI wrapper for {@link StashCreateOperation} */
public class StashCreateUI {

	private Repository repo;

	/**
	 * @param repo
	 */
	public StashCreateUI(Repository repo) {
		this.repo = repo;
	}

	/**
	 * @param shell
	 *            the shell to use for showing the message input dialog
	 * @return true if a stash create operation was triggered
	 */
	public boolean createStash(Shell shell) {
		InputDialog commitMessageDialog = new InputDialog(shell,
				UIText.StashCreateCommand_titleEnterCommitMessage,
				UIText.StashCreateCommand_messageEnterCommitMessage,
				null, null);
		if (commitMessageDialog.open() != Window.OK)
			return false;
		String message = commitMessageDialog.getValue();
		if (message.length() == 0)
			message = null;

		final StashCreateOperation op = new StashCreateOperation(repo, message);
		Job job = new Job(UIText.StashCreateCommand_jobTitle) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("", 1); //$NON-NLS-1$
				try {
					op.execute(monitor);
					RevCommit commit = op.getCommit();
					if (commit == null)
						showNoChangesToStash();

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
		return true;

	}

	private static void showNoChangesToStash() {
		Display.getDefault().asyncExec(new Runnable() {

			public void run() {
				Shell shell = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell();
				MessageDialog.openInformation(shell,
						UIText.StashCreateCommand_titleNoChanges,
						UIText.StashCreateCommand_messageNoChanges);
			}
		});
	}

}
