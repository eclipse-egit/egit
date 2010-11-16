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
public class CommitDialogPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	/** */
	public CommitDialogPreferencePage() {
		super(GRID);
		setTitle(UIText.CommitDialogPreferencePage_title);
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
		BooleanFieldEditor hardWrap = new BooleanFieldEditor(
				UIPreferences.COMMIT_DIALOG_HARD_WRAP_MESSAGE,
				UIText.CommitDialogPreferencePage_hardWrapMessage,
				getFieldEditorParent());
		hardWrap.getDescriptionControl(getFieldEditorParent()).setToolTipText(UIText.CommitDialogPreferencePage_hardWrapMessageTooltip);
		addField(hardWrap);
	}
}
