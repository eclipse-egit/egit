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
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Displays the global Git configuration and allows to edit it.
 * <p>
 * In EGit, this maps to the user configuration.
 */
public class GlobalConfigurationPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {
	private FileBasedConfig userConfig;

	private boolean isDirty;

	private ConfigurationEditorComponent editor;

	@Override
	protected Control createContents(Composite parent) {

		editor = new ConfigurationEditorComponent(parent, userConfig, true) {
			@Override
			protected void setErrorMessage(String message) {
				GlobalConfigurationPreferencePage.this.setErrorMessage(message);
			}

			@Override
			protected void setDirty(boolean dirty) {
				isDirty = dirty;
				updateApplyButton();
			}
		};
		Control result = editor.createContents();
		Dialog.applyDialogFont(result);
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
			getApplyButton().setEnabled(isDirty);
	}

	@Override
	public boolean performOk() {
		if (isDirty)
			try {
				editor.save();
				return super.performOk();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
				return false;
			}
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		try {
			editor.restore();
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
		super.performDefaults();
	}

	public void init(IWorkbench workbench) {
		if (userConfig == null)
			userConfig = SystemReader.getInstance().openUserConfig(FS.DETECTED);
	}
}
