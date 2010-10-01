/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/** Root preference page for the all of our workspace preferences. */
public class GitPreferenceRoot extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {
	/**
	 * The default constructor
	 */
	public GitPreferenceRoot() {
		super(GRID);
	}

	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	public void init(final IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected void createFieldEditors() {
		DirectoryFieldEditor editor = new DirectoryFieldEditor(
				UIPreferences.DEFAULT_REPO_DIR,
				UIText.GitPreferenceRoot_DefaultRepoFolderLabel,
				getFieldEditorParent()) {

			@Override
			protected void createControl(Composite parent) {
				// setting validate strategy using the setter method is too late
				super
						.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);
				super.createControl(parent);
			}

		};
		editor.setEmptyStringAllowed(false);
		addField(editor);
		editor.getLabelControl(getFieldEditorParent()).setToolTipText(
				UIText.GitPreferenceRoot_DefaultRepoFolderTooltip);
	}
}
