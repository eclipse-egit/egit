/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2014, Philip Langer <planger@eclipsesource.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/** Preference page for views preferences */
public class SynchronizePreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	/**
	 * The default constructor
	 */
	public SynchronizePreferencePage() {
		super(FLAT);
		setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE,
				Activator.getPluginId()));
	}

	public void init(final IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected void createFieldEditors() {
		addField(createBooleanFieldEditorWithUIPreferenceStore(
				UIPreferences.SYNC_VIEW_FETCH_BEFORE_LAUNCH,
				UIText.GitPreferenceRoot_fetchBeforeSynchronization));
		addField(createBooleanFieldEditorWithUIPreferenceStore(
				UIPreferences.SYNC_VIEW_ALWAYS_SHOW_CHANGESET_MODEL,
				UIText.GitPreferenceRoot_automaticallyEnableChangesetModel));
		addField(new BooleanFieldEditor(
				GitCorePreferences.core_useLogicalModel,
				UIText.GitPreferenceRoot_useLogicalModel,
				getFieldEditorParent()));
	}

	private BooleanFieldEditor createBooleanFieldEditorWithUIPreferenceStore(
			final String name, final String label) {
		return new BooleanFieldEditor(name, label, getFieldEditorParent()) {
			public IPreferenceStore getPreferenceStore() {
				return org.eclipse.egit.ui.Activator.getDefault()
						.getPreferenceStore();
			}
		};
	}
}
