/*******************************************************************************
 * Copyright (C) 2007, David Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryView;

/**
 *	Compare the resources filtered in the history view with the current
 *	revision.
 */
public class CompareWithRevisionAction extends TeamAction {

	@Override
	public void execute(IAction action) {
		IHistoryView view = TeamUI.showHistoryFor(TeamUIPlugin.getActivePage(), getSelectedResources()[0], null);
		if (view == null)
			return;
		IHistoryPage page = view.getHistoryPage();
		if (page instanceof GitHistoryPage){
			GitHistoryPage gitHistoryPage = (GitHistoryPage) page;
			gitHistoryPage.setCompareMode(true);
		}
	}

	public boolean isEnabled() {
		return !getSelection().isEmpty();
	}
}
