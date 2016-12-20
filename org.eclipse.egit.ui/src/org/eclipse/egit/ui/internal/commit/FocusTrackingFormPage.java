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
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;

/**
 * A {@link FormPage} that provides support for tracking the currently focused
 * {@link Control} on the page. When the page is re-activated, focus is re-set
 * to the control that had the focus when the page became inactive.
 */
public class FocusTrackingFormPage extends FormPage {

	private final Set<Control> trackedControls = new HashSet<>();

	private final DisposeListener disposeListener = event -> trackedControls
			.remove(event.widget);

	private final FocusListener focusTracker = new FocusAdapter() {

		@Override
		public void focusLost(FocusEvent e) {
			if (e.widget instanceof Control) {
				lastFocusControl = (Control) e.widget;
			}
		}
	};

	private Control lastFocusControl;

	/**
	 * Creates a new {@link FocusTrackingFormPage} with the given id and title;
	 * the {@link FormEditor} must be set via {@link #initialize(FormEditor)}.
	 *
	 * @param id
	 *            of the page
	 * @param title
	 *            of the page
	 */
	public FocusTrackingFormPage(String id, String title) {
		super(id, title);
	}

	/**
	 * Creates a new {@link FocusTrackingFormPage}.
	 *
	 * @param editor
	 *            containing the page
	 * @param id
	 *            of the page
	 * @param title
	 *            of the page
	 */
	public FocusTrackingFormPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	/**
	 * Registers the given {@link Control} for having focus changes being
	 * tracked. Has no effect if the control is already being tracked.
	 *
	 * @param control
	 *            to track
	 */
	protected void addToFocusTracking(@NonNull Control control) {
		if (trackedControls.add(control)) {
			control.addDisposeListener(disposeListener);
			control.addFocusListener(focusTracker);
		}
	}

	/**
	 * Stops tracking focus changes for the given {@link Control}. Has no effect
	 * if the control is {@code null} or is not currently being tracked.
	 *
	 * @param control
	 *            to remove from focus tracking
	 */
	protected void removeFromFocusTracking(Control control) {
		if (trackedControls.remove(control) && !control.isDisposed()) {
			control.removeDisposeListener(disposeListener);
			control.removeFocusListener(focusTracker);
		}
	}

	@Override
	public void setFocus() {
		if (lastFocusControl != null) {
			if (lastFocusControl.isDisposed()) {
				trackedControls.remove(lastFocusControl);
				lastFocusControl = null;
			} else if (lastFocusControl.forceFocus()) {
				return;
			}
		}
		getManagedForm().getForm().setFocus();
	}

	@Override
	public void dispose() {
		for (Control tracked : trackedControls) {
			if (!tracked.isDisposed()) {
				tracked.removeDisposeListener(disposeListener);
				tracked.removeFocusListener(focusTracker);
			}
		}
		trackedControls.clear();
		lastFocusControl = null;
		super.dispose();
	}
}
