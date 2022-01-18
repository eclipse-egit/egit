/*******************************************************************************
 * Copyright (c) 2014, 2019 Maik Schreiber
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Maik Schreiber - initial implementation
 *    Simon Muschel <smuschel@gmx.de> - Bug 451817
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
import org.eclipse.egit.core.CommitUtil;
import org.eclipse.egit.core.op.SquashCommitsOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIRepositoryUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.handler.SelectionHandler;
import org.eclipse.egit.ui.internal.rebase.CommitMessageEditorDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler2;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.CommitConfig.CleanupMode;
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/** Handler to squash multiple commits into one. */
public class SquashHandler extends SelectionHandler {

	/** Command id */
	public static final String ID = "org.eclipse.egit.ui.commit.Squash"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RevCommit> commits = getSelectedItems(RevCommit.class, event);
		if ((commits == null) || commits.isEmpty())
			return null;
		Repository repo = getSelectedItem(Repository.class, event);
		if (repo == null)
			return null;

		commits = CommitUtil.sortCommits(commits);

		final Shell shell = getPart(event).getSite().getShell();

		try {
			if (!UIRepositoryUtils.handleUncommittedFiles(repo, shell))
				return null;
		} catch (GitAPIException e) {
			Activator.logError(e.getMessage(), e);
			return null;
		}

		InteractiveHandler2 messageHandler = new InteractiveHandler2() {

			@Override
			public void prepareSteps(List<RebaseTodoLine> steps) {
				// not used
			}

			@Override
			public ModifyResult editCommitMessage(String message,
					CleanupMode mode, char commentChar) {
				String edited = promptCommitMessage(message, mode, commentChar);
				return new ModifyResult() {

					@Override
					public String getMessage() {
						return edited == null ? "" : edited; //$NON-NLS-1$
					}

					@Override
					public CleanupMode getCleanupMode() {
						return CleanupMode.VERBATIM;
					}
				};
			}
		};

		final SquashCommitsOperation op = new SquashCommitsOperation(repo,
				commits, messageHandler);
		Job job = new Job(MessageFormat.format(UIText.SquashHandler_JobName,
				Integer.valueOf(commits.size()))) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					op.execute(monitor);
				} catch (CoreException e) {
					Activator.logError(UIText.SquashHandler_InternalError, e);
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.SQUASH.equals(family))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.setRule(op.getSchedulingRule());
		job.schedule();
		return null;
	}

	private String promptCommitMessage(String message, CleanupMode mode,
			char commentChar) {
		String[] msg = { message };
		PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
			Shell shell = PlatformUI.getWorkbench()
					.getModalDialogShellProvider().getShell();
			CommitMessageEditorDialog dialog = new CommitMessageEditorDialog(
					shell, msg[0], mode, commentChar,
					UIText.CommitMessageEditorDialog_OkButton,
					UIText.SquashHandler_EditMessageDialogCancelButton);
			if (dialog.open() == Window.OK) {
				msg[0] = dialog.getCommitMessage();
			}
		});
		return msg[0];
	}
}
