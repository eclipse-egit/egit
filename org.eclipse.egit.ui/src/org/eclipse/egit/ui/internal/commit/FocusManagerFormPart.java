/*******************************************************************************
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;

/**
 * An {@link IFormPart} that uses a given {@link FocusTracker} to determine the
 * {@link Control} set the focus to.
 */
public abstract class FocusManagerFormPart implements IFormPart {

	private final FocusTracker focusTracker;

	/**
	 * Creates a new {@link FocusManagerFormPart}.
	 *
	 * @param focusTracker
	 *            to use
	 */
	public FocusManagerFormPart(FocusTracker focusTracker) {
		this.focusTracker = focusTracker;
	}

	@Override
	public void setFocus() {
		Control control = focusTracker.getLastFocusControl();
		if (control != null && control.forceFocus()) {
			return;
		}
		setDefaultFocus();
	}

	/**
	 * Invoked by {@link #setFocus} if the {@link FocusTracker} didn't identify
	 * a control to set the focus to, or that control could not be focused.
	 */
	public abstract void setDefaultFocus();

	@Override
	public void initialize(IManagedForm form) {
		// Nothing to do
	}

	@Override
	public void dispose() {
		// Nothing to do
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public void commit(boolean onSave) {
		// Nothing to do
	}

	@Override
	public boolean setFormInput(Object input) {
		return false;
	}

	@Override
	public boolean isStale() {
		return false;
	}

	@Override
	public void refresh() {
		// Nothing to do
	}

}
