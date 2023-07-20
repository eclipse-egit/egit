/*******************************************************************************
 * Copyright (C) 2012-2014, Markus Duft <markus.duft@salomon.at> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.push.PushBranchWizard;
import org.eclipse.egit.ui.internal.push.PushWizardDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

/**
 * Command handler to enable pushing commits from the Git History View
 */
public class PushCommitHandler extends AbstractHistoryCommandHandler
		implements IElementUpdater {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		PlotCommit commit = (PlotCommit) getSelection(event).getFirstElement();
		final Repository repo = getRepository(event);

		try {
			PushBranchWizard wizard = null;
			Ref localBranch = findLocalBranch(commit);
			if (localBranch == null) {
				wizard = new PushBranchWizard(repo, commit.getId());
			} else {
				wizard = new PushBranchWizard(repo, localBranch);
			}
			PushWizardDialog dlg = new PushWizardDialog(
					HandlerUtil.getActiveShellChecked(event),
					wizard);
			dlg.setHelpAvailable(true);
			dlg.open();
		} catch (Exception e) {
			Activator.handleError(e.getMessage(), e, true);
		}

		return null;
	}

	private Ref findLocalBranch(PlotCommit commit) {
		for (int i = 0; i < commit.getRefCount(); i++) {
			Ref currentRef = commit.getRef(i);
			if (currentRef.getName().startsWith(Constants.R_HEADS)) {
				return currentRef;
			}
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		GitHistoryPage page = getPage();
		if (page == null)
			return false;
		IStructuredSelection sel = getSelection(page);
		return sel.size() == 1 && sel.getFirstElement() instanceof RevCommit;
	}

	@Override
	public void updateElement(UIElement element, Map parameters) {
		GitHistoryPage page = getPage();
		if (page == null) {
			return;
		}
		IStructuredSelection sel = getSelection(page);
		Object item = (sel.size() == 1) ? sel.getFirstElement() : null;
		Ref ref = null;
		if (item instanceof PlotCommit) {
			ref = findLocalBranch((PlotCommit) item);
		}
		if (ref != null) {
			element.setText(UIText.PushCommitHandler_pushBranchLabel);
		} else {
			element.setText(UIText.PushCommitHandler_pushCommitLabel);
		}
	}
}
