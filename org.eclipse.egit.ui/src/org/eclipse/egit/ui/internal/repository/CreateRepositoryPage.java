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

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Asks for a directory and whether to create a bare repository
 */
public class CreateRepositoryPage extends WizardPage {
	private final boolean hideBare;

	private Text directoryText;

	private Button bareButton;

	/**
	 * Constructs this page
	 *
	 * @param hideBareOption
	 */
	public CreateRepositoryPage(boolean hideBareOption) {
		super(CreateRepositoryPage.class.getName());
		this.hideBare = hideBareOption;
		setTitle(UIText.CreateRepositoryPage_PageTitle);
		setMessage(UIText.CreateRepositoryPage_PageMessage);
		// we must at least enter the directory
		setPageComplete(false);
	}

	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		Label directoryLabel = new Label(main, SWT.NONE);
		directoryLabel.setText(UIText.CreateRepositoryPage_DirectoryLabel);
		directoryText = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).applyTo(directoryText);
		Button browseButton = new Button(main, SWT.PUSH);
		browseButton.setText(UIText.CreateRepositoryPage_BrowseButton);
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String previous = directoryText.getText();
				File previousFile = new File(previous);
				String result;
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				if (previousFile.exists() && previousFile.isDirectory()) {
					dialog.setFilterPath(previousFile.getPath());
				}
				result = dialog.open();
				if (result != null)
					directoryText.setText(result);
			}
		});

		if (!hideBare) {
			bareButton = new Button(main, SWT.CHECK);
			bareButton.setText(UIText.CreateRepositoryPage_BareCheckbox);
			GridDataFactory.fillDefaults().indent(10, 0).span(3, 1)
					.applyTo(bareButton);
		}

		directoryText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});

		setControl(main);
	}

	/**
	 * @return the directory where to create the Repository
	 */
	public String getDirectory() {
		return directoryText.getText();
	}

	/**
	 * @return <code>true</code> if a bare Repository is to be created
	 */
	public boolean getBare() {
		return bareButton != null && bareButton.getSelection();
	}

	void checkPage() {
		setErrorMessage(null);
		try {
			String dir = directoryText.getText();
			if (dir.length() == 0) {
				setErrorMessage(UIText.CreateRepositoryPage_PleaseSelectDirectoryMessage);
				return;
			}
			File testFile = new File(dir);
			IPath path = new Path(dir);
			if (!path.isAbsolute()) {
				setErrorMessage(UIText.CreateRepositoryPage_PleaseUseAbsoluePathMessage);
				return;
			}
			if (testFile.exists() && !testFile.isDirectory()) {
				setErrorMessage(NLS.bind(
						UIText.CreateRepositoryPage_NotADirectoryMessage, dir));
				return;
			}
			if (testFile.exists() && testFile.list().length > 0) {
				setErrorMessage(NLS.bind(
						UIText.CreateRepositoryPage_NotEmptyMessage, dir));
				return;
			}
		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}
}
