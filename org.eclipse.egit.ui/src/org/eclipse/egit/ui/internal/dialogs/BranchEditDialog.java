/*******************************************************************************
 * Copyright (c) 2013 Tasktop Technologies
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tomasz Zarna (Tasktop Technologies) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for renaming or deleting a local branch.
 */
public class BranchEditDialog extends LocalBranchSelectionDialog {
	/**
	 * @param shell
	 * @param repository
	 * @param branchToMark
	 */
	public BranchEditDialog(Shell shell, Repository repository,
			String branchToMark) {
		super(shell, repository, branchToMark);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createRenameButton(parent);
		createDeleteButton(parent);

		createButton(parent, IDialogConstants.CANCEL_ID,
				UIText.BranchSelectionAndEditDialog_OkClose, true);
	}
}
