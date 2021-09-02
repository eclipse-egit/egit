/*******************************************************************************
 * Copyright (C) 2019, Max Hohenegger <eclipse@hohenegger.eu> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.actions;

import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.BranchNameNormalizer;
import org.eclipse.egit.ui.internal.dialogs.ResizingInputDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

class StartDialog extends ResizingInputDialog {

	StartDialog(Shell parentShell, String dialogTitle,
			String dialogMessage, String initialValue,
			IInputValidator validator) {
		super(parentShell, dialogTitle, dialogMessage, initialValue, validator);
	}

	@Override
	protected Button createButton(Composite parent, int id, String label,
			boolean defaultButton) {
		if (id == IDialogConstants.OK_ID) {
			return super.createButton(parent, id, UIText.StartDialog_ButtonOK,
					defaultButton);
		}
		return super.createButton(parent, id, label, defaultButton);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control result = super.createDialogArea(parent);
		BranchNameNormalizer normalizer = new BranchNameNormalizer(getText());
		normalizer.setVisible(false);
		return result;
	}
}
