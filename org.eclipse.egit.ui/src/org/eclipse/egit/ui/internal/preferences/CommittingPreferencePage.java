/*******************************************************************************
 * Copyright (C) 2010, Robin Stocker <robin@nibor.org>
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
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/** Preferences for commit dialog. */
public class CommittingPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	/** */
	public CommittingPreferencePage() {
		super(GRID);
		setTitle(UIText.CommittingPreferencePage_title);
	}

	public void init(IWorkbench workbench) {
		// Nothing to do
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(
				UIPreferences.COMMITTING_HARD_WRAP_MESSAGE,
				UIText.CommittingPreferencePage_hardWrapMessage,
				getFieldEditorParent()));
	}
}
