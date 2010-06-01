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
package org.eclipse.egit.ui.internal.clone;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * Asks the user to select a wizard and what to do with the imported projects
 * (automatic/manual/no share)
 */
public class GitSelectWizardPage extends WizardPage {

	/** */
	public static final int EXISTING_PROJECTS_WIZARD = 0;

	/** */
	public static final int NEW_WIZARD = 1;

	/** */
	public static final int GENERAL_WIZARD = 2;

	// TODO check if we need/can support Import... wizard
	// see also remarks in GitCreateProjectViaWizardWizard

	/** */
	public static final int ACTION_DIALOG_SHARE = 0;

	/** */
	public static final int ACTION_AUTO_SHARE = 1;

	/** */
	public static final int ACTION_NO_SHARE = 2;

	private final String PREF_WIZ = getName() + "WizardSel"; //$NON-NLS-1$

	private final String PREF_ACT = getName() + "ActionSel"; //$NON-NLS-1$

	Button importExisting;

	Button newProjectWizard;

	Button generalWizard;

	Button actionAutoShare;

	Button actionDialogShare;

	Button actionNothing;

	/**
	 * Default constructor
	 */
	public GitSelectWizardPage() {
		super(GitSelectWizardPage.class.getName());
		setTitle(UIText.GitSelectWizardPage_WizardTitle);
	}

	/**
	 * @param name
	 *            the page name
	 */
	protected GitSelectWizardPage(String name) {
		super(name);
	}

	public void createControl(Composite parent) {

		Composite main = new Composite(parent, SWT.NO_RADIO_GROUP);

		main.setLayout(new GridLayout(1, false));

		SelectionListener sl = new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPage();
			}
		};

		Group wizardType = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(wizardType);
		wizardType.setText(UIText.GitSelectWizardPage_ProjectCreationHeader);
		wizardType.setLayout(new GridLayout(1, false));

		importExisting = new Button(wizardType, SWT.RADIO);
		importExisting.setText(UIText.GitSelectWizardPage_ImportExistingButton);
		importExisting.addSelectionListener(sl);

		newProjectWizard = new Button(wizardType, SWT.RADIO);
		newProjectWizard
				.setText(UIText.GitSelectWizardPage_UseNewProjectsWizardButton);
		newProjectWizard.addSelectionListener(sl);

		generalWizard = new Button(wizardType, SWT.RADIO);
		generalWizard.setText(UIText.GitSelectWizardPage_ImportAsGeneralButton);
		generalWizard.addSelectionListener(sl);

		Group afterImportAction = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(
				afterImportAction);
		afterImportAction
				.setText(UIText.GitSelectWizardPage_SharingProjectsHeader);
		afterImportAction.setLayout(new GridLayout(1, false));

		actionAutoShare = new Button(afterImportAction, SWT.RADIO);
		actionAutoShare.setText(UIText.GitSelectWizardPage_AutoShareButton);
		actionAutoShare.addSelectionListener(sl);

		actionDialogShare = new Button(afterImportAction, SWT.RADIO);
		actionDialogShare
				.setText(UIText.GitSelectWizardPage_InteractiveShareButton);
		actionDialogShare.addSelectionListener(sl);

		actionNothing = new Button(afterImportAction, SWT.RADIO);
		actionNothing.setText(UIText.GitSelectWizardPage_NoShareButton);
		actionNothing.addSelectionListener(sl);

		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		int previousWiz;
		try {
			previousWiz = settings.getInt(PREF_WIZ);
		} catch (NumberFormatException e) {
			previousWiz = EXISTING_PROJECTS_WIZARD;
		}
		switch (previousWiz) {
		case EXISTING_PROJECTS_WIZARD:
			importExisting.setSelection(true);
			break;
		case GENERAL_WIZARD:
			generalWizard.setSelection(true);
			break;
		case NEW_WIZARD:
			newProjectWizard.setSelection(true);
			break;

		}

		int previousAct;
		try {
			previousAct = settings.getInt(PREF_ACT);
		} catch (NumberFormatException e) {
			previousAct = ACTION_AUTO_SHARE;
		}
		switch (previousAct) {
		case ACTION_AUTO_SHARE:
			actionAutoShare.setSelection(true);
			break;
		case ACTION_DIALOG_SHARE:
			actionDialogShare.setSelection(true);
			break;
		case ACTION_NO_SHARE:
			actionNothing.setSelection(true);
			break;
		}

		setControl(main);

	}

	/**
	 * @return the wizard selection
	 */
	public int getWizardSelection() {
		if (importExisting.getSelection())
			return EXISTING_PROJECTS_WIZARD;
		if (newProjectWizard.getSelection())
			return NEW_WIZARD;
		if (generalWizard.getSelection())
			return GENERAL_WIZARD;
		return -1;
	}

	/**
	 * @return the action selection
	 */
	public int getActionSelection() {
		if (actionAutoShare.getSelection())
			return ACTION_AUTO_SHARE;
		if (actionDialogShare.getSelection())
			return ACTION_DIALOG_SHARE;
		if (actionNothing.getSelection())
			return ACTION_NO_SHARE;
		return -1;
	}

	/**
	 * check routine
	 */
	protected void checkPage() {

		// we save the selected radio button in the preferences
		IDialogSettings settings = Activator.getDefault().getDialogSettings();

		settings.put(PREF_WIZ, getWizardSelection());
		settings.put(PREF_ACT, getActionSelection());

		setErrorMessage(null);
		try {
			// no special checks yet
		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}
}
