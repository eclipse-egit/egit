/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/** Preference page for views preferences */
public class SynchronizePreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	/**
	 * The default constructor
	 */
	public SynchronizePreferencePage() {
		super(FLAT);
	}

	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	public void init(final IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(
				UIPreferences.SYNC_VIEW_FETCH_BEFORE_LAUNCH,
				UIText.GitPreferenceRoot_fetchBeforeSynchronization,
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				UIPreferences.SYNC_VIEW_ALWAYS_SHOW_CHANGESET_MODEL,
				UIText.GitPreferenceRoot_automaticallyEnableChangesetModel,
				getFieldEditorParent()));
	}
}
