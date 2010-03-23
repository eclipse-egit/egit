/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 *
 */
public class SelectRemoteNamePage extends WizardPage {

	Text remoteName;

	/**
	 *
	 */
	SelectRemoteNamePage() {
		super(SelectRemoteNamePage.class.getName());
		setTitle(UIText.SelectRemoteNamePage_RemoteNameTitle);
		setMessage(UIText.SelectRemoteNamePage_RemoteNameMessage);
	}

	public void createControl(Composite parent) {

		setMessage(UIText.SelectRemoteNamePage_SelectRemoteNameMessage);

		Composite main = new Composite(parent, SWT.NONE);

		main.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		Label nameLabel = new Label(main, SWT.NONE);
		nameLabel.setText(UIText.SelectRemoteNamePage_RemoteNameLabel);

		remoteName = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(remoteName);
		remoteName.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});

		setControl(main);
		setPageComplete(false);

	}

	private void checkPage() {
		try {
			setErrorMessage(null);
			if (remoteName.getText().equals("")) { //$NON-NLS-1$
				setErrorMessage(UIText.SelectRemoteNamePage_NameMustNotBeEmptyMessage);
				return;
			}

			ConfigureRemoteWizard wizard = (ConfigureRemoteWizard) getWizard();
			if (wizard.getConfiguration().getSubsections(
					RepositoriesView.REMOTE).contains(remoteName.getText())) {
				setErrorMessage(UIText.SelectRemoteNamePage_NameInUseMessage);
				return;
			}
		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}
}
