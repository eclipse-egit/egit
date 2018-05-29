/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.DeleteBranchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for deleting a branch
 *
 */
public class DeleteBranchDialog extends AbstractBranchSelectionDialog {

	private final Set<Ref> selectedRefs = new HashSet<>();
	private String currentBranch;

	/**
	 * @param parentShell
	 * @param repo
	 */
	public DeleteBranchDialog(Shell parentShell, Repository repo) {
		super(parentShell, repo, SHOW_LOCAL_BRANCHES
				| EXPAND_LOCAL_BRANCHES_NODE | SHOW_REMOTE_BRANCHES
				| ALLOW_MULTISELECTION);
		try {
			currentBranch = repo.getFullBranch();
		} catch (IOException e) {
			// just ignore here
		}
	}

	@Override
	protected String getMessageText() {
		return UIText.DeleteBranchDialog_DialogMessage;
	}

	@Override
	protected String getTitle() {
		return UIText.DeleteBranchDialog_DialogTitle;
	}

	@Override
	protected String getWindowTitle() {
		return UIText.DeleteBranchDialog_WindowTitle;
	}

	@Override
	protected String refNameFromDialog() {
		selectedRefs.clear();
		Set<String> selected = new HashSet<>();
		IStructuredSelection selection = (IStructuredSelection) branchTree.getSelection();
		for (Object sel : selection.toArray()) {
			if (!(sel instanceof RefNode))
				continue;

			RefNode node = (RefNode) sel;
			Ref ref = node.getObject();
			selectedRefs.add(ref);
			selected.add(ref.getName());
		}

		boolean enabled = !selected.isEmpty()
				&& !selected.contains(currentBranch);
		getButton(Window.OK).setEnabled(enabled);

		return null;
	}

	@Override
	protected void refNameSelected(String refName) {
		// unused
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == Window.OK) {
			try {
				int result = deleteBranch(selectedRefs, false);
				if (result == DeleteBranchOperation.REJECTED_UNMERGED) {
					List<RefNode> nodes = extractSelectedRefNodes();

					MessageDialog messageDialog = new UnmergedBranchDialog<>(
							getShell(), nodes);

					if (messageDialog.open() == Window.OK)
						deleteBranch(selectedRefs, true);
					else
						return;
				} else if (result == DeleteBranchOperation.REJECTED_CURRENT)
					Activator
							.handleError(
									UIText.DeleteBranchCommand_CannotDeleteCheckedOutBranch,
									null, true);
			} catch (CoreException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
		}

		super.buttonPressed(buttonId);
	}

	private int deleteBranch(final Set<Ref> ref, boolean force) throws CoreException {
		DeleteBranchOperation dbop = new DeleteBranchOperation(repo, ref, force);
		dbop.execute(null);
		return dbop.getStatus();
	}

	private List<RefNode> extractSelectedRefNodes() {
		List<RefNode> nodes = new ArrayList<>();
		Object[] array = ((IStructuredSelection) super.branchTree
				.getSelection()).toArray();

		for (Object selected : array)
			if (selected instanceof RefNode)
				nodes.add((RefNode) selected);

		return nodes;
	}

}
