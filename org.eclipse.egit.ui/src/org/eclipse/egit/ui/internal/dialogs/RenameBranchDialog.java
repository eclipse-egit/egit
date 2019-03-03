/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Chris Aniszczyk <caniszczyk@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
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
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * The branch and reset selection dialog
 */
public class RenameBranchDialog extends AbstractBranchSelectionDialog {
	/**
	 * Construct a dialog to select a branch to reset to or check out
	 *
	 * @param parentShell
	 * @param repo
	 */
	public RenameBranchDialog(Shell parentShell, Repository repo) {
		super(parentShell, repo, SHOW_LOCAL_BRANCHES | SHOW_REMOTE_BRANCHES
				| EXPAND_LOCAL_BRANCHES_NODE);
	}

	@Override
	protected void okPressed() {
		final Ref toRename = refFromDialog();

		if (toRename != null) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

				@Override
				public void run() {
					BranchRenameDialog dialog = new BranchRenameDialog(
							PlatformUI.getWorkbench().getActiveWorkbenchWindow()
									.getShell(),
							repo, toRename);
					dialog.open();
				}
			});
		}
		super.okPressed();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(Window.OK).setText(
				UIText.RenameBranchDialog_RenameButtonLabel);

		// can't advance without a selection
		getButton(Window.OK).setEnabled(!branchTree.getSelection().isEmpty());
	}

	/**
	 * @return the message shown above the refs tree
	 */
	@Override
	protected String getMessageText() {
		return UIText.RenameBranchDialog_DialogMessage;
	}

	/**
	 * Subclasses may add UI elements
	 *
	 * @param parent
	 */
	@Override
	protected void createCustomArea(Composite parent) {
		// do nothing
	}

	/**
	 * Subclasses may change the title of the dialog
	 *
	 * @return the title of the dialog
	 */
	@Override
	protected String getTitle() {
		return UIText.RenameBranchDialog_DialogTitle;
	}

	@Override
	protected String getWindowTitle() {
		return UIText.RenameBranchDialog_WindowTitle;
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	@Override
	protected void refNameSelected(String refName) {
		boolean branchSelected = refName != null
				&& (refName.startsWith(Constants.R_HEADS) || refName
						.startsWith(Constants.R_REMOTES));

		setOkButtonEnabled(branchSelected);
	}
}
