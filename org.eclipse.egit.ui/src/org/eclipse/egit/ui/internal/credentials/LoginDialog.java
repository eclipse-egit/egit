/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Edwin Kempin <edwin.kempin@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.credentials;

import org.eclipse.egit.core.credentials.UserPasswordCredentials;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * This class implements a login dialog asking for user and password for a given
 * URI.
 */
class LoginDialog extends Dialog {

	private final URIish uri;

	private final boolean changeCredentials;

	private Text user;

	private Text password;

	private Button storeCheckbox;

	private UserPasswordCredentials credentials;

	private boolean storeInSecureStore;

	private boolean isUserSet;

	private String oldUser;

	LoginDialog(Shell shell, URIish uri) {
		this(shell, uri, false);
	}

	LoginDialog(Shell shell, URIish uri, boolean changeCrendentials) {
		super(shell);
		this.uri = uri;
		this.changeCredentials = changeCrendentials;
		isUserSet = uri.getUser() != null && !uri.getUser().isEmpty();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite composite = (Composite) super.createDialogArea(parent);
		composite.setLayout(new GridLayout(2, false));
		getShell().setText(
				changeCredentials ? UIText.LoginDialog_changeCredentials
						: UIText.LoginDialog_login);

		Label uriLabel = new Label(composite, SWT.NONE);
		uriLabel.setText(UIText.LoginDialog_repository);
		Text uriText = new Text(composite, SWT.READ_ONLY);
		uriText.setText(uri.toString());

		Label userLabel = new Label(composite, SWT.NONE);
		userLabel.setText(UIText.LoginDialog_user);
		if (isUserSet) {
			user = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
			user.setText(uri.getUser());
		} else {
			user = new Text(composite, SWT.BORDER);
			if (oldUser != null)
				user.setText(oldUser);
		}
		GridDataFactory.fillDefaults().grab(true, false).applyTo(user);

		Label passwordLabel = new Label(composite, SWT.NONE);
		passwordLabel.setText(UIText.LoginDialog_password);
		password = new Text(composite, SWT.PASSWORD | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(password);

		if(!changeCredentials) {
			Label storeLabel = new Label(composite, SWT.NONE);
			storeLabel.setText(UIText.LoginDialog_storeInSecureStore);
			storeCheckbox = new Button(composite, SWT.CHECK);
			storeCheckbox.setSelection(Activator.getDefault()
					.getPreferenceStore()
					.getBoolean(UIPreferences.CLONE_WIZARD_STORE_SECURESTORE));
		}

		if (isUserSet)
			password.setFocus();
		else
			user.setFocus();

		return composite;
	}

	UserPasswordCredentials getCredentials() {
		return credentials;
	}

	boolean getStoreInSecureStore() {
		return storeInSecureStore;
	}

	@Override
	protected void okPressed() {
		if (user.getText().length() > 0) {
			credentials = new UserPasswordCredentials(user.getText(),
					password.getText());
			if(!changeCredentials)
				storeInSecureStore = storeCheckbox.getSelection();
		}
		super.okPressed();
	}

	public void setOldUser(String oldUser) {
		this.oldUser = oldUser;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				changeCredentials ? UIText.LoginDialog_ButtonSave
						: UIText.LoginDialog_ButtonLogin,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}
}
