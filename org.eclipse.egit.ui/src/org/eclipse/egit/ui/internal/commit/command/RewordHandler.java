/*******************************************************************************
 *  Copyright (c) 2014 Maik Schreiber
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Maik Schreiber - initial implementation
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
import org.eclipse.egit.core.op.RewordCommitOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIRepositoryUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.handler.SelectionHandler;
import org.eclipse.egit.ui.internal.rebase.CommitMessageEditorDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;

/** Handler to reword a commit's message. */
public class RewordHandler extends SelectionHandler {

	/** Command id */
	public static final String ID = "org.eclipse.egit.ui.commit.Reword"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RevCommit commit = getSelectedItem(RevCommit.class, event);
		if (commit == null)
			return null;
		Repository repo = getSelectedItem(Repository.class, event);
		if (repo == null)
			return null;

		Shell shell = getPart(event).getSite().getShell();

		try {
			if (!UIRepositoryUtils.handleUncommittedFiles(repo, shell))
				return null;
		} catch (GitAPIException e) {
			Activator.logError(e.getMessage(), e);
			return null;
		}

		String newMessage = promptCommitMessage(shell, commit);
		if (newMessage == null)
			return null;

		final RewordCommitOperation op = new RewordCommitOperation(repo,
				commit, newMessage);

		Job job = new WorkspaceJob(MessageFormat.format(
				UIText.RewordHandler_JobName,
				commit.name())) {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				try {
					op.execute(monitor);
				} catch (CoreException e) {
					Activator.logError(UIText.RewordHandler_InternalError, e);
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.REWORD.equals(family))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.setRule(op.getSchedulingRule());
		job.schedule();
		return null;
	}

	private String promptCommitMessage(final Shell shell, RevCommit commit) {
		final String[] message = { commit.getFullMessage() };
		shell.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				CommitMessageEditorDialog dialog = new CommitMessageEditorDialog(
						shell, message[0]);
				if (dialog.open() == Window.OK)
					message[0] = dialog.getCommitMessage();
				else
					message[0] = null;
			}
		});
		return message[0];
	}
}
