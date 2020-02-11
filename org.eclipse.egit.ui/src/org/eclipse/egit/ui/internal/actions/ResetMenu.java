/*******************************************************************************
 * Copyright (C) 2014, 2016 Konrad Kügler <swamblumat-eclipsebugs@yahoo.de> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 495777
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.ResetOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CommandConfirmation;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;

/**
 * Common code used by reset popup menus
 */
public class ResetMenu {

	/** "Reset" mode parameter ID (soft, mixed, hard) */
	public static final String RESET_MODE = "org.eclipse.egit.ui.history.ResetMode"; //$NON-NLS-1$

	/**
	 * @param shell
	 * @param repo
	 * @param commitId
	 * @param resetType
	 */
	public static void performReset(Shell shell,
			final Repository repo, final ObjectId commitId,
			ResetType resetType) {

		final String jobName;
		switch (resetType) {
		case HARD:
			if (!CommandConfirmation.confirmHardReset(shell, repo)) {
				return;
			}
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

		ResetOperation operation = new ResetOperation(repo, commitId.getName(),
				resetType);
		JobUtil.scheduleUserWorkspaceJob(operation, jobName, JobFamilies.RESET);
	}
}
