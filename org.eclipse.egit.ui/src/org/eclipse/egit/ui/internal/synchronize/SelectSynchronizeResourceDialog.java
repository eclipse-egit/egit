/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for selecting repositories to synchronization
 */
public class SelectSynchronizeResourceDialog extends Dialog {

	private final IProject project;

	private String remoteRef;

	private final List<SyncRepoEntity> syncRepos;

	private RemoteSelectionCombo remoteCombo;

	/**
	 * Construct dialog that gives user possibility to choose with which
	 * repository and branch he want to synchronize
	 *
	 * @param parent
	 * @param project
	 * @param syncRepos
	 */
	public SelectSynchronizeResourceDialog(Shell parent, IProject project,
			List<SyncRepoEntity> syncRepos) {
		super(parent);
		this.project = project;
		this.syncRepos = syncRepos;
	}

	/**
	 * @return remote ref name
	 */
	public String getValue() {
		return remoteRef;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		parent.setLayout(GridLayoutFactory.swtDefaults().create());
		parent.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
				.create());

		Button clearButton = createButton(parent, 1,
				UIText.SelectSynchronizeResourceDialog_manageRepositories,
				false);
		clearButton.setToolTipText(UIText.CreateTagDialog_clearButtonTooltip);
		setButtonLayoutData(clearButton);

		Composite margin = new Composite(parent, SWT.NONE);
		margin.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
				.create());

		super.createButtonsForButtonBar(parent);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		Label header = new Label(composite, SWT.WRAP);
		header.setText(UIText.SelectSynchronizeResourceDialog_header);
		header.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
				.create());

		remoteCombo = new RemoteSelectionCombo(composite, syncRepos);
		remoteCombo.setLayoutData(GridDataFactory.fillDefaults().grab(true,
				false).create());

		Button isDefault = new Button(composite, SWT.CHECK);
		isDefault.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());
		isDefault.setText(UIText.SelectSynchronizeResourceDialog_setAsDefault);

		return composite;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			remoteRef = remoteCombo.getValue();
		}
		super.buttonPressed(buttonId);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(NLS.bind(
				UIText.SelectSynchronizeResourceDialog_selectProject, project
						.getName()));
	}

}
