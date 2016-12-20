/*******************************************************************************
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;

/**
 * Utility class to track the focus of a set of {@link Control}s to determine
 * the one that last had the focus.
 */
public class FocusTracker {

	private final Set<Control> trackedControls = new HashSet<>();

	private final Listener listener = event -> {
		switch (event.type) {
		case SWT.Dispose:
			trackedControls.remove(event.widget);
			break;
		case SWT.FocusIn:
		case SWT.FocusOut:
			if (event.widget instanceof Control) {
				lastFocusControl = (Control) event.widget;
			}
			break;
		default:
			break;
		}
	};

	private Control lastFocusControl;

	private void hookControl(@NonNull Control control) {
		control.addListener(SWT.Dispose, listener);
		control.addListener(SWT.FocusIn, listener);
		control.addListener(SWT.FocusOut, listener);
	}

	private void unhookControl(@NonNull Control control) {
		control.removeListener(SWT.FocusOut, listener);
		control.removeListener(SWT.FocusIn, listener);
		control.removeListener(SWT.Dispose, listener);
	}

	/**
	 * Registers the given {@link Control} for having focus changes being
	 * tracked. Has no effect if the control is already being tracked.
	 *
	 * @param control
	 *            to track
	 */
	public void addToFocusTracking(@NonNull Control control) {
		if (trackedControls.add(control)) {
			hookControl(control);
		}
	}

	/**
	 * Stops tracking focus changes for the given {@link Control}. Has no effect
	 * if the control is {@code null} or is not currently being tracked.
	 *
	 * @param control
	 *            to remove from focus tracking
	 */
	public void removeFromFocusTracking(Control control) {
		if (trackedControls.remove(control) && !control.isDisposed()) {
			unhookControl(control);
		}
	}

	/**
	 * Retrieves the last control to have had the focus.
	 *
	 * @return the control, or {@code null} if none determined yet.
	 */
	public Control getLastFocusControl() {
		if (lastFocusControl != null && lastFocusControl.isDisposed()) {
			trackedControls.remove(lastFocusControl);
			lastFocusControl = null;
		}
		return lastFocusControl;
	}

	/**
	 * Dispose of this {@link FocusTracker}.
	 */
	public void dispose() {
		for (Control tracked : trackedControls) {
			if (!tracked.isDisposed()) {
				unhookControl(tracked);
			}
		}
		trackedControls.clear();
		lastFocusControl = null;
	}
}
