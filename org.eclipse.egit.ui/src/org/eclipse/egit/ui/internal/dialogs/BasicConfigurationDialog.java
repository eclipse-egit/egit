/*******************************************************************************
 * Copyright (c) 2011 Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (c) 2011 Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.preferences.GlobalConfigurationPreferencePage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.lib.UserConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Dialog for basic configuration (User name, e-mail)
 *
 */
public class BasicConfigurationDialog extends TitleAreaDialog {
	private Button dontShowAgain;

	private StoredConfig userScopedConfig;

	private Text email;

	private Text userName;

	private boolean needsUpdate = false;

	/**
	 * Opens the dialog if the {@link UIPreferences#SHOW_INITIAL_CONFIG_DIALOG}
	 * is true and author or committer identity is based on implicit data
	 *
	 * @param repositories
	 *            if called from repository context otherwise null
	 */
	public static void show(Repository... repositories) {
		if (Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.SHOW_INITIAL_CONFIG_DIALOG)
				&& isImplicitUserConfig(repositories))
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					new BasicConfigurationDialog(PlatformUI.getWorkbench()
							.getDisplay().getActiveShell()).open();
				}
			});
	}

	private static boolean isImplicitUserConfig(Repository... repositories) {
		if (repositories == null)
			return false;

		for (Repository repository : repositories) {
			UserConfig uc = loadRepoScopedConfig(repository)
					.get(UserConfig.KEY);
			if (uc.isAuthorNameImplicit() //
					|| uc.isAuthorEmailImplicit()
					|| uc.isCommitterNameImplicit()
					|| uc.isCommitterEmailImplicit())
				return true;
		}
		return false;
	}

	private static StoredConfig loadUserScopedConfig() {
		StoredConfig c = SystemReader.getInstance().openUserConfig(null,
				FS.DETECTED);
		try {
			c.load();
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		} catch (ConfigInvalidException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
		return c;
	}

	private static StoredConfig loadRepoScopedConfig(Repository repo) {
		StoredConfig c = repo.getConfig();
		try {
			c.load();
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		} catch (ConfigInvalidException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
		return c;
	}

	/**
	 * @param parentShell
	 */
	public BasicConfigurationDialog(Shell parentShell) {
		super(parentShell);
		setHelpAvailable(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		userScopedConfig = loadUserScopedConfig();

		UserConfig userConfig = userScopedConfig.get(UserConfig.KEY);
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		// user name
		Label userNameLabel = new Label(main, SWT.NONE);
		userNameLabel.setText(UIText.BasicConfigurationDialog_UserNameLabel);
		userName = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(userName);
		String currentName = null;
		if (userConfig != null)
			currentName = userConfig.getAuthorName();
		if (currentName != null)
			userName.setText(currentName);
		userName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				needsUpdate = true;
			}
		});

		// user email
		Label emailLabel = new Label(main, SWT.NONE);
		emailLabel.setText(UIText.BasicConfigurationDialog_UserEmailLabel);
		email = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(email);
		String currentMail = null;
		if (userConfig != null)
			currentMail = userConfig.getAuthorEmail();
		if (currentMail != null)
			email.setText(currentMail);
		email.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				needsUpdate = true;
			}
		});

		CLabel configLocationInfoLabel = new CLabel(main, SWT.NONE);
		configLocationInfoLabel.setImage(JFaceResources
				.getImage(Dialog.DLG_IMG_MESSAGE_INFO));
		configLocationInfoLabel
				.setText(UIText.BasicConfigurationDialog_ConfigLocationInfo);
		GridDataFactory.fillDefaults().span(2, 1)
				.applyTo(configLocationInfoLabel);

		dontShowAgain = new Button(main, SWT.CHECK);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(dontShowAgain);
		dontShowAgain.setText(UIText.BasicConfigurationDialog_DontShowAgain);
		dontShowAgain.setSelection(true);

		Link link = new Link(main, SWT.UNDERLINE_LINK);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(link);
		link.setText(UIText.BasicConfigurationDialog_OpenPreferencePage);
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(getShell(),
						GlobalConfigurationPreferencePage.ID, null, null)
						.open();
			}
		});
		applyDialogFont(main);
		return main;
	}

	@Override
	public void create() {
		super.create();
		setTitle(UIText.BasicConfigurationDialog_DialogTitle);
		setMessage(UIText.BasicConfigurationDialog_DialogMessage);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.BasicConfigurationDialog_WindowTitle);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == Window.OK) {
			if (needsUpdate) {
				userScopedConfig.setString(ConfigConstants.CONFIG_USER_SECTION,
						null, ConfigConstants.CONFIG_KEY_NAME, userName
								.getText());
				userScopedConfig
						.setString(ConfigConstants.CONFIG_USER_SECTION, null,
								ConfigConstants.CONFIG_KEY_EMAIL, email
										.getText());
				try {
					userScopedConfig.save();
				} catch (IOException e) {
					Activator.handleError(e.getMessage(), e, true);
				}
			}
			if (dontShowAgain.getSelection())
				Activator.getDefault().getPreferenceStore().setValue(
						UIPreferences.SHOW_INITIAL_CONFIG_DIALOG, false);
		}
		super.buttonPressed(buttonId);
	}
}
