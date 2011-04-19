/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for creating a branch based on a branch, tag, or Reference
 *
 */
public class CreateBranchDialog extends AbstractBranchSelectionDialog {
	/**
	 * @param parentShell
	 * @param repo
	 */
	public CreateBranchDialog(Shell parentShell, Repository repo) {
		super(parentShell, repo, SHOW_LOCAL_BRANCHES | SHOW_REMOTE_BRANCHES
				| SHOW_TAGS | SHOW_REFERENCES | SELECT_CURRENT_REF);
	}

	@Override
	protected String getMessageText() {
		return UIText.CreateBranchDialog_SelectRefMessage;
	}

	@Override
	protected String getTitle() {
		return UIText.CreateBranchDialog_DialogTitle;
	}

	@Override
	protected String getWindowTitle() {
		return UIText.CreateBranchDialog_WindowTitle;
	}

	@Override
	protected void refNameSelected(String refName) {
		getButton(Window.OK).setEnabled(refName != null);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(Window.OK).setText(UIText.CreateBranchDialog_OKButtonText);
	}


}
