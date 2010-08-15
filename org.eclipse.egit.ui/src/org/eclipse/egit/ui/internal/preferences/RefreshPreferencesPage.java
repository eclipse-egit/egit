/*******************************************************************************
 * Copyright (C) 2010, Robin Rosenberg
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/** Preferences refreshing based on changes in Git Repositories. */
public class RefreshPreferencesPage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	/** */
	public RefreshPreferencesPage() {
		super(GRID);
		setTitle(UIText.RefreshPreferencePage_title);
		ScopedPreferenceStore store = new ScopedPreferenceStore(
				new InstanceScope(), Activator.getPluginId());
		setPreferenceStore(store);
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(UIPreferences.REFESH_ON_INDEX_CHANGE,
				UIText.RefreshPreferencesPage_RefreshWhenIndexChange,
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(UIPreferences.REFESH_ONLY_WHEN_ACTIVE,
				UIText.RefreshPreferencesPage_RefreshOnlyWhenActive,
				getFieldEditorParent()));
	}

	public boolean performOk() {
		// first put the editor values into the configuration
		return super.performOk();
		// No action required. The thread that need these properties
		// read them directly.
	}

	public void init(IWorkbench workbench) {
		// Nothing to do
	}
}
