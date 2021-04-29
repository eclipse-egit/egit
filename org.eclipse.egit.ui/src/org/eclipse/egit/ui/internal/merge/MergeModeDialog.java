/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.merge;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Asks the user whether to use the workspace or HEAD
 */
public class MergeModeDialog extends Dialog {

	private MergeInputMode mode = MergeInputMode.STAGE_2;

	private Button dontAskAgain;

	/**
	 * @param parentShell
	 */
	public MergeModeDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * @return whether the workspace should be used
	 */
	public MergeInputMode getMergeMode() {
		return mode;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		final Button useWorkspace = new Button(main, SWT.RADIO);
		useWorkspace.setText(UIText.MergeModeDialog_MergeMode_1_Label);
		useWorkspace.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				mode = MergeInputMode.WORKTREE;
			}
		});
		useWorkspace.setSelection(false);
		final Button useHead = new Button(main, SWT.RADIO);
		useHead.setText(UIText.MergeModeDialog_MergeMode_2_Label);
		useHead.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				mode = MergeInputMode.STAGE_2;
			}
		});
		useHead.setSelection(true);

		dontAskAgain = new Button(main, SWT.CHECK);
		dontAskAgain.setText(UIText.MergeModeDialog_DontAskAgainLabel);
		return main;
	}

	@Override
	protected void okPressed() {
		boolean save = dontAskAgain.getSelection();
		super.okPressed();
		if (save) {
			int value = mode.toInteger();
			IPreferenceStore store = Activator.getDefault()
					.getPreferenceStore();
			store.setValue(UIPreferences.MERGE_MODE, value);
		}
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.MergeModeDialog_DialogTitle);
	}
}
