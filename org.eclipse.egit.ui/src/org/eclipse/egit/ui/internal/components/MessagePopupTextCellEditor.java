/*******************************************************************************
 * Copyright (c) 2019 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A {@link TextCellEditor} that automatically shows any message from an
 * {@link org.eclipse.jface.viewers.ICellEditorValidator ICellEditorValidator}
 * in a {@link DefaultToolTip} pop-up. The tooltip by default has a delay of
 * 200ms and a light red background. The tooltip can be obtained via
 * {@link #getToolTip()} and then customized, if needed.
 * <p>
 * Additionally, this editor supports canceling the edit when the focus is lost.
 * </p>
 */
public class MessagePopupTextCellEditor extends TextCellEditor {

	/** Default pop-up delay in milliseconds. */
	private static final int DEFAULT_DELAY_MILLIS = 200;

	/**
	 * Default background color. See css/egit.css for the use of this pinkish
	 * color.
	 */
	private static final RGB DEFAULT_BACKGROUND = new RGB(0xFF, 0x96, 0x96);

	private final boolean cancelOnFocusLost;

	private DefaultToolTip errorPopup;

	/**
	 * Creates a new {@link MessagePopupTextCellEditor} parented under the given
	 * control. The editor will have the standard behavior of applying the value
	 * when it loses the focus.
	 *
	 * @param parent
	 *            the parent control
	 * @see TextCellEditor#TextCellEditor(Composite)
	 */
	public MessagePopupTextCellEditor(Composite parent) {
		this(parent, false);
	}

	/**
	 * Creates a new {@link MessagePopupTextCellEditor} parented under the given
	 * control.
	 *
	 * @param parent
	 *            the parent control
	 * @param cancelOnFocusLost
	 *            whether to cancel the edit when the focus is lost
	 * @see TextCellEditor#TextCellEditor(Composite)
	 */
	public MessagePopupTextCellEditor(Composite parent,
			boolean cancelOnFocusLost) {
		super(parent);
		this.cancelOnFocusLost = cancelOnFocusLost;
	}

	/**
	 * Creates a new {@link MessagePopupTextCellEditor} parented under the given
	 * control using the given style. The editor will have the standard behavior
	 * of applying the value when it loses the focus.
	 *
	 * @param parent
	 *            the parent control
	 * @param style
	 *            the style bits
	 * @see TextCellEditor#TextCellEditor(Composite, int)
	 */
	public MessagePopupTextCellEditor(Composite parent, int style) {
		this(parent, false, style);
	}

	/**
	 * Creates a new {@link MessagePopupTextCellEditor} parented under the given
	 * control using the given style.
	 *
	 * @param parent
	 *            the parent control
	 * @param cancelOnFocusLost
	 *            whether to cancel the edit when the focus is lost
	 * @param style
	 *            the style bits
	 * @see TextCellEditor#TextCellEditor(Composite, int)
	 */
	public MessagePopupTextCellEditor(Composite parent,
			boolean cancelOnFocusLost, int style) {
		super(parent, style);
		this.cancelOnFocusLost = cancelOnFocusLost;
	}

	/**
	 * This cell editor uses the built-in focus listener provided by the super
	 * class.
	 */
	@Override
	protected boolean dependsOnExternalFocusListener() {
		return false;
	}

	/**
	 * Invoked when the cell editor has lost the focus; cancels the editor
	 * without applying the value.
	 */
	@Override
	protected void focusLost() {
		// The super implementation applies the value, but that may be a bit
		// risky. In some contexts edits should be done only if the user
		// explicitly hit <return>.
		if (cancelOnFocusLost) {
			if (isActivated()) {
				fireCancelEditor();
			}
		} else {
			super.focusLost();
		}
	}

	@Override
	protected Control createControl(Composite parent) {
		Control control = super.createControl(parent);
		errorPopup = new DefaultToolTip(control, ToolTip.NO_RECREATE, true);
		// A delay enables us to cancel showing the tooltip if the user keeps
		// typing and the value is valid again.
		errorPopup.setPopupDelay(DEFAULT_DELAY_MILLIS);
		errorPopup.setBackgroundColor(Activator.getDefault()
				.getResourceManager().createColor(DEFAULT_BACKGROUND));
		control.addDisposeListener(event -> errorPopup.hide());
		addListener(new ICellEditorListener() {

			@Override
			public void editorValueChanged(boolean oldValidState,
					boolean newValidState) {
				if (newValidState) {
					errorPopup.hide();
					return;
				}
				Control editor = getControl();
				Point pos = editor.getSize();
				errorPopup.setText(getErrorMessage());
				pos.x = 0;
				errorPopup.show(pos);
			}

			@Override
			public void cancelEditor() {
				errorPopup.hide();
			}

			@Override
			public void applyEditorValue() {
				errorPopup.hide();
			}
		});
		// Since "text" in the super class is protected, we may rely on (a)
		// super.createControl() having created and returned a Text, and (b) it
		// having actually set "text", and (c) control == text.
		if ((text.getStyle() & SWT.SINGLE) != 0) {
			// Prevent pasting multi-line text into a single-line control. See
			// bug 273470.
			text.addVerifyListener(
					event -> event.text = Utils.firstLine(event.text));
		}
		return control;
	}

	/**
	 * Retrieves the {@link DefaultToolTip} used for the validation message
	 * pop-up.
	 *
	 * @return the tooltip
	 */
	public DefaultToolTip getToolTip() {
		return errorPopup;
	}
}
