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
package org.eclipse.egit.ui.internal.staging;

import org.eclipse.egit.ui.internal.components.ToggleableWarningLabel;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * A toggleable label that can show a warning or an info message.
 */
public class ToggleableLabel extends ToggleableWarningLabel {

	private boolean isInfo;

	/**
	 * Creates a new ToggleableLabel
	 *
	 * @param parent
	 *            of the new label
	 * @param style
	 *            of the Composite
	 */
	public ToggleableLabel(Composite parent, int style) {
		super(parent, style);
		isInfo = false;
	}

	/**
	 * Show the info message with an info icon.
	 *
	 * @param message
	 *            to show
	 * @return whether the {@link ToggleableLabel} changed appearance
	 */
	public boolean showInfo(String message) {
		boolean changed = false;
		if (!isInfo) {
			setImage(PlatformUI.getWorkbench().getSharedImages()
					.getImage(ISharedImages.IMG_OBJS_INFO_TSK));
			isInfo = true;
			changed = true;
		}
		changed |= setText(message);
		return changed;
	}

	@Override
	public boolean showMessage(String message) {
		isInfo = false;
		return super.showMessage(message);
	}
}
