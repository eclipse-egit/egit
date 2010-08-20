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

	@Override
	protected Control createContents(Composite parent) {
		Control result = new ConfigurationEditorComponent(parent, userConfig,
				true) {
			@Override
			protected void setErrorMessage(String message) {
				GlobalConfigurationPreferencePage.this.setErrorMessage(message);
			}
		}.createContents();
		Dialog.applyDialogFont(result);
		return result;
	}

	public void init(IWorkbench workbench) {
		super.noDefaultAndApplyButton();
		if (userConfig == null)
			userConfig = SystemReader.getInstance().openUserConfig(FS.DETECTED);
	}
}
