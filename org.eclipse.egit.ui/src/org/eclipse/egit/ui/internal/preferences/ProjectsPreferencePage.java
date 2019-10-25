/*******************************************************************************
 * Copyright (C) 2011, 2017 Matthias Sohn <matthias.sohn@sap.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Thomas Wolf <thomas.wolf@paranor.ch> - Bug 498548
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/** Preference page for project preferences */
public class ProjectsPreferencePage extends DoublePreferencesPreferencePage
		implements IWorkbenchPreferencePage {

	/**
	 * The default constructor
	 */
	public ProjectsPreferencePage() {
		super(GRID);
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return new ScopedPreferenceStore(InstanceScope.INSTANCE,
				Activator.getPluginId());
	}

	@Override
	protected IPreferenceStore doGetSecondaryPreferenceStore() {
		return org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore();
	}

	@Override
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

			@Override
			public void setPreferenceStore(IPreferenceStore store) {
				super.setPreferenceStore(
						store == null ? null : getSecondaryPreferenceStore());
			}
		});
		addField(new BooleanFieldEditor(
				GitCorePreferences.core_autoIgnoreDerivedResources,
				UIText.ProjectsPreferencePage_AutoIgnoreDerivedResources,
				getFieldEditorParent()));
	}
}
