/*******************************************************************************
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2010, Christian Halstrick <christian.halstrick@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.credentials;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.Dialog;
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

class CertPasswordDialog extends Dialog {

	private final String promptText;

	private String certPassword;

	protected CertPasswordDialog(Shell parentShell, String promptText) {
		super(parentShell);
		this.promptText = promptText;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite composite = (Composite) super.createDialogArea(parent);
		composite.setLayout(new GridLayout(2, false));
		getShell().setText(UIText.CertPasswordDialog_title);

		Label promptLabel = new Label(composite, SWT.NONE);
		promptLabel.setText(promptText);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
				.applyTo(promptLabel);

		Label passwordLabel = new Label(composite, SWT.NONE);
		passwordLabel.setText(UIText.LoginDialog_password);
		final Text certPasswordField = new Text(composite, SWT.PASSWORD
				| SWT.BORDER);
		certPasswordField.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				certPassword = certPasswordField.getText();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(certPasswordField);
		return composite;
	}

	public String getCertPassword() {
		return certPassword;
	}

}
