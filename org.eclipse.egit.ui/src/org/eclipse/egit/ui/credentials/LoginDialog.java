/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Edwin Kempin <edwin.kempin@sap.com>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.credentials;

import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * This class implements a login dialog asking for user and password for a given
 * URI.
 */
public class LoginDialog extends Dialog {

	private Text user;

	private Text password;

	private UserPasswordCredentials credentials;

	private final URIish uri;

	private boolean isUserSet;

	private boolean changeCredentials = false;

	private LoginDialog(Shell shell, URIish uri) {
		super(shell);
		this.uri = uri;
		isUserSet = uri.getUser() != null && uri.getUser().length() > 0;
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
		}
		GridDataFactory.fillDefaults().grab(true, false).applyTo(user);
		Label passwordLabel = new Label(composite, SWT.NONE);
		passwordLabel.setText(UIText.LoginDialog_password);
		password = new Text(composite, SWT.PASSWORD | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(password);
		if (isUserSet)
			password.setFocus();
		else
			user.setFocus();
		return composite;
	}

	/**
	 * The method shows a login dialog for a given URI. The user field is taken
	 * from the URI if a user is present in the URI.
	 * In this case the user is not editable.
	 *
	 * @param parent
	 * @param uri
	 * @return credentials, <code>null</code> if the user canceled the dialog.
	 */
	public static UserPasswordCredentials login(Shell parent, URIish uri) {
		LoginDialog dialog = new LoginDialog(parent, uri);
		if (dialog.open() == OK) {
			return dialog.credentials;
		}
		return null;
	}

	/**
	 * The method shows a change credentials dialog for a given URI. The user field is taken
	 * from the URI if a user is present in the URI.
	 * In this case the user is not editable.
	 *
	 * @param parent
	 * @param uri
	 * @return credentials, <code>null</code> if the user canceled the dialog.
	 */
	public static UserPasswordCredentials changeCredentials(Shell parent, URIish uri) {
		LoginDialog dialog = new LoginDialog(parent, uri);
		dialog.changeCredentials = true;
		if (dialog.open() == OK) {
			return dialog.credentials;
		}
		return null;
	}

	@Override
	protected void okPressed() {
		if (user.getText().length() > 0)
			credentials = new UserPasswordCredentials(user.getText(),
					password.getText());
		super.okPressed();
	}

}
