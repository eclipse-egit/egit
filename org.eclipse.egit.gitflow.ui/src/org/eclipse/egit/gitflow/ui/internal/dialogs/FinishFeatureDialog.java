/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.dialogs;

import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
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

	private Button squashButton;

	private String featureBranch;

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

		return area;
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		// TODO: we should have options to persist the selected configuration
		return super.createButtonBar(parent);
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
}