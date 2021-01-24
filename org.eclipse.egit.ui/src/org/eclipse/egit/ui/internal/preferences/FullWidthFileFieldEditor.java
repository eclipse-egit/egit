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
package org.eclipse.egit.ui.internal.preferences;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * A {@link FileFieldEditor} that is wrapped inside another composite so
 * that it can always fill a whole row and the columns for the three
 * controls are independent of the global grid layout of the page.
 */
class FullWidthFileFieldEditor extends FileFieldEditor {

	private Composite wrapper;

	private GridData layoutData;

	public FullWidthFileFieldEditor(String configName, String label,
			boolean enforceAbsolute, Composite parent) {
		super(configName, label, enforceAbsolute, parent);
	}

	@Override
	protected void createControl(Composite parent) {
		wrapper = new Composite(parent, SWT.NONE);
		layoutData = GridDataFactory.fillDefaults().grab(true, false)
				.create();
		wrapper.setLayoutData(layoutData);
		GridLayoutFactory.fillDefaults()
				.numColumns(super.getNumberOfControls()).applyTo(wrapper);
		doFillIntoGrid(wrapper, super.getNumberOfControls());
		if (SystemReader.getInstance().isMacOS()) {
			// The default "Open File" dialog on Mac does not show
			// hidden files, even if the user has enabled showing them
			// in the Finder. GPG is normally installed under /usr,
			// which is a hidden directory on Mac. There is a keyboard
			// shortcut to make it show hidden files and directories
			// (Cmd-Shift-.), but that's not obvious. Tell the user
			// about that shortcut in a tooltip.
			getChangeControl(wrapper).setToolTipText(
					UIText.FullWidthFileFieldEditor_buttonTooltipMac);
		}
	}

	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		if (parent != wrapper) {
			layoutData.horizontalSpan = numColumns;
		}
		super.doFillIntoGrid(wrapper, super.getNumberOfControls());
	}

	@Override
	protected void adjustForNumColumns(int numColumns) {
		layoutData.horizontalSpan = numColumns;
	}

	@Override
	public Label getLabelControl(Composite parent) {
		return super.getLabelControl(wrapper);
	}

	@Override
	public Text getTextControl(Composite parent) {
		return super.getTextControl(wrapper);
	}

	@Override
	protected Button getChangeControl(Composite parent) {
		return super.getChangeControl(wrapper);
	}

	@Override
	public int getNumberOfControls() {
		return 1;
	}
}