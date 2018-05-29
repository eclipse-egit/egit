/*******************************************************************************
 *  Copyright (c) 2014 Maik Schreiber and others.
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
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.EditCommitOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIRepositoryUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.handler.SelectionHandler;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;

/** Handler to edit an existing commit. */
public class EditHandler extends SelectionHandler {

	/** Command id */
	public static final String ID = "org.eclipse.egit.ui.commit.Edit"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RevCommit commit = getSelectedItem(RevCommit.class, event);
		if (commit == null)
			return null;
		Repository repo = getSelectedItem(Repository.class, event);
		if (repo == null)
			return null;

		Shell shell = getPart(event).getSite().getShell();

		editCommit(commit, repo, shell);
		return null;
	}

	/**
	 * @param commit
	 * @param repo
	 * @param shell
	 * @return true, if edit was started, false if user aborted
	 */
	public static boolean editCommit(RevCommit commit, Repository repo,
			Shell shell) {
		try {
			if (!UIRepositoryUtils.handleUncommittedFiles(repo, shell))
				return false;
		} catch (GitAPIException e) {
			Activator.logError(e.getMessage(), e);
			return false;
		}

		final EditCommitOperation op = new EditCommitOperation(repo, commit);

		JobUtil.scheduleUserWorkspaceJob(
				op,
				MessageFormat.format(UIText.EditHandler_JobName, commit.name()),
				JobFamilies.EDIT);
		return true;
	}
}
