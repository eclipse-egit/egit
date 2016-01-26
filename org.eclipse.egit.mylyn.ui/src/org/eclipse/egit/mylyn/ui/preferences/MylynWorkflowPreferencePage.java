/*******************************************************************************
 * Copyright (C) 2012, Robert Pofuk <rpofuk@gmail.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 *    Robert Pofuk <rpofuk@gmail.com> Bug 486268

 *******************************************************************************/
package org.eclipse.egit.mylyn.ui.preferences;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.PreferenceConstants;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preferences for Mylyn commit message editor
 *
 */
public class MylynWorkflowPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	BooleanFieldEditor booleanField;


	/**
	 *
	 */
	public MylynWorkflowPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Enable workflow enabled commit messages"); //$NON-NLS-1$
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	@Override
	public void createFieldEditors() {
		booleanField = new BooleanFieldEditor(PreferenceConstants.FORCE_TASK,
				"Force user to select task when commiting ", //$NON-NLS-1$
				getFieldEditorParent());

		addField(booleanField);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {
	}


}