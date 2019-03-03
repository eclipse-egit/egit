/*******************************************************************************
 * Copyright (c) 2010, 2016 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Mathias Kinzler (SAP AG) - use the abstract super class
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 499482
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for selecting a merge target.
 */
public class RebaseTargetSelectionDialog extends AbstractBranchSelectionDialog {

	private boolean interactive = false;

	private boolean preserveMerges = false;

	/**
	 * @param parentShell
	 * @param repo
	 */
	public RebaseTargetSelectionDialog(Shell parentShell, Repository repo) {
		super(parentShell, repo, getMergeTarget(repo), SHOW_LOCAL_BRANCHES
				| SHOW_REMOTE_BRANCHES | EXPAND_REMOTE_BRANCHES_NODE
				| getSelectSetting(repo));
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(Window.OK).setText(
				UIText.RebaseTargetSelectionDialog_RebaseButton);
	}

	@Override
	protected String getMessageText() {
		String branch = getCurrentBranch();
		if (branch != null)
			return MessageFormat.format(
					UIText.RebaseTargetSelectionDialog_DialogMessageWithBranch,
					branch);
		else
			return UIText.RebaseTargetSelectionDialog_DialogMessage;
	}

	@Override
	protected String getTitle() {
		String branch = getCurrentBranch();
		if (branch != null)
			return MessageFormat.format(
					UIText.RebaseTargetSelectionDialog_DialogTitleWithBranch,
					branch);
		else
			return UIText.RebaseTargetSelectionDialog_DialogTitle;
	}

	@Override
	protected String getWindowTitle() {
		String branch = getCurrentBranch();
		if (branch != null)
			return MessageFormat.format(
					UIText.RebaseTargetSelectionDialog_RebaseTitleWithBranch,
					branch);
		else
			return UIText.RebaseTargetSelectionDialog_RebaseTitle;
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

		setOkButtonEnabled(!currentSelected && (branchSelected || tagSelected));
	}

	@Override
	protected void createCustomArea(Composite parent) {
		final Button interactiveButton = new Button(parent, SWT.CHECK);
		interactiveButton
				.setText(UIText.RebaseTargetSelectionDialog_InteractiveButton);
		interactiveButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				interactive = interactiveButton.getSelection();
			}
		});
		final Button preserveMergesButton = new Button(parent, SWT.CHECK);
		preserveMergesButton
				.setText(UIText.RebaseTargetSelectionDialog_PreserveMergesButton);
		preserveMergesButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				preserveMerges = preserveMergesButton.getSelection();
			}
		});
		String branchName = getCurrentBranch();
		if (branchName == null) {
			return;
		}
		Config cfg = repo.getConfig();
		BranchRebaseMode rebase = cfg.getEnum(BranchRebaseMode.values(),
				ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
				ConfigConstants.CONFIG_KEY_REBASE, BranchRebaseMode.NONE);
		switch (rebase) {
		case PRESERVE:
			preserveMergesButton.setSelection(true);
			preserveMerges = true;
			break;
		case INTERACTIVE:
			interactiveButton.setSelection(true);
			interactive = true;
			break;
		default:
			break;
		}
	}

	/**
	 * @return whether the rebase should be interactive
	 */
	public boolean isInteractive() {
		return interactive;
	}

	/**
	 * @return whether merges should be preserved during rebase
	 */
	public boolean isPreserveMerges() {
		return preserveMerges;
	}
}
