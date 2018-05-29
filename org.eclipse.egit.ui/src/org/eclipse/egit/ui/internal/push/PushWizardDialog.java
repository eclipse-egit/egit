/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.NonBlockingWizardDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * A dialog dedicated to {@link PushBranchWizard}, customizing button labels
 */
public class PushWizardDialog extends NonBlockingWizardDialog {

	/**
	 * @param parentShell
	 * @param newWizard
	 */
	public PushWizardDialog(Shell parentShell, IWizard newWizard) {
		super(parentShell, newWizard);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		Button nextButton = getButton(IDialogConstants.NEXT_ID);
		if (nextButton != null) {
			nextButton.setText(UIText.PushBranchWizard_previewButton);
		}
		Button finishButton = getButton(IDialogConstants.FINISH_ID);
		if (finishButton != null) {
			finishButton.setText(UIText.PushBranchWizard_pushButton);
		}
	}

}
