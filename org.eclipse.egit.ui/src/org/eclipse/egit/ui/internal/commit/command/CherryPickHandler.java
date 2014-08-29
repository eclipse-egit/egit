/******************************************************************************
 *  Copyright (c) 2010 SAP AG.
 *  Copyright (c) 2011, 2014 GitHub Inc.
 *  and other copyright owners as documented in the project's IP log.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Maik Schreiber - modify to using interactive rebase mechanics
 *****************************************************************************/
package org.eclipse.egit.ui.internal.commit.command;

import java.io.IOException;
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
import org.eclipse.egit.core.op.CherryPickOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIRepositoryUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.handler.SelectionHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;

/**
 * Handler to cherry pick the commit onto the current branch
 */
public class CherryPickHandler extends SelectionHandler {

	/**
	 * Command id
	 */
	public static final String ID = "org.eclipse.egit.ui.commit.CherryPick"; //$NON-NLS-1$

	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RevCommit> commits = getSelectedItems(RevCommit.class, event);
		if ((commits == null) || commits.isEmpty())
			return null;
		Repository repo = getSelectedItem(Repository.class, event);
		if (repo == null)
			return null;
		final Shell parent = getPart(event).getSite().getShell();

		if (!confirmCherryPick(parent, repo, commits))
			return null;

		try {
			if (!UIRepositoryUtils.handleUncommittedFiles(repo, parent))
				return null;
		} catch (GitAPIException e) {
			Activator.logError(e.getMessage(), e);
			return null;
		}

		final CherryPickOperation op = new CherryPickOperation(repo, commits);
		Job job = new Job(MessageFormat.format(
				UIText.CherryPickHandler_JobName,
				Integer.valueOf(commits.size()))) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					op.execute(monitor);
				} catch (CoreException e) {
					Activator.logError(
							UIText.CherryPickOperation_InternalError, e);
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.CHERRY_PICK.equals(family))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.setRule(op.getSchedulingRule());
		job.schedule();
		return null;
	}

	private boolean confirmCherryPick(final Shell shell,
			final Repository repository, final List<RevCommit> commits)
			throws ExecutionException {
		final AtomicBoolean confirmed = new AtomicBoolean(false);
		final StringBuilder message = new StringBuilder();
		final ObjectReader reader = repository.newObjectReader();
		try {
			message.append(MessageFormat.format(
					UIText.CherryPickHandler_ConfirmMessage,
					Integer.valueOf(commits.size()), repository.getBranch()));
			for (RevCommit c : commits)
				message.append(MessageFormat.format(
						UIText.CherryPickHandler_CommitFormat, reader
								.abbreviate(c.getId(), 7).name(), c
								.getShortMessage()));
		} catch (IOException e) {
			throw new ExecutionException(
					"Exception obtaining current repository branch", e); //$NON-NLS-1$
		} finally {
			if (reader != null)
				reader.release();
		}

		shell.getDisplay().syncExec(new Runnable() {

			public void run() {
				confirmed.set(MessageDialog.openConfirm(shell,
						UIText.CherryPickHandler_ConfirmTitle,
						message.toString()));
			}
		});
		return confirmed.get();
	}
}
