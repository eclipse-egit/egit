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

import org.eclipse.egit.core.op.ResetOperation.ResetType;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for selecting a reset target.
 */
public class ResetTargetSelectionDialog extends BranchSelectionDialog {

	private ResetType resetType = ResetType.MIXED;

	/**
	 * Construct a dialog to select a branch to reset to
	 * @param parentShell
	 * @param repo
	 */
	public ResetTargetSelectionDialog(Shell parentShell, Repository repo) {
		super(parentShell, repo);
	}

	@Override
	protected void createCustomArea(Composite parent) {
		Group g = new Group(parent, SWT.NONE);
		g.setText(UIText.BranchSelectionDialog_ResetType);
		g.setLayoutData(GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).create());
		g.setLayout(new GridLayout(1, false));

		Button soft = new Button(g, SWT.RADIO);
		soft.setText(UIText.BranchSelectionDialog_ResetTypeSoft);
		soft.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				resetType = ResetType.SOFT;
			}
		});

		Button medium = new Button(g, SWT.RADIO);
		medium.setSelection(true);
		medium.setText(UIText.BranchSelectionDialog_ResetTypeMixed);
		medium.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				resetType = ResetType.MIXED;
			}
		});

		Button hard = new Button(g, SWT.RADIO);
		hard.setText(UIText.BranchSelectionDialog_ResetTypeHard);
		hard.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				resetType = ResetType.HARD;
			}
		});
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		confirmationBtn = createButton(parent, IDialogConstants.OK_ID,
				UIText.BranchSelectionDialog_OkReset, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected String getTitle() {
		return UIText.BranchSelectionDialog_TitleReset;
	}

	@Override
	protected boolean canConfirmOnTag() {
		return false;
	}

	/**
	 * @return Type of Reset
	 */
	public ResetType getResetType() {
		return resetType;
	}

	@Override
	protected void okPressed() {
		if (resetType == ResetType.HARD) {
			if (!MessageDialog.openQuestion(getShell(),
					UIText.BranchSelectionDialog_ReallyResetTitle,
					UIText.BranchSelectionDialog_ReallyResetMessage)) {
				return;
			}
		}
		super.okPressed();
	}
}