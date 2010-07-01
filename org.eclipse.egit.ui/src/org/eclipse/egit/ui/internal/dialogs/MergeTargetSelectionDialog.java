/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for selecting a merge target.
 *
 */
public class MergeTargetSelectionDialog extends BranchSelectionDialog {

	/**
	 * Construct a dialog to select a branch to reset to or check out
	 *
	 * @param parentShell
	 * @param repo
	 */
	public MergeTargetSelectionDialog(Shell parentShell, FileRepository repo) {
		super(parentShell, repo);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		confirmationBtn = createButton(parent, IDialogConstants.OK_ID,
				UIText.MergeTargetSelectionDialog_ButtonMerge, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected String getMessageText() {
		return UIText.MergeTargetSelectionDialog_SelectRef
				+ " " + UIText.MergeTargetSelectionDialog_OnlyFastForward; //$NON-NLS-1$
	}

	@Override
	protected String getTitle() {
		return UIText.MergeTargetSelectionDialog_TitleMerge;
	}

}
