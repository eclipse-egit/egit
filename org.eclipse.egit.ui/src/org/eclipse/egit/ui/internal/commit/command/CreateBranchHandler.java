/*******************************************************************************
 *  Copyright (c) 2011, 2019 GitHub Inc. and others
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.internal.IRepositoryCommit;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.repository.CreateBranchWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Create branch handler. Used in the history view and in the commit editor.
 */
public class CreateBranchHandler extends CommitCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<IRepositoryCommit> commits = getSelectedItems(
				IRepositoryCommit.class, event);
		if (commits.size() == 1) {
			IRepositoryCommit commit = commits.get(0);

			List<Ref> branches = getBranches(commit);

			String base;
			if (branches.isEmpty()) {
				base = commit.getRevCommit().getName();
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
				base = branch.getName();

			}
			WizardDialog dlg = new WizardDialog(
					HandlerUtil.getActiveShellChecked(event),
					new CreateBranchWizard(commit.getRepository(), base));
			dlg.setHelpAvailable(false);
			dlg.open();
		}
		return null;
	}

	private List<Ref> getBranches(IRepositoryCommit commit) {
		RevCommit c = commit.getRevCommit();
		if (!(c instanceof PlotCommit)) {
			return Collections.emptyList();
		}
		PlotCommit p = (PlotCommit) c;

		List<Ref> result = new ArrayList<>();
		int refCount = p.getRefCount();
		for (int i = 0; i < refCount; i++) {
			Ref ref = p.getRef(i);
			String refName = ref.getName();
			if (refName.startsWith(Constants.R_HEADS)
					|| refName.startsWith(Constants.R_REMOTES))
				result.add(ref);
		}
		return result;
	}

}
