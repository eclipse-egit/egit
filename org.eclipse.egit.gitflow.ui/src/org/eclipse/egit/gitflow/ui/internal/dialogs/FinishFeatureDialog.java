/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.dialogs;

import static org.eclipse.egit.gitflow.ui.internal.UIPreferences.FEATURE_FINISH_KEEP_BRANCH;
import static org.eclipse.egit.gitflow.ui.internal.UIPreferences.FEATURE_FINISH_SQUASH;

import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog to select options for feature finish operation.
 *
 */
public class FinishFeatureDialog extends TitleAreaDialog {

	private boolean squash;

	private boolean keepBranch;

	private Button squashButton;

	private Button keepBranchButton;

	private String featureBranch;

	private Button rememberOptionsButton;

	/**
	 * @param parentShell
	 * @param featureBranch
	 */
	public FinishFeatureDialog(Shell parentShell, String featureBranch) {
		super(parentShell);
		this.featureBranch = featureBranch;
	}

	@Override
	public void create() {
		super.create();
		setTitle(UIText.FinishFeatureDialog_title);
		getShell().setText(UIText.FinishFeatureDialog_title);
		setMessage(NLS.bind(
				UIText.FinishFeatureDialog_setParameterForFinishing,
				featureBranch), IMessageProvider.INFORMATION);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(1, false);
		container.setLayout(layout);

		squashButton = new Button(container, SWT.CHECK);
		squashButton.setText(UIText.FinishFeatureDialog_squashCheck);

		keepBranchButton = new Button(container, SWT.CHECK);
		keepBranchButton.setText(UIText.FinishFeatureDialog_keepBranch);

		restoreInput();

		return area;
	}

	private void restoreInput() {
		IPreferenceStore prefStore = Activator
				.getDefault()
				.getPreferenceStore();
		squashButton.setSelection(prefStore.getBoolean(FEATURE_FINISH_SQUASH));
		keepBranchButton.setSelection(prefStore.getBoolean(FEATURE_FINISH_KEEP_BRANCH));
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		final Composite customButtonBar = new Composite(parent, SWT.NONE);

		int horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		GridLayoutFactory.swtDefaults().numColumns(2).equalWidth(false)
				.spacing(horizontalSpacing, 0).applyTo(customButtonBar);

		GridDataFactory.swtDefaults().grab(true, false)
				.align(SWT.FILL, SWT.BOTTOM).applyTo(customButtonBar);

		customButtonBar.setFont(parent.getFont());

		rememberOptionsButton = new Button(customButtonBar, SWT.CHECK);
		rememberOptionsButton.setText(UIText.FinishFeatureDialog_saveAsDefault);

		// TODO: Checkbox "Don't ask again"

		int horizontlIndent = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		GridDataFactory.swtDefaults().grab(true, true)
				.align(SWT.FILL, SWT.CENTER).indent(horizontlIndent, 0)
				.applyTo(rememberOptionsButton);

		// add the dialog's button bar to the right
		final Control buttonControl = super.createButtonBar(customButtonBar);
		GridDataFactory.swtDefaults().grab(true, false)
				.align(SWT.RIGHT, SWT.CENTER).applyTo(buttonControl);


		return customButtonBar;
	}

	@Override
	public boolean isHelpAvailable() {
		return false;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	private void saveInput() {
		this.squash = squashButton.getSelection();

		this.keepBranch = keepBranchButton.getSelection();
		if (rememberOptionsButton.getSelection()) {
			IPreferenceStore preferenceStore = Activator
					.getDefault()
					.getPreferenceStore();
			preferenceStore.setValue(FEATURE_FINISH_SQUASH, squashButton.getSelection());
			preferenceStore.setValue(FEATURE_FINISH_KEEP_BRANCH, keepBranchButton.getSelection());
		}

	}

	@Override
	protected void okPressed() {
		saveInput();
		super.okPressed();
	}

	/**
	 * @return is squash
	 */
	public boolean isSquash() {
		return squash;
	}

	/**
	 * @return Whether or not the branch should be kept after the operation is finished
	 */
	public boolean isKeepBranch() {
		return keepBranch;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, UIText.FinishFeatureDialog_ButtonOK, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}
}
