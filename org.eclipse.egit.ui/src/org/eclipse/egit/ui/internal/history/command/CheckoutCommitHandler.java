/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.egit.ui.internal.dialogs.BranchSelectionDialog;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Check out of a commit.
 */
public class CheckoutCommitHandler extends AbstractHistoryCommandHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RevCommit commit = (RevCommit) getSelection(getPage()).getFirstElement();
		Repository repo = getRepository(event);

		final BranchOperationUI op;

		List<RefNode> nodes = getRefNodes(commit, repo, Constants.R_HEADS,
				Constants.R_REMOTES);

		if (nodes.isEmpty())
			op = BranchOperationUI.checkout(repo, commit.name());
		else if (nodes.size() == 1)
			op = BranchOperationUI.checkout(repo, nodes.get(0).getObject().getName());
		else {
			BranchSelectionDialog<RefNode> dlg = new BranchSelectionDialog<RefNode>(
					HandlerUtil.getActiveShellChecked(event), nodes,
					UIText.CheckoutHandler_SelectBranchTitle,
					UIText.CheckoutHandler_SelectBranchMessage, SWT.SINGLE);
			if (dlg.open() == Window.OK) {
				op = BranchOperationUI.checkout(repo, dlg.getSelectedNode()
						.getObject().getName());
			} else {
				op = null;
			}
		}

		if (op == null)
			return null;

		op.start();
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
