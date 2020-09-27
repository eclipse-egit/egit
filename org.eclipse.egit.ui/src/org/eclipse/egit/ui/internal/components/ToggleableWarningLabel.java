/*******************************************************************************
 * Copyright (C) 2012, 2018 Robin Stocker <robin@nibor.org> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.util.Objects;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
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

	private Label image;

	private boolean isBuiltInImage;

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

		image = new Label(this, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING)
				.applyTo(image);
		image.setImage(PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJS_WARN_TSK));
		isBuiltInImage = true;
		warningText = new Label(this, SWT.WRAP);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(warningText);
	}

	/**
	 * Set the warning message and show the label. Forces the image to the
	 * warning icon if it had been changed.
	 *
	 * @param message
	 * @return whether something changed
	 */
	public boolean showMessage(String message) {
		boolean changed = false;
		if (!isBuiltInImage) {
			image.setImage(PlatformUI.getWorkbench().getSharedImages()
					.getImage(ISharedImages.IMG_OBJS_WARN_TSK));
			isBuiltInImage = true;
			changed = true;
		}
		changed |= setText(message);
		return changed;
	}

	/**
	 * Hide the warning label.
	 *
	 * @return whether the visibility changed
	 */
	public boolean hideMessage() {
		return changeVisibility(false);
	}

	/**
	 * Sets a new image. Does not change visibility or layout. Can be used by
	 * subclasses to define their own images.
	 *
	 * @param image
	 *            to set
	 */
	protected void setImage(Image image) {
		this.image.setImage(image);
		isBuiltInImage = false;
	}

	/**
	 * Sets the text and shows the Composite.
	 *
	 * @param message
	 *            to show
	 * @return whether the Composite changed
	 */
	protected boolean setText(String message) {
		boolean changed = false;
		if (!Objects.equals(message, warningText.getText())) {
			warningText.setText(message);
			changed = true;
		}
		changed |= changeVisibility(true);
		return changed;
	}

	/**
	 * Defines the visibility of the whole Composite.
	 *
	 * @param visible
	 *            whether to show the composite
	 * @return whether the visibility changed
	 */
	protected boolean changeVisibility(boolean visible) {
		if (visible != getVisible()) {
			setVisible(visible);
			GridData data = (GridData) getLayoutData();
			data.exclude = !visible;
			return true;
		}
		return false;
	}
}
