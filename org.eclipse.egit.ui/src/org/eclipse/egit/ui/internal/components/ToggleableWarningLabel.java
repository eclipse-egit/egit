/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Warning label that can be completely shown/hidden, depending on whether there
 * is a warning to display or not.
 */
public class ToggleableWarningLabel extends Composite {

	private Label warningText;

	/**
	 * Constructs the composite. Note that for the toggling to work, its parent
	 * composite must use a grid layout.
	 *
	 * @param parent
	 * @param style
	 */
	public ToggleableWarningLabel(Composite parent, int style) {
		super(parent, style);

		setVisible(false);
		setLayout(new GridLayout(2, false));

		Label image = new Label(this, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING)
				.applyTo(image);
		image.setImage(PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJS_WARN_TSK));

		warningText = new Label(this, SWT.WRAP);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(warningText);
	}

	/**
	 * Set the warning message and show the label.
	 *
	 * @param message
	 */
	public void showMessage(String message) {
		warningText.setText(message);
		layout(true);

		changeVisibility(true);
	}

	/**
	 * Hide the warning label.
	 */
	public void hideMessage() {
		if (getVisible())
			changeVisibility(false);
	}

	private void changeVisibility(boolean visible) {
		setVisible(visible);
		GridData data = (GridData) getLayoutData();
		data.exclude = !visible;
		getParent().layout();
	}
}
