/*******************************************************************************
 * Copyright (C) 2010, 2014, Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.ResetOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * "Reset" with parameter (hard, mixed, soft).
 */
public class ResetHandler extends AbstractHistoryCommandHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repo = getRepository(event);
		final RevCommit commit = (RevCommit) getSelection(getPage())
				.getFirstElement();

		String resetMode = event.getParameter(HistoryViewCommands.RESET_MODE);
		performReset(event, repo, commit, resetMode);
		return null;
	}

	/**
	 * @param event
	 * @param repo
	 * @param commit
	 * @param resetMode
	 * @throws ExecutionException
	 */
	public static void performReset(ExecutionEvent event,
			final Repository repo, final RevCommit commit, String resetMode)
			throws ExecutionException {
		final ResetType resetType = ResetType.valueOf(resetMode);

		final String jobName;
		switch (resetType) {
		case HARD:
			if (!MessageDialog.openQuestion(
					HandlerUtil.getActiveShellChecked(event),
					UIText.ResetTargetSelectionDialog_ResetQuestion,
					UIText.ResetTargetSelectionDialog_ResetConfirmQuestion))
				return;

			jobName = UIText.HardResetToRevisionAction_hardReset;
			break;
		case SOFT:
			jobName = UIText.SoftResetToRevisionAction_softReset;
			break;
		case MIXED:
			jobName = UIText.MixedResetToRevisionAction_mixedReset;
			break;
		default:
			return; // other types are currently not used
		}

		ResetOperation operation = new ResetOperation(repo, commit.getName(),
				resetType);
		JobUtil.scheduleUserWorkspaceJob(operation, jobName, JobFamilies.RESET);
	}
}
