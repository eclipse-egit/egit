/*******************************************************************************
 * Copyright (C) 2010, 2016 Mathias Kinzler <mathias.kinzler@sap.com> and others.
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
package org.eclipse.egit.ui.internal.history.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.actions.ResetMenu;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * "Reset" with parameter (hard, mixed, soft).
 */
public class ResetHandler extends AbstractHistoryCommandHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repo = getRepository(event);
		final ObjectId commitId = getSelectedCommitId(event);

		String resetMode = event.getParameter(ResetMenu.RESET_MODE);
		ResetMenu.performReset(HandlerUtil.getActiveShellChecked(event), repo,
				commitId, ResetType.valueOf(resetMode));
		return null;
	}
}
