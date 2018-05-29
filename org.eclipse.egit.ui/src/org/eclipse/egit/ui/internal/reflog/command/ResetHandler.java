/*******************************************************************************
 * Copyright (c) 2014, 2016 Konrad Kügler <swamblumat-eclipsebugs@yahoo.de> and others.
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
package org.eclipse.egit.ui.internal.reflog.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.actions.ResetMenu;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler to reset (soft/mixed/hard) to a reflog entry's commit
 */
public class ResetHandler extends AbstractReflogCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(event);
		String resetMode = event.getParameter(ResetMenu.RESET_MODE);
		RevCommit commit = getSelectedCommit(event, repository);
		if (commit != null)
			ResetMenu.performReset(HandlerUtil.getActiveShellChecked(event),
					repository, commit, ResetType.valueOf(resetMode));
		return null;
	}

}
