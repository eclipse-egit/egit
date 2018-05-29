/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.Set;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog for creating a new remote
 * <p>
 * Asks for a name for the new remote and whether it should be configured for
 * fetch or push
 */
public class NewRemoteDialog extends TitleAreaDialog {
	private Text nameText;

	private Set<String> existingRemotes;

	private Button forPush;

	private String name;

	private boolean pushMode;

	/**
	 * @param parentShell
	 * @param repository
	 */
	public NewRemoteDialog(Shell parentShell, Repository repository) {
		super(parentShell);
		existingRemotes = repository.getConfig().getSubsections(
				ConfigConstants.CONFIG_REMOTE_SECTION);
	}

	@Override
	public void create() {
		super.create();
		setTitle(UIText.NewRemoteDialog_DialogTitle);
		setMessage(UIText.NewRemoteDialog_ConfigurationMessage);
		if (existingRemotes.isEmpty()) {
			nameText.setText(Constants.DEFAULT_REMOTE_NAME);
			nameText.selectAll();
		}
		checkPage();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.NewRemoteDialog_WindowTitle);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		Label nameLabel = new Label(main, SWT.NONE);
		nameLabel.setText(UIText.NewRemoteDialog_NameLabel);
		nameText = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(nameText);
		nameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});

		Composite buttonComposite = new Composite(main, SWT.NONE);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(buttonComposite);
		buttonComposite.setLayout(new RowLayout(SWT.VERTICAL));
		forPush = new Button(buttonComposite, SWT.RADIO);
		forPush.setText(UIText.NewRemoteDialog_PushRadio);
		forPush.setSelection(true);

		Button forFetch = new Button(buttonComposite, SWT.RADIO);
		forFetch.setText(UIText.NewRemoteDialog_FetchRadio);
		nameText.setFocus();
		applyDialogFont(main);
		main.setTabList(new Control[] { nameText, buttonComposite });
		return main;
	}

	private void checkPage() {
		boolean errorFound = false;
		setErrorMessage(null);
		String t = getTrimmedRemoteName();
		if (t.length() > 0
				&& !Repository.isValidRefName(Constants.R_REMOTES + t)) {
			setErrorMessage(NLS.bind(UIText.NewRemoteDialog_InvalidRemoteName,
					t));
			errorFound = true;
		} else if (existingRemotes.contains(t)) {
			setErrorMessage(NLS.bind(
					UIText.NewRemoteDialog_RemoteAlreadyExistsMessage, t));
			errorFound = true;
		}
		getButton(OK).setEnabled(!errorFound && t.length() > 0);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK) {
			name = getTrimmedRemoteName();
			pushMode = forPush.getSelection();
		}
		super.buttonPressed(buttonId);
	}

	private String getTrimmedRemoteName() {
		return nameText.getText().trim();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return if the new remote is to be configured for push
	 */
	public boolean getPushMode() {
		return pushMode;
	}

}
