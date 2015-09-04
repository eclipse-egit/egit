/*******************************************************************************
 * Copyright (C) 2011, 2015 Mathias Kinzler <mathias.kinzler@sap.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/** Preference page for views preferences */
public class SynchronizePreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	private final PreferredMergedStrategyHelper helper;

	/**
	 * The default constructor
	 */
	public SynchronizePreferencePage() {
		super(FLAT);
		helper = new PreferredMergedStrategyHelper(true);
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	@Override
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
		addField(new BooleanFieldEditor(UIPreferences.USE_LOGICAL_MODEL,
				UIText.GitPreferenceRoot_useLogicalModel,
				getFieldEditorParent()));

		Label spacer = new Label(getFieldEditorParent(), SWT.NONE);
		spacer.setSize(0, 12);
		helper.createPreferredStrategyPanel(getFieldEditorParent());

		addField(new BooleanFieldEditor(
				UIPreferences.PREFERRED_MERGE_STRATEGY_HIDE_DIALOG,
				UIText.GitPreferenceRoot_hideMergeStrategyDialog,
				getFieldEditorParent()));
	}

	@Override
	protected void initialize() {
		super.initialize();
		helper.load();
	}

	@Override
	public boolean performOk() {
		if (super.performOk()) {
			// Need to save the core preference store because the
			// PreferenceDialog will only save the store provided
			// by doGetPreferenceStore()
			helper.store();
			helper.save();
		}
		return true;
	}
}
