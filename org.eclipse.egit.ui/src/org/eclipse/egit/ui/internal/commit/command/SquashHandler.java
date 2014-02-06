/*******************************************************************************
 *  Copyright (c) 2014 Maik Schreiber
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Maik Schreiber - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit.command;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.SquashCommitsOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.handler.SelectionHandler;
import org.eclipse.egit.ui.internal.rebase.CommitMessageEditorDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;

/** Handler to reword a commit's message. */
public class SquashHandler extends SelectionHandler {

	/** Command id */
	public static final String ID = "org.eclipse.egit.ui.commit.Squash"; //$NON-NLS-1$

	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RevCommit> commits = getSelectedItems(RevCommit.class, event);
		if ((commits == null) || commits.isEmpty())
			return null;
		Repository repo = getSelectedItem(Repository.class, event);
		if (repo == null)
			return null;

		commits = sortCommits(commits);

		Shell shell = getPart(event).getSite().getShell();
		String message = promptCommitMessage(shell, commits);
		if (message == null)
			return null;

		final SquashCommitsOperation op = new SquashCommitsOperation(repo,
				commits, message);
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

	private String promptCommitMessage(final Shell shell,
			List<RevCommit> commits) {

		StringBuilder buf = new StringBuilder();
		for (RevCommit commit : commits) {
			if (buf.length() > 0)
				buf.append("\n\n"); //$NON-NLS-1$

			buf.append("# ") //$NON-NLS-1$
					.append(MessageFormat.format(UIText.SquashHandler_MessageFromCommitX,
							commit.getId().abbreviate(7).name()))
					.append("\n").append(commit.getFullMessage().trim()); //$NON-NLS-1$
			int length = buf.length();
			while ((length > 0) && (buf.charAt(length - 1) == '\n')) {
				buf.deleteCharAt(length - 1);
				length--;
			}
		}

		final String[] message = { buf.toString() };
		shell.getDisplay().syncExec(new Runnable() {
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

	/**
	 * Sorts commits in parent-first order.
	 *
	 * @param commits
	 *            the commits to sort
	 * @return a new list containing the sorted commits
	 */
	private List<RevCommit> sortCommits(List<RevCommit> commits) {
		Map<RevCommit, RevCommit> parentToChild = new HashMap<RevCommit, RevCommit>();
		RevCommit firstCommit = null;
		for (RevCommit commit : commits) {
			RevCommit parentCommit = commit.getParent(0);
			parentToChild.put(parentCommit, commit);
			if (!commits.contains(parentCommit))
				firstCommit = commit;
		}

		List<RevCommit> sortedCommits = new ArrayList<RevCommit>();
		sortedCommits.add(firstCommit);
		RevCommit parentCommit = firstCommit;
		for (;;) {
			RevCommit childCommit = parentToChild.get(parentCommit);
			if (childCommit == null)
				break;
			sortedCommits.add(childCommit);
			parentCommit = childCommit;
		}

		return sortedCommits;
	}
}
