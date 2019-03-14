/*******************************************************************************
 * Copyright (c) 2014, Konrad Kügler <swamblumat-eclipsebugs@yahoo.de> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit.command;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.internal.IRepositoryCommit;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Shows the selected commit in the history view
 */
public class ShowInHistoryHandler extends CommitCommandHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		List<IRepositoryCommit> commits = getCommits(event);
		if (commits.size() == 1) {
			IRepositoryCommit repoCommit = commits.get(0);

			try {
				IHistoryView view = (IHistoryView) PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage()
						.showView(IHistoryView.VIEW_ID);
				view.showHistoryFor(repoCommit);
			} catch (PartInitException e) {
				throw new ExecutionException(e.getMessage(), e);
			}
		}
		return null;
	}
}
