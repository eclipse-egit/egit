/*******************************************************************************
 * Copyright (C) 2011, 2015 Mathias Kinzler <mathias.kinzler@sap.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator.MergeStrategyDescriptor;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Policy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/** Preference page for views preferences */
public class SynchronizePreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	private RadioGroupFieldEditor preferredMergeStrategyEditor;

	private ScopedPreferenceStore corePreferenceStore;

	/**
	 * The default constructor
	 */
	public SynchronizePreferencePage() {
		super(FLAT);
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
		Composite modelStrategyParent = getFieldEditorParent();
		preferredMergeStrategyEditor = new RadioGroupFieldEditor(
				GitCorePreferences.core_preferredMergeStrategy,
				UIText.GitPreferenceRoot_preferreMergeStrategy_group, 1,
				getAvailableMergeStrategies(), modelStrategyParent, false);
		preferredMergeStrategyEditor.getLabelControl(modelStrategyParent)
				.setToolTipText(
						UIText.GitPreferenceRoot_preferreMergeStrategy_label);
		addField(preferredMergeStrategyEditor);
	}

	@Override
	protected void initialize() {
		super.initialize();
		// The default initialize sets the same preference store for every field
		// We want the preferred strategy to use the core store
		preferredMergeStrategyEditor
				.setPreferenceStore(getCorePreferenceStore());
		preferredMergeStrategyEditor.load();
	}

	private String[][] getAvailableMergeStrategies() {
		org.eclipse.egit.core.Activator coreActivator = org.eclipse.egit.core.Activator
				.getDefault();
		List<String[]> strategies = new ArrayList<>();

		// We need to add a default name for EGit default strategy in order to
		// be able to distinguish a state where users haven't chosen any
		// preference (in this case, values defined in eclipse configuration
		// files may apply) or if they have chosen the default merge strategy.
		strategies.add(new String[] {
				UIText.GitPreferenceRoot_defaultMergeStrategyLabel,
				GitCorePreferences.core_preferredMergeStrategy_Default });
		for (MergeStrategyDescriptor strategy : coreActivator
				.getRegisteredMergeStrategies()) {
			strategies.add(new String[] { strategy.getLabel(),
					strategy.getName() });
		}
		return strategies.toArray(new String[strategies.size()][2]);
	}

	private ScopedPreferenceStore getCorePreferenceStore() {
		if (corePreferenceStore == null) {
			corePreferenceStore = new ScopedPreferenceStore(
					InstanceScope.INSTANCE,
					org.eclipse.egit.core.Activator.getPluginId());
		}
		return corePreferenceStore;
	}

	@Override
	public boolean performOk() {
		if (super.performOk()) {
			// Need to save the core preference store because the
			// PreferenceDialog will only save the store provided
			// by doGetPreferenceStore()
			if (getCorePreferenceStore().needsSaving()) {
				try {
					getCorePreferenceStore().save();
				} catch (IOException e) {
					String message = JFaceResources
							.format("PreferenceDialog.saveErrorMessage", new Object[] { //$NON-NLS-1$
									getTitle(), e.getMessage() });
					Policy.getStatusHandler()
							.show(new Status(IStatus.ERROR, Policy.JFACE,
									message, e),
									JFaceResources
											.getString("PreferenceDialog.saveErrorTitle")); //$NON-NLS-1$

				}
			}
		}
		return true;
	}
}
