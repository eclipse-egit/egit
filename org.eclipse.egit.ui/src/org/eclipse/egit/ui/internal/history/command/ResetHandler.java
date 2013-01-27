/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
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

		String type = event.getParameter(HistoryViewCommands.RESET_MODE);
		final ResetType resetType;

		if (type.equals("Hard")) { //$NON-NLS-1$
			resetType = ResetType.HARD;
		} else if (type.equals("Mixed")) { //$NON-NLS-1$
			resetType = ResetType.MIXED;
		} else if (type.equals("Soft")) { //$NON-NLS-1$
			resetType = ResetType.SOFT;
		} else {
			throw new ExecutionException("Could not determine the reset type"); //$NON-NLS-1$ TODO
		}

		String jobName = "Reset"; //$NON-NLS-1$
		switch (resetType) {
		case HARD:
			if (!MessageDialog.openQuestion(HandlerUtil
					.getActiveShellChecked(event),
					UIText.ResetTargetSelectionDialog_ResetQuestion,
					UIText.ResetTargetSelectionDialog_ResetConfirmQuestion))
				return null;

			jobName = UIText.HardResetToRevisionAction_hardReset;
			break;
		case SOFT:
			jobName = UIText.SoftResetToRevisionAction_softReset;
			break;
		case MIXED:
			jobName = UIText.MixedResetToRevisionAction_mixedReset;
			break;
		}

		ResetOperation operation = new ResetOperation(repo, commit.getName(), resetType);
		JobUtil.scheduleUserJob(operation, jobName, JobFamilies.RESET);
		return null;
	}
}
