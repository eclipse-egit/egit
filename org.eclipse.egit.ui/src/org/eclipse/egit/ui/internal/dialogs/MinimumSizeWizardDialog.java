/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Shell;

/**
 * A {@link WizardDialog} that can prevent making the dialog smaller than the
 * size needed to show the starting page.
 */
public class MinimumSizeWizardDialog extends FinishableWizardDialog {

	private final boolean restrictResize;

	/**
	 * Creates a new {@link MinimumSizeWizardDialog} that prevents making the
	 * dialog smaller than the size needed to show the starting page.
	 *
	 * @param parentShell
	 *            for the dialog
	 * @param newWizard
	 *            to show in the dialog
	 */
	public MinimumSizeWizardDialog(Shell parentShell, IWizard newWizard) {
		this(parentShell, newWizard, true);
	}

	/**
	 * Creates a new {@link MinimumSizeWizardDialog}.
	 *
	 * @param parentShell
	 *            for the dialog
	 * @param newWizard
	 *            to show in the dialog
	 * @param restrictResize
	 *            {@code true} if the dialog should prevent being resized
	 *            smaller than necessary to show the starting page;
	 *            {@code false} if resizing to any size should be allowed
	 */
	public MinimumSizeWizardDialog(Shell parentShell, IWizard newWizard,
			boolean restrictResize) {
		super(parentShell, newWizard);
		this.restrictResize = restrictResize;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		if (restrictResize) {
			newShell.addShellListener(new ShellAdapter() {

				@Override
				public void shellActivated(ShellEvent e) {
					// Prevent making the dialog smaller than the starting page
					newShell.removeShellListener(this); // Only the first time
					newShell.setMinimumSize(newShell.getSize());
				}
			});
		}
	}
}
