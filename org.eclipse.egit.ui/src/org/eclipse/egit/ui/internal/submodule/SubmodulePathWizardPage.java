/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.submodule;

import java.io.File;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Wizard page to configure the path of a submodule
 */
public class SubmodulePathWizardPage extends WizardPage {

	private final Repository repo;

	private Text pathText;

	private String path;

	/**
	 * Create submodule path wizard page
	 *
	 * @param repo
	 */
	public SubmodulePathWizardPage(Repository repo) {
		super("pathPage"); //$NON-NLS-1$
		this.repo = repo;
	}

	@Override
	public void createControl(Composite parent) {
		Composite displayArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).equalWidth(false)
				.applyTo(displayArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(displayArea);

		new Label(displayArea, SWT.NONE)
				.setText(UIText.SubmodulePathWizardPage_PathLabel);

		pathText = new Text(displayArea, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).applyTo(pathText);

		pathText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		});

		setControl(displayArea);
		setTitle(UIText.SubmodulePathWizardPage_Title);
		setMessage(UIText.SubmodulePathWizardPage_Message);
		setPageComplete(false);
	}

	/**
	 * @return path
	 */
	public String getPath() {
		return path;
	}

	private void validate() {
		final String currentPath = pathText.getText();
		if (currentPath.length() == 0) {
			setPageComplete(false);
			return;
		}

		File file = new File(repo.getWorkTree(), currentPath);
		if (file.isFile()) {
			setErrorMessage(UIText.SubmodulePathWizardPage_ErrorPathMustBeEmpty);
			setPageComplete(false);
			return;
		}

		if (file.isDirectory()) {
			String[] children = file.list();
			if (children != null && children.length > 0) {
				setErrorMessage(UIText.SubmodulePathWizardPage_ErrorPathMustBeEmpty);
				setPageComplete(false);
				return;
			}
		}

		this.path = currentPath;
		setErrorMessage(null);
		setPageComplete(true);
	}
}
