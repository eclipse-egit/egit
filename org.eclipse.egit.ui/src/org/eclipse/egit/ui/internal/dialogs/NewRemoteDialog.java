/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.Set;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(main);
		GridDataFactory.fillDefaults().indent(5, 5).grab(true, true).applyTo(
				main);
		Label nameLabel = new Label(main, SWT.NONE);
		nameLabel.setText(UIText.NewRemoteDialog_NameLabel);
		nameText = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(nameText);
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});

		forPush = new Button(main, SWT.RADIO);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(forPush);
		forPush.setText(UIText.NewRemoteDialog_PushRadio);
		forPush.setSelection(true);

		Button forFetch = new Button(main, SWT.RADIO);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(forFetch);
		forFetch.setText(UIText.NewRemoteDialog_FetchRadio);
		nameText.setFocus();
		applyDialogFont(main);
		return main;
	}

	private void checkPage() {
		boolean errorFound = false;
		setErrorMessage(null);
		if (existingRemotes.contains(nameText.getText())) {
			setErrorMessage(NLS.bind(
					UIText.NewRemoteDialog_RemoteAlreadyExistsMessage, nameText
							.getText()));
			errorFound = true;
		}
		getButton(OK).setEnabled(!errorFound);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK) {
			name = nameText.getText();
			pushMode = forPush.getSelection();
		}
		super.buttonPressed(buttonId);
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
