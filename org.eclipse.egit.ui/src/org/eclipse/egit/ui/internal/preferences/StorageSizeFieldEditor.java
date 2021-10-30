/*******************************************************************************
 * Copyright (C) 2008, 2016, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

class StorageSizeFieldEditor extends StringFieldEditor {

	private final int minValidValue;

	private final int maxValidValue;

	StorageSizeFieldEditor(String name, String labelText, Composite parent,
			int min, int max) {
		Assert.isTrue(min > 0 && min < max);
		init(name, labelText);
		setTextLimit(10);
		setEmptyStringAllowed(false);
		setErrorMessage(
				JFaceResources.format("IntegerFieldEditor.errorMessageRange", //$NON-NLS-1$
						new Object[] { Integer.valueOf(min),
								Integer.valueOf(max) }));
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
			text.setText(StringUtils.formatWithSuffix(value));
		}
	}

	@Override
	protected void doLoadDefault() {
		final Text text = getTextControl();
		if (text != null) {
			int value = getPreferenceStore().getDefaultInt(getPreferenceName());
			text.setText(StringUtils.formatWithSuffix(value));
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

	private int parse(final String str) {
		try {
			return StringUtils.parseIntWithSuffix(str, true);
		} catch (NumberFormatException | StringIndexOutOfBoundsException e) {
			return 0;
		}
	}
}
