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
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.op.StashDropOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.handler.SelectionHandler;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.StashListCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Handler to delete a stashed commit
 */
public class StashDropHandler extends SelectionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final RevCommit commit = getSelectedItem(RevCommit.class, event);
		if (commit == null)
			return null;
		Repository repo = getSelectedItem(Repository.class, event);
		if (repo == null)
			return null;
		final Shell parent = getPart(event).getSite().getShell();
		final int stashIndex = getStashIndex(repo, commit.getId());

		if (!confirmStashDrop(parent, stashIndex, commit.getShortMessage()))
			return null;

		final StashDropOperation op = new StashDropOperation(repo, stashIndex);

		Job job = new WorkspaceJob(MessageFormat.format(
				UIText.StashDropCommand_jobTitle, commit.name())) {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				try {
					op.execute(monitor);
				} catch (CoreException e) {
					Activator.logError(MessageFormat.format(
							UIText.StashDropCommand_dropFailed, "stash@{" //$NON-NLS-1$
									+ stashIndex + "}"), e); //$NON-NLS-1$
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
		final IWorkbenchPart part = getPart(event);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent changeEvent) {
				if (changeEvent.getResult().isOK()
						&& part instanceof CommitEditor) {
					((CommitEditor) part).close(false);
				}
			}

		});
		job.setUser(true);
		job.setRule(op.getSchedulingRule());
		job.schedule();
		return null;
	}

	private int getStashIndex(Repository repo, ObjectId id)
			throws ExecutionException {
		int index = 0;
		try {
			StashListCommand stashes = new StashListCommand(repo);
			for (RevCommit commit : stashes.call()) {
				if (commit.getId().equals(id)) {
					return index;
				} else {
					index++;
				}
			}
			throw new IllegalStateException(MessageFormat.format(
					UIText.StashDropCommand_stashCommitNotFound, id.name()));
		} catch (Exception e) {
			String message = MessageFormat.format(
					UIText.StashDropCommand_dropFailed, id.name());
			Activator.showError(message, e);
			throw new ExecutionException(message, e);
		}
	}

	private boolean confirmStashDrop(final Shell shell, final int stashIndex,
			String commitMessage) {
		final AtomicBoolean confirmed = new AtomicBoolean(false);

		shell.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				String message = MessageFormat.format(
						UIText.StashDropCommand_confirmSingle,
						Integer.toString(stashIndex), commitMessage);
				String[] buttonLabels = { UIText.StashDropCommand_buttonDelete,
						IDialogConstants.CANCEL_LABEL };

				MessageDialog confirmDialog = new MessageDialog(shell,
						UIText.StashDropCommand_confirmTitle, null, message,
						MessageDialog.CONFIRM, buttonLabels, 0);
				confirmed.set(confirmDialog.open() == Window.OK);
			}
		});
		return confirmed.get();
	}
}
