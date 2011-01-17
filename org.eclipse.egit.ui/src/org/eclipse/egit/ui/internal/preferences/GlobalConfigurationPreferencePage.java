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

import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.SWTUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Displays the global Git configuration and allows to edit it.
 * <p>
 * In EGit, this maps to the user configuration.
 */
public class GlobalConfigurationPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {
	/** The ID of this page */
	public static final String ID = "org.eclipse.egit.ui.internal.preferences.GlobalConfigurationPreferencePage"; //$NON-NLS-1$

	private FileBasedConfig userConfig;

	private FileBasedConfig sysConfig;

	private boolean userIsDirty;

	private boolean sysIsDirty;

	private ConfigurationEditorComponent userConfigEditor;

	private ConfigurationEditorComponent sysConfigEditor;

	@Override
	protected Control createContents(Composite parent) {

		Composite composite = SWTUtils.createHVFillComposite(parent,
				SWTUtils.MARGINS_NONE);
		TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
		tabFolder.setLayoutData(SWTUtils.createHVFillGridData());
		userConfigEditor = new ConfigurationEditorComponent(tabFolder, userConfig, true) {
			@Override
			protected void setErrorMessage(String message) {
				GlobalConfigurationPreferencePage.this.setErrorMessage(message);
			}

			@Override
			protected void setDirty(boolean dirty) {
				userIsDirty = dirty;
				updateApplyButton();
			}
		};
		sysConfigEditor = new ConfigurationEditorComponent(tabFolder, sysConfig, true) {
			@Override
			protected void setErrorMessage(String message) {
				GlobalConfigurationPreferencePage.this.setErrorMessage(message);
			}

			@Override
			protected void setDirty(boolean dirty) {
				sysIsDirty = dirty;
				updateApplyButton();
			}
		};
		Control result = userConfigEditor.createContents();
		Dialog.applyDialogFont(result);
		TabItem userTabItem = new TabItem(tabFolder, SWT.FILL);
		userTabItem.setControl(result);
		userTabItem.setText(UIText.GlobalConfigurationPreferencePage_userSettingTabTitle);
		result = sysConfigEditor.createContents();
		Dialog.applyDialogFont(result);
		TabItem sysTabItem = new TabItem(tabFolder, SWT.FILL);
		sysTabItem.setControl(result);
		sysTabItem.setText(UIText.GlobalConfigurationPreferencePage_systemSettingTabTitle);
		return result;
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible)
			updateApplyButton();
		super.setVisible(visible);
	}

	@Override
	protected void updateApplyButton() {
		if (getApplyButton() != null)
			getApplyButton().setEnabled(userIsDirty || sysIsDirty);
	}

	@Override
	public boolean performOk() {
		boolean ok = true;
		if (userIsDirty) {
			try {
				userConfigEditor.save();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
				ok = false;
			}
		}
		if (sysIsDirty) {
			try {
				sysConfigEditor.save();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
				ok = false;
			}
		}
		return ok;
	}

	@Override
	protected void performDefaults() {
		try {
			userConfigEditor.restore();
			sysConfigEditor.restore();
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
		super.performDefaults();
	}

	public void init(IWorkbench workbench) {
		if (sysConfig == null)
			sysConfig = SystemReader.getInstance().openSystemConfig(null, FS.DETECTED);
		if (userConfig == null)
			userConfig = SystemReader.getInstance().openUserConfig(null, FS.DETECTED); // no inherit here!
	}
}
