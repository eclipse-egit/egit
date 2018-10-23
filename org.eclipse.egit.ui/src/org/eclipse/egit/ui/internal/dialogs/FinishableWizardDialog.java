/*******************************************************************************
 * Copyright (C) 2018, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;

/**
 * A {@link WizardDialog} that can be programmatically finished.
 */
public class FinishableWizardDialog extends WizardDialog {

	/**
	 * Creates a new wizard dialog for the given wizard.
	 *
	 * @param parentShell
	 *            the parent shell
	 * @param newWizard
	 *            the wizard this dialog is working on
	 */
	public FinishableWizardDialog(Shell parentShell, IWizard newWizard) {
		super(parentShell, newWizard);
	}

	/**
	 * Tries to finish the wizard dialog as if the "Finish" button had been
	 * clicked. Does nothing if the "Finish" button is not enabled or the wizard
	 * cannot finish.
	 *
	 * @return whether the dialog was closed
	 */
	public boolean finish() {
		Button finishButton = getButton(IDialogConstants.FINISH_ID);
		if (finishButton.isEnabled() && getWizard().canFinish()) {
			// Probably checking both the button and the wizard is a bit
			// paranoid, especially since finishPressed only does something if
			// wizard.performFinish() == true, but yes, let's be paranoid and
			// not rely on implementation details. The javadoc of finishPressed
			// makes no promises.
			finishPressed();
			return getShell() == null || getShell().isDisposed();
		}
		return false;
	}
}
