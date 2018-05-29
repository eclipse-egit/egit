/*******************************************************************************
 * Copyright (c) 2013 Tomasz Zarna <tomasz.zarna@tasktop.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for renaming or deleting a local branch.
 *
 */
public class BranchEditDialog extends BranchSelectionAndEditDialog {

	/**
	 * @param parentShell
	 * @param repository
	 * @param branchToMark
	 */
	public BranchEditDialog(Shell parentShell, Repository repository,
			String branchToMark) {
		super(parentShell, repository, branchToMark, SHOW_LOCAL_BRANCHES
				| EXPAND_LOCAL_BRANCHES_NODE | SELECT_CURRENT_REF);
	}

	@Override
	protected String getTitle() {
		return UIText.BranchEditDialog_Title;
	}

	/**
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button renameButton = createRenameButton(parent);
		Button deleteButton = createDeleteButton(parent);

		if (branchTree.getSelection().isEmpty()) {
			renameButton.setEnabled(false);
			deleteButton.setEnabled(false);
		}

		createButton(parent, IDialogConstants.CANCEL_ID,
				UIText.BranchSelectionAndEditDialog_OkClose, true);
	}

}
