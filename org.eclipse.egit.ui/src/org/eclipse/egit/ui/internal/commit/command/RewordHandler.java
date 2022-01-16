/*******************************************************************************
 *  Copyright (c) 2014, 2020 Maik Schreiber and others.
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
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit.command;

import java.text.MessageFormat;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.signing.GpgConfigurationException;
import org.eclipse.egit.core.op.RewordCommitOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.handler.SelectionHandler;
import org.eclipse.egit.ui.internal.jobs.GpgConfigProblemReportAction;
import org.eclipse.egit.ui.internal.jobs.RepositoryJob;
import org.eclipse.egit.ui.internal.rebase.CommitMessageEditorDialog;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.CommitConfig;
import org.eclipse.jgit.lib.CommitConfig.CleanupMode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.StringUtils;
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

		String newMessage = promptCommitMessage(shell, repo, commit);
		if (StringUtils.isEmptyOrNull(newMessage)
				|| newMessage.equals(commit.getFullMessage())) {
			return null;
		}

		final RewordCommitOperation op = new RewordCommitOperation(repo,
				commit, newMessage);

		Job job = new RepositoryJob(MessageFormat.format(
				UIText.RewordHandler_JobName, commit.name()), null) {

			private IStatus gpgConfigProblem;

			@Override
			protected IStatus performJob(IProgressMonitor monitor) {
				try {
					op.execute(monitor);
				} catch (CoreException e) {
					IStatus status = e.getStatus();
					if (status
							.getException() instanceof GpgConfigurationException) {
						gpgConfigProblem = e.getStatus();
						// We're going to show our own dialog
						return Status.OK_STATUS;
					}
					return status;
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.REWORD.equals(family))
					return true;
				return super.belongsTo(family);
			}

			@Override
			protected IAction getAction() {
				if (gpgConfigProblem == null || gpgConfigProblem.isOK()) {
					return null;
				}
				return new GpgConfigProblemReportAction(gpgConfigProblem,
						UIText.RewordHandler_GpgConfigProblem);
			}
		};
		job.setUser(true);
		job.setRule(op.getSchedulingRule());
		job.schedule();
		return null;
	}

	private String promptCommitMessage(final Shell shell, Repository repo,
			RevCommit commit) {
		CommitConfig config = repo.getConfig().get(CommitConfig.KEY);
		CleanupMode mode = config.resolve(CleanupMode.DEFAULT, true);
		CommitMessageEditorDialog dialog = new CommitMessageEditorDialog(shell,
				commit.getFullMessage(), mode, '#');
		return dialog.open() == Window.OK ? dialog.getCommitMessage() : null;
	}
}
