/*******************************************************************************
 * Copyright (c) 2010, SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.util.StringTokenizer;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Requests a key and value for adding a configuration entry
 */
public class AddConfigEntryDialog extends TitleAreaDialog {
	private Text keyText;

	private Text valueText;

	private String key;

	private String value;

	private final String suggestedKey;

	/**
	 * @param parentShell
	 * @param suggestedKey
	 *            may be null
	 */
	public AddConfigEntryDialog(Shell parentShell, String suggestedKey) {
		super(parentShell);
		setHelpAvailable(false);
		this.suggestedKey = suggestedKey;
		setHelpAvailable(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(UIText.AddConfigEntryDialog_AddConfigTitle);
		setTitle(UIText.AddConfigEntryDialog_AddConfigTitle);
		setMessage(UIText.AddConfigEntryDialog_DialogMessage);
		Composite titleParent = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(titleParent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		Label keylLabel = new Label(main, SWT.NONE);
		keylLabel.setText(UIText.AddConfigEntryDialog_KeyLabel);
		keylLabel.setToolTipText(UIText.AddConfigEntryDialog_ConfigKeyTooltip);
		keyText = new Text(main, SWT.BORDER);
		if (suggestedKey != null) {
			keyText.setText(suggestedKey);
			keyText.selectAll();
		}

		keyText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				check();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(keyText);
		new Label(main, SWT.NONE)
				.setText(UIText.AddConfigEntryDialog_ValueLabel);
		valueText = new Text(main, SWT.BORDER);
		valueText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				check();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(valueText);

		applyDialogFont(main);
		return main;
	}

	@Override
	public void create() {
		super.create();
		// we need to enter something
		getButton(OK).setEnabled(false);
	}

	private void check() {
		setErrorMessage(null);
		boolean hasError = false;
		try {
			if (keyText.getText().length() == 0) {
				setErrorMessage(UIText.AddConfigEntryDialog_MustEnterKeyMessage);
				hasError = true;
				return;
			}
			StringTokenizer st = new StringTokenizer(keyText.getText(), "."); //$NON-NLS-1$
			if (st.countTokens() < 2 || st.countTokens() > 3) {
				setErrorMessage(UIText.AddConfigEntryDialog_KeyComponentsMessage);
				hasError = true;
				return;
			}
			if (valueText.getText().length() == 0) {
				setErrorMessage(UIText.AddConfigEntryDialog_EnterValueMessage);
				hasError = true;
				return;
			}
		} finally {
			getButton(OK).setEnabled(!hasError);
		}
	}

	@Override
	protected void okPressed() {
		key = keyText.getText();
		value = valueText.getText();
		super.okPressed();
	}

	/**
	 * @return the key as entered by the user
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @return the value as entered by the user
	 */
	public String getValue() {
		return value;
	}
}
