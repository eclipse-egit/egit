/*******************************************************************************
 * Copyright (C) 2011, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.commands.shared.RebaseCurrentRefCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;

/**
 * An action to rebase the current branch on top of given branch. This is the
 * "main" action of the rebase pulldown, see {@link RebaseAction} for the menu
 * items.
 *
 * @see RebaseAction
 */
public class RebaseActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RebaseCurrentRefCommand rebaseCurrent = new RebaseCurrentRefCommand();
		rebaseCurrent.setEnabled(event.getApplicationContext());
		// Because the enabled state is for both starting a new rebase as well
		// as working with an existing rebase, it can be that this is executed
		// even though starting a new rebase is not possible. So check enabled
		// state again here. See also isEnabled.
		if (rebaseCurrent.isEnabled())
			return rebaseCurrent.execute(event);
		else
			return null;
	}

	@Override
	public boolean isEnabled() {
		Repository repo = getRepository();
		if (repo == null)
			return false;

		// Either we want this to be enabled because a new rebase can be started
		// (main action) or an active rebase can be continued, skipped or
		// aborted (menu items). Even when the main action is not enabled we
		// must enable this because otherwise the menu items cannot be opened.
		RepositoryState state = repo.getRepositoryState();
		return state.isRebasing()
				|| RebaseCurrentRefCommand.isEnabledForState(repo, state);
	}
}
