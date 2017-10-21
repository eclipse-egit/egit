/*******************************************************************************
 * Copyright (C) 2012-2014, Markus Duft <markus.duft@salomon.at> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.push.PushWizardDialog;
import org.eclipse.egit.ui.internal.push.PushBranchWizard;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Command handler to enable pushing commits from the Git History View
 */
public class PushCommitHandler extends AbstractHistoryCommandHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		PlotCommit commit = (PlotCommit) getSelection(event).getFirstElement();
		final Repository repo = getRepository(event);

		try {
			PushBranchWizard wizard = null;
			Ref localBranch = null;
			for (int i = 0; i < commit.getRefCount(); i++) {
				Ref currentRef = commit.getRef(i);
				if (localBranch == null
						&& currentRef.getName().startsWith(Constants.R_HEADS))
					localBranch = currentRef;
			}
			if (localBranch == null)
				wizard = new PushBranchWizard(repo, commit.getId());
			else
				wizard = new PushBranchWizard(repo, localBranch);
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

	@Override
	public boolean isEnabled() {
		GitHistoryPage page = getPage();
		if (page == null)
			return false;
		IStructuredSelection sel = getSelection(page);
		return sel.size() == 1 && sel.getFirstElement() instanceof RevCommit;
	}
}
