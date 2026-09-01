/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.text.MessageFormat;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Asks whether to check out a branch. For repositories with submodules the
 * dialog also offers to update them after the checkout.
 */
public class CheckoutConfirmationDialog extends MessageDialogWithToggle {

	private final Repository repository;

	private Button updateSubmodulesButton;

	private boolean updateSubmodules;

	/**
	 * @param shell
	 *            parent shell
	 * @param repository
	 *            to check out in
	 * @param refName
	 *            full name of the ref to check out
	 */
	public CheckoutConfirmationDialog(Shell shell, Repository repository,
			String refName) {
		super(shell, UIText.RepositoriesView_CheckoutConfirmationTitle, null,
				MessageFormat.format(
						UIText.RepositoriesView_CheckoutConfirmationMessage,
						Repository.shortenRefName(refName)),
				QUESTION,
				new String[] {
						UIText.RepositoriesView_CheckoutConfirmationDefaultButtonLabel,
						IDialogConstants.CANCEL_LABEL },
				0, UIText.RepositoriesView_CheckoutConfirmationToggleMessage,
				false);
		this.repository = repository;
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		if (!BranchOperationUI.hasSubmodules(repository)) {
			return null;
		}
		updateSubmodulesButton = new Button(parent, SWT.CHECK);
		updateSubmodulesButton.setText(UIText.CheckoutDialog_UpdateSubmodules);
		updateSubmodulesButton
				.setToolTipText(UIText.CheckoutDialog_UpdateSubmodulesTooltip);
		updateSubmodulesButton.setSelection(Activator.getDefault()
				.getPreferenceStore()
				.getBoolean(UIPreferences.CHECKOUT_UPDATE_SUBMODULES));
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false)
				.applyTo(updateSubmodulesButton);
		return updateSubmodulesButton;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (updateSubmodulesButton != null) {
			updateSubmodules = updateSubmodulesButton.getSelection();
		}
		super.buttonPressed(buttonId);
	}

	/**
	 * Opens the dialog and stores the chosen options.
	 *
	 * @return whether the checkout was confirmed
	 */
	public boolean confirm() {
		// With custom buttons the first button reports INTERNAL_ID, not OK
		int result = open();
		if (result != Window.OK && result != IDialogConstants.INTERNAL_ID) {
			return false;
		}
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		if (getToggleState()) {
			store.setValue(UIPreferences.SHOW_CHECKOUT_CONFIRMATION, false);
		}
		if (updateSubmodulesButton != null) {
			store.setValue(UIPreferences.CHECKOUT_UPDATE_SUBMODULES,
					updateSubmodules);
		}
		return true;
	}
}
