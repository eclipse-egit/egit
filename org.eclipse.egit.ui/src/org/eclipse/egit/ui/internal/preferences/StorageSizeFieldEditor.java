/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

class StorageSizeFieldEditor extends StringFieldEditor {
	private static final int KB = 1024;

	private static final int MB = 1024 * KB;

	private static final int GB = 1024 * MB;

	private final int minValidValue;

	private final int maxValidValue;

	StorageSizeFieldEditor(String name, String labelText, Composite parent,
			int min, int max) {
		init(name, labelText);
		setTextLimit(10);
		setEmptyStringAllowed(false);
		setErrorMessage(JFaceResources
				.getString("IntegerFieldEditor.errorMessage")); //$NON-NLS-1$
		createControl(parent);
		minValidValue = min;
		maxValidValue = max;
	}

	@Override
	protected boolean checkState() {
		final Text text = getTextControl();
		if (text == null)
			return false;

		final String numberString = text.getText();
		final int number = parse(numberString);
		if (checkValue(number)) {
			clearErrorMessage();
			return true;
		}
		showErrorMessage();
		return false;
	}

	/**
	 * Verify this value is acceptable.
	 *
	 * @param number
	 *            the value parsed from the input.
	 * @return true if the value is OK; false otherwise.
	 */
	protected boolean checkValue(final int number) {
		return number >= minValidValue && number <= maxValidValue;
	}

	@Override
	protected void doLoad() {
		final Text text = getTextControl();
		if (text != null) {
			int value = getPreferenceStore().getInt(getPreferenceName());
			text.setText(format(value));
		}
	}

	@Override
	protected void doLoadDefault() {
		final Text text = getTextControl();
		if (text != null) {
			int value = getPreferenceStore().getDefaultInt(getPreferenceName());
			text.setText(format(value));
		}
		valueChanged();
	}

	@Override
	protected void doStore() {
		final Text text = getTextControl();
		if (text != null) {
			final int v = parse(text.getText());
			getPreferenceStore().setValue(getPreferenceName(), v);
		}
	}

	private String format(int value) {
		if (value > GB && (value / GB) * GB == value)
			return String.valueOf(value / GB) + " g"; //$NON-NLS-1$
		if (value > MB && (value / MB) * MB == value)
			return String.valueOf(value / MB) + " m"; //$NON-NLS-1$
		if (value > KB && (value / KB) * KB == value)
			return String.valueOf(value / KB) + " k"; //$NON-NLS-1$
		return String.valueOf(value);
	}

	private int parse(final String str) {
		String n = str.trim();
		if (n.length() == 0)
			return 0;

		int mul = 1;
		switch (Character.toLowerCase(n.charAt(n.length() - 1))) {
		case 'g':
			mul = GB;
			break;
		case 'm':
			mul = MB;
			break;
		case 'k':
			mul = KB;
			break;
		}
		if (mul > 1)
			n = n.substring(0, n.length() - 1).trim();
		if (n.length() == 0)
			return 0;

		try {
			return mul * Integer.parseInt(n);
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}
}
