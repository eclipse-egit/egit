/*******************************************************************************
 * Copyright (C) 2011, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/** Preference page for project preferences */
public class ProjectsPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	/**
	 * The default constructor
	 */
	public ProjectsPreferencePage() {
		super(GRID);
		ScopedPreferenceStore store = new ScopedPreferenceStore(
				InstanceScope.INSTANCE, Activator.getPluginId());
		setPreferenceStore(store);
	}

	public void init(final IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(GitCorePreferences.core_autoShareProjects,
				UIText.ProjectsPreferencePage_AutoShareProjects,
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(UIPreferences.CHECKOUT_PROJECT_RESTORE,
				UIText.ProjectsPreferencePage_RestoreBranchProjects,
				getFieldEditorParent()) {
			public IPreferenceStore getPreferenceStore() {
				return org.eclipse.egit.ui.Activator.getDefault()
						.getPreferenceStore();
			}
		});
		addField(new BooleanFieldEditor(
				GitCorePreferences.core_autoIgnoreDerivedResources,
				UIText.ProjectsPreferencePage_AutoIgnoreDerivedResources,
				getFieldEditorParent()));
	}
}
