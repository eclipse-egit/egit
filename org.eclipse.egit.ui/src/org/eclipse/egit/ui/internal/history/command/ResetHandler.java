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
import org.eclipse.egit.ui.internal.actions.ResetMenu;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * "Reset" with parameter (hard, mixed, soft).
 */
public class ResetHandler extends AbstractHistoryCommandHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repo = getRepository(event);
		final ObjectId commitId = getSelectedCommitId(event);

		String resetMode = event.getParameter(ResetMenu.RESET_MODE);
		ResetMenu.performReset(event, repo, commitId, resetMode);
		return null;
	}
}
