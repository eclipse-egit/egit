/******************************************************************************
 *  Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.Objects;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * An {@link InputDialog} that resizes itself when the error message is set: if
 * the error message is long, the height of the dialog is increased such that
 * the whole message can be displayed.
 */
public class ResizingInputDialog extends InputDialog {

	private String currentErrorMessage;

	private Composite container;

	/**
	 * Creates a {@link ResizingInputDialog} with OK and Cancel buttons.
	 *
	 * @param parentShell
	 *            the parent shell, or {@code null} to create a top-level shell
	 * @param dialogTitle
	 *            the dialog title, or {@code null} if none
	 * @param dialogMessage
	 *            the dialog message, or {@code null} if none
	 * @param initialValue
	 *            the initial input value, or {@code null} if none (equivalent
	 *            to the empty string)
	 * @param validator
	 *            an input validator, or {@code null} if none
	 */
	public ResizingInputDialog(Shell parentShell, String dialogTitle,
			String dialogMessage, String initialValue,
			IInputValidator validator) {
		super(parentShell, dialogTitle, dialogMessage, initialValue, validator);
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control result = super.createButtonBar(parent);
		container = parent;
		return result;
	}

	@Override
	public void setErrorMessage(String errorMessage) {
		super.setErrorMessage(errorMessage);
		if (!Objects.equals(currentErrorMessage, errorMessage)) {
			currentErrorMessage = errorMessage;
			if (container != null) {
				Rectangle oldSize = container.getClientArea();
				Point newSize = container.computeSize(oldSize.width,
						SWT.DEFAULT, true);
				int dh = newSize.y - oldSize.height;
				if (dh > 0) {
					Shell shell = container.getShell();
					Point currentSize = shell.getSize();
					currentSize.y += dh;
					shell.setSize(currentSize);
				}
			}
		}
	}
}
