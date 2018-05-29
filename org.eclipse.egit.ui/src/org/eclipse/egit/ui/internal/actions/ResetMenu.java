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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.ResetOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CommandConfirmation;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

/**
 * Common code used by reset popup menus
 */
public class ResetMenu {

	/** "Reset" (with parameter {@link #RESET_MODE}) */
	public static final String RESET = "org.eclipse.egit.ui.history.Reset"; //$NON-NLS-1$

	/** "Reset" mode (soft, mixed, hard) */
	public static final String RESET_MODE = "org.eclipse.egit.ui.history.ResetMode"; //$NON-NLS-1$

	/**
	 * @param site
	 * @return a new menu manager representing the "Reset" submenu
	 */
	public static MenuManager createMenu(IWorkbenchSite site) {
		MenuManager resetManager = new MenuManager(
				UIText.GitHistoryPage_ResetMenuLabel, UIIcons.RESET,
				"Reset"); //$NON-NLS-1$

		Map<String, String> parameters = new HashMap<>();
		parameters.put(RESET_MODE, ResetType.SOFT.name());
		resetManager.add(getCommandContributionItem(RESET,
				UIText.GitHistoryPage_ResetSoftMenuLabel, parameters, site));

		parameters = new HashMap<>();
		parameters.put(RESET_MODE, ResetType.MIXED.name());
		resetManager.add(getCommandContributionItem(RESET,
				UIText.GitHistoryPage_ResetMixedMenuLabel, parameters, site));

		parameters = new HashMap<>();
		parameters.put(RESET_MODE, ResetType.HARD.name());
		resetManager.add(getCommandContributionItem(RESET,
				UIText.GitHistoryPage_ResetHardMenuLabel, parameters, site));
		return resetManager;
	}

	private static CommandContributionItem getCommandContributionItem(
			String commandId, String menuLabel, Map<String, String> parameters,
			IWorkbenchSite site) {
		CommandContributionItemParameter parameter = new CommandContributionItemParameter(
				site, commandId, commandId, CommandContributionItem.STYLE_PUSH);
		parameter.label = menuLabel;
		parameter.parameters = parameters;
		return new CommandContributionItem(parameter);
	}

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
