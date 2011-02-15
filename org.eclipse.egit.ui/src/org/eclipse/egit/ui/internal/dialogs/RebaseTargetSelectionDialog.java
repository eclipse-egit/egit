/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Mathias Kinzler (SAP AG) - use the abstract super class
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for selecting a merge target.
 *
 */
public class RebaseTargetSelectionDialog extends AbstractBranchSelectionDialog {

	/**
	 * @param parentShell
	 * @param repo
	 */
	public RebaseTargetSelectionDialog(Shell parentShell, Repository repo) {
		super(parentShell, repo);
		// local and remote branches only
		setRootsToShow(true, true, false, false);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(Window.OK).setText(
				UIText.RebaseTargetSelectionDialog_RebaseButton);
	}

	@Override
	protected String getMessageText() {
		return UIText.RebaseTargetSelectionDialog_DialogMessage;
	}

	@Override
	protected String getTitle() {
		return UIText.RebaseTargetSelectionDialog_DialogTitle;
	}

	@Override
	protected String getWindowTitle() {
		return NLS.bind(UIText.RebaseTargetSelectionDialog_RebaseTitle, repo
				.getDirectory().toString());
	}

	@Override
	protected void refNameSelected(String refName) {
		boolean tagSelected = refName != null
				&& refName.startsWith(Constants.R_TAGS);

		boolean branchSelected = refName != null
				&& (refName.startsWith(Constants.R_HEADS) || refName
						.startsWith(Constants.R_REMOTES));

		boolean currentSelected;
		try {
			currentSelected = refName != null
					&& refName.equals(repo.getFullBranch());
		} catch (IOException e) {
			currentSelected = false;
		}

		getButton(Window.OK).setEnabled(
				!currentSelected && (branchSelected || tagSelected));
	}
}
