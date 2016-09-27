/*******************************************************************************
 * Copyright (C) 2011, 2016 Matthias Sohn <matthias.sohn@sap.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Thomas Wolf <thomas.wolf@paranor.ch> - Bug 498548
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Policy;
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

	@Override
	public void init(final IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	public boolean performOk() {
		boolean isOk = super.performOk();
		if (isOk) {
			IPreferenceStore uiPreferences = org.eclipse.egit.ui.Activator
					.getDefault().getPreferenceStore();
			if (uiPreferences.needsSaving()
					&& (uiPreferences instanceof IPersistentPreferenceStore)) {
				try {
					((IPersistentPreferenceStore) uiPreferences).save();
				} catch (IOException e) {
					String message = JFaceResources.format(
							"PreferenceDialog.saveErrorMessage", //$NON-NLS-1$
							new Object[] { getTitle(), e.getMessage() });
					Policy.getStatusHandler().show(
							new Status(IStatus.ERROR, Policy.JFACE, message, e),
							JFaceResources.getString(
									"PreferenceDialog.saveErrorTitle")); //$NON-NLS-1$
				}
			}
		}
		return isOk;
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
