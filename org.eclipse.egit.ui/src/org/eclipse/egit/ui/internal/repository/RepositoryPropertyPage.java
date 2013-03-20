/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.io.IOException;

import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.preferences.ConfigurationEditorComponent;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Property page for elements that can adapt to a {@link Repository} object.
 */
public class RepositoryPropertyPage extends PropertyPage {

	private ConfigurationEditorComponent editor;

	protected Control createContents(Composite parent) {
		Composite displayArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(displayArea);
		GridDataFactory.fillDefaults().applyTo(displayArea);

		Repository repo = (Repository) getElement()
				.getAdapter(Repository.class);
		if (repo == null)
			return displayArea;

		StoredConfig config = repo.getConfig();
		if (config instanceof FileBasedConfig) {
			File configFile = ((FileBasedConfig) config).getFile();
			config = new FileBasedConfig(configFile, repo.getFS());
		}
		editor = new ConfigurationEditorComponent(displayArea, config, true, false) {
			@Override
			protected void setErrorMessage(String message) {
				RepositoryPropertyPage.this.setErrorMessage(message);
			}
		};
		editor.createContents();
		return displayArea;
	}

	protected void performDefaults() {
		if (editor != null)
			try {
				editor.restore();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
		super.performDefaults();
	}

	public boolean performOk() {
		if (editor != null)
			try {
				editor.save();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
		return super.performOk();
	}
}
