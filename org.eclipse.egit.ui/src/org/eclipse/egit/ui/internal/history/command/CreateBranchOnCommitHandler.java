/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2013, Tomasz Zarna <tomasz.zarna@tasktop.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.repository.CreateBranchWizard;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Create a branch based on a commit.
 */
public class CreateBranchOnCommitHandler extends AbstractHistoryCommandHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repo = getRepository(event);
		IStructuredSelection selection = getSelection(event);

		IWizard wiz = null;
		List<Ref> branches = getBranches(selection, repo);

		if (branches.isEmpty()) {
			PlotCommit commit = (PlotCommit) selection
					.getFirstElement();
			wiz = new CreateBranchWizard(repo, commit.name());
		} else {
			// prefer to create new branch based on a remote tracking branch
			Collections.sort(branches, new Comparator<Ref>() {

				@Override
				public int compare(Ref o1, Ref o2) {
					String refName1 = o1.getName();
					String refName2 = o2.getName();
					if (refName1.startsWith(Constants.R_REMOTES)) {
						if (refName2.startsWith(Constants.R_HEADS))
							return -1;
						else
							return CommonUtils.STRING_ASCENDING_COMPARATOR
									.compare(refName1, refName2);
					} else {
						if (refName2.startsWith(Constants.R_REMOTES))
							return 1;
						else
							return CommonUtils.STRING_ASCENDING_COMPARATOR
									.compare(refName1, refName2);
					}
				}
			});
			Ref branch = branches.get(0).getLeaf();
			wiz = new CreateBranchWizard(repo, branch.getName());
		}

		WizardDialog dlg = new WizardDialog(
				HandlerUtil.getActiveShellChecked(event), wiz);
		dlg.setHelpAvailable(false);
		dlg.open();
		return null;
	}

	private List<Ref> getBranches(IStructuredSelection selection,
			Repository repo) {
		try {
			return getBranchesOfCommit(selection, repo, false);
		} catch (IOException e) {
			// ignore, use commit name
			return Collections.<Ref> emptyList();
		}
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
