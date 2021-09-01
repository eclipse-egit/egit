/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;

/**
 * Tracks the visibility of an (editor) part and can run code only if the
 * part is currently visible. If it is not, the code will be run when that
 * part becomes visible.
 */
public class EditorVisibilityTracker extends PartVisibilityListener {

	private Runnable runnable;

	/**
	 * Creates a new {@link EditorVisibilityTracker} for the given part.
	 *
	 * @param part
	 *            to track
	 */
	public EditorVisibilityTracker(IWorkbenchPart part) {
		super(part);
	}

	/**
	 * Is the part is visible, runs the given {@link Runnable} right away.
	 * Otherwise it is saved and run when the part <em>does</em> become visible.
	 * A subsequent call to {@link #runWhenVisible(Runnable)} while the part is
	 * still <em>not</em> visible will override the first {@link Runnable}.
	 *
	 * @param code
	 *            {@link Runnable} to execute when the part is visible
	 */
	public void runWhenVisible(Runnable code) {
		if (isVisible()) {
			code.run();
		} else {
			runnable = code;
		}
	}

	@Override
	protected void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && runnable != null) {
			try {
				runnable.run();
			} finally {
				runnable = null;
			}
		}
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		// Nothing to do
	}
}
