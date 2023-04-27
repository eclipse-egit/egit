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

import java.net.URISyntaxException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.clone.GitUrlChecker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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
		else {
			getRemoteNameFromOrganization();
		}
		checkPage();
	}

	private void getRemoteNameFromOrganization() {
		Clipboard clipboard = new Clipboard(Display.getCurrent());
		try {
			String text = (String) clipboard
					.getContents(TextTransfer.getInstance());
			if (text != null) {
				text = GitUrlChecker.sanitizeAsGitUrl(text);
				if (GitUrlChecker.isValidGitUrl(text)) {
					final URIish u = new URIish(text);
					String repoPath = u.getPath();
					if (repoPath != null) {
						// find the organization part in
						// https://host/organization/repo.git
						Matcher matcher = Pattern.compile("^/((?:\\w|-)+)/") //$NON-NLS-1$
								.matcher(repoPath);
					if (matcher.find()) {
						nameText.setText(matcher.group(1));
						nameText.selectAll();
					}
				}
				}
			}
		} catch (URISyntaxException e) {
			// nothing to do here, clipboard may contain arbitrary junk
		} finally {
			clipboard.dispose();
		}
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

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				UIText.NewRemoteDialog_ButtonOK, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}
}
