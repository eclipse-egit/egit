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
import java.text.MessageFormat;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for selecting a merge target.
 *
 */
public class MergeTargetSelectionDialog extends AbstractBranchSelectionDialog {

	/**
	 * @param parentShell
	 * @param repo
	 */
	public MergeTargetSelectionDialog(Shell parentShell, Repository repo) {
		super(parentShell, repo, getMergeTarget(repo), SHOW_LOCAL_BRANCHES
				| SHOW_REMOTE_BRANCHES | SHOW_TAGS | EXPAND_LOCAL_BRANCHES_NODE
				| getSelectSetting(repo));
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(Window.OK).setText(
				UIText.MergeTargetSelectionDialog_ButtonMerge);
	}

	@Override
	protected String getMessageText() {
		String branch = getCurrentBranch();
		if (branch != null)
			return MessageFormat.format(
					UIText.MergeTargetSelectionDialog_SelectRefWithBranch,
					branch);
		else
			return UIText.MergeTargetSelectionDialog_SelectRef;
	}

	@Override
	protected String getTitle() {
		String branch = getCurrentBranch();
		if (branch != null)
			return MessageFormat.format(
					UIText.MergeTargetSelectionDialog_TitleMergeWithBranch,
					branch);
		else
			return UIText.MergeTargetSelectionDialog_TitleMerge;
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
